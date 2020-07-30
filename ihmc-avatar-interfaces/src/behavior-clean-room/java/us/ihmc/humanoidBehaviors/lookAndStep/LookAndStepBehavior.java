package us.ihmc.humanoidBehaviors.lookAndStep;

import us.ihmc.commons.thread.Notification;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.util.TimerSnapshotWithExpiration;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FramePose3DReadOnly;
import us.ihmc.footstepPlanning.FootstepPlan;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersBasics;
import us.ihmc.footstepPlanning.swing.SwingPlannerParametersBasics;
import us.ihmc.humanoidBehaviors.BehaviorDefinition;
import us.ihmc.humanoidBehaviors.BehaviorInterface;
import us.ihmc.humanoidBehaviors.lookAndStep.parts.*;
import us.ihmc.humanoidBehaviors.lookAndStep.parts.LookAndStepBodyPathPlanningTask.LookAndStepBodyPathPlanning;
import us.ihmc.humanoidBehaviors.lookAndStep.parts.LookAndStepFootstepPlanningTask.LookAndStepFootstepPlanning;
import us.ihmc.humanoidBehaviors.lookAndStep.parts.LookAndStepRobotMotionTask.LookAndStepRobotMotion;
import us.ihmc.humanoidBehaviors.tools.BehaviorHelper;
import us.ihmc.humanoidBehaviors.tools.RemoteHumanoidRobotInterface;
import us.ihmc.humanoidBehaviors.tools.RemoteSyncedRobotModel;
import us.ihmc.humanoidBehaviors.tools.interfaces.StatusLogger;
import us.ihmc.humanoidBehaviors.tools.walking.WalkingFootstepTracker;
import us.ihmc.log.LogTools;
import us.ihmc.pathPlanning.visibilityGraphs.parameters.VisibilityGraphsParametersBasics;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.commons.thread.TypedNotification;
import us.ihmc.robotics.robotSide.SideDependentList;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static us.ihmc.humanoidBehaviors.lookAndStep.LookAndStepBehaviorAPI.*;

public class LookAndStepBehavior implements BehaviorInterface
{
   public static final BehaviorDefinition DEFINITION = new BehaviorDefinition("Look and Step", LookAndStepBehavior::new, create());

   private final BehaviorHelper helper;
   private final RemoteHumanoidRobotInterface robotInterface;

   private final LookAndStepBodyPathPlanning bodyPathPlanning = new LookAndStepBodyPathPlanning();
   private final LookAndStepFootstepPlanning footstepPlanning = new LookAndStepFootstepPlanning();
   private final LookAndStepRobotMotion robotMotion = new LookAndStepRobotMotion();
   private final LookAndStepReview<List<? extends Pose3DReadOnly>> bodyPathReview = new LookAndStepReview<>();
   private final LookAndStepReview<FootstepPlan> footstepPlanReview = new LookAndStepReview<>();
   private final BehaviorStateReference<State> behaviorStateReference;
   private final StatusLogger statusLogger;
   private final LookAndStepBehaviorParameters lookAndStepParameters;
   private final FootstepPlannerParametersBasics footstepPlannerParameters;
   private final SwingPlannerParametersBasics swingPlannerParameters;
   private final RemoteSyncedRobotModel syncedRobot;

   /**
    * At any time the behavior will be executing on one of this tasks
    * or "trying" to do it. Sometimes conditions will not be satisfied
    * to execute the task the behavior will be waiting on those conditions.
    */
   public enum State
   {
      BODY_PATH_PLANNING, FOOTSTEP_PLANNING, ROBOT_MOTION
   }

   /**
    * We'll make the following assumptions/constraints about the data being passed around between task/modules:
    * - Data output from module will be read only and the underlying data must not be modified after sending
    * - Timers are considered parallel tasks/ modules
    * <p>
    * Inputs can be polled or triggered by an event
    * Outputs can be an event or polled by another task
    */
   public LookAndStepBehavior(BehaviorHelper helper)
   {
      this.helper = helper;

      robotInterface = helper.getOrCreateRobotInterface();
      syncedRobot = robotInterface.newSyncedRobot();
      statusLogger = helper.getOrCreateStatusLogger();

      VisibilityGraphsParametersBasics visibilityGraphParameters = helper.getRobotModel().getVisibilityGraphsParameters();
      visibilityGraphParameters.setIncludePreferredExtrusions(false);
      visibilityGraphParameters.setTooHighToStepDistance(0.2);

      lookAndStepParameters = new LookAndStepBehaviorParameters();
      footstepPlannerParameters = helper.getRobotModel().getFootstepPlannerParameters();
      swingPlannerParameters = helper.getRobotModel().getSwingPlannerParameters();

      updateOverrideParameters();

      helper.createUICallback(LookAndStepParameters, parameters ->
      {
         statusLogger.info("Accepting new look and step parameters");
         lookAndStepParameters.setAllFromStrings(parameters);
         updateOverrideParameters();
      });
      helper.createUICallback(FootstepPlannerParameters, parameters ->
      {
         statusLogger.info("Accepting new footstep planner parameters");
         footstepPlannerParameters.setAllFromStrings(parameters);
      }); // TODO: This overrides overrides?

      AtomicReference<Boolean> operatorReviewEnabledInput = helper.createUIInput(OperatorReviewEnabled, true);
      TypedNotification<Boolean> approvalNotification = helper.createUITypedNotification(ReviewApproval);

      // Trying to hold a lot of the state here? TODO: In general, where to put what state?
      AtomicReference<RobotSide> lastStanceSide = new AtomicReference<>();
      SideDependentList<FramePose3DReadOnly> lastSteppedSolePoses = new SideDependentList<>();
      Notification resetInput = helper.createROS2Notification(RESET);
      behaviorStateReference = new BehaviorStateReference<>(State.BODY_PATH_PLANNING, statusLogger, helper::publishToUI);

      // TODO: Footstep log
      WalkingFootstepTracker walkingFootstepTracker = new WalkingFootstepTracker(helper.getManagedROS2Node(),
                                                                                 helper.getRobotModel().getSimpleRobotName());

      // TODO: Want to be able to wire up behavior here and see all present modules
      // TODO: Add more meaning to the construction by establishing data patterns
      // TODO: Use named interfaces or pass in actual objects to prevent misordered arguments

      // could add meaning by local variables before passing
      bodyPathPlanning.initialize(
            statusLogger,
            helper::publishToUI,
            visibilityGraphParameters,
            lookAndStepParameters,
            operatorReviewEnabledInput::get,
            robotInterface.newSyncedRobot(),
            behaviorStateReference,
            this::robotConnected,
            walkingFootstepTracker,
            bodyPathReview,
            footstepPlanning::acceptBodyPathPlan
      );
      helper.createROS2Callback(ROS2Tools.LIDAR_REA_REGIONS, bodyPathPlanning::acceptMapRegions);
      helper.createROS2Callback(GOAL_INPUT, bodyPathPlanning::acceptGoal);
      bodyPathReview.initialize(
            statusLogger,
            "body path",
            approvalNotification,
            bodyPathPlan ->
            {
               behaviorStateReference.set(State.FOOTSTEP_PLANNING);
               footstepPlanning.acceptBodyPathPlan(bodyPathPlan);
            }
      );
      footstepPlanning.initialize(
            statusLogger,
            lookAndStepParameters, footstepPlannerParameters, swingPlannerParameters,
            resetInput::poll,
            helper::publishToUI,
            () -> {
               bodyPathPlanning.acceptGoal(null);
               helper.publishROS2(REACHED_GOAL);
            },
            lastSteppedSolePoses,
            helper.getOrCreateFootstepPlanner(),
            lastStanceSide,
            operatorReviewEnabledInput::get,
            robotInterface.newSyncedRobot(),
            behaviorStateReference,
            this::robotConnected,
            walkingFootstepTracker,
            footstepPlanReview,
            robotMotion::acceptFootstepPlan
      );
      helper.createROS2Callback(ROS2Tools.REALSENSE_SLAM_REGIONS, footstepPlanning::acceptPlanarRegions);
      footstepPlanReview.initialize(
            statusLogger,
            "footstep plan",
            approvalNotification,
            footstepPlan ->
            {
               behaviorStateReference.set(LookAndStepBehavior.State.ROBOT_MOTION);
               robotMotion.acceptFootstepPlan(footstepPlan);
            }
      );
      robotMotion.initialize(
            statusLogger,
            robotInterface.newSyncedRobot(),
            lookAndStepParameters,
            lastSteppedSolePoses,
            helper::publishToUI,
            robotInterface::requestWalk,
            footstepPlanning::runOrQueue,
            behaviorStateReference,
            this::robotConnected
      );
   }

   private void updateOverrideParameters()
   {
      footstepPlannerParameters.setIdealFootstepLength(lookAndStepParameters.getIdealFootstepLengthOverride());
      footstepPlannerParameters.setWiggleInsideDelta(lookAndStepParameters.getWiggleInsideDeltaOverride());
      footstepPlannerParameters.setCliffBaseHeightToAvoid(lookAndStepParameters.getCliffBaseHeightToAvoidOverride());
      footstepPlannerParameters.setEnableConcaveHullWiggler(lookAndStepParameters.getEnableConcaveHullWigglerOverride());
      swingPlannerParameters.setMinimumSwingFootClearance(lookAndStepParameters.getMinimumSwingFootClearanceOverride());
   }

   private boolean robotConnected()
   {
      TimerSnapshotWithExpiration timerSnaphot = syncedRobot.getDataReceptionTimerSnapshot()
                                                            .withExpiration(lookAndStepParameters.getRobotConfigurationDataExpiration());
      return timerSnaphot.hasBeenSet() && !timerSnaphot.isExpired();
   }

   @Override
   public void setEnabled(boolean enabled)
   {
      LogTools.info("Look and step behavior selected = {}", enabled);

      helper.setCommunicationCallbacksEnabled(enabled);

      behaviorStateReference.broadcast();

      robotInterface.pitchHeadWithRespectToChest(0.38);
      //      Commanding neck trajectory: slider: 43.58974358974359 angle: 0.3824055641025641
   }
}
