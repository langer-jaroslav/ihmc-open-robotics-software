package us.ihmc.rdx.ui.vr;

import us.ihmc.avatar.drcRobot.ROS2SyncedRobotModel;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.log.LogTools;
import us.ihmc.motionRetargeting.RetargetingParameters;
import us.ihmc.motionRetargeting.VRTrackedSegmentType;
import us.ihmc.robotics.referenceFrames.MutableReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrameMissingTools;
import us.ihmc.robotics.robotSide.RobotSide;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static us.ihmc.motionRetargeting.VRTrackedSegmentType.*;

/**
 * Class responsible for motion retargeting from VR tracked segments to a robot model.
 * It adjusts the robot's pelvis and center of mass based on the tracked segments.
 */
public class RDXVRMotionRetargeting
{
   // Set of trackers whose references are unchanged during retargeting
   private final Set<VRTrackedSegmentType> UNCHANGED_TRACKER_REFERENCES = new HashSet<>()
   {
      {
         add(CHEST);
         add(LEFT_WRIST);
         add(RIGHT_WRIST);
      }
   };
   private final ROS2SyncedRobotModel syncedRobot;
   private final Map<String, MutableReferenceFrame> trackerReferenceFrames;
   private final RetargetingParameters retargetingParameters;

   private final RigidBodyTransform initialWaistTrackerTransformToWorld = new RigidBodyTransform();
   private final FramePose3D newPelvisFramePose = new FramePose3D();
   private final Map<VRTrackedSegmentType, ReferenceFrame> desiredFrames = new HashMap<>();
   private final RigidBodyTransform initialPelvisTransformToWorld = new RigidBodyTransform();
   private Point3D centerOfMassDesiredXYInWorld;
   private ReferenceFrame initialPelvisFrame;
   private boolean controlArmsOnly = false;

   /**
    * Constructor for the motion retargeting class.
    *
    * @param syncedRobot the synchronized robot model
    * @param trackerReferenceFrames the reference frames of the trackers
    * @param retargetingParameters the retargeting parameters
    */
   public RDXVRMotionRetargeting(ROS2SyncedRobotModel syncedRobot, Map<String, MutableReferenceFrame> trackerReferenceFrames,
                                 RetargetingParameters retargetingParameters)
   {
      this.syncedRobot = syncedRobot;
      this.retargetingParameters = retargetingParameters;
      this.trackerReferenceFrames = trackerReferenceFrames;
   }

   /**
    * Computes the desired values for retargeting.
    * Updates the pelvis and center of mass.
    */
   public void computeDesiredValues()
   {
      desiredFrames.clear();
      retargetPelvis();
      retargetCoM();
   }

   /**
    * Compute the desired pose of pelvis.
    * Scale the pelvis height based on ratio user/robot height and use incremental retargeting.
    */
   private void retargetPelvis()
   {
      if (trackerReferenceFrames.containsKey(WAIST.getSegmentName()) && !controlArmsOnly)
      {
         if (initialPelvisFrame == null)
         {
            initialPelvisTransformToWorld.set(syncedRobot.getFullRobotModel().getPelvis().getBodyFixedFrame().getTransformToWorldFrame());
            initialPelvisFrame = ReferenceFrameMissingTools.constructFrameWithUnchangingTransformToParent(ReferenceFrame.getWorldFrame(),
                                                                                                          initialPelvisTransformToWorld);
            initialWaistTrackerTransformToWorld.set(trackerReferenceFrames.get(WAIST.getSegmentName()).getReferenceFrame().getTransformToWorldFrame());
         }
         // Calculate the variation of the tracker's frame from its initial value
         RigidBodyTransform waistTrackerVariationFromInitialValue = new RigidBodyTransform(trackerReferenceFrames.get(WAIST.getSegmentName())
                                                                                                                 .getReferenceFrame()
                                                                                                                 .getTransformToWorldFrame());
         // Get variation from initial value
         initialWaistTrackerTransformToWorld.inverseTransform(waistTrackerVariationFromInitialValue);
         // Scale the z waist variation
         double scalingRobotHumanWaistHeight = retargetingParameters.getPelvisHeightExtendedLegs() / initialWaistTrackerTransformToWorld.getTranslationZ();
         waistTrackerVariationFromInitialValue.getTranslation()
                                              .setZ(scalingRobotHumanWaistHeight * waistTrackerVariationFromInitialValue.getTranslationZ());

         // Concatenate the initial pelvis transform with the variation
         RigidBodyTransform combinedTransformToWorld = new RigidBodyTransform(initialPelvisTransformToWorld);
         combinedTransformToWorld.multiply(waistTrackerVariationFromInitialValue);

         newPelvisFramePose.changeFrame(ReferenceFrame.getWorldFrame());
         newPelvisFramePose.set(combinedTransformToWorld);
         // Zero roll orientation variation as it can lead to very unnatural motions (at least when in double support)
         newPelvisFramePose.changeFrame(initialPelvisFrame);
         newPelvisFramePose.getRotation().setYawPitchRoll(newPelvisFramePose.getRotation().getYaw(), newPelvisFramePose.getRotation().getPitch(),0.0);
         newPelvisFramePose.changeFrame(ReferenceFrame.getWorldFrame());

         desiredFrames.put(WAIST, ReferenceFrameMissingTools.constructFrameWithUnchangingTransformToParent(ReferenceFrame.getWorldFrame(), newPelvisFramePose));
      }
   }

   /**
    * Compute desired CoM for the robot based on ankles and waist tracker on user.
    * Use center of mass normalized offset computation.
    */
   private void retargetCoM()
   {
      if (trackerReferenceFrames.containsKey(WAIST.getSegmentName())
          && trackerReferenceFrames.containsKey(LEFT_ANKLE.getSegmentName())
          && trackerReferenceFrames.containsKey(RIGHT_ANKLE.getSegmentName()) && !controlArmsOnly)
      {
         if (centerOfMassDesiredXYInWorld == null)
         {
            centerOfMassDesiredXYInWorld = new Point3D();
            centerOfMassDesiredXYInWorld.set(syncedRobot.getReferenceFrames().getMidFeetZUpFrame().getTransformToWorldFrame().getTranslation());
         }
         // Fetch the current frames for left and right ankle
         ReferenceFrame leftAnkleFrame = trackerReferenceFrames.get(LEFT_ANKLE.getSegmentName()).getReferenceFrame();
         ReferenceFrame rightAnkleFrame = trackerReferenceFrames.get(RIGHT_ANKLE.getSegmentName()).getReferenceFrame();

         // Get the waist tracker ground projection of the user (assuming this can approximate the ground projection of the center of mass)
         Point3D waistTrackerXYInWorld = new Point3D(trackerReferenceFrames.get(WAIST.getSegmentName()).getReferenceFrame().getTransformToWorldFrame().getTranslation());
         waistTrackerXYInWorld.setZ(0.0);

         // Get the positions of the left and right foot on the ground
         Point3D leftAnkleTrackerXYInWorld = new Point3D(leftAnkleFrame.getTransformToWorldFrame().getTranslation());
         leftAnkleTrackerXYInWorld.setZ(0.0);
         Point3D rightAnkleTrackerXYInWorld = new Point3D(rightAnkleFrame.getTransformToWorldFrame().getTranslation());
         rightAnkleTrackerXYInWorld.setZ(0.0);

         // Calculate the vector between the ankles (feet)
         Vector3D virtualFeetVector = new Vector3D();
         virtualFeetVector.sub(rightAnkleTrackerXYInWorld, leftAnkleTrackerXYInWorld);

         // Project the waist tracker ground projection onto the line between the feet
         Vector3D projectionVector = new Vector3D(waistTrackerXYInWorld);
         projectionVector.sub(leftAnkleTrackerXYInWorld);
         double dotProduct = projectionVector.dot(virtualFeetVector);
         double normSqFeetVector = virtualFeetVector.lengthSquared();

         // Calculate normalized offset along the line connecting the feet
         double normalizedOffset = dotProduct / normSqFeetVector;

         Point3D leftFootXYInWorld = new Point3D(syncedRobot.getFullRobotModel().getSoleFrame(RobotSide.LEFT).getTransformToWorldFrame().getTranslation());
         Point3D rightFootXYInWorld = new Point3D(syncedRobot.getFullRobotModel().getSoleFrame(RobotSide.RIGHT).getTransformToWorldFrame().getTranslation());

         // Reconstruct robot CoM based on the normalized offset and its feet position
         Vector3D feetVector = new Vector3D();
         feetVector.sub(rightFootXYInWorld, leftFootXYInWorld);

         LogTools.info("MidFeet: {}", syncedRobot.getReferenceFrames().getMidFeetZUpFrame().getTransformToWorldFrame().getTranslation());
         LogTools.info("COM: {}", centerOfMassDesiredXYInWorld);
         LogTools.info("Offset: {}", normalizedOffset);
         centerOfMassDesiredXYInWorld.set(feetVector);
         centerOfMassDesiredXYInWorld.scale(normalizedOffset);
         centerOfMassDesiredXYInWorld.add(leftFootXYInWorld);
      }
   }

   public void setControlArmsOnly(boolean controlArmsOnly)
   {
      this.controlArmsOnly = controlArmsOnly;
   }

   public void reset()
   {
      initialPelvisFrame = null;
      desiredFrames.clear();
      centerOfMassDesiredXYInWorld = null;
   }

   public Set<VRTrackedSegmentType> getRetargetedSegments()
   {
      return desiredFrames.keySet();
   }

   public ReferenceFrame getDesiredFrame(VRTrackedSegmentType segment)
   {
      return desiredFrames.get(segment);
   }

   public Point3D getDesiredCenterOfMassXYInWorld()
   {
      return centerOfMassDesiredXYInWorld;
   }

   public boolean isCenterOfMassAvailable()
   {
      return centerOfMassDesiredXYInWorld != null;
   }

   public boolean isRetargetingNotNeeded(VRTrackedSegmentType segment)
   {
      return UNCHANGED_TRACKER_REFERENCES.contains(segment);
   }
}
