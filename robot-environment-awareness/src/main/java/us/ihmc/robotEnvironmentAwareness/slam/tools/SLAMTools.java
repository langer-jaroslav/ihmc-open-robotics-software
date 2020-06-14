package us.ihmc.robotEnvironmentAwareness.slam.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.mutable.MutableDouble;

import gnu.trove.list.array.TIntArrayList;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.Plane3D;
import us.ihmc.euclid.geometry.interfaces.Plane3DBasics;
import us.ihmc.euclid.geometry.interfaces.Plane3DReadOnly;
import us.ihmc.euclid.geometry.interfaces.Vertex3DSupplier;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.transform.interfaces.RigidBodyTransformReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DReadOnly;
import us.ihmc.jOctoMap.iterators.OcTreeIterable;
import us.ihmc.jOctoMap.iterators.OcTreeIteratorFactory;
import us.ihmc.jOctoMap.key.OcTreeKey;
import us.ihmc.jOctoMap.node.NormalOcTreeNode;
import us.ihmc.jOctoMap.normalEstimation.NormalEstimationParameters;
import us.ihmc.jOctoMap.ocTree.NormalOcTree;
import us.ihmc.jOctoMap.pointCloud.PointCloud;
import us.ihmc.jOctoMap.pointCloud.Scan;
import us.ihmc.jOctoMap.pointCloud.ScanCollection;
import us.ihmc.jOctoMap.tools.OcTreeNearestNeighborTools;
import us.ihmc.log.LogTools;
import us.ihmc.robotEnvironmentAwareness.planarRegion.PlanarRegionSegmentationCalculator;
import us.ihmc.robotEnvironmentAwareness.planarRegion.PlanarRegionSegmentationParameters;
import us.ihmc.robotEnvironmentAwareness.planarRegion.PlanarRegionSegmentationRawData;
import us.ihmc.robotEnvironmentAwareness.planarRegion.SurfaceNormalFilterParameters;
import us.ihmc.robotEnvironmentAwareness.slam.SLAMFrame;

public class SLAMTools
{
   public static Scan toScan(Point3DReadOnly[] points, Tuple3DReadOnly sensorPosition)
   {
      PointCloud pointCloud = new PointCloud();

      for (int i = 0; i < points.length; i++)
      {
         double x = points[i].getX();
         double y = points[i].getY();
         double z = points[i].getZ();
         pointCloud.add(x, y, z);
      }
      return new Scan(new Point3D(sensorPosition), pointCloud);
   }

   public static Scan toScan(Point3DReadOnly[] points, Point3DReadOnly[] pointsToSensorFrame, RigidBodyTransformReadOnly sensorPose, NormalOcTree map,
                             double windowMargin)
   {
      if (points.length != pointsToSensorFrame.length)
      {
         LogTools.error("size of point cloud are different " + points.length + " " + pointsToSensorFrame.length);
         return null;
      }

      ConvexPolygon2D windowForMap = SLAMTools.computeMapConvexHullInSensorFrame(map, sensorPose);
      PointCloud pointCloud = new PointCloud();

      for (int i = 0; i < points.length; i++)
      {
         if (windowForMap.isPointInside(pointsToSensorFrame[i].getX(), pointsToSensorFrame[i].getY(), -windowMargin))
         {
            double x = points[i].getX();
            double y = points[i].getY();
            double z = points[i].getZ();
            pointCloud.add(x, y, z);
         }
      }
      return new Scan(new Point3D(sensorPose.getTranslation()), pointCloud);
   }

   public static Point3D[] createConvertedPointsToSensorPose(RigidBodyTransformReadOnly sensorPose, Point3DReadOnly[] pointCloud)
   {
      Point3D[] convertedPoints = new Point3D[pointCloud.length];
      for (int i = 0; i < pointCloud.length; i++)
      {
         convertedPoints[i] = createConvertedPointToSensorPose(sensorPose, pointCloud[i]);
      }

      return convertedPoints;
   }
   
   public static Plane3D createConvertedSurfaceElementToSensorPose(RigidBodyTransformReadOnly sensorPose, Plane3DReadOnly surfaceElement)
   {
      Plane3D convertedSurfel = new Plane3D();
      sensorPose.inverseTransform(surfaceElement.getPoint(), convertedSurfel.getPoint());
      sensorPose.inverseTransform(surfaceElement.getNormal(), convertedSurfel.getNormal());

      return convertedSurfel;
   }

   public static Point3D createConvertedPointToSensorPose(RigidBodyTransformReadOnly sensorPose, Point3DReadOnly point)
   {
      Point3D convertedPoint = new Point3D();
      sensorPose.inverseTransform(point, convertedPoint);
      return convertedPoint;
   }

   public static Point3D[] createConvertedPointsToWorld(RigidBodyTransformReadOnly otherSensorPose, Point3DReadOnly[] pointCloudToSensorPose)
   {
      Point3D[] pointCloudToWorld = new Point3D[pointCloudToSensorPose.length];
      for (int i = 0; i < pointCloudToWorld.length; i++)
      {
         pointCloudToWorld[i] = new Point3D(pointCloudToSensorPose[i]);
         otherSensorPose.transform(pointCloudToWorld[i]);
      }

      return pointCloudToWorld;
   }

   public static NormalOcTree computeOctreeData(Point3DReadOnly[] pointCloud, Tuple3DReadOnly sensorPosition, double octreeResolution)
   {
      ScanCollection scanCollection = new ScanCollection();
      int numberOfPoints = pointCloud.length;

      scanCollection.setSubSampleSize(numberOfPoints);
      scanCollection.addScan(toScan(pointCloud, sensorPosition));

      NormalOcTree octree = new NormalOcTree(octreeResolution);

      octree.insertScanCollection(scanCollection, false);

      octree.enableParallelComputationForNormals(true);

      NormalEstimationParameters normalEstimationParameters = new NormalEstimationParameters();
      normalEstimationParameters.setNumberOfIterations(7);
      octree.setNormalEstimationParameters(normalEstimationParameters);

      octree.updateNormals();
      return octree;
   }

   public static NormalOcTree computeOctreeData(List<Point3DReadOnly[]> pointCloudMap, List<RigidBodyTransformReadOnly> sensorPoses, double octreeResolution)
   {
      ScanCollection scanCollection = new ScanCollection();
      for (int i = 0; i < pointCloudMap.size(); i++)
      {
         int numberOfPoints = pointCloudMap.get(i).length;

         scanCollection.setSubSampleSize(numberOfPoints);
         scanCollection.addScan(toScan(pointCloudMap.get(i), sensorPoses.get(i).getTranslation()));
      }

      NormalOcTree octree = new NormalOcTree(octreeResolution);

      octree.insertScanCollection(scanCollection, false);

      octree.enableParallelComputationForNormals(true);

      NormalEstimationParameters normalEstimationParameters = new NormalEstimationParameters();
      normalEstimationParameters.setNumberOfIterations(7);
      octree.setNormalEstimationParameters(normalEstimationParameters);

      octree.updateNormals();
      return octree;
   }

   public static List<PlanarRegionSegmentationRawData> computePlanarRegionRawData(List<Point3DReadOnly[]> pointCloudMap,
                                                                                  List<RigidBodyTransformReadOnly> sensorPoses, double octreeResolution,
                                                                                  PlanarRegionSegmentationParameters planarRegionSegmentationParameters)
   {
      // TODO: FB-348: Surface normal filter in NormalOctree.
      NormalOcTree referenceOctree = computeOctreeData(pointCloudMap, sensorPoses, octreeResolution);

      PlanarRegionSegmentationCalculator segmentationCalculator = new PlanarRegionSegmentationCalculator();

      SurfaceNormalFilterParameters surfaceNormalFilterParameters = new SurfaceNormalFilterParameters();
      surfaceNormalFilterParameters.setUseSurfaceNormalFilter(false);

      segmentationCalculator.setParameters(planarRegionSegmentationParameters);
      segmentationCalculator.setSurfaceNormalFilterParameters(surfaceNormalFilterParameters);
      segmentationCalculator.setSensorPosition(new Point3D(0.0, 0.0, 20.0)); //TODO: work this for every poses.

      segmentationCalculator.compute(referenceOctree.getRoot());

      List<PlanarRegionSegmentationRawData> rawData = segmentationCalculator.getSegmentationRawData();

      return rawData;
   }

   public static List<PlanarRegionSegmentationRawData> computePlanarRegionRawData(Point3DReadOnly[] pointCloud, Tuple3DReadOnly sensorPosition,
                                                                                  double octreeResolution,
                                                                                  PlanarRegionSegmentationParameters planarRegionSegmentationParameters)
   {
      return computePlanarRegionRawData(pointCloud, sensorPosition, octreeResolution, planarRegionSegmentationParameters, true);
   }

   public static List<PlanarRegionSegmentationRawData> computePlanarRegionRawData(Point3DReadOnly[] pointCloud, Tuple3DReadOnly sensorPosition,
                                                                                  double octreeResolution,
                                                                                  PlanarRegionSegmentationParameters planarRegionSegmentationParameters,
                                                                                  boolean useSurfaceNormalFilter)
   {
      NormalOcTree referenceOctree = computeOctreeData(pointCloud, sensorPosition, octreeResolution);

      PlanarRegionSegmentationCalculator segmentationCalculator = new PlanarRegionSegmentationCalculator();

      SurfaceNormalFilterParameters surfaceNormalFilterParameters = new SurfaceNormalFilterParameters();
      surfaceNormalFilterParameters.setUseSurfaceNormalFilter(useSurfaceNormalFilter);

      segmentationCalculator.setParameters(planarRegionSegmentationParameters);
      segmentationCalculator.setSurfaceNormalFilterParameters(surfaceNormalFilterParameters);
      segmentationCalculator.setSensorPosition(sensorPosition);

      segmentationCalculator.compute(referenceOctree.getRoot());

      List<PlanarRegionSegmentationRawData> rawData = segmentationCalculator.getSegmentationRawData();

      return rawData;
   }

   public static double computeDistanceToNormalOctree(NormalOcTree octree, Point3DReadOnly point)
   {
      OcTreeKey occupiedKey = octree.coordinateToKey(point);
      OcTreeKey nearestKey = new OcTreeKey();
      OcTreeNearestNeighborTools.findNearestNeighbor(octree.getRoot(), octree.keyToCoordinate(occupiedKey), nearestKey);

      MutableDouble nearestHitDistanceSquared = new MutableDouble(Double.POSITIVE_INFINITY);

      double resolution = octree.getResolution();
      OcTreeNearestNeighborTools.findRadiusNeighbors(octree.getRoot(), octree.keyToCoordinate(nearestKey), resolution*1.5, node ->
      {
         nearestHitDistanceSquared.setValue(Math.min(nearestHitDistanceSquared.doubleValue(), node.getHitLocationCopy().distanceSquared(point)));
      });

      return Math.sqrt(nearestHitDistanceSquared.getValue());
   }
   
      public static void computeCorrespondingPointToPoint(NormalOcTree octree, Point3DReadOnly point, Point3D correspondingPointToPack)
   {
      OcTreeKey occupiedKey = octree.coordinateToKey(point);
      OcTreeKey nearestKey = new OcTreeKey();
      OcTreeNearestNeighborTools.findNearestNeighbor(octree.getRoot(), octree.keyToCoordinate(occupiedKey), nearestKey);

      double resolution = octree.getResolution();
      OcTreeNearestNeighborTools.findRadiusNeighbors(octree.getRoot(), octree.keyToCoordinate(nearestKey), resolution, node ->
      {
         correspondingPointToPack.set(node.getHitLocationCopy());
      });
   }

   public static double computePerpendicularDistanceToNormalOctree(NormalOcTree octree, Point3DReadOnly point)
   {
      OcTreeKey occupiedKey = octree.coordinateToKey(point);
      OcTreeKey nearestKey = new OcTreeKey();
      OcTreeNearestNeighborTools.findNearestNeighbor(octree.getRoot(), octree.keyToCoordinate(occupiedKey), nearestKey);

      MutableDouble nearestHitDistanceSquared = new MutableDouble(Double.POSITIVE_INFINITY);

      double resolution = octree.getResolution();
      OcTreeNearestNeighborTools.findRadiusNeighbors(octree.getRoot(), octree.keyToCoordinate(nearestKey), resolution*2, node ->
      {
         nearestHitDistanceSquared.setValue(Math.min(nearestHitDistanceSquared.doubleValue(),
                                                     EuclidGeometryTools.distanceFromPoint3DToPlane3D(point, node.getHitLocationCopy(), node.getNormalCopy())));
      });

      return nearestHitDistanceSquared.getValue();
   }

   public static double computeSurfaceElementDistanceToNormalOctree(NormalOcTree octree, Plane3DBasics surfel)
   {
      OcTreeKey occupiedKey = octree.coordinateToKey(surfel.getPoint());
      OcTreeKey nearestKey = new OcTreeKey();
      OcTreeNearestNeighborTools.findNearestNeighbor(octree.getRoot(), octree.keyToCoordinate(occupiedKey), nearestKey);

      MutableDouble nearestHitDistanceSquared = new MutableDouble(Double.POSITIVE_INFINITY);

      double resolution = octree.getResolution();
      OcTreeNearestNeighborTools.findRadiusNeighbors(octree.getRoot(), octree.keyToCoordinate(nearestKey), resolution, node ->
      {
         double distanceToSurfel = EuclidGeometryTools.distanceFromPoint3DToPlane3D(surfel.getPoint(), node.getHitLocationCopy(), node.getNormalCopy());
         double distanceToMap = EuclidGeometryTools.distanceFromPoint3DToPlane3D(node.getHitLocationCopy(), surfel.getPoint(), surfel.getNormal());
         double distance = Math.max(distanceToSurfel, distanceToMap);
         nearestHitDistanceSquared.setValue(Math.min(nearestHitDistanceSquared.doubleValue(), distance));
      });

      return nearestHitDistanceSquared.getValue();
   }
   
   public static double computeSurfaceElementBoundedDistanceToNormalOctree(NormalOcTree octree, Plane3DBasics surfel)
   {
      OcTreeKey occupiedKey = octree.coordinateToKey(surfel.getPoint());
      OcTreeKey nearestKey = new OcTreeKey();
      OcTreeNearestNeighborTools.findNearestNeighbor(octree.getRoot(), octree.keyToCoordinate(occupiedKey), nearestKey);

      MutableDouble nearestHitDistanceSquared = new MutableDouble(Double.POSITIVE_INFINITY);

      double resolution = octree.getResolution();
      double linearDistanceThreshold = resolution;
      double perpendicularDistanceThreshold = resolution;
      OcTreeNearestNeighborTools.findRadiusNeighbors(octree.getRoot(), octree.keyToCoordinate(nearestKey), resolution, node ->
      {
         double linearDistance = surfel.getPoint().distance(node.getHitLocationCopy());         
         double distanceToSurfel = EuclidGeometryTools.distanceFromPoint3DToPlane3D(surfel.getPoint(), node.getHitLocationCopy(), node.getNormalCopy());

         double distance = linearDistance;
         
         if(linearDistance < linearDistanceThreshold && distanceToSurfel < perpendicularDistanceThreshold)
         {
            distance = distanceToSurfel;
         }
         
         nearestHitDistanceSquared.setValue(Math.min(nearestHitDistanceSquared.doubleValue(), distance));
      });

      return nearestHitDistanceSquared.getValue();
   }


   /**
    * Computes the convex hull of all the {@code mapOctree} nodes in the sensor frame (z-axis is
    * depth).
    */
   public static ConvexPolygon2D computeMapConvexHullInSensorFrame(NormalOcTree mapOctree, RigidBodyTransformReadOnly sensorPose)
   {
      List<Point3D> vertex = new ArrayList<>();

      OcTreeIterable<NormalOcTreeNode> iterable = OcTreeIteratorFactory.createIterable(mapOctree.getRoot());
      // TODO Consider using a bounding box as follows:
      //      OcTreeIterable<NormalOcTreeNode> iterable = OcTreeIteratorFactory.createLeafBoundingBoxIteratable(octree.getRoot(), boundingBox);

      for (NormalOcTreeNode node : iterable)
      {
         Point3D hitLocation = node.getHitLocationCopy();
         sensorPose.inverseTransform(hitLocation);
         vertex.add(hitLocation);
      }
      Vertex3DSupplier supplier = Vertex3DSupplier.asVertex3DSupplier(vertex);
      ConvexPolygon2D windowForMap = new ConvexPolygon2D(supplier);

      return windowForMap;
   }

   /**
    * Collects the points in the {@code frame} that overlap with the {@code mapOctree} from the sensor
    * perspective.
    * <p>
    * The points from the new frame that overlap with the map are called <i>source points</i>.
    * </p>
    * 
    * @param frame                       single frame previously measured.
    * @param mapOctree                   the octree used to record frames over-time and serving as a
    *                                    map.
    * @param desiredNumberOfSourcePoints the number of source points required. If the actual number of
    *                                    source points found is less than
    *                                    {@code desiredNumberOfSourcePoints}, then this method returns
    *                                    {@code null}.
    * @param minimumOverlapRatio         minimum ratio of the actual number of source points over the
    *                                    frame size. If the actual ratio is under, then this method
    *                                    returns {@code null}.
    * @param windowMargin                tolerance used to shrink the map when testing if a point is
    *                                    overlapping.
    * @return the source points or {@code null} if {@code frame} should be thrown away.
    */
   public static Point3D[] createSourcePointsToSensorPose(SLAMFrame frame, NormalOcTree mapOctree, int desiredNumberOfSourcePoints, double minimumOverlapRatio,
                                                          double windowMargin)
   {
      ConvexPolygon2D windowForMap = computeMapConvexHullInSensorFrame(mapOctree, frame.getInitialSensorPoseToWorld());

      Point3DReadOnly[] newPointCloudToSensorPose = frame.getOriginalPointCloudToSensorPose();
      boolean[] isInPreviousView = new boolean[newPointCloudToSensorPose.length];
      int numberOfPointsInWindow = 0;
      for (int i = 0; i < newPointCloudToSensorPose.length; i++)
      {
         Point3DReadOnly point = newPointCloudToSensorPose[i];
         isInPreviousView[i] = false;
         if (windowForMap.isPointInside(point.getX(), point.getY(), -windowMargin))
         {
            isInPreviousView[i] = true;
            numberOfPointsInWindow++;
         }
      }

      Point3D[] pointsInPreviousWindow = new Point3D[numberOfPointsInWindow];
      int indexOfPointsInWindow = 0;
      for (int i = 0; i < newPointCloudToSensorPose.length; i++)
      {
         if (isInPreviousView[i])
         {
            pointsInPreviousWindow[indexOfPointsInWindow] = new Point3D(newPointCloudToSensorPose[i]);
            indexOfPointsInWindow++;
         }
      }

      double overlappedRatio = (double) numberOfPointsInWindow / newPointCloudToSensorPose.length;
      if (overlappedRatio < minimumOverlapRatio)
      {
         return null;
      }
      if (numberOfPointsInWindow < desiredNumberOfSourcePoints)
      {
         return null;
      }

      TIntArrayList indexOfSourcePoints = new TIntArrayList();
      int indexOfSourcePoint = 0;
      Point3D[] sourcePoints = new Point3D[desiredNumberOfSourcePoints];
      Random randomSelector = new Random(0612L);

      while (indexOfSourcePoints.size() != desiredNumberOfSourcePoints)
      {
         int selectedIndex = randomSelector.nextInt(pointsInPreviousWindow.length);
         if (!indexOfSourcePoints.contains(selectedIndex))
         {
            Point3D selectedPoint = pointsInPreviousWindow[selectedIndex];
            sourcePoints[indexOfSourcePoint] = selectedPoint;
            indexOfSourcePoint++;
            indexOfSourcePoints.add(selectedIndex);
         }
      }

      return sourcePoints;
   }

   public static int countNumberOfInliers(NormalOcTree octree, RigidBodyTransformReadOnly sensorPoseToWorld, Point3DReadOnly[] sourcePointsToSensor)
   {
      int numberOfInliers = 0;
      Point3D newSourcePointToWorld = new Point3D();
      for (Point3DReadOnly sourcePoint : sourcePointsToSensor)
      {
         newSourcePointToWorld.set(sourcePoint);
         sensorPoseToWorld.transform(newSourcePointToWorld);

         double distance = SLAMTools.computeDistanceToNormalOctree(octree, newSourcePointToWorld);

         if (distance >= 0)
         {
            if (distance < octree.getResolution())
            {
               numberOfInliers++;
            }
         }
      }
      return numberOfInliers;
   }
}
