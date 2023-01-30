package us.ihmc.behaviors.tools;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import us.ihmc.avatar.drcRobot.ROS2SyncedRobotModel;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.mecano.algorithms.GeometricJacobianCalculator;
import us.ihmc.mecano.multiBodySystem.interfaces.JointReadOnly;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointReadOnly;
import us.ihmc.mecano.spatial.SpatialVector;
import us.ihmc.mecano.spatial.Wrench;
import us.ihmc.mecano.tools.MultiBodySystemTools;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.functionApproximation.DampedLeastSquaresSolver;
import us.ihmc.robotics.partNames.ArmJointName;
import us.ihmc.robotics.partNames.HumanoidJointNameMap;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;

import java.util.List;

public class HandWrenchCalculator
{
   private FullHumanoidRobotModel fullRobotModel;
   HumanoidJointNameMap jointNameMap;
   private final GeometricJacobianCalculator jacobianCalculator = new GeometricJacobianCalculator();
   private SideDependentList<DMatrixRMaj> armJacobianMatrix = new SideDependentList<>();
   private SideDependentList<List<OneDoFJointBasics>> armJoints = new SideDependentList<>();
   private SideDependentList<ReferenceFrame> referenceFrame = new SideDependentList<>();
   private SideDependentList<SpatialVector> wrenches = new SideDependentList<>(new SpatialVector(), new SpatialVector());

   public HandWrenchCalculator(ROS2SyncedRobotModel syncedRobot)
   {
      fullRobotModel = syncedRobot.getFullRobotModel();
      jointNameMap = syncedRobot.getRobotModel().getJointMap();
   }

   private void updateJacobians()
   {
      for (RobotSide side : RobotSide.values)
      {
         jacobianCalculator.setKinematicChain(fullRobotModel.getChest(), fullRobotModel.getHand(side));
         referenceFrame.set(side, jacobianCalculator.getJacobianFrame());
         armJacobianMatrix.set(side, jacobianCalculator.getJacobianMatrix());
         List<OneDoFJointBasics> oneDoFJoints = MultiBodySystemTools.filterJoints(jacobianCalculator.getJointsFromBaseToEndEffector(), OneDoFJointBasics.class);
         armJoints.set(side, oneDoFJoints);
      }
   }

   private DMatrixRMaj leftPseudoInverse(DMatrixRMaj matrix)
   {
      // PREVIOUSLY . . . >>>
      /*
      double lambda = 1e-6;

      DMatrixRMaj matrixTransposed = CommonOps_DDRM.transpose(matrix, null);
      int numRow = matrixTransposed.getNumRows();

      DMatrixRMaj AT_A = new DMatrixRMaj(numRow, numRow);
      CommonOps_DDRM.mult(matrixTransposed, matrix, AT_A);

      DMatrixRMaj identity = CommonOps_DDRM.identity(numRow);
      CommonOps_DDRM.scale(lambda, identity);

      DMatrixRMaj result = new DMatrixRMaj(identity.getNumRows(),identity.getNumRows());
      CommonOps_DDRM.add(AT_A, identity, result);

      CommonOps_DDRM.invert(result);

      DMatrixRMaj matrixDagger = new DMatrixRMaj(result.getNumRows(),matrixTransposed.getNumCols());
      CommonOps_DDRM.mult(result, matrixTransposed, matrixDagger);

      return matrixDagger;

       */
      // <<< PREVIOUSLY . . .

      // TODO: check this works the same
      DampedLeastSquaresSolver pseudoInverseSolver = new DampedLeastSquaresSolver(matrix.getNumRows(), 1e-6);
      pseudoInverseSolver.setA(matrix);
      DMatrixRMaj leftPseudoInverseOfMatrix = new DMatrixRMaj(matrix);
      pseudoInverseSolver.invert(leftPseudoInverseOfMatrix);
      return leftPseudoInverseOfMatrix;
   }

   private void calculateTaskForces()
   {
      for (RobotSide side : RobotSide.values)
      {
         List<OneDoFJointBasics> oneSideArmJoints = armJoints.get(side);
         double[] jointTorques = new double[oneSideArmJoints.size()];
         for (int i = 0; i < oneSideArmJoints.size(); ++i)
         {
            jointTorques[i] = oneSideArmJoints.get(i).getTau();
         }

         DMatrixRMaj armJacobian = armJacobianMatrix.get(side);
         DMatrixRMaj armJacobianTransposed = CommonOps_DDRM.transpose(armJacobian, null);
         DMatrixRMaj armJacobianTransposedDagger = leftPseudoInverse(armJacobianTransposed);
         DMatrixRMaj jointTorqueVector = new DMatrixRMaj(jointTorques);
         DMatrixRMaj wrenchVector = new DMatrixRMaj(6,1);
         CommonOps_DDRM.mult(armJacobianTransposedDagger, jointTorqueVector, wrenchVector);

         wrenches.set(side, makeWrench(wrenchVector));
      }
   }

   // Wrench expressed in world-aligned frame
   private static SpatialVector makeWrench(DMatrixRMaj wrenchVector)
   {
      // Linear and angular part into spatial vector
      SpatialVector spatialVector = new SpatialVector();
      spatialVector.set(wrenchVector);
      // Express in world-frame
      spatialVector.changeFrame(ReferenceFrame.getWorldFrame());

      return spatialVector;
   }

   public void update()
   {
      updateJacobians();
      calculateTaskForces();
   }

   public SideDependentList<SpatialVector> getWrench()
   {
      return wrenches;
   }

   public SideDependentList<ReferenceFrame> getReferenceFrame()
   {
      return referenceFrame;
   }

   // expressed in world-aligned frame
   public FrameVector3D getWrenchLinear(RobotSide side)
   {
      return new FrameVector3D(wrenches.get(side).getLinearPart().getReferenceFrame(), wrenches.get(side).getLinearPart());
   }

   // expressed in world-aligned frame
   public FrameVector3D getWrenchAngular(RobotSide side)
   {
      return new FrameVector3D(wrenches.get(side).getAngularPart().getReferenceFrame(), wrenches.get(side).getAngularPart());
   }
}