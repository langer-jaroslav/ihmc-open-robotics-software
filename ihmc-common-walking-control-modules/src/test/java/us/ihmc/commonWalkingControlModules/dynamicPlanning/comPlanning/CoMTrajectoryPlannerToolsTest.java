package us.ihmc.commonWalkingControlModules.dynamicPlanning.comPlanning;

import org.junit.Test;
import us.ihmc.commons.RandomNumbers;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;

import java.util.Random;

import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CoMTrajectoryPlannerToolsTest
{
   private static final double epsilon = 1e-8;
   private static final int iters = 1000;

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetFirstCoefficientPositionMultiplier()
   {
      Random random = new Random(1738L);
      for (int iter = 0; iter < iters; iter++)
      {
         double timeInPhase = RandomNumbers.nextDouble(random, 0.0, 10000);
         double omega = RandomNumbers.nextDouble(random, 1.0, 10.0);
         double multiplier = PiecewiseCoMTrajectoryPlannerTools.getFirstCoefficientPositionMultiplier(ContactState.IN_CONTACT, timeInPhase, omega);

         double expectedMultiplier = Math.exp(omega * timeInPhase);
         if (!Double.isFinite(expectedMultiplier))
            expectedMultiplier = Double.MAX_VALUE;

         assertTrue(Double.isFinite(multiplier));
         assertEquals(expectedMultiplier, multiplier, epsilon);

         multiplier = PiecewiseCoMTrajectoryPlannerTools.getFirstCoefficientPositionMultiplier(ContactState.FLIGHT, timeInPhase, omega);
         expectedMultiplier = timeInPhase;

         assertTrue(Double.isFinite(multiplier));
         assertEquals(expectedMultiplier, multiplier);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetSecondCoefficientPositionMultiplier()
   {
      Random random = new Random(1738L);
      for (int iter = 0; iter < iters; iter++)
      {
         double timeInPhase = RandomNumbers.nextDouble(random, 0.0, 10000);
         double omega = RandomNumbers.nextDouble(random, 1.0, 10.0);
         double multiplier = PiecewiseCoMTrajectoryPlannerTools.getSecondCoefficientPositionMultiplier(ContactState.IN_CONTACT, timeInPhase, omega);

         double expectedMultiplier = Math.exp(-omega * timeInPhase);
         if (!Double.isFinite(expectedMultiplier))
            expectedMultiplier = Double.MAX_VALUE;

         assertTrue(Double.isFinite(multiplier));
         assertEquals(expectedMultiplier, multiplier, epsilon);

         multiplier = PiecewiseCoMTrajectoryPlannerTools.getSecondCoefficientPositionMultiplier(ContactState.FLIGHT, timeInPhase, omega);
         expectedMultiplier = 1.0;

         assertTrue(Double.isFinite(multiplier));
         assertEquals(expectedMultiplier, multiplier);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetFirstCoefficientVelocityMultiplier()
   {
      Random random = new Random(1738L);
      for (int iter = 0; iter < iters; iter++)
      {
         double timeInPhase = RandomNumbers.nextDouble(random, 0.0, 10000);
         double omega = RandomNumbers.nextDouble(random, 1.0, 10.0);
         double multiplier = PiecewiseCoMTrajectoryPlannerTools.getFirstCoefficientVelocityMultiplier(ContactState.IN_CONTACT, timeInPhase, omega);

         double expectedMultiplier = omega * Math.exp(omega * timeInPhase);
         if (!Double.isFinite(expectedMultiplier))
            expectedMultiplier = Double.MAX_VALUE;

         assertTrue("time = " + timeInPhase, Double.isFinite(multiplier));
         assertEquals(expectedMultiplier, multiplier, epsilon);
         assertEquals(Math.min(omega * PiecewiseCoMTrajectoryPlannerTools.getFirstCoefficientPositionMultiplier(ContactState.IN_CONTACT, timeInPhase, omega), Double.MAX_VALUE), multiplier, epsilon);

         multiplier = PiecewiseCoMTrajectoryPlannerTools.getFirstCoefficientVelocityMultiplier(ContactState.FLIGHT, timeInPhase, omega);
         expectedMultiplier = 1.0;

         assertTrue(Double.isFinite(multiplier));
         assertEquals(expectedMultiplier, multiplier);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetSecondCoefficientVelocityMultiplier()
   {
      Random random = new Random(1738L);
      for (int iter = 0; iter < iters; iter++)
      {
         double timeInPhase = RandomNumbers.nextDouble(random, 0.0, 10000);
         double omega = RandomNumbers.nextDouble(random, 1.0, 10.0);
         double multiplier = PiecewiseCoMTrajectoryPlannerTools.getSecondCoefficientVelocityMultiplier(ContactState.IN_CONTACT, timeInPhase, omega);

         double expectedMultiplier = -omega * Math.exp(-omega * timeInPhase);
         if (!Double.isFinite(expectedMultiplier))
            expectedMultiplier = Double.MIN_VALUE;

         assertTrue(Double.isFinite(multiplier));
         assertEquals(expectedMultiplier, multiplier, epsilon);
         assertEquals(Math.min(-omega * PiecewiseCoMTrajectoryPlannerTools.getSecondCoefficientPositionMultiplier(ContactState.IN_CONTACT, timeInPhase, omega), Double.MAX_VALUE), multiplier, epsilon);


         multiplier = PiecewiseCoMTrajectoryPlannerTools.getSecondCoefficientVelocityMultiplier(ContactState.FLIGHT, timeInPhase, omega);
         expectedMultiplier = 0.0;

         assertTrue(Double.isFinite(multiplier));
         assertEquals(expectedMultiplier, multiplier);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetFirstCoefficientAccelerationMultiplier()
   {
      Random random = new Random(1738L);
      for (int iter = 0; iter < iters; iter++)
      {
         double timeInPhase = RandomNumbers.nextDouble(random, 0.0, 10000);
         double omega = RandomNumbers.nextDouble(random, 1.0, 10.0);
         double multiplier = PiecewiseCoMTrajectoryPlannerTools.getFirstCoefficientAccelerationMultiplier(ContactState.IN_CONTACT, timeInPhase, omega);

         double expectedMultiplier = omega * omega * Math.exp(omega * timeInPhase);
         if (!Double.isFinite(expectedMultiplier))
            expectedMultiplier = Double.MAX_VALUE;

         assertTrue("time = " + timeInPhase, Double.isFinite(multiplier));
         assertEquals(expectedMultiplier, multiplier, epsilon);
         assertEquals(Math.min(omega * omega * PiecewiseCoMTrajectoryPlannerTools
               .getFirstCoefficientPositionMultiplier(ContactState.IN_CONTACT, timeInPhase, omega), Double.MAX_VALUE), multiplier, epsilon);

         multiplier = PiecewiseCoMTrajectoryPlannerTools.getFirstCoefficientAccelerationMultiplier(ContactState.FLIGHT, timeInPhase, omega);
         expectedMultiplier = 1.0;

         assertTrue(Double.isFinite(multiplier));
         assertEquals(expectedMultiplier, multiplier);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetSecondCoefficientAccelerationMultiplier()
   {
      Random random = new Random(1738L);
      for (int iter = 0; iter < iters; iter++)
      {
         double timeInPhase = RandomNumbers.nextDouble(random, 0.0, 10000);
         double omega = RandomNumbers.nextDouble(random, 1.0, 10.0);
         double multiplier = PiecewiseCoMTrajectoryPlannerTools.getSecondCoefficientAccelerationMultiplier(ContactState.IN_CONTACT, timeInPhase, omega);

         double expectedMultiplier = omega * omega * Math.exp(-omega * timeInPhase);
         if (!Double.isFinite(expectedMultiplier))
            expectedMultiplier = Double.MIN_VALUE;

         assertTrue(Double.isFinite(multiplier));
         assertEquals(expectedMultiplier, multiplier, epsilon);
         assertEquals(Math.min(omega * omega * PiecewiseCoMTrajectoryPlannerTools
               .getSecondCoefficientPositionMultiplier(ContactState.IN_CONTACT, timeInPhase, omega), Double.MAX_VALUE), multiplier, epsilon);


         multiplier = PiecewiseCoMTrajectoryPlannerTools.getSecondCoefficientAccelerationMultiplier(ContactState.FLIGHT, timeInPhase, omega);
         expectedMultiplier = 0.0;

         assertTrue(Double.isFinite(multiplier));
         assertEquals(expectedMultiplier, multiplier);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetGravityPositionEffect()
   {
      Random random = new Random(1738L);
      for (int iter = 0; iter < iters; iter++)
      {
         double timeInPhase = RandomNumbers.nextDouble(random, 0.0, 10000);
         double gravity = RandomNumbers.nextDouble(random, 8.0, 10.0);
         double multiplier = PiecewiseCoMTrajectoryPlannerTools.getGravityPositionEffect(ContactState.IN_CONTACT, timeInPhase, gravity);
         assertEquals(0.0, multiplier, epsilon);

         multiplier = PiecewiseCoMTrajectoryPlannerTools.getGravityPositionEffect(ContactState.FLIGHT, timeInPhase, gravity);
         double expectedMultiplier =  -0.5 * gravity * timeInPhase * timeInPhase;

         if (!Double.isFinite(expectedMultiplier))
            expectedMultiplier = Double.MIN_VALUE;

         assertTrue(Double.isFinite(multiplier));
         assertEquals(expectedMultiplier, multiplier, 100 * epsilon);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetGravityVelocityEffect()
   {
      Random random = new Random(1738L);
      for (int iter = 0; iter < iters; iter++)
      {
         double timeInPhase = RandomNumbers.nextDouble(random, 0.0, 10000);
         double gravity = RandomNumbers.nextDouble(random, 8.0, 10.0);
         double multiplier = PiecewiseCoMTrajectoryPlannerTools.getGravityVelocityEffect(ContactState.IN_CONTACT, timeInPhase, gravity);
         assertEquals(0.0, multiplier, epsilon);

         multiplier = PiecewiseCoMTrajectoryPlannerTools.getGravityVelocityEffect(ContactState.FLIGHT, timeInPhase, gravity);
         double expectedMultiplier =  -gravity * timeInPhase;

         if (!Double.isFinite(expectedMultiplier))
            expectedMultiplier = Double.MIN_VALUE;

         assertTrue(Double.isFinite(multiplier));
         assertEquals(expectedMultiplier, multiplier, epsilon);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetGravityAccelerationEffect()
   {
      Random random = new Random(1738L);
      for (int iter = 0; iter < iters; iter++)
      {
         double gravity = RandomNumbers.nextDouble(random, 8.0, 10.0);
         double multiplier = PiecewiseCoMTrajectoryPlannerTools.getGravityAccelerationEffect(ContactState.IN_CONTACT, gravity);
         assertEquals(0.0, multiplier, epsilon);

         multiplier = PiecewiseCoMTrajectoryPlannerTools.getGravityAccelerationEffect(ContactState.FLIGHT, gravity);
         double expectedMultiplier =  -gravity ;

         if (!Double.isFinite(expectedMultiplier))
            expectedMultiplier = Double.MIN_VALUE;

         assertTrue(Double.isFinite(multiplier));
         assertEquals(expectedMultiplier, multiplier, epsilon);
      }
   }


   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetFirstCoefficientIndex()
   {
      Random random = new Random(1738L);
      for (int iter = 0; iter < iters; iter++)
      {
         int sequence = RandomNumbers.nextInt(random, 0, 10000);
         int index = PiecewiseCoMTrajectoryPlannerTools.getFirstCoefficientIndex(sequence);
         int expectedIndex = 2 * sequence;

         assertEquals(expectedIndex, index);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetSecondCoefficientIndex()
   {
      Random random = new Random(1738L);
      for (int iter = 0; iter < iters; iter++)
      {
         int sequence = RandomNumbers.nextInt(random, 0, 10000);
         int index = PiecewiseCoMTrajectoryPlannerTools.getSecondCoefficientIndex(sequence);
         int expectedIndex = 2 * sequence + 1;

         assertEquals(expectedIndex, index);
      }
   }
}
