package actionlib;

public interface TestRequestGoal extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "actionlib/TestRequestGoal";
  static final java.lang.String _DEFINITION = "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\nint32 TERMINATE_SUCCESS = 0\nint32 TERMINATE_ABORTED = 1\nint32 TERMINATE_REJECTED = 2\nint32 TERMINATE_LOSE = 3\nint32 TERMINATE_DROP = 4\nint32 TERMINATE_EXCEPTION = 5\nint32 terminate_status\nbool ignore_cancel  # If true, ignores requests to cancel\nstring result_text\nint32 the_result    # Desired value for the_result in the Result\nbool is_simple_client\nduration delay_accept  # Delays accepting the goal by this amount of time\nduration delay_terminate  # Delays terminating for this amount of time\nduration pause_status  # Pauses the status messages for this amount of time\n";
  static final int TERMINATE_SUCCESS = 0;
  static final int TERMINATE_ABORTED = 1;
  static final int TERMINATE_REJECTED = 2;
  static final int TERMINATE_LOSE = 3;
  static final int TERMINATE_DROP = 4;
  static final int TERMINATE_EXCEPTION = 5;
  int getTerminateStatus();
  void setTerminateStatus(int value);
  boolean getIgnoreCancel();
  void setIgnoreCancel(boolean value);
  java.lang.String getResultText();
  void setResultText(java.lang.String value);
  int getTheResult();
  void setTheResult(int value);
  boolean getIsSimpleClient();
  void setIsSimpleClient(boolean value);
  org.ros.message.Duration getDelayAccept();
  void setDelayAccept(org.ros.message.Duration value);
  org.ros.message.Duration getDelayTerminate();
  void setDelayTerminate(org.ros.message.Duration value);
  org.ros.message.Duration getPauseStatus();
  void setPauseStatus(org.ros.message.Duration value);
}
