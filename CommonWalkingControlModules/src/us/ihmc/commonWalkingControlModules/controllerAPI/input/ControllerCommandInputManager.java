package us.ihmc.commonWalkingControlModules.controllerAPI.input;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ControllerMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiableAbortWalkingMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiableArmDesiredAccelerationsMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiableArmTrajectoryMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiableAutomaticManipulationAbortMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiableChestTrajectoryMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiableEndEffectorLoadBearingMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiableFootTrajectoryMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiableFootstepDataListMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiableGoHomeMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiableHandComplianceControlParametersMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiableHandTrajectoryMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiableHeadTrajectoryMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiableHighLevelStateMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiablePauseWalkingMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiablePelvisHeightTrajectoryMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiablePelvisOrientationTrajectoryMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiablePelvisTrajectoryMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiableStopAllTrajectoryMessage;
import us.ihmc.communication.packets.Packet;
import us.ihmc.concurrent.Builder;
import us.ihmc.concurrent.ConcurrentRingBuffer;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.ArmTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.ChestTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.wholebody.WholeBodyTrajectoryMessage;
import us.ihmc.robotics.lists.RecyclingArrayList;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.tools.io.printing.PrintTools;

public class ControllerCommandInputManager
{
   private final int buffersCapacity = 8;

   private final List<ConcurrentRingBuffer<?>> allBuffers = new ArrayList<>();
   private final Map<Class<? extends ControllerMessage<?, ?>>, ConcurrentRingBuffer<? extends ControllerMessage<?, ?>>> modifiableMessageClassToBufferMap = new HashMap<>();
   private final Map<Class<? extends Packet<?>>, ConcurrentRingBuffer<? extends ControllerMessage<?, ?>>> messageClassToBufferMap = new HashMap<>();
   private final Map<Class<? extends ControllerMessage<?, ?>>, RecyclingArrayList<? extends ControllerMessage<?, ?>>> controllerMessagesMap = new HashMap<>();

   private final List<Class<? extends Packet<?>>> listOfSupportedMessages;

   public ControllerCommandInputManager()
   {
      createBuffer(ModifiableArmTrajectoryMessage.class);
      createBuffer(ModifiableHandTrajectoryMessage.class);
      createBuffer(ModifiableFootTrajectoryMessage.class);
      createBuffer(ModifiableHeadTrajectoryMessage.class);
      createBuffer(ModifiableChestTrajectoryMessage.class);
      createBuffer(ModifiablePelvisTrajectoryMessage.class);
      createBuffer(ModifiablePelvisOrientationTrajectoryMessage.class);
      createBuffer(ModifiablePelvisHeightTrajectoryMessage.class);
      createBuffer(ModifiableStopAllTrajectoryMessage.class);
      createBuffer(ModifiableFootstepDataListMessage.class);
      createBuffer(ModifiableGoHomeMessage.class);
      createBuffer(ModifiableEndEffectorLoadBearingMessage.class);
      createBuffer(ModifiableArmDesiredAccelerationsMessage.class);
      createBuffer(ModifiableAutomaticManipulationAbortMessage.class);
      createBuffer(ModifiableHandComplianceControlParametersMessage.class);
      createBuffer(ModifiableHighLevelStateMessage.class);
      createBuffer(ModifiableAbortWalkingMessage.class);
      createBuffer(ModifiablePauseWalkingMessage.class);

      listOfSupportedMessages = new ArrayList<>(messageClassToBufferMap.keySet());
      // This message has to be added manually as it is handled in a different way to the others.
      listOfSupportedMessages.add(WholeBodyTrajectoryMessage.class);
   }

   private <T extends ControllerMessage<T, M>, M extends Packet<M>> ConcurrentRingBuffer<T> createBuffer(Class<T> clazz)
   {
      Builder<T> builer = createBuilderWithEmptyConstructor(clazz);
      ConcurrentRingBuffer<T> newBuffer = new ConcurrentRingBuffer<>(builer, buffersCapacity);
      allBuffers.add(newBuffer);
      // This is retarded, but I could not find another way that is more elegant.
      Class<M> messageClass = builer.newInstance().getMessageClass();
      modifiableMessageClassToBufferMap.put(clazz, newBuffer);
      messageClassToBufferMap.put(messageClass, newBuffer);
      controllerMessagesMap.put(clazz, new RecyclingArrayList<>(buffersCapacity, clazz));

      return newBuffer;
   }

   public <M extends Packet<M>> void submitMessage(M message)
   {
      if (message instanceof WholeBodyTrajectoryMessage)
      {
         submitWholeBodyTrajectoryMessage((WholeBodyTrajectoryMessage) message);
         return;
      }

      ConcurrentRingBuffer<? extends ControllerMessage<?, ?>> buffer = messageClassToBufferMap.get(message.getClass());
      if (buffer == null)
      {
         PrintTools.error(this, "The message type " + message.getClass().getSimpleName() + " is not supported.");
         return;
      }
      @SuppressWarnings("unchecked")
      ControllerMessage<?, M> nextModifiableMessage = (ControllerMessage<?, M>) buffer.next();
      if (nextModifiableMessage == null)
         return;
      nextModifiableMessage.set(message);
      buffer.commit();
   }

   public <T extends ControllerMessage<T, ?>> void submitModifiableMessage(T modifiableMessage)
   {
      ConcurrentRingBuffer<? extends ControllerMessage<?, ?>> buffer = modifiableMessageClassToBufferMap.get(modifiableMessage.getClass());
      if (buffer == null)
      {
         PrintTools.error(this, "The message type " + modifiableMessage.getClass().getSimpleName() + " is not supported.");
         return;
      }
      @SuppressWarnings("unchecked")
      ControllerMessage<T, ?> nextModifiableMessage = (ControllerMessage<T, ?>) buffer.next();
      if (nextModifiableMessage == null)
         return;
      nextModifiableMessage.set(modifiableMessage);
      buffer.commit();
   }

   public void submitWholeBodyTrajectoryMessage(WholeBodyTrajectoryMessage wholeBodyTrajectoryMessage)
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         ArmTrajectoryMessage armTrajectoryMessage = wholeBodyTrajectoryMessage.getArmTrajectoryMessage(robotSide);
         if (armTrajectoryMessage != null && armTrajectoryMessage.getUniqueId() != Packet.INVALID_MESSAGE_ID)
            submitMessage(armTrajectoryMessage);
         HandTrajectoryMessage handTrajectoryMessage = wholeBodyTrajectoryMessage.getHandTrajectoryMessage(robotSide);
         if (handTrajectoryMessage != null && handTrajectoryMessage.getUniqueId() != Packet.INVALID_MESSAGE_ID)
            submitMessage(handTrajectoryMessage);
         FootTrajectoryMessage footTrajectoryMessage = wholeBodyTrajectoryMessage.getFootTrajectoryMessage(robotSide);
         if (footTrajectoryMessage != null && footTrajectoryMessage.getUniqueId() != Packet.INVALID_MESSAGE_ID)
            submitMessage(footTrajectoryMessage);
      }

      PelvisTrajectoryMessage pelvisTrajectoryMessage = wholeBodyTrajectoryMessage.getPelvisTrajectoryMessage();
      if (pelvisTrajectoryMessage != null && pelvisTrajectoryMessage.getUniqueId() != Packet.INVALID_MESSAGE_ID)
         submitMessage(pelvisTrajectoryMessage);
      ChestTrajectoryMessage chestTrajectoryMessage = wholeBodyTrajectoryMessage.getChestTrajectoryMessage();
      if (chestTrajectoryMessage != null && chestTrajectoryMessage.getUniqueId() != Packet.INVALID_MESSAGE_ID)
         submitMessage(chestTrajectoryMessage);
   }

   public boolean isNewMessageAvailable(Class<? extends ControllerMessage<?, ?>> messageClassToCheck)
   {
      return modifiableMessageClassToBufferMap.get(messageClassToCheck).poll();
   }

   public ModifiableEndEffectorLoadBearingMessage pollAndCompileEndEffectorLoadBearingMessages()
   {
      RecyclingArrayList<ModifiableEndEffectorLoadBearingMessage> messages = pollNewMessages(ModifiableEndEffectorLoadBearingMessage.class);
      for (int i = 1; i < messages.size(); i++)
         messages.get(0).set(messages.get(i));
      return messages.get(0);
   }

   public ModifiableGoHomeMessage pollAndCompileGoHomeMessages()
   {
      RecyclingArrayList<ModifiableGoHomeMessage> messages = pollNewMessages(ModifiableGoHomeMessage.class);
      for (int i = 1; i < messages.size(); i++)
         messages.get(0).set(messages.get(i));
      return messages.get(0);
   }

   public void flushManipulationBuffers()
   {
      flushMessages(ModifiableHandTrajectoryMessage.class);
      flushMessages(ModifiableArmTrajectoryMessage.class);
      flushMessages(ModifiableArmDesiredAccelerationsMessage.class);
      flushMessages(ModifiableHandComplianceControlParametersMessage.class);
   }

   public void flushPelvisBuffers()
   {
      flushMessages(ModifiablePelvisTrajectoryMessage.class);
      flushMessages(ModifiablePelvisOrientationTrajectoryMessage.class);
      flushMessages(ModifiablePelvisHeightTrajectoryMessage.class);
   }

   public void flushFootstepBuffers()
   {
      flushMessages(ModifiableFootstepDataListMessage.class);
   }

   public void flushFlamingoBuffers()
   {
      flushMessages(ModifiableFootTrajectoryMessage.class);
   }

   public void flushBuffers()
   {
      for (int i = 0; i < allBuffers.size(); i++)
         allBuffers.get(i).flush();
   }

   public <T extends ControllerMessage<T, ?>> void flushMessages(Class<T> messageToFlushClass)
   {
      modifiableMessageClassToBufferMap.get(messageToFlushClass).flush();
   }

   public <T extends ControllerMessage<T, ?>> T pollNewestMessage(Class<T> messageToPollClass)
   {
      return pollNewMessages(messageToPollClass).getLast();
   }

   @SuppressWarnings("unchecked")
   public <T extends ControllerMessage<T, ?>> RecyclingArrayList<T> pollNewMessages(Class<T> messageToPollClass)
   {
      RecyclingArrayList<T> messages = (RecyclingArrayList<T>) controllerMessagesMap.get(messageToPollClass);
      messages.clear();
      ConcurrentRingBuffer<T> buffer = (ConcurrentRingBuffer<T>) modifiableMessageClassToBufferMap.get(messageToPollClass);
      pollNewMessages(buffer, messages);
      return messages;
   }

   private static <T extends ControllerMessage<T, ?>> void pollNewMessages(ConcurrentRingBuffer<T> buffer, RecyclingArrayList<T> messagesToPack)
   {
      if (buffer.poll())
      {
         T message;
         while ((message = buffer.read()) != null)
         {
            messagesToPack.add().set(message);
            message.clear();
         }
         buffer.flush();
      }
   }

   public static <U> Builder<U> createBuilderWithEmptyConstructor(Class<U> clazz)
   {
      final Constructor<U> emptyConstructor;
      // Trying to get an empty constructor from clazz
      try
      {
         emptyConstructor = clazz.getConstructor();
      }
      catch (NoSuchMethodException | SecurityException e)
      {
         throw new RuntimeException("Could not find a visible empty constructor in the class: " + clazz.getSimpleName());
      }

      Builder<U> builder = new Builder<U>()
      {
         @Override
         public U newInstance()
         {
            U newInstance = null;

            try
            {
               newInstance = emptyConstructor.newInstance();
            }
            catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
            {
               e.printStackTrace();
               throw new RuntimeException(
                     "Something went wrong the empty constructor implemented in the class: " + emptyConstructor.getDeclaringClass().getSimpleName());
            }

            return newInstance;
         }
      };
      return builder;
   }

   public List<Class<? extends Packet<?>>> getListOfSupportedMessages()
   {
      return listOfSupportedMessages;
   }
}
