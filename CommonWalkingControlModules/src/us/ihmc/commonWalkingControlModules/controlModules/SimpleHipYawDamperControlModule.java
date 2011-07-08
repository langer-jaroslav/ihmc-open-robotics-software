package us.ihmc.commonWalkingControlModules.controlModules;

import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.HipDamperControlModule;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LegJointName;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LegTorques;
import us.ihmc.commonWalkingControlModules.sensors.ProcessedSensorsInterface;
import us.ihmc.robotSide.RobotSide;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class SimpleHipYawDamperControlModule implements HipDamperControlModule
{
   private final ProcessedSensorsInterface processedSensors;
   private final YoVariableRegistry registry = new YoVariableRegistry("SimpleHipDamperControlModule");
   private final DoubleYoVariable hipYawDamping = new DoubleYoVariable("hipYawDamping", registry);

   public SimpleHipYawDamperControlModule(ProcessedSensorsInterface processedSensors, YoVariableRegistry parentRegistry)
   {
      this.processedSensors = processedSensors;
   }

   public void addHipDamping(LegTorques legTorques)
   {
      RobotSide robotSide = legTorques.getRobotSide();
      LegJointName hipYaw = LegJointName.HIP_YAW;
      double dampingTorque = -hipYawDamping.getDoubleValue() * processedSensors.getLegJointVelocity(robotSide, hipYaw);
      legTorques.addTorque(hipYaw, dampingTorque);
   }
   
   public void setParametersForR2()
   {
      hipYawDamping.set(10.0);
   }
   
   public void setParametersForM2V2()
   {
      hipYawDamping.set(0.0);
   }

}
