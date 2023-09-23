package us.ihmc.perception.sceneGraph.rigidBodies;

import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.log.LogTools;
import us.ihmc.perception.sceneGraph.modification.SceneGraphNodeAddition;
import us.ihmc.perception.sceneGraph.modification.SceneGraphTreeModification;
import us.ihmc.perception.sceneGraph.ros2.ROS2SceneGraph;
import us.ihmc.perception.sceneGraph.SceneNode;
import us.ihmc.perception.sceneGraph.arUco.ArUcoMarkerNode;
import us.ihmc.robotics.EuclidCoreMissingTools;

import java.util.function.Consumer;

/**
 * Static methods to create boxes, cylinders, etc.
 */
public class RigidBodySceneObjectDefinitions
{
   /**
    * This is the width of the markers printed with IHMC's large format
    * printer. Send ihmc-perception/src/main/resources/arUcoMarkers/Markers0Through3.pdf
    * to IT to get new ones printed.
    */
   public static final double LARGE_MARKER_WIDTH = 0.1982;

   public static final int BOX_MARKER_ID = 2;
   // The box is a cube
   public static final double BOX_SIZE = 0.35;
   public static final double BOX_MARKER_FROM_BOTTOM_Z = 0.047298;
   public static final double BOX_MARKER_FROM_RIGHT_Y = 0.047298;
   public static final RigidBodyTransform MARKER_TO_BOX_TRANSFORM = new RigidBodyTransform();
   public static final RigidBodyTransform BOX_TO_MARKER_TRANSFORM = new RigidBodyTransform();
   static
   {
      EuclidCoreMissingTools.setYawPitchRollDegrees(MARKER_TO_BOX_TRANSFORM.getRotation(), 180.0, 0.0, 0.0);
      MARKER_TO_BOX_TRANSFORM.getTranslation().set(0.0, BOX_MARKER_FROM_RIGHT_Y, BOX_MARKER_FROM_BOTTOM_Z);
      BOX_TO_MARKER_TRANSFORM.setAndInvert(MARKER_TO_BOX_TRANSFORM);
   }
   public static final String BOX_VISUAL_MODEL_FILE_PATH = "environmentObjects/box/box.g3dj";
   public static final RigidBodyTransform BOX_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM = new RigidBodyTransform();

   // TODO: Get soup can model from Arghya
   public static final int CAN_OF_SOUP_MARKER_ID = 3;
   public static final double CAN_OF_SOUP_RADIUS = 0.0329375;
   public static final double CAN_OF_SOUP_HEIGHT = 0.082388;
   public static final String CAN_OF_SOUP_VISUAL_MODEL_FILE_PATH = "environmentObjects/canOfSoup/CanOfSoup.g3dj";
   public static final RigidBodyTransform CAN_OF_SOUP_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM = new RigidBodyTransform();
   static
   {
      CAN_OF_SOUP_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM.getTranslation().addZ(CAN_OF_SOUP_HEIGHT / 2.0);
   }
   public static final double MARKER_TO_CAN_OF_SOUP_X = 0.5;
   public static final RigidBodyTransform MARKER_TO_CAN_OF_SOUP_TRANSFORM = new RigidBodyTransform();
   public static final RigidBodyTransform CAN_OF_SOUP_TO_MARKER_TRANSFORM = new RigidBodyTransform();
   static
   {
      EuclidCoreMissingTools.setYawPitchRollDegrees(MARKER_TO_CAN_OF_SOUP_TRANSFORM.getRotation(), 0.0, -90.0, 0.0);
      MARKER_TO_CAN_OF_SOUP_TRANSFORM.getTranslation().set(0.0, -MARKER_TO_CAN_OF_SOUP_X, 0.0);
      CAN_OF_SOUP_TO_MARKER_TRANSFORM.setAndInvert(MARKER_TO_CAN_OF_SOUP_TRANSFORM);
   }

   public static void ensureNodesAdded(ROS2SceneGraph sceneGraph, Consumer<SceneGraphTreeModification> modificationQueue)
   {
      ArUcoMarkerNode boxArUcoMarker = sceneGraph.getArUcoMarkerIDToNodeMap().get(BOX_MARKER_ID);
      if (boxArUcoMarker != null)
      {
         SceneNode box = sceneGraph.getNamesToNodesMap().get("Box");
         if (box == null)
         {
            box = new PredefinedRigidBodySceneNode(sceneGraph.getNextID().getAndIncrement(),
                                                   "Box",
                                                   sceneGraph.getIDToNodeMap(),
                                                   boxArUcoMarker.getID(),
                                                   BOX_TO_MARKER_TRANSFORM,
                                                   BOX_VISUAL_MODEL_FILE_PATH,
                                                   BOX_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM);
            LogTools.info("Adding Box to scene graph.");
            modificationQueue.accept(new SceneGraphNodeAddition(box, boxArUcoMarker));
         }
      }

      ArUcoMarkerNode canOfSoupMarker = sceneGraph.getArUcoMarkerIDToNodeMap().get(CAN_OF_SOUP_MARKER_ID);
      if (canOfSoupMarker != null)
      {
         SceneNode canOfSoup = sceneGraph.getNamesToNodesMap().get("CanOfSoup");
         if (canOfSoup == null)
         {
            // Represents a can of soup detected by a statically nearby placed ArUco marker.
            canOfSoup = new PredefinedRigidBodySceneNode(sceneGraph.getNextID().getAndIncrement(),
                                                         "CanOfSoup",
                                                         sceneGraph.getIDToNodeMap(),
                                                         canOfSoupMarker.getID(),
                                                         CAN_OF_SOUP_TO_MARKER_TRANSFORM,
                                                         CAN_OF_SOUP_VISUAL_MODEL_FILE_PATH,
                                                         CAN_OF_SOUP_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM);
            LogTools.info("Adding CanOfSoup to scene graph.");
            modificationQueue.accept(new SceneGraphNodeAddition(canOfSoup, canOfSoupMarker));
         }
      }
   }
}
