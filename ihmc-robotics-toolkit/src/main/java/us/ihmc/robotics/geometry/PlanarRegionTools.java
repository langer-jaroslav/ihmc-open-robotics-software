package us.ihmc.robotics.geometry;

import org.apache.commons.lang3.tuple.ImmutablePair;
import us.ihmc.commons.MathTools;
import us.ihmc.commons.lists.ListWrappingIndexTools;
import us.ihmc.euclid.geometry.*;
import us.ihmc.euclid.geometry.interfaces.*;
import us.ihmc.euclid.geometry.tools.EuclidGeometryPolygonTools;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.shape.primitives.Box3D;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.transform.interfaces.RigidBodyTransformReadOnly;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DBasics;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static us.ihmc.euclid.geometry.tools.EuclidGeometryTools.*;

public class PlanarRegionTools
{
   /**
    * Checks to see if regionA is above regionB in terms of not ever having to "step up" from regionA to regionB.
    * If regionA is above regionB, then regionB should not be able to be an obstacle of regionA.
    *
    * @param regionA PlanarRegion to check to see if it is above the other region.
    * @param regionB PlanarRegion to check to see if it is below the other region.
    * @param epsilon Margin of error. If epsilon is positive, then will still return true even if there are points in regionB higher than regionA by at most epsilon.
    * @return true if regionA is above regionB, else false.
    */
   public static boolean isPlanarRegionAAbovePlanarRegionB(PlanarRegion regionA, PlanarRegion regionB, double epsilon)
   {
      // first, check the bounding boxes
      double minBzInWorld = regionB.getBoundingBox3dInWorld().getMinZ();
      double maxBzInWorld = regionB.getBoundingBox3dInWorld().getMaxZ();

      // TODO add an initial bounding box check

      ConvexPolygon2D convexHullA = regionA.getConvexHull();

      RigidBodyTransformReadOnly transformToWorldA = regionA.getTransformToWorld();
      RigidBodyTransformReadOnly transformFromWorldToLocalB = regionB.getTransformToLocal();

      List<? extends Point2DReadOnly> verticesA = convexHullA.getPolygonVerticesView();

      Point3D pointAInWorld = new Point3D();
      Point3D pointAInLocalB = new Point3D();

      for (Point2DReadOnly vertexAInLocal : verticesA)
      {
         pointAInWorld.set(vertexAInLocal);
         pointAInWorld.setZ(0.0);
         transformToWorldA.transform(pointAInWorld);

         pointAInLocalB.set(pointAInWorld);
         transformFromWorldToLocalB.transform(pointAInLocalB);

         if (pointAInWorld.getZ() > maxBzInWorld + epsilon)
            return true;

         if ((pointAInWorld.getZ() > minBzInWorld + epsilon) && (pointAInLocalB.getZ() > epsilon))
            return true;
      }

      return false;
   }

   private static double[] getMinAndMaxZInWorld(List<? extends Point2DReadOnly> pointsInLocal, RigidBodyTransformReadOnly transformToWorld)
   {
      double minZ = Double.POSITIVE_INFINITY;
      double maxZ = Double.NEGATIVE_INFINITY;

      Point3D pointInWorld = new Point3D();

      for (Point2DReadOnly pointInLocal : pointsInLocal)
      {
         pointInWorld.set(pointInLocal);
         pointInWorld.setZ(0.0);
         transformToWorld.transform(pointInWorld);

         double pointZInWorld = pointInWorld.getZ();
         if (pointZInWorld < minZ)
            minZ = pointZInWorld;

         if (pointZInWorld > maxZ)
            maxZ = pointZInWorld;
      }

      return new double[] {minZ, maxZ};
   }

   public static boolean isPlanarRegionIntersectingWithCircle(Point2DReadOnly circleOriginInWorld, double circleRadius, PlanarRegion query)
   {
      RigidBodyTransform transformToWorld = new RigidBodyTransform();
      query.getTransformToWorld(transformToWorld);

      Point2D originInLocal = new Point2D(circleOriginInWorld);
      originInLocal.applyInverseTransform(transformToWorld, false);

      return query.getConvexHull().signedDistance(originInLocal) <= circleRadius;
   }

   public static boolean isPointInsidePolygon(Point2DReadOnly[] polygon, Point2DReadOnly pointToCheck)
   {
      return isPointInsidePolygon(Arrays.asList(polygon), pointToCheck);
   }

   public static boolean isPointInsidePolygon(List<? extends Point2DReadOnly> polygon, Point2DReadOnly pointToCheck)
   {
      if (polygon.size() < 3)
      {
         return false;
      }

      Point2DReadOnly rayOrigin = pointToCheck;
      Vector2D rayDirection = new Vector2D();

      Point2D pointOnArbitraryEdge = new Point2D();

      for (int i = 0; i < polygon.size(); i++)
      { // Picking an edge that is not parallel to the ray.
         Point2DReadOnly edgeStart = polygon.get(i);
         Point2DReadOnly edgeEnd = ListWrappingIndexTools.getNext(i, polygon);
         Vector2D edgeDirection = new Vector2D();
         edgeDirection.sub(edgeEnd, edgeStart);

         pointOnArbitraryEdge.interpolate(edgeStart, edgeEnd, 0.5);
         rayDirection.sub(pointOnArbitraryEdge, rayOrigin);

         double cross = edgeDirection.cross(rayDirection);

         if (Math.abs(cross) > 1.0e-3)
            break;
      }

      int numberOfIntersections = 0;

      Point2D previousIntersection = null;
      Point2D currentIntersection = null;

      for (int i = 0; i < polygon.size(); i++)
      {
         Point2DReadOnly edgeStart = polygon.get(i);
         Point2DReadOnly edgeEnd = ListWrappingIndexTools.getNext(i, polygon);

         currentIntersection = EuclidGeometryTools.intersectionBetweenRay2DAndLineSegment2D(rayOrigin, rayDirection, edgeStart, edgeEnd);

         if (currentIntersection != null)
         { // There is an intersection
            if (previousIntersection == null || !currentIntersection.epsilonEquals(previousIntersection, 1.0e-10))
            { // Because the intersection is different from the previous, the intersection is not on a vertex.
               numberOfIntersections++;
            }
         }

         previousIntersection = currentIntersection;
      }

      if (numberOfIntersections == 0)
      {
         //Could be both outside or inside
         return false;
      }

      // If the number of intersections is odd, the point is inside.
      return numberOfIntersections % 2 != 0;
   }

   /**
    * Return true if the given point is contained inside the boundary.
    * https://stackoverflow.com/questions/8721406/how-to-determine-if-a-point-is-inside-a-2d-convex-polygon
    *
    * Also check https://en.wikipedia.org/wiki/Point_in_polygon.
    *
    * @param test The point to check
    * @return true if the point is inside the boundary, false otherwise
    *
    */
   public static boolean isPointInsideConvexPolygon2D(ConvexPolygon2D polygon, Point2DReadOnly test)
   {
      int numberOfVertices = polygon.getNumberOfVertices();

      int i;
      int j;
      boolean result = false;

      for (i = 0, j = numberOfVertices - 1; i < numberOfVertices; j = i++)
      {
         Point2DReadOnly iVertex = polygon.getVertex(i);
         Point2DReadOnly jVertex = polygon.getVertex(j);

         if ((iVertex.getY() > test.getY()) != (jVertex.getY() > test.getY())
               && (test.getX() < (jVertex.getX() - iVertex.getX()) * (test.getY() - iVertex.getY()) / (jVertex.getY() - iVertex.getY()) + iVertex.getX()))
         {
            result = !result;
         }
      }
      return result;
   }

   public static boolean isPointInWorldInsidePlanarRegion(PlanarRegion planarRegion, Point3DReadOnly pointInWorldToCheck, double epsilon)
   {
      RigidBodyTransform transformToWorld = new RigidBodyTransform();
      planarRegion.getTransformToWorld(transformToWorld);
      Point2D pointInLocalToCheck = new Point2D(pointInWorldToCheck);
      pointInLocalToCheck.applyInverseTransform(transformToWorld, false);
      return isPointInLocalInsidePlanarRegion(planarRegion, pointInLocalToCheck, epsilon);
   }

   public static boolean isPointInLocalInsidePlanarRegion(PlanarRegion planarRegion, Point3DReadOnly pointInLocalToCheck)
   {
      return isPointInLocalInsidePlanarRegion(planarRegion, new Point2D(pointInLocalToCheck));
   }

   public static boolean isPointInLocalInsidePlanarRegion(PlanarRegion planarRegion, Point3DReadOnly pointInLocalToCheck, double epsilon)
   {
      return isPointInLocalInsidePlanarRegion(planarRegion, new Point2D(pointInLocalToCheck), epsilon);
   }

   public static boolean isPointInLocalInsidePlanarRegion(PlanarRegion planarRegion, Point2DReadOnly pointInLocalToCheck)
   {
      return isPointInLocalInsidePlanarRegion(planarRegion, pointInLocalToCheck, 0.0);
   }

   public static boolean isPointInLocalInsidePlanarRegion(PlanarRegion planarRegion, Point2DReadOnly pointInLocalToCheck, double epsilon)
   {
      ConvexPolygon2D convexHull = planarRegion.getConvexHull();
      BoundingBox2D boundingBox = convexHull.getBoundingBox();

      if (!boundingBox.isInsideEpsilon(pointInLocalToCheck, epsilon))
         return false;
      if (!convexHull.isPointInside(pointInLocalToCheck, epsilon))
         return false;
      List<ConvexPolygon2D> convexPolygons = planarRegion.getConvexPolygons();

      // If inside the convex hull at this point, then if there is only one polygon, you are also inside that too...
      //TODO: Unit tests for all of this.
      if (convexPolygons.size() == 1)
      {
         return true;
      }

      if (MathTools.epsilonEquals(0.0, epsilon, 1.0e-10))
      {
         //TODO: +++JerryPratt: Discuss this one with Sylvain. Do we want to check inside the concave hull, or check each planar region individually?

         for (ConvexPolygon2D convexPolygon : convexPolygons)
         {
            //+++JerryPratt: Not sure if this one is faster or not. Discuss with Sylvain best way to do point inside convex polygon check.
            // Seems like you should be able to do a binary search on the distance to vertices, since it should be monotonic, right?
            //            boolean isInsidePolygon = convexPolygon.isPointInside(pointInLocalToCheck);
            boolean isInsidePolygon = isPointInsideConvexPolygon2D(convexPolygon, pointInLocalToCheck);

            if (isInsidePolygon)
               return true;
         }
         return false;

         //         return isPointInsidePolygon(planarRegion.getConcaveHull(), pointInLocalToCheck);
      }
      else
      {
         //TODO: +++JerryPratt: Discuss this one with Sylvain. Do we want to check inside the concave hull, or check each planar region individually?

         for (ConvexPolygon2D convexPolygon : convexPolygons)
         {
            //+++JerryPratt: Not sure if this one is faster or not. Discuss with Sylvain best way to do point inside convex polygon check.
            // Seems like you should be able to do a binary search on the distance to vertices, since it should be monotonic, right?
            //            boolean isInsidePolygon = convexPolygon.isPointInside(pointInLocalToCheck);
            boolean isInsidePolygon = convexPolygon.isPointInside(pointInLocalToCheck, epsilon);

            if (isInsidePolygon)
               return true;

            //TODO: +++JerryPratt: Discuss using the concaveHull or not. It seems buggy when points cross over the other side..
            // When ClusterTools.extrudePolygon() is buggy...
            //
            //            if (planarRegion.getConcaveHullSize() < convexHull.getNumberOfVertices())
            //               throw new IllegalArgumentException("The concave hull of this polygon is not valid.");
            //
            //         double[] epsilons = new double[planarRegion.getConcaveHullSize()];
            //         Arrays.fill(epsilons, epsilon);
            //         List<Point2D> concaveHull = ClusterTools.extrudePolygon(true, Arrays.asList(planarRegion.getConcaveHull()), epsilons);
            //
            //         return isPointInsidePolygon(concaveHull, pointInLocalToCheck);
         }
      }
      return false;
   }

   public static double getDistanceFromLineSegment3DToPlanarRegion(LineSegment3DReadOnly lineSegmentInLocal,
                                                                   PlanarRegion planarRegion, Point3DBasics closestPointOnSegmentToPack,
                                                                   Point3DBasics closestPointOnRegionToPack)
   {
      return getDistanceFromLineSegment3DToPlanarRegion(lineSegmentInLocal.getFirstEndpoint(), lineSegmentInLocal.getSecondEndpoint(), planarRegion,
                                                        closestPointOnSegmentToPack, closestPointOnRegionToPack);
   }

   public static double getDistanceFromLineSegment3DToPlanarRegion(Point3DReadOnly firstEndPointInLocal, Point3DReadOnly secondEndPointInLocal,
                                                                   PlanarRegion planarRegion, Point3DBasics closestPointOnSegmentToPack,
                                                                   Point3DBasics closestPointOnRegionToPack)
   {
      List<Point3D> vertices = Arrays.stream(planarRegion.getConcaveHull()).map(Point3D::new).collect(Collectors.toList());
      int numberOfVertices = vertices.size();
      double minDistanceToEdge = Double.POSITIVE_INFINITY;

      if (numberOfVertices == 0)
      {
         minDistanceToEdge = Double.NaN;
      }
      else if (numberOfVertices == 1)
      {
         if (closestPointOnRegionToPack != null)
            closestPointOnRegionToPack.set(vertices.get(0));
         if (closestPointOnSegmentToPack != null)
         {
            closestPointOnSegmentToPack.set(orthogonalProjectionOnLineSegment3D(vertices.get(0), firstEndPointInLocal, secondEndPointInLocal));
            minDistanceToEdge = vertices.get(0).distance(closestPointOnSegmentToPack);
         }
         else
         {
            minDistanceToEdge = distanceFromPoint3DToLineSegment3D(vertices.get(0), firstEndPointInLocal, secondEndPointInLocal);
         }
      }
      else if (numberOfVertices == 2)
      {
         minDistanceToEdge = closestPoint3DsBetweenTwoLineSegment3Ds(firstEndPointInLocal, secondEndPointInLocal, vertices.get(0), vertices.get(1),
                                                                     closestPointOnSegmentToPack, closestPointOnRegionToPack);
      }
      else
      {
         Point3D point1ToThrowAway = new Point3D();
         Point3D point2ToThrowAway = new Point3D();

         for (ConvexPolygon2D convexPolygon : planarRegion.getConvexPolygons())
         {
            List<Point3D> polygonVertices = convexPolygon.getVertexBufferView().stream().map(Point3D::new).collect(Collectors.toList());
            double distanceToPolygon = getDistanceFromLineSegment3DToConvexPolygon(firstEndPointInLocal, secondEndPointInLocal, polygonVertices,
                                                                                   point1ToThrowAway, point2ToThrowAway);
            if (distanceToPolygon < minDistanceToEdge)
            {
               minDistanceToEdge = distanceToPolygon;
               if (closestPointOnSegmentToPack != null)
                  closestPointOnSegmentToPack.set(point1ToThrowAway);
               if (closestPointOnRegionToPack != null)
                  closestPointOnRegionToPack.set(point2ToThrowAway);
            }
         }
      }

      return minDistanceToEdge;
   }

   public static double getDistanceFromLineSegment3DToConvexPolygon(Point3DReadOnly firstEndPointInLocal, Point3DReadOnly secondEndPointInLocal,
                                                                    List<Point3D> convexPolygon3D, Point3DBasics closestPointOnSegmentToPack,
                                                                    Point3DBasics closestPointOnRegionToPack)
   {
      int numberOfVertices = convexPolygon3D.size();
      double minDistanceToEdge = Double.POSITIVE_INFINITY;

      if (numberOfVertices == 0)
         minDistanceToEdge = Double.NaN;

      else if (numberOfVertices == 1)
      {
         if (closestPointOnRegionToPack != null)
            closestPointOnRegionToPack.set(convexPolygon3D.get(0));
         if (closestPointOnSegmentToPack != null)
         {
            closestPointOnSegmentToPack.set(orthogonalProjectionOnLineSegment3D(convexPolygon3D.get(0), firstEndPointInLocal, secondEndPointInLocal));
            minDistanceToEdge = convexPolygon3D.get(0).distance(closestPointOnSegmentToPack);
         }
         else
         {
            minDistanceToEdge = distanceFromPoint3DToLineSegment3D(convexPolygon3D.get(0), firstEndPointInLocal, secondEndPointInLocal);
         }
      }

      else if (numberOfVertices == 2)
      {
         minDistanceToEdge = closestPoint3DsBetweenTwoLineSegment3Ds(firstEndPointInLocal, secondEndPointInLocal, convexPolygon3D.get(0), convexPolygon3D.get(1),
                                                                     closestPointOnSegmentToPack, closestPointOnRegionToPack);
      }
      else
      {
         Point3D point1ToThrowAway = new Point3D();
         Point3D point2ToThrowAway = new Point3D();
         boolean isQueryStartOutsidePolygon = false;
         boolean isQueryEndOutsidePolygon = false;

         for (int index = 0; index < numberOfVertices; index++)
         {
            Point3DReadOnly edgeStart = convexPolygon3D.get(index);
            Point3DReadOnly edgeEnd = convexPolygon3D.get(EuclidGeometryPolygonTools.next(index, numberOfVertices));
            Point2DReadOnly edgeStart2D = new Point2D(edgeStart);
            Point2DReadOnly edgeEnd2D = new Point2D(edgeEnd);

            double distanceToEdge = closestPoint3DsBetweenTwoLineSegment3Ds(firstEndPointInLocal, secondEndPointInLocal, edgeStart, edgeEnd, point1ToThrowAway,
                                                                            point2ToThrowAway);
            if (distanceToEdge < minDistanceToEdge)
            {
               minDistanceToEdge = distanceToEdge;
               if (closestPointOnSegmentToPack != null)
                  closestPointOnSegmentToPack.set(point1ToThrowAway);
               if (closestPointOnRegionToPack != null)
                  closestPointOnRegionToPack.set(point2ToThrowAway);
            }

            isQueryStartOutsidePolygon |= isPoint2DOnSideOfLine2D(firstEndPointInLocal.getX(), firstEndPointInLocal.getY(), edgeStart2D, edgeEnd2D, true);
            isQueryEndOutsidePolygon |= isPoint2DOnSideOfLine2D(secondEndPointInLocal.getX(), secondEndPointInLocal.getY(), edgeStart2D, edgeEnd2D, true);
         }

         boolean pointsAllAbove = firstEndPointInLocal.getZ() > 0 && secondEndPointInLocal.getZ() > 0;
         boolean pointsAllBelow = firstEndPointInLocal.getZ() < 0 && secondEndPointInLocal.getZ() < 0;

         if (!isQueryStartOutsidePolygon && !isQueryEndOutsidePolygon)
         { // points are within the edges, so the shortest distance is to the plane surface
            double distanceToFirstEnd = Math.abs(firstEndPointInLocal.getZ());
            double distanceToSecondEnd = Math.abs(secondEndPointInLocal.getZ());
            if (distanceToFirstEnd < distanceToSecondEnd)
            {
               minDistanceToEdge = distanceToFirstEnd;
               if (closestPointOnSegmentToPack != null)
               closestPointOnSegmentToPack.set(firstEndPointInLocal);
               if (closestPointOnRegionToPack != null)
                  closestPointOnRegionToPack.set(firstEndPointInLocal.getX(), firstEndPointInLocal.getY(), 0.0);
            }
            else
            {
               minDistanceToEdge = distanceToSecondEnd;
               if (closestPointOnSegmentToPack != null)
                  closestPointOnSegmentToPack.set(secondEndPointInLocal);
               if (closestPointOnRegionToPack != null)
                  closestPointOnRegionToPack.set(secondEndPointInLocal.getX(), secondEndPointInLocal.getY(), 0.0);
            }
         }
         else if (!pointsAllAbove || !pointsAllBelow)
         { // points are on opposite sides of the plane
            Point3DReadOnly intersectionWithPlane = EuclidGeometryTools.intersectionBetweenLineSegment3DAndPlane3D(convexPolygon3D.get(0),
                                                                                                                   new Vector3D(0.0, 0.0, 1.0),
                                                                                                                   firstEndPointInLocal, secondEndPointInLocal);

            // checking convex hull here - might be better to check all polygons to avoid false positive
            if (intersectionWithPlane != null)
            {
               List<Point2DReadOnly> polygonIn2D = new ArrayList<>();
               Point2DReadOnly intersectionIn2D = new Point2D(intersectionWithPlane);
               convexPolygon3D.forEach(vertex -> polygonIn2D.add(new Point2D(vertex)));
               if (isPointInsidePolygon(polygonIn2D, intersectionIn2D))
               {
                  minDistanceToEdge = -minDistanceToEdge;
                  if (closestPointOnRegionToPack != null)
                     closestPointOnRegionToPack.set(intersectionWithPlane);
                  if (closestPointOnSegmentToPack != null)
                     closestPointOnSegmentToPack.set(intersectionWithPlane);
               }
            }
         }
      }

      return minDistanceToEdge;
   }

   public static double getDistanceFromLineSegment3DToConvexPolygon(Point3DReadOnly firstEndPointInLocal, Point3DReadOnly secondEndPointInLocal,
                                                                    List<Point3D> convexPolygon3D)
   {
      int numberOfVertices = convexPolygon3D.size();
      double minDistanceToEdge = Double.POSITIVE_INFINITY;

      if (numberOfVertices == 0)
         minDistanceToEdge = Double.NaN;

      else if (numberOfVertices == 1)
         minDistanceToEdge = distanceFromPoint3DToLineSegment3D(convexPolygon3D.get(0), firstEndPointInLocal, secondEndPointInLocal);

      else if (numberOfVertices == 2)
         minDistanceToEdge = distanceBetweenTwoLineSegment3Ds(firstEndPointInLocal, secondEndPointInLocal, convexPolygon3D.get(0), convexPolygon3D.get(1));
      else
      {
         boolean isQueryStartOutsidePolygon = false;
         boolean isQueryEndOutsidePolygon = false;

         for (int index = 0; index < numberOfVertices; index++)
         {
            Point3DReadOnly edgeStart = convexPolygon3D.get(index);
            Point3DReadOnly edgeEnd = convexPolygon3D.get(EuclidGeometryPolygonTools.next(index, numberOfVertices));
            Point2DReadOnly edgeStart2D = new Point2D(edgeStart);
            Point2DReadOnly edgeEnd2D = new Point2D(edgeEnd);

            minDistanceToEdge = Math.min(minDistanceToEdge, distanceBetweenTwoLineSegment3Ds(firstEndPointInLocal, secondEndPointInLocal, edgeStart, edgeEnd));

            isQueryStartOutsidePolygon |= isPoint2DOnSideOfLine2D(firstEndPointInLocal.getX(), firstEndPointInLocal.getY(), edgeStart2D, edgeEnd2D, true);
            isQueryEndOutsidePolygon |= isPoint2DOnSideOfLine2D(secondEndPointInLocal.getX(), secondEndPointInLocal.getY(), edgeStart2D, edgeEnd2D, true);
         }

         boolean pointsAllAbove = firstEndPointInLocal.getZ() > 0 && secondEndPointInLocal.getZ() > 0;
         boolean pointsAllBelow = firstEndPointInLocal.getZ() < 0 && secondEndPointInLocal.getZ() < 0;

         if (!isQueryStartOutsidePolygon && !isQueryEndOutsidePolygon)
         { // points are within the edges, so the shortest distance is to the plane surface
            minDistanceToEdge = Math.min(Math.abs(firstEndPointInLocal.getZ()), Math.abs(secondEndPointInLocal.getZ()));
         }
         else if (!pointsAllAbove || !pointsAllBelow)
         { // points are on opposite sides of the plane
            Point3DReadOnly intersectionWithPlane = EuclidGeometryTools.intersectionBetweenLineSegment3DAndPlane3D(convexPolygon3D.get(0),
                                                                                                                   new Vector3D(0.0, 0.0, 1.0),
                                                                                                                   firstEndPointInLocal, secondEndPointInLocal);

            // checking convex hull here - might be better to check all polygons to avoid false positive
            if (intersectionWithPlane != null)
            {
               List<Point2DReadOnly> polygonIn2D = new ArrayList<>();
               Point2DReadOnly intersectionIn2D = new Point2D(intersectionWithPlane);
               convexPolygon3D.forEach(vertex -> polygonIn2D.add(new Point2D(vertex)));
               if (isPointInsidePolygon(polygonIn2D, intersectionIn2D))
               {
                  minDistanceToEdge = -minDistanceToEdge;
               }
            }
         }
      }

      return minDistanceToEdge;
   }

   public static boolean isPlanarRegionIntersectingWithCapsule(LineSegment3D capsuleSegmentInWorld, double capsuleRadius, PlanarRegion query)
   {
      RigidBodyTransform transformToWorld = new RigidBodyTransform();
      query.getTransformToWorld(transformToWorld);

      Point3D firstEndPointInLocal = new Point3D(capsuleSegmentInWorld.getFirstEndpoint());
      Point3D secondEndPointInLocal = new Point3D(capsuleSegmentInWorld.getSecondEndpoint());
      firstEndPointInLocal.applyInverseTransform(transformToWorld);
      secondEndPointInLocal.applyInverseTransform(transformToWorld);

      Point2D[] concaveHull = query.getConcaveHull();

      List<Point3D> convexPolygon3D = new ArrayList<>();
      for (int i = 0; i < concaveHull.length; i++)
         convexPolygon3D.add(new Point3D(concaveHull[i]));

      double minDistanceToEdge = getDistanceFromLineSegment3DToConvexPolygon(firstEndPointInLocal, secondEndPointInLocal, convexPolygon3D);

      return minDistanceToEdge <= capsuleRadius;
   }

   public static BoundingBox3D getLocalBoundingBox3DInLocal(PlanarRegion planarRegion)
   {
      BoundingBox3D boundingBox3DInLocal = new BoundingBox3D();

      for (ConvexPolygon2D convexPolygon : planarRegion.getConvexPolygons())
      {
         for (int j = 0; j < convexPolygon.getNumberOfVertices(); j++)
         {
            boundingBox3DInLocal.updateToIncludePoint(convexPolygon.getVertex(j).getX(), convexPolygon.getVertex(j).getY(), 0.0);
         }
      }

      return boundingBox3DInLocal;
   }

   public static BoundingBox2D getLocalBoundingBox2DInLocal(PlanarRegion planarRegion)
   {
      BoundingBox2D boundingBox2DInLocal = null;

      for (ConvexPolygon2D convexPolygon : planarRegion.getConvexPolygons())
      {
         for (int j = 0; j < convexPolygon.getNumberOfVertices(); j++)
         {
            Point2DReadOnly vertex = convexPolygon.getVertex(j);
            if (boundingBox2DInLocal == null)
            {
               boundingBox2DInLocal = new BoundingBox2D(vertex, vertex);
            }
            else
            {
               boundingBox2DInLocal.updateToIncludePoint(vertex);
            }
         }
      }

      return boundingBox2DInLocal;
   }

   public static Box3D getLocalBoundingBox3DInWorld(PlanarRegion planarRegion, double height)
   {
      BoundingBox3D boundingBox3DInLocal = getLocalBoundingBox3DInLocal(planarRegion);
      boundingBox3DInLocal.updateToIncludePoint(0.0, 0.0, height / 2.0);
      boundingBox3DInLocal.updateToIncludePoint(0.0, 0.0, -height / 2.0);
      Box3D box = GeometryTools.convertBoundingBox3DToBox3D(boundingBox3DInLocal);
      box.applyTransform(planarRegion.getTransformToWorld());
      return box;
   }

   public static List<PlanarRegion> filterPlanarRegionsByHullSize(int minNumberOfVertices, List<PlanarRegion> planarRegions)
   {
      if (minNumberOfVertices <= 0)
         return planarRegions;

      return planarRegions.stream().filter(region -> region.getConcaveHull().length >= minNumberOfVertices).collect(Collectors.toList());
   }

   public static List<PlanarRegion> filterPlanarRegionsByArea(double minArea, List<PlanarRegion> planarRegions)
   {
      if (!Double.isFinite(minArea) || minArea <= 0.0)
         return planarRegions;

      return planarRegions.stream().filter(region -> computePlanarRegionArea(region) >= minArea).collect(Collectors.toList());
   }

   public static List<PlanarRegion> filterPlanarRegionsWithBoundingCircle(Point2DReadOnly circleOrigin, double circleRadius, List<PlanarRegion> planarRegions)
   {
      if (!Double.isFinite(circleRadius) || circleRadius < 0.0)
         return planarRegions;

      return planarRegions.stream().filter(region -> isPlanarRegionIntersectingWithCircle(circleOrigin, circleRadius, region)).collect(Collectors.toList());
   }

   public static List<PlanarRegion> filterPlanarRegionsWithBoundingCapsule(Point3DReadOnly capsuleStartInWorld, Point3DReadOnly capsuleEndInWorld,
                                                                           double capsuleRadius, List<PlanarRegion> planarRegions)
   {
      return filterPlanarRegionsWithBoundingCapsule(new LineSegment3D(capsuleStartInWorld, capsuleEndInWorld), capsuleRadius, planarRegions);
   }

   public static List<PlanarRegion> filterPlanarRegionsWithBoundingCapsule(LineSegment3D capsuleSegmentInWorld, double capsuleRadius,
                                                                           List<PlanarRegion> planarRegions)
   {
      if (!Double.isFinite(capsuleRadius) || capsuleRadius < 0.0)
         return planarRegions;

      return planarRegions.stream().filter(region -> isPlanarRegionIntersectingWithCapsule(capsuleSegmentInWorld, capsuleRadius, region))
                          .collect(Collectors.toList());
   }

   public static double computePlanarRegionArea(PlanarRegion planarRegion)
   {
      double area = 0.0;
      for (int i = 0; i < planarRegion.getNumberOfConvexPolygons(); i++)
      {
         area += planarRegion.getConvexPolygon(i).getArea();
      }
      return area;
   }

   public static Point2D getAverageCentroid2DInLocal(PlanarRegion planarRegion)
   {
      Point2D centroid = new Point2D();

      int count = 0;
      double xSum = 0.0;
      double ySum = 0.0;
      for (ConvexPolygon2D convexPolygon : planarRegion.getConvexPolygons())
      {
         Point2DReadOnly convexPolygonCentroid = convexPolygon.getCentroid();

         xSum += convexPolygonCentroid.getX();
         ySum += convexPolygonCentroid.getY();
         ++count;
      }

      centroid.setX(xSum / count);
      centroid.setY(ySum / count);

      return centroid;
   }

   public static Point3D getAverageCentroid3DInWorld(PlanarRegion planarRegion)
   {
      Point2D averageCentroid2DInLocal = getAverageCentroid2DInLocal(planarRegion);
      Point3D point3D = new Point3D(averageCentroid2DInLocal);
      point3D.applyTransform(planarRegion.getTransformToWorld());
      return point3D;
   }

   /**
    * Finds the minimum height between the convex hulls of two PlanarRegions when projected vertically.
    * It does this by projecting all the vertices of one of the convex hulls to the other PlanarRegion's plane vertically in world. It then finds the minimum distance of those projections.
    * Next it does the same thing for the other convex hull projected vertically onto the other region's plane. Finally, it takes the max of the two.
    * If one of the Planar Regions are vertical, such that the points of the other cannot be projected vertical on it, then that projection is ignored.
    *
    * @param regionA PlanarRegion to test height of points of its convex hull above the plane. Not modified.
    * @param regionB PlanarRegion that defines the plane that the points will be projected down onto. Not modified.
    * @return Minimum vertical projection of {@code regionA} vertices onto the plane of regionB. The returned value is negative if the lowest vertex is below {@code region B}.
    */
   public static double computeMinHeightOfRegionAAboveRegionB(PlanarRegion regionA, PlanarRegion regionB)
   {
      RigidBodyTransformReadOnly transformFromAToWorld = regionA.getTransformToWorld();

      double minZOfAProjectedToB = Double.POSITIVE_INFINITY;
      ConvexPolygon2D convexHullInLocalA = regionA.getConvexHull();

      for (int i = 0; i < convexHullInLocalA.getNumberOfVertices(); i++)
      {
         Point3D vertexOfAInWorld = new Point3D(convexHullInLocalA.getVertex(i));
         transformFromAToWorld.transform(vertexOfAInWorld);
         Point3D vertexOfAProjectedToBInWorld = projectInZToPlanarRegion(vertexOfAInWorld, regionB);
         if (vertexOfAProjectedToBInWorld != null)
         {
            double deltaZ = vertexOfAInWorld.getZ() - vertexOfAProjectedToBInWorld.getZ();
            minZOfAProjectedToB = Math.min(minZOfAProjectedToB, deltaZ);
         }
      }

      RigidBodyTransformReadOnly transformFromBToWorld = regionB.getTransformToWorld();

      double minZOfBProjectedToA = Double.POSITIVE_INFINITY;
      ConvexPolygon2D convexHullInLocalB = regionB.getConvexHull();

      for (int i = 0; i < convexHullInLocalB.getNumberOfVertices(); i++)
      {
         Point3D vertexOfBInWorld = new Point3D(convexHullInLocalB.getVertex(i));
         transformFromBToWorld.transform(vertexOfBInWorld);
         Point3D vertexOfBProjectedToAInWorld = projectInZToPlanarRegion(vertexOfBInWorld, regionA);
         if (vertexOfBProjectedToAInWorld != null)
         {
            double deltaZ = vertexOfBProjectedToAInWorld.getZ() - vertexOfBInWorld.getZ();
            minZOfBProjectedToA = Math.min(minZOfBProjectedToA, deltaZ);
         }
      }

      if (Double.isInfinite(minZOfAProjectedToB))
         return minZOfBProjectedToA;

      if (Double.isInfinite(minZOfBProjectedToA))
         return minZOfAProjectedToB;

      return Math.max(minZOfAProjectedToB, minZOfBProjectedToA);
   }

   /**
    * Projects a point in world frame vertically down or up onto a PlanarRegion and returns the intersection in world frame.
    *
    * @param pointInWorldToProjectInZ
    * @param planarRegion
    * @return the vertically projected point
    */
   public static Point3D projectInZToPlanarRegion(Point3DReadOnly pointInWorldToProjectInZ, PlanarRegion planarRegion)
   {
      Vector3D surfaceNormalInWorld = planarRegion.getNormal();

      RigidBodyTransform transformToWorld = new RigidBodyTransform();
      planarRegion.getTransformToWorld(transformToWorld);

      Point3D planarRegionReferencePointInWorld = new Point3D(0.0, 0.0, 0.0);
      transformToWorld.transform(planarRegionReferencePointInWorld);

      Vector3DReadOnly verticalLine = new Vector3D(0.0, 0.0, 1.0);

      return EuclidGeometryTools.intersectionBetweenLine3DAndPlane3D(planarRegionReferencePointInWorld, surfaceNormalInWorld, pointInWorldToProjectInZ,
                                                                     verticalLine);
   }

   public static boolean allVerticesAreAbovePlane3D(Plane3D plane, PlanarRegion planarRegion)
   {
      Point3D vertexInWorld = new Point3D();
      RigidBodyTransformReadOnly regionToWorld = planarRegion.getTransformToWorld();

      for (ConvexPolygon2D convexPolygon : planarRegion.getConvexPolygons())
      {
         for (int i = 0; i < convexPolygon.getNumberOfVertices(); i++)
         {
            Point2DReadOnly vertex = convexPolygon.getVertex(i);
            vertexInWorld.set(vertex.getX(), vertex.getY(), 0.0);
            vertexInWorld.applyTransform(regionToWorld);

            if (!EuclidGeometryTools.isPoint3DAbovePlane3D(vertexInWorld, plane.getPoint(), plane.getNormal()))
            {
               return false;
            }
         }
      }

      return true;
   }

   public static boolean allVerticesAreBelowPlane3D(Plane3D plane, PlanarRegion planarRegion)
   {
      Point3D vertexInWorld = new Point3D();
      RigidBodyTransformReadOnly regionToWorld = planarRegion.getTransformToWorld();

      for (ConvexPolygon2D convexPolygon : planarRegion.getConvexPolygons())
      {
         for (int i = 0; i < convexPolygon.getNumberOfVertices(); i++)
         {
            Point2DReadOnly vertex = convexPolygon.getVertex(i);
            vertexInWorld.set(vertex.getX(), vertex.getY(), 0.0);
            vertexInWorld.applyTransform(regionToWorld);

            if (!EuclidGeometryTools.isPoint3DBelowPlane3D(vertexInWorld, plane.getPoint(), plane.getNormal()))
            {
               return false;
            }
         }
      }

      return true;
   }

   /**
    * Find all the planar regions that intersect with the given convex polygon. The algorithm is
    * equivalent to projecting all the regions onto the XY-plane and then finding the regions
    * intersecting with the given convex polygon.
    *
    * @param convexPolygon the query.
    * @return the list of planar regions intersecting with the given polygon. Returns null when no
    *         region intersects.
    */
   public static List<PlanarRegion> findPlanarRegionsIntersectingPolygon(ConvexPolygon2DReadOnly convexPolygon, PlanarRegionsList regions)
   {
      return findPlanarRegionsIntersectingPolygon(convexPolygon, regions.getPlanarRegionsAsList());
   }

   /**
    * Find all the planar regions that intersect with the given convex polygon. The algorithm is
    * equivalent to projecting all the regions onto the XY-plane and then finding the regions
    * intersecting with the given convex polygon.
    *
    * @param convexPolygon the query.
    * @return the list of planar regions intersecting with the given polygon. Returns null when no
    *         region intersects.
    */
   public static List<PlanarRegion> findPlanarRegionsIntersectingPolygon(ConvexPolygon2DReadOnly convexPolygon, List<PlanarRegion> regions)
   {
      List<PlanarRegion> containers = null;

      for (int i = 0; i < regions.size(); i++)
      {
         PlanarRegion candidateRegion = regions.get(i);
         if (candidateRegion.isVertical())
            continue;

         if (candidateRegion.isPolygonIntersecting(convexPolygon))
         {
            if (containers == null)
               containers = new ArrayList<>();
            containers.add(candidateRegion);
         }
      }

      return containers;
   }

   /**
    * Find all the planar regions that contain the given point.
    *
    * @param point the query coordinates.
    * @param maximumOrthogonalDistance tolerance expressed as maximum orthogonal distance from the
    *           region.
    * @return the list of planar regions containing the query. Returns null when no region contains
    *         the query.
    */
   public static List<PlanarRegion> findPlanarRegionsContainingPoint(List<PlanarRegion> planarRegionsToCheck, Point3DReadOnly point, double maximumOrthogonalDistance)
   {
      List<PlanarRegion> containers = null;

      for (int i = 0; i < planarRegionsToCheck.size(); i++)
      {
         PlanarRegion candidateRegion = planarRegionsToCheck.get(i);
         if (candidateRegion.isPointInside(point, maximumOrthogonalDistance))
         {
            if (containers == null)
               containers = new ArrayList<>();
            containers.add(candidateRegion);
         }
      }

      return containers;
   }

   /**
    * Find all the planar regions that contain the given point. The algorithm is equivalent to
    * projecting all the regions onto the XY-plane and then finding the regions containing the
    * point.
    *
    * @param planarRegionsToCheck the list of the planar regions to look through
    * @param point the query coordinates.
    * @return the list of planar regions containing the query. Returns null when no region contains
    *         the query.
    */
   public static List<PlanarRegion> findPlanarRegionsContainingPointByProjectionOntoXYPlane(List<PlanarRegion> planarRegionsToCheck, Point2DReadOnly point)
   {
      return findPlanarRegionsContainingPointByProjectionOntoXYPlane(planarRegionsToCheck, point.getX(), point.getY());
   }

   /**
    * Find all the planar regions that contain the given point. The algorithm is equivalent to
    * projecting all the regions onto the XY-plane and then finding the regions containing the
    * point.
    *
    * @param planarRegionsToCheck the list of the planar regions to look through.
    * @param x the query x-coordinate.
    * @param y the query y-coordinate.
    * @return the list of planar regions containing the query. Returns null when no region contains
    *         the query.
    */
   public static List<PlanarRegion> findPlanarRegionsContainingPointByProjectionOntoXYPlane(List<PlanarRegion> planarRegionsToCheck, double x, double y)
   {
      List<PlanarRegion> containers = null;

      for (int i = 0; i < planarRegionsToCheck.size(); i++)
      {
         PlanarRegion candidateRegion = planarRegionsToCheck.get(i);
         if (candidateRegion.isPointInsideByProjectionOntoXYPlane(x, y))
         {
            if (containers == null)
               containers = new ArrayList<>();
            containers.add(candidateRegion);
         }
      }

      return containers;
   }

   /**
    * Check if the vertical projections of the convex hulls of two planar regions overlap within epsilon.
    */
   public static boolean isRegionAOverlappingWithRegionB(PlanarRegion regionOne, PlanarRegion regionTwo, double epsilon)
   {
      BoundingBox2DReadOnly boundingBoxOne = getVerticallyProjectedBoundingBox(regionOne);
      BoundingBox2DReadOnly boundingBoxTwo = getVerticallyProjectedBoundingBox(regionTwo);

      // first check the bounding boxes
      if (!boundingBoxOne.intersectsEpsilon(boundingBoxTwo, epsilon))
         return false;

//      ConvexPolygon2D convexHullOne = getVerticallyProjectedConvexHull(regionOne);
//      ConvexPolygon2D convexHullTwo = getVerticallyProjectedConvexHull(regionTwo);

      return true;
      //++++++JerryPratt: FixMe fix this and use this if you want it to be more accurate.
//      return doPolygonsIntersect(convexHullOne, convexHullTwo, epsilon);
   }

   public static BoundingBox2DReadOnly getVerticallyProjectedBoundingBox(PlanarRegion planarRegion)
   {
      BoundingBox3DReadOnly boundingBox3D = planarRegion.getBoundingBox3dInWorld();
      Point3DReadOnly maxPoint = boundingBox3D.getMaxPoint();
      Point3DReadOnly minPoint = boundingBox3D.getMinPoint();
      return new BoundingBox2D(minPoint.getX(), minPoint.getY(), maxPoint.getX(), maxPoint.getY());
   }

   public static ConvexPolygon2D getVerticallyProjectedConvexHull(PlanarRegion planarRegion)
   {
      return projectPolygonVertically(planarRegion.getTransformToWorld(), planarRegion.getConvexHull());
   }

   public static ConvexPolygon2D projectPolygonVertically(RigidBodyTransformReadOnly transformToWorld, ConvexPolygon2D polygonInLocalToProjectVertically)
   {
      List<? extends Point2DReadOnly> verticesToProject = polygonInLocalToProjectVertically.getPolygonVerticesView();

      ConvexPolygon2D projectedPolygon = new ConvexPolygon2D();

      for (Point2DReadOnly vertexToProject : verticesToProject)
      {
         Point3D pointToProjectInWorld = new Point3D(vertexToProject);
         transformToWorld.transform(pointToProjectInWorld);
         projectedPolygon.addVertex(pointToProjectInWorld.getX(), pointToProjectInWorld.getY());
      }

      return projectedPolygon;
   }

   //TODO: Should be more efficient way to do this check. And should be moved to Euclid Polygon Tools.
   public static boolean doPolygonsIntersect(ConvexPolygon2D polygonOne, ConvexPolygon2D polygonTwo, double epsilon)
   {
      if (polygonOne.getNumberOfVertices() == 2)
      {
         if (polygonTwo.getNumberOfVertices() == 2)
         {
            return EuclidGeometryTools.distanceBetweenTwoLineSegment3Ds(new Point3D(polygonOne.getVertex(0)), new Point3D(polygonOne.getVertex(1)), new Point3D(polygonTwo.getVertex(0)), new Point3D(polygonTwo.getVertex(1))) < epsilon;
         }
         else
         {
            Point2D[] intersections = EuclidGeometryPolygonTools.intersectionBetweenLineSegment2DAndConvexPolygon2D(polygonOne.getVertex(0), polygonOne.getVertex(1),
                                                                                                                    polygonTwo.getVertexBufferView(),
                                                                                                                    polygonTwo.getNumberOfVertices(),
                                                                                                                    polygonTwo.isClockwiseOrdered());
            return intersections != null;
         }
      }
      else if (polygonTwo.getNumberOfVertices() == 2)
      {
         Point2D[] intersections = EuclidGeometryPolygonTools.intersectionBetweenLineSegment2DAndConvexPolygon2D(polygonTwo.getVertex(0), polygonTwo.getVertex(1),
                                                                                                                 polygonOne.getVertexBufferView(),
                                                                                                                 polygonOne.getNumberOfVertices(),
                                                                                                                 polygonOne.isClockwiseOrdered());
         return intersections != null;
      }


      //TODO: Inefficient:
      ConvexPolygonTools convexPolygonTools = new ConvexPolygonTools();

      if (convexPolygonTools.computeIntersectionOfPolygons(polygonOne, polygonTwo, new ConvexPolygon2D()))
         return true;

      Point2DBasics point1ToPack = new Point2D();
      Point2DBasics point2ToPack = new Point2D();
      try
      {
         convexPolygonTools.computeMinimumDistancePoints(polygonOne, polygonTwo, point1ToPack, point2ToPack);
      }
      catch (Exception e)
      {
         System.err.println("polygonOne = " + polygonOne);
         System.err.println("polygonTwo = " + polygonTwo);
         e.printStackTrace();
      }

      return (point1ToPack.distance(point2ToPack) < epsilon);
   }

   private static ConvexPolygon2D createSmallRectangleFromLineSegment(ConvexPolygon2D linePolygon, double rectangleWidth)
   {
      List<? extends Point2DReadOnly> vertices = linePolygon.getPolygonVerticesView();
      Vector2D vector = new Vector2D();
      vector.set(vertices.get(1));
      vector.sub(vertices.get(0));
      vector.normalize();
      Vector2D toTheRight = EuclidGeometryTools.perpendicularVector2D(vector);
      toTheRight.scale(-1.0 * rectangleWidth);

      Point2D newPointA = new Point2D(vertices.get(1));
      newPointA.add(toTheRight);

      Point2D newPointB = new Point2D(vertices.get(0));
      newPointB.add(toTheRight);

      List<Point2DReadOnly> polygonPoints = new ArrayList<>();
      polygonPoints.add(vertices.get(0));
      polygonPoints.add(vertices.get(1));
      polygonPoints.add(newPointA);
      polygonPoints.add(newPointB);

      linePolygon = new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(polygonPoints));
      return linePolygon;
   }

   public static boolean isPointInWorldInsidePlanarRegion(PlanarRegion planarRegion, Point3DReadOnly pointInWorldToCheck)
   {
      return isPointInWorldInsidePlanarRegion(planarRegion, pointInWorldToCheck, 0.0);
   }

   public static boolean areBothPointsInsidePlanarRegion(Point2DReadOnly point1, Point2DReadOnly point2, PlanarRegion homeRegion)
   {
      if (!isPointInLocalInsidePlanarRegion(homeRegion, point1))
         return false;
      if (!isPointInLocalInsidePlanarRegion(homeRegion, point2))
         return false;

      return true;
   }

   /**
    * Finds and returns the closest point the the provided point on the planar regions.
    */
   public static Point3D projectPointToPlanes(Point3DReadOnly point, PlanarRegionsList regions)
   {
      double smallestDistance = Double.POSITIVE_INFINITY;
      Point3D closestPoint = null;

      for (PlanarRegion region : regions.getPlanarRegionsAsList())
      {
         Point3D intersection = closestPointOnPlane(point, region);
         double distance = intersection.distance(point);

         if (closestPoint == null || distance < smallestDistance)
         {
            smallestDistance = distance;
            closestPoint = intersection;
         }
      }

      return closestPoint;
   }

   /**
    * Projects the given point in the world onto the planar region, returning the closest point in the world on the region to
    * the provided point.
    */
   public static Point3D closestPointOnPlane(Point3DReadOnly pointInWorld, PlanarRegion region)
   {
      RigidBodyTransform regionToWorld = new RigidBodyTransform();
      region.getTransformToWorld(regionToWorld);

      Vector3D planeNormal = new Vector3D(0.0, 0.0, 1.0);
      planeNormal.applyTransform(regionToWorld);

      Point3D pointOnPlane = new Point3D();
      pointOnPlane.set(region.getConvexPolygon(0).getVertex(0));
      pointOnPlane.applyTransform(regionToWorld);

      Point3D intersectionWithPlane = EuclidGeometryTools.intersectionBetweenLine3DAndPlane3D(pointOnPlane, planeNormal, pointInWorld, planeNormal);
      if (intersectionWithPlane == null)
      {
         return null;
      }

      Point3D intersectionInPlaneFrame = new Point3D(intersectionWithPlane);
      intersectionInPlaneFrame.applyInverseTransform(regionToWorld);
      Point2D intersectionInPlaneFrame2D = new Point2D(intersectionInPlaneFrame);

      // checking convex hull here - might be better to check all polygons to avoid false positive
      ConvexPolygon2D convexHull = region.getConvexHull();
      if (!convexHull.isPointInside(intersectionInPlaneFrame2D))
      {
         convexHull.orthogonalProjection(intersectionInPlaneFrame2D);
         intersectionWithPlane.setToZero();
         intersectionWithPlane.set(intersectionInPlaneFrame2D);
         intersectionWithPlane.applyTransform(regionToWorld);
      }
      return intersectionWithPlane;
   }

   /**
    * Will return the intersection point between a line and a single planar region. If the line does
    * not intersect the region this method will return null.
    */
   public static Point3D intersectRegionWithLine(PlanarRegion region, Line3D projectionLineInWorld)
   {
      RigidBodyTransform regionToWorld = new RigidBodyTransform();
      region.getTransformToWorld(regionToWorld);

      Vector3DReadOnly planeNormal = new Vector3D(0.0, 0.0, 1.0);
      Point3DReadOnly pointOnPlane = new Point3D(region.getConvexPolygon(0).getVertex(0));

      Point3DBasics pointOnLineInLocal = new Point3D(projectionLineInWorld.getPoint());
      Vector3DBasics directionOfLineInLocal = new Vector3D(projectionLineInWorld.getDirection());

      pointOnLineInLocal.applyInverseTransform(regionToWorld);
      directionOfLineInLocal.applyInverseTransform(regionToWorld);

      Point3D intersectionWithPlaneInLocal = EuclidGeometryTools.intersectionBetweenLine3DAndPlane3D(pointOnPlane, planeNormal, pointOnLineInLocal,
                                                                                                     directionOfLineInLocal);
      if (intersectionWithPlaneInLocal == null)
      {
         return null;
      }

      if (region.isPointInside(intersectionWithPlaneInLocal.getX(), intersectionWithPlaneInLocal.getY()))
      {
         intersectionWithPlaneInLocal.applyTransform(regionToWorld);
         return intersectionWithPlaneInLocal;
      }

      return null;
   }

   public static ImmutablePair<Point3D, PlanarRegion> intersectRegionsWithRay(PlanarRegionsList regions, Point3DReadOnly rayStart, Vector3D rayDirection)
   {
      return intersectRegionsWithRay(regions.getPlanarRegionsAsList(), rayStart, rayDirection);
   }

   public static ImmutablePair<Point3D, PlanarRegion> intersectRegionsWithRay(List<PlanarRegion> regions, Point3DReadOnly rayStart, Vector3D rayDirection)
   {
      double smallestDistance = Double.POSITIVE_INFINITY;
      ImmutablePair<Point3D, PlanarRegion> closestIntersection = null;

      for (PlanarRegion region : regions)
      {
         Point3D intersection = intersectRegionWithRay(region, rayStart, rayDirection);
         if (intersection == null)
         {
            continue;
         }
         double distance = intersection.distance(rayStart);
         if (distance < smallestDistance)
         {
            smallestDistance = distance;
            closestIntersection = new ImmutablePair<>(intersection, region);
         }
      }

      return closestIntersection;
   }

   public static Point3D intersectRegionWithRay(PlanarRegion region, Point3DReadOnly rayStart, Vector3D rayDirection)
   {
      RigidBodyTransform regionToWorld = new RigidBodyTransform();
      region.getTransformToWorld(regionToWorld);

      Vector3D planeNormal = new Vector3D(0.0, 0.0, 1.0);
      planeNormal.applyTransform(regionToWorld);

      Point3D pointOnPlane = new Point3D();
      pointOnPlane.set(region.getConvexPolygon(0).getVertex(0));
      pointOnPlane.applyTransform(regionToWorld);

      Point3D intersectionWithPlane = EuclidGeometryTools.intersectionBetweenLine3DAndPlane3D(pointOnPlane, planeNormal, rayStart, rayDirection);
      if (intersectionWithPlane == null)
      {
         return null;
      }

      Point3D intersectionInPlaneFrame = new Point3D(intersectionWithPlane);
      intersectionInPlaneFrame.applyInverseTransform(regionToWorld);
      // checking convex hull here - might be better to check all polygons to avoid false positive
      if (!region.getConvexHull().isPointInside(intersectionInPlaneFrame.getX(), intersectionInPlaneFrame.getY()))
      {
         return null;
      }

      Vector3D rayToIntersection = new Vector3D();
      rayToIntersection.sub(intersectionWithPlane, rayStart);
      if (rayToIntersection.dot(rayDirection) < 0.0)
      {
         return null;
      }

      return intersectionWithPlane;
   }

   /**
    * Projects the given point onto a planar region from the list. The projection is done along the
    * z axis in world frame and if there is multiple regions that the point can be projected onto,
    * the highest intersection point will be returned.
    * <p>
    * Will return null if the is no planar region above or below the point.
    */
   public static Point3D projectPointToPlanesVertically(Point3DReadOnly pointInWorld, PlanarRegionsList regions)
   {
      if (regions == null)
         return projectPointToPlanesVertically(pointInWorld, (List<PlanarRegion>) null);
      else
         return projectPointToPlanesVertically(pointInWorld, regions.getPlanarRegionsAsList());
   }

   /**
    * Projects the given point onto a planar region from the list. The projection is done along the
    * z axis in world frame and if there is multiple regions that the point can be projected onto,
    * the highest intersection point will be returned.
    * <p>
    * Will return null if the is no planar region above or below the point.
    */
   public static Point3D projectPointToPlanesVertically(Point3DReadOnly pointInWorld, List<PlanarRegion> regions)
   {
      Point3D highestIntersection = null;

      Line3D verticalLine = new Line3D();
      verticalLine.set(pointInWorld, new Vector3D(0.0, 0.0, 1.0));

      if (regions == null)
         return null;

      for (PlanarRegion region : regions)
      {
         Point3D intersection = intersectRegionWithLine(region, verticalLine);
         if (intersection == null)
            continue;

         if (highestIntersection == null)
         {
            highestIntersection = new Point3D(pointInWorld);
            highestIntersection.setZ(Double.NEGATIVE_INFINITY);
         }

         double height = intersection.getZ();

         if (highestIntersection.getZ() < height)
         {
            highestIntersection.setZ(height);
         }
      }

      if (highestIntersection != null && Double.isInfinite(highestIntersection.getZ()))
         return null;

      return highestIntersection;
   }

   public static boolean isPointOnRegion(PlanarRegion region, Point3D point, double epsilon)
   {
      Point3D closestPoint = closestPointOnPlane(point, region);
      return closestPoint.epsilonEquals(point, epsilon);
   }
}
