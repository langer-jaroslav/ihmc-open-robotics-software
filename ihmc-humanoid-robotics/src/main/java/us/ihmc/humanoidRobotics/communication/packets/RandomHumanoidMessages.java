package us.ihmc.humanoidRobotics.communication.packets;

import static us.ihmc.euclid.tools.EuclidCoreRandomTools.nextPoint2D;
import static us.ihmc.humanoidRobotics.communication.packets.walking.CapturabilityBasedStatus.MAXIMUM_NUMBER_OF_VERTICES;

import java.util.ArrayList;
import java.util.Random;
import java.util.stream.IntStream;

import us.ihmc.commons.RandomNumbers;
import us.ihmc.communication.packets.BoundingBoxesPacket;
import us.ihmc.communication.packets.ExecutionMode;
import us.ihmc.communication.packets.ExecutionTiming;
import us.ihmc.communication.packets.HeatMapPacket;
import us.ihmc.communication.packets.IMUPacket;
import us.ihmc.communication.packets.KinematicsToolboxOutputStatus;
import us.ihmc.communication.packets.LidarScanParametersMessage;
import us.ihmc.communication.packets.MessageTools;
import us.ihmc.communication.packets.ObjectDetectorResultPacket;
import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.packets.QueueableMessage;
import us.ihmc.communication.packets.SelectionMatrix3DMessage;
import us.ihmc.communication.packets.SimulatedLidarScanPacket;
import us.ihmc.communication.packets.SpatialVectorMessage;
import us.ihmc.communication.packets.WeightMatrix3DMessage;
import us.ihmc.communication.producers.VideoSource;
import us.ihmc.euclid.geometry.Pose2D;
import us.ihmc.euclid.geometry.tools.EuclidGeometryRandomTools;
import us.ihmc.euclid.tools.EuclidCoreRandomTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D32;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.footstepPlanning.FootstepPlanningResult;
import us.ihmc.humanoidRobotics.communication.packets.atlas.AtlasLowLevelControlMode;
import us.ihmc.humanoidRobotics.communication.packets.atlas.AtlasLowLevelControlModeMessage;
import us.ihmc.humanoidRobotics.communication.packets.bdi.BDIBehaviorCommandPacket;
import us.ihmc.humanoidRobotics.communication.packets.bdi.BDIBehaviorStatusPacket;
import us.ihmc.humanoidRobotics.communication.packets.bdi.BDIRobotBehavior;
import us.ihmc.humanoidRobotics.communication.packets.behaviors.BehaviorControlModeEnum;
import us.ihmc.humanoidRobotics.communication.packets.behaviors.BehaviorControlModePacket;
import us.ihmc.humanoidRobotics.communication.packets.behaviors.BehaviorControlModeResponsePacket;
import us.ihmc.humanoidRobotics.communication.packets.behaviors.HumanoidBehaviorType;
import us.ihmc.humanoidRobotics.communication.packets.behaviors.HumanoidBehaviorTypePacket;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HandConfiguration;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HighLevelControllerName;
import us.ihmc.humanoidRobotics.communication.packets.driving.VehiclePosePacket;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.ArmDesiredAccelerationsMessage;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.ArmTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.AtlasDesiredPumpPSIPacket;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.AtlasElectricMotorAutoEnableFlagPacket;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.AtlasElectricMotorEnablePacket;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.AtlasElectricMotorPacketEnum;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.AtlasWristSensorCalibrationRequestPacket;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandDesiredConfigurationMessage;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandJointAnglePacket;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandLoadBearingMessage;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandPowerCyclePacket;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.ManualHandControlPacket;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.ObjectWeightPacket;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.OneDoFJointTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.StopAllTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.momentum.CenterOfMassTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.momentum.MomentumTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.sensing.BlackFlyParameterPacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.LocalizationPacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.LocalizationPointMapPacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.LocalizationStatusPacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.MultisenseParameterPacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.PelvisPoseErrorPacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.PointCloudWorldPacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.VideoPacket;
import us.ihmc.humanoidRobotics.communication.packets.walking.AbortWalkingMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.AdjustFootstepMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.CapturabilityBasedStatus;
import us.ihmc.humanoidRobotics.communication.packets.walking.ChestTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootLoadBearingMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataListMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepPathPlanPacket;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepPlanRequestPacket;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepPlanRequestType;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepPlanningToolboxOutputStatus;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepStatus;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepStatusMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.GoHomeMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.HeadTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.HumanoidBodyPart;
import us.ihmc.humanoidRobotics.communication.packets.walking.LoadBearingRequest;
import us.ihmc.humanoidRobotics.communication.packets.walking.NeckDesiredAccelerationsMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.NeckTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PauseWalkingMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisHeightTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisOrientationTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PrepareForLocomotionMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.SnapFootstepPacket;
import us.ihmc.humanoidRobotics.communication.packets.walking.SpineDesiredAccelerationsMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.SpineTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.WalkingStatus;
import us.ihmc.humanoidRobotics.communication.packets.walking.WalkingStatusMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.hybridRigidBodyManager.ChestHybridJointspaceTaskspaceTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.hybridRigidBodyManager.HandHybridJointspaceTaskspaceTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.hybridRigidBodyManager.HeadHybridJointspaceTaskspaceTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.wholebody.WholeBodyTrajectoryMessage;
import us.ihmc.robotics.random.RandomGeometry;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.trajectories.TrajectoryType;
import us.ihmc.sensorProcessing.communication.packets.dataobjects.RobotConfigurationData;
import us.ihmc.sensorProcessing.model.RobotMotionStatus;

public final class RandomHumanoidMessages
{
   private RandomHumanoidMessages()
   {
   }

   public static QueueableMessage nextQueueableMessage(Random random)
   {
      QueueableMessage next = new QueueableMessage();
      next.setExecutionMode(RandomNumbers.nextEnum(random, ExecutionMode.class).toByte());
      next.setPreviousMessageId(random.nextLong());
      next.executionDelayTime = RandomNumbers.nextDoubleWithEdgeCases(random, 0.1);
      return next;
   }

   public static FrameInformation nextFrameInformation(Random random)
   {
      FrameInformation next = new FrameInformation();
      next.trajectoryReferenceFrameId = random.nextLong();
      next.dataReferenceFrameId = random.nextLong();
      return next;
   }

   public static SelectionMatrix3DMessage nextSelectionMatrix3DMessage(Random random)
   {
      SelectionMatrix3DMessage next = new SelectionMatrix3DMessage();
      next.selectionFrameId = random.nextLong();
      next.xSelected = random.nextBoolean();
      next.ySelected = random.nextBoolean();
      next.zSelected = random.nextBoolean();
      return next;
   }

   public static WeightMatrix3DMessage nextWeightMatrix3DMessage(Random random)
   {
      WeightMatrix3DMessage next = new WeightMatrix3DMessage();
      next.weightFrameId = random.nextLong();
      next.xWeight = RandomNumbers.nextDoubleWithEdgeCases(random, 0.1);
      next.yWeight = RandomNumbers.nextDoubleWithEdgeCases(random, 0.1);
      next.zWeight = RandomNumbers.nextDoubleWithEdgeCases(random, 0.1);
      return next;
   }

   public static TrajectoryPoint1DMessage nextTrajectoryPoint1DMessage(Random random)
   {
      TrajectoryPoint1DMessage next = new TrajectoryPoint1DMessage();
      next.setTime(RandomNumbers.nextDoubleWithEdgeCases(random, 0.01));
      next.setPosition(RandomNumbers.nextDoubleWithEdgeCases(random, 0.01));
      next.setVelocity(RandomNumbers.nextDoubleWithEdgeCases(random, 0.01));
      return next;
   }

   public static TrajectoryPoint1DMessage[] nextTrajectoryPoint1DMessages(Random random)
   {
      return nextTrajectoryPoint1DMessages(random, random.nextInt(16) + 1);
   }

   public static TrajectoryPoint1DMessage[] nextTrajectoryPoint1DMessages(Random random, int length)
   {
      TrajectoryPoint1DMessage[] next = new TrajectoryPoint1DMessage[length];
      for (int i = 0; i < length; i++)
         next[i] = nextTrajectoryPoint1DMessage(random);
      return next;
   }

   public static OneDoFJointTrajectoryMessage nextOneDoFJointTrajectoryMessage(Random random)
   {
      OneDoFJointTrajectoryMessage next = new OneDoFJointTrajectoryMessage();
      MessageTools.copyData(nextTrajectoryPoint1DMessages(random), next.trajectoryPoints);
      next.weight = RandomNumbers.nextDoubleWithEdgeCases(random, 0.1);
      return next;
   }

   public static OneDoFJointTrajectoryMessage[] nextOneDoFJointTrajectoryMessages(Random random)
   {
      return nextOneDoFJointTrajectoryMessages(random, random.nextInt(10) + 1);
   }

   public static OneDoFJointTrajectoryMessage[] nextOneDoFJointTrajectoryMessages(Random random, int length)
   {
      OneDoFJointTrajectoryMessage[] next = new OneDoFJointTrajectoryMessage[length];
      for (int i = 0; i < length; i++)
         next[i] = nextOneDoFJointTrajectoryMessage(random);
      return next;
   }

   public static JointspaceTrajectoryMessage nextJointspaceTrajectoryMessage(Random random)
   {
      JointspaceTrajectoryMessage next = new JointspaceTrajectoryMessage();
      next.queueingProperties = nextQueueableMessage(random);
      MessageTools.copyData(nextOneDoFJointTrajectoryMessages(random), next.jointTrajectoryMessages);
      return next;
   }

   public static ArmTrajectoryMessage nextArmTrajectoryMessage(Random random)
   {
      ArmTrajectoryMessage next = new ArmTrajectoryMessage();
      next.jointspaceTrajectory = RandomHumanoidMessages.nextJointspaceTrajectoryMessage(random);
      next.robotSide = RandomNumbers.nextEnum(random, RobotSide.class).toByte();
      return next;
   }

   public static SE3TrajectoryPointMessage nextSE3TrajectoryPointMessage(Random random)
   {
      SE3TrajectoryPointMessage next = new SE3TrajectoryPointMessage();
      next.time = RandomNumbers.nextDoubleWithEdgeCases(random, 0.01);
      next.position = RandomGeometry.nextPoint3D(random, 1.0, 1.0, 1.0);
      next.orientation = RandomGeometry.nextQuaternion(random);
      next.linearVelocity = RandomGeometry.nextVector3D(random);
      next.angularVelocity = RandomGeometry.nextVector3D(random);
      return next;
   }

   public static SE3TrajectoryPointMessage[] nextSE3TrajectoryPointMessages(Random random)
   {
      return nextSE3TrajectoryPointMessages(random, random.nextInt(16) + 1);
   }

   public static SE3TrajectoryPointMessage[] nextSE3TrajectoryPointMessages(Random random, int length)
   {
      SE3TrajectoryPointMessage[] next = new SE3TrajectoryPointMessage[length];
      for (int i = 0; i < length; i++)
      {
         next[i] = nextSE3TrajectoryPointMessage(random);
      }
      return next;
   }

   public static SE3TrajectoryMessage nextSE3TrajectoryMessage(Random random)
   {
      SE3TrajectoryMessage next = new SE3TrajectoryMessage();
      MessageTools.copyData(nextSE3TrajectoryPointMessages(random), next.taskspaceTrajectoryPoints);
      next.angularSelectionMatrix = nextSelectionMatrix3DMessage(random);
      next.linearSelectionMatrix = nextSelectionMatrix3DMessage(random);
      next.frameInformation = nextFrameInformation(random);
      next.angularWeightMatrix = nextWeightMatrix3DMessage(random);
      next.linearWeightMatrix = nextWeightMatrix3DMessage(random);
      next.useCustomControlFrame = random.nextBoolean();
      next.controlFramePose = EuclidGeometryRandomTools.nextPose3D(random);
      next.queueingProperties = nextQueueableMessage(random);
      return next;
   }

   public static PelvisTrajectoryMessage nextPelvisTrajectoryMessage(Random random)
   {
      PelvisTrajectoryMessage next = new PelvisTrajectoryMessage();
      next.enableUserPelvisControl = random.nextBoolean();
      next.enableUserPelvisControlDuringWalking = random.nextBoolean();
      next.se3Trajectory = nextSE3TrajectoryMessage(random);
      return next;
   }

   public static HandTrajectoryMessage nextHandTrajectoryMessage(Random random)
   {
      HandTrajectoryMessage next = new HandTrajectoryMessage();
      next.robotSide = RandomNumbers.nextEnum(random, RobotSide.class).toByte();
      next.se3Trajectory = nextSE3TrajectoryMessage(random);
      return next;
   }

   public static FootTrajectoryMessage nextFootTrajectoryMessage(Random random)
   {
      FootTrajectoryMessage next = new FootTrajectoryMessage();
      next.robotSide = RandomNumbers.nextEnum(random, RobotSide.class).toByte();
      next.se3Trajectory = nextSE3TrajectoryMessage(random);
      return next;
   }

   public static HandHybridJointspaceTaskspaceTrajectoryMessage nextHandHybridJointspaceTaskspaceTrajectoryMessage(Random random)
   {
      HandHybridJointspaceTaskspaceTrajectoryMessage next = new HandHybridJointspaceTaskspaceTrajectoryMessage();
      next.robotSide = RandomNumbers.nextEnum(random, RobotSide.class).toByte();
      next.taskspaceTrajectoryMessage = RandomHumanoidMessages.nextSE3TrajectoryMessage(random);
      next.jointspaceTrajectoryMessage = RandomHumanoidMessages.nextJointspaceTrajectoryMessage(random);
      next.jointspaceTrajectoryMessage.queueingProperties.set(next.taskspaceTrajectoryMessage.getQueueingProperties());
      return next;
   }

   public static HeadHybridJointspaceTaskspaceTrajectoryMessage nextHeadHybridJointspaceTaskspaceTrajectoryMessage(Random random)
   {
      HeadHybridJointspaceTaskspaceTrajectoryMessage next = new HeadHybridJointspaceTaskspaceTrajectoryMessage();
      next.taskspaceTrajectoryMessage = RandomHumanoidMessages.nextSO3TrajectoryMessage(random);
      next.jointspaceTrajectoryMessage = RandomHumanoidMessages.nextJointspaceTrajectoryMessage(random);
      next.jointspaceTrajectoryMessage.queueingProperties.set(next.taskspaceTrajectoryMessage.getQueueingProperties());
      return next;
   }

   public static ChestHybridJointspaceTaskspaceTrajectoryMessage nextChestHybridJointspaceTaskspaceTrajectoryMessage(Random random)
   {
      ChestHybridJointspaceTaskspaceTrajectoryMessage next = new ChestHybridJointspaceTaskspaceTrajectoryMessage();
      next.taskspaceTrajectoryMessage = RandomHumanoidMessages.nextSO3TrajectoryMessage(random);
      next.jointspaceTrajectoryMessage = RandomHumanoidMessages.nextJointspaceTrajectoryMessage(random);
      next.jointspaceTrajectoryMessage.queueingProperties.set(next.taskspaceTrajectoryMessage.getQueueingProperties());
      return next;
   }

   public static SO3TrajectoryPointMessage nextSO3TrajectoryPointMessage(Random random)
   {
      SO3TrajectoryPointMessage next = new SO3TrajectoryPointMessage();
      next.time = RandomNumbers.nextDoubleWithEdgeCases(random, 0.01);
      next.orientation = RandomGeometry.nextQuaternion(random);
      next.angularVelocity = RandomGeometry.nextVector3D(random);
      return next;
   }

   public static SO3TrajectoryPointMessage[] nextSO3TrajectoryPointMessages(Random random)
   {
      return nextSO3TrajectoryPointMessages(random, random.nextInt(16) + 1);
   }

   public static SO3TrajectoryPointMessage[] nextSO3TrajectoryPointMessages(Random random, int length)
   {
      SO3TrajectoryPointMessage[] next = new SO3TrajectoryPointMessage[length];
      for (int i = 0; i < length; i++)
      {
         next[i] = nextSO3TrajectoryPointMessage(random);
      }
      return next;
   }

   public static SO3TrajectoryMessage nextSO3TrajectoryMessage(Random random)
   {
      SO3TrajectoryMessage next = new SO3TrajectoryMessage();
      MessageTools.copyData(nextSO3TrajectoryPointMessages(random), next.taskspaceTrajectoryPoints);
      next.frameInformation = nextFrameInformation(random);
      next.selectionMatrix = nextSelectionMatrix3DMessage(random);
      next.weightMatrix = nextWeightMatrix3DMessage(random);
      next.useCustomControlFrame = random.nextBoolean();
      next.controlFramePose = EuclidGeometryRandomTools.nextPose3D(random);
      next.queueingProperties = nextQueueableMessage(random);
      return next;
   }

   public static HeadTrajectoryMessage nextHeadTrajectoryMessage(Random random)
   {
      HeadTrajectoryMessage next = new HeadTrajectoryMessage();
      next.so3Trajectory = nextSO3TrajectoryMessage(random);
      return next;
   }

   public static PelvisOrientationTrajectoryMessage nextPelvisOrientationTrajectoryMessage(Random random)
   {
      PelvisOrientationTrajectoryMessage next = new PelvisOrientationTrajectoryMessage();
      next.so3Trajectory = nextSO3TrajectoryMessage(random);
      next.enableUserPelvisControlDuringWalking = random.nextBoolean();
      return next;
   }

   public static ChestTrajectoryMessage nextChestTrajectoryMessage(Random random)
   {
      ChestTrajectoryMessage next = new ChestTrajectoryMessage();
      next.so3Trajectory = nextSO3TrajectoryMessage(random);
      return next;
   }

   public static WholeBodyTrajectoryMessage nextWholeBodyTrajectoryMessage(Random random)
   {
      WholeBodyTrajectoryMessage next = new WholeBodyTrajectoryMessage();
      next.leftHandTrajectoryMessage = RandomHumanoidMessages.nextHandTrajectoryMessage(random);
      next.leftHandTrajectoryMessage.robotSide = RobotSide.LEFT.toByte();
      next.rightHandTrajectoryMessage = RandomHumanoidMessages.nextHandTrajectoryMessage(random);
      next.rightHandTrajectoryMessage.robotSide = RobotSide.RIGHT.toByte();
      next.leftArmTrajectoryMessage = RandomHumanoidMessages.nextArmTrajectoryMessage(random);
      next.leftArmTrajectoryMessage.robotSide = RobotSide.LEFT.toByte();
      next.rightArmTrajectoryMessage = RandomHumanoidMessages.nextArmTrajectoryMessage(random);
      next.rightArmTrajectoryMessage.robotSide = RobotSide.RIGHT.toByte();
      next.leftFootTrajectoryMessage = RandomHumanoidMessages.nextFootTrajectoryMessage(random);
      next.leftFootTrajectoryMessage.robotSide = RobotSide.LEFT.toByte();
      next.rightFootTrajectoryMessage = RandomHumanoidMessages.nextFootTrajectoryMessage(random);
      next.rightFootTrajectoryMessage.robotSide = RobotSide.RIGHT.toByte();
      next.chestTrajectoryMessage = RandomHumanoidMessages.nextChestTrajectoryMessage(random);
      next.pelvisTrajectoryMessage = RandomHumanoidMessages.nextPelvisTrajectoryMessage(random);
      next.headTrajectoryMessage = RandomHumanoidMessages.nextHeadTrajectoryMessage(random);
      return next;
   }

   public static NeckTrajectoryMessage nextNeckTrajectoryMessage(Random random)
   {
      NeckTrajectoryMessage next = new NeckTrajectoryMessage();
      next.jointspaceTrajectory = nextJointspaceTrajectoryMessage(random);
      return next;
   }

   public static SpineTrajectoryMessage nextSpineTrajectoryMessage(Random random)
   {
      SpineTrajectoryMessage next = new SpineTrajectoryMessage();
      next.jointspaceTrajectory = nextJointspaceTrajectoryMessage(random);
      return next;
   }

   public static LoadBearingMessage nextLoadBearingMessage(Random random)
   {
      LoadBearingMessage next = new LoadBearingMessage();
      next.load = random.nextBoolean();
      next.coefficientOfFriction = RandomNumbers.nextDoubleWithEdgeCases(random, 0.1);
      next.bodyFrameToContactFrame = EuclidGeometryRandomTools.nextPose3D(random);
      next.contactNormalInWorldFrame = EuclidCoreRandomTools.nextVector3D(random);
      return next;
   }

   public static HandLoadBearingMessage nextHandLoadBearingMessage(Random random)
   {
      HandLoadBearingMessage next = new HandLoadBearingMessage();
      next.robotSide = RobotSide.generateRandomRobotSide(random).toByte();
      next.useJointspaceCommand = random.nextBoolean();
      next.jointspaceTrajectory = nextJointspaceTrajectoryMessage(random);
      next.executionDelayTime = RandomNumbers.nextDoubleWithEdgeCases(random, 0.1);
      next.loadBearingMessage = nextLoadBearingMessage(random);
      return next;
   }

   public static FootLoadBearingMessage nextFootLoadBearingMessage(Random random)
   {
      FootLoadBearingMessage next = new FootLoadBearingMessage();
      next.robotSide = RobotSide.generateRandomRobotSide(random).toByte();
      next.executionDelayTime = RandomNumbers.nextDoubleWithEdgeCases(random, 0.1);
      next.loadBearingRequest = RandomNumbers.nextEnum(random, LoadBearingRequest.class).toByte();
      return next;
   }

   public static FootstepDataMessage nextFootstepDataMessage(Random random)
   {
      FootstepDataMessage next = new FootstepDataMessage();
      next.robotSide = RobotSide.generateRandomRobotSide(random).toByte();
      next.location = EuclidCoreRandomTools.nextPoint3D(random);
      next.orientation = EuclidCoreRandomTools.nextQuaternion(random);
      IntStream.range(0, random.nextInt(10)).forEach(i -> next.predictedContactPoints.add().set(EuclidCoreRandomTools.nextPoint2D(random)));
      next.trajectoryType = RandomNumbers.nextEnum(random, TrajectoryType.class).toByte();
      next.swingHeight = RandomNumbers.nextDoubleWithEdgeCases(random, 0.1);
      if (next.trajectoryType == TrajectoryType.CUSTOM.toByte())
      {
         next.customPositionWaypoints.add().set(RandomGeometry.nextPoint3D(random, -10.0, 10.0));
         next.customPositionWaypoints.add().set(RandomGeometry.nextPoint3D(random, -10.0, 10.0));
      }
      else if (next.trajectoryType == TrajectoryType.WAYPOINTS.toByte())
      {
         MessageTools.copyData(nextSE3TrajectoryPointMessages(random), next.swingTrajectory);
      }
      next.swingTrajectoryBlendDuration = RandomNumbers.nextDoubleWithEdgeCases(random, 0.1);
      next.swingDuration = RandomNumbers.nextDoubleWithEdgeCases(random, 0.1);
      next.transferDuration = RandomNumbers.nextDoubleWithEdgeCases(random, 0.1);
      next.touchdownDuration = RandomNumbers.nextDoubleWithEdgeCases(random, 0.1);
      next.executionDelayTime = RandomNumbers.nextDoubleWithEdgeCases(random, 0.1);
      return next;
   }

   public static ArrayList<FootstepDataMessage> nextFootstepDataMessages(Random random)
   {
      return nextFootstepDataMessages(random, random.nextInt(16) + 1);
   }

   public static ArrayList<FootstepDataMessage> nextFootstepDataMessages(Random random, int length)
   {
      ArrayList<FootstepDataMessage> next = new ArrayList<>();
      for (int i = 0; i < length; i++)
         next.add(nextFootstepDataMessage(random));
      return next;
   }

   public static FootstepDataListMessage nextFootstepDataListMessage(Random random)
   {
      FootstepDataListMessage next = new FootstepDataListMessage();
      MessageTools.copyData(nextFootstepDataMessages(random), next.footstepDataList);
      next.executionTiming = RandomNumbers.nextEnum(random, ExecutionTiming.class).toByte();
      next.defaultSwingDuration = RandomNumbers.nextDoubleWithEdgeCases(random, 0.1);
      next.defaultTransferDuration = RandomNumbers.nextDoubleWithEdgeCases(random, 0.1);
      next.finalTransferDuration = RandomNumbers.nextDoubleWithEdgeCases(random, 0.1);
      next.trustHeightOfFootsteps = random.nextBoolean();
      next.areFootstepsAdjustable = random.nextBoolean();
      next.offsetFootstepsWithExecutionError = random.nextBoolean();
      next.queueingProperties = nextQueueableMessage(random);
      return next;
   }

   public static HumanoidBehaviorTypePacket nextHumanoidBehaviorTypePacket(Random random)
   {
      HumanoidBehaviorTypePacket next = new HumanoidBehaviorTypePacket();
      next.humanoidBehaviorType = RandomNumbers.nextEnum(random, HumanoidBehaviorType.class).toByte();
      return next;
   }

   public static IMUPacket nextIMUPacket(Random random)
   {
      IMUPacket next = new IMUPacket();
      next.linearAcceleration = EuclidCoreRandomTools.nextVector3D32(random);
      next.orientation = EuclidCoreRandomTools.nextQuaternion32(random);
      next.angularVelocity = EuclidCoreRandomTools.nextVector3D32(random);
      next.time = random.nextDouble();
      return next;
   }

   public static IMUPacket[] nextIMUPackets(Random random)
   {
      return nextIMUPackets(random, random.nextInt(16) + 1);
   }

   public static IMUPacket[] nextIMUPackets(Random random, int length)
   {
      IMUPacket[] next = new IMUPacket[length];
      for (int i = 0; i < length; i++)
      {
         next[i] = nextIMUPacket(random);
      }
      return next;
   }

   public static RobotConfigurationData nextRobotConfigurationData(Random random)
   {
      RobotConfigurationData next = new RobotConfigurationData();
      int size = random.nextInt(10000);
      next.timestamp = random.nextLong();
      next.sensorHeadPPSTimestamp = random.nextLong();
      next.jointNameHash = random.nextInt(10000);
      next.jointAngles.add(RandomNumbers.nextFloatArray(random, size, 1.0f));
      next.jointVelocities.add(RandomNumbers.nextFloatArray(random, size, 1.0f));
      next.jointTorques.add(RandomNumbers.nextFloatArray(random, size, 1.0f));
      next.rootTranslation = EuclidCoreRandomTools.nextVector3D32(random);
      next.pelvisLinearVelocity = EuclidCoreRandomTools.nextVector3D32(random);
      next.pelvisAngularVelocity = EuclidCoreRandomTools.nextVector3D32(random);
      next.rootOrientation = EuclidCoreRandomTools.nextQuaternion32(random);
      next.pelvisLinearAcceleration = EuclidCoreRandomTools.nextVector3D32(random);

      size = Math.abs(random.nextInt(1000));
      for (int i = 0; i < next.momentAndForceDataAllForceSensors.size(); i++)
         next.momentAndForceDataAllForceSensors.add().set(nextSpatialVectorMessage(random));
      for (IMUPacket imuPacket : nextIMUPackets(random))
         next.imuSensorData.add().set(imuPacket);
      next.robotMotionStatus = RandomNumbers.nextEnum(random, RobotMotionStatus.class).toByte();
      next.lastReceivedPacketTypeID = random.nextInt(1000);
      next.lastReceivedPacketUniqueId = random.nextLong();
      next.lastReceivedPacketRobotTimestamp = random.nextLong();
      return next;
   }

   public static SpatialVectorMessage nextSpatialVectorMessage(Random random)
   {
      SpatialVectorMessage next = new SpatialVectorMessage();
      next.angularPart.set(EuclidCoreRandomTools.nextVector3D(random));
      next.linearPart.set(EuclidCoreRandomTools.nextVector3D(random));
      return next;
   }

   public static HighLevelStateChangeStatusMessage nextHighLevelStateChangeStatusMessage(Random random)
   {
      HighLevelStateChangeStatusMessage next = new HighLevelStateChangeStatusMessage();
      next.initialHighLevelControllerName = RandomNumbers.nextEnum(random, HighLevelControllerName.class).toByte();
      next.endHighLevelControllerName = RandomNumbers.nextEnum(random, HighLevelControllerName.class).toByte();
      return next;
   }

   public static FootstepPlanRequestPacket nextFootstepPlanRequestPacket(Random random)
   {
      FootstepPlanRequestPacket next = new FootstepPlanRequestPacket();
      next.startFootstep = nextFootstepDataMessage(random);
      next.thetaStart = random.nextDouble();
      next.maxSuboptimality = random.nextDouble();
      MessageTools.copyData(nextFootstepDataMessages(random), next.goals);
      next.footstepPlanRequestType = RandomNumbers.nextEnum(random, FootstepPlanRequestType.class).toByte();
      return next;
   }

   public static HeatMapPacket nextHeatMapPacket(Random random)
   {
      HeatMapPacket next = new HeatMapPacket();
      next.height = RandomNumbers.nextInt(random, -100, 100);
      next.width = RandomNumbers.nextInt(random, -100, 100);
      next.data.add(RandomNumbers.nextFloatArray(random, next.height * next.width, 1.0f));
      next.name.append(Integer.toHexString(random.nextInt()));
      return next;
   }

   public static BoundingBoxesPacket nextBoundingBoxesPacket(Random random)
   {
      BoundingBoxesPacket next = new BoundingBoxesPacket();
      int boxesToGenerate = random.nextInt(20);

      for (int i = 0; i < boxesToGenerate; i++)
      {
         next.labels.add().append(Integer.toHexString(random.nextInt()));
         next.boundingBoxXCoordinates.add(RandomNumbers.nextInt(random, -1000, 1000));
         next.boundingBoxYCoordinates.add(RandomNumbers.nextInt(random, -1000, 1000));
         next.boundingBoxWidths.add(RandomNumbers.nextInt(random, 0, 1000));
         next.boundingBoxHeights.add(RandomNumbers.nextInt(random, 0, 1000));
      }
      return next;
   }

   public static ObjectDetectorResultPacket nextObjectDetectorResultPacket(Random random)
   {
      ObjectDetectorResultPacket next = new ObjectDetectorResultPacket();
      next.heatMap = nextHeatMapPacket(random);
      next.boundingBoxes = nextBoundingBoxesPacket(random);
      return next;
   }

   public static PauseWalkingMessage nextPauseWalkingMessage(Random random)
   {
      PauseWalkingMessage next = new PauseWalkingMessage();
      next.pause = random.nextBoolean();
      return next;
   }

   public static AtlasLowLevelControlModeMessage nextAtlasLowLevelControlModeMessage(Random random)
   {
      AtlasLowLevelControlModeMessage next = new AtlasLowLevelControlModeMessage();
      next.requestedAtlasLowLevelControlMode = RandomNumbers.nextEnum(random, AtlasLowLevelControlMode.class).toByte();
      return next;
   }

   public static BehaviorControlModeResponsePacket nextBehaviorControlModeResponsePacket(Random random)
   {
      BehaviorControlModeResponsePacket next = new BehaviorControlModeResponsePacket();
      next.behaviorControlModeEnumRequest = RandomNumbers.nextEnum(random, BehaviorControlModeEnum.class).toByte();
      return next;
   }

   public static EuclideanTrajectoryPointMessage nextEuclideanTrajectoryPointMessage(Random random)
   {
      EuclideanTrajectoryPointMessage next = new EuclideanTrajectoryPointMessage();
      next.time = RandomNumbers.nextDoubleWithEdgeCases(random, 0.01);
      next.position = RandomGeometry.nextPoint3D(random, 1.0, 1.0, 1.0);
      next.linearVelocity = RandomGeometry.nextVector3D(random);
      return next;
   }

   public static EuclideanTrajectoryPointMessage[] nextEuclideanTrajectoryPointMessages(Random random)
   {
      return nextEuclideanTrajectoryPointMessages(random, random.nextInt(16) + 1);
   }

   public static EuclideanTrajectoryPointMessage[] nextEuclideanTrajectoryPointMessages(Random random, int length)
   {
      EuclideanTrajectoryPointMessage[] next = new EuclideanTrajectoryPointMessage[length];
      for (int i = 0; i < length; i++)
      {
         next[i] = nextEuclideanTrajectoryPointMessage(random);
      }
      return next;
   }

   public static EuclideanTrajectoryMessage nextEuclideanTrajectoryMessage(Random random)
   {
      EuclideanTrajectoryMessage next = new EuclideanTrajectoryMessage();
      MessageTools.copyData(nextEuclideanTrajectoryPointMessages(random), next.taskspaceTrajectoryPoints);
      next.selectionMatrix = nextSelectionMatrix3DMessage(random);
      next.frameInformation = nextFrameInformation(random);
      next.weightMatrix = nextWeightMatrix3DMessage(random);
      next.useCustomControlFrame = random.nextBoolean();
      next.controlFramePose = EuclidGeometryRandomTools.nextPose3D(random);
      next.queueingProperties = nextQueueableMessage(random);
      return next;
   }

   public static PelvisHeightTrajectoryMessage nextPelvisHeightTrajectoryMessage(Random random)
   {
      PelvisHeightTrajectoryMessage next = new PelvisHeightTrajectoryMessage();
      next.euclideanTrajectory = nextEuclideanTrajectoryMessage(random);
      next.enableUserPelvisControl = random.nextBoolean();
      next.enableUserPelvisControlDuringWalking = random.nextBoolean();
      next.euclideanTrajectory.selectionMatrix = new SelectionMatrix3DMessage();
      next.euclideanTrajectory.selectionMatrix.xSelected = false;
      next.euclideanTrajectory.selectionMatrix.ySelected = false;
      next.euclideanTrajectory.selectionMatrix.zSelected = true;
      return next;
   }

   public static MomentumTrajectoryMessage nextMomentumTrajectoryMessage(Random random)
   {
      MomentumTrajectoryMessage next = new MomentumTrajectoryMessage();
      next.angularMomentumTrajectory = nextEuclideanTrajectoryMessage(random);
      return next;
   }

   public static CenterOfMassTrajectoryMessage nextCenterOfMassTrajectoryMessage(Random random)
   {
      CenterOfMassTrajectoryMessage next = new CenterOfMassTrajectoryMessage();
      next.euclideanTrajectory = nextEuclideanTrajectoryMessage(random);
      return next;
   }

   public static LocalizationStatusPacket nextLocalizationStatusPacket(Random random)
   {
      LocalizationStatusPacket next = new LocalizationStatusPacket();
      next.overlap = random.nextDouble();
      next.status.append(Integer.toHexString(random.nextInt()));
      return next;
   }

   public static PelvisPoseErrorPacket nextPelvisPoseErrorPacket(Random random)
   {
      PelvisPoseErrorPacket next = new PelvisPoseErrorPacket();
      next.residualError = random.nextFloat();
      next.totalError = random.nextFloat();
      next.hasMapBeenReset = random.nextBoolean();
      return next;
   }

   public static PointCloudWorldPacket nextPointCloudWorldPacket(Random random)
   {
      PointCloudWorldPacket next = new PointCloudWorldPacket();
      next.timestamp = random.nextLong();
      next.groundQuadTreeSupport.add(RandomNumbers.nextFloatArray(random, random.nextInt(), 100.0f));
      next.decayingWorldScan.add(RandomNumbers.nextFloatArray(random, random.nextInt(), 100.0f));
      next.defaultGroundHeight = random.nextFloat();
      return next;
   }

   public static FootstepPathPlanPacket nextFootstepPathPlanPacket(Random random)
   {
      FootstepPathPlanPacket next = new FootstepPathPlanPacket();
      next.goalsValid = random.nextBoolean();
      next.start = nextFootstepDataMessage(random);
      MessageTools.copyData(nextFootstepDataMessages(random), next.originalGoals);
      MessageTools.copyData(nextFootstepDataMessages(random), next.pathPlan);
      int size = Math.abs(random.nextInt(1000));
      for (int i = 0; i < size; i++)
      {
         next.footstepUnknown.add((byte) random.nextInt(2));
      }
      next.subOptimality = random.nextDouble();
      next.pathCost = random.nextDouble();

      return next;
   }

   public static AtlasWristSensorCalibrationRequestPacket nextAtlasWristSensorCalibrationRequestPacket(Random random)
   {
      AtlasWristSensorCalibrationRequestPacket next = new AtlasWristSensorCalibrationRequestPacket();
      next.robotSide = RandomNumbers.nextEnum(random, RobotSide.class).toByte();
      return next;
   }

   public static VehiclePosePacket nextVehiclePosePacket(Random random)
   {
      VehiclePosePacket next = new VehiclePosePacket();
      next.position = EuclidCoreRandomTools.nextPoint3D(random);
      next.orientation = EuclidCoreRandomTools.nextQuaternion(random);
      next.index = random.nextInt();
      return next;
   }

   public static HandPowerCyclePacket nextHandPowerCyclePacket(Random random)
   {
      HandPowerCyclePacket next = new HandPowerCyclePacket();
      next.robotSide = RandomNumbers.nextEnum(random, RobotSide.class).toByte();
      return next;
   }

   public static CapturabilityBasedStatus nextCapturabilityBasedStatus(Random random)
   {
      CapturabilityBasedStatus next = new CapturabilityBasedStatus();
      double max = Double.MAX_VALUE / 2;
      next.capturePoint = RandomGeometry.nextPoint2D(random, max, max);
      next.desiredCapturePoint = RandomGeometry.nextPoint2D(random, max, max);
      next.centerOfMass = RandomGeometry.nextPoint3D(random, max, max, max);

      IntStream.range(0, MAXIMUM_NUMBER_OF_VERTICES).mapToObj(i -> nextPoint2D(random)).forEach(next.leftFootSupportPolygon.add()::set);
      IntStream.range(0, MAXIMUM_NUMBER_OF_VERTICES).mapToObj(i -> nextPoint2D(random)).forEach(next.rightFootSupportPolygon.add()::set);

      return next;
   }

   public static HandDesiredConfigurationMessage nextHandDesiredConfigurationMessage(Random random)
   {
      HandDesiredConfigurationMessage next = new HandDesiredConfigurationMessage();
      next.robotSide = RandomNumbers.nextEnum(random, RobotSide.class).toByte();
      next.desiredHandConfiguration = RandomNumbers.nextEnum(random, HandConfiguration.class).toByte();
      return next;
   }

   public static WalkingStatusMessage nextWalkingStatusMessage(Random random)
   {
      WalkingStatusMessage next = new WalkingStatusMessage();
      next.walkingStatus = RandomNumbers.nextEnum(random, WalkingStatus.class).toByte();
      return next;
   }

   public static SnapFootstepPacket nextSnapFootstepPacket(Random random)
   {
      SnapFootstepPacket next = new SnapFootstepPacket();
      // Number of footsteps
      int numberOfFootsteps = random.nextInt(255);

      // create random footsteps
      int[] footstepOrder = new int[numberOfFootsteps];
      byte[] flag = new byte[numberOfFootsteps];
      ArrayList<FootstepDataMessage> footsteps = new ArrayList<FootstepDataMessage>();
      RigidBodyTransform previousFootstep = new RigidBodyTransform();

      double[] XYZ_MAX = {2.0, 2.0, 2.0};
      double[] XYZ_MIN = {-2.0, -2.0, -3.0};

      double xMax = 0.90 * Math.min(Math.abs(XYZ_MAX[0]), Math.abs(XYZ_MIN[0]));
      double yMax = 0.90 * Math.min(Math.abs(XYZ_MAX[1]), Math.abs(XYZ_MIN[1]));
      double zMax = 0.90 * Math.min(Math.abs(XYZ_MAX[2]), Math.abs(XYZ_MIN[2]));

      for (int footstepNumber = 0; footstepNumber < numberOfFootsteps; footstepNumber++)
      {
         footstepOrder[footstepNumber] = footstepNumber;
         flag[footstepNumber] = (byte) random.nextInt(3);
         RobotSide robotSide = (footstepNumber % 2 == 0) ? RobotSide.RIGHT : RobotSide.LEFT;

         Point3D position = RandomGeometry.nextPoint3D(random, xMax, yMax, zMax);

         Quaternion orientation = new Quaternion();
         orientation.set(RandomGeometry.nextAxisAngle(random));

         previousFootstep.transform(position);

         previousFootstep.setTranslation(new Vector3D32(position));
         previousFootstep.setRotation(orientation);

         FootstepDataMessage footstepData = HumanoidMessageTools.createFootstepDataMessage(robotSide, new Point3D(position), orientation);

         footsteps.add(footstepData);
      }

      MessageTools.copyData(footsteps, next.footstepData);
      next.footstepOrder.add(footstepOrder);
      next.flag.add(flag);
      return next;
   }

   public static DetectedObjectPacket nextDetectedObjectPacket(Random random)
   {
      DetectedObjectPacket next = new DetectedObjectPacket();
      next.pose = EuclidGeometryRandomTools.nextPose3D(random);
      next.id = random.nextInt(255);
      return next;
   }

   public static DesiredAccelerationsMessage nextDesiredAccelerationsMessage(Random random)
   {
      DesiredAccelerationsMessage next = new DesiredAccelerationsMessage();
      next.desiredJointAccelerations.add(RandomNumbers.nextDoubleArray(random, random.nextInt(16) + 1, 1.0));
      next.queueingProperties = nextQueueableMessage(random);
      return next;
   }

   public static NeckDesiredAccelerationsMessage nextNeckDesiredAccelerationsMessage(Random random)
   {
      NeckDesiredAccelerationsMessage next = new NeckDesiredAccelerationsMessage();
      next.desiredAccelerations = nextDesiredAccelerationsMessage(random);
      return next;
   }

   public static LocalizationPacket nextLocalizationPacket(Random random)
   {
      LocalizationPacket next = new LocalizationPacket();
      next.reset = random.nextBoolean();
      next.toggle = random.nextBoolean();
      return next;
   }

   public static ArmDesiredAccelerationsMessage nextArmDesiredAccelerationsMessage(Random random)
   {
      ArmDesiredAccelerationsMessage next = new ArmDesiredAccelerationsMessage();
      next.robotSide = RandomNumbers.nextEnum(random, RobotSide.class).toByte();
      next.desiredAccelerations = nextDesiredAccelerationsMessage(random);
      return next;
   }

   public static SpineDesiredAccelerationsMessage nextSpineDesiredAccelerationsMessage(Random random)
   {
      SpineDesiredAccelerationsMessage next = new SpineDesiredAccelerationsMessage();
      next.desiredAccelerations = nextDesiredAccelerationsMessage(random);
      return next;
   }

   public static MultisenseParameterPacket nextMultisenseParameterPacket(Random random)
   {
      MultisenseParameterPacket next = new MultisenseParameterPacket();
      next.initialize = random.nextBoolean();
      next.gain = random.nextDouble();
      next.motorSpeed = random.nextDouble();
      next.ledEnable = random.nextBoolean();
      next.flashEnable = random.nextBoolean();
      next.dutyCycle = random.nextInt();
      next.autoExposure = random.nextBoolean();
      next.autoWhiteBalance = random.nextBoolean();
      return next;
   }

   public static KinematicsToolboxOutputStatus nextKinematicsToolboxOutputStatus(Random random)
   {
      KinematicsToolboxOutputStatus next = new KinematicsToolboxOutputStatus();
      next.jointNameHash = random.nextInt();
      next.desiredJointAngles.add(RandomNumbers.nextFloatArray(random, random.nextInt(100), 1.0f));
      next.desiredRootTranslation = EuclidCoreRandomTools.nextVector3D32(random);
      next.desiredRootOrientation = EuclidCoreRandomTools.nextQuaternion32(random);
      next.solutionQuality = random.nextDouble();
      return next;
   }

   public static BlackFlyParameterPacket nextBlackFlyParameterPacket(Random random)
   {
      BlackFlyParameterPacket next = new BlackFlyParameterPacket();
      next.autoExposure = random.nextBoolean();
      next.autoGain = random.nextBoolean();
      next.autoShutter = random.nextBoolean();
      next.exposure = random.nextDouble();
      next.frameRate = random.nextDouble();
      next.fromUI = random.nextBoolean();
      next.gain = random.nextDouble();
      next.shutter = random.nextDouble();
      next.robotSide = RandomNumbers.nextEnum(random, RobotSide.class).toByte();
      return next;
   }

   public static VideoPacket nextVideoPacket(Random random)
   {
      VideoPacket next = new VideoPacket();
      next.videoSource = RandomNumbers.nextEnum(random, VideoSource.class).toByte();
      next.timeStamp = random.nextLong();
      byte[] data = new byte[random.nextInt((int) (Math.pow(2, 20) - 19))];
      random.nextBytes(data);
      next.data.add(data);
      next.position = EuclidCoreRandomTools.nextPoint3D(random);
      next.orientation = EuclidCoreRandomTools.nextQuaternion(random);
      next.intrinsicParameters = nextIntrinsicParametersMessage(random);
      return next;
   }

   public static IntrinsicParametersMessage nextIntrinsicParametersMessage(Random random)
   {
      IntrinsicParametersMessage next = new IntrinsicParametersMessage();
      next.width = random.nextInt();
      next.height = random.nextInt();
      next.fx = random.nextDouble();
      next.fy = random.nextDouble();
      next.skew = random.nextDouble();
      next.cx = random.nextDouble();
      next.cy = random.nextDouble();
      next.radial.add(RandomNumbers.nextDoubleArray(random, random.nextInt(1000), 1.0));
      next.t1 = random.nextDouble();
      next.t2 = random.nextDouble();
      return next;
   }

   public static LocalizationPointMapPacket nextLocalizationPointMapPacket(Random random)
   {
      LocalizationPointMapPacket next = new LocalizationPointMapPacket();
      next.timestamp = random.nextLong();
      next.localizationPointMap.add(RandomNumbers.nextFloatArray(random, random.nextInt(10000), 1.0f));
      return next;
   }

   public static GoHomeMessage nextGoHomeMessage(Random random)
   {
      GoHomeMessage next = new GoHomeMessage();
      next.humanoidBodyPart = RandomNumbers.nextEnum(random, HumanoidBodyPart.class).toByte();
      next.robotSide = RandomNumbers.nextEnum(random, RobotSide.class).toByte();
      next.trajectoryTime = RandomNumbers.nextDoubleWithEdgeCases(random, 0.01);
      return next;
   }

   public static PrepareForLocomotionMessage nextPrepareForLocomotionMessage(Random random)
   {
      PrepareForLocomotionMessage next = new PrepareForLocomotionMessage();
      next.prepareManipulation = random.nextBoolean();
      next.preparePelvis = random.nextBoolean();
      return next;
   }

   public static StampedPosePacket nextStampedPosePacket(Random random)
   {
      StampedPosePacket next = new StampedPosePacket();
      next.pose = EuclidGeometryRandomTools.nextPose3D(random);
      next.timeStamp = random.nextLong();
      next.confidenceFactor = random.nextDouble();
      next.frameId.append(Integer.toHexString(random.nextInt()));
      return next;
   }

   public static AtlasDesiredPumpPSIPacket nextAtlasDesiredPumpPSIPacket(Random random)
   {
      AtlasDesiredPumpPSIPacket next = new AtlasDesiredPumpPSIPacket();
      next.desiredPumpPsi = random.nextInt();
      return next;
   }

   public static BDIBehaviorStatusPacket nextBDIBehaviorStatusPacket(Random random)
   {
      BDIBehaviorStatusPacket next = new BDIBehaviorStatusPacket();
      next.currentBDIRobotBehavior = RandomNumbers.nextEnum(random, BDIRobotBehavior.class).toByte();
      return next;
   }

   public static StopAllTrajectoryMessage nextStopAllTrajectoryMessage(Random random)
   {
      StopAllTrajectoryMessage next = new StopAllTrajectoryMessage();
      return next;
   }

   public static AdjustFootstepMessage nextAdjustFootstepMessage(Random random)
   {
      AdjustFootstepMessage next = new AdjustFootstepMessage();
      next.robotSide = RandomNumbers.nextEnum(random, RobotSide.class).toByte();
      next.location = EuclidCoreRandomTools.nextPoint3D(random);
      next.orientation = EuclidCoreRandomTools.nextQuaternion(random);
      IntStream.range(0, random.nextInt(10)).mapToObj(i -> EuclidCoreRandomTools.nextPoint2D(random)).forEach(next.predictedContactPoints.add()::set);
      next.executionDelayTime = RandomNumbers.nextDoubleWithEdgeCases(random, 0.1);
      return next;
   }

   public static FootstepPlanningToolboxOutputStatus nextFootstepPlanningToolboxOutputStatus(Random random)
   {
      FootstepPlanningToolboxOutputStatus next = new FootstepPlanningToolboxOutputStatus();
      next.footstepDataList = RandomHumanoidMessages.nextFootstepDataListMessage(random);
      int result = random.nextInt(FootstepPlanningResult.values.length);
      next.footstepPlanningResult = FootstepPlanningResult.values[result].toByte();
      next.planId = random.nextInt();

      for (int i = 0; i < random.nextInt(10); i++)
      {
         next.bodyPath.add().set(EuclidCoreRandomTools.nextPoint2D(random));
      }

      next.lowLevelPlannerGoal = new Pose2D(random.nextDouble(), random.nextDouble(), random.nextDouble());

      return next;
   }

   public static FootstepStatusMessage nextFootstepStatusMessage(Random random)
   {
      FootstepStatusMessage next = new FootstepStatusMessage();
      next.footstepStatus = RandomNumbers.nextEnum(random, FootstepStatus.class).toByte();
      next.footstepIndex = RandomNumbers.nextIntWithEdgeCases(random, 0.1);
      next.robotSide = RobotSide.generateRandomRobotSide(random).toByte();
      next.desiredFootPositionInWorld = RandomGeometry.nextPoint3D(random, 1.0, 1.0, 1.0);
      next.desiredFootOrientationInWorld = RandomGeometry.nextQuaternion(random);
      next.actualFootPositionInWorld = RandomGeometry.nextPoint3D(random, 1.0, 1.0, 1.0);
      next.actualFootOrientationInWorld = RandomGeometry.nextQuaternion(random);
      return next;
   }

   public static AtlasElectricMotorAutoEnableFlagPacket nextAtlasElectricMotorAutoEnableFlagPacket(Random random)
   {
      AtlasElectricMotorAutoEnableFlagPacket next = new AtlasElectricMotorAutoEnableFlagPacket();
      next.shouldAutoEnable = random.nextBoolean();
      return next;
   }

   public static BDIBehaviorCommandPacket nextBDIBehaviorCommandPacket(Random random)
   {
      BDIBehaviorCommandPacket next = new BDIBehaviorCommandPacket();
      next.atlasBDIRobotBehavior = RandomNumbers.nextEnum(random, BDIRobotBehavior.class).toByte();
      next.stop = random.nextBoolean();
      return next;
   }

   public static ObjectWeightPacket nextObjectWeightPacket(Random random)
   {
      ObjectWeightPacket next = new ObjectWeightPacket();
      next.weight = random.nextDouble();
      next.robotSide = random.nextBoolean() ? RobotSide.LEFT.toByte() : RobotSide.RIGHT.toByte();
      return next;
   }

   public static LegCompliancePacket nextLegCompliancePacket(Random random)
   {
      LegCompliancePacket next = new LegCompliancePacket();
      next.robotSide = RandomNumbers.nextEnum(random, RobotSide.class).toByte();
      next.maxVelocityDeltas.add(RandomNumbers.nextFloatArray(random, random.nextInt(1000), 1.0f));
      return next;
   }

   public static HandJointAnglePacket nextHandJointAnglePacket(Random random)
   {
      HandJointAnglePacket next = new HandJointAnglePacket();
      next.robotSide = RandomNumbers.nextEnum(random, RobotSide.class).toByte();
      next.jointAngles.add(RandomNumbers.nextDoubleArray(random, random.nextInt(1000), 1.0));
      next.connected = random.nextBoolean();
      next.calibrated = random.nextBoolean();
      return next;
   }

   public static BehaviorControlModePacket nextBehaviorControlModePacket(Random random)
   {
      BehaviorControlModePacket next = new BehaviorControlModePacket();
      next.behaviorControlModeEnumRequest = RandomNumbers.nextEnum(random, BehaviorControlModeEnum.class).toByte();
      return next;
   }

   public static AtlasElectricMotorEnablePacket nextAtlasElectricMotorEnablePacket(Random random)
   {
      AtlasElectricMotorEnablePacket next = new AtlasElectricMotorEnablePacket();
      next.atlasElectricMotorPacketEnumEnable = RandomNumbers.nextEnum(random, AtlasElectricMotorPacketEnum.class).toByte();
      next.enable = random.nextBoolean();
      return next;
   }

   public static HighLevelStateMessage nextHighLevelStateMessage(Random random)
   {
      HighLevelStateMessage next = new HighLevelStateMessage();
      next.highLevelControllerName = RandomNumbers.nextEnum(random, HighLevelControllerName.class).toByte();
      return next;
   }

   public static SCSListenerPacket nextSCSListenerPacket(Random random)
   {
      SCSListenerPacket next = new SCSListenerPacket();
      next.isStopped = random.nextBoolean();
      return next;
   }

   public static SimulatedLidarScanPacket nextSimulatedLidarScanPacket(Random random)
   {
      SimulatedLidarScanPacket next = new SimulatedLidarScanPacket();
      int size = Math.abs(random.nextInt(1000000));
      for (int i = 0; i < size; i++)
      {
         next.ranges.add(random.nextFloat());
      }

      next.sensorId = random.nextInt();

      next.lidarScanParameters = nextLidarScanParametersMessage(random);
      return next;
   }

   public static LidarScanParametersMessage nextLidarScanParametersMessage(Random random)
   {
      LidarScanParametersMessage next = new LidarScanParametersMessage();
      next.timestamp = random.nextLong();
      next.sweepYawMax = random.nextFloat();
      next.sweepYawMin = random.nextFloat();
      next.heightPitchMax = random.nextFloat();
      next.heightPitchMin = random.nextFloat();
      next.timeIncrement = random.nextFloat();
      next.scanTime = random.nextFloat();
      next.minRange = random.nextFloat();
      next.maxRange = random.nextFloat();
      next.pointsPerSweep = random.nextInt();
      next.scanHeight = random.nextInt();
      return null;
   }

   public static ManualHandControlPacket nextManualHandControlPacket(Random random)
   {
      ManualHandControlPacket next = new ManualHandControlPacket();
      next.robotSide = RobotSide.generateRandomRobotSide(random).toByte();
      double[] angles = RandomNumbers.nextDoubleArray(random, 4, 0, 1);

      next.index = angles[0];
      next.middle = angles[1];
      next.thumb = angles[2];
      next.spread = angles[3];
      next.controlType = 0;
      return next;
   }

   public static AbortWalkingMessage nextAbortWalkingMessage(Random random)
   {
      AbortWalkingMessage next = new AbortWalkingMessage();
      return next;
   }
}
