package us.ihmc.commonWalkingControlModules.controllerAPI.input.command;

import us.ihmc.humanoidRobotics.communication.packets.walking.AutomaticManipulationAbortMessage;

public class ModifiableAutomaticManipulationAbortMessage implements ControllerMessage<ModifiableAutomaticManipulationAbortMessage, AutomaticManipulationAbortMessage>
{
   private boolean enable;

   public ModifiableAutomaticManipulationAbortMessage()
   {
   }

   @Override
   public void set(ModifiableAutomaticManipulationAbortMessage other)
   {
      enable = other.enable;
   }

   @Override
   public void set(AutomaticManipulationAbortMessage message)
   {
      enable = message.enable;
   }

   public boolean isEnable()
   {
      return enable;
   }

   public void setEnable(boolean enable)
   {
      this.enable = enable;
   }

   @Override
   public Class<AutomaticManipulationAbortMessage> getMessageClass()
   {
      return AutomaticManipulationAbortMessage.class;
   }
}
