package us.ihmc.atlas.behaviorTests;

import org.junit.Ignore;

import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.darpaRoboticsChallenge.behaviorTests.DRCPushButtonBehaviorTest;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.tools.testing.TestPlanTarget;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestClass;

@Ignore
@DeployableTestClass(targets = {TestPlanTarget.InDevelopment})
public class AtlasPushButtonBehaviorTest extends DRCPushButtonBehaviorTest
{
   private final AtlasRobotModel robotModel;
   
   public AtlasPushButtonBehaviorTest()
   {
      robotModel = new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_DUAL_ROBOTIQ, DRCRobotModel.RobotTarget.SCS, false);
   boolean useHighResolutionContactPointGrid = true;
      robotModel.createHandContactPoints(useHighResolutionContactPointGrid);
   }
   
   @Override
   public DRCRobotModel getRobotModel()
   {
      return robotModel;
   }
   
   @Override
   public String getSimpleRobotName()
   {
      return BambooTools.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.ATLAS);
   }
}
