package controller_msgs.msg.dds;

/**
 * Topic data type of the struct "UIConnectedPacket" defined in "UIConnectedPacket_.idl". Use this class to provide the TopicDataType to a Participant.
 *
 * This file was automatically generated from UIConnectedPacket_.idl by us.ihmc.idl.generator.IDLGenerator.
 * Do not update this file directly, edit UIConnectedPacket_.idl instead.
 */
public class UIConnectedPacketPubSubType implements us.ihmc.pubsub.TopicDataType<controller_msgs.msg.dds.UIConnectedPacket>
{
   public static final java.lang.String name = "controller_msgs::msg::dds_::UIConnectedPacket_";
   private final us.ihmc.idl.CDR serializeCDR = new us.ihmc.idl.CDR();
   private final us.ihmc.idl.CDR deserializeCDR = new us.ihmc.idl.CDR();

   public UIConnectedPacketPubSubType()
   {

   }

   public static int getMaxCdrSerializedSize()
   {
      return getMaxCdrSerializedSize(0);
   }

   public static int getMaxCdrSerializedSize(int current_alignment)
   {
      int initial_alignment = current_alignment;

      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);

      return current_alignment - initial_alignment;
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.UIConnectedPacket data)
   {
      return getCdrSerializedSize(data, 0);
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.UIConnectedPacket data, int current_alignment)
   {
      int initial_alignment = current_alignment;

      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);

      return current_alignment - initial_alignment;
   }

   public static void write(controller_msgs.msg.dds.UIConnectedPacket data, us.ihmc.idl.CDR cdr)
   {

      cdr.write_type_7(data.getUnusedPlaceholderField());
   }

   public static void read(controller_msgs.msg.dds.UIConnectedPacket data, us.ihmc.idl.CDR cdr)
   {

      data.setUnusedPlaceholderField(cdr.read_type_7());
   }

   public static void staticCopy(controller_msgs.msg.dds.UIConnectedPacket src, controller_msgs.msg.dds.UIConnectedPacket dest)
   {
      dest.set(src);
   }

   @Override
   public void serialize(controller_msgs.msg.dds.UIConnectedPacket data, us.ihmc.pubsub.common.SerializedPayload serializedPayload) throws java.io.IOException
   {
      serializeCDR.serialize(serializedPayload);
      write(data, serializeCDR);
      serializeCDR.finishSerialize();
   }

   @Override
   public void deserialize(us.ihmc.pubsub.common.SerializedPayload serializedPayload, controller_msgs.msg.dds.UIConnectedPacket data) throws java.io.IOException
   {
      deserializeCDR.deserialize(serializedPayload);
      read(data, deserializeCDR);
      deserializeCDR.finishDeserialize();
   }

   @Override
   public final void serialize(controller_msgs.msg.dds.UIConnectedPacket data, us.ihmc.idl.InterchangeSerializer ser)
   {
      ser.write_type_7("unused_placeholder_field", data.getUnusedPlaceholderField());
   }

   @Override
   public final void deserialize(us.ihmc.idl.InterchangeSerializer ser, controller_msgs.msg.dds.UIConnectedPacket data)
   {
      data.setUnusedPlaceholderField(ser.read_type_7("unused_placeholder_field"));
   }

   @Override
   public controller_msgs.msg.dds.UIConnectedPacket createData()
   {
      return new controller_msgs.msg.dds.UIConnectedPacket();
   }

   @Override
   public int getTypeSize()
   {
      return us.ihmc.idl.CDR.getTypeSize(getMaxCdrSerializedSize());
   }

   @Override
   public java.lang.String getName()
   {
      return name;
   }

   public void serialize(controller_msgs.msg.dds.UIConnectedPacket data, us.ihmc.idl.CDR cdr)
   {
      write(data, cdr);
   }

   public void deserialize(controller_msgs.msg.dds.UIConnectedPacket data, us.ihmc.idl.CDR cdr)
   {
      read(data, cdr);
   }

   public void copy(controller_msgs.msg.dds.UIConnectedPacket src, controller_msgs.msg.dds.UIConnectedPacket dest)
   {
      staticCopy(src, dest);
   }

   @Override
   public UIConnectedPacketPubSubType newInstance()
   {
      return new UIConnectedPacketPubSubType();
   }
}