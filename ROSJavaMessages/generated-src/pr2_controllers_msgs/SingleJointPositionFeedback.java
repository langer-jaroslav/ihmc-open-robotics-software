package pr2_controllers_msgs;

public interface SingleJointPositionFeedback extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "pr2_controllers_msgs/SingleJointPositionFeedback";
  static final java.lang.String _DEFINITION = "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\nHeader header\nfloat64 position\nfloat64 velocity\nfloat64 error\n\n";
  std_msgs.Header getHeader();
  void setHeader(std_msgs.Header value);
  double getPosition();
  void setPosition(double value);
  double getVelocity();
  void setVelocity(double value);
  double getError();
  void setError(double value);
}
