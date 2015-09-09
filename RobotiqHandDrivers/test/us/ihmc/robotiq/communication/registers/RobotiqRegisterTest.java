package us.ihmc.robotiq.communication.registers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import us.ihmc.robotiq.communication.Finger;
import us.ihmc.robotiq.communication.registers.ActionRequestRegister.rACT;
import us.ihmc.robotiq.communication.registers.ActionRequestRegister.rATR;
import us.ihmc.robotiq.communication.registers.ActionRequestRegister.rGTO;
import us.ihmc.robotiq.communication.registers.ActionRequestRegister.rMOD;
import us.ihmc.tools.testing.BambooPlanType;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestClass;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

@DeployableTestClass(planType = BambooPlanType.Fast)
public class RobotiqRegisterTest
{
   @Test(timeout = 30000)
	@DeployableTestMethod(duration = 0.0)
   public void testEquals()
   {
      ActionRequestRegister arr = new ActionRequestRegister(rACT.ACTIVATE_GRIPPER, rMOD.BASIC_MODE, rGTO.GO_TO, rATR.NORMAL);
      ActionRequestRegister arrEqual= new ActionRequestRegister(rACT.ACTIVATE_GRIPPER, rMOD.BASIC_MODE, rGTO.GO_TO, rATR.NORMAL);
      ActionRequestRegister arrUnequal= new ActionRequestRegister(rACT.ACTIVATE_GRIPPER, rMOD.PINCH_MODE, rGTO.GO_TO, rATR.NORMAL);
      
      assertTrue(arr.equals(arrEqual));
      assertFalse(arr.equals(arrUnequal));
      
      FingerSpeedRegister fsr = new FingerSpeedRegister(Finger.FINGER_A);
      fsr.setSpeed(arr.getRegisterValue());
      assertFalse(arr.equals(fsr));
   }

}
