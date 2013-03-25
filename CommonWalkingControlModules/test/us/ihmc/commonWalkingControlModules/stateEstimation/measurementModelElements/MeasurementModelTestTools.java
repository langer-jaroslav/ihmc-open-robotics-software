package us.ihmc.commonWalkingControlModules.stateEstimation.measurementModelElements;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.EjmlUnitTests;

import us.ihmc.commonWalkingControlModules.stateEstimation.MeasurementModelElement;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.geometry.Direction;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.RotationFunctions;

public class MeasurementModelTestTools
{
   public static void assertDeltaResidualCorrect(MeasurementModelElement modelElement, DenseMatrix64F outputMatrixBlock, DenseMatrix64F perturbationEjmlVector,
           double tol)
   {
      DenseMatrix64F residual = modelElement.computeResidual();
      DenseMatrix64F residualFromOutputMatrix = computeDeltaResidualFromOutputMatrix(outputMatrixBlock, perturbationEjmlVector);
      EjmlUnitTests.assertEquals(residual, residualFromOutputMatrix, tol);
   }

   public static DenseMatrix64F computeDeltaResidualFromOutputMatrix(DenseMatrix64F biasOutputMatrixBlock, DenseMatrix64F perturbationEjmlVector)
   {
      DenseMatrix64F residualFromOutputMatrix = new DenseMatrix64F(3, 1);
      CommonOps.mult(biasOutputMatrixBlock, perturbationEjmlVector, residualFromOutputMatrix);
      CommonOps.scale(-1.0, residualFromOutputMatrix);

      return residualFromOutputMatrix;
   }

   public static void assertOutputMatrixCorrectUsingPerturbation(ControlFlowOutputPort<FrameVector> statePort, MeasurementModelElement modelElement,
           FrameVector nominalState, double perturbationMagnitude, double tolerance, Runnable runnable)
   {
      DenseMatrix64F outputMatrixBlock = modelElement.getOutputMatrixBlock(statePort);
      for (Direction direction : Direction.values())
      {
         FrameVector perturbationVector = new FrameVector(nominalState.getReferenceFrame());
         perturbationVector.set(direction, perturbationMagnitude);

         DenseMatrix64F perturbationEjmlVector = new DenseMatrix64F(3, 1);
         MatrixTools.setDenseMatrixFromTuple3d(perturbationEjmlVector, perturbationVector.getVector(), 0, 0);

         FrameVector perturbedState = new FrameVector(nominalState);
         perturbedState.add(perturbationVector);
         statePort.setData(perturbedState);

         if (runnable != null)
            runnable.run();

         MeasurementModelTestTools.assertDeltaResidualCorrect(modelElement, outputMatrixBlock, perturbationEjmlVector, tolerance);
      }

      statePort.setData(nominalState);
   }

   public static void assertOutputMatrixCorrectUsingPerturbation(ControlFlowOutputPort<FrameOrientation> statePort, MeasurementModelElement modelElement,
           FrameOrientation nominalState, double perturbationMagnitude, double tolerance, Runnable runnable)
   {
      DenseMatrix64F outputMatrixBlock = modelElement.getOutputMatrixBlock(statePort);
      for (Direction direction : Direction.values())
      {
         Vector3d perturbationRotationVector = new Vector3d();
         MathTools.set(perturbationRotationVector, direction, perturbationMagnitude);

         DenseMatrix64F perturbationEjmlVector = new DenseMatrix64F(3, 1);
         MatrixTools.setDenseMatrixFromTuple3d(perturbationEjmlVector, perturbationRotationVector, 0, 0);

         Quat4d perturbedQuaternion = new Quat4d();
         AxisAngle4d perturbationAxisAngle = new AxisAngle4d();
         RotationFunctions.setAxisAngleBasedOnRotationVector(perturbationAxisAngle, perturbationRotationVector);
         Quat4d perturbationQuaternion = new Quat4d();
         perturbationQuaternion.set(perturbationAxisAngle);
         perturbedQuaternion.mul(perturbationQuaternion);
         FrameOrientation perturbedState = new FrameOrientation(nominalState.getReferenceFrame(), perturbedQuaternion);
         statePort.setData(perturbedState);

         if (runnable != null)
            runnable.run();

         MeasurementModelTestTools.assertDeltaResidualCorrect(modelElement, outputMatrixBlock, perturbationEjmlVector, tolerance);
      }

      statePort.setData(nominalState);
   }
}
