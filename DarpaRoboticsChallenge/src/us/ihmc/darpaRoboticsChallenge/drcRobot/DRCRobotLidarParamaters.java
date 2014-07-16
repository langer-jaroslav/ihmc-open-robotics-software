package us.ihmc.darpaRoboticsChallenge.drcRobot;


public class DRCRobotLidarParamaters
{
   private final String lidarSensorName;
   private final String laserTopic;
   private final String lidarSpindleJointName; 
   private final String lidarSpindleJointTopic;
   private final String lidarBaseFrameForRos;
   private final String lidarEndFrameForRos;
   private final double lidarSpinVelocity;

   public DRCRobotLidarParamaters(String lidarSensorName, String laserTopic, String lidarSpindleJointName, String lidarSpindleJointTopic, String lidarBaseFrameForRos, String lidarEndFrameForRos, double lidar_spindle_velocity)
   {
      this.lidarSensorName = lidarSensorName;
      this.laserTopic = laserTopic;
      this.lidarSpindleJointName =  lidarSpindleJointName;
      this.lidarSpindleJointTopic = lidarSpindleJointTopic;
      this.lidarBaseFrameForRos = lidarBaseFrameForRos;
      this.lidarEndFrameForRos = lidarEndFrameForRos;
      this.lidarSpinVelocity = lidar_spindle_velocity; 
   }
   
   public String getLidarSensorNameInSdf()
   {
      return lidarSensorName;
   }

   public String getLaserTopic()
   {
      return laserTopic;
   }

   public String getLidarSpindleJointName()
   {
      return lidarSpindleJointName;
   }

   public String getLidarSpindleJointTopic()
   {
      return lidarSpindleJointTopic;
   }

   public String getLidarBaseFrameForRos()
   {
      return lidarBaseFrameForRos;
   }

   public String getLidarEndFrameForRos()
   {
      return lidarEndFrameForRos;
   }

   public double getLidarSpindleVelocity()
   {
      return lidarSpinVelocity;
   }

}
