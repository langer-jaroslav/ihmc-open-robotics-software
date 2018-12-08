package us.ihmc.pathPlanning.visibilityGraphs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.interfaces.Vertex2DSupplier;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.Connection;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.ConnectionPoint3D;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.InterRegionVisibilityMap;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.NavigableRegion;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityGraphEdge;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityGraphNavigableRegion;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityGraphNode;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityMap;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityMapSolution;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityMapWithNavigableRegion;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.InterRegionConnectionFilter;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.NavigableExtrusionDistanceCalculator;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.PlanarRegionFilter;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.VisibilityGraphsParameters;
import us.ihmc.robotics.geometry.PlanarRegion;

public class VisibilityGraphTest
{
   private static final double EPSILON = 1e-10;

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testVisibilityGraphJustOneSquare()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();
      List<PlanarRegion> planarRegions = new ArrayList<>();

      RigidBodyTransform transform = new RigidBodyTransform();
      Point2D pointA = new Point2D(10.0 - 0.01, -0.01);
      Point2D pointB = new Point2D(10.0 - 0.01, 1.01);
      Point2D pointC = new Point2D(10.0 + 1.01, 1.01);
      Point2D pointD = new Point2D(10.0 + 1.01, -0.01);

      transform.setTranslation(-10.0, 0.0, 0.0);
      ConvexPolygon2D polygon0_0 = new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(pointA, pointB, pointC, pointD));

      PlanarRegion planarRegion0 = new PlanarRegion(transform, polygon0_0);
      planarRegion0.setRegionId(97);
      planarRegions.add(planarRegion0);

      NavigableRegions navigableRegions = new NavigableRegions(parameters, planarRegions);
      navigableRegions.createNavigableRegions();

      List<NavigableRegion> naviableRegionsList = navigableRegions.getNaviableRegionsList();
      assertEquals(1, naviableRegionsList.size());

      NavigableRegion navigableRegion = naviableRegionsList.get(0);
      assertEquals(97, navigableRegion.getMapId());

      InterRegionConnectionFilter filter = new InterRegionConnectionFilter()
      {
         @Override
         public boolean isConnectionValid(ConnectionPoint3D source, ConnectionPoint3D target)
         {
            double distance = source.distance(target);
            return distance < 0.1;
         }

         @Override
         public double getMaximumInterRegionConnetionDistance()
         {
            return 0.1;
         }
      };
      VisibilityGraph visibilityGraph = new VisibilityGraph(navigableRegions, filter);

      ArrayList<VisibilityGraphNavigableRegion> visibilityGraphNavigableRegions = visibilityGraph.getVisibilityGraphNavigableRegions();
      assertEquals(1, visibilityGraphNavigableRegions.size());
      VisibilityGraphNavigableRegion visibilityGraphNavigableRegion = visibilityGraphNavigableRegions.get(0);

      ArrayList<VisibilityGraphEdge> navigableRegionEdges = visibilityGraphNavigableRegion.getAllEdges();
      assertEquals(28, navigableRegionEdges.size());
      ArrayList<VisibilityGraphNode> homeRegionNodes = visibilityGraphNavigableRegion.getHomeRegionNodes();
      assertEquals(8, homeRegionNodes.size());

      ArrayList<VisibilityGraphEdge> crossRegionEdges = visibilityGraph.getCrossRegionEdges();
      assertEquals(0, crossRegionEdges.size());

      ConnectionPoint3D connectionA = new ConnectionPoint3D(0.0, 1.0, 0.0, 0);
      ConnectionPoint3D connectionAB = new ConnectionPoint3D(0.5, 1.0, 0.0, 0);
      ConnectionPoint3D connectionB = new ConnectionPoint3D(1.0, 1.0, 0.0, 0);
      ConnectionPoint3D connectionBC = new ConnectionPoint3D(1.0, 0.5, 0.0, 0);
      ConnectionPoint3D connectionC = new ConnectionPoint3D(1.0, 0.0, 0.0, 0);
      ConnectionPoint3D connectionCD = new ConnectionPoint3D(0.5, 0.0, 0.0, 0);
      ConnectionPoint3D connectionD = new ConnectionPoint3D(0.0, 0.0, 0.0, 0);
      ConnectionPoint3D connectionDA = new ConnectionPoint3D(0.0, 0.5, 0.0, 0);

      assertTrue(edgeListContains(navigableRegionEdges, connectionA, connectionAB));
      assertTrue(edgeListContains(navigableRegionEdges, connectionA, connectionDA));
      assertTrue(edgeListContains(navigableRegionEdges, connectionD, connectionBC));

      VisibilityMapSolution visibilityMapSolution = visibilityGraph.createVisibilityMapSolution();

      ArrayList<VisibilityMapWithNavigableRegion> visibilityMapsWithNavigableRegions = visibilityMapSolution.getVisibilityMapsWithNavigableRegions();
      assertEquals(1, visibilityMapsWithNavigableRegions.size());

      VisibilityMapWithNavigableRegion innerMapAndRegion0 = visibilityMapsWithNavigableRegions.get(0);
      VisibilityMap innerMap0 = innerMapAndRegion0.getVisibilityMapInWorld();

      Set<ConnectionPoint3D> vertices0 = innerMap0.getVertices();
      Set<Connection> connections0 = innerMap0.getConnections();

      assertEquals(8, vertices0.size());

      assertTrue(vertices0.contains(connectionA));
      assertTrue(vertices0.contains(connectionAB));
      assertTrue(vertices0.contains(connectionB));
      assertTrue(vertices0.contains(connectionBC));
      assertTrue(vertices0.contains(connectionC));
      assertTrue(vertices0.contains(connectionCD));
      assertTrue(vertices0.contains(connectionD));
      assertTrue(vertices0.contains(connectionDA));

      assertEquals(28, connections0.size());

      assertTrue(connectionsContain(connections0, connectionA, connectionAB));
      assertTrue(connectionsContain(connections0, connectionA, connectionB));
      assertTrue(connectionsContain(connections0, connectionA, connectionBC));
      assertTrue(connectionsContain(connections0, connectionA, connectionC));
      assertTrue(connectionsContain(connections0, connectionA, connectionCD));
      assertTrue(connectionsContain(connections0, connectionA, connectionD));
      assertTrue(connectionsContain(connections0, connectionA, connectionDA));
      assertTrue(connectionsContain(connections0, connectionAB, connectionB));
      assertTrue(connectionsContain(connections0, connectionAB, connectionBC));
      assertTrue(connectionsContain(connections0, connectionAB, connectionC));
      assertTrue(connectionsContain(connections0, connectionAB, connectionCD));
      assertTrue(connectionsContain(connections0, connectionAB, connectionD));
      assertTrue(connectionsContain(connections0, connectionAB, connectionDA));
      assertTrue(connectionsContain(connections0, connectionB, connectionBC));
      assertTrue(connectionsContain(connections0, connectionB, connectionC));
      assertTrue(connectionsContain(connections0, connectionB, connectionCD));
      assertTrue(connectionsContain(connections0, connectionB, connectionD));
      assertTrue(connectionsContain(connections0, connectionB, connectionDA));
      assertTrue(connectionsContain(connections0, connectionBC, connectionC));
      assertTrue(connectionsContain(connections0, connectionBC, connectionCD));
      assertTrue(connectionsContain(connections0, connectionBC, connectionD));
      assertTrue(connectionsContain(connections0, connectionBC, connectionDA));
      assertTrue(connectionsContain(connections0, connectionC, connectionCD));
      assertTrue(connectionsContain(connections0, connectionC, connectionD));
      assertTrue(connectionsContain(connections0, connectionC, connectionDA));
      assertTrue(connectionsContain(connections0, connectionCD, connectionD));
      assertTrue(connectionsContain(connections0, connectionCD, connectionDA));
      assertTrue(connectionsContain(connections0, connectionD, connectionDA));

      InterRegionVisibilityMap interRegionVisibilityMap = visibilityMapSolution.getInterRegionVisibilityMap();
      assertTrue(interRegionVisibilityMap.getVisibilityMapInWorld().isEmpty());
   }

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testVisibilityGraphTwoSquares()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();
      List<PlanarRegion> planarRegions = new ArrayList<>();

      Point2D pointA = new Point2D(-0.01, -0.01);
      Point2D pointB = new Point2D(-0.01, 1.01);
      Point2D pointC = new Point2D(1.01, 1.01);
      Point2D pointD = new Point2D(1.01, -0.01);

      Point2D pointE = new Point2D(7.0 - 0.01, 3.0 - 0.01);
      Point2D pointF = new Point2D(7.0 - 0.01, 3.0 + 1.01);
      Point2D pointG = new Point2D(7.0 + 1.01, 3.0 + 1.01);
      Point2D pointH = new Point2D(7.0 + 1.01, 3.0 - 0.01);

      ConvexPolygon2D polygon0_0 = new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(pointA, pointB, pointC, pointD));
      ConvexPolygon2D polygon1_0 = new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(pointE, pointF, pointG, pointH));

      RigidBodyTransform transform0 = new RigidBodyTransform();
      RigidBodyTransform transform1 = new RigidBodyTransform();
      transform1.setTranslation(-7.0 + 1.1, -3.0, 0.0);

      PlanarRegion planarRegion0 = new PlanarRegion(transform0, polygon0_0);
      planarRegion0.setRegionId(77);
      PlanarRegion planarRegion1 = new PlanarRegion(transform1, polygon1_0);
      planarRegion1.setRegionId(63);

      planarRegions.add(planarRegion0);
      planarRegions.add(planarRegion1);

      NavigableRegions navigableRegions = new NavigableRegions(parameters, planarRegions);
      navigableRegions.createNavigableRegions();

      List<NavigableRegion> naviableRegionsList = navigableRegions.getNaviableRegionsList();
      assertEquals(2, naviableRegionsList.size());

      NavigableRegion navigableRegion0 = naviableRegionsList.get(0);
      NavigableRegion navigableRegion1 = naviableRegionsList.get(1);
      assertEquals(77, navigableRegion0.getMapId());
      assertEquals(63, navigableRegion1.getMapId());

      InterRegionConnectionFilter filter = new InterRegionConnectionFilter()
      {
         @Override
         public boolean isConnectionValid(ConnectionPoint3D source, ConnectionPoint3D target)
         {
            double distance = source.distance(target);
            return distance < 0.58;
         }

         @Override
         public double getMaximumInterRegionConnetionDistance()
         {
            return 0.58;
         }
      };
      VisibilityGraph visibilityGraph = new VisibilityGraph(navigableRegions, filter);

      ArrayList<VisibilityGraphNavigableRegion> visibilityGraphNavigableRegions = visibilityGraph.getVisibilityGraphNavigableRegions();
      assertEquals(2, visibilityGraphNavigableRegions.size());
      VisibilityGraphNavigableRegion visibilityGraphNavigableRegion0 = visibilityGraphNavigableRegions.get(0);
      VisibilityGraphNavigableRegion visibilityGraphNavigableRegion1 = visibilityGraphNavigableRegions.get(1);

      ArrayList<VisibilityGraphEdge> internalEdges0 = visibilityGraphNavigableRegion0.getAllEdges();
      ArrayList<VisibilityGraphEdge> internalEdges1 = visibilityGraphNavigableRegion1.getAllEdges();

      assertEquals(28, internalEdges0.size());
      assertEquals(28, internalEdges1.size());

      ArrayList<VisibilityGraphNode> nodes0 = visibilityGraphNavigableRegion0.getHomeRegionNodes();
      ArrayList<VisibilityGraphNode> nodes1 = visibilityGraphNavigableRegion1.getHomeRegionNodes();
      assertEquals(8, nodes0.size());
      assertEquals(8, nodes1.size());

      ArrayList<VisibilityGraphEdge> crossRegionEdges = visibilityGraph.getCrossRegionEdges();
      assertEquals(7, crossRegionEdges.size());

      ConnectionPoint3D connectionA = new ConnectionPoint3D(0.0, 0.0, 0.0, 0);
      ConnectionPoint3D connectionAB = new ConnectionPoint3D(0.0, 0.5, 0.0, 0);
      ConnectionPoint3D connectionB = new ConnectionPoint3D(0.0, 1.0, 0.0, 0);
      ConnectionPoint3D connectionBC = new ConnectionPoint3D(0.5, 1.0, 0.0, 0);
      ConnectionPoint3D connectionC = new ConnectionPoint3D(1.0, 1.0, 0.0, 0);
      ConnectionPoint3D connectionCD = new ConnectionPoint3D(1.0, 0.5, 0.0, 0);
      ConnectionPoint3D connectionD = new ConnectionPoint3D(1.0, 0.0, 0.0, 0);
      ConnectionPoint3D connectionDA = new ConnectionPoint3D(0.5, 0.0, 0.0, 0);

      ConnectionPoint3D connectionE = new ConnectionPoint3D(1.1, 0.0, 0.0, 0);
      ConnectionPoint3D connectionEF = new ConnectionPoint3D(1.1, 0.5, 0.0, 0);
      ConnectionPoint3D connectionF = new ConnectionPoint3D(1.1, 1.0, 0.0, 0);
      ConnectionPoint3D connectionFG = new ConnectionPoint3D(1.6, 1.0, 0.0, 0);
      ConnectionPoint3D connectionG = new ConnectionPoint3D(2.1, 1.0, 0.0, 0);
      ConnectionPoint3D connectionGH = new ConnectionPoint3D(2.1, 0.5, 0.0, 0);
      ConnectionPoint3D connectionH = new ConnectionPoint3D(2.1, 0.0, 0.0, 0);
      ConnectionPoint3D connectionHE = new ConnectionPoint3D(1.6, 0.0, 0.0, 0);

      assertTrue(edgeListContains(crossRegionEdges, connectionC, connectionF));
      assertTrue(edgeListContains(crossRegionEdges, connectionC, connectionEF));
      assertTrue(edgeListContains(crossRegionEdges, connectionCD, connectionF));
      assertTrue(edgeListContains(crossRegionEdges, connectionCD, connectionEF));
      assertTrue(edgeListContains(crossRegionEdges, connectionCD, connectionE));
      assertTrue(edgeListContains(crossRegionEdges, connectionD, connectionEF));
      assertTrue(edgeListContains(crossRegionEdges, connectionD, connectionE));

      VisibilityMapSolution visibilityMapSolution = visibilityGraph.createVisibilityMapSolution();

      ArrayList<VisibilityMapWithNavigableRegion> visibilityMapsWithNavigableRegions = visibilityMapSolution.getVisibilityMapsWithNavigableRegions();
      assertEquals(2, visibilityMapsWithNavigableRegions.size());

      VisibilityMapWithNavigableRegion innerMapAndRegion0 = visibilityMapsWithNavigableRegions.get(0);
      VisibilityMapWithNavigableRegion innerMapAdnRegion1 = visibilityMapsWithNavigableRegions.get(1);

      VisibilityMap innerMap0 = innerMapAndRegion0.getVisibilityMapInWorld();
      VisibilityMap innerMap1 = innerMapAdnRegion1.getVisibilityMapInWorld();

      Set<Connection> connections0 = innerMap0.getConnections();
      Set<ConnectionPoint3D> vertices0 = innerMap0.getVertices();

      assertEquals(8, vertices0.size());
      assertEquals(28, connections0.size());

      InterRegionVisibilityMap interRegionVisibilityMap = visibilityMapSolution.getInterRegionVisibilityMap();
      VisibilityMap interRegionVisibilityMapInWorld = interRegionVisibilityMap.getVisibilityMapInWorld();

      Set<Connection> connections = interRegionVisibilityMapInWorld.getConnections();
      Set<ConnectionPoint3D> vertices = interRegionVisibilityMapInWorld.getVertices();

      assertEquals(7, connections.size());
      assertEquals(0, vertices.size());

      assertTrue(connectionsContain(connections, connectionC, connectionF));
      assertTrue(connectionsContain(connections, connectionC, connectionEF));
      assertTrue(connectionsContain(connections, connectionCD, connectionF));
      assertTrue(connectionsContain(connections, connectionCD, connectionEF));
      assertTrue(connectionsContain(connections, connectionCD, connectionE));
      assertTrue(connectionsContain(connections, connectionD, connectionEF));
      assertTrue(connectionsContain(connections, connectionD, connectionE));
   }

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testVisibilityGraphSquareInSquare()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();
      List<PlanarRegion> planarRegions = new ArrayList<>();

      Point2D pointA = new Point2D(-0.01, -0.01);
      Point2D pointB = new Point2D(-0.01, 1.01);
      Point2D pointC = new Point2D(1.01, 1.01);
      Point2D pointD = new Point2D(1.01, -0.01);

      Point2D pointE = new Point2D(0.20, 0.20);
      Point2D pointF = new Point2D(0.20, 0.80);
      Point2D pointG = new Point2D(0.80, 0.80);
      Point2D pointH = new Point2D(0.80, 0.20);

      ConvexPolygon2D polygon0_0 = new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(pointA, pointB, pointC, pointD));
      ConvexPolygon2D polygon1_0 = new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(pointE, pointF, pointG, pointH));

      RigidBodyTransform transform0 = new RigidBodyTransform();
      RigidBodyTransform transform1 = new RigidBodyTransform();

      double height = 0.05;
      transform1.setTranslation(0.0, 0.0, height);

      PlanarRegion planarRegion0 = new PlanarRegion(transform0, polygon0_0);
      planarRegion0.setRegionId(77);
      PlanarRegion planarRegion1 = new PlanarRegion(transform1, polygon1_0);
      planarRegion1.setRegionId(63);

      planarRegions.add(planarRegion0);
      planarRegions.add(planarRegion1);

      NavigableRegions navigableRegions = new NavigableRegions(parameters, planarRegions);
      navigableRegions.createNavigableRegions();

      List<NavigableRegion> naviableRegionsList = navigableRegions.getNaviableRegionsList();
      assertEquals(2, naviableRegionsList.size());

      NavigableRegion navigableRegion0 = naviableRegionsList.get(0);
      NavigableRegion navigableRegion1 = naviableRegionsList.get(1);
      assertEquals(77, navigableRegion0.getMapId());
      assertEquals(63, navigableRegion1.getMapId());

      InterRegionConnectionFilter filter = new InterRegionConnectionFilter()
      {
         @Override
         public boolean isConnectionValid(ConnectionPoint3D source, ConnectionPoint3D target)
         {
            double distance = source.distance(target);
            return distance < 0.58;
         }

         @Override
         public double getMaximumInterRegionConnetionDistance()
         {
            return 0.58;
         }
      };
      VisibilityGraph visibilityGraph = new VisibilityGraph(navigableRegions, filter);

      ArrayList<VisibilityGraphNavigableRegion> visibilityGraphNavigableRegions = visibilityGraph.getVisibilityGraphNavigableRegions();

      assertEquals(2, visibilityGraphNavigableRegions.size());
      VisibilityGraphNavigableRegion visibilityGraphNavigableRegion0 = visibilityGraphNavigableRegions.get(0);
      VisibilityGraphNavigableRegion visibilityGraphNavigableRegion1 = visibilityGraphNavigableRegions.get(1);

      ArrayList<VisibilityGraphEdge> internalEdges0 = visibilityGraphNavigableRegion0.getAllEdges();
      ArrayList<VisibilityGraphEdge> internalEdges1 = visibilityGraphNavigableRegion1.getAllEdges();

      ArrayList<VisibilityGraphNode> nodes0 = visibilityGraphNavigableRegion0.getHomeRegionNodes();
      ArrayList<VisibilityGraphNode> nodes1 = visibilityGraphNavigableRegion1.getHomeRegionNodes();
      assertEquals(8, nodes0.size());
      assertEquals(8, nodes1.size());

      ConnectionPoint3D connectionA = new ConnectionPoint3D(0.0, 0.0, 0.0, 0);
      ConnectionPoint3D connectionAB = new ConnectionPoint3D(0.0, 0.5, 0.0, 0);
      ConnectionPoint3D connectionB = new ConnectionPoint3D(0.0, 1.0, 0.0, 0);
      ConnectionPoint3D connectionBC = new ConnectionPoint3D(0.5, 1.0, 0.0, 0);
      ConnectionPoint3D connectionC = new ConnectionPoint3D(1.0, 1.0, 0.0, 0);
      ConnectionPoint3D connectionCD = new ConnectionPoint3D(1.0, 0.5, 0.0, 0);
      ConnectionPoint3D connectionD = new ConnectionPoint3D(1.0, 0.0, 0.0, 0);
      ConnectionPoint3D connectionDA = new ConnectionPoint3D(0.5, 0.0, 0.0, 0);

      assertTrue(arePointsAllContainedIn(nodes0, connectionA, connectionAB, connectionB, connectionBC, connectionC, connectionCD, connectionD, connectionDA));

      ConnectionPoint3D connectionE = new ConnectionPoint3D(0.21, 0.21, height, 0);
      ConnectionPoint3D connectionEF = new ConnectionPoint3D(0.21, 0.5, height, 0);
      ConnectionPoint3D connectionF = new ConnectionPoint3D(0.21, 0.79, height, 0);
      ConnectionPoint3D connectionFG = new ConnectionPoint3D(0.5, 0.79, height, 0);
      ConnectionPoint3D connectionG = new ConnectionPoint3D(0.79, 0.79, height, 0);
      ConnectionPoint3D connectionGH = new ConnectionPoint3D(0.79, 0.5, height, 0);
      ConnectionPoint3D connectionH = new ConnectionPoint3D(0.79, 0.21, height, 0);
      ConnectionPoint3D connectionHE = new ConnectionPoint3D(0.5, 0.21, height, 0);

      assertTrue(arePointsAllContainedIn(nodes1, connectionE, connectionEF, connectionF, connectionFG, connectionG, connectionGH, connectionH, connectionHE));

      ArrayList<ArrayList<VisibilityGraphNode>> listOfObstacleNavigableNodes0 = visibilityGraphNavigableRegion0.getObstacleNavigableNodes();
      ArrayList<ArrayList<VisibilityGraphNode>> listOfObstacleNavigableNodes1 = visibilityGraphNavigableRegion1.getObstacleNavigableNodes();

      assertEquals(1, listOfObstacleNavigableNodes0.size());
      assertEquals(0, listOfObstacleNavigableNodes1.size());

      ArrayList<VisibilityGraphNode> obstacleNavigableNodes0_0 = listOfObstacleNavigableNodes0.get(0);
      assertEquals(16, obstacleNavigableNodes0_0.size());

      double sqrt2By2 = Math.sqrt(2.0) / 2.0;

      ConnectionPoint3D connectionE0 = new ConnectionPoint3D(0.2, 0.15, 0.0, 0);
      ConnectionPoint3D connectionE1 = new ConnectionPoint3D(0.2 - sqrt2By2 * 0.05, 0.2 - sqrt2By2 * 0.05, 0.0, 0);
      ConnectionPoint3D connectionE2 = new ConnectionPoint3D(0.15, 0.2, 0.0, 0);
      ConnectionPoint3D connectionEF0 = new ConnectionPoint3D(0.15, 0.5, 0.0, 0);
      ConnectionPoint3D connectionF0 = new ConnectionPoint3D(0.15, 0.8, 0.0, 0);
      ConnectionPoint3D connectionF1 = new ConnectionPoint3D(0.2 - sqrt2By2 * 0.05, 0.8 + sqrt2By2 * 0.05, 0.0, 0);
      ConnectionPoint3D connectionF2 = new ConnectionPoint3D(0.2, 0.85, 0.0, 0);
      ConnectionPoint3D connectionFG0 = new ConnectionPoint3D(0.5, 0.85, 0.0, 0);
      ConnectionPoint3D connectionG0 = new ConnectionPoint3D(0.8, 0.85, 0.0, 0);
      ConnectionPoint3D connectionG1 = new ConnectionPoint3D(0.8 + sqrt2By2 * 0.05, 0.8 + sqrt2By2 * 0.05, 0.0, 0);
      ConnectionPoint3D connectionG2 = new ConnectionPoint3D(0.85, 0.8, 0.0, 0);
      ConnectionPoint3D connectionGH0 = new ConnectionPoint3D(0.85, 0.5, 0.0, 0);
      ConnectionPoint3D connectionH0 = new ConnectionPoint3D(0.85, 0.2, 0.0, 0);
      ConnectionPoint3D connectionH1 = new ConnectionPoint3D(0.8 + sqrt2By2 * 0.05, 0.2 - sqrt2By2 * 0.05, 0.0, 0);
      ConnectionPoint3D connectionH2 = new ConnectionPoint3D(0.8, 0.15, 0.0, 0);
      ConnectionPoint3D connectionHE0 = new ConnectionPoint3D(0.5, 0.15, 0.0, 0);

      assertTrue(arePointsAllContainedIn(obstacleNavigableNodes0_0, connectionE0, connectionE1, connectionE2));
      assertTrue(arePointsAllContainedIn(obstacleNavigableNodes0_0, connectionEF0, connectionF0, connectionF1, connectionF2, connectionFG0));
      assertTrue(arePointsAllContainedIn(obstacleNavigableNodes0_0, connectionG0, connectionG1, connectionG2, connectionGH0));
      assertTrue(arePointsAllContainedIn(obstacleNavigableNodes0_0, connectionH0, connectionH1, connectionH2, connectionHE0));

      assertFalse(edgeListContains(internalEdges0, connectionA, connectionA));
      assertTrue(edgeListContains(internalEdges0, connectionA, connectionAB));
      assertTrue(edgeListContains(internalEdges0, connectionA, connectionB));
      assertFalse(edgeListContains(internalEdges0, connectionA, connectionBC));
      assertFalse(edgeListContains(internalEdges0, connectionA, connectionC));
      assertFalse(edgeListContains(internalEdges0, connectionA, connectionCD));
      assertTrue(edgeListContains(internalEdges0, connectionA, connectionD));
      assertTrue(edgeListContains(internalEdges0, connectionA, connectionDA));

      assertTrue(edgeListContains(internalEdges0, connectionA, connectionE0));
      assertTrue(edgeListContains(internalEdges0, connectionA, connectionE1));
      assertTrue(edgeListContains(internalEdges0, connectionA, connectionE2));
      assertTrue(edgeListContains(internalEdges0, connectionA, connectionEF0));
      assertTrue(edgeListContains(internalEdges0, connectionA, connectionF0));
      assertFalse(edgeListContains(internalEdges0, connectionA, connectionF1));
      assertFalse(edgeListContains(internalEdges0, connectionA, connectionF2));
      assertFalse(edgeListContains(internalEdges0, connectionA, connectionFG0));
      assertFalse(edgeListContains(internalEdges0, connectionA, connectionG0));
      assertFalse(edgeListContains(internalEdges0, connectionA, connectionG1));
      assertFalse(edgeListContains(internalEdges0, connectionA, connectionG2));
      assertFalse(edgeListContains(internalEdges0, connectionA, connectionGH0));
      assertFalse(edgeListContains(internalEdges0, connectionA, connectionH0));
      assertFalse(edgeListContains(internalEdges0, connectionA, connectionH1));
      assertTrue(edgeListContains(internalEdges0, connectionA, connectionH2));
      assertTrue(edgeListContains(internalEdges0, connectionA, connectionHE0));

      assertFalse(edgeListContains(internalEdges0, connectionCD, connectionA));
      assertFalse(edgeListContains(internalEdges0, connectionCD, connectionAB));
      assertFalse(edgeListContains(internalEdges0, connectionCD, connectionB));
      assertFalse(edgeListContains(internalEdges0, connectionCD, connectionBC));
      assertTrue(edgeListContains(internalEdges0, connectionCD, connectionC));
      assertFalse(edgeListContains(internalEdges0, connectionCD, connectionCD));
      assertTrue(edgeListContains(internalEdges0, connectionCD, connectionD));
      assertFalse(edgeListContains(internalEdges0, connectionCD, connectionDA));

      assertFalse(edgeListContains(internalEdges0, connectionCD, connectionE0));
      assertFalse(edgeListContains(internalEdges0, connectionCD, connectionE1));
      assertFalse(edgeListContains(internalEdges0, connectionCD, connectionE2));
      assertFalse(edgeListContains(internalEdges0, connectionCD, connectionEF0));
      assertFalse(edgeListContains(internalEdges0, connectionCD, connectionF0));
      assertFalse(edgeListContains(internalEdges0, connectionCD, connectionF1));
      assertFalse(edgeListContains(internalEdges0, connectionCD, connectionF2));
      assertFalse(edgeListContains(internalEdges0, connectionCD, connectionFG0));
      assertFalse(edgeListContains(internalEdges0, connectionCD, connectionG0));
      assertTrue(edgeListContains(internalEdges0, connectionCD, connectionG1));
      assertTrue(edgeListContains(internalEdges0, connectionCD, connectionG2));
      assertTrue(edgeListContains(internalEdges0, connectionCD, connectionGH0));
      assertTrue(edgeListContains(internalEdges0, connectionCD, connectionH0));
      assertTrue(edgeListContains(internalEdges0, connectionCD, connectionH1));
      assertFalse(edgeListContains(internalEdges0, connectionCD, connectionH2));
      assertFalse(edgeListContains(internalEdges0, connectionCD, connectionHE0));

      assertTrue(edgeListContains(internalEdges0, connectionE0, connectionE1));
      assertTrue(edgeListContains(internalEdges0, connectionE1, connectionE2));
      assertTrue(edgeListContains(internalEdges0, connectionE2, connectionEF0));
      assertTrue(edgeListContains(internalEdges0, connectionEF0, connectionF0));
      assertTrue(edgeListContains(internalEdges0, connectionF0, connectionF1));
      assertTrue(edgeListContains(internalEdges0, connectionF1, connectionF2));
      assertTrue(edgeListContains(internalEdges0, connectionF2, connectionFG0));
      assertTrue(edgeListContains(internalEdges0, connectionFG0, connectionG0));
      assertTrue(edgeListContains(internalEdges0, connectionG0, connectionG1));
      assertTrue(edgeListContains(internalEdges0, connectionG1, connectionG2));
      assertTrue(edgeListContains(internalEdges0, connectionG2, connectionGH0));
      assertTrue(edgeListContains(internalEdges0, connectionGH0, connectionH0));
      assertTrue(edgeListContains(internalEdges0, connectionH0, connectionH1));
      assertTrue(edgeListContains(internalEdges0, connectionH1, connectionH2));
      assertTrue(edgeListContains(internalEdges0, connectionH2, connectionHE0));

      assertEquals(11 + 6 + 9 + 6 + 9 + 6 + 8 + 5 + 16, internalEdges0.size());
      assertEquals(28, internalEdges1.size());

      ArrayList<VisibilityGraphEdge> crossRegionEdges = visibilityGraph.getCrossRegionEdges();
      assertEquals(24, crossRegionEdges.size());

      assertTrue(edgeListContains(crossRegionEdges, connectionA, connectionE));
      assertTrue(edgeListContains(crossRegionEdges, connectionA, connectionEF));
      assertTrue(edgeListContains(crossRegionEdges, connectionA, connectionHE));
      assertTrue(edgeListContains(crossRegionEdges, connectionBC, connectionF));
      assertTrue(edgeListContains(crossRegionEdges, connectionBC, connectionFG));
      assertTrue(edgeListContains(crossRegionEdges, connectionBC, connectionG));

      VisibilityMapSolution visibilityMapSolution = visibilityGraph.createVisibilityMapSolution();

      ArrayList<VisibilityMapWithNavigableRegion> visibilityMapsWithNavigableRegions = visibilityMapSolution.getVisibilityMapsWithNavigableRegions();
      assertEquals(2, visibilityMapsWithNavigableRegions.size());

      VisibilityMapWithNavigableRegion innerMapAndRegion0 = visibilityMapsWithNavigableRegions.get(0);
      VisibilityMapWithNavigableRegion innerMapAdnRegion1 = visibilityMapsWithNavigableRegions.get(1);

      VisibilityMap innerMap0 = innerMapAndRegion0.getVisibilityMapInWorld();
      Set<ConnectionPoint3D> vertices0 = innerMap0.getVertices();
      Set<Connection> connections0 = innerMap0.getConnections();

      assertEquals(24, vertices0.size());
      assertEquals(11 + 6 + 9 + 6 + 9 + 6 + 8 + 5 + 16, connections0.size());

      VisibilityMap innerMap1 = innerMapAdnRegion1.getVisibilityMapInWorld();
      Set<ConnectionPoint3D> vertices1 = innerMap1.getVertices();
      Set<Connection> connections1 = innerMap1.getConnections();

      assertEquals(8, vertices1.size());
      assertEquals(28, connections1.size());

      InterRegionVisibilityMap interRegionVisibilityMap = visibilityMapSolution.getInterRegionVisibilityMap();
      VisibilityMap interRegionVisibilityMapInWorld = interRegionVisibilityMap.getVisibilityMapInWorld();

      Set<Connection> interRegionConnections = interRegionVisibilityMapInWorld.getConnections();
      Set<ConnectionPoint3D> interRegionVertices = interRegionVisibilityMapInWorld.getVertices();

      assertEquals(24, interRegionConnections.size());
      //TODO: Does it even make sense to hold the vertices here?
      assertEquals(0, interRegionVertices.size());

      assertTrue(connectionsContain(interRegionConnections, connectionA, connectionE));
      assertTrue(connectionsContain(interRegionConnections, connectionA, connectionEF));
      assertTrue(connectionsContain(interRegionConnections, connectionA, connectionHE));
      assertTrue(connectionsContain(interRegionConnections, connectionBC, connectionF));
      assertTrue(connectionsContain(interRegionConnections, connectionBC, connectionFG));
      assertTrue(connectionsContain(interRegionConnections, connectionBC, connectionG));

      //TODO: We are missing these, right? Or even better, shouldn't cross any keep out regions...
      //      assertTrue(connectionsContain(interRegionConnections, connectionE0, connectionE));
      //      assertTrue(connectionsContain(interRegionConnections, connectionE1, connectionE));
      //      assertTrue(connectionsContain(interRegionConnections, connectionE2, connectionE));
      //      assertTrue(connectionsContain(interRegionConnections, connectionCD, connectionE));
      //      assertTrue(connectionsContain(interRegionConnections, connectionD, connectionEF));
      //      assertTrue(connectionsContain(interRegionConnections, connectionD, connectionE));
   }

   private void printNodes(ArrayList<VisibilityGraphNode> nodes)
   {
      for (VisibilityGraphNode node : nodes)
      {
         System.out.println(node.getPointInWorld());
      }
      System.out.println();
   }

   private boolean arePointsAllContainedIn(ArrayList<VisibilityGraphNode> nodes, ConnectionPoint3D... points)
   {
      for (ConnectionPoint3D point : points)
      {
         if (!nodesContainPoint(nodes, point))
            return false;
      }
      return true;
   }

   private boolean nodesContainPoint(ArrayList<VisibilityGraphNode> nodes, ConnectionPoint3D point)
   {
      for (VisibilityGraphNode node : nodes)
      {
         if (node.getPointInWorld().epsilonEquals(point, EPSILON))
            return true;
      }
      return false;
   }

   private boolean connectionsContain(Set<Connection> connections, ConnectionPoint3D pointOne, ConnectionPoint3D pointTwo)
   {
      //FIXME: This doesn't seem to be working... Maybe something wrong with hash or rounding or something...
      //    return connections.contains(new Connection(pointOne, pointTwo));

      for (Connection connection : connections)
      {
         ConnectionPoint3D sourcePoint = connection.getSourcePoint();
         ConnectionPoint3D targetPoint = connection.getTargetPoint();

         if (sourcePoint.epsilonEquals(pointOne, EPSILON) && (targetPoint.epsilonEquals(pointTwo, EPSILON)))
            return true;

         if (sourcePoint.epsilonEquals(pointTwo, EPSILON) && (targetPoint.epsilonEquals(pointOne, EPSILON)))
            return true;
      }

      return false;
   }

   private void printEdges(ArrayList<VisibilityGraphEdge> edges)
   {
      for (VisibilityGraphEdge edge : edges)
      {
         System.out.println(edge);
      }
      System.out.println();

   }

   private boolean edgeListContains(ArrayList<VisibilityGraphEdge> edges, ConnectionPoint3D pointOne, ConnectionPoint3D pointTwo)
   {
      for (VisibilityGraphEdge edge : edges)
      {
         ConnectionPoint3D sourcePoint = edge.getSourcePoint();
         ConnectionPoint3D targetPoint = edge.getTargetPoint();

         if (sourcePoint.equals(pointOne) && targetPoint.equals(pointTwo))
            return true;

         if (sourcePoint.equals(pointTwo) && targetPoint.equals(pointOne))
            return true;
      }

      return false;
   }

   private void printConnections(Set<Connection> connections)
   {
      for (Connection connection : connections)
      {
         System.out.println(connection);
      }
      System.out.println();
   }

   private void printVertices(Set<ConnectionPoint3D> vertices)
   {
      for (ConnectionPoint3D vertex : vertices)
      {
         System.out.println(vertex);
      }
      System.out.println();

   }

   private VisibilityGraphsParameters createVisibilityGraphParametersForTest()
   {
      return new DefaultVisibilityGraphParameters()
      {
         @Override
         public PlanarRegionFilter getPlanarRegionFilter()
         {
            return new PlanarRegionFilter()
            {
               @Override
               public boolean isPlanarRegionRelevant(PlanarRegion region)
               {
                  return true;
               }
            };
         }

         @Override
         public double getClusterResolution()
         {
            return 0.501;
         }

         @Override
         public NavigableExtrusionDistanceCalculator getNavigableExtrusionDistanceCalculator()
         {
            return new NavigableExtrusionDistanceCalculator()
            {
               @Override
               public double computeExtrusionDistance(PlanarRegion navigableRegionToBeExtruded)
               {
                  return 0.01;
               }
            };
         }

         //         @Override
         //         public double getExtrusionDistance()
         //         {
         //            return 0.2;
         //         }
      };
   }

}
