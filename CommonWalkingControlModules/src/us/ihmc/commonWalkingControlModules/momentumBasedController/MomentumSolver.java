package us.ihmc.commonWalkingControlModules.momentumBasedController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolver;
import org.ejml.ops.CommonOps;

import us.ihmc.utilities.CheckTools;
import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.NullspaceCalculator;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.CentroidalMomentumMatrix;
import us.ihmc.utilities.screwTheory.GeometricJacobian;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.Momentum;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTools;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.screwTheory.SpatialForceVector;
import us.ihmc.utilities.screwTheory.SpatialMotionVector;
import us.ihmc.utilities.screwTheory.TwistCalculator;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.MatrixYoVariableConversionTools;

public class MomentumSolver
{
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);

   private final DenseMatrix64F vdotRoot;
   private final DenseMatrix64F b;
   private final DenseMatrix64F v;

   private final DenseMatrix64F T;
   private final DenseMatrix64F alpha1;
   private final DenseMatrix64F N;
   private final DenseMatrix64F beta1;

   private final RootJointSolver rootJointSolver;
   private final JointSpaceConstraintResolver jointSpaceConstraintResolver;
   private final TaskSpaceConstraintResolver taskSpaceConstraintResolver;
   private final NullspaceCalculator nullspaceCalculator;

   private final double controlDT;

   private final CentroidalMomentumMatrix centroidalMomentumMatrix;
   private final DenseMatrix64F centroidalMomentumMatrixDerivative;
   private final DenseMatrix64F previousCentroidalMomentumMatrix;
   private final DoubleYoVariable[][] yoPreviousCentroidalMomentumMatrix;    // to make numerical differentiation rewindable

   private final InverseDynamicsJoint[] jointsInOrder;
   private final List<InverseDynamicsJoint> jointsInOrderList;
   private final SixDoFJoint rootJoint;


   // LinkedHashMaps so that order of computation is defined
   private final LinkedHashMap<InverseDynamicsJoint, int[]> columnsForJoints = new LinkedHashMap<InverseDynamicsJoint, int[]>();
   private final LinkedHashMap<InverseDynamicsJoint, DenseMatrix64F> aHats = new LinkedHashMap<InverseDynamicsJoint, DenseMatrix64F>();
   private final LinkedHashMap<InverseDynamicsJoint, DenseMatrix64F> jointSpaceAccelerations = new LinkedHashMap<InverseDynamicsJoint, DenseMatrix64F>();
   private final LinkedHashMap<GeometricJacobian, SpatialAccelerationVector> spatialAccelerations = new LinkedHashMap<GeometricJacobian,
                                                                                                       SpatialAccelerationVector>();
   private final LinkedHashMap<GeometricJacobian, DenseMatrix64F> nullspaceMultipliers = new LinkedHashMap<GeometricJacobian, DenseMatrix64F>();
   private final LinkedHashMap<GeometricJacobian, DenseMatrix64F> taskSpaceSelectionMatrices = new LinkedHashMap<GeometricJacobian, DenseMatrix64F>();

   private final List<InverseDynamicsJoint> unconstrainedJoints = new ArrayList<InverseDynamicsJoint>();

   public MomentumSolver(SixDoFJoint rootJoint, RigidBody elevator, ReferenceFrame centerOfMassFrame, TwistCalculator twistCalculator,
                         LinearSolver<DenseMatrix64F> jacobianSolver, double controlDT, YoVariableRegistry parentRegistry)
   {
      this.rootJoint = rootJoint;
      this.jointsInOrder = ScrewTools.computeJointsInOrder(elevator);
      this.jointsInOrderList = Collections.unmodifiableList(Arrays.asList(jointsInOrder));

      int size = Momentum.SIZE;
      this.nullspaceCalculator = new NullspaceCalculator(size);
      this.rootJointSolver = new RootJointSolver();
      this.jointSpaceConstraintResolver = new JointSpaceConstraintResolver();
      this.taskSpaceConstraintResolver = new TaskSpaceConstraintResolver(rootJoint, jointsInOrder, twistCalculator, nullspaceCalculator, jacobianSolver);

      int nDegreesOfFreedom = ScrewTools.computeDegreesOfFreedom(jointsInOrder);

      v = new DenseMatrix64F(nDegreesOfFreedom, 1);
      vdotRoot = new DenseMatrix64F(rootJoint.getDegreesOfFreedom(), 1);

      this.controlDT = controlDT;

      this.centroidalMomentumMatrix = new CentroidalMomentumMatrix(elevator, centerOfMassFrame);
      this.previousCentroidalMomentumMatrix = new DenseMatrix64F(centroidalMomentumMatrix.getMatrix().getNumRows(),
              centroidalMomentumMatrix.getMatrix().getNumCols());
      this.centroidalMomentumMatrixDerivative = new DenseMatrix64F(centroidalMomentumMatrix.getMatrix().getNumRows(),
              centroidalMomentumMatrix.getMatrix().getNumCols());

      b = new DenseMatrix64F(centroidalMomentumMatrixDerivative.getNumRows(), v.getNumCols());

      for (InverseDynamicsJoint joint : jointsInOrder)
      {
         columnsForJoints.put(joint, ScrewTools.computeIndicesForJoint(jointsInOrder, joint));
         aHats.put(joint, new DenseMatrix64F(size, joint.getDegreesOfFreedom()));
      }

      T = new DenseMatrix64F(size, size);    // will reshape later
      alpha1 = new DenseMatrix64F(size, 1);    // will reshape later
      N = new DenseMatrix64F(size, size);    // will reshape later
      beta1 = new DenseMatrix64F(size, 1);    // will reshape later

      yoPreviousCentroidalMomentumMatrix = new DoubleYoVariable[previousCentroidalMomentumMatrix.getNumRows()][previousCentroidalMomentumMatrix.getNumCols()];
      MatrixYoVariableConversionTools.populateYoVariables(yoPreviousCentroidalMomentumMatrix, "previousCMMatrix", registry);

      parentRegistry.addChild(registry);
      reset();
   }

   public void initialize()
   {
      centroidalMomentumMatrix.compute();
      previousCentroidalMomentumMatrix.set(centroidalMomentumMatrix.getMatrix());
      MatrixYoVariableConversionTools.storeInYoVariables(previousCentroidalMomentumMatrix, yoPreviousCentroidalMomentumMatrix);
   }

   public void reset()
   {
      jointSpaceAccelerations.clear();
      spatialAccelerations.clear();
      nullspaceMultipliers.clear();
      taskSpaceSelectionMatrices.clear();
      taskSpaceConstraintResolver.reset();
      unconstrainedJoints.clear();
      unconstrainedJoints.addAll(jointsInOrderList);
      unconstrainedJoints.remove(rootJoint);
   }

   public void setDesiredJointAcceleration(InverseDynamicsJoint joint, DenseMatrix64F jointAcceleration)
   {
      checkAndRegisterConstraint(joint);
      jointSpaceAccelerations.put(joint, jointAcceleration);
   }

   public void setDesiredSpatialAcceleration(GeometricJacobian jacobian, SpatialAccelerationVector spatialAcceleration, DenseMatrix64F nullspaceMultipliers)
   {
      int size = SpatialAccelerationVector.SIZE;
      DenseMatrix64F s = new DenseMatrix64F(size, size);    // TODO: garbage
      CommonOps.setIdentity(s);
      setDesiredSpatialAcceleration(jacobian, spatialAcceleration, nullspaceMultipliers, s);
   }

   public void setDesiredAngularAcceleration(GeometricJacobian jacobian, ReferenceFrame baseFrame, FrameVector desiredAngularAcceleration,
           DenseMatrix64F nullspaceMultiplier)
   {
      SpatialAccelerationVector spatialAcceleration = new SpatialAccelerationVector(jacobian.getEndEffector().getBodyFixedFrame(), baseFrame,
                                                         desiredAngularAcceleration.getReferenceFrame());
      spatialAcceleration.setAngularPart(desiredAngularAcceleration.getVector());

      DenseMatrix64F selectionMatrix = new DenseMatrix64F(3, SpatialMotionVector.SIZE);    // TODO: garbage
      selectionMatrix.set(0, 0, 1.0);
      selectionMatrix.set(1, 1, 1.0);
      selectionMatrix.set(2, 2, 1.0);

      setDesiredSpatialAcceleration(jacobian, spatialAcceleration, nullspaceMultiplier, selectionMatrix);
   }

   public void setDesiredSpatialAcceleration(GeometricJacobian jacobian, SpatialAccelerationVector spatialAcceleration, DenseMatrix64F nullspaceMultipliers,
           DenseMatrix64F selectionMatrix)
   {
      checkNullspaceDimensions(jacobian, nullspaceMultipliers);

      for (InverseDynamicsJoint joint : jacobian.getJointsInOrder())
      {
         checkAndRegisterConstraint(joint);
      }

      ReferenceFrame baseFrame = spatialAcceleration.getBaseFrame();
      boolean isWithRespectToWorld = baseFrame == rootJoint.getPredecessor().getBodyFixedFrame();
      if (isWithRespectToWorld)
      {
         this.spatialAccelerations.put(jacobian, spatialAcceleration);
         this.nullspaceMultipliers.put(jacobian, nullspaceMultipliers);
         this.taskSpaceSelectionMatrices.put(jacobian, selectionMatrix);
      }
      else
      {
         if (baseFrame == jacobian.getBaseFrame())
         {
            taskSpaceConstraintResolver.convertInternalSpatialAccelerationToJointSpace(jointSpaceAccelerations, jacobian, spatialAcceleration,
                    nullspaceMultipliers, selectionMatrix);
         }
         else
         {
            throw new RuntimeException("Case not yet implemented: baseFrame = " + baseFrame);
         }
      }
   }

   public void compute()
   {
      checkFullyConstrained();

      centroidalMomentumMatrix.compute();
      MatrixYoVariableConversionTools.getFromYoVariables(previousCentroidalMomentumMatrix, yoPreviousCentroidalMomentumMatrix);
      MatrixTools.numericallyDifferentiate(centroidalMomentumMatrixDerivative, previousCentroidalMomentumMatrix, centroidalMomentumMatrix.getMatrix(),
              controlDT);
      MatrixYoVariableConversionTools.storeInYoVariables(previousCentroidalMomentumMatrix, yoPreviousCentroidalMomentumMatrix);

      initializeAHats(aHats, centroidalMomentumMatrix.getMatrix());
      initializeB(b, centroidalMomentumMatrixDerivative, jointsInOrder);

      for (GeometricJacobian jacobian : spatialAccelerations.keySet())
      {
         SpatialAccelerationVector taskSpaceAcceleration = spatialAccelerations.get(jacobian);
         DenseMatrix64F nullspaceMultiplier = nullspaceMultipliers.get(jacobian);
         DenseMatrix64F selectionMatrix = taskSpaceSelectionMatrices.get(jacobian);
         taskSpaceConstraintResolver.handleTaskSpaceAccelerations(aHats, b, centroidalMomentumMatrix.getMatrix(), jacobian, taskSpaceAcceleration,
                 nullspaceMultiplier, selectionMatrix);
      }

      for (InverseDynamicsJoint joint : jointSpaceAccelerations.keySet())
      {
         DenseMatrix64F jointSpaceAcceleration = jointSpaceAccelerations.get(joint);
         jointSpaceConstraintResolver.handleJointSpaceAcceleration(aHats.get(joint), b, centroidalMomentumMatrix.getMatrix(), joint, jointSpaceAcceleration);
      }
   }

   public void solve(SpatialForceVector momentumRateOfChange)
   {
      T.reshape(SpatialMotionVector.SIZE, 0);
      alpha1.reshape(T.getNumCols(), 1);
      N.reshape(SpatialForceVector.SIZE, SpatialForceVector.SIZE);
      beta1.reshape(N.getNumCols(), 1);

      CommonOps.setIdentity(N);
      momentumRateOfChange.packMatrix(beta1);

      solve(T, alpha1, N, beta1);
   }

   public void solve(DenseMatrix64F accelerationSubspace, DenseMatrix64F accelerationMultipliers, DenseMatrix64F momentumSubspace,
                     DenseMatrix64F momentumMultipliers)
   {
      rootJointSolver.solveAndSetRootJointAcceleration(vdotRoot, aHats.get(rootJoint), b, accelerationSubspace, accelerationMultipliers, momentumSubspace,
              momentumMultipliers, rootJoint);
      jointSpaceConstraintResolver.solveAndSetJointspaceAccelerations(jointSpaceAccelerations);
      taskSpaceConstraintResolver.solveAndSetTaskSpaceAccelerations();
   }

   public void getRateOfChangeOfMomentum(SpatialForceVector rateOfChangeOfMomentumToPack)
   {
      rateOfChangeOfMomentumToPack.set(centroidalMomentumMatrix.getReferenceFrame(), rootJointSolver.getRateOfChangeOfMomentum());
   }

   private void initializeB(DenseMatrix64F b, DenseMatrix64F centroidalMomentumMatrixDerivative, InverseDynamicsJoint[] jointsInOrder)
   {
      ScrewTools.packJointVelocitiesMatrix(jointsInOrder, v);
      CommonOps.mult(centroidalMomentumMatrixDerivative, v, b);
      CommonOps.scale(-1.0, b);
   }

   private void checkFullyConstrained()
   {
      if (unconstrainedJoints.size() > 0)
         throw new RuntimeException("Not fully constrained. Unconstrained joints: " + unconstrainedJoints);
   }

   private void initializeAHats(LinkedHashMap<InverseDynamicsJoint, DenseMatrix64F> aHats, DenseMatrix64F centroidalMomentumMatrix)
   {
      for (InverseDynamicsJoint joint : jointsInOrder)
      {
         MatrixTools.extractColumns(centroidalMomentumMatrix, aHats.get(joint), columnsForJoints.get(joint));
      }
   }

   private void checkAndRegisterConstraint(InverseDynamicsJoint joint)
   {
      if (!unconstrainedJoints.remove(joint))
         throw new RuntimeException("Joint overconstrained: " + joint);
   }

   private static void checkNullspaceDimensions(GeometricJacobian jacobian, DenseMatrix64F nullspaceMultipliers)
   {
      CheckTools.checkEquals(nullspaceMultipliers.getNumCols(), 1);
      if (nullspaceMultipliers.getNumRows() > jacobian.getNumberOfColumns())
         throw new RuntimeException("nullspaceMultipliers dimension too large");
   }

   public SpatialAccelerationVector getSpatialAcceleration(GeometricJacobian jacobian)
   {
      return spatialAccelerations.get(jacobian);
   }
}
