package us.ihmc.robotics.trajectories.providers;

import static us.ihmc.robotics.Assert.*;

import java.util.Random;

import org.junit.After;
import org.junit.Test;

import us.ihmc.commons.RandomNumbers;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.tools.ReferenceFrameTools;

public class ConstantPositionProviderTest
{
   private static final double EPS = 1e-12;

   @After
   public void tearDown()
   {
      ReferenceFrameTools.clearWorldFrameTree();
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void test()
   {
      Random random = new Random();
      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
      double[] xyz = RandomNumbers.nextDoubleArray(random, 3, Double.MAX_VALUE);
      FramePoint3D positionExpected = new FramePoint3D(worldFrame, xyz);
      FramePoint3D positionActual = new FramePoint3D(positionExpected);
      ConstantPositionProvider constantPositionProvider = new ConstantPositionProvider(positionActual);
      constantPositionProvider.getPosition(positionActual);
      
      assertTrue(positionActual.epsilonEquals(positionExpected, EPS));
   }

}
