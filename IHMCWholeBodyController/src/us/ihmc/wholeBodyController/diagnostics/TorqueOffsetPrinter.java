package us.ihmc.wholeBodyController.diagnostics;

public interface TorqueOffsetPrinter
{
   public abstract void printTorqueOffsets(JointTorqueOffsetEstimator jointTorqueOffsetEstimator);
}
