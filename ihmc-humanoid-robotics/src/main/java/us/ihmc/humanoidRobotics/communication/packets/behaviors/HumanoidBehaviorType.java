package us.ihmc.humanoidRobotics.communication.packets.behaviors;

public enum HumanoidBehaviorType
{
   STOP,
   TEST,
   BASIC_TIMER_BEHAVIOR,
   ROUGH_TERRAIN_OPERATOR_TIMING_BEHAVIOR,
   WALK_THROUGH_DOOR,
   WALK_THROUGH_DOOR_OPERATOR_TIMING_BEHAVIOR,
   WALK_TO_LOCATION,
   WALK_TO_GOAL,
   DIAGNOSTIC,
   PICK_UP_BALL,
   RESET_ROBOT,
   TURN_VALVE,
   EXAMPLE_BEHAVIOR,
   BALL_DETECTION,
   TEST_PIPELINE,
   TEST_STATEMACHINE,
   FOLLOW_FIDUCIAL_50,
   LOCATE_FIDUCIAL,
   WALK_OVER_TERRAIN,
   FOLLOW_VALVE,
   LOCATE_VALVE,
   WALK_OVER_TERRAIN_TO_VALVE,
   DEBUG_PARTIAL_FOOTHOLDS,
   WALK_TO_GOAL_ANYTIME_PLANNER,
   TEST_ICP_OPTIMIZATION,
   TEST_GC_GENERATION,
   TEST_SMOOTH_ICP_PLANNER,
   @Deprecated
   PUSH_AND_WALK,
   COLLABORATIVE_TASK,
   FIRE_FIGHTING,   
   CUTTING_WALL,
   REPEATEDLY_WALK_FOOTSTEP_LIST;

   public static final HumanoidBehaviorType[] values = values();

   public byte toByte()
   {
      return (byte) ordinal();
   }

   public static HumanoidBehaviorType fromByte(byte enumAsByte)
   {
      return values[enumAsByte];
   }
}
