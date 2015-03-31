package us.ihmc.darpaRoboticsChallenge.networkProcessor.depthData;

import java.util.ArrayList;
import java.util.Arrays;

import javax.vecmath.Point3d;

import us.ihmc.communication.net.ObjectCommunicator;
import us.ihmc.communication.net.ObjectConsumer;
import us.ihmc.communication.packets.sensing.SimulatedLidarScanPacket;
import us.ihmc.utilities.lidar.polarLidar.LidarScan;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;

public class SCSPointCloudLidarReceiver implements ObjectConsumer<SimulatedLidarScanPacket>
{

   private final PointCloudDataReceiver pointCloudDataReceiver;
   private final ReferenceFrame lidarFrame;

   private final ReferenceFrame lidarScanFrame;
   private final RigidBodyTransform identityTransform = new RigidBodyTransform();

   public SCSPointCloudLidarReceiver(String lidarName, ObjectCommunicator scsSensorsCommunicator, PointCloudDataReceiver pointCloudDataReceiver)
   {
      this.pointCloudDataReceiver = pointCloudDataReceiver;
      this.lidarFrame = pointCloudDataReceiver.getLidarFrame(lidarName);

      RigidBodyTransform lidarBaseToSensorTransform = pointCloudDataReceiver.getLidarToSensorTransform(lidarName);
      ReferenceFrame lidarAfterJointFrame = pointCloudDataReceiver.getLidarJoint(lidarName).getFrameAfterJoint();
      this.lidarScanFrame = ReferenceFrame
            .constructBodyFrameWithUnchangingTransformToParent("lidarScanFrame", lidarAfterJointFrame, lidarBaseToSensorTransform);

      scsSensorsCommunicator.attachListener(SimulatedLidarScanPacket.class, this);
   }

   @Override
   public void consumeObject(SimulatedLidarScanPacket packet)
   {
      LidarScan scan = new LidarScan(packet.getLidarScanParameters(), packet.getRanges(), packet.getSensorId());
      // Set the world transforms to nothing, so points are in lidar scan frame
      scan.setWorldTransforms(identityTransform, identityTransform);
      ArrayList<Point3d> points = scan.getAllPoints();

      long[] timestamps = new long[points.size()];
      Arrays.fill(timestamps, packet.getScanStartTime());

      pointCloudDataReceiver.receivedPointCloudData(lidarScanFrame, lidarFrame, timestamps, points);
   }

   public void connect()
   {

   }

}
