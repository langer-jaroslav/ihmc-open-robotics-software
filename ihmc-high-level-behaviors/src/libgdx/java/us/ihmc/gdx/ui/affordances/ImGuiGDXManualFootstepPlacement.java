package us.ihmc.gdx.ui.affordances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import controller_msgs.msg.dds.FootstepDataListMessage;
import controller_msgs.msg.dds.FootstepDataMessage;
import imgui.flag.ImGuiMouseButton;
import imgui.internal.ImGui;
import imgui.type.ImFloat;
import org.lwjgl.openvr.InputDigitalActionData;
import us.ihmc.avatar.drcRobot.ROS2SyncedRobotModel;
import us.ihmc.behaviors.tools.CommunicationHelper;
import us.ihmc.communication.packets.ExecutionMode;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Point3D32;
import us.ihmc.euclid.tuple3D.Vector3D32;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.footstepPlanning.graphSearch.graph.DiscreteFootstep;
import us.ihmc.gdx.imgui.ImGuiLabelMap;
import us.ihmc.gdx.imgui.ImGuiTools;
import us.ihmc.gdx.input.ImGui3DViewInput;
import us.ihmc.gdx.input.editor.GDXUIActionMap;
import us.ihmc.gdx.input.editor.GDXUITrigger;
import us.ihmc.gdx.sceneManager.GDXSceneLevel;
import us.ihmc.gdx.tools.GDXTools;
import us.ihmc.gdx.ui.GDX3DPanel;
import us.ihmc.gdx.ui.GDXImGuiBasedUI;
import us.ihmc.gdx.ui.gizmo.GDXPose3DGizmo;
import us.ihmc.gdx.vr.GDXVRManager;
import us.ihmc.robotics.robotSide.RobotSide;

import java.util.ArrayList;
import java.util.UUID;

public class ImGuiGDXManualFootstepPlacement implements RenderableProvider
{
   private boolean isFirstStep = false;
   private final ImGuiLabelMap labels = new ImGuiLabelMap();
   private final ImFloat goalZOffset = new ImFloat(0.0f);

   private GDXUIActionMap placeGoalActionMap;
   private boolean placingGoal = false;
   private boolean placingPosition = true;
   private Point3D lastObjectIntersection;
   private final Pose3D goalPoseForReading = new Pose3D();
   private final Point3D32 tempSpherePosition = new Point3D32();
   private final Vector3D32 tempRotationVector = new Vector3D32();
   private final RigidBodyTransform tempTransform = new RigidBodyTransform();

   ReferenceFrame referenceFrameFootstep;
   FramePose3D footTextPose;
   boolean footstepCreated = false;

   private float textheight = 12;

   ArrayList<SingleFootstep> footstepArrayList = new ArrayList<SingleFootstep>();
   int footstepIndex = -1;
   GDXImGuiBasedUI baseUI;
   private CommunicationHelper communicationHelper;
   RobotSide currentFootStepSide;
   private ROS2SyncedRobotModel syncedRobot;

   private SimpleStepChecker stepChecker;

   GDXPose3DGizmo gizmo;
   private ImGui3DViewInput latestInput;
   private GDX3DPanel primary3DPanel;



   public void create(GDXImGuiBasedUI baseUI, CommunicationHelper communicationHelper, ROS2SyncedRobotModel syncedRobotModel)
   {
      this.syncedRobot = syncedRobotModel;
      this.baseUI = baseUI;
      this.communicationHelper = communicationHelper;
      primary3DPanel = baseUI.getPrimary3DPanel();
      primary3DPanel.addWindowDrawListAddition(this::renderTooltips);

      placeGoalActionMap = new GDXUIActionMap(startAction ->
                                              {
                                                 placingGoal = true;
                                                 placingPosition = true;
                                              });
      placeGoalActionMap.mapAction(GDXUITrigger.POSITION_LEFT_CLICK, trigger ->
      {
         placingPosition = false;
      });

      placeGoalActionMap.mapAction(GDXUITrigger.RIGHT_CLICK, trigger ->
      {
         placingGoal = false;
      });
      stepChecker = new SimpleStepChecker(baseUI, communicationHelper, syncedRobot);
      clear();
   }

   public void calculate3DViewPick(ImGui3DViewInput input)
   {
      renderTooltip = false;

      for (SingleFootstep singleFootstep : footstepArrayList)
      {
         singleFootstep.calculate3DViewPick(input);
      }
   }

   boolean renderTooltip = false;

   public void processImGui3DViewInput(ImGui3DViewInput input)
   {
      latestInput = input;

      for (SingleFootstep singleFootstep : footstepArrayList)
      {
         singleFootstep.process3DViewInput(input);
      }

      if (placingGoal && input.isWindowHovered())
      {
         Point3DReadOnly pickPointInWorld = input.getPickPointInWorld();
         renderTooltip = true;

         double z = (lastObjectIntersection != null ? lastObjectIntersection.getZ() : 0.0) + goalZOffset.get();
         if (placingPosition && footstepArrayList.size() > 0)
         {
            if (ImGui.getIO().getKeyCtrl())
            {
               goalZOffset.set(goalZOffset.get() - (input.getMouseWheelDelta() / 30.0f));
            }

            footstepArrayList.get(footstepIndex).getFootstepModelInstance().transform.setTranslation(pickPointInWorld.getX32(),
                                                                                                     pickPointInWorld.getY32(),
                                                                                                     pickPointInWorld.getZ32());
            footstepArrayList.get(footstepIndex).setFootPose(pickPointInWorld.getX(), pickPointInWorld.getY(), pickPointInWorld.getZ());
            footstepArrayList.get(footstepIndex).boundingSphere.getPosition().set(pickPointInWorld.getX(), pickPointInWorld.getY(), pickPointInWorld.getZ());

            // when left button clicked and released.
            if (input.mouseReleasedWithoutDrag(ImGuiMouseButton.Left))
            {
               placeGoalActionMap.triggerAction(GDXUITrigger.POSITION_LEFT_CLICK);
               placingGoal = true;
               placingPosition = true;
               footstepCreated = false;

               //Switch sides
               currentFootStepSide = currentFootStepSide.getOppositeSide();
               createNewFootStep(currentFootStepSide);
            }

            // hovering.
            // TODO: (need yaw here?)
            stepChecker.update(footstepArrayList, new DiscreteFootstep(pickPointInWorld.getX(), pickPointInWorld.getY(), 0, currentFootStepSide) , placingGoal);
            stepChecker.checkValidStep();
            stepChecker.getInput(input, placingGoal);
         }

         if (input.mouseReleasedWithoutDrag(ImGuiMouseButton.Right))
         {
            placeGoalActionMap.triggerAction(GDXUITrigger.RIGHT_CLICK);
//            baseUI.getPrimaryScene().removeRenderableAdapter((footstepArrayList.remove(footstepIndex).getFootstepModelInstance()), GDXSceneLevel.VIRTUAL);
            footstepIndex--;
         }
      }
   }

   public void renderImGuiWidgets()
   {
      ImGui.text("Place footstep:");
      ImGui.sameLine();
      if(ImGui.button(labels.get("Left")))
      {
         createNewFootStep(RobotSide.LEFT);
      }
      ImGui.sameLine();
      if(ImGui.button(labels.get("Right")))
      {
         createNewFootStep(RobotSide.RIGHT);
      }

      ImGui.sameLine();
      if (ImGui.button(labels.get("Walk")))
      {
         if(getFootstepArrayList().size() > 0)
         {
            walkFromSteps();
         }
      }
   }

   private void renderTooltips()
   {
      if (renderTooltip)
      {
         float offsetX = 10.0f;
         float offsetY = 10.0f;
         float mousePosX = latestInput.getMousePosX();
         float mousePosY = latestInput.getMousePosY();
         float drawStartX = primary3DPanel.getWindowDrawMinX() + mousePosX + offsetX;
         float drawStartY = primary3DPanel.getWindowDrawMinY() + mousePosY + offsetY;

         ImGui.getWindowDrawList().addRectFilled(drawStartX , drawStartY, drawStartX + 150.0f, drawStartY + 21.0f, new Color(0.2f, 0.2f, 0.2f, 0.7f).toIntBits());
         ImGui.getWindowDrawList()
              .addText(ImGuiTools.getSmallFont(),
                       ImGuiTools.getSmallFont().getFontSize(),
                       drawStartX + 5.0f,
                       drawStartY + 2.0f,
                       Color.WHITE.toIntBits(),
                       "Right click to exit");
      }
   }

   public void handleVREvents(GDXVRManager vrManager)
   {
      vrManager.getContext().getController(RobotSide.LEFT).runIfConnected(controller ->
      {
         InputDigitalActionData triggerClick = controller.getClickTriggerActionData();
         if (triggerClick.bChanged() && triggerClick.bState())
         {
            placingGoal = true;
         }
         if (triggerClick.bChanged() && !triggerClick.bState())
         {
            placingGoal = false;
         }

         controller.getTransformZUpToWorld(footstepArrayList.get(footstepIndex).getFootstepModelInstance().transform);

      });
   }

   @Override
   public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool)
   {
      if (isPlaced())
      {
         for (int i = 0; i < footstepArrayList.size(); i++)
         {
            footstepArrayList.get(i).getFootstepModelInstance().getRenderables(renderables, pool);
            footstepArrayList.get(i).getVirtualRenderables(renderables, pool);
         }
      }
   }

   public boolean isPlaced()
   {
      if (footstepArrayList.size() <= 0)
      {
         return false;
      }
      else
      {
         return !Float.isNaN(footstepArrayList.get(footstepIndex).getFootstepModelInstance().transform.val[Matrix4.M03]);
      }
   }

   public void update()
   {
      for (int i =0; i<footstepArrayList.size(); i++)
      {
         footstepArrayList.get(i).update();
      }
   }

   public void clear()
   {
      placingGoal = false;
      placingPosition = true;
      if (footstepArrayList.size() > 0 && footstepArrayList.get(footstepIndex).getFootstepModelInstance() != null)
         footstepArrayList.get(footstepIndex).getFootstepModelInstance().transform.val[Matrix4.M03] = Float.NaN;
      goalZOffset.set(0.0f);

//      for (int i = 0; i <= footstepIndex; i++)
//      {
//         baseUI.getPrimaryScene().removeRenderableAdapter((footstepArrayList.remove(0).getFootstepModelInstance()), GDXSceneLevel.VIRTUAL);
//      }

      footstepArrayList.clear();
      footstepIndex = -1;
   }

   private void walkFromSteps()
   {
      ArrayList<SingleFootstep> steps = footstepArrayList;

      FootstepDataListMessage messageList = new FootstepDataListMessage();
      for (SingleFootstep step : steps)
      {
         generateFootStepDataMessage(messageList, step);
         messageList.getQueueingProperties().setExecutionMode(ExecutionMode.OVERRIDE.toByte());
         messageList.getQueueingProperties().setMessageId(UUID.randomUUID().getLeastSignificantBits());
      }
      communicationHelper.publishToController(messageList);
      // done walking >> delete steps in singleFootStepAffordance.
      clear();
   }

   private void generateFootStepDataMessage(FootstepDataListMessage messageList, SingleFootstep step)
   {
      FootstepDataMessage stepMessage = messageList.getFootstepDataList().add();
      stepMessage.setRobotSide(step.getFootstepSide().toByte());
      stepMessage.getLocation().set(new Point3D(step.getSelectablePose3DGizmo().getPoseGizmo().getPose().getPosition()));
      stepMessage.setSwingDuration(1.2);
      stepMessage.setTransferDuration(0.8);
   }

   public Pose3DReadOnly getGoalPose()
   {
      return goalPoseForReading;
   }

   public void setGoalPoseAndPassOn(Pose3DReadOnly pose)
   {
      setGoalPoseNoCallbacks(pose);
   }

   public void setGoalPoseNoCallbacks(Pose3DReadOnly pose)
   {
      if (pose == null)
      {
         clear();
      }
      else
      {
         GDXTools.toGDX(pose.getPosition(), footstepArrayList.get(footstepIndex).getFootstepModelInstance().transform);
         goalZOffset.set((float) pose.getZ());
      }
      goalPoseForReading.set(pose);
   }

   public ArrayList<SingleFootstep> getFootstepArrayList()
   {
      return footstepArrayList;
   }

   public void createNewFootStep(RobotSide footstepSide)
   {
      placingGoal = true;
      footstepIndex++;
      footstepArrayList.add(new SingleFootstep(baseUI, footstepSide, footstepIndex));
      footstepCreated = true;
      currentFootStepSide = footstepSide;
   }
}
