package us.ihmc.behaviors.activeMapping;

import us.ihmc.tools.property.StoredPropertySetBasics;

/**
 * This class was auto generated. Do not edit by hand. Edit the cooresponding JSON file
 * and run the main in super to regenerate.
 */
public interface ContinuousHikingParametersBasics extends ContinuousHikingParametersReadOnly, StoredPropertySetBasics
{
   default void setEnableContinuousHiking(boolean enableContinuousHiking)
   {
      set(ContinuousHikingParameters.enableContinuousHiking, enableContinuousHiking);
   }

   default void setStepPublisherEnabled(boolean stepPublisherEnabled)
   {
      set(ContinuousHikingParameters.stepPublisherEnabled, stepPublisherEnabled);
   }

   default void setOverrideEntireQueueEachStep(boolean overrideEntireQueueEachStep)
   {
      set(ContinuousHikingParameters.overrideEntireQueueEachStep, overrideEntireQueueEachStep);
   }

   default void setNumberOfStepsToSend(int numberOfStepsToSend)
   {
      set(ContinuousHikingParameters.numberOfStepsToSend, numberOfStepsToSend);
   }

   default void setGoalPoseForwardDistance(double goalPoseForwardDistance)
   {
      set(ContinuousHikingParameters.goalPoseForwardDistance, goalPoseForwardDistance);
   }

   default void setGoalPoseUpDistance(double goalPoseUpDistance)
   {
      set(ContinuousHikingParameters.goalPoseUpDistance, goalPoseUpDistance);
   }

   default void setSwingTime(double swingTime)
   {
      set(ContinuousHikingParameters.swingTime, swingTime);
   }

   default void setTransferTime(double transferTime)
   {
      set(ContinuousHikingParameters.transferTime, transferTime);
   }

   default void setPlannerTimeoutFraction(double plannerTimeoutFraction)
   {
      set(ContinuousHikingParameters.plannerTimeoutFraction, plannerTimeoutFraction);
   }

   default void setPlanningWithoutReferenceTimeout(double planningWithoutReferenceTimeout)
   {
      set(ContinuousHikingParameters.planningWithoutReferenceTimeout, planningWithoutReferenceTimeout);
   }

   default void setPercentThroughSwingToPlanTo(double percentThroughSwingToPlanTo)
   {
      set(ContinuousHikingParameters.percentThroughSwingToPlanTo, percentThroughSwingToPlanTo);
   }

   default void setLogFootstepPlans(boolean logFootstepPlans)
   {
      set(ContinuousHikingParameters.logFootstepPlans, logFootstepPlans);
   }

   default void setNextWaypointDistanceMargin(double nextWaypointDistanceMargin)
   {
      set(ContinuousHikingParameters.nextWaypointDistanceMargin, nextWaypointDistanceMargin);
   }
}
