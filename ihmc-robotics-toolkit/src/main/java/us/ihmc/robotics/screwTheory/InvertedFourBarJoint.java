package us.ihmc.robotics.screwTheory;

import java.util.Collections;
import java.util.List;
import java.util.function.DoubleSupplier;

import org.ejml.data.DMatrixRMaj;

import us.ihmc.euclid.orientation.interfaces.Orientation3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameVector3DReadOnly;
import us.ihmc.euclid.referenceFrame.tools.EuclidFrameFactories;
import us.ihmc.euclid.tools.EuclidCoreIOTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.transform.interfaces.RigidBodyTransformReadOnly;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.mecano.frames.MovingReferenceFrame;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.RevoluteJointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.mecano.spatial.SpatialAcceleration;
import us.ihmc.mecano.spatial.Twist;
import us.ihmc.mecano.spatial.Wrench;
import us.ihmc.mecano.spatial.interfaces.SpatialAccelerationBasics;
import us.ihmc.mecano.spatial.interfaces.SpatialAccelerationReadOnly;
import us.ihmc.mecano.spatial.interfaces.SpatialVectorReadOnly;
import us.ihmc.mecano.spatial.interfaces.TwistReadOnly;
import us.ihmc.mecano.spatial.interfaces.WrenchReadOnly;
import us.ihmc.mecano.tools.MecanoFactories;
import us.ihmc.yoVariables.exceptions.IllegalOperationException;

public class InvertedFourBarJoint implements OneDoFJointBasics
{
   private final String name;
   private final String nameId;
   private final RigidBodyBasics predecessor;
   private final RigidBodyBasics successor;
   private final MovingReferenceFrame beforeJointFrame;
   private final MovingReferenceFrame afterJointFrame;

   private final FourBarKinematicLoopFunction fourBarFunction;
   private InvertedFourBarJointIKSolver ikSolver;

   private final TwistReadOnly jointTwist;
   private final Twist unitJointTwist = new Twist();
   private final Twist unitSuccessorTwist = new Twist();
   private final Twist unitPredecessorTwist = new Twist();
   private final List<TwistReadOnly> unitTwists;

   private final SpatialAccelerationReadOnly jointAcceleration;
   private final SpatialAcceleration biasJointAcceleration = new SpatialAcceleration();
   private final SpatialAcceleration biasSuccessorAcceleration = new SpatialAcceleration();
   private final SpatialAcceleration unitJointAcceleration = new SpatialAcceleration();
   private final SpatialAcceleration unitSuccessorAcceleration = new SpatialAcceleration();
   private final SpatialAcceleration unitPredecessorAcceleration = new SpatialAcceleration();

   private final Wrench unitJointWrench = new Wrench();
   private final WrenchReadOnly jointWrench;

   /** Variable to store intermediate results for garbage-free operations. */
   private final Vector3D rotationVector = new Vector3D();

   public InvertedFourBarJoint(String name, RevoluteJointBasics[] fourBarJoints, int masterJointIndex)
   {
      fourBarFunction = new FourBarKinematicLoopFunction(name, fourBarJoints, masterJointIndex);
      if (!fourBarFunction.isInverted())
         throw new IllegalArgumentException("The given joint configuration does not represent an inverted four bar.");
      setIKSolver(new InvertedFourBarJointIKBinarySolver(1.0e-5));

      this.name = name;
      predecessor = getJointA().getPredecessor(); // TODO Need to replace child of predecessor.
      successor = getJointD().getSuccessor(); // TODO Need to re-map the parent joint of the successor.
      beforeJointFrame = getJointA().getFrameBeforeJoint();
      afterJointFrame = getJointD().getFrameAfterJoint();

      if (predecessor.isRootBody())
         nameId = name;
      else
         nameId = predecessor.getParentJoint().getNameId() + NAME_ID_SEPARATOR + name;

      unitTwists = Collections.singletonList(unitJointTwist);
      jointTwist = MecanoFactories.newTwistReadOnly(this::getQd, unitJointTwist);
      jointAcceleration = setupJointAcceleration();
      jointWrench = MecanoFactories.newWrenchReadOnly(this::getTau, unitJointWrench);
   }

   private SpatialAccelerationReadOnly setupJointAcceleration()
   {
      DoubleSupplier wx = () -> getQdd() * unitJointAcceleration.getAngularPartX() + biasJointAcceleration.getAngularPartX();
      DoubleSupplier wy = () -> getQdd() * unitJointAcceleration.getAngularPartY() + biasJointAcceleration.getAngularPartY();
      DoubleSupplier wz = () -> getQdd() * unitJointAcceleration.getAngularPartZ() + biasJointAcceleration.getAngularPartZ();
      DoubleSupplier vx = () -> getQdd() * unitJointAcceleration.getLinearPartX() + biasJointAcceleration.getLinearPartX();
      DoubleSupplier vy = () -> getQdd() * unitJointAcceleration.getLinearPartY() + biasJointAcceleration.getLinearPartY();
      DoubleSupplier vz = () -> getQdd() * unitJointAcceleration.getLinearPartZ() + biasJointAcceleration.getLinearPartZ();
      FrameVector3DReadOnly angularPart = EuclidFrameFactories.newLinkedFrameVector3DReadOnly(this::getFrameAfterJoint, wx, wy, wz);
      FrameVector3DReadOnly linearPart = EuclidFrameFactories.newLinkedFrameVector3DReadOnly(this::getFrameAfterJoint, vx, vy, vz);
      return MecanoFactories.newSpatialAccelerationVectorReadOnly(afterJointFrame, beforeJointFrame, angularPart, linearPart);
   }

   /**
    * Sets the solver to use for computing the four bar configuration given the joint angle via
    * {@link #setQ(double)}.
    * 
    * @param ikSolver the solver to use.
    */
   public void setIKSolver(InvertedFourBarJointIKSolver ikSolver)
   {
      this.ikSolver = ikSolver;
      ikSolver.setConverters(fourBarFunction.getConverters());
   }

   @Override
   public void updateFramesRecursively()
   {
      fourBarFunction.updateState(true, true);
      getJointA().getFrameBeforeJoint().update();
      getJointB().getFrameBeforeJoint().update();
      getJointC().getFrameBeforeJoint().update();
      getJointD().getFrameBeforeJoint().update();
      getJointA().getFrameAfterJoint().update();
      getJointB().getFrameAfterJoint().update();
      getJointC().getFrameAfterJoint().update();
      getJointD().getFrameAfterJoint().update();

      updateMotionSubspace();

      if (getSuccessor() != null)
      {
         getSuccessor().updateFramesRecursively();
      }
   }

   private final Twist tempTwist = new Twist();
   private final SpatialAcceleration tempAcceleration = new SpatialAcceleration();
   private final Twist deltaTwist = new Twist();
   private final Twist bodyTwist = new Twist();

   @Override
   public void updateMotionSubspace()
   {
      DMatrixRMaj loopJacobian = fourBarFunction.getLoopJacobian();
      DMatrixRMaj loopConvectiveTerm = fourBarFunction.getLoopConvectiveTerm();
      RevoluteJointBasics jointA = getJointA();
      RevoluteJointBasics jointD = getJointD();
      double J_A = loopJacobian.get(0);
      double J_D = loopJacobian.get(3);

      unitJointTwist.setIncludingFrame(jointA.getUnitJointTwist());
      unitJointTwist.scale(J_A);
      unitJointTwist.setBodyFrame(jointD.getFrameBeforeJoint());
      unitJointTwist.changeFrame(jointD.getFrameAfterJoint());
      tempTwist.setIncludingFrame(jointD.getUnitJointTwist());
      tempTwist.scale(J_D);
      unitJointTwist.add(tempTwist);
      unitJointTwist.scale(1.0 / (J_A + J_D));

      unitSuccessorTwist.setIncludingFrame(unitJointTwist);
      unitSuccessorTwist.setBaseFrame(predecessor.getBodyFixedFrame());
      unitSuccessorTwist.setBodyFrame(successor.getBodyFixedFrame());
      unitSuccessorTwist.changeFrame(successor.getBodyFixedFrame());

      unitPredecessorTwist.setIncludingFrame(unitSuccessorTwist);
      unitPredecessorTwist.invert();
      unitPredecessorTwist.changeFrame(predecessor.getBodyFixedFrame());

      unitJointAcceleration.setIncludingFrame(jointA.getUnitJointAcceleration());
      unitJointAcceleration.scale(J_A);
      unitJointAcceleration.setBodyFrame(jointD.getFrameBeforeJoint());
      unitJointAcceleration.changeFrame(jointD.getFrameAfterJoint());
      tempAcceleration.setIncludingFrame(jointD.getUnitJointAcceleration());
      tempAcceleration.scale(J_D);
      unitJointAcceleration.add(tempAcceleration);
      unitJointAcceleration.scale(1.0 / (J_A + J_D));

      unitSuccessorAcceleration.setIncludingFrame(unitJointAcceleration);
      unitSuccessorAcceleration.setBaseFrame(predecessor.getBodyFixedFrame());
      unitSuccessorAcceleration.setBodyFrame(successor.getBodyFixedFrame());
      unitSuccessorAcceleration.changeFrame(successor.getBodyFixedFrame());

      unitPredecessorAcceleration.setIncludingFrame(unitSuccessorAcceleration);
      unitPredecessorAcceleration.invert();
      unitPredecessorAcceleration.changeFrame(getPredecessor().getBodyFixedFrame());

      jointD.getFrameAfterJoint().getTwistRelativeToOther(jointA.getFrameAfterJoint(), deltaTwist);
      jointD.getFrameBeforeJoint().getTwistRelativeToOther(jointA.getFrameBeforeJoint(), bodyTwist);

      deltaTwist.changeFrame(jointD.getFrameAfterJoint());
      bodyTwist.changeFrame(jointD.getFrameAfterJoint());
      biasJointAcceleration.setIncludingFrame(jointA.getUnitJointAcceleration());
      biasJointAcceleration.scale(loopConvectiveTerm.get(0));
      biasJointAcceleration.setBodyFrame(jointD.getFrameBeforeJoint());
      biasJointAcceleration.changeFrame(jointD.getFrameAfterJoint(), deltaTwist, bodyTwist);
      tempAcceleration.setIncludingFrame(jointD.getUnitJointAcceleration());
      tempAcceleration.scale(loopConvectiveTerm.get(3));
      biasJointAcceleration.add(tempAcceleration);

      tempAcceleration.setIncludingFrame(unitJointAcceleration);
      tempAcceleration.scale(-(loopConvectiveTerm.get(0) + loopConvectiveTerm.get(3)));
      biasJointAcceleration.add((SpatialVectorReadOnly) tempAcceleration);

      biasSuccessorAcceleration.setIncludingFrame(biasJointAcceleration);
      biasSuccessorAcceleration.setBaseFrame(getPredecessor().getBodyFixedFrame());
      biasSuccessorAcceleration.setBodyFrame(getSuccessor().getBodyFixedFrame());
      biasSuccessorAcceleration.changeFrame(getSuccessor().getBodyFixedFrame());

      unitJointWrench.setIncludingFrame(fourBarFunction.getMasterJoint().getUnitJointTwist());
      unitJointWrench.changeFrame(afterJointFrame);
      unitJointWrench.setBodyFrame(getSuccessor().getBodyFixedFrame());
   }

   public FourBarKinematicLoopFunction getFourBarFunction()
   {
      return fourBarFunction;
   }

   public RevoluteJointBasics getMasterJoint()
   {
      return fourBarFunction.getMasterJoint();
   }

   public RevoluteJointBasics getJointA()
   {
      return fourBarFunction.getJointA();
   }

   public RevoluteJointBasics getJointB()
   {
      return fourBarFunction.getJointB();
   }

   public RevoluteJointBasics getJointC()
   {
      return fourBarFunction.getJointC();
   }

   public RevoluteJointBasics getJointD()
   {
      return fourBarFunction.getJointD();
   }

   public InvertedFourBarJointIKSolver getIKSolver()
   {
      return ikSolver;
   }

   @Override
   public MovingReferenceFrame getFrameBeforeJoint()
   {
      return beforeJointFrame;
   }

   @Override
   public MovingReferenceFrame getFrameAfterJoint()
   {
      return afterJointFrame;
   }

   @Override
   public RigidBodyBasics getPredecessor()
   {
      return predecessor;
   }

   @Override
   public RigidBodyBasics getSuccessor()
   {
      return successor;
   }

   @Override
   public MovingReferenceFrame getLoopClosureFrame()
   {
      return null;
   }

   @Override
   public String getName()
   {
      return name;
   }

   @Override
   public String getNameId()
   {
      return nameId;
   }

   @Override
   public void setupLoopClosure(RigidBodyBasics successor, RigidBodyTransformReadOnly transformFromSuccessorParentJoint)
   {
      throw new UnsupportedOperationException("Loop closure using a four bar joint has not been implemented.");
   }

   @Override
   public FrameVector3DReadOnly getJointAxis()
   {
      return getMasterJoint().getJointAxis();
   }

   @Override
   public double getQ()
   {
      return getJointA().getQ() + getJointD().getQ();
   }

   @Override
   public double getQd()
   {
      return getJointA().getQd() + getJointD().getQd();
   }

   @Override
   public double getQdd()
   {
      return getJointA().getQdd() + getJointD().getQdd();
   }

   @Override
   public double getTau()
   {
      return getMasterJoint().getTau();
   }

   @Override
   public double getJointLimitLower()
   {
      return getJointA().getJointLimitLower() + getJointD().getJointLimitLower();
   }

   @Override
   public double getJointLimitUpper()
   {
      return getJointA().getJointLimitUpper() + getJointD().getJointLimitUpper();
   }

   @Override
   public double getVelocityLimitLower()
   {
      return getJointA().getVelocityLimitLower() + getJointD().getVelocityLimitLower();
   }

   @Override
   public double getVelocityLimitUpper()
   {
      return getJointA().getVelocityLimitUpper() + getJointD().getVelocityLimitUpper();
   }

   @Override
   public double getEffortLimitLower()
   {
      return getMasterJoint().getEffortLimitLower();
   }

   @Override
   public double getEffortLimitUpper()
   {
      return getMasterJoint().getEffortLimitUpper();
   }

   @Override
   public TwistReadOnly getUnitJointTwist()
   {
      return unitJointTwist;
   }

   @Override
   public TwistReadOnly getUnitSuccessorTwist()
   {
      return unitSuccessorTwist;
   }

   @Override
   public TwistReadOnly getUnitPredecessorTwist()
   {
      return unitPredecessorTwist;
   }

   @Override
   public SpatialAccelerationReadOnly getUnitJointAcceleration()
   {
      return unitJointAcceleration;
   }

   @Override
   public SpatialAccelerationReadOnly getUnitSuccessorAcceleration()
   {
      return unitSuccessorAcceleration;
   }

   @Override
   public SpatialAccelerationReadOnly getUnitPredecessorAcceleration()
   {
      return unitPredecessorAcceleration;
   }

   @Override
   public void getJointConfiguration(RigidBodyTransform jointConfigurationToPack)
   {
      afterJointFrame.getTransformToDesiredFrame(jointConfigurationToPack, beforeJointFrame);
   }

   @Override
   public TwistReadOnly getJointTwist()
   {
      return jointTwist;
   }

   @Override
   public List<TwistReadOnly> getUnitTwists()
   {
      return unitTwists;
   }

   @Override
   public SpatialAccelerationReadOnly getJointAcceleration()
   {
      return jointAcceleration;
   }

   public SpatialAccelerationReadOnly getBiasJointAcceleration()
   {
      return biasJointAcceleration;
   }

   @Override
   public void getSuccessorAcceleration(SpatialAccelerationBasics accelerationToPack)
   {
      accelerationToPack.setIncludingFrame(unitSuccessorAcceleration);
      accelerationToPack.scale(getQdd());
      accelerationToPack.add((SpatialVectorReadOnly) biasSuccessorAcceleration);
   }

   @Override
   public void getPredecessorAcceleration(SpatialAccelerationBasics accelerationToPack)
   {
      // OneDoFJointReadOnly.getPredecessorAcceleration(...) was not used when creating this joint.
      // Implementing it would require extra calculation in the updateMotionSubspace().
      throw new UnsupportedOperationException("Implement me!");
   }

   @Override
   public WrenchReadOnly getJointWrench()
   {
      return jointWrench;
   }

   @Override
   public void setSuccessor(RigidBodyBasics successor)
   {
      throw new IllegalOperationException("The successor is set at construction");
   }

   @Override
   public void setJointOrientation(Orientation3DReadOnly jointOrientation)
   {
      jointOrientation.getRotationVector(rotationVector);
      setQ(rotationVector.dot(getJointAxis()));
   }

   @Override
   public void setJointPosition(Tuple3DReadOnly jointPosition)
   {
      // This joint type behaves more like a revolute joint.
   }

   @Override
   public void setJointAngularVelocity(Vector3DReadOnly jointAngularVelocity)
   {
      setQd(jointAngularVelocity.dot(getJointAxis()));
   }

   @Override
   public void setJointLinearVelocity(Vector3DReadOnly jointLinearVelocity)
   {
      // This joint type behaves more like a revolute joint.
   }

   @Override
   public void setJointAngularAcceleration(Vector3DReadOnly jointAngularAcceleration)
   {
      setQdd(jointAngularAcceleration.dot(getJointAxis()));
   }

   @Override
   public void setJointLinearAcceleration(Vector3DReadOnly jointLinearAcceleration)
   {
      // This joint type behaves more like a revolute joint.
   }

   @Override
   public void setJointTorque(Vector3DReadOnly jointTorque)
   {
      setTau(jointTorque.dot(getJointAxis()));
   }

   @Override
   public void setJointForce(Vector3DReadOnly jointForce)
   {
      // This joint type behaves more like a revolute joint.
   }

   @Override
   public void setQ(double q)
   {
      getMasterJoint().setQ(ikSolver.solve(q, fourBarFunction.getMasterVertex()));
   }

   @Override
   public void setQd(double qd)
   {
      fourBarFunction.updateState(false, false);
      // qd = (J_A + J_D) qd_M = (J_B + J_C) qd_M
      DMatrixRMaj loopJacobian = fourBarFunction.getLoopJacobian();
      double qd_master = qd / (loopJacobian.get(0) + loopJacobian.get(3));
      getMasterJoint().setQd(qd_master);
   }

   @Override
   public void setQdd(double qdd)
   {
      fourBarFunction.updateState(false, false);
      // qdd = (J_A + J_D) qdd_M + c_A + c_D = (J_B + J_C) qdd_M + c_B + c_C
      DMatrixRMaj loopJacobian = fourBarFunction.getLoopJacobian();
      DMatrixRMaj loopConvectiveTerm = fourBarFunction.getLoopConvectiveTerm();
      qdd = qdd - loopConvectiveTerm.get(0) - loopConvectiveTerm.get(3);
      double qdd_master = qdd / (loopJacobian.get(0) + loopJacobian.get(3));
      getMasterJoint().setQdd(qdd_master);
   }

   @Override
   public void setTau(double tau)
   {
      getMasterJoint().setTau(tau);
   }

   @Override
   public void setJointLimitLower(double jointLimitLower)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public void setJointLimitUpper(double jointLimitUpper)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public void setVelocityLimitLower(double velocityLimitLower)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public void setVelocityLimitUpper(double velocityLimitUpper)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public void setEffortLimitLower(double effortLimitLower)
   {
      getMasterJoint().setEffortLimitLower(effortLimitLower);
   }

   @Override
   public void setEffortLimitUpper(double effortLimitUpper)
   {
      getMasterJoint().setEffortLimitUpper(effortLimitUpper);
   }

   /**
    * Returns the implementation name of this joint and the joint name.
    */
   @Override
   public String toString()
   {
      String qAsString = String.format(EuclidCoreIOTools.DEFAULT_FORMAT, getQ());
      String qdAsString = String.format(EuclidCoreIOTools.DEFAULT_FORMAT, getQd());
      String qddAsString = String.format(EuclidCoreIOTools.DEFAULT_FORMAT, getQdd());
      String tauAsString = String.format(EuclidCoreIOTools.DEFAULT_FORMAT, getTau());
      return super.toString() + ", q: " + qAsString + ", qd: " + qdAsString + ", qdd: " + qddAsString + ", tau: " + tauAsString;
   }

   /**
    * The hash code of a joint is based on its {@link #getNameId()}.
    *
    * @return the hash code of the {@link #getNameId()} of this joint.
    */
   @Override
   public int hashCode()
   {
      return nameId.hashCode();
   }
}
