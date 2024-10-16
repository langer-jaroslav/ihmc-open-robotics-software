package us.ihmc.communication.crdt;

import us.ihmc.communication.ros2.ROS2ActorDesignation;

/**
 * Represents an enum field that should only be modified by one actor type
 * and read-only for the others.
 */
public class CRDTUnidirectionalEnumField<T extends Enum<T>> extends CRDTUnidirectionalImmutableField<T>
{
   public CRDTUnidirectionalEnumField(ROS2ActorDesignation sideThatCanModify, RequestConfirmFreezable requestConfirmFreezable, T initialValue)
   {
      super(sideThatCanModify, requestConfirmFreezable, initialValue);
   }

   public int toMessageOrdinal()
   {
      return toMessage() == null ? -1 : toMessage().ordinal();
   }

   /**
    * @param messageValue i.e. message.getFieldName()
    * @param enumValues T.values()
    */
   public void fromMessageOrdinal(int messageValue, T[] enumValues)
   {
      fromMessage(messageValue == -1 ? null : enumValues[messageValue]);
   }
}
