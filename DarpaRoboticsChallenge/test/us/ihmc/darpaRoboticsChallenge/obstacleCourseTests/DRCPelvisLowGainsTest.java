package us.ihmc.darpaRoboticsChallenge.obstacleCourseTests;

import static org.junit.Assert.assertTrue;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseStartingLocation;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.drcRobot.FlatGroundEnvironment;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCSimulationTestHelper;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.SimulationDoneCriterion;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.EstimatedDuration;
import us.ihmc.utilities.math.geometry.BoundingBox3d;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

/**
 * This end to end test is to make sure the pelvis doesn't flip out when it has low gains. In March, 2015 we started investigating this bug.
 * It seems to have something to do with either some sort of problem in the MomentumBasedController or InverseDynamicsCalculator or State Estimator.
 *
 */
public abstract class DRCPelvisLowGainsTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();

   private DRCSimulationTestHelper drcSimulationTestHelper;

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
      if (drcSimulationTestHelper != null)
      {
         drcSimulationTestHelper.destroySimulation();
         drcSimulationTestHelper = null;
      }

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

// @Ignore
// @QuarantinedTest("150313: This test currently fails, seemingly due to some sort of problem in the MomentumBasedController or InverseDynamicsCalculator. Trying to fix it...")
   @EstimatedDuration(duration = 20.0)
   @Test(timeout = 300000)
   public void testStandingWithLowPelvisOrientationGains() throws SimulationExceededMaximumTimeException
   {
      // March 2015: Low pelvis orientation gains cause the pelvis to flip out. Trying to track down why this happens.

      BambooTools.reportTestStartedMessage();

      FlatGroundEnvironment flatGround = new FlatGroundEnvironment();
      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT;

      DRCRobotModel robotModel = getRobotModel();
      robotModel.setEnableJointDamping(false);
      drcSimulationTestHelper = new DRCSimulationTestHelper(flatGround, "DRCPelvisFlippingOutBugTest", "", selectedLocation, simulationTestingParameters,
              robotModel);
      
      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();

      setupCameraForElvisPelvis();

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(3.0);

      final DoubleYoVariable pelvisOrientationError = getPelvisOrientationErrorVariableName(simulationConstructionSet);

      SimulationDoneCriterion checkPelvisOrientationError = new SimulationDoneCriterion()
      {
         @Override
         public boolean isSimulationDone()
         {
            return (Math.abs(pelvisOrientationError.getDoubleValue()) > 0.22);
         }
      };

      simulationConstructionSet.setSimulateDoneCriterion(checkPelvisOrientationError);

      DoubleYoVariable kpPelvisOrientation = (DoubleYoVariable) simulationConstructionSet.getVariable("kpPelvisOrientation");
      DoubleYoVariable zetaPelvisOrientation = (DoubleYoVariable) simulationConstructionSet.getVariable("zetaPelvisOrientation");

      kpPelvisOrientation.set(20.0);
      zetaPelvisOrientation.set(0.7);

      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(12.0);

      drcSimulationTestHelper.createMovie(getSimpleRobotName(), 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      Point3d center = new Point3d(-0.09807959403314585, 0.002501752329158081, 0.7867972043876718);
      Vector3d plusMinusVector = new Vector3d(0.2, 0.2, 0.5);
      BoundingBox3d boundingBox = BoundingBox3d.createUsingCenterAndPlusMinusVector(center, plusMinusVector);
      drcSimulationTestHelper.assertRobotsRootJointIsInBoundingBox(boundingBox);

      BambooTools.reportTestFinishedMessage();
   }

   private void setupCameraForElvisPelvis()
   {
      Point3d cameraFix = new Point3d(0.0, 0.0, 0.9);
      Point3d cameraPosition = new Point3d(0.0, -1.8, 0.9);

      drcSimulationTestHelper.setupCameraForUnitTest(cameraFix, cameraPosition);
   }

   protected abstract DoubleYoVariable getPelvisOrientationErrorVariableName(SimulationConstructionSet scs);

}
