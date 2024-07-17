package us.ihmc.rdx.ui.behavior.actions;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import imgui.ImGui;
import imgui.flag.ImGuiMouseButton;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.behaviors.sequence.actions.PelvisHeightOrientationActionDefinition;
import us.ihmc.behaviors.sequence.actions.PelvisHeightOrientationActionState;
import us.ihmc.communication.crdt.CRDTInfo;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.mecano.multiBodySystem.interfaces.MultiBodySystemBasics;
import us.ihmc.rdx.imgui.*;
import us.ihmc.rdx.input.ImGui3DViewInput;
import us.ihmc.rdx.input.ImGui3DViewPickResult;
import us.ihmc.rdx.ui.RDX3DPanel;
import us.ihmc.rdx.ui.RDX3DPanelTooltip;
import us.ihmc.rdx.ui.affordances.RDXInteractableHighlightModel;
import us.ihmc.rdx.ui.affordances.RDXInteractableTools;
import us.ihmc.rdx.ui.behavior.sequence.RDXActionNode;
import us.ihmc.rdx.ui.gizmo.RDXSelectablePose3DGizmo;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.MultiBodySystemMissingTools;
import us.ihmc.robotics.interaction.MouseCollidable;
import us.ihmc.robotics.physics.Collidable;
import us.ihmc.robotics.physics.RobotCollisionModel;
import us.ihmc.robotics.referenceFrames.MutableReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrameLibrary;
import us.ihmc.tools.io.WorkspaceResourceDirectory;

import java.util.ArrayList;
import java.util.List;

public class RDXPelvisHeightOrientationAction extends RDXActionNode<PelvisHeightOrientationActionState, PelvisHeightOrientationActionDefinition>
{
   private final PelvisHeightOrientationActionState state;
   private final ImGuiUniqueLabelMap labels = new ImGuiUniqueLabelMap(getClass());
   private final ImDoubleWrapper heightWidget;
   private final ImDoubleWrapper yawWidget;
   private final ImDoubleWrapper pitchWidget;
   private final ImDoubleWrapper rollWidget;
   private final ImDoubleWrapper trajectoryDurationWidget;
   /** Gizmo is control frame */
   private final RDXSelectablePose3DGizmo poseGizmo;
   private final MutableReferenceFrame graphicFrame = new MutableReferenceFrame();
   private final MutableReferenceFrame collisionShapeFrame = new MutableReferenceFrame();
   private boolean isMouseHovering = false;
   private final ImGui3DViewPickResult pickResult = new ImGui3DViewPickResult();
   private final ArrayList<MouseCollidable> mouseCollidables = new ArrayList<>();
   private final RDXInteractableHighlightModel highlightModel;
   private final ImGuiReferenceFrameLibraryCombo parentFrameComboBox;
   private final RDX3DPanelTooltip tooltip;
   private final FullHumanoidRobotModel syncedFullRobotModel;

   public RDXPelvisHeightOrientationAction(long id,
                                           CRDTInfo crdtInfo,
                                           WorkspaceResourceDirectory saveFileDirectory,
                                           RDX3DPanel panel3D,
                                           DRCRobotModel robotModel,
                                           FullHumanoidRobotModel syncedFullRobotModel,
                                           RobotCollisionModel selectionCollisionModel,
                                           ReferenceFrameLibrary referenceFrameLibrary)
   {
      super(new PelvisHeightOrientationActionState(id, crdtInfo, saveFileDirectory, referenceFrameLibrary));

      state = getState();

      this.syncedFullRobotModel = syncedFullRobotModel;

      getDefinition().setName("Pelvis height and orientation");

      poseGizmo = new RDXSelectablePose3DGizmo(ReferenceFrame.getWorldFrame(), getDefinition().getPelvisToParentTransform().accessValue());
      poseGizmo.create(panel3D);

      parentFrameComboBox = new ImGuiReferenceFrameLibraryCombo("Parent frame",
                                                                referenceFrameLibrary,
                                                                getDefinition()::getParentFrameName,
                                                                getState().getPelvisFrame()::changeFrame);
      heightWidget = new ImDoubleWrapper(getDefinition()::getHeight,
                                         getDefinition()::setHeight,
                                         imDouble -> ImGuiTools.volatileInputDouble(labels.get("Height"), imDouble));
      yawWidget = new ImDoubleWrapper(getDefinition().getRotation()::getYaw, getDefinition()::setYaw,
                                      imDouble -> ImGuiTools.volatileInputDouble(labels.get("Yaw"), imDouble));
      pitchWidget = new ImDoubleWrapper(getDefinition()::getPitch,
                                        getDefinition()::setPitch,
                                        imDouble -> ImGuiTools.volatileInputDouble(labels.get("Pitch"), imDouble));
      rollWidget = new ImDoubleWrapper(getDefinition().getRotation()::getRoll, getDefinition()::setRoll,
                                       imDouble -> ImGuiTools.volatileInputDouble(labels.get("Roll"), imDouble));
      trajectoryDurationWidget = new ImDoubleWrapper(getDefinition()::getTrajectoryDuration,
                                                     getDefinition()::setTrajectoryDuration,
                                                     imDouble -> ImGuiTools.volatileInputDouble(labels.get("Trajectory duration"), imDouble));

      String pelvisBodyName = syncedFullRobotModel.getPelvis().getName();
      String modelFileName = RDXInteractableTools.getModelFileName(robotModel.getRobotDefinition().getRigidBodyDefinition(pelvisBodyName));
      highlightModel = new RDXInteractableHighlightModel(modelFileName);

      MultiBodySystemBasics pelvisOnlySystem = MultiBodySystemMissingTools.createSingleBodySystem(syncedFullRobotModel.getPelvis());
      List<Collidable> pelvisCollidables = selectionCollisionModel.getRobotCollidables(pelvisOnlySystem);

      for (Collidable pelvisCollidable : pelvisCollidables)
      {
         mouseCollidables.add(new MouseCollidable(pelvisCollidable));
      }

      tooltip = new RDX3DPanelTooltip(panel3D);
      panel3D.addImGuiOverlayAddition(this::render3DPanelImGuiOverlays);
   }

   @Override
   public void update()
   {
      super.update();

      if (state.getPelvisFrame().isChildOfWorld())
      {
         if (poseGizmo.getPoseGizmo().getGizmoFrame() != state.getPelvisFrame().getReferenceFrame())
         {
            poseGizmo.getPoseGizmo().setGizmoFrame(state.getPelvisFrame().getReferenceFrame());
            graphicFrame.setParentFrame(state.getPelvisFrame().getReferenceFrame());
            collisionShapeFrame.setParentFrame(state.getPelvisFrame().getReferenceFrame());
         }

         poseGizmo.getPoseGizmo().update();

         if (!getSelected())
            poseGizmo.setSelected(false);

         if (poseGizmo.getPoseGizmo().getGizmoModifiedByUser().poll())
         {
            getDefinition().getPelvisToParentTransform().accessValue();
         }

         if (state.getIsNextForExecution() || getSelected())
         {
            highlightModel.setPose(graphicFrame.getReferenceFrame());
            if (poseGizmo.isSelected() || isMouseHovering)
            {
               highlightModel.setTransparency(0.7);
            }
            else
            {
               highlightModel.setTransparency(0.5);
            }
         }

         // compute transform variation from previous pose
         FramePose3D currentRobotPelvisPose = new FramePose3D(syncedFullRobotModel.getPelvis().getParentJoint().getFrameAfterJoint());
         if (state.getPelvisFrame().getReferenceFrame().getParent() != currentRobotPelvisPose.getReferenceFrame())
            currentRobotPelvisPose.changeFrame(state.getPelvisFrame().getReferenceFrame().getParent());
         RigidBodyTransform transformVariation = new RigidBodyTransform();
         transformVariation.setAndInvert(currentRobotPelvisPose);
         getDefinition().getPelvisToParentTransform().getValueReadOnly().transform(transformVariation);
      }
   }

   @Override
   protected void renderImGuiWidgetsInternal()
   {
      ImGui.checkbox(labels.get("Adjust Goal Pose"), poseGizmo.getSelected());
      parentFrameComboBox.render();
      ImGui.pushItemWidth(80.0f);
      heightWidget.renderImGuiWidget();
      yawWidget.renderImGuiWidget();
      ImGui.sameLine();
      pitchWidget.renderImGuiWidget();
      ImGui.sameLine();
      rollWidget.renderImGuiWidget();
      trajectoryDurationWidget.renderImGuiWidget();
      ImGui.popItemWidth();
   }

   public void render3DPanelImGuiOverlays()
   {
      if (isMouseHovering)
      {
         tooltip.render("%s Action\nIndex: %d\nName: %s".formatted(getActionTypeTitle(), state.getActionIndex(), getDefinition().getName()));
      }
   }

   @Override
   public void calculate3DViewPick(ImGui3DViewInput input)
   {
      if (state.getPelvisFrame().isChildOfWorld())
      {
         poseGizmo.calculate3DViewPick(input);

         pickResult.reset();
         for (MouseCollidable mouseCollidable : mouseCollidables)
         {
            double collision = mouseCollidable.collide(input.getPickRayInWorld(), collisionShapeFrame.getReferenceFrame());
            if (!Double.isNaN(collision))
               pickResult.addPickCollision(collision);
         }
         if (pickResult.getPickCollisionWasAddedSinceReset())
            input.addPickResult(pickResult);
      }
   }

   @Override
   public void process3DViewInput(ImGui3DViewInput input)
   {
      if (state.getPelvisFrame().isChildOfWorld())
      {
         isMouseHovering = input.getClosestPick() == pickResult;

         boolean isClickedOn = isMouseHovering && input.mouseReleasedWithoutDrag(ImGuiMouseButton.Left);
         if (isClickedOn)
         {
            poseGizmo.setSelected(true);
         }

         poseGizmo.process3DViewInput(input, isMouseHovering);

         tooltip.setInput(input);
      }
   }

   @Override
   public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool)
   {
      if (state.getPelvisFrame().isChildOfWorld() && (state.getIsNextForExecution() || getSelected()))
      {
         highlightModel.getRenderables(renderables, pool);
         poseGizmo.getVirtualRenderables(renderables, pool);
      }
   }

   public ReferenceFrame getReferenceFrame()
   {
      return poseGizmo.getPoseGizmo().getGizmoFrame();
   }

   @Override
   public String getActionTypeTitle()
   {
      return "Pelvis Height and Orientation";
   }
}
