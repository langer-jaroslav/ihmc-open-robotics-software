package us.ihmc.atlas.behaviorTests;

import org.junit.Test;

import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.darpaRoboticsChallenge.behaviorTests.DRCGraspPieceOfDebrisBehaviorTest;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.tools.testing.BambooPlanType;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestClass;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;


@DeployableTestClass(planType = {BambooPlanType.InDevelopment})
public class AtlasGraspPieceOfDebrisBehaviorTest extends DRCGraspPieceOfDebrisBehaviorTest 
{
	private final AtlasRobotModel robotModel;
	
	public AtlasGraspPieceOfDebrisBehaviorTest() 
	{
		robotModel = new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_DUAL_ROBOTIQ, DRCRobotModel.RobotTarget.SCS, false);
      boolean useHighResolutionContactPointGrid = false;
		robotModel.createHandContactPoints(useHighResolutionContactPointGrid);
	}

	@Override
	public DRCRobotModel getRobotModel() {
	      return robotModel;
	}

	@Override
	public String getSimpleRobotName()
	{
		return BambooTools.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.ATLAS);
	}
	
	@Override
	@DeployableTestMethod(duration = 542.6)
   @Test(timeout = 2700000)
	public void testGraspingHorizontalDebrisWithRightHand() throws SimulationExceededMaximumTimeException
	{
	   super.testGraspingHorizontalDebrisWithRightHand();
	}
	
	@Override
	@DeployableTestMethod(duration = 70.0)
   @Test(timeout = 300000)
	public void testGraspingLeaningAgainstAWallDebrisWithRightHand() throws SimulationExceededMaximumTimeException
	{
	   BambooPlanType.assumeRunningLocally();
	   super.testGraspingLeaningAgainstAWallDebrisWithRightHand();
	}
	
	@Override
	@DeployableTestMethod(duration = 86.4)
   @Test(timeout = 430000)
	public void testGraspingStandingDebrisWithLeftHand() throws SimulationExceededMaximumTimeException
	{
	   super.testGraspingStandingDebrisWithLeftHand();
	}
	
	@Override
	@DeployableTestMethod(duration = 114.1)
   @Test(timeout = 570000)
	public void testGraspingStandingDebrisWithRightHand() throws SimulationExceededMaximumTimeException
	{
	   super.testGraspingStandingDebrisWithRightHand();
	}
}
