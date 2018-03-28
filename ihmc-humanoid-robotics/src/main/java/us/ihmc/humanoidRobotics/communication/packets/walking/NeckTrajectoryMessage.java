package us.ihmc.humanoidRobotics.communication.packets.walking;

import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.ros.generators.RosExportedField;
import us.ihmc.communication.ros.generators.RosMessagePacket;
import us.ihmc.humanoidRobotics.communication.packets.JointspaceTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.PacketValidityChecker;

@RosMessagePacket(documentation =
      "This message commands the controller to move the neck in jointspace to the desired joint angles while going through the specified trajectory points."
      + " A third order polynomial function is used to interpolate between trajectory points."
      + " A message with a unique id equals to 0 will be interpreted as invalid and will not be processed by the controller. This rule does not apply to the fields of this message.",
                  rosPackage = RosMessagePacket.CORE_IHMC_PACKAGE,
                  topic = "/control/neck_trajectory")
public class NeckTrajectoryMessage extends Packet<NeckTrajectoryMessage>
{
   @RosExportedField(documentation = "Trajectories for each joint.")
   public JointspaceTrajectoryMessage jointspaceTrajectory = new JointspaceTrajectoryMessage();

   /**
    * Empty constructor for serialization.
    * Set the id of the message to {@link Packet#VALID_MESSAGE_DEFAULT_ID}.
    */
   public NeckTrajectoryMessage()
   {
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
   }

   /**
    * Clone constructor.
    * @param neckTrajectoryMessage message to clone.
    */
   public NeckTrajectoryMessage(NeckTrajectoryMessage neckTrajectoryMessage)
   {
      jointspaceTrajectory = new JointspaceTrajectoryMessage(neckTrajectoryMessage.jointspaceTrajectory);
   }

   @Override
   public void set(NeckTrajectoryMessage other)
   {
      jointspaceTrajectory = new JointspaceTrajectoryMessage();
      jointspaceTrajectory.set(other.jointspaceTrajectory);
      setPacketInformation(other);
   }

   public void setJointspaceTrajectory(JointspaceTrajectoryMessage jointspaceTrajectory)
   {
      this.jointspaceTrajectory = jointspaceTrajectory;
   }

   public JointspaceTrajectoryMessage getJointspaceTrajectory()
   {
      return jointspaceTrajectory;
   }

   @Override
   public boolean epsilonEquals(NeckTrajectoryMessage other, double epsilon)
   {
      if (!jointspaceTrajectory.epsilonEquals(other.jointspaceTrajectory, epsilon))
         return false;
      return true;
   }

   @Override
   public String validateMessage()
   {
      return PacketValidityChecker.validateNeckTrajectoryMessage(this);
   }
}
