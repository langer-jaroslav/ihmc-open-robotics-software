package us.ihmc.behaviors.activeMapping;

import behavior_msgs.msg.dds.ContinuousWalkingCommandMessage;
import controller_msgs.msg.dds.*;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ControllerAPIDefinition;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.ros2.ROS2Helper;
import us.ihmc.communication.ros2.ROS2PublisherMap;
import us.ihmc.communication.video.ContinuousPlanningAPI;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.footstepPlanning.monteCarloPlanning.TerrainPlanningDebugger;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.log.LogTools;
import us.ihmc.perception.heightMap.TerrainMapData;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.ros2.ROS2Node;
import us.ihmc.ros2.ROS2Topic;
import us.ihmc.sensorProcessing.heightMap.HeightMapData;
import us.ihmc.tools.thread.ExecutorServiceTools;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ContinuousPlannerSchedulingTask
{
   private final static long CONTINUOUS_PLANNING_DELAY_MS = 16;

   private ContinuousWalkingState state = ContinuousWalkingState.NOT_STARTED;

   private enum ContinuousWalkingState
   {
      NOT_STARTED, READY_TO_PLAN, NEED_TO_REPLAN, PLAN_AVAILABLE, WAITING_TO_LAND, PAUSED
   }

   protected final ScheduledExecutorService executorService = ExecutorServiceTools.newScheduledThreadPool(1,
                                                                                                          getClass(),
                                                                                                          ExecutorServiceTools.ExceptionHandling.CATCH_AND_REPORT);

   private final AtomicReference<FootstepStatusMessage> footstepStatusMessage = new AtomicReference<>(new FootstepStatusMessage());
   private final AtomicReference<ContinuousWalkingCommandMessage> commandMessage = new AtomicReference<>(new ContinuousWalkingCommandMessage());
   private final ROS2Topic controllerFootstepDataTopic;
   private final IHMCROS2Publisher<PauseWalkingMessage> pauseWalkingPublisher;
   private final ROS2PublisherMap publisherMap;
   private TerrainPlanningDebugger debugger;
   private HeightMapData latestHeightMapData;
   private TerrainMapData terrainMap;

   private ContinuousPlannerStatistics statistics;
   private final ContinuousWalkingParameters parameters;

   private final ContinuousPlanner continuousPlanner;
   private final HumanoidReferenceFrames referenceFrames;

   private String message = "";
   private int controllerQueueSize = 0;
   private List<QueuedFootstepStatusMessage> controllerQueue;

   public ContinuousPlannerSchedulingTask(DRCRobotModel robotModel,
                                          ROS2Node ros2Node,
                                          HumanoidReferenceFrames referenceFrames,
                                          ContinuousWalkingParameters parameters,
                                          ContinuousPlanner.PlanningMode mode)
   {
      this.debugger = new TerrainPlanningDebugger(ros2Node);
      this.referenceFrames = referenceFrames;
      this.parameters = parameters;

      controllerFootstepDataTopic = ControllerAPIDefinition.getTopic(FootstepDataListMessage.class, robotModel.getSimpleRobotName());
      publisherMap = new ROS2PublisherMap(ros2Node);
      publisherMap.getOrCreatePublisher(controllerFootstepDataTopic);

      ROS2Topic<?> inputTopic = ROS2Tools.getControllerInputTopic(robotModel.getSimpleRobotName());

      pauseWalkingPublisher = ROS2Tools.createPublisherTypeNamed(ros2Node, PauseWalkingMessage.class, inputTopic);

      ROS2Helper ros2Helper = new ROS2Helper(ros2Node);
      ros2Helper.subscribeViaCallback(ControllerAPIDefinition.getTopic(FootstepStatusMessage.class, robotModel.getSimpleRobotName()),
                                      this::footstepStatusReceived);
      ros2Helper.subscribeViaCallback(ControllerAPIDefinition.getTopic(FootstepQueueStatusMessage.class, robotModel.getSimpleRobotName()),
                                      this::footstepQueueStatusReceived);
      ros2Helper.subscribeViaCallback(ContinuousPlanningAPI.CONTINUOUS_WALKING_COMMAND, commandMessage::set);

      continuousPlanner = new ContinuousPlanner(robotModel, referenceFrames, mode, debugger);

      statistics = new ContinuousPlannerStatistics();
      continuousPlanner.setContinuousPlannerStatistics(statistics);

      executorService.scheduleWithFixedDelay(this::tickStateMachine, 1500, CONTINUOUS_PLANNING_DELAY_MS, TimeUnit.MILLISECONDS);
   }

   FramePose3D rightRobotFoot;
   FramePose3D leftRobotFoot;

   /**
    * Runs the continuous planner state machine every ACTIVE_MAPPING_UPDATE_TICK_MS milliseconds. The state is stored in the ContinuousWalkingState
    */
   private void tickStateMachine()
   {
      // Sets the planner timeout to be a percentage of the total step duration
      //      double stepDuration = continuousWalkingParameters.getSwingTime() + continuousWalkingParameters.getTransferTime();
      //      continuousWalkingParameters.setPlanningReferenceTimeout(stepDuration * continuousWalkingParameters.getPlannerTimeoutFraction());

      if (!parameters.getEnableContinuousWalking() || !commandMessage.get().getEnableContinuousWalking())
      {
         state = ContinuousWalkingState.NOT_STARTED;

         sendPauseWalkingMessage();
         setImminentStanceToCurrent();

         continuousPlanner.setInitialized(false);
         return;
      }

      if (!continuousPlanner.isInitialized())
      {
         statistics.appendString("Restarting State Machine");
         initializeContinuousPlanner();
      }
      else
      {
         handleStateMachine();
      }
   }

   private void sendPauseWalkingMessage()
   {
      PauseWalkingMessage message = new PauseWalkingMessage();

      if (continuousPlanner.isInitialized())
      {
         message.setPause(true);
         pauseWalkingPublisher.publish(message);
      }
   }

   public void initializeContinuousPlanner()
   {
      continuousPlanner.initialize();
      continuousPlanner.setGoalWaypointPoses(parameters);
      continuousPlanner.planToGoalWithHeightMap(latestHeightMapData, terrainMap, false, true);
      debugger.publishMonteCarloPlan(continuousPlanner.getMonteCarloFootstepDataListMessage());
      debugger.publishMonteCarloNodesForVisualization(continuousPlanner.getMonteCarloFootstepPlanner().getRoot(), terrainMap);

      if (continuousPlanner.isPlanAvailable())
      {
         state = ContinuousWalkingState.PLAN_AVAILABLE;
      }
      else
      {
         state = ContinuousWalkingState.NOT_STARTED;
         continuousPlanner.setInitialized(false);

         LogTools.error(message = String.format("State: [%s]: Initialization failed... will retry initializing next tick", state));
         statistics.appendString(message);
      }
   }

   public void handleStateMachine()
   {
      /*
       * Ready to plan means that the current step is completed and the planner is ready to plan the next step
       */
      if (state == ContinuousWalkingState.READY_TO_PLAN)
      {
         LogTools.info("State: " + state);
         statistics.setLastAndTotalWaitingTimes();

         if (parameters.getStepPublisherEnabled())
            continuousPlanner.getImminentStanceFromLatestStatus(footstepStatusMessage, controllerQueue);

         debugger.publishStartAndGoalForVisualization(continuousPlanner.getStartingStancePose(), continuousPlanner.getGoalStancePose());
         continuousPlanner.setGoalWaypointPoses(parameters);
         continuousPlanner.planToGoalWithHeightMap(latestHeightMapData, terrainMap, true, true);
         debugger.publishMonteCarloPlan(continuousPlanner.getMonteCarloFootstepDataListMessage());
         debugger.publishMonteCarloNodesForVisualization(continuousPlanner.getMonteCarloFootstepPlanner().getRoot(), terrainMap);

         if (continuousPlanner.isPlanAvailable())
         {
            state = ContinuousWalkingState.PLAN_AVAILABLE;
         }
         else
         {
            state = ContinuousWalkingState.WAITING_TO_LAND;
            LogTools.error(message = String.format("State: [%s]: Planning failed... will try again when current step is completed", state));
            statistics.appendString(message);
         }
      }

      /*
       * Plan available means that the planner has a plan ready to send to the controller
       */
      if (state == ContinuousWalkingState.PLAN_AVAILABLE)
      {
         LogTools.info("State: " + state);
         FootstepDataListMessage footstepDataList = continuousPlanner.getLimitedFootstepDataListMessage(parameters, controllerQueue);

         debugger.publishPlannedFootsteps(footstepDataList);
         debugger.publishMonteCarloPlan(continuousPlanner.getMonteCarloFootstepDataListMessage());
         debugger.publishMonteCarloNodesForVisualization(continuousPlanner.getMonteCarloFootstepPlanner().getRoot(), terrainMap);

         if (parameters.getStepPublisherEnabled())
         {
            LogTools.info(message = String.format("State: [%s]: Sending (" + footstepDataList.getFootstepDataList().size() + ") steps to controller", state));
            publisherMap.publish(controllerFootstepDataTopic, footstepDataList);

            state = ContinuousWalkingState.WAITING_TO_LAND;
            continuousPlanner.setPlanAvailable(false);
            continuousPlanner.transitionCallback();
            statistics.setStartWaitingTime();
         }
         else
         {
            state = ContinuousWalkingState.READY_TO_PLAN;
         }
      }
   }

   // This receives a message each time there is a change in the FootstepStatusMessage
   private void footstepStatusReceived(FootstepStatusMessage footstepStatusMessage)
   {
      if (!parameters.getEnableContinuousWalking())
         return;

      if (footstepStatusMessage.getFootstepStatus() == FootstepStatusMessage.FOOTSTEP_STATUS_STARTED)
      {
         state = ContinuousWalkingState.READY_TO_PLAN;
         statistics.endStepTime();
         statistics.startStepTime();
      }
      else if (footstepStatusMessage.getFootstepStatus() == FootstepStatusMessage.FOOTSTEP_STATUS_COMPLETED)
      {
         statistics.setLastFootstepQueueLength(controllerQueueSize);
         statistics.incrementTotalStepsCompleted();

         double distance = referenceFrames.getSoleFrame(RobotSide.LEFT)
                                          .getTransformToDesiredFrame(referenceFrames.getSoleFrame(RobotSide.RIGHT))
                                          .getTranslation()
                                          .norm();
         statistics.setLastLengthCompleted((float) distance);

         statistics.logToFile(true, true);
      }

      this.footstepStatusMessage.set(footstepStatusMessage);
   }

   private void footstepQueueStatusReceived(FootstepQueueStatusMessage footstepQueueStatusMessage)
   {
      if (!parameters.getEnableContinuousWalking())
         return;

      controllerQueue = footstepQueueStatusMessage.getQueuedFootstepList();
      if (controllerQueueSize != footstepQueueStatusMessage.getQueuedFootstepList().size())
      {
         LogTools.warn(message = String.format("State: [%s]: Controller Queue Footstep Size: " + footstepQueueStatusMessage.getQueuedFootstepList().size(),
                                               state));
      }
      controllerQueueSize = footstepQueueStatusMessage.getQueuedFootstepList().size();
   }

   private void setImminentStanceToCurrent()
   {
      rightRobotFoot = new FramePose3D(ReferenceFrame.getWorldFrame(), referenceFrames.getSoleFrame(RobotSide.RIGHT).getTransformToWorldFrame());
      leftRobotFoot = new FramePose3D(ReferenceFrame.getWorldFrame(), referenceFrames.getSoleFrame(RobotSide.LEFT).getTransformToWorldFrame());

      if (continuousPlanner.updateImminentStance(rightRobotFoot, leftRobotFoot, RobotSide.LEFT))
      {
         debugger.publishStartAndGoalForVisualization(continuousPlanner.getStartingStancePose(), continuousPlanner.getGoalStancePose());
      }
   }

   public void setLatestHeightMapData(HeightMapData heightMapData)
   {
      this.latestHeightMapData = new HeightMapData(heightMapData);
      this.continuousPlanner.setLatestHeightMapData(heightMapData);
   }

   public void setTerrainMapData(TerrainMapData terrainMapData)
   {
      this.terrainMap = new TerrainMapData(terrainMapData);
      this.continuousPlanner.setLatestTerrainMapData(terrainMapData);
   }

   public ContinuousPlanner getContinuousPlanner()
   {
      return continuousPlanner;
   }

   public void destroy()
   {
      executorService.shutdown();
   }
}
