package controller_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;

/**
       * This message is part of the IHMC whole-body controller API.
       * This message commands the controller to move the chest in both taskspace and jointspace
       * to the desired orientation and joint angles while going through the specified trajectory points.
       */
public class ChestHybridJointspaceTaskspaceTrajectoryMessage extends Packet<ChestHybridJointspaceTaskspaceTrajectoryMessage> implements Settable<ChestHybridJointspaceTaskspaceTrajectoryMessage>, EpsilonComparable<ChestHybridJointspaceTaskspaceTrajectoryMessage>
{
   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public long sequence_id_;
   /**
            * The taskspace trajectory information.
            */
   public controller_msgs.msg.dds.SO3TrajectoryMessage taskspace_trajectory_message_;
   /**
            * The jointspace trajectory information.
            * The indexing for the joints goes increasingly from the joint the closest to the pelvis to the joint the closest to the chest.
            */
   public controller_msgs.msg.dds.JointspaceTrajectoryMessage jointspace_trajectory_message_;

   public ChestHybridJointspaceTaskspaceTrajectoryMessage()
   {
      taskspace_trajectory_message_ = new controller_msgs.msg.dds.SO3TrajectoryMessage();
      jointspace_trajectory_message_ = new controller_msgs.msg.dds.JointspaceTrajectoryMessage();
   }

   public ChestHybridJointspaceTaskspaceTrajectoryMessage(ChestHybridJointspaceTaskspaceTrajectoryMessage other)
   {
      this();
      set(other);
   }

   public void set(ChestHybridJointspaceTaskspaceTrajectoryMessage other)
   {
      sequence_id_ = other.sequence_id_;

      controller_msgs.msg.dds.SO3TrajectoryMessagePubSubType.staticCopy(other.taskspace_trajectory_message_, taskspace_trajectory_message_);
      controller_msgs.msg.dds.JointspaceTrajectoryMessagePubSubType.staticCopy(other.jointspace_trajectory_message_, jointspace_trajectory_message_);
   }

   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public void setSequenceId(long sequence_id)
   {
      sequence_id_ = sequence_id;
   }
   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public long getSequenceId()
   {
      return sequence_id_;
   }


   /**
            * The taskspace trajectory information.
            */
   public controller_msgs.msg.dds.SO3TrajectoryMessage getTaskspaceTrajectoryMessage()
   {
      return taskspace_trajectory_message_;
   }


   /**
            * The jointspace trajectory information.
            * The indexing for the joints goes increasingly from the joint the closest to the pelvis to the joint the closest to the chest.
            */
   public controller_msgs.msg.dds.JointspaceTrajectoryMessage getJointspaceTrajectoryMessage()
   {
      return jointspace_trajectory_message_;
   }


   @Override
   public boolean epsilonEquals(ChestHybridJointspaceTaskspaceTrajectoryMessage other, double epsilon)
   {
      if(other == null) return false;
      if(other == this) return true;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.sequence_id_, other.sequence_id_, epsilon)) return false;

      if (!this.taskspace_trajectory_message_.epsilonEquals(other.taskspace_trajectory_message_, epsilon)) return false;
      if (!this.jointspace_trajectory_message_.epsilonEquals(other.jointspace_trajectory_message_, epsilon)) return false;

      return true;
   }

   @Override
   public boolean equals(Object other)
   {
      if(other == null) return false;
      if(other == this) return true;
      if(!(other instanceof ChestHybridJointspaceTaskspaceTrajectoryMessage)) return false;

      ChestHybridJointspaceTaskspaceTrajectoryMessage otherMyClass = (ChestHybridJointspaceTaskspaceTrajectoryMessage) other;

      if(this.sequence_id_ != otherMyClass.sequence_id_) return false;

      if (!this.taskspace_trajectory_message_.equals(otherMyClass.taskspace_trajectory_message_)) return false;
      if (!this.jointspace_trajectory_message_.equals(otherMyClass.jointspace_trajectory_message_)) return false;

      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("ChestHybridJointspaceTaskspaceTrajectoryMessage {");
      builder.append("sequence_id=");
      builder.append(this.sequence_id_);      builder.append(", ");
      builder.append("taskspace_trajectory_message=");
      builder.append(this.taskspace_trajectory_message_);      builder.append(", ");
      builder.append("jointspace_trajectory_message=");
      builder.append(this.jointspace_trajectory_message_);
      builder.append("}");
      return builder.toString();
   }
}
