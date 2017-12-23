package us.ihmc.pathPlanning.visibilityGraphs;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.apache.commons.math3.util.Precision;
import org.junit.Test;

import us.ihmc.euclid.tools.EuclidCoreRandomTools;

public class ConnectionPoint3DTest
{
   private static final int ITERATIONS = 10000;
   private static final double EPSILON = 1.0e-12;

   @Test(timeout = 1000)
   public void testRound() throws Exception
   {
      Random random = new Random(43566787);

      for (int i = 0; i < ITERATIONS; i++)
      {
         double raw = EuclidCoreRandomTools.nextDouble(random, 1000.0);
         double expected = Precision.round(raw, 3);
         double actual = ConnectionPoint3D.round(raw);
         assertEquals(expected, actual, EPSILON);
      }
   }
}
