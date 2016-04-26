package ihmc_msgs;

public interface PelvisHeightTrajectoryRosMessage extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "ihmc_msgs/PelvisHeightTrajectoryRosMessage";
  static final java.lang.String _DEFINITION = "## PelvisHeightTrajectoryRosMessage\n# This mesage commands the controller to move the pelvis to a new height in world while going through the specified trajectory points. Sending this command will not affect the pelvis horizontal position. To control the pelvis 3D position use the PelvisTrajectoryMessage instead. A third order polynomial is used to interpolate between trajectory points. A message with a unique id equals to 0 will be interpreted as invalid and will not be processed by the controller. This rule does not apply to the fields of this message.\n\n# List of trajectory points to go through while executing the trajectory.\nTrajectoryPoint1DRosMessage[] trajectory_points\n\n# A unique id for the current message. This can be a timestamp or sequence number.\n# Only the unique id in the top level message is used, the unique id in nested messages is ignored.\n# Use /output/last_received_message for feedback about when the last message was received.\n# A message with a unique id equals to 0 will be interpreted as invalid and will not be processed by the controller.\nint64 unique_id\n\n\n";
  java.util.List<ihmc_msgs.TrajectoryPoint1DRosMessage> getTrajectoryPoints();
  void setTrajectoryPoints(java.util.List<ihmc_msgs.TrajectoryPoint1DRosMessage> value);
  long getUniqueId();
  void setUniqueId(long value);
}
