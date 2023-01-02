package us.ihmc.rdx.ui.behavior.editor.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import imgui.ImGui;
import us.ihmc.behaviors.sequence.actions.ArmJointAnglesAction;
import us.ihmc.behaviors.sequence.actions.ArmJointAnglesActionData;
import us.ihmc.rdx.imgui.ImDoubleWrapper;
import us.ihmc.rdx.imgui.ImGuiUniqueLabelMap;
import us.ihmc.rdx.imgui.ImIntegerWrapper;
import us.ihmc.rdx.ui.behavior.editor.RDXBehaviorAction;

public class RDXArmJointAnglesAction extends RDXBehaviorAction
{
   private final ArmJointAnglesActionData action = new ArmJointAnglesActionData();
   private final ImGuiUniqueLabelMap labels = new ImGuiUniqueLabelMap(getClass());
   private final ImIntegerWrapper sideWidget = new ImIntegerWrapper(action::getSide, action::setSide, labels.get("Side"));
   private final ImDoubleWrapper[] jointAngleWidgets = new ImDoubleWrapper[ArmJointAnglesAction.NUMBER_OF_JOINTS];
   private final ImDoubleWrapper trajectoryDurationWidget = new ImDoubleWrapper(action::getTrajectoryDuration,
                                                                                action::setTrajectoryDuration,
                                                                                imDouble -> ImGui.inputDouble(labels.get("Trajectory duration"), imDouble));

   public RDXArmJointAnglesAction()
   {
      super("Arm Joint Angles");

      for (int i = 0; i < ArmJointAnglesAction.NUMBER_OF_JOINTS; i++)
      {
         int jointIndex = i;
         jointAngleWidgets[i] = new ImDoubleWrapper(() -> action.getJointAngles()[jointIndex],
                                              jointAngle -> action.getJointAngles()[jointIndex] = jointAngle,
                                              imDouble -> ImGui.inputDouble(labels.get("j" + jointIndex), imDouble));
      }
   }

   @Override
   public void renderImGuiSettingWidgets()
   {
      ImGui.pushItemWidth(100.0f);
      sideWidget.renderImGuiWidget();
      ImGui.popItemWidth();
      ImGui.pushItemWidth(80.0f);
      trajectoryDurationWidget.renderImGuiWidget();
      for (int i = 0; i < ArmJointAnglesAction.NUMBER_OF_JOINTS; i++)
      {
         jointAngleWidgets[i].renderImGuiWidget();
      }
      ImGui.popItemWidth();
   }

   @Override
   public void saveToFile(ObjectNode jsonNode)
   {
      action.saveToFile(jsonNode);
   }

   @Override
   public void loadFromFile(JsonNode jsonNode)
   {
      action.loadFromFile(jsonNode);
   }
}
