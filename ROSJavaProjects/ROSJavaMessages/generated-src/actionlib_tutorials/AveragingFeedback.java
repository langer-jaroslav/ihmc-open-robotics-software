package actionlib_tutorials;

public interface AveragingFeedback extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "actionlib_tutorials/AveragingFeedback";
  static final java.lang.String _DEFINITION = "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\n#feedback\nint32 sample\nfloat32 data\nfloat32 mean\nfloat32 std_dev\n\n";
  int getSample();
  void setSample(int value);
  float getData();
  void setData(float value);
  float getMean();
  void setMean(float value);
  float getStdDev();
  void setStdDev(float value);
}
