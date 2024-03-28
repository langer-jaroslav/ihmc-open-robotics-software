package us.ihmc.avatar.networkProcessor.reaStateUpdater;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import controller_msgs.msg.dds.HighLevelStateChangeStatusMessage;
import perception_msgs.msg.dds.REAStateRequestMessage;
import controller_msgs.msg.dds.WalkingStatusMessage;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.communication.HumanoidControllerAPI;
import us.ihmc.ros2.ROS2PublisherBasics;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HighLevelControllerName;
import us.ihmc.humanoidRobotics.communication.packets.walking.WalkingStatus;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.pubsub.subscriber.Subscriber;
import us.ihmc.robotEnvironmentAwareness.communication.REACommunicationProperties;
import us.ihmc.ros2.ROS2Topic;
import us.ihmc.ros2.RealtimeROS2Node;
import us.ihmc.tools.thread.CloseableAndDisposable;

public class HumanoidAvatarREAStateUpdater implements CloseableAndDisposable
{
   private final boolean manageROS2Node;
   private final RealtimeROS2Node ros2Node;
   private final ROS2PublisherBasics<REAStateRequestMessage> reaStateRequestPublisher;

   private final ExecutorService executorService = Executors.newSingleThreadExecutor(ThreadTools.createNamedThreadFactory(getClass().getSimpleName()));

   private final REAStateRequestMessage clearRequestMessage = new REAStateRequestMessage();
   private final REAStateRequestMessage pauseRequestMessage = new REAStateRequestMessage();
   private final REAStateRequestMessage resumeRequestMessage = new REAStateRequestMessage();
   private final REAStateRequestMessage clearAndResumeRequestMessage = new REAStateRequestMessage();

   public HumanoidAvatarREAStateUpdater(DRCRobotModel robotModel, RealtimeROS2Node realtimeROS2Node)
   {
      this(robotModel, realtimeROS2Node, REACommunicationProperties.inputTopic);
   }

   public HumanoidAvatarREAStateUpdater(DRCRobotModel robotModel, PubSubImplementation implementation)
   {
      this(robotModel, implementation, REACommunicationProperties.inputTopic);
   }

   public HumanoidAvatarREAStateUpdater(DRCRobotModel robotModel, RealtimeROS2Node realtimeROS2Node, ROS2Topic<?> inputTopic)
   {
      this(robotModel, realtimeROS2Node, null, inputTopic);
   }

   public HumanoidAvatarREAStateUpdater(DRCRobotModel robotModel, PubSubImplementation implementation, ROS2Topic<?> inputTopic)
   {
      this(robotModel, null, implementation, inputTopic);
   }

   private HumanoidAvatarREAStateUpdater(DRCRobotModel robotModel, RealtimeROS2Node realtimeROS2Node, PubSubImplementation implementation,
                                         ROS2Topic<?> inputTopic)
   {
      String robotName = robotModel.getSimpleRobotName();

      clearRequestMessage.setRequestClear(true);
      pauseRequestMessage.setRequestPause(true);
      resumeRequestMessage.setRequestResume(true);
      clearAndResumeRequestMessage.setRequestClear(true);
      clearAndResumeRequestMessage.setRequestResume(true);

      manageROS2Node = realtimeROS2Node == null;
      if (realtimeROS2Node == null)
         realtimeROS2Node = ROS2Tools.createRealtimeROS2Node(implementation, "avatar_rea_state_updater");
      ros2Node = realtimeROS2Node;

      reaStateRequestPublisher = ros2Node.createPublisher(inputTopic.withTypeName(REAStateRequestMessage.class));
      ROS2Tools.createCallbackSubscriptionTypeNamed(ros2Node,
                                                    HighLevelStateChangeStatusMessage.class,
                                                    HumanoidControllerAPI.getOutputTopic(robotName),
                                                    this::handleHighLevelStateChangeMessage);
      ROS2Tools.createCallbackSubscriptionTypeNamed(ros2Node,
                                                    WalkingStatusMessage.class,
                                                    HumanoidControllerAPI.getOutputTopic(robotName),
                                                    this::handleWalkingStatusMessage);

      if (manageROS2Node)
         ros2Node.spin();
   }

   private void handleHighLevelStateChangeMessage(Subscriber<HighLevelStateChangeStatusMessage> subscriber)
   {
      if (executorService.isShutdown())
         return;

      HighLevelStateChangeStatusMessage newMessage = subscriber.takeNextData();

      if (newMessage.getInitialHighLevelControllerName() == newMessage.getEndHighLevelControllerName())
         return;

      switch (HighLevelControllerName.fromByte(newMessage.getEndHighLevelControllerName()))
      {
         case WALKING:
            executorService.execute(() -> reaStateRequestPublisher.publish(clearAndResumeRequestMessage));
            break;
         default:
            executorService.execute(() -> reaStateRequestPublisher.publish(pauseRequestMessage));
            break;
      }
   }

   private void handleWalkingStatusMessage(Subscriber<WalkingStatusMessage> subscriber)
   {
      if (executorService.isShutdown())
         return;

      WalkingStatusMessage newMessage = subscriber.takeNextData();

      switch (WalkingStatus.fromByte(newMessage.getWalkingStatus()))
      {
         case STARTED:
         case RESUMED:
            executorService.execute(() -> reaStateRequestPublisher.publish(pauseRequestMessage));
            break;
         case COMPLETED:
         case PAUSED:
            executorService.execute(() -> reaStateRequestPublisher.publish(resumeRequestMessage));
            break;
         case ABORT_REQUESTED:
         default:
            // Do nothing?
            break;
      }
   }

   private void shutdown()
   {
      executorService.shutdownNow();
      if (manageROS2Node)
         ros2Node.destroy();
   }

   @Override
   public void closeAndDispose()
   {
      shutdown();
   }
}
