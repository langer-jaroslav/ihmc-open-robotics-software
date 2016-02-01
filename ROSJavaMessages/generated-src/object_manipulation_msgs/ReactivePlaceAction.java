package object_manipulation_msgs;

public interface ReactivePlaceAction extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "object_manipulation_msgs/ReactivePlaceAction";
  static final java.lang.String _DEFINITION = "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\n\nReactivePlaceActionGoal action_goal\nReactivePlaceActionResult action_result\nReactivePlaceActionFeedback action_feedback\n";
  object_manipulation_msgs.ReactivePlaceActionGoal getActionGoal();
  void setActionGoal(object_manipulation_msgs.ReactivePlaceActionGoal value);
  object_manipulation_msgs.ReactivePlaceActionResult getActionResult();
  void setActionResult(object_manipulation_msgs.ReactivePlaceActionResult value);
  object_manipulation_msgs.ReactivePlaceActionFeedback getActionFeedback();
  void setActionFeedback(object_manipulation_msgs.ReactivePlaceActionFeedback value);
}
