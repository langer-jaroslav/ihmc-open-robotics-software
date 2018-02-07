package us.ihmc.avatar.behaviorTests;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

import us.ihmc.avatar.DRCObstacleCourseStartingLocation;
import us.ihmc.avatar.MultiRobotTestInterface;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.testTools.DRCBehaviorTestHelper;
import us.ihmc.continuousIntegration.ContinuousIntegrationTools;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidBehaviors.behaviors.behaviorServices.FiducialDetectorBehaviorService;
import us.ihmc.humanoidBehaviors.behaviors.primitives.AtlasPrimitiveActions;
import us.ihmc.humanoidBehaviors.behaviors.roughTerrain.WalkOverTerrainGoalPacket;
import us.ihmc.humanoidBehaviors.behaviors.roughTerrain.WalkOverTerrainStateMachineBehavior;
import us.ihmc.humanoidBehaviors.communication.CommunicationBridge;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.multicastLogDataProtocol.modelLoaders.LogModelProvider;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.simulationConstructionSetTools.bambooTools.BambooTools;
import us.ihmc.simulationConstructionSetTools.util.environments.CinderBlockFieldEnvironment;
import us.ihmc.simulationConstructionSetTools.util.environments.CinderBlockFieldWithFiducialEnvironment;
import us.ihmc.simulationConstructionSetTools.util.environments.CinderBlockFieldWithFiducialEnvironment.FiducialType;
import us.ihmc.simulationConstructionSetTools.util.environments.CommonAvatarEnvironmentInterface;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner;
import us.ihmc.simulationconstructionset.util.simulationTesting.SimulationTestingParameters;
import us.ihmc.tools.MemoryTools;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

public abstract class AvatarWalkOverTerrainBehaviorTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromSystemProperties();
   private static final boolean keepSCSUp = false;
   private static final int timeout = 120000; // to easily keep scs up. unfortunately can't be set programmatically, has to be a constant

   private DRCBehaviorTestHelper drcBehaviorTestHelper;
   private DRCRobotModel drcRobotModel;

   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }

   @After
   public void destroySimulationAndRecycleMemory()
   {
      if (simulationTestingParameters.getKeepSCSUp())
      {
         ThreadTools.sleepForever();
      }

      // Do this here in case a test fails. That way the memory will be recycled.
      if (drcBehaviorTestHelper != null)
      {
         drcBehaviorTestHelper.closeAndDispose();
         drcBehaviorTestHelper = null;
      }

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   @AfterClass
   public static void printMemoryUsageAfterClass()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(DRCWalkToLocationBehaviorTest.class + " after class.");
   }

   @Before
   public void setUp()
   {
      drcRobotModel = getRobotModel();
      drcBehaviorTestHelper = new DRCBehaviorTestHelper(new CinderBlockFieldEnvironment(), getSimpleRobotName(), DRCObstacleCourseStartingLocation.DEFAULT,
                                                        simulationTestingParameters, drcRobotModel);
   }

   public void testWalkOverCinderBlocks() throws BlockingSimulationRunner.SimulationExceededMaximumTimeException
   {
      if (!ContinuousIntegrationTools.isRunningOnContinuousIntegrationServer())
      {
         simulationTestingParameters.setKeepSCSUp(true);
      }

      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      YoGraphicsListRegistry yoGraphicsListRegistry = new YoGraphicsListRegistry();

      CommunicationBridge communicationBridge = drcBehaviorTestHelper.getBehaviorCommunicationBridge();
      HumanoidReferenceFrames referenceFrames = drcBehaviorTestHelper.getReferenceFrames();
      YoDouble yoTime = drcBehaviorTestHelper.getYoTime();

      WalkOverTerrainStateMachineBehavior walkOverTerrainBehavior = new WalkOverTerrainStateMachineBehavior(communicationBridge, yoTime, referenceFrames);
      walkOverTerrainBehavior.initialize();
      drcBehaviorTestHelper.getSimulationConstructionSet().addYoGraphicsListRegistry(yoGraphicsListRegistry);
      drcBehaviorTestHelper.addChildRegistry(walkOverTerrainBehavior.getYoVariableRegistry());

      drcBehaviorTestHelper.getAvatarSimulation().start();

      while(drcBehaviorTestHelper.getYoTime().getDoubleValue() < 1.0)
         ThreadTools.sleep(100);

      FramePose3D goalPose = new FramePose3D(ReferenceFrame.getWorldFrame(), new Pose3D(5.0, 0.0, 0.0, 0.0, 0.0, 0.0));
      drcBehaviorTestHelper.getBehaviorCommunicationBridge().sendPacketToBehavior(new WalkOverTerrainGoalPacket(
            goalPose));

      assertTrue(drcBehaviorTestHelper.executeBehaviorUntilDone(walkOverTerrainBehavior));
      assertTrue(walkOverTerrainBehavior.isDone());

      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
   }
}
