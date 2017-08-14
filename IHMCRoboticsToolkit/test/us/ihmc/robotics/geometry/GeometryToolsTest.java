package us.ihmc.robotics.geometry;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.commons.Epsilons;
import us.ihmc.commons.MutationTestFacilitator;
import us.ihmc.commons.RandomNumbers;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.tools.EuclidCoreTestTools;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DBasics;
import us.ihmc.robotics.random.RandomGeometry;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public class GeometryToolsTest
{
   private static final int ITERATIONS = 1000;

   @Before
   public void setUp() throws Exception
   {
   }

   @After
   public void tearDown() throws Exception
   {
   }

   private static final double EPSILON = 1e-6;

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetDistanceBetweenPointAndPlane1()
   {
      FramePoint3D pointOnPlane = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      FrameVector3D planeNormal = new FrameVector3D(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      FramePoint3D point = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 3);
      double actual = GeometryTools.distanceFromPointToPlane(point, pointOnPlane, planeNormal);
      double expected = 3.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);

      pointOnPlane = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector3D(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      point = new FramePoint3D(ReferenceFrame.getWorldFrame(), 3, 3, -3);
      actual = GeometryTools.distanceFromPointToPlane(point, pointOnPlane, planeNormal);
      expected = 3.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);

      pointOnPlane = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector3D(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      point = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, -3);
      actual = GeometryTools.distanceFromPointToPlane(point, pointOnPlane, planeNormal);
      expected = 3.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);

      pointOnPlane = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 3);
      planeNormal = new FrameVector3D(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      point = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, -3);
      actual = GeometryTools.distanceFromPointToPlane(point, pointOnPlane, planeNormal);
      expected = 6.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);

      pointOnPlane = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector3D(pointOnPlane.getReferenceFrame(), 1, 0, 0);
      point = new FramePoint3D(ReferenceFrame.getWorldFrame(), 3, 0, 0);
      actual = GeometryTools.distanceFromPointToPlane(point, pointOnPlane, planeNormal);
      expected = 3.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);

      pointOnPlane = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector3D(pointOnPlane.getReferenceFrame(), 0, 1, 0);
      point = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 3, 0);
      actual = GeometryTools.distanceFromPointToPlane(point, pointOnPlane, planeNormal);
      expected = 3.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);

      pointOnPlane = new FramePoint3D(ReferenceFrame.getWorldFrame(), 1, 1, 1);
      planeNormal = new FrameVector3D(pointOnPlane.getReferenceFrame(), 0, 1, 0);
      point = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 3, 0);
      actual = GeometryTools.distanceFromPointToPlane(point, pointOnPlane, planeNormal);
      expected = 2.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testIsLineSegmentIntersectingPlane1()
   {
      FramePoint3D pointOnPlane = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      FrameVector3D planeNormal = new FrameVector3D(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      FramePoint3D lineStart = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, -1);
      FramePoint3D lineEnd = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 3);
      assertTrue(GeometryTools.isLineSegmentIntersectingPlane(pointOnPlane, planeNormal, lineStart, lineEnd));

      pointOnPlane = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector3D(pointOnPlane.getReferenceFrame(), 1, 0, 0);
      lineStart = new FramePoint3D(ReferenceFrame.getWorldFrame(), -6, 3, -3);
      lineEnd = new FramePoint3D(ReferenceFrame.getWorldFrame(), 6, 3, 6);
      assertTrue(GeometryTools.isLineSegmentIntersectingPlane(pointOnPlane, planeNormal, lineStart, lineEnd));

      pointOnPlane = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector3D(pointOnPlane.getReferenceFrame(), 0, 1, 0);
      lineStart = new FramePoint3D(ReferenceFrame.getWorldFrame(), 6, -3, -3);
      lineEnd = new FramePoint3D(ReferenceFrame.getWorldFrame(), 6, 3, 6);
      assertTrue(GeometryTools.isLineSegmentIntersectingPlane(pointOnPlane, planeNormal, lineStart, lineEnd));

      pointOnPlane = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector3D(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      lineStart = new FramePoint3D(ReferenceFrame.getWorldFrame(), 6, -3, 3);
      lineEnd = new FramePoint3D(ReferenceFrame.getWorldFrame(), 6, 3, 6);
      assertFalse(GeometryTools.isLineSegmentIntersectingPlane(pointOnPlane, planeNormal, lineStart, lineEnd));

      pointOnPlane = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector3D(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      lineStart = new FramePoint3D(ReferenceFrame.getWorldFrame(), 6, -3, -3);
      lineEnd = new FramePoint3D(ReferenceFrame.getWorldFrame(), 6, 3, -1);
      assertFalse(GeometryTools.isLineSegmentIntersectingPlane(pointOnPlane, planeNormal, lineStart, lineEnd));
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetPerpendicularVectorFromLineToPoint1()
   {
      FramePoint3D point0 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      FramePoint3D lineStart0 = new FramePoint3D(ReferenceFrame.getWorldFrame(), -10, 10, 0);
      FramePoint3D lineEnd0 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 10, 10, 0);
      FramePoint3D intersectionPoint0 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 10, 0);
      FrameVector3D x0 = new FrameVector3D(point0.getReferenceFrame());
      x0.sub(point0, intersectionPoint0);
      FrameVector3D expectedReturn0 = x0;
      FrameVector3D actualReturn0 = GeometryTools.getPerpendicularVectorFromLineToPoint(point0, lineStart0, lineEnd0, intersectionPoint0);

      assertTrue("Test Failed", expectedReturn0.epsilonEquals(actualReturn0, EPSILON));

      FramePoint3D point = new FramePoint3D(ReferenceFrame.getWorldFrame(), 4, 2, 0);
      FramePoint3D lineStart = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      FramePoint3D lineEnd = new FramePoint3D(ReferenceFrame.getWorldFrame(), 10, 10, 0);
      FramePoint3D intersectionPoint = new FramePoint3D(ReferenceFrame.getWorldFrame(), 3, 3, 0);
      FrameVector3D x = new FrameVector3D(point.getReferenceFrame());
      x.sub(point, intersectionPoint);
      FrameVector3D expectedReturn = x;
      FrameVector3D actualReturn = GeometryTools.getPerpendicularVectorFromLineToPoint(point, lineStart, lineEnd, intersectionPoint);
      assertTrue("Test Failed", expectedReturn.epsilonEquals(actualReturn, EPSILON));

      FramePoint3D point1 = new FramePoint3D(ReferenceFrame.getWorldFrame(), -2.5, 1.5, 0);
      FramePoint3D lineStart1 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      FramePoint3D lineEnd1 = new FramePoint3D(ReferenceFrame.getWorldFrame(), -4, 4, 0);
      FramePoint3D intersectionPoint1 = new FramePoint3D(ReferenceFrame.getWorldFrame(), -2, 2, 0);

      EuclidGeometryTools.orthogonalProjectionOnLineSegment2D(new Point2D(-2.5, 1.5), new Point2D(0, 0), new Point2D(-4, 4));
      FrameVector3D x1 = new FrameVector3D(point1.getReferenceFrame());
      x1.sub(point1, intersectionPoint1);
      FrameVector3D expectedReturn1 = x1;
      FrameVector3D actualReturn1 = GeometryTools.getPerpendicularVectorFromLineToPoint(point1, lineStart1, lineEnd1, intersectionPoint1);

      assertTrue("Test Failed", expectedReturn1.epsilonEquals(actualReturn1, EPSILON));
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetPlaneNormalGivenThreePoints()
   {
      FramePoint3D point1 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 5, 0);
      FramePoint3D point2 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 5, 0);
      FramePoint3D point3 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 5, 0);
      FrameVector3D expectedReturn = null;
      FrameVector3D actualReturn = GeometryTools.getPlaneNormalGivenThreePoints(point1, point2, point3);
      assertEquals("test failed", expectedReturn, actualReturn);

      FramePoint3D point91 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 3, 0);
      FramePoint3D point92 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 5, 0);
      FramePoint3D point93 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 1, 0);
      FrameVector3D expectedReturn9 = null;
      FrameVector3D actualReturn9 = GeometryTools.getPlaneNormalGivenThreePoints(point91, point92, point93);
      assertEquals("test failed", expectedReturn9, actualReturn9);

      FramePoint3D point81 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 9, 0, 0);
      FramePoint3D point82 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 7, 0, 0);
      FramePoint3D point83 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 4, 0, 0);
      FrameVector3D expectedReturn8 = null;
      FrameVector3D actualReturn8 = GeometryTools.getPlaneNormalGivenThreePoints(point81, point82, point83);
      assertEquals("test failed", expectedReturn8, actualReturn8);

      FramePoint3D point71 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 4);
      FramePoint3D point72 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 6);
      FramePoint3D point73 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 7);
      FrameVector3D expectedReturn7 = null;
      FrameVector3D actualReturn7 = GeometryTools.getPlaneNormalGivenThreePoints(point71, point72, point73);
      assertEquals("test failed", expectedReturn7, actualReturn7);

      FramePoint3D point11 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 5, 46);
      FramePoint3D point12 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 587, 3);
      FramePoint3D point13 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 18, 8);
      FramePoint3D p1 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 5, 5);
      FramePoint3D v1 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 1, 5, 5);
      FrameVector3D expectedReturn1 = new FrameVector3D(p1.getReferenceFrame());
      expectedReturn1.sub(p1, v1);
      FrameVector3D actualReturn1 = GeometryTools.getPlaneNormalGivenThreePoints(point11, point12, point13);
      assertTrue("Test Failed", expectedReturn1.epsilonEquals(actualReturn1, EPSILON));

      FramePoint3D point21 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 65, 0, 46);
      FramePoint3D point22 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 43, 0, 3);
      FramePoint3D point23 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 13, 0, 8);
      FramePoint3D p2 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 1, 5);
      FramePoint3D v2 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 0, 5);
      FrameVector3D expectedReturn2 = new FrameVector3D(p2.getReferenceFrame());
      expectedReturn2.sub(p2, v2);
      FrameVector3D actualReturn2 = GeometryTools.getPlaneNormalGivenThreePoints(point21, point22, point23);
      assertTrue("Test Failed", expectedReturn2.epsilonEquals(actualReturn2, EPSILON));

      FramePoint3D point31 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 65, 56, 0);
      FramePoint3D point32 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 43, 3, 0);
      FramePoint3D point33 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 13, 87, 0);
      FramePoint3D p3 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 55, 0);
      FramePoint3D v3 = new FramePoint3D(ReferenceFrame.getWorldFrame(), 0, 55, 1);
      FrameVector3D expectedReturn3 = new FrameVector3D(p3.getReferenceFrame());
      expectedReturn3.sub(p3, v3);
      FrameVector3D actualReturn3 = GeometryTools.getPlaneNormalGivenThreePoints(point31, point32, point33);
      assertTrue("Test Failed", expectedReturn3.epsilonEquals(actualReturn3, EPSILON));
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testIsPointOnLeftSideOfLine()
   {
      FramePoint3D lineStart = new FramePoint3D(ReferenceFrame.getWorldFrame(), 5.0, 0.0, 0.0);
      FramePoint3D lineEnd = new FramePoint3D(ReferenceFrame.getWorldFrame(), 5.0, 10.0, 0.0);

      FramePoint3D point = new FramePoint3D(ReferenceFrame.getWorldFrame(), 10, 5, 0.0);

      boolean expectedReturn = false;
      boolean actualReturn = GeometryTools.isPointOnLeftSideOfLine(point, lineStart, lineEnd);
      assertEquals("return value", expectedReturn, actualReturn);

      /** @todo fill in the test code */
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testClipToBoundingBox()
   {
      Tuple3DBasics tuple3d = new Point3D(1.0, -1.0, 0.0);
      GeometryTools.clipToBoundingBox(tuple3d, -0.5, 0.5, 0.5, -0.5, 0.0, 0.0);
      EuclidCoreTestTools.assertTuple3DEquals("not equal", new Point3D(0.5, -0.5, 0.0), tuple3d, 0.0);
      tuple3d.set(1.0, -1.0, 0.0);
      GeometryTools.clipToBoundingBox(tuple3d, 0.5, -0.5, -0.5, 0.5, -0.1, 0.1);
      EuclidCoreTestTools.assertTuple3DEquals("not equal", new Point3D(0.5, -0.5, 0.0), tuple3d, 0.0);
      tuple3d.set(1.0, -1.0, 2.0);
      GeometryTools.clipToBoundingBox(tuple3d, 0.5, -0.5, -0.5, 0.5, -0.1, 1.0);
      EuclidCoreTestTools.assertTuple3DEquals("not equal", new Point3D(0.5, -0.5, 1.0), tuple3d, 0.0);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testCombine()
   {
      Random random = new Random(1176L);
      ArrayList<Point2D> firstList = new ArrayList<Point2D>();
      for (int i = 0; i < 100; i++)
      {
         firstList.add(new Point2D(random.nextDouble(), random.nextDouble()));
      }

      ConvexPolygon2D firstPolygon = new ConvexPolygon2D(firstList);

      ArrayList<Point2D> secondList = new ArrayList<Point2D>();
      for (int i = 0; i < 200; i++)
      {
         secondList.add(new Point2D(random.nextDouble(), random.nextDouble()));
      }

      ConvexPolygon2D secondPolygon = new ConvexPolygon2D(secondList);

      ConvexPolygon2D result = new ConvexPolygon2D(firstPolygon, secondPolygon);

      // convexity of the result is already checked in another test
      for (Point2D point : firstList)
      {
         if (!result.isPointInside(point))
         {
            double distance = result.distance(point);

            if (distance > 1e-7)
               throw new RuntimeException("Not each point is inside the result. distance = " + distance);
         }

         //       assertTrue("Not each point isinside the result. distance = " , result.isPointInside(point));
      }

      for (Point2D point : secondList)
      {
         if (!result.isPointInside(point))
         {
            double distance = result.distance(point);

            if (distance > 1e-7)
               throw new RuntimeException("Not each point is inside the result. distance = " + distance);
         }

         //       assertTrue("Not each point is inside the result", result.isPointInside(point));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testNormalizeSafeZUp() throws Exception
   {
      Vector3D actualVector;
      Vector3D expectedVector = new Vector3D();
      Random random = new Random(1176L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         actualVector = RandomGeometry.nextVector3D(random, RandomNumbers.nextDouble(random, Epsilons.ONE_TRILLIONTH, 10.0));

         expectedVector.setAndNormalize(actualVector);
         GeometryTools.normalizeSafelyZUp(actualVector);
         EuclidCoreTestTools.assertTuple3DEquals(expectedVector, actualVector, Epsilons.ONE_TRILLIONTH);

         actualVector = RandomGeometry.nextVector3D(random, 0.999 * Epsilons.ONE_TRILLIONTH);
         expectedVector.set(0.0, 0.0, 1.0);
         GeometryTools.normalizeSafelyZUp(actualVector);
         EuclidCoreTestTools.assertTuple3DEquals(expectedVector, actualVector, Epsilons.ONE_TRILLIONTH);

         actualVector = new Vector3D();
         expectedVector.set(0.0, 0.0, 1.0);
         GeometryTools.normalizeSafelyZUp(actualVector);
         EuclidCoreTestTools.assertTuple3DEquals(expectedVector, actualVector, Epsilons.ONE_TRILLIONTH);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testIsZero() throws Exception
   {
      Random random = new Random(23423L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         double x = RandomNumbers.nextDouble(random, 0.0, 10.0);
         double y = RandomNumbers.nextDouble(random, 0.0, 10.0);
         double z = RandomNumbers.nextDouble(random, 0.0, 10.0);
         double epsilon = RandomNumbers.nextDouble(random, 0.0, 10.0);

         boolean isTuple2dZero = x < epsilon && y < epsilon;
         boolean isTuple3dZero = x < epsilon && y < epsilon && z < epsilon;

         assertEquals(isTuple2dZero, GeometryTools.isZero(new Point2D(x, y), epsilon));
         assertEquals(isTuple2dZero, GeometryTools.isZero(new Point2D(-x, y), epsilon));
         assertEquals(isTuple2dZero, GeometryTools.isZero(new Point2D(-x, -y), epsilon));
         assertEquals(isTuple2dZero, GeometryTools.isZero(new Point2D(x, -y), epsilon));
         assertEquals(isTuple2dZero, GeometryTools.isZero(new Point2D(x, y), epsilon));
         assertEquals(isTuple2dZero, GeometryTools.isZero(new Point2D(-x, y), epsilon));
         assertEquals(isTuple2dZero, GeometryTools.isZero(new Point2D(-x, -y), epsilon));
         assertEquals(isTuple2dZero, GeometryTools.isZero(new Point2D(x, -y), epsilon));

         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(x, y, z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(x, y, -z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(x, -y, z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(x, -y, -z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(-x, y, z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(-x, y, -z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(-x, -y, z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(-x, -y, -z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(x, y, z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(x, y, -z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(x, -y, z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(x, -y, -z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(-x, y, z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(-x, y, -z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(-x, -y, z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(-x, -y, -z), epsilon));
      }
   }

   public static void main(String[] args)
   {
      MutationTestFacilitator.facilitateMutationTestForClass(GeometryTools.class, GeometryToolsTest.class);
   }
}
