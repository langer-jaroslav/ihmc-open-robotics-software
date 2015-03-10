package us.ihmc.darpaRoboticsChallenge.behaviorTests;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Random;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.SdfLoader.SDFJointNameMap;
import us.ihmc.communication.kryo.IHMCCommunicationKryoNetClassList;
import us.ihmc.communication.packetCommunicator.KryoLocalPacketCommunicator;
import us.ihmc.communication.packetCommunicator.KryoPacketCommunicator;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.dataobjects.FingerState;
import us.ihmc.communication.packets.manipulation.FingerStatePacket;
import us.ihmc.communication.util.NetworkConfigParameters;
import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseStartingLocation;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.environment.DRCDemo01NavigationEnvironment;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCBehaviorTestHelper;
import us.ihmc.humanoidBehaviors.behaviors.primitives.FingerStateBehavior;
import us.ihmc.simulationconstructionset.Joint;
import us.ihmc.simulationconstructionset.OneDegreeOfFreedomJoint;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;

import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.ThreadTools;

import us.ihmc.utilities.code.agileTesting.BambooAnnotations.EstimatedDuration;
import us.ihmc.utilities.io.printing.SysoutTool;
import us.ihmc.utilities.math.geometry.BoundingBox3d;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.yoUtilities.time.GlobalTimer;

public abstract class DRCFingerStateBehaviorTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();

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

      GlobalTimer.clearTimers();
      
      

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   @AfterClass
   public static void printMemoryUsageAfterClass()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(DRCFingerStateBehaviorTest.class + " after class.");
   }

   private final boolean DEBUG = false;
   private DRCBehaviorTestHelper drcBehaviorTestHelper;

   @Before
   public void setUp()
   {
      if (NetworkConfigParameters.USE_BEHAVIORS_MODULE)
      {
         throw new RuntimeException("Must set NetworkConfigParameters.USE_BEHAVIORS_MODULE = false in order to perform this test!");
      }

      DRCDemo01NavigationEnvironment testEnvironment = new DRCDemo01NavigationEnvironment();

      KryoPacketCommunicator controllerCommunicator = new KryoLocalPacketCommunicator(new IHMCCommunicationKryoNetClassList(),
            PacketDestination.CONTROLLER.ordinal(), "DRCControllerCommunicator");
      KryoPacketCommunicator networkObjectCommunicator = new KryoLocalPacketCommunicator(new IHMCCommunicationKryoNetClassList(),
            PacketDestination.NETWORK_PROCESSOR.ordinal(), "MockNetworkProcessorCommunicator");

      drcBehaviorTestHelper = new DRCBehaviorTestHelper(testEnvironment, networkObjectCommunicator, getSimpleRobotName(), null,
            DRCObstacleCourseStartingLocation.DEFAULT, simulationTestingParameters, getRobotModel(), controllerCommunicator);
   }

   @EstimatedDuration(duration = 27.7)
   @Test(timeout = 83115)
   public void testCloseHand() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);
      RobotSide robotSide = RobotSide.LEFT;
      double trajectoryTime = 2.0;

      double fingerJointQInitial = getTotalFingerJointQ(robotSide);
      FingerStateBehavior fingerStateBehavior = testFingerStateBehavior(new FingerStatePacket(robotSide, FingerState.CLOSE), trajectoryTime);
      success = drcBehaviorTestHelper.executeBehaviorUntilDone(fingerStateBehavior);
      assertTrue(success);
      double fingerJointQFinal = getTotalFingerJointQ(robotSide);

      SysoutTool.println("fingerJointQInitial: " + fingerJointQInitial, DEBUG);
      SysoutTool.println("fingerJointQFinal : " + fingerJointQFinal, DEBUG);


      assertTrue(fingerJointQFinal > fingerJointQInitial);
      assertTrue(fingerStateBehavior.isDone());

      BambooTools.reportTestFinishedMessage();
   }

   @EstimatedDuration(duration = 27.7)
   @Test(timeout = 83115)
   public void testStopCloseHand() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      SysoutTool.println("Initializing Simulation", DEBUG);
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      RobotSide robotSide = RobotSide.LEFT;
      double trajectoryTime = 0.3; // [0.3] Hand closes quickly!
      double stopTime = trajectoryTime / 2.0;

      SysoutTool.println("Initializing Behavior", DEBUG);
      FingerStateBehavior fingerStateBehavior = testFingerStateBehavior(new FingerStatePacket(robotSide, FingerState.CLOSE), trajectoryTime);

      SysoutTool.println("Starting Behavior", DEBUG);
      double fingerJointQInitial = getTotalFingerJointQ(robotSide);
      success = drcBehaviorTestHelper.executeBehaviorSimulateAndBlockAndCatchExceptions(fingerStateBehavior, stopTime);
      assertTrue(success);
      SysoutTool.println("Stopping Behavior", DEBUG);
      double fingerJointQAtStop = getTotalFingerJointQ(robotSide);
      fingerStateBehavior.stop();
      assertTrue(!fingerStateBehavior.isDone());

      success = drcBehaviorTestHelper.executeBehaviorSimulateAndBlockAndCatchExceptions(fingerStateBehavior, 1.0);
      assertTrue(success);
      double fingerJointQFinal = getTotalFingerJointQ(robotSide);

      SysoutTool.println("fingerJointQInitial: " + fingerJointQInitial, DEBUG);
      SysoutTool.println("fingerJointQAtStop : " + fingerJointQAtStop, DEBUG);
      SysoutTool.println("fingerJointQFinal : " + fingerJointQFinal, DEBUG);

      assertTrue(Math.abs(fingerJointQFinal - fingerJointQAtStop) < 3.0);
      assertTrue(!fingerStateBehavior.isDone());

      BambooTools.reportTestFinishedMessage();
   }
   
   @EstimatedDuration(duration = 27.7)
   @Test(timeout = 83115)
   public void testPauseAndResumeCloseHand() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      SysoutTool.println("Initializing Simulation", DEBUG);
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      RobotSide robotSide = RobotSide.LEFT;
      double trajectoryTime = 0.3; // [0.3] Hand closes quickly!
      double stopTime = trajectoryTime / 2.0;

      SysoutTool.println("Initializing Behavior", DEBUG);
      FingerStateBehavior fingerStateBehavior = testFingerStateBehavior(new FingerStatePacket(robotSide, FingerState.CLOSE), trajectoryTime);

      SysoutTool.println("Starting Behavior", DEBUG);
      double fingerJointQInitial = getTotalFingerJointQ(robotSide);
      success = drcBehaviorTestHelper.executeBehaviorSimulateAndBlockAndCatchExceptions(fingerStateBehavior, stopTime);
      assertTrue(success);
      SysoutTool.println("Pausing Behavior", DEBUG);
      double fingerJointQAtPause = getTotalFingerJointQ(robotSide);
      fingerStateBehavior.pause();

      success = drcBehaviorTestHelper.executeBehaviorSimulateAndBlockAndCatchExceptions(fingerStateBehavior, 1.0);
      assertTrue(success);
      SysoutTool.println("Resuming Behavior", DEBUG);
      double fingerJointQAtResume = getTotalFingerJointQ(robotSide);
      fingerStateBehavior.resume();
      assertTrue(!fingerStateBehavior.isDone());

      success = drcBehaviorTestHelper.executeBehaviorSimulateAndBlockAndCatchExceptions(fingerStateBehavior, 1.0);
      assertTrue(success);
      SysoutTool.println("Behavior Should Be Done", DEBUG);
      double fingerJointQFinal = getTotalFingerJointQ(robotSide);
      fingerStateBehavior.resume();

      SysoutTool.println("fingerJointQInitial: " + fingerJointQInitial, DEBUG);
      SysoutTool.println("fingerJointQAtPause : " + fingerJointQAtPause, DEBUG);
      SysoutTool.println("fingerJointQAtResume : " + fingerJointQAtResume, DEBUG);
      SysoutTool.println("fingerJointQFinal : " + fingerJointQFinal, DEBUG);

      assertTrue(Math.abs(fingerJointQAtResume - fingerJointQAtPause) < 3.0);
      assertTrue(fingerJointQFinal > fingerJointQAtResume);
//      assertTrue(fingerStateBehavior.isDone());

      BambooTools.reportTestFinishedMessage();
   }

   private FingerStatePacket getRandomClosedTypeFingerStatePacket(RobotSide robotSide)
   {
      ArrayList<FingerState> closedFingerConfigs = new ArrayList<FingerState>();
      closedFingerConfigs.add(FingerState.CLOSE);
      closedFingerConfigs.add(FingerState.CLOSE_FINGERS);
      closedFingerConfigs.add(FingerState.CLOSE_THUMB);
      closedFingerConfigs.add(FingerState.CRUSH);
      closedFingerConfigs.add(FingerState.CRUSH_INDEX);
      closedFingerConfigs.add(FingerState.CRUSH_MIDDLE);
      closedFingerConfigs.add(FingerState.CRUSH_THUMB);

      FingerState fingerState = closedFingerConfigs.get(RandomTools.generateRandomInt(new Random(), 0, closedFingerConfigs.size() - 1));
      if (DEBUG)
      {
         SysoutTool.println(fingerState.name());
      }

      return new FingerStatePacket(robotSide, fingerState);
   }

   private double getTotalFingerJointQ(RobotSide robotSide)
   {
      double ret = 0.0;

      ArrayList<OneDegreeOfFreedomJoint> fingerJoints = new ArrayList<OneDegreeOfFreedomJoint>();
      SDFJointNameMap jointNameMap = (SDFJointNameMap) drcBehaviorTestHelper.getSDFFullRobotModel().getRobotSpecificJointNames();
      Joint wristJoint = drcBehaviorTestHelper.getRobot().getJoint(jointNameMap.getJointBeforeHandName(robotSide));
      wristJoint.recursiveGetOneDegreeOfFreedomJoints(fingerJoints);
      fingerJoints.remove(0);

      for (OneDegreeOfFreedomJoint fingerJoint : fingerJoints)
      {
         double q = fingerJoint.getQ().getDoubleValue();
         ret += q;
         if (DEBUG)
         {
            SysoutTool.println(fingerJoint.getName() + " q : " + q);
         }
      }

      return ret;
   }

   private BoundingBox3d getDistalFingerJointBoundingBox(RobotSide robotSide)
   {
      ArrayList<OneDegreeOfFreedomJoint> fingerJoints = new ArrayList<OneDegreeOfFreedomJoint>();
      SDFJointNameMap jointNameMap = (SDFJointNameMap) drcBehaviorTestHelper.getSDFFullRobotModel().getRobotSpecificJointNames();
      Joint wristJoint = drcBehaviorTestHelper.getRobot().getJoint(jointNameMap.getJointBeforeHandName(robotSide));
      wristJoint.recursiveGetOneDegreeOfFreedomJoints(fingerJoints);
      fingerJoints.remove(0);

      ArrayList<OneDegreeOfFreedomJoint> mostDistalJoints = new ArrayList<OneDegreeOfFreedomJoint>();

      for (OneDegreeOfFreedomJoint fingerJoint : fingerJoints)
      {
         if (fingerJoint.childrenJoints.size() == 0)
            mostDistalJoints.add(fingerJoint);
      }
      
      SysoutTool.println("mostDistalJoints: " + mostDistalJoints, DEBUG);
      
      BoundingBox3d boundingBoxOld = null;

      while(mostDistalJoints.size() >= 2)
      {
         Vector3d translationToWorldA = new Vector3d();
         mostDistalJoints.remove(0).getTranslationToWorld(translationToWorldA);
         Point3d positionInWorldA = new Point3d(translationToWorldA);
         
         Vector3d translationToWorldB = new Vector3d();
         mostDistalJoints.remove(0).getTranslationToWorld(translationToWorldB);
         Point3d positionInWorldB = new Point3d(translationToWorldB);

         if(boundingBoxOld != null)
         {
            double xMin = Math.min(positionInWorldA.getX(), positionInWorldB.getX());
            double yMin = Math.min(positionInWorldA.getY(), positionInWorldB.getY());
            double zMin = Math.min(positionInWorldA.getZ(), positionInWorldB.getZ());

            double xMax = Math.max(positionInWorldA.getX(), positionInWorldB.getX());
            double yMax = Math.max(positionInWorldA.getY(), positionInWorldB.getY());
            double zMax = Math.max(positionInWorldA.getZ(), positionInWorldB.getZ());

            BoundingBox3d boundingBoxNew = new BoundingBox3d(xMin, yMin, zMin, xMax, yMax, zMax);
            
            boundingBoxOld = BoundingBox3d.union(boundingBoxOld, boundingBoxNew);
         }
         else
         {
            double xMin = Math.min(positionInWorldA.getX(), positionInWorldB.getX());
            double yMin = Math.min(positionInWorldA.getY(), positionInWorldB.getY());
            double zMin = Math.min(positionInWorldA.getZ(), positionInWorldB.getZ());

            double xMax = Math.max(positionInWorldA.getX(), positionInWorldB.getX());
            double yMax = Math.max(positionInWorldA.getY(), positionInWorldB.getY());
            double zMax = Math.max(positionInWorldA.getZ(), positionInWorldB.getZ());

            boundingBoxOld = new BoundingBox3d(xMin, yMin, zMin, xMax, yMax, zMax);
         }
      }
      
      return boundingBoxOld;
   }
   
   private double getDistalFingerJointBoundingBoxSize(RobotSide robotSide)
   {
      BoundingBox3d boundingBox = getDistalFingerJointBoundingBox(robotSide);
      
      Point3d minPoint = new Point3d();
      Point3d maxPoint = new Point3d();
      
      boundingBox.getMinPoint(minPoint);
      boundingBox.getMaxPoint(maxPoint);
      
      double size = minPoint.distance(maxPoint);
      
      return size;
   }

   private FingerStateBehavior testFingerStateBehavior(FingerStatePacket fingerStatePacket, double trajectoryTime)
         throws SimulationExceededMaximumTimeException
   {
      final FingerStateBehavior fingerStateBehavior = new FingerStateBehavior(drcBehaviorTestHelper.getBehaviorCommunicationBridge(),
            drcBehaviorTestHelper.getYoTime());

      fingerStateBehavior.initialize();
      fingerStateBehavior.setInput(fingerStatePacket);
      assertTrue(fingerStateBehavior.hasInputBeenSet());

      return fingerStateBehavior;
   }
}
