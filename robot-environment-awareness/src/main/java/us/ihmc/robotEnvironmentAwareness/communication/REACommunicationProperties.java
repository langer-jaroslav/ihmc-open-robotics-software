package us.ihmc.robotEnvironmentAwareness.communication;

import java.util.ArrayList;

import controller_msgs.msg.dds.BoundingBox3DMessagePubSubType;
import controller_msgs.msg.dds.LidarScanMessage;
import controller_msgs.msg.dds.LidarScanMessagePubSubType;
import controller_msgs.msg.dds.PlanarRegionMessage;
import controller_msgs.msg.dds.PlanarRegionMessagePubSubType;
import controller_msgs.msg.dds.PlanarRegionsListMessage;
import controller_msgs.msg.dds.PlanarRegionsListMessagePubSubType;
import controller_msgs.msg.dds.Polygon2DMessage;
import controller_msgs.msg.dds.Polygon2DMessagePubSubType;
import controller_msgs.msg.dds.RequestLidarScanMessagePubSubType;
import controller_msgs.msg.dds.RequestPlanarRegionsListMessagePubSubType;
import geometry_msgs.msg.dds.PointPubSubType;
import geometry_msgs.msg.dds.QuaternionPubSubType;
import geometry_msgs.msg.dds.Vector3PubSubType;
import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.ros2.ROS2MessageTopicNameGenerator;
import us.ihmc.ros2.ROS2TopicQualifier;
import us.ihmc.communication.net.NetClassList;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Point3D32;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.Vector3D32;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.idl.IDLSequence;
import us.ihmc.jOctoMap.normalEstimation.NormalEstimationParameters;
import us.ihmc.messager.Message;
import us.ihmc.messager.MessagerAPIFactory.TopicID;
import us.ihmc.pubsub.TopicDataType;
import us.ihmc.robotEnvironmentAwareness.communication.packets.BoundingBoxParametersMessage;
import us.ihmc.robotEnvironmentAwareness.communication.packets.BoxMessage;
import us.ihmc.robotEnvironmentAwareness.communication.packets.LineSegment3DMessage;
import us.ihmc.robotEnvironmentAwareness.communication.packets.NormalOcTreeMessage;
import us.ihmc.robotEnvironmentAwareness.communication.packets.NormalOcTreeNodeMessage;
import us.ihmc.robotEnvironmentAwareness.communication.packets.OcTreeKeyMessage;
import us.ihmc.robotEnvironmentAwareness.communication.packets.PlanarRegionSegmentationMessage;
import us.ihmc.robotEnvironmentAwareness.geometry.ConcaveHullFactoryParameters;
import us.ihmc.robotEnvironmentAwareness.planarRegion.CustomRegionMergeParameters;
import us.ihmc.robotEnvironmentAwareness.planarRegion.IntersectionEstimationParameters;
import us.ihmc.robotEnvironmentAwareness.planarRegion.PlanarRegionSegmentationParameters;
import us.ihmc.robotEnvironmentAwareness.planarRegion.PolygonizerParameters;

/**
 * Created by adrien on 11/18/16.
 */
public class REACommunicationProperties
{
   public static final ROS2MessageTopicNameGenerator publisherTopicNameGenerator = ROS2Tools.getTopicNameGenerator(null, ROS2Tools.REA_MODULE_NAME, ROS2TopicQualifier.OUTPUT);
   public static final ROS2MessageTopicNameGenerator subscriberTopicNameGenerator = ROS2Tools.getTopicNameGenerator(null, ROS2Tools.REA_MODULE_NAME, ROS2TopicQualifier.INPUT);
   public static final ROS2MessageTopicNameGenerator subscriberCustomRegionsTopicNameGenerator = ROS2Tools.getTopicNameGenerator(null, ROS2Tools.REA_MODULE_NAME
                                                                                                                                       + "/custom_region", ROS2TopicQualifier.INPUT);

   private static final NetClassList privateNetClassList = new NetClassList();
   static
   {
      privateNetClassList.registerPacketClass(Message.class);
      privateNetClassList.registerPacketField(PacketDestination.class);
      privateNetClassList.registerPacketField(Boolean.class);
      privateNetClassList.registerPacketField(Double.class);
      privateNetClassList.registerPacketField(Integer.class);
      privateNetClassList.registerPacketField(float[].class);
      privateNetClassList.registerPacketField(int[].class);
      privateNetClassList.registerPacketField(ArrayList.class);
      privateNetClassList.registerPacketField(Point3D.class);
      privateNetClassList.registerPacketField(Point3D32.class);
      privateNetClassList.registerPacketField(Point2D.class);
      privateNetClassList.registerPacketField(Vector3D.class);
      privateNetClassList.registerPacketField(Vector3D32.class);
      privateNetClassList.registerPacketField(Quaternion.class);
      privateNetClassList.registerPacketField(Point3D[].class);
      privateNetClassList.registerPacketField(Point3D32[].class);
      privateNetClassList.registerPacketField(Point2D[].class);
      privateNetClassList.registerPacketField(LineSegment3DMessage.class);
      privateNetClassList.registerPacketField(LineSegment3DMessage[].class);
      privateNetClassList.registerPacketField(TopicID.class);
      privateNetClassList.registerPacketField(LidarScanMessage.class);
      privateNetClassList.registerPacketField(BoxMessage.class);
      privateNetClassList.registerPacketField(BoundingBoxParametersMessage.class);
      privateNetClassList.registerPacketField(NormalEstimationParameters.class);
      privateNetClassList.registerPacketField(PlanarRegionSegmentationParameters.class);
      privateNetClassList.registerPacketField(CustomRegionMergeParameters.class);
      privateNetClassList.registerPacketField(IntersectionEstimationParameters.class);
      privateNetClassList.registerPacketField(PolygonizerParameters.class);
      privateNetClassList.registerPacketField(NormalOcTreeMessage.class);
      privateNetClassList.registerPacketField(NormalOcTreeNodeMessage.class);
      privateNetClassList.registerPacketField(NormalOcTreeNodeMessage[].class);
      privateNetClassList.registerPacketField(OcTreeKeyMessage.class);
      privateNetClassList.registerPacketField(OcTreeKeyMessage[].class);
      privateNetClassList.registerPacketField(PlanarRegionSegmentationMessage.class);
      privateNetClassList.registerPacketField(PlanarRegionSegmentationMessage[].class);
      privateNetClassList.registerPacketField(PlanarRegionsListMessage.class);
      privateNetClassList.registerPacketField(Polygon2DMessage.class);
      privateNetClassList.registerPacketField(PlanarRegionMessage.class);
      privateNetClassList.registerPacketField(ConcaveHullFactoryParameters.class);

      privateNetClassList.registerPacketField(Vector3PubSubType.class);
      privateNetClassList.registerPacketField(PointPubSubType.class);
      privateNetClassList.registerPacketField(QuaternionPubSubType.class);
      privateNetClassList.registerPacketField(Polygon2DMessagePubSubType.class);
      privateNetClassList.registerPacketField(BoundingBox3DMessagePubSubType.class);
      privateNetClassList.registerPacketField(RequestPlanarRegionsListMessagePubSubType.class);
      privateNetClassList.registerPacketField(RequestLidarScanMessagePubSubType.class);
      privateNetClassList.registerPacketField(PlanarRegionsListMessagePubSubType.class);
      privateNetClassList.registerPacketField(LidarScanMessagePubSubType.class);
      privateNetClassList.registerPacketField(PlanarRegionMessagePubSubType.class);

      privateNetClassList.registerPacketField(IDLSequence.Object.class);
      privateNetClassList.registerPacketField(IDLSequence.Float.class);
      privateNetClassList.registerPacketField(IDLSequence.Boolean.class);
      privateNetClassList.registerPacketField(IDLSequence.Double.class);
      privateNetClassList.registerPacketField(IDLSequence.Integer.class);
      privateNetClassList.registerPacketField(IDLSequence.Byte.class);
      privateNetClassList.registerPacketField(IDLSequence.Long.class);
      privateNetClassList.registerPacketField(IDLSequence.StringBuilderHolder.class);
      privateNetClassList.registerPacketField(TopicDataType.class);
      privateNetClassList.registerPacketField(RecyclingArrayList.class);
      privateNetClassList.registerPacketField(us.ihmc.idl.CDR.class);
   }
   
   public static NetClassList getPrivateNetClassList()
   {
      return privateNetClassList;
   }
}
