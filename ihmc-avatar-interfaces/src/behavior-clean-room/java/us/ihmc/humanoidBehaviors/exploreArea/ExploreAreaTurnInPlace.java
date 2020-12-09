package us.ihmc.humanoidBehaviors.exploreArea;

import controller_msgs.msg.dds.FootstepDataListMessage;
import controller_msgs.msg.dds.FootstepDataMessage;
import controller_msgs.msg.dds.WalkingStatusMessage;
import us.ihmc.avatar.drcRobot.RemoteSyncedRobotModel;
import us.ihmc.commons.thread.TypedNotification;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.humanoidBehaviors.tools.BehaviorHelper;
import us.ihmc.humanoidBehaviors.tools.behaviorTree.ParallelNodeBasics;
import us.ihmc.humanoidRobotics.communication.packets.HumanoidMessageTools;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;

import java.util.ArrayList;

import static us.ihmc.humanoidBehaviors.exploreArea.ExploreAreaBehaviorAPI.CurrentState;

public class ExploreAreaTurnInPlace extends ParallelNodeBasics
{
   public static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final ExploreAreaBehaviorParameters parameters;
   private final BehaviorHelper helper;
   private final RemoteSyncedRobotModel syncedRobot;

   public ExploreAreaTurnInPlace(ExploreAreaBehaviorParameters parameters,
                                 BehaviorHelper helper)
   {
      this.parameters = parameters;
      this.helper = helper;

      syncedRobot = helper.getOrCreateRobotInterface().newSyncedRobot();
   }

   @Override
   public void doAction()
   {
      helper.publishToUI(CurrentState, ExploreAreaBehavior.ExploreAreaBehaviorState.TurnInPlace);

      ArrayList<Pose3D> posesFromThePreviousStep = new ArrayList<>();

      RobotSide supportSide = RobotSide.RIGHT;
      RobotSide initialSupportSide = supportSide;
      double rotationPerStepOutside = Math.PI / 4.0;
      double rotationPerStepInside = Math.PI / 8.0;

      Quaternion orientation = new Quaternion();
      orientation.setYawPitchRoll(rotationPerStepOutside, 0.0, 0.0);
      Point3D translation = new Point3D(0.0, supportSide.negateIfLeftSide(0.2), 0.0);
      posesFromThePreviousStep.add(new Pose3D(translation, orientation));

      supportSide = supportSide.getOppositeSide();
      orientation = new Quaternion();
      orientation.setYawPitchRoll(rotationPerStepInside, 0.0, 0.0);
      translation = new Point3D(0.0, supportSide.negateIfLeftSide(0.2), 0.0);
      posesFromThePreviousStep.add(new Pose3D(translation, orientation));

      supportSide = supportSide.getOppositeSide();
      orientation = new Quaternion();
      orientation.setYawPitchRoll(rotationPerStepOutside, 0.0, 0.0);
      translation = new Point3D(0.0, supportSide.negateIfLeftSide(0.2), 0.0);
      posesFromThePreviousStep.add(new Pose3D(translation, orientation));

      supportSide = supportSide.getOppositeSide();
      orientation = new Quaternion();
      translation = new Point3D(0.0, supportSide.negateIfLeftSide(0.2), 0.0);
      posesFromThePreviousStep.add(new Pose3D(translation, orientation));

      TypedNotification<WalkingStatusMessage> walkingCompleted = requestWalk(initialSupportSide, posesFromThePreviousStep);
      walkingCompleted.blockingPoll(); // TODO: Timeout after a while
      // If times out, return failure.
   }

   private TypedNotification<WalkingStatusMessage> requestWalk(RobotSide supportSide, ArrayList<Pose3D> posesFromThePreviousStep)
   {
      RobotSide swingSide = supportSide.getOppositeSide();
      FootstepDataListMessage footstepDataListMessageToTurnInPlace = new FootstepDataListMessage();
      us.ihmc.idl.IDLSequence.Object<FootstepDataMessage> footstepDataList = footstepDataListMessageToTurnInPlace.getFootstepDataList();
      syncedRobot.update();
      ReferenceFrame supportFootFrame = syncedRobot.getReferenceFrames().getSoleFrame(supportSide);

      for (Pose3D pose : posesFromThePreviousStep)
      {
         FramePose3D nextStepFramePose = new FramePose3D(supportFootFrame, pose);
         PoseReferenceFrame nextStepFrame = new PoseReferenceFrame("nextStepFrame", nextStepFramePose);

         nextStepFramePose.changeFrame(worldFrame);

         FootstepDataMessage footstepMessage = footstepDataList.add();
         FootstepDataMessage footstep = HumanoidMessageTools.createFootstepDataMessage(swingSide, nextStepFramePose);
         footstepMessage.set(footstep);

         supportSide = supportSide.getOppositeSide();
         swingSide = swingSide.getOppositeSide();
         supportFootFrame = nextStepFrame;
      }

      return helper.getOrCreateRobotInterface().requestWalk(footstepDataListMessageToTurnInPlace);
   }
}
