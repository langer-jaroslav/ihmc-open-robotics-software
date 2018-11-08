package us.ihmc.sensorProcessing.diagnostic;

import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.sensorProcessing.outputData.JointDesiredOutputReadOnly;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class OneDoFJointForceTrackingDelayEstimator implements DiagnosticUpdatable
{
   private final YoVariableRegistry registry;
   private final OneDoFJointBasics joint;
   private final JointDesiredOutputReadOnly output;
   private final DelayEstimatorBetweenTwoSignals delayEstimator;

   public OneDoFJointForceTrackingDelayEstimator(OneDoFJointBasics joint, JointDesiredOutputReadOnly outputDataToCheck, double dt, YoVariableRegistry parentRegistry)
   {
      this.joint = joint;
      this.output = outputDataToCheck;
      String jointName = joint.getName();
      registry = new YoVariableRegistry(jointName + "ForceTrackingDelayEstimator");
      delayEstimator = new DelayEstimatorBetweenTwoSignals(jointName + "ForceTracking", dt, registry);
   }

   @Override
   public void enable()
   {
      delayEstimator.enable();
   }

   @Override
   public void disable()
   {
      delayEstimator.disable();
   }

   @Override
   public void update()
   {
      delayEstimator.update(output.getDesiredTorque(), joint.getTau());
   }

   public void setAlphaFilterBreakFrequency(double delayEstimatorFilterBreakFrequency)
   {
      delayEstimator.setAlphaFilterBreakFrequency(delayEstimatorFilterBreakFrequency);
   }

   public void setEstimationParameters(double maxAbsoluteLead, double maxAbsoluteLag, double observationWindow)
   {
      delayEstimator.setEstimationParameters(maxAbsoluteLead, maxAbsoluteLag, observationWindow);
   }

   public boolean isEstimatingDelay()
   {
      return delayEstimator.isEstimatingDelay();
   }

   public double getEstimatedDelay()
   {
      return delayEstimator.getEstimatedDelay();
   }

   public double getCorrelation()
   {
      return delayEstimator.getCorrelationCoefficient();
   }
}
