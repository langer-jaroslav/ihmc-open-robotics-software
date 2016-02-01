package object_manipulation_msgs;

public interface PickupAction extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "object_manipulation_msgs/PickupAction";
  static final java.lang.String _DEFINITION = "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\n\nPickupActionGoal action_goal\nPickupActionResult action_result\nPickupActionFeedback action_feedback\n";
  object_manipulation_msgs.PickupActionGoal getActionGoal();
  void setActionGoal(object_manipulation_msgs.PickupActionGoal value);
  object_manipulation_msgs.PickupActionResult getActionResult();
  void setActionResult(object_manipulation_msgs.PickupActionResult value);
  object_manipulation_msgs.PickupActionFeedback getActionFeedback();
  void setActionFeedback(object_manipulation_msgs.PickupActionFeedback value);
}
