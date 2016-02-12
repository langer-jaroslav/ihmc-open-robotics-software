package us.ihmc.humanoidRobotics.communication.packets.walking;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.communication.packetAnnotations.ClassDocumentation;
import us.ihmc.communication.packetAnnotations.FieldDocumentation;
import us.ihmc.communication.packets.IHMCRosApiPacket;
import us.ihmc.communication.packets.VisualizablePacket;
import us.ihmc.humanoidRobotics.communication.TransformableDataObject;
import us.ihmc.humanoidRobotics.communication.packets.SO3WaypointMessage;
import us.ihmc.robotics.geometry.RigidBodyTransform;

@ClassDocumentation("This message commands the controller to move in taskspace the chest to the desired orientation while going through the specified waypoints."
      + " A hermite based curve (third order) is used to interpolate the orientations."
      + " To excute a simple trajectory to reach a desired chest orientation, set only one waypoint with zero velocity and its time to be equal to the desired trajectory time."
      + " A message with a unique id equals to 0 will be interpreted as invalid and will not be processed by the controller. This rule does not apply to the fields of this message.")
public class ChestTrajectoryMessage extends IHMCRosApiPacket<ChestTrajectoryMessage>
      implements TransformableDataObject<ChestTrajectoryMessage>, VisualizablePacket
{
   @FieldDocumentation("List of waypoints (in taskpsace) to go through while executing the trajectory. All the information contained in these waypoints needs to be expressed in world frame.")
   public SO3WaypointMessage[] taskspaceWaypoints;

   /**
    * Empty constructor for serialization.
    */
   public ChestTrajectoryMessage()
   {
      setUniqueId(1L);
   }

   public ChestTrajectoryMessage(ChestTrajectoryMessage chestTrajectoryMessage)
   {
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
      setDestination(chestTrajectoryMessage.getDestination());
      taskspaceWaypoints = new SO3WaypointMessage[chestTrajectoryMessage.getNumberOfWaypoints()];
      for (int i = 0; i < getNumberOfWaypoints(); i++)
         taskspaceWaypoints[i] = new SO3WaypointMessage(chestTrajectoryMessage.taskspaceWaypoints[i]);
   }

   /**
    * Use this constructor to execute a simple interpolation in taskspace to the desired orientation.
    * @param trajectoryTime how long it takes to reach the desired orientation.
    * @param desiredOrientation desired chest orientation expressed in world frame.
    */
   public ChestTrajectoryMessage(double trajectoryTime, Quat4d desiredOrientation)
   {
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
      Vector3d zeroAngularVelocity = new Vector3d();
      taskspaceWaypoints = new SO3WaypointMessage[] {new SO3WaypointMessage(trajectoryTime, desiredOrientation, zeroAngularVelocity)};
   }

   /**
    * Use this constructor to build a message with more than one waypoint.
    * This constructor only allocates memory for the waypoints, you need to call {@link #setWaypoint(int, double, Quat4d, Vector3d)} for each waypoint afterwards.
    * @param numberOfWaypoints number of waypoints that will be sent to the controller.
    */
   public ChestTrajectoryMessage(int numberOfWaypoints)
   {
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
      taskspaceWaypoints = new SO3WaypointMessage[numberOfWaypoints];
   }

   /**
    * Create a waypoint.
    * @param waypointIndex index of the waypoint to create.
    * @param time time at which the waypoint has to be reached. The time is relative to when the trajectory starts.
    * @param orientation define the desired 3D orientation to be reached at this waypoint. It is expressed in world frame.
    * @param angularVelocity define the desired 3D angular velocity to be reached at this waypoint. It is expressed in world frame.
    */
   public void setWaypoint(int waypointIndex, double time, Quat4d orientation, Vector3d angularVelocity)
   {
      rangeCheck(waypointIndex);
      taskspaceWaypoints[waypointIndex] = new SO3WaypointMessage(time, orientation, angularVelocity);
   }

   public int getNumberOfWaypoints()
   {
      return taskspaceWaypoints.length;
   }

   public SO3WaypointMessage getWaypoint(int waypointIndex)
   {
      rangeCheck(waypointIndex);
      return taskspaceWaypoints[waypointIndex];
   }

   public SO3WaypointMessage[] getWaypoints()
   {
      return taskspaceWaypoints;
   }

   private void rangeCheck(int waypointIndex)
   {
      if (waypointIndex >= getNumberOfWaypoints() || waypointIndex < 0)
         throw new IndexOutOfBoundsException("Waypoint index: " + waypointIndex + ", number of waypoints: " + getNumberOfWaypoints());
   }

   @Override
   public boolean epsilonEquals(ChestTrajectoryMessage other, double epsilon)
   {
      if (getNumberOfWaypoints() != other.getNumberOfWaypoints())
         return false;

      for (int i = 0; i < getNumberOfWaypoints(); i++)
      {
         if (!taskspaceWaypoints[i].epsilonEquals(other.taskspaceWaypoints[i], epsilon))
            return false;
      }

      return true;
   }

   @Override
   public ChestTrajectoryMessage transform(RigidBodyTransform transform)
   {
      ChestTrajectoryMessage transformedChestTrajectoryMessage = new ChestTrajectoryMessage(getNumberOfWaypoints());

      for (int i = 0; i < getNumberOfWaypoints(); i++)
         transformedChestTrajectoryMessage.taskspaceWaypoints[i] = taskspaceWaypoints[i].transform(transform);

      return transformedChestTrajectoryMessage;
   }

   @Override
   public String toString()
   {
      if (taskspaceWaypoints != null)
         return "Chest SO3 trajectory: number of SO3 waypoints = " + getNumberOfWaypoints();
      else
         return "Chest SO3 trajectory: no SO3 waypoints";
   }
}
