package us.ihmc.rdx.ui.behavior.tree;

import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.ROS2SyncedRobotModel;
import us.ihmc.behaviors.behaviorTree.BehaviorTreeNodeStateBuilder;
import us.ihmc.behaviors.sequence.ActionSequenceDefinition;
import us.ihmc.behaviors.sequence.actions.*;
import us.ihmc.communication.crdt.CRDTInfo;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersBasics;
import us.ihmc.mecano.frames.MovingReferenceFrame;
import us.ihmc.rdx.ui.RDX3DPanel;
import us.ihmc.rdx.ui.RDXBaseUI;
import us.ihmc.rdx.ui.behavior.actions.*;
import us.ihmc.rdx.ui.behavior.sequence.RDXActionSequence;
import us.ihmc.robotics.physics.RobotCollisionModel;
import us.ihmc.robotics.referenceFrames.ReferenceFrameLibrary;

public class RDXBehaviorTreeNodeBuilder implements BehaviorTreeNodeStateBuilder
{
   private final DRCRobotModel robotModel;
   private final ROS2SyncedRobotModel syncedRobot;
   private final RobotCollisionModel selectionCollisionModel;
   private final RDXBaseUI baseUI;
   private final RDX3DPanel panel3D;
   private final ReferenceFrameLibrary referenceFrameLibrary;
   private final FootstepPlannerParametersBasics footstepPlannerParametersBasics;

   public RDXBehaviorTreeNodeBuilder(DRCRobotModel robotModel,
                                     ROS2SyncedRobotModel syncedRobot,
                                     RobotCollisionModel selectionCollisionModel,
                                     RDXBaseUI baseUI,
                                     RDX3DPanel panel3D,
                                     ReferenceFrameLibrary referenceFrameLibrary,
                                     FootstepPlannerParametersBasics footstepPlannerParametersBasics)
   {
      this.robotModel = robotModel;
      this.syncedRobot = syncedRobot;
      this.selectionCollisionModel = selectionCollisionModel;
      this.baseUI = baseUI;
      this.panel3D = panel3D;
      this.referenceFrameLibrary = referenceFrameLibrary;
      this.footstepPlannerParametersBasics = footstepPlannerParametersBasics;
   }

   @Override
   public RDXBehaviorTreeNode<?, ?> createNode(Class<?> nodeType, long id, CRDTInfo crdtInfo)
   {
      // Control nodes:
      if (nodeType == ActionSequenceDefinition.class)
      {
         return new RDXActionSequence(id, crdtInfo);
      }

      // Actions:
      if (nodeType == ArmJointAnglesActionDefinition.class)
      {
         return new RDXArmJointAnglesAction(id, crdtInfo, robotModel);
      }
      if (nodeType == ChestOrientationActionDefinition.class)
      {
         return new RDXChestOrientationAction(id,
                                              crdtInfo,
                                              panel3D,
                                              robotModel,
                                              syncedRobot.getFullRobotModel(),
                                              selectionCollisionModel,
                                              referenceFrameLibrary);
      }
      if (nodeType == FootstepPlanActionDefinition.class)
      {
         return new RDXFootstepPlanAction(id, crdtInfo, baseUI, robotModel, syncedRobot, referenceFrameLibrary);
      }
      if (nodeType == HandPoseActionDefinition.class)
      {
         return new RDXHandPoseAction(id, crdtInfo, panel3D, robotModel, syncedRobot.getFullRobotModel(), selectionCollisionModel, referenceFrameLibrary);
      }
      if (nodeType == HandWrenchActionDefinition.class)
      {
         return new RDXHandWrenchAction(id, crdtInfo);
      }
      if (nodeType == PelvisHeightPitchActionDefinition.class)
      {
         return new RDXPelvisHeightPitchAction(id,
                                               crdtInfo,
                                               panel3D,
                                               robotModel,
                                               syncedRobot.getFullRobotModel(),
                                               selectionCollisionModel,
                                               referenceFrameLibrary);
      }
      if (nodeType == SakeHandCommandActionDefinition.class)
      {
         return new RDXSakeHandCommandAction(id, crdtInfo);
      }
      if (nodeType == WaitDurationActionDefinition.class)
      {
         return new RDXWaitDurationAction(id, crdtInfo);
      }
      if (nodeType == WalkActionDefinition.class)
      {
         return new RDXWalkAction(id, crdtInfo, panel3D, robotModel, referenceFrameLibrary, footstepPlannerParametersBasics);
      }
      else
      {
         return null;
      }
   }

   public void initializeNewNode(RDXBehaviorTreeNode<?, ?> newNode)
   {
      if (newNode instanceof RDXWalkAction walkAction)
      {
         MovingReferenceFrame parentFrame = syncedRobot.getReferenceFrames().getMidFeetZUpFrame();
         walkAction.getDefinition().setParentFrameName(parentFrame.getName());
         walkAction.getState().update();
      }

   }
}
