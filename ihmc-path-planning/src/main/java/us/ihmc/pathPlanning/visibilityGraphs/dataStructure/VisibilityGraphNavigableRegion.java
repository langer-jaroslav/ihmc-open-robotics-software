package us.ihmc.pathPlanning.visibilityGraphs.dataStructure;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.pathPlanning.visibilityGraphs.clusterManagement.Cluster;
import us.ihmc.pathPlanning.visibilityGraphs.tools.VisibilityTools;
import us.ihmc.robotics.geometry.PlanarRegion;

public class VisibilityGraphNavigableRegion
{
   private static final int INNER_CONNECTIONS_EVERY_N_POINTS = 1;

   private final NavigableRegion navigableRegion;

   private final ArrayList<VisibilityGraphNode> homeRegionNodes = new ArrayList<VisibilityGraphNode>();
   private final ArrayList<ArrayList<VisibilityGraphNode>> obstacleNavigableNodes = new ArrayList<ArrayList<VisibilityGraphNode>>();

   private final ArrayList<VisibilityGraphEdge> innerRegionEdges = new ArrayList<VisibilityGraphEdge>();

   public VisibilityGraphNavigableRegion(NavigableRegion navigableRegion)
   {
      this.navigableRegion = navigableRegion;
   }

   public NavigableRegion getNavigableRegion()
   {
      return navigableRegion;
   }

   public ArrayList<VisibilityGraphNode> getHomeRegionNodes()
   {
      return homeRegionNodes;
   }

   public ArrayList<ArrayList<VisibilityGraphNode>> getObstacleNavigableNodes()
   {
      return obstacleNavigableNodes;
   }

   public ArrayList<VisibilityGraphEdge> getAllEdges()
   {
      return innerRegionEdges;
   }

   public void createGraphAroundClusterRings()
   {
      PlanarRegion homePlanarRegion = navigableRegion.getHomePlanarRegion();
      Cluster homeRegionCluster = navigableRegion.getHomeRegionCluster();
      List<Cluster> allClusters = navigableRegion.getAllClusters();
      List<Cluster> obstacleClusters = navigableRegion.getObstacleClusters();
      int mapId = navigableRegion.getMapId();

      addClusterSelfVisibilityAroundRing(homeRegionCluster, homePlanarRegion, allClusters, mapId, homeRegionNodes, innerRegionEdges);

      obstacleNavigableNodes.clear();
      for (int i = 0; i < obstacleClusters.size(); i++)
      {
         obstacleNavigableNodes.add(new ArrayList<>());
      }

      for (int i = 0; i < obstacleClusters.size(); i++)
      {
         Cluster obstacleCluster = obstacleClusters.get(i);
         ArrayList<VisibilityGraphNode> obstacleNodes = obstacleNavigableNodes.get(i);
         addClusterSelfVisibilityAroundRing(obstacleCluster, homePlanarRegion, allClusters, mapId, obstacleNodes, innerRegionEdges);
      }
   }

   public void createGraphBetweenInnerClusterRings()
   {
      int regionId = navigableRegion.getMapId();
      PlanarRegion homeRegion = navigableRegion.getHomePlanarRegion();

      List<Cluster> allClusters = navigableRegion.getAllClusters();

      addClusterSelfVisibility(homeRegion, allClusters, regionId, homeRegionNodes, innerRegionEdges);

      for (int targetIndex = 0; targetIndex < obstacleNavigableNodes.size(); targetIndex++)
      {
         ArrayList<VisibilityGraphNode> targetNodes = obstacleNavigableNodes.get(targetIndex);

         addCrossClusterVisibility(homeRegionNodes, targetNodes, allClusters, regionId, innerRegionEdges);
      }

      for (int sourceIndex = 0; sourceIndex < obstacleNavigableNodes.size(); sourceIndex++)
      {
         ArrayList<VisibilityGraphNode> sourceNodes = obstacleNavigableNodes.get(sourceIndex);

         for (int targetIndex = sourceIndex + 1; targetIndex < obstacleNavigableNodes.size(); targetIndex++)
         {
            ArrayList<VisibilityGraphNode> targetNodes = obstacleNavigableNodes.get(targetIndex);

            addCrossClusterVisibility(sourceNodes, targetNodes, allClusters, regionId, innerRegionEdges);
         }
      }

   }

   public static void addClusterSelfVisibilityAroundRing(Cluster clusterToBuildMapOf, PlanarRegion homeRegion, List<Cluster> allClusters, int mapId,
                                                         ArrayList<VisibilityGraphNode> nodesToPack, ArrayList<VisibilityGraphEdge> edgesToPack)
   {
      List<? extends Point2DReadOnly> navigableExtrusionPoints = clusterToBuildMapOf.getNavigableExtrusionsInLocal();
      boolean[] arePointsActuallyNavigable = VisibilityTools.checkIfPointsInsidePlanarRegionAndOutsideNonavigableZones(homeRegion, allClusters,
                                                                                                                       navigableExtrusionPoints);

      VisibilityGraphNode sourceNode = null;

      // Going through all of the possible combinations of two points for finding connections
      for (int sourceIndex = 0; sourceIndex < navigableExtrusionPoints.size(); sourceIndex++)
      {
         if (!arePointsActuallyNavigable[sourceIndex])
            continue; // Both source and target have to be navigable for the connection to be valid

         Point2DReadOnly sourcePointInLocal = navigableExtrusionPoints.get(sourceIndex);

         if (sourceNode == null)
         {
            Point3D sourcePointInWorld = new Point3D(sourcePointInLocal);
            homeRegion.transformFromLocalToWorld(sourcePointInWorld);
            sourceNode = new VisibilityGraphNode(sourcePointInWorld, sourcePointInLocal, mapId);
         }
         nodesToPack.add(sourceNode);

         // Starting from after the next vertex of the source as we already added all the edges as connections
         int targetIndex = (sourceIndex + 1) % navigableExtrusionPoints.size();
         {
            if (!arePointsActuallyNavigable[targetIndex])
               continue; // Both source and target have to be navigable for the connection to be valid

            Point2DReadOnly targetPointInLocal = navigableExtrusionPoints.get(targetIndex);

            // Finally run the expensive test to verify if the target can be seen from the source.
            if (VisibilityTools.isPointVisibleForStaticMaps(allClusters, sourcePointInLocal, targetPointInLocal))
            {
               Point3D targetPointInWorld = new Point3D(targetPointInLocal);
               homeRegion.transformFromLocalToWorld(targetPointInWorld);
               VisibilityGraphNode targetNode = new VisibilityGraphNode(targetPointInWorld, targetPointInLocal, mapId);

               VisibilityGraphEdge edge = new VisibilityGraphEdge(sourceNode, targetNode);

               sourceNode.addEdge(edge);
               targetNode.addEdge(edge);

               edgesToPack.add(edge);

               sourceNode = targetNode;
            }
         }
      }
   }

   public static void addClusterSelfVisibility(PlanarRegion homeRegion, List<Cluster> allClusters, int mapId, ArrayList<VisibilityGraphNode> homeRegionNodes,
                                               ArrayList<VisibilityGraphEdge> edgesToPack)
   {
      // Going through all of the possible combinations of two points for finding connections
      for (int sourceIndex = 0; sourceIndex < homeRegionNodes.size(); sourceIndex++)
      {
         VisibilityGraphNode sourceNode = homeRegionNodes.get(sourceIndex);

         // Starting from after the next vertex of the source as we already added all the edges around the ring as connections.
         // And then end before the last one
         for (int targetIndex = sourceIndex + 2; targetIndex < homeRegionNodes.size(); targetIndex = targetIndex + INNER_CONNECTIONS_EVERY_N_POINTS)
         {
            if ((sourceIndex == 0) && (targetIndex == homeRegionNodes.size() - 1))
               continue;
            VisibilityGraphNode targetNode = homeRegionNodes.get(targetIndex);

            // Finally run the expensive test to verify if the target can be seen from the source.
            if (VisibilityTools.isPointVisibleForStaticMaps(allClusters, sourceNode.getPoint2DInLocal(), targetNode.getPoint2DInLocal()))
            {
               VisibilityGraphEdge edge = new VisibilityGraphEdge(sourceNode, targetNode);
               edgesToPack.add(edge);

               sourceNode.addEdge(edge);
               targetNode.addEdge(edge);
            }
         }
      }
   }

   public static void addCrossClusterVisibility(ArrayList<VisibilityGraphNode> sourceNodes, ArrayList<VisibilityGraphNode> targetNodes,
                                                List<Cluster> allClusters, int mapId, ArrayList<VisibilityGraphEdge> edgesToPack)
   {
      for (int sourceIndex = 0; sourceIndex < sourceNodes.size(); sourceIndex++)
      {
         VisibilityGraphNode sourceNode = sourceNodes.get(sourceIndex);

         for (int targetIndex = 0; targetIndex < targetNodes.size(); targetIndex++)
         {
            VisibilityGraphNode targetNode = targetNodes.get(targetIndex);

            if (VisibilityTools.isPointVisibleForStaticMaps(allClusters, sourceNode.getPoint2DInLocal(), targetNode.getPoint2DInLocal()))
            {

               VisibilityGraphEdge edge = new VisibilityGraphEdge(sourceNode, targetNode);
               edgesToPack.add(edge);

               sourceNode.addEdge(edge);
               targetNode.addEdge(edge);
            }
         }
      }
   }

   public int getMapId()
   {
      return navigableRegion.getMapId();
   }

}
