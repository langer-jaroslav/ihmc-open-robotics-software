package head_monitor_msgs;

public interface HeadMonitorGoal extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "head_monitor_msgs/HeadMonitorGoal";
  static final java.lang.String _DEFINITION = "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\nstring group_name\ntrajectory_msgs/JointTrajectory joint_trajectory\n\n#goal definition\nduration time_offset\nstring target_link\n";
  java.lang.String getGroupName();
  void setGroupName(java.lang.String value);
  trajectory_msgs.JointTrajectory getJointTrajectory();
  void setJointTrajectory(trajectory_msgs.JointTrajectory value);
  org.ros.message.Duration getTimeOffset();
  void setTimeOffset(org.ros.message.Duration value);
  java.lang.String getTargetLink();
  void setTargetLink(java.lang.String value);
}
