package us.ihmc.rdx.ui.teleoperation.locomotion;

import imgui.ImGui;
import us.ihmc.behaviors.tools.CommunicationHelper;
import us.ihmc.communication.controllerAPI.RobotLowLevelMessenger;
import us.ihmc.log.LogTools;
import us.ihmc.rdx.imgui.ImGuiUniqueLabelMap;

public class RDXWalkingLowLevelMessenger
{
   private final ImGuiUniqueLabelMap labels = new ImGuiUniqueLabelMap(getClass());

   private final RobotLowLevelMessenger robotLowLevelMessenger;

   public boolean robotPausedWalking = false;

   public RDXWalkingLowLevelMessenger(CommunicationHelper communicationHelper)
   {
      robotLowLevelMessenger = communicationHelper.getOrCreateRobotLowLevelMessenger();

      if (robotLowLevelMessenger == null)
      {
         String robotName = communicationHelper.getRobotModel().getSimpleRobotName();
         throw new RuntimeException("Please add implementation of RobotLowLevelMessenger for " + robotName);
      }
   }

   public void renderImGuiWidgets()
   {
      if (ImGui.button(labels.get("Pause")))
      {
         sendPauseWalkingRequest();
      }
      ImGui.sameLine();
      if (ImGui.button(labels.get("Continue")))
      {
         sendContinueWalkingRequest();
      }
   }

   public boolean getRobotPausedWalking()
   {
      return robotPausedWalking;
   }

   public void setRobotPausedWalking(boolean newValue)
   {
      robotPausedWalking = newValue;
   }

   public void sendPauseWalkingRequest()
   {
      LogTools.info("Paused walking");
      robotLowLevelMessenger.sendPauseWalkingRequest();
      robotPausedWalking = true;
   }

   public void sendContinueWalkingRequest()
   {
      LogTools.info("Resumed walking");
      robotLowLevelMessenger.sendContinueWalkingRequest();
      robotPausedWalking = false;
   }
}
