package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization;

public interface ICPOptimizationParameters
{
   public int getMaximumNumberOfFootstepsToConsider();

   public int numberOfFootstepsToConsider();

   public double getFootstepWeight();

   public double getFootstepRegularizationWeight();

   public double getFeedbackWeight();

   public double getFeedbackRegularizationWeight();

   public double getFeedbackParallelGain();

   public double getFeedbackOrthogonalGain();

   public double getDynamicRelaxationWeight();

   public double getDynamicRelaxationDoubleSupportWeightModifier();

   public boolean scaleStepRegularizationWeightWithTime();

   public boolean scaleFeedbackWeightWithGain();

   public boolean scaleUpcomingStepWeights();

   public boolean useFeedback();

   public boolean useFeedbackRegularization();

   public boolean useStepAdjustment();

   public boolean useFootstepRegularization();

   public boolean useFeedbackWeightHardening();

   public boolean useICPFromBeginningOfState();

   public double getMinimumFootstepWeight();

   public double getMinimumFeedbackWeight();

   public double getMinimumTimeRemaining();

   public double getFeedbackWeightHardeningMultiplier();

   public double getMaxCMPForwardExit();

   public double getMaxCMPLateralExit();

   public double getForwardAdjustmentDeadband();

   public double getLateralAdjustmentDeadband();

   public double getRemainingTimeToStopAdjusting();
}
