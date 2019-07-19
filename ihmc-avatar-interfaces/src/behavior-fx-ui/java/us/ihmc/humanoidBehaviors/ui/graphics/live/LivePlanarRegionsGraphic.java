package us.ihmc.humanoidBehaviors.ui.graphics.live;

import controller_msgs.msg.dds.PlanarRegionsListMessage;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.communication.ROS2Callback;
import us.ihmc.communication.packets.PlanarRegionMessageConverter;
import us.ihmc.humanoidBehaviors.ui.graphics.PlanarRegionsGraphic;
import us.ihmc.humanoidBehaviors.ui.tools.PrivateAnimationTimer;
import us.ihmc.robotEnvironmentAwareness.updaters.LIDARBasedREAModule;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.ros2.Ros2Node;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LivePlanarRegionsGraphic extends PlanarRegionsGraphic
{
   private final PrivateAnimationTimer animationTimer = new PrivateAnimationTimer(this::handle);

   private final ExecutorService executorService = Executors.newSingleThreadExecutor(ThreadTools.getNamedThreadFactory(getClass().getSimpleName()));

   private boolean acceptNewRegions = true;
   private volatile PlanarRegionsList latestPlanarRegionsList = new PlanarRegionsList(); // prevent NPEs

   public LivePlanarRegionsGraphic(Ros2Node ros2Node)
   {
      super(true);
   }

   public LivePlanarRegionsGraphic(Ros2Node ros2Node, boolean initializeToFlatGround)
   {
      super(initializeToFlatGround);

      new ROS2Callback<>(ros2Node, PlanarRegionsListMessage.class, null, LIDARBasedREAModule.ROS2_ID, this::acceptPlanarRegions);
      animationTimer.start();
   }

   private void acceptPlanarRegions(PlanarRegionsListMessage incomingData)
   {
      if (acceptNewRegions)
      {
         synchronized (this) // just here for clear method
         {
            executorService.submit(() -> convertAndGenerateMesh(incomingData));
         }
      }
   }

   private void convertAndGenerateMesh(PlanarRegionsListMessage incomingData)
   {
      latestPlanarRegionsList = PlanarRegionMessageConverter.convertToPlanarRegionsList(incomingData);
      generateMeshes(latestPlanarRegionsList); // important not to execute this in either ROS2 or JavaFX threads
   }

   private void handle(long now)
   {
      super.update();
   }

   public void clear()
   {
      latestPlanarRegionsList.clear();
      synchronized (this) // to avoid collision with ROS 2 thread
      {
         executorService.submit(() -> generateMeshes(latestPlanarRegionsList));
      }
   }

   public void setAcceptNewRegions(boolean acceptNewRegions)
   {
      this.acceptNewRegions = acceptNewRegions;
   }

   public PlanarRegionsList getLatestPlanarRegionsList()
   {
      return latestPlanarRegionsList;
   }
}
