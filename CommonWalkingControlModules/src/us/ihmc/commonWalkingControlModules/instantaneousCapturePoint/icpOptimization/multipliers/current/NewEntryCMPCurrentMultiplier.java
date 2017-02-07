package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.current;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.stateMatrices.swing.NewSwingEntryCMPMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.stateMatrices.transfer.NewTransferEntryCMPMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.interpolation.CubicDerivativeMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.interpolation.CubicMatrix;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;

import java.util.ArrayList;

public class NewEntryCMPCurrentMultiplier
{
   private final CubicMatrix cubicMatrix;
   private final CubicDerivativeMatrix cubicDerivativeMatrix;

   private final NewTransferEntryCMPMatrix transferEntryCMPMatrix;
   private final NewSwingEntryCMPMatrix swingEntryCMPMatrix;

   public final DoubleYoVariable exitCMPRatio;
   public final DoubleYoVariable defaultDoubleSupportSplitRatio;

   public final DoubleYoVariable startOfSplineTime;
   public final DoubleYoVariable endOfSplineTime;
   public final DoubleYoVariable totalTrajectoryTime;

   private final DenseMatrix64F matrixOut = new DenseMatrix64F(1, 1);

   private final DoubleYoVariable positionMultiplier;
   private final DoubleYoVariable velocityMultiplier;

   private final boolean projectForward;

   public NewEntryCMPCurrentMultiplier(DoubleYoVariable defaultDoubleSupportSplitRatio, DoubleYoVariable exitCMPRatio, DoubleYoVariable startOfSplineTime,
         DoubleYoVariable endOfSplineTime, DoubleYoVariable totalTrajectoryTime, boolean projectForward, YoVariableRegistry registry)
   {
      positionMultiplier = new DoubleYoVariable("EntryCMPCurrentMultiplier", registry);
      velocityMultiplier = new DoubleYoVariable("EntryCMPCurrentVelocityMultiplier", registry);

      this.exitCMPRatio = exitCMPRatio;
      this.defaultDoubleSupportSplitRatio = defaultDoubleSupportSplitRatio;

      this.startOfSplineTime = startOfSplineTime;
      this.endOfSplineTime = endOfSplineTime;
      this.totalTrajectoryTime = totalTrajectoryTime;

      this.projectForward = projectForward;

      cubicMatrix = new CubicMatrix();
      cubicDerivativeMatrix = new CubicDerivativeMatrix();

      transferEntryCMPMatrix = new NewTransferEntryCMPMatrix(defaultDoubleSupportSplitRatio);
      swingEntryCMPMatrix = new NewSwingEntryCMPMatrix(startOfSplineTime);
   }

   public void reset()
   {
      positionMultiplier.set(0.0);
      velocityMultiplier.set(0.0);
   }

   public double getPositionMultiplier()
   {
      return positionMultiplier.getDoubleValue();
   }

   public double getVelocityMultiplier()
   {
      return velocityMultiplier.getDoubleValue();
   }

   public void compute(ArrayList<DoubleYoVariable> doubleSupportDurations, ArrayList<DoubleYoVariable> singleSupportDurations, double timeRemaining,
         boolean useTwoCMPs, boolean isInTransfer, double omega0)
   {
      double positionMultiplier, velocityMultiplier;
      if (isInTransfer)
      {
         positionMultiplier = computeInTransfer(doubleSupportDurations, timeRemaining, useTwoCMPs, omega0);
      }
      else
      {
         if (useTwoCMPs)
            positionMultiplier = computeSegmentedSwing(doubleSupportDurations, singleSupportDurations, timeRemaining, omega0);
         else
            positionMultiplier = computeInSwingOneCMP();
      }
      this.positionMultiplier.set(positionMultiplier);

      if (isInTransfer)
      {
         velocityMultiplier = computeInTransferVelocity();
      }
      else
      {
         if (useTwoCMPs)
            velocityMultiplier = computeSegmentedSwingVelocity(timeRemaining, omega0);
         else
            velocityMultiplier = computeInSwingOneCMPVelocity();
      }

      this.velocityMultiplier.set(velocityMultiplier);
   }




   private double computeInTransfer(ArrayList<DoubleYoVariable> doubleSupportDurations, double timeRemaining, boolean useTwoCMPs, double omega0)
   {
      transferEntryCMPMatrix.compute(doubleSupportDurations, useTwoCMPs, omega0);

      double splineDuration = doubleSupportDurations.get(0).getDoubleValue();

      cubicDerivativeMatrix.setSegmentDuration(splineDuration);
      cubicDerivativeMatrix.update(timeRemaining);
      cubicMatrix.setSegmentDuration(splineDuration);
      cubicMatrix.update(timeRemaining);

      CommonOps.mult(cubicMatrix, transferEntryCMPMatrix, matrixOut);

      return matrixOut.get(0, 0);
   }

   private double computeInTransferVelocity()
   {
      CommonOps.mult(cubicDerivativeMatrix, transferEntryCMPMatrix, matrixOut);

      return matrixOut.get(0, 0);
   }



   private double computeInSwingOneCMP()
   {
      return 0.0;
   }

   private double computeInSwingOneCMPVelocity()
   {
      return 0.0;
   }




   private double computeSegmentedSwing(ArrayList<DoubleYoVariable> doubleSupportDurations, ArrayList<DoubleYoVariable> singleSupportDurations,
         double timeRemaining, double omega0)
   {
      double timeInState = totalTrajectoryTime.getDoubleValue() - timeRemaining;

      if (timeInState < startOfSplineTime.getDoubleValue())
         return computeSwingFirstSegment(doubleSupportDurations, singleSupportDurations, timeInState, omega0);
      else if (timeInState >= endOfSplineTime.getDoubleValue())
         return computeSwingThirdSegment();
      else
         return computeSwingSecondSegment(timeRemaining, omega0);
   }




   private double computeSwingFirstSegment(ArrayList<DoubleYoVariable> doubleSupportDurations, ArrayList<DoubleYoVariable> singleSupportDurations,
         double timeInState, double omega0)
   {
      if (!projectForward)
      {
         double doubleSupportDuration = doubleSupportDurations.get(0).getDoubleValue();
         double singleSupportDuration = singleSupportDurations.get(0).getDoubleValue();
         double stepDuration = doubleSupportDuration + singleSupportDuration;

         double timeSpentOnEntryCMP = (1.0 - exitCMPRatio.getDoubleValue()) * stepDuration;
         double endOfDoubleSupportDuration = (1.0 - defaultDoubleSupportSplitRatio.getDoubleValue()) * doubleSupportDuration;
         double initialSingleSupportDuration = timeSpentOnEntryCMP - endOfDoubleSupportDuration;

         double projectionTime = timeInState - initialSingleSupportDuration;

         return 1.0 - Math.exp(omega0 * projectionTime);
      }
      else
      {
         return 1.0 - Math.exp(omega0 * timeInState);
      }
   }

   private double computeSwingSecondSegment(double timeRemaining, double omega0)
   {
      swingEntryCMPMatrix.compute(omega0);

      double lastSegmentDuration = totalTrajectoryTime.getDoubleValue() - endOfSplineTime.getDoubleValue();
      double timeRemainingInSpline = timeRemaining - lastSegmentDuration;
      double splineDuration = endOfSplineTime.getDoubleValue() - startOfSplineTime.getDoubleValue();

      cubicMatrix.setSegmentDuration(splineDuration);
      cubicMatrix.update(timeRemainingInSpline);

      cubicDerivativeMatrix.setSegmentDuration(splineDuration);
      cubicDerivativeMatrix.update(timeRemainingInSpline);

      CommonOps.mult(cubicMatrix, swingEntryCMPMatrix, matrixOut);

      return matrixOut.get(0, 0);
   }

   private double computeSwingThirdSegment()
   {
      return computeInSwingOneCMP();
   }




   private double computeSegmentedSwingVelocity(double timeRemaining, double omega0)
   {
      double timeInState = totalTrajectoryTime.getDoubleValue() - timeRemaining;

      if (timeInState < startOfSplineTime.getDoubleValue())
         return computeSwingFirstSegmentVelocity(omega0);
      else if (timeInState >= endOfSplineTime.getDoubleValue())
         return computeSwingThirdSegmentVelocity(omega0);
      else
         return computeSwingSecondSegmentVelocity();
   }




   private double computeSwingFirstSegmentVelocity(double omega0)
   {
      return omega0 * (positionMultiplier.getDoubleValue() - 1.0);
   }

   private double computeSwingSecondSegmentVelocity()
   {
      CommonOps.mult(cubicDerivativeMatrix, swingEntryCMPMatrix, matrixOut);

      return matrixOut.get(0, 0);
   }

   private double computeSwingThirdSegmentVelocity(double omega0)
   {
      return omega0 * computeInSwingOneCMPVelocity();
   }
}
