package us.ihmc.commonWalkingControlModules.virtualModelControl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import us.ihmc.commonWalkingControlModules.controllerCore.command.virtualModelControl.VirtualWrenchCommand;
import us.ihmc.commonWalkingControlModules.virtualModelControl.VirtualModelControllerTestHelper.RobotLegs;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.tools.ReferenceFrameTools;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.mecano.multiBodySystem.interfaces.JointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.mecano.spatial.Wrench;
import us.ihmc.mecano.tools.MultiBodySystemTools;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.screwTheory.GeometricJacobian;
import us.ihmc.simulationConstructionSetTools.tools.RobotTools.SCSRobotFromInverseDynamicsRobotModel;
import us.ihmc.simulationconstructionset.ExternalForcePoint;
import us.ihmc.simulationconstructionset.util.simulationTesting.SimulationTestingParameters;
import us.ihmc.yoVariables.registry.YoRegistry;

public class VirtualModelControllerTest
{
   private final Random bigRandom = new Random(1000L);
   private final Random random = new Random();
   private final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromSystemProperties();

   private boolean hasSCSSimulation = false;

   @Test
   public void testJacobianCalculation()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      RobotLegs robotLeg = VirtualModelControllerTestHelper.createRobotLeg(gravity);
      RigidBodyBasics endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBodyBasics foot = endEffector.getParentJoint().getSuccessor();
      RigidBodyBasics pelvis = robotLeg.getRootJoint().getSuccessor();

      FrameVector3D desiredForce = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector3D desiredTorque = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench wrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredTorque, desiredForce);

      JointBasics[] controlledJoints = MultiBodySystemTools.createJointPath(pelvis, foot);
      GeometricJacobian jacobian = new GeometricJacobian(controlledJoints, pelvis.getBodyFixedFrame());
      jacobian.compute();

      DMatrixRMaj jacobianMatrix = jacobian.getJacobianMatrix();
      DMatrixRMaj transposeJacobianMatrix = new DMatrixRMaj(jacobianMatrix.numCols, Wrench.SIZE);
      CommonOps_DDRM.transpose(jacobianMatrix, transposeJacobianMatrix);

      wrench.changeFrame(pelvis.getBodyFixedFrame());
      DMatrixRMaj wrenchMatrix = new DMatrixRMaj(Wrench.SIZE, 1);
      wrenchMatrix.set(0, 0, wrench.getAngularPartX());
      wrenchMatrix.set(1, 0, wrench.getAngularPartY());
      wrenchMatrix.set(2, 0, wrench.getAngularPartZ());
      wrenchMatrix.set(3, 0, wrench.getLinearPartX());
      wrenchMatrix.set(4, 0, wrench.getLinearPartY());
      wrenchMatrix.set(5, 0, wrench.getLinearPartZ());

      DMatrixRMaj jointEffort = new DMatrixRMaj(controlledJoints.length, 1);
      CommonOps_DDRM.multTransA(jacobianMatrix, wrenchMatrix, jointEffort);

      desiredForce.changeFrame(foot.getBodyFixedFrame());
      wrench.changeFrame(foot.getBodyFixedFrame());

      DMatrixRMaj appliedWrenchMatrix = new DMatrixRMaj(Wrench.SIZE, 1);
      CommonOps_DDRM.invert(transposeJacobianMatrix);
      CommonOps_DDRM.mult(transposeJacobianMatrix, jointEffort, appliedWrenchMatrix);
      Wrench appliedWrench = new Wrench(foot.getBodyFixedFrame(), jacobian.getJacobianFrame(), appliedWrenchMatrix);
      appliedWrench.changeFrame(foot.getBodyFixedFrame());

      VirtualModelControllerTestHelper.compareWrenches(wrench, appliedWrench);
   }

   @Test
   public void testVMC()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      
      RobotLegs robotLeg = VirtualModelControllerTestHelper.createRobotLeg(gravity);
      RigidBodyBasics endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBodyBasics foot = endEffector.getParentJoint().getSuccessor();
      RigidBodyBasics pelvis = robotLeg.getRootJoint().getSuccessor();
      ReferenceFrame centerOfMassFrame = robotLeg.getReferenceFrames().getCenterOfMassFrame();

      // send in the correct frame and no selection matrix
      FrameVector3D desiredForce = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector3D desiredTorque = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredTorque, desiredForce);

      submitAndCheckVMC(pelvis, foot, centerOfMassFrame, desiredWrench, null);
   }

   @Test
   public void testVMCSelectAll()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      
      RobotLegs robotLeg = VirtualModelControllerTestHelper.createRobotLeg(gravity);
      RigidBodyBasics endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBodyBasics foot = endEffector.getParentJoint().getSuccessor();
      RigidBodyBasics pelvis = robotLeg.getRootJoint().getSuccessor();
      ReferenceFrame centerOfMassFrame = robotLeg.getReferenceFrames().getCenterOfMassFrame();


      // send in the correct frame with identity selection matrix
      FrameVector3D desiredForce = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector3D desiredTorque = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredTorque, desiredForce);

      submitAndCheckVMC(pelvis, foot, centerOfMassFrame, desiredWrench, CommonOps_DDRM.identity(Wrench.SIZE, Wrench.SIZE));
   }

   @Test
   public void testVMCSelectForce()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      
      RobotLegs robotLeg = VirtualModelControllerTestHelper.createRobotLeg(gravity);
      RigidBodyBasics endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBodyBasics foot = endEffector.getParentJoint().getSuccessor();
      RigidBodyBasics pelvis = robotLeg.getRootJoint().getSuccessor();
      ReferenceFrame centerOfMassFrame = robotLeg.getReferenceFrames().getCenterOfMassFrame();


      // send in the correct frame with identity selection matrix
      FrameVector3D desiredForce = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector3D desiredTorque = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredTorque, desiredForce);

      // select only force
      DMatrixRMaj selectionMatrix = new DMatrixRMaj(3, 6);
      selectionMatrix.set(0, 3, 1);
      selectionMatrix.set(1, 4, 1);
      selectionMatrix.set(2, 5, 1);

      submitAndCheckVMC(pelvis, foot, centerOfMassFrame, desiredWrench, selectionMatrix);
   }

   @Test
   public void testVMCSelectTorque()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      
      RobotLegs robotLeg = VirtualModelControllerTestHelper.createRobotLeg(gravity);
      RigidBodyBasics endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBodyBasics foot = endEffector.getParentJoint().getSuccessor();
      RigidBodyBasics pelvis = robotLeg.getRootJoint().getSuccessor();
      ReferenceFrame centerOfMassFrame = robotLeg.getReferenceFrames().getCenterOfMassFrame();


      // send in the correct frame with identity selection matrix
      FrameVector3D desiredForce = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector3D desiredTorque = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredTorque, desiredForce);

      // select only torque
      DMatrixRMaj selectionMatrix = new DMatrixRMaj(3, 6);
      selectionMatrix.set(0, 0, 1);
      selectionMatrix.set(1, 1, 1);
      selectionMatrix.set(2, 2, 1);

      submitAndCheckVMC(pelvis, foot, centerOfMassFrame, desiredWrench, selectionMatrix);
   }

   @Test
   public void testVMCSelectForceX()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      
      RobotLegs robotLeg = VirtualModelControllerTestHelper.createRobotLeg(gravity);
      RigidBodyBasics endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBodyBasics foot = endEffector.getParentJoint().getSuccessor();
      RigidBodyBasics pelvis = robotLeg.getRootJoint().getSuccessor();
      ReferenceFrame centerOfMassFrame = robotLeg.getReferenceFrames().getCenterOfMassFrame();

      // send in the correct frame with identity selection matrix
      FrameVector3D desiredForce = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector3D desiredTorque = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredTorque, desiredForce);

      // select only torque
      DMatrixRMaj selectionMatrix = new DMatrixRMaj(1, 6);
      selectionMatrix.set(0, 3, 1);

      submitAndCheckVMC(pelvis, foot, centerOfMassFrame, desiredWrench, selectionMatrix);
   }

   @Test
   public void testVMCSelectForceY()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      
      RobotLegs robotLeg = VirtualModelControllerTestHelper.createRobotLeg(gravity);
      RigidBodyBasics endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBodyBasics foot = endEffector.getParentJoint().getSuccessor();
      RigidBodyBasics pelvis = robotLeg.getRootJoint().getSuccessor();
      ReferenceFrame centerOfMassFrame = robotLeg.getReferenceFrames().getCenterOfMassFrame();

      // send in the correct frame with identity selection matrix
      FrameVector3D desiredForce = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector3D desiredTorque = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredTorque, desiredForce);

      // select only torque
      DMatrixRMaj selectionMatrix = new DMatrixRMaj(1, 6);
      selectionMatrix.set(0, 4, 1);

      submitAndCheckVMC(pelvis, foot, centerOfMassFrame, desiredWrench, selectionMatrix);
   }

   @Test
   public void testVMCSelectForceZ()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      
      RobotLegs robotLeg = VirtualModelControllerTestHelper.createRobotLeg(gravity);
      RigidBodyBasics endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBodyBasics foot = endEffector.getParentJoint().getSuccessor();
      RigidBodyBasics pelvis = robotLeg.getRootJoint().getSuccessor();
      ReferenceFrame centerOfMassFrame = robotLeg.getReferenceFrames().getCenterOfMassFrame();

      // send in the correct frame with identity selection matrix
      FrameVector3D desiredForce = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector3D desiredTorque = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredTorque, desiredForce);

      // select only torque
      DMatrixRMaj selectionMatrix = new DMatrixRMaj(1, 6);
      selectionMatrix.set(0, 5, 1);

      submitAndCheckVMC(pelvis, foot, centerOfMassFrame, desiredWrench, selectionMatrix);
   }

   @Test
   public void testVMCSelectTorqueX()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      
      RobotLegs robotLeg = VirtualModelControllerTestHelper.createRobotLeg(gravity);
      RigidBodyBasics endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBodyBasics foot = endEffector.getParentJoint().getSuccessor();
      RigidBodyBasics pelvis = robotLeg.getRootJoint().getSuccessor();
      ReferenceFrame centerOfMassFrame = robotLeg.getReferenceFrames().getCenterOfMassFrame();

      // send in the correct frame with identity selection matrix
      FrameVector3D desiredForce = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector3D desiredTorque = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredTorque, desiredForce);

      // select only torque
      DMatrixRMaj selectionMatrix = new DMatrixRMaj(1, 6);
      selectionMatrix.set(0, 0, 1);

      submitAndCheckVMC(pelvis, foot, centerOfMassFrame, desiredWrench, selectionMatrix);
   }

   @Test
   public void testVMCSelectTorqueY()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      
      RobotLegs robotLeg = VirtualModelControllerTestHelper.createRobotLeg(gravity);
      RigidBodyBasics endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBodyBasics foot = endEffector.getParentJoint().getSuccessor();
      RigidBodyBasics pelvis = robotLeg.getRootJoint().getSuccessor();
      ReferenceFrame centerOfMassFrame = robotLeg.getReferenceFrames().getCenterOfMassFrame();

      // send in the correct frame with identity selection matrix
      FrameVector3D desiredForce = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector3D desiredTorque = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredTorque, desiredForce);

      // select only torque
      DMatrixRMaj selectionMatrix = new DMatrixRMaj(1, 6);
      selectionMatrix.set(0, 1, 1);

      submitAndCheckVMC(pelvis, foot, centerOfMassFrame, desiredWrench, selectionMatrix);
   }

   @Test
   public void testVMCSelectTorqueZ()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      
      RobotLegs robotLeg = VirtualModelControllerTestHelper.createRobotLeg(gravity);
      RigidBodyBasics endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBodyBasics foot = endEffector.getParentJoint().getSuccessor();
      RigidBodyBasics pelvis = robotLeg.getRootJoint().getSuccessor();
      ReferenceFrame centerOfMassFrame = robotLeg.getReferenceFrames().getCenterOfMassFrame();

      // send in the correct frame with identity selection matrix
      FrameVector3D desiredForce = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector3D desiredTorque = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredTorque, desiredForce);

      // select only torque
      DMatrixRMaj selectionMatrix = new DMatrixRMaj(1, 6);
      selectionMatrix.set(0, 2, 1);

      submitAndCheckVMC(pelvis, foot, centerOfMassFrame, desiredWrench, selectionMatrix);
   }

   @Test
   public void testVMCSelectForceXTorqueY()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      
      RobotLegs robotLeg = VirtualModelControllerTestHelper.createRobotLeg(gravity);
      RigidBodyBasics endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBodyBasics foot = endEffector.getParentJoint().getSuccessor();
      RigidBodyBasics pelvis = robotLeg.getRootJoint().getSuccessor();
      ReferenceFrame centerOfMassFrame = robotLeg.getReferenceFrames().getCenterOfMassFrame();

      // send in the correct frame with identity selection matrix
      FrameVector3D desiredForce = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector3D desiredTorque = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredTorque, desiredForce);

      // select only torque
      DMatrixRMaj selectionMatrix = new DMatrixRMaj(2, 6);
      selectionMatrix.set(0, 1, 1);
      selectionMatrix.set(1, 4, 1);

      submitAndCheckVMC(pelvis, foot, centerOfMassFrame, desiredWrench, selectionMatrix);
   }

   @Test
   public void testVMCSelectForceYZTorqueX()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      
      RobotLegs robotLeg = VirtualModelControllerTestHelper.createRobotLeg(gravity);
      RigidBodyBasics endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBodyBasics foot = endEffector.getParentJoint().getSuccessor();
      RigidBodyBasics pelvis = robotLeg.getRootJoint().getSuccessor();
      ReferenceFrame centerOfMassFrame = robotLeg.getReferenceFrames().getCenterOfMassFrame();

      // send in the correct frame with identity selection matrix
      FrameVector3D desiredForce = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector3D desiredTorque = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredTorque, desiredForce);

      // select only torque
      DMatrixRMaj selectionMatrix = new DMatrixRMaj(3, 6);
      selectionMatrix.set(0, 0, 1);
      selectionMatrix.set(1, 4, 1);
      selectionMatrix.set(2, 5, 1);

      submitAndCheckVMC(pelvis, foot, centerOfMassFrame, desiredWrench, selectionMatrix);
   }

   @Test
   public void testVMCSelectForceXTorqueXZ()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      
      RobotLegs robotLeg = VirtualModelControllerTestHelper.createRobotLeg(gravity);
      RigidBodyBasics endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBodyBasics foot = endEffector.getParentJoint().getSuccessor();
      RigidBodyBasics pelvis = robotLeg.getRootJoint().getSuccessor();
      ReferenceFrame centerOfMassFrame = robotLeg.getReferenceFrames().getCenterOfMassFrame();

      // send in the correct frame with identity selection matrix
      FrameVector3D desiredForce = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector3D desiredTorque = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredTorque, desiredForce);

      // select only torque
      DMatrixRMaj selectionMatrix = new DMatrixRMaj(3, 6);
      selectionMatrix.set(0, 0, 1);
      selectionMatrix.set(1, 2, 1);
      selectionMatrix.set(2, 3, 1);

      submitAndCheckVMC(pelvis, foot, centerOfMassFrame, desiredWrench, selectionMatrix);
   }

   @Test
   public void testVMCWrongExpressedInFrame()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      
      RobotLegs robotLeg = VirtualModelControllerTestHelper.createRobotLeg(gravity);
      RigidBodyBasics endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBodyBasics foot = endEffector.getParentJoint().getSuccessor();
      RigidBodyBasics pelvis = robotLeg.getRootJoint().getSuccessor();
      ReferenceFrame centerOfMassFrame = robotLeg.getReferenceFrames().getCenterOfMassFrame();

      // send in the correct frame with identity selection matrix
      FrameVector3D desiredForce = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector3D desiredTorque = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), pelvis.getBodyFixedFrame(), desiredTorque, desiredForce);

      // select only torque
      DMatrixRMaj selectionMatrix = new DMatrixRMaj(3, 6);
      selectionMatrix.set(0, 0, 1);
      selectionMatrix.set(1, 2, 1);
      selectionMatrix.set(2, 3, 1);

      submitAndCheckVMC(pelvis, foot, centerOfMassFrame, desiredWrench, selectionMatrix);
   }

   @Test
   public void testVMCWrongExpressedOnFrame()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      
      RobotLegs robotLeg = VirtualModelControllerTestHelper.createRobotLeg(gravity);
      RigidBodyBasics endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBodyBasics foot = endEffector.getParentJoint().getSuccessor();
      RigidBodyBasics pelvis = robotLeg.getRootJoint().getSuccessor();
      ReferenceFrame centerOfMassFrame = robotLeg.getReferenceFrames().getCenterOfMassFrame();

      // send in the correct frame with identity selection matrix
      FrameVector3D desiredForce = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector3D desiredTorque = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(pelvis.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredTorque, desiredForce);

      // select only torque
      DMatrixRMaj selectionMatrix = new DMatrixRMaj(3, 6);
      selectionMatrix.set(0, 0, 1);
      selectionMatrix.set(1, 2, 1);
      selectionMatrix.set(2, 3, 1);

      submitAndCheckVMC(pelvis, foot, centerOfMassFrame, desiredWrench, selectionMatrix);
   }

   @Test
   public void testVMCWrongExpressedInAndOnFrame()
   {
      double gravity = -9.81;

      
      RobotLegs robotLeg = VirtualModelControllerTestHelper.createRobotLeg(gravity);
      RigidBodyBasics endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBodyBasics foot = endEffector.getParentJoint().getSuccessor();
      RigidBodyBasics pelvis = robotLeg.getRootJoint().getSuccessor();
      ReferenceFrame centerOfMassFrame = robotLeg.getReferenceFrames().getCenterOfMassFrame();

      // send in the correct frame with identity selection matrix
      FrameVector3D desiredForce = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector3D desiredTorque = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(pelvis.getBodyFixedFrame(), pelvis.getBodyFixedFrame(), desiredTorque, desiredForce);

      // select only torque
      DMatrixRMaj selectionMatrix = new DMatrixRMaj(3, 6);
      selectionMatrix.set(0, 0, 1);
      selectionMatrix.set(1, 2, 1);
      selectionMatrix.set(2, 3, 1);

      submitAndCheckVMC(pelvis, foot, centerOfMassFrame, desiredWrench, selectionMatrix);
   }

   @Test
   public void testVMCVirtualWrenchCommand()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      
      VirtualModelControllerTestHelper.RobotLegs robotLeg = VirtualModelControllerTestHelper.createRobotLeg(gravity);
      RigidBodyBasics endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBodyBasics foot = endEffector.getParentJoint().getSuccessor();
      RigidBodyBasics pelvis = robotLeg.getRootJoint().getSuccessor();
      ReferenceFrame centerOfMassFrame = robotLeg.getReferenceFrames().getCenterOfMassFrame();

      // send in the correct frame with identity selection matrix
      FrameVector3D desiredForce = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector3D desiredTorque = new FrameVector3D(foot.getBodyFixedFrame(), new Vector3D(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredTorque, desiredForce);

      DMatrixRMaj selectionMatrix = CommonOps_DDRM.identity(Wrench.SIZE, Wrench.SIZE);

      JointBasics[] controlledJoints = MultiBodySystemTools.createJointPath(pelvis, endEffector);
      GeometricJacobian jacobian = new GeometricJacobian(controlledJoints, pelvis.getBodyFixedFrame());
      jacobian.compute();

      DMatrixRMaj jacobianMatrix = jacobian.getJacobianMatrix();
      DMatrixRMaj transposeJacobianMatrix = new DMatrixRMaj(jacobianMatrix.numCols, Wrench.SIZE);
      CommonOps_DDRM.transpose(jacobianMatrix, transposeJacobianMatrix);
      CommonOps_DDRM.invert(transposeJacobianMatrix);

      YoRegistry registry = new YoRegistry(this.getClass().getSimpleName());
      VirtualModelController virtualModelController = new VirtualModelController(pelvis, centerOfMassFrame, registry, null);
      virtualModelController.registerControlledBody(endEffector, pelvis);

      VirtualWrenchCommand virtualWrenchCommand = new VirtualWrenchCommand();
      virtualWrenchCommand.set(pelvis, endEffector);
      virtualWrenchCommand.setWrench(desiredWrench.getReferenceFrame(), desiredWrench);

      virtualModelController.submitControlledBodyVirtualWrench(virtualWrenchCommand);

      // find jacobian transpose solution
      VirtualModelControlSolution virtualModelControlSolution = new VirtualModelControlSolution();
      virtualModelController.compute(virtualModelControlSolution);

      desiredWrench.changeFrame(pelvis.getBodyFixedFrame());

      // compute end effector force from torques
      DMatrixRMaj jointEffortMatrix = virtualModelControlSolution.getJointTorques();

      DMatrixRMaj appliedWrenchMatrix = new DMatrixRMaj(Wrench.SIZE, 1);
      CommonOps_DDRM.mult(transposeJacobianMatrix, jointEffortMatrix, appliedWrenchMatrix);
      Wrench appliedWrench = new Wrench(endEffector.getBodyFixedFrame(), jacobian.getJacobianFrame(), appliedWrenchMatrix);

      VirtualModelControllerTestHelper.compareWrenches(desiredWrench, appliedWrench, selectionMatrix);
   }

   @Disabled
   // TODO GITHUB WORKFLOWS
   // This test has a hard crash
   // X Error of failed request:  BadWindow (invalid Window parameter)
   @Test
   public void testVMCWithArm() throws Exception
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      
      VirtualModelControllerTestHelper.RobotArm robotArm = VirtualModelControllerTestHelper.createRobotArm();
      SCSRobotFromInverseDynamicsRobotModel scsRobotArm = robotArm.getSCSRobotArm();

      List<RigidBodyBasics> endEffectors = new ArrayList<>();
      RigidBodyBasics hand = robotArm.getHand();
      endEffectors.add(hand);

      double forceX = random.nextDouble() * 10.0;
      double forceY = random.nextDouble() * 10.0;
      double forceZ = random.nextDouble() * 10.0;
      double torqueX = random.nextDouble() * 10.0;
      double torqueY = random.nextDouble() * 10.0;
      double torqueZ = random.nextDouble() * 10.0;
      Vector3D desiredForce = new Vector3D(forceX, forceY, forceZ);
      Vector3D desiredTorque = new Vector3D(torqueX, torqueY, torqueZ);

      List<Vector3D> desiredForces = new ArrayList<>();
      List<Vector3D> desiredTorques = new ArrayList<>();
      desiredForces.add(desiredForce);
      desiredTorques.add(desiredTorque);

      List<ExternalForcePoint> externalForcePoints = new ArrayList<>();
      externalForcePoints.add(robotArm.getExternalForcePoint());

      DMatrixRMaj selectionMatrix = CommonOps_DDRM.identity(Wrench.SIZE, Wrench.SIZE);
      VirtualModelControllerTestHelper.createVirtualModelControlTest(scsRobotArm, robotArm, robotArm.getCenterOfMassFrame(), endEffectors, desiredForces,
            desiredTorques, externalForcePoints, selectionMatrix, simulationTestingParameters);

      simulationTestingParameters.setKeepSCSUp(false);
   }

   @Disabled
   // TODO GITHUB WORKFLOWS
   // This test has a hard crash
   // X Error of failed request:  BadWindow (invalid Window parameter)
   @Test
   public void testVMCWithPlanarArm() throws Exception
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      
      VirtualModelControllerTestHelper.PlanarRobotArm robotArm = VirtualModelControllerTestHelper.createPlanarArm();
      SCSRobotFromInverseDynamicsRobotModel scsRobotArm = robotArm.getSCSRobotArm();

      List<RigidBodyBasics> endEffectors = new ArrayList<>();
      RigidBodyBasics hand = robotArm.getHand();
      endEffectors.add(hand);

      double forceX = random.nextDouble() * 10.0;
      double forceZ = random.nextDouble() * 10.0;
      double torqueY = random.nextDouble() * 10.0;
      Vector3D desiredForce = new Vector3D(forceX, 0.0, forceZ);
      Vector3D desiredTorque = new Vector3D(0.0, torqueY, 0.0);

      List<Vector3D> desiredForces = new ArrayList<>();
      List<Vector3D> desiredTorques = new ArrayList<>();
      desiredForces.add(desiredForce);
      desiredTorques.add(desiredTorque);

      List<ExternalForcePoint> externalForcePoints = new ArrayList<>();
      externalForcePoints.add(robotArm.getExternalForcePoint());

      DMatrixRMaj selectionMatrix = CommonOps_DDRM.identity(Wrench.SIZE, Wrench.SIZE);
      VirtualModelControllerTestHelper.createVirtualModelControlTest(scsRobotArm, robotArm, robotArm.getCenterOfMassFrame(), endEffectors, desiredForces, desiredTorques, externalForcePoints, selectionMatrix, simulationTestingParameters);

      simulationTestingParameters.setKeepSCSUp(false);
   }

   /*
   @Test
   public void testPlanarHydra() throws Exception
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      
      VirtualModelControllerTestHelper.PlanarForkedRobotArm robotArm = VirtualModelControllerTestHelper.createPlanarForkedRobotArm();
      SCSRobotFromInverseDynamicsRobotModel scsRobotArm = robotArm.getSCSRobotArm();

      List<RigidBody> endEffectors = new ArrayList<>();
      RigidBody leftHand = robotArm.getHand(RobotSide.LEFT);
      RigidBody rightHand = robotArm.getHand(RobotSide.RIGHT);
      endEffectors.add(leftHand);
      endEffectors.add(rightHand);

      double forceX = random.nextDouble() * 10.0;
      double forceZ = random.nextDouble() * 10.0;
      double torqueY = random.nextDouble() * 10.0;
      Vector3D desiredForce1 = new Vector3D(forceX, 0.0, forceZ);
      Vector3D desiredForce2 = new Vector3D(forceX, 0.0, forceZ);
      Vector3D desiredTorque1 = new Vector3D(0.0, torqueY, 0.0);
      Vector3D desiredTorque2 = new Vector3D(0.0, torqueY, 0.0);

      List<Vector3D> desiredForces = new ArrayList<>();
      List<Vector3D> desiredTorques = new ArrayList<>();
      desiredForces.add(desiredForce1);
      desiredForces.add(desiredForce2);
      desiredTorques.add(desiredTorque1);
      desiredTorques.add(desiredTorque2);

      SideDependentList<ExternalForcePoint> sideDependentExternalForcePoints = robotArm.getExternalForcePoints();
      List<ExternalForcePoint> externalForcePoints = new ArrayList<>();
      externalForcePoints.add(sideDependentExternalForcePoints.get(RobotSide.LEFT));
      externalForcePoints.add(sideDependentExternalForcePoints.get(RobotSide.RIGHT));

      DMatrixRMaj selectionMatrix = CommonOps_DDRM.identity(Wrench.SIZE, Wrench.SIZE);

      VirtualModelControllerTestHelper.createVirtualModelControlTest(scsRobotArm, robotArm, robotArm.getCenterOfMassFrame(), endEffectors, desiredForces,
            desiredTorques, externalForcePoints, selectionMatrix, simulationTestingParameters);

      simulationTestingParameters.setKeepSCSUp(false);
      CITools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
   }

   @Test
   public void testHydra() throws Exception
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      
      VirtualModelControllerTestHelper.ForkedRobotArm robotArm = VirtualModelControllerTestHelper.createForkedRobotArm();
      SCSRobotFromInverseDynamicsRobotModel scsRobotArm = robotArm.getSCSRobotArm();

      List<RigidBody> endEffectors = new ArrayList<>();
      RigidBody leftHand = robotArm.getHand(RobotSide.LEFT);
      RigidBody rightHand = robotArm.getHand(RobotSide.RIGHT);
      endEffectors.add(leftHand);
      endEffectors.add(rightHand);

      double forceZ = random.nextDouble() * 10.0;
      Vector3D desiredForce1 = new Vector3D(0.0, 0.0, forceZ);
      Vector3D desiredForce2 = new Vector3D(0.0, 0.0, forceZ);
      Vector3D desiredTorque1 = new Vector3D(0.0, 0.0, 0.0);
      Vector3D desiredTorque2 = new Vector3D(0.0, 0.0, 0.0);

      List<Vector3D> desiredForces = new ArrayList<>();
      List<Vector3D> desiredTorques = new ArrayList<>();
      desiredForces.add(desiredForce1);
      desiredForces.add(desiredForce2);
      desiredTorques.add(desiredTorque1);
      desiredTorques.add(desiredTorque2);

      SideDependentList<ExternalForcePoint> sideDependentExternalForcePoints = robotArm.getExternalForcePoints();
      List<ExternalForcePoint> externalForcePoints = new ArrayList<>();
      externalForcePoints.add(sideDependentExternalForcePoints.get(RobotSide.LEFT));
      externalForcePoints.add(sideDependentExternalForcePoints.get(RobotSide.RIGHT));

      DMatrixRMaj selectionMatrix = CommonOps_DDRM.identity(Wrench.SIZE, Wrench.SIZE);

      VirtualModelControllerTestHelper.createVirtualModelControlTest(scsRobotArm, robotArm, robotArm.getCenterOfMassFrame(), endEffectors, desiredForces,
            desiredTorques, externalForcePoints, selectionMatrix, simulationTestingParameters);

      simulationTestingParameters.setKeepSCSUp(false);
   }
   */

   @AfterEach
   public void destroySimulationAndRecycleMemory()
   {
      if (simulationTestingParameters.getKeepSCSUp() && hasSCSSimulation)
      {
         ThreadTools.sleepForever();
      }
      ReferenceFrameTools.clearWorldFrameTree();
   }

   private void submitAndCheckVMC(RigidBodyBasics base, RigidBodyBasics endEffector, ReferenceFrame centerOfMassFrame, Wrench desiredWrench, DMatrixRMaj selectionMatrix)
   {
      YoRegistry registry = new YoRegistry("robert");

      simulationTestingParameters.setKeepSCSUp(false);

      OneDoFJointBasics[] controlledJoints = MultiBodySystemTools.createOneDoFJointPath(base, endEffector);
      GeometricJacobian jacobian = new GeometricJacobian(controlledJoints, base.getBodyFixedFrame());
      jacobian.compute();

      DMatrixRMaj jacobianMatrix = jacobian.getJacobianMatrix();
      DMatrixRMaj transposeJacobianMatrix = new DMatrixRMaj(jacobianMatrix.numCols, Wrench.SIZE);
      CommonOps_DDRM.transpose(jacobianMatrix, transposeJacobianMatrix);
      CommonOps_DDRM.invert(transposeJacobianMatrix);

      VirtualModelController virtualModelController = new VirtualModelController(base, centerOfMassFrame, registry, null);
      virtualModelController.registerControlledBody(endEffector, base);

      desiredWrench.changeFrame(base.getBodyFixedFrame());

      if (selectionMatrix == null)
         virtualModelController.submitControlledBodyVirtualWrench(endEffector, desiredWrench);
      else
         virtualModelController.submitControlledBodyVirtualWrench(endEffector, desiredWrench, selectionMatrix);

      // find jacobian transpose solution
      VirtualModelControlSolution virtualModelControlSolution = new VirtualModelControlSolution();
      virtualModelController.compute(virtualModelControlSolution);

      // compute end effector force from torques
      DMatrixRMaj jointEffortMatrix = virtualModelControlSolution.getJointTorques();

      DMatrixRMaj appliedWrenchMatrix = new DMatrixRMaj(Wrench.SIZE, 1);
      CommonOps_DDRM.mult(transposeJacobianMatrix, jointEffortMatrix, appliedWrenchMatrix);
      Wrench appliedWrench = new Wrench(endEffector.getBodyFixedFrame(), jacobian.getJacobianFrame(), appliedWrenchMatrix);

      if (selectionMatrix == null)
         VirtualModelControllerTestHelper.compareWrenches(desiredWrench, appliedWrench);
      else
         VirtualModelControllerTestHelper.compareWrenches(desiredWrench, appliedWrench, selectionMatrix);
   }
}
