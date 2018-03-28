package us.ihmc.valkyrie.controllerAPI;

import org.junit.Test;

import us.ihmc.avatar.controllerAPI.EndToEndPelvisHeightTrajectoryMessageTest;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.simulationConstructionSetTools.bambooTools.BambooTools;
import us.ihmc.valkyrie.ValkyrieRobotModel;

public class ValkyrieEndToEndPelvisHeightTrajectoryMessageTest extends EndToEndPelvisHeightTrajectoryMessageTest
{
   private final ValkyrieRobotModel robotModel = new ValkyrieRobotModel(RobotTarget.SCS, false);

   @Override
   public DRCRobotModel getRobotModel()
   {
      return robotModel;
   }

   @Override
   public String getSimpleRobotName()
   {
      return BambooTools.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.VALKYRIE);
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 43.6)
   @Test(timeout = 220000)
   public void testSingleWaypoint() throws Exception
   {
      super.testSingleWaypoint();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 28.7)
   @Test(timeout = 140000)
   public void testSingleWaypointInUserMode() throws Exception
   {
      super.testSingleWaypointInUserMode();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 105.6)
   @Test(timeout = 530000)
   public void testSingleWaypointThenManualChange() throws Exception
   {
      super.testSingleWaypointThenManualChange();
   }
}
