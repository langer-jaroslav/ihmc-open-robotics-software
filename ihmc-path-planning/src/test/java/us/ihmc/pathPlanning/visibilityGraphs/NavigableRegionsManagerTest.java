package us.ihmc.pathPlanning.visibilityGraphs;

import org.junit.Test;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.continuousIntegration.ContinuousIntegrationTools;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.interfaces.Vertex2DSupplier;
import us.ihmc.euclid.tools.EuclidCoreTestTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.graphicsDescription.Graphics3DObject;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.NavigableExtrusionDistanceCalculator;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.PlanarRegionFilter;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.VisibilityGraphsParameters;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.graphics.Graphics3DObjectTools;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class NavigableRegionsManagerTest
{
   private static final boolean visualize = true;
   private static final double epsilon = 5e-3;

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testFlatGroundWithWallInlineWithWall()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithWallEnvironment());

      // test aligned with the edge of the wall, requiring slight offset
      Point3D start = new Point3D(-15.0, 0.0, 0.0);
      Point3D goal = new Point3D(-5.0, 0.0, 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList());
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPathWithOcclusions(start, goal);

      assertEquals(3, path.size());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(start, path.get(0), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-10.0, -parameters.getPreferredObstacleExtrusionDistance(), 0.0), path.get(1), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(goal, path.get(2), epsilon);

      if (visualize)
      {
         visualize(path, planarRegionsList, start, goal);
      }
   }

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testFlatGroundWithWallOnOppositeSidesOfWall()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithWallEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, 1.0, 0.0);
      Point3D goal = new Point3D(-5.0, 1.0, 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList());
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPathWithOcclusions(start, goal);

      assertEquals(3, path.size());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(start, path.get(0), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-10.0, -parameters.getPreferredObstacleExtrusionDistance(), 0.0), path.get(1), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(goal, path.get(2), epsilon);

      if (visualize)
      {
         visualize(path, planarRegionsList, start, goal);
      }
   }

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testFlatGroundWithWallStraightShotButVeryNearWall()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithWallEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, -0.05 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);
      Point3D goal = new Point3D(-5.0, -0.05 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList());
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPathWithOcclusions(start, goal);

      if (visualize)
      {
         visualize(path, planarRegionsList, start, goal);
      }

      assertEquals(3, path.size());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(start, path.get(0), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-10.0, -parameters.getPreferredObstacleExtrusionDistance(), 0.0), path.get(1), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(goal, path.get(2), epsilon);
   }

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testFlatGroundWithWallStraightShotButNearWall()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithWallEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, -0.1 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);
      Point3D goal = new Point3D(-5.0, -0.1 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList());
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPathWithOcclusions(start, goal);

      if (visualize)
      {
         visualize(path, planarRegionsList, start, goal);
      }

      assertEquals(3, path.size());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(start, path.get(0), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-10.0, -parameters.getPreferredObstacleExtrusionDistance(), 0.0), path.get(1), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(goal, path.get(2), epsilon);
   }

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testFlatGroundWithWallAlmostStraightShot()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithWallEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, -0.95 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);
      Point3D goal = new Point3D(-5.0, -0.95 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList());
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPathWithOcclusions(start, goal);

      if (visualize)
      {
         visualize(path, planarRegionsList, start, goal);
      }

      assertEquals(3, path.size());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(start, path.get(0), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-10.0, -parameters.getPreferredObstacleExtrusionDistance(), 0.0), path.get(1), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(goal, path.get(2), epsilon);
   }

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testFlatGroundWithWallStraightShot()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithWallEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, -1.05 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);
      Point3D goal = new Point3D(-5.0, -1.05 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList());
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPathWithOcclusions(start, goal);

      if (visualize)
      {
         visualize(path, planarRegionsList, start, goal);
      }

      assertEquals(2, path.size());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(start, path.get(0), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(goal, path.get(1), epsilon);
   }

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testFlatGroundWithBoxInlineWithWall()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithBoxEnvironment());

      // test aligned with the edge of the wall, requiring slight offset
      Point3D start = new Point3D(-15.0, 0.0, 0.0);
      Point3D goal = new Point3D(-5.0, 0.0, 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList());
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPathWithOcclusions(start, goal);


      if (visualize)
      {
         visualize(path, planarRegionsList, start, goal);
      }

      assertEquals(4, path.size());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(start, path.get(0), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-11.5, -parameters.getPreferredObstacleExtrusionDistance(), 0.0), path.get(1), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-8.5, -parameters.getPreferredObstacleExtrusionDistance(), 0.0), path.get(2), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(goal, path.get(3), epsilon);
   }

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testFlatGroundWithBoxOnOppositeSidesOfWall()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithBoxEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, 1.0, 0.0);
      Point3D goal = new Point3D(-5.0, 1.0, 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList());
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPathWithOcclusions(start, goal);

      if (visualize)
      {
         visualize(path, planarRegionsList, start, goal);
      }

      assertEquals(4, path.size());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(start, path.get(0), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-11.5, -parameters.getPreferredObstacleExtrusionDistance(), 0.0), path.get(1), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-8.5, -parameters.getPreferredObstacleExtrusionDistance(), 0.0), path.get(2), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(goal, path.get(3), epsilon);
   }

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testFlatGroundWithBoxStraightShotButVeryNearWall()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithBoxEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, -0.05 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);
      Point3D goal = new Point3D(-5.0, -0.05 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList());
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPathWithOcclusions(start, goal);

      if (visualize)
      {
         visualize(path, planarRegionsList, start, goal);
      }

      assertEquals(4, path.size());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(start, path.get(0), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-11.5, -parameters.getPreferredObstacleExtrusionDistance(), 0.0), path.get(1), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-8.5, -parameters.getPreferredObstacleExtrusionDistance(), 0.0), path.get(2), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(goal, path.get(3), epsilon);
   }

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testFlatGroundWithBoxStraightShotButNearWall()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithBoxEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, -0.1 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);
      Point3D goal = new Point3D(-5.0, -0.1 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList());
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPathWithOcclusions(start, goal);

      if (visualize)
      {
         visualize(path, planarRegionsList, start, goal);
      }

      assertEquals(4, path.size());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(start, path.get(0), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-11.5, -parameters.getPreferredObstacleExtrusionDistance(), 0.0), path.get(1), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-8.5, -parameters.getPreferredObstacleExtrusionDistance(), 0.0), path.get(2), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(goal, path.get(3), epsilon);
   }

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testFlatGroundWithBoxAlmostStraightShot()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithBoxEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, -0.95 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);
      Point3D goal = new Point3D(-5.0, -0.95 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList());
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPathWithOcclusions(start, goal);

      if (visualize)
      {
         visualize(path, planarRegionsList, start, goal);
      }

      assertEquals(4, path.size());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(start, path.get(0), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-11.5, -parameters.getPreferredObstacleExtrusionDistance(), 0.0), path.get(1), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-8.5, -parameters.getPreferredObstacleExtrusionDistance(), 0.0), path.get(2), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(goal, path.get(3), epsilon);
   }

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testFlatGroundWithBoxStraightShot()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithBoxEnvironment());

      // test on opposite sides of the wall, requiring going around it
      Point3D start = new Point3D(-15.0, -1.05 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);
      Point3D goal = new Point3D(-5.0, -1.05 * parameters.getPreferredObstacleExtrusionDistance(), 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList());
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPathWithOcclusions(start, goal);

      if (visualize)
      {
         visualize(path, planarRegionsList, start, goal);
      }

      assertEquals(2, path.size());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(start, path.get(0), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(goal, path.get(1), epsilon);
   }

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testFlatGroundWithTwoDifferentWalls()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundTwoDifferentWidthWallsEnvironment());

      // test straight shot, initially going to one of the nodes
      Point3D start = new Point3D(-15.0, -0.5 * parameters.getObstacleExtrusionDistance(), 0.0);
      Point3D goal = new Point3D(-5.0, -0.5 * parameters.getObstacleExtrusionDistance(), 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList());
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPathWithOcclusions(start, goal);

      if (visualize)
      {
         visualize(path, planarRegionsList, start, goal);
      }

      assertEquals(4, path.size());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(start, path.get(0), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-11.5, 0.5 - parameters.getPreferredObstacleExtrusionDistance(), 0.0), path.get(1), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-8.5, -parameters.getPreferredObstacleExtrusionDistance(), 0.0), path.get(1), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(goal, path.get(2), epsilon);
   }

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testFlatGroundBetweenWallOpening()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithWallOpeningEnvironment());

      // test aligned with the edge of the wall, requiring slight offset
      Point3D start = new Point3D(-15.0, 1.5, 0.0);
      Point3D goal = new Point3D(-5.0, 1.5, 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList());
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPathWithOcclusions(start, goal);

      if (visualize)
      {
         visualize(path, planarRegionsList, start, goal);
      }

      assertEquals(3, path.size());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(start, path.get(0), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-10.0, 0.0, 0.0), path.get(1), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(goal, path.get(2), epsilon);


   }


   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testFlatGroundBetweenWallOpeningStraightShot()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithWallOpeningEnvironment());

      // test aligned with the edge of the wall, requiring slight offset
      Point3D start = new Point3D(-15.0, 0.0, 0.0);
      Point3D goal = new Point3D(-5.0, 0.0, 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList());
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPathWithOcclusions(start, goal);

      if (visualize)
      {
         visualize(path, planarRegionsList, start, goal);
      }

      assertEquals(2, path.size());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(start, path.get(0), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(goal, path.get(1), epsilon);
   }

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testFlatGroundBetweenBoxesOpening()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithBoxesEnvironment());

      // test aligned with the edge of the wall, requiring slight offset
      Point3D start = new Point3D(-15.0, 1.5, 0.0);
      Point3D goal = new Point3D(-5.0, 1.5, 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList());
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPathWithOcclusions(start, goal);

      if (visualize)
      {
         visualize(path, planarRegionsList, start, goal);
      }

      assertEquals(3, path.size());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(start, path.get(0), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(new Point3D(-10.0, 0.0, 0.0), path.get(1), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(goal, path.get(2), epsilon);


   }


   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   public void testFlatGroundBetweenBoxesOpeningStraightShot()
   {
      VisibilityGraphsParameters parameters = createVisibilityGraphParametersForTest();

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(createFlatGroundWithBoxesEnvironment());

      // test aligned with the edge of the wall, requiring slight offset
      Point3D start = new Point3D(-15.0, 0.0, 0.0);
      Point3D goal = new Point3D(-5.0, 0.0, 0.0);

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(parameters, planarRegionsList.getPlanarRegionsAsList());
      navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

      List<Point3DReadOnly> path = navigableRegionsManager.calculateBodyPathWithOcclusions(start, goal);

      if (visualize)
      {
         visualize(path, planarRegionsList, start, goal);
      }

      assertEquals(2, path.size());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(start, path.get(0), epsilon);
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(goal, path.get(1), epsilon);
   }


   private static List<PlanarRegion> createFlatGroundWithWallEnvironment()
   {
      List<PlanarRegion> planarRegions = new ArrayList<>();

      // set up ground plane, 20 x 10
      Point2D groundPlanePointA = new Point2D(10.0, -5.0);
      Point2D groundPlanePointB = new Point2D(10.0, 5.0);
      Point2D groundPlanePointC = new Point2D(-10.0, 5.0);
      Point2D groundPlanePointD = new Point2D(-10.0, -5.0);

      RigidBodyTransform groundTransform = new RigidBodyTransform();
      groundTransform.setTranslation(-10.0, 0.0, 0.0);
      PlanarRegion groundPlaneRegion = new PlanarRegion(groundTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(groundPlanePointA, groundPlanePointB, groundPlanePointC, groundPlanePointD)));

      // set up wall, 5x2
      Point2D wallPointA = new Point2D(2.0, 0.0);
      Point2D wallPointB = new Point2D(0.0, 0.0);
      Point2D wallPointC = new Point2D(2.0, 5.0);
      Point2D wallPointD = new Point2D(0.0, 5.0);

      RigidBodyTransform wallTransform = new RigidBodyTransform();
      wallTransform.setTranslation(-10.0, 0.0, 0.0);
      wallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion wallRegion = new PlanarRegion(wallTransform,
                                                 new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(wallPointA, wallPointB, wallPointC, wallPointD)));

      planarRegions.add(groundPlaneRegion);
      planarRegions.add(wallRegion);

      return planarRegions;
   }

   private static List<PlanarRegion> createFlatGroundWithWallOpeningEnvironment()
   {
      List<PlanarRegion> planarRegions = new ArrayList<>();

      // set up ground plane, 20 x 10
      Point2D groundPlanePointA = new Point2D(10.0, -5.0);
      Point2D groundPlanePointB = new Point2D(10.0, 5.0);
      Point2D groundPlanePointC = new Point2D(-10.0, 5.0);
      Point2D groundPlanePointD = new Point2D(-10.0, -5.0);

      RigidBodyTransform groundTransform = new RigidBodyTransform();
      groundTransform.setTranslation(-10.0, 0.0, 0.0);
      PlanarRegion groundPlaneRegion = new PlanarRegion(groundTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(groundPlanePointA, groundPlanePointB, groundPlanePointC, groundPlanePointD)));

      // set up wall, 5x2
      Point2D wallPointA = new Point2D(2.0, 0.0);
      Point2D wallPointB = new Point2D(0.0, 0.0);
      Point2D wallPointC = new Point2D(2.0, 4.5);
      Point2D wallPointD = new Point2D(0.0, 4.5);

      RigidBodyTransform leftWallTransform = new RigidBodyTransform();
      leftWallTransform.setTranslation(-10.0, 0.5, 0.0);
      leftWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion leftWallRegion = new PlanarRegion(leftWallTransform,
                                                 new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(wallPointA, wallPointB, wallPointC, wallPointD)));

      RigidBodyTransform rightWallTransform = new RigidBodyTransform();
      rightWallTransform.setTranslation(-10.0, -5.0, 0.0);
      rightWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion rightWallRegion = new PlanarRegion(rightWallTransform,
                                                     new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(wallPointA, wallPointB, wallPointC, wallPointD)));

      planarRegions.add(groundPlaneRegion);
      planarRegions.add(leftWallRegion);
      planarRegions.add(rightWallRegion);

      return planarRegions;
   }

   private static List<PlanarRegion> createFlatGroundWithBoxEnvironment()
   {
      List<PlanarRegion> planarRegions = new ArrayList<>();

      // set up ground plane, 20 x 10
      Point2D groundPlanePointA = new Point2D(10.0, -5.0);
      Point2D groundPlanePointB = new Point2D(10.0, 5.0);
      Point2D groundPlanePointC = new Point2D(-10.0, 5.0);
      Point2D groundPlanePointD = new Point2D(-10.0, -5.0);

      RigidBodyTransform groundTransform = new RigidBodyTransform();
      groundTransform.setTranslation(-10.0, 0.0, 0.0);
      PlanarRegion groundPlaneRegion = new PlanarRegion(groundTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(groundPlanePointA, groundPlanePointB, groundPlanePointC, groundPlanePointD)));

      // set up wall, 5x2
      Point2D frontWallPointA = new Point2D(2.0, 0.0);
      Point2D frontWallPointB = new Point2D(0.0, 0.0);
      Point2D frontWallPointC = new Point2D(2.0, 5.0);
      Point2D frontWallPointD = new Point2D(0.0, 5.0);

      RigidBodyTransform frontWallTransform = new RigidBodyTransform();
      frontWallTransform.setTranslation(-11.5, 0.0, 0.0);
      frontWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion frontWallRegion = new PlanarRegion(frontWallTransform,
                                                 new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(frontWallPointA, frontWallPointB, frontWallPointC, frontWallPointD)));

      RigidBodyTransform backWallTransform = new RigidBodyTransform();
      backWallTransform.setTranslation(-8.5, 0.0, 0.0);
      backWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion backWallRegion = new PlanarRegion(backWallTransform,
                                                      new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(frontWallPointA, frontWallPointB, frontWallPointC, frontWallPointD)));

      Point2D sideWallPointA = new Point2D(2.0, 0.0);
      Point2D sideWallPointB = new Point2D(0.0, 0.0);
      Point2D sideWallPointC = new Point2D(2.0, 3.0);
      Point2D sideWallPointD = new Point2D(0.0, 3.0);

      RigidBodyTransform sideWallTransform = new RigidBodyTransform();
      sideWallTransform.setTranslation(-11.5, 0.0, 0.0);
      sideWallTransform.setRotationYawPitchRoll(-Math.PI / 2.0, -Math.PI / 2.0, 0.0);
      PlanarRegion sideWallRegion = new PlanarRegion(sideWallTransform,
                                                     new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(sideWallPointA, sideWallPointB, sideWallPointC, sideWallPointD)));

      planarRegions.add(groundPlaneRegion);
      planarRegions.add(frontWallRegion);
      planarRegions.add(backWallRegion);
      planarRegions.add(sideWallRegion);

      return planarRegions;
   }

   private static List<PlanarRegion> createFlatGroundWithBoxesEnvironment()
   {
      List<PlanarRegion> planarRegions = new ArrayList<>();

      // set up ground plane, 20 x 10
      Point2D groundPlanePointA = new Point2D(10.0, -5.0);
      Point2D groundPlanePointB = new Point2D(10.0, 5.0);
      Point2D groundPlanePointC = new Point2D(-10.0, 5.0);
      Point2D groundPlanePointD = new Point2D(-10.0, -5.0);

      RigidBodyTransform groundTransform = new RigidBodyTransform();
      groundTransform.setTranslation(-10.0, 0.0, 0.0);
      PlanarRegion groundPlaneRegion = new PlanarRegion(groundTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(groundPlanePointA, groundPlanePointB, groundPlanePointC, groundPlanePointD)));

      // set up wall, 5x2
      Point2D frontWallPointA = new Point2D(2.0, 0.0);
      Point2D frontWallPointB = new Point2D(0.0, 0.0);
      Point2D frontWallPointC = new Point2D(2.0, 4.5);
      Point2D frontWallPointD = new Point2D(0.0, 4.5);

      RigidBodyTransform frontLeftWallTransform = new RigidBodyTransform();
      frontLeftWallTransform.setTranslation(-11.5, 0.5, 0.0);
      frontLeftWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion frontLeftWallRegion = new PlanarRegion(frontLeftWallTransform,
                                                      new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(frontWallPointA, frontWallPointB, frontWallPointC, frontWallPointD)));

      RigidBodyTransform frontRightWallTransform = new RigidBodyTransform();
      frontRightWallTransform.setTranslation(-11.5, -5.0, 0.0);
      frontRightWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion frontRightWallRegion = new PlanarRegion(frontRightWallTransform,
                                                          new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(frontWallPointA, frontWallPointB, frontWallPointC, frontWallPointD)));

      // set up wall, 5x2
      Point2D backWallPointA = new Point2D(2.0, 0.0);
      Point2D backWallPointB = new Point2D(0.0, 0.0);
      Point2D backWallPointC = new Point2D(2.0, 4.25);
      Point2D backWallPointD = new Point2D(0.0, 4.25);


      RigidBodyTransform backLeftWallTransform = new RigidBodyTransform();
      backLeftWallTransform.setTranslation(-8.5, 0.75, 0.0);
      backLeftWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion backLeftWallRegion = new PlanarRegion(backLeftWallTransform,
                                                     new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(backWallPointA, backWallPointB, backWallPointC, backWallPointD)));

      RigidBodyTransform backRightWallTransform = new RigidBodyTransform();
      backRightWallTransform.setTranslation(-8.5, -5.0, 0.0);
      backRightWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion backRightWallRegion = new PlanarRegion(backRightWallTransform,
                                                         new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(backWallPointA, backWallPointB, backWallPointC, backWallPointD)));

      Point2D sideWallPointA = new Point2D(2.0, 0.0);
      Point2D sideWallPointB = new Point2D(0.0, 0.0);
      Point2D sideWallPointC = new Point2D(2.0, 3.0);
      Point2D sideWallPointD = new Point2D(0.0, 3.0);

      RigidBodyTransform leftSideWallTransform = new RigidBodyTransform();
      leftSideWallTransform.setTranslation(-11.5, 0.625, 0.0);
      leftSideWallTransform.setRotationYawPitchRoll(-Math.PI / 2.0, -Math.PI / 2.0, 0.0);
      PlanarRegion leftSideWallRegion = new PlanarRegion(leftSideWallTransform,
                                                     new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(sideWallPointA, sideWallPointB, sideWallPointC, sideWallPointD)));

      planarRegions.add(groundPlaneRegion);
      planarRegions.add(frontLeftWallRegion);
      planarRegions.add(backLeftWallRegion);
      planarRegions.add(frontRightWallRegion);
      planarRegions.add(backRightWallRegion);
      //      planarRegions.add(leftSideWallRegion);

      return planarRegions;
   }

   private static List<PlanarRegion> createFlatGroundTwoDifferentWidthWallsEnvironment()
   {
      List<PlanarRegion> planarRegions = new ArrayList<>();

      // set up ground plane, 20 x 10
      Point2D groundPlanePointA = new Point2D(10.0, -5.0);
      Point2D groundPlanePointB = new Point2D(10.0, 5.0);
      Point2D groundPlanePointC = new Point2D(-10.0, 5.0);
      Point2D groundPlanePointD = new Point2D(-10.0, -5.0);

      RigidBodyTransform groundTransform = new RigidBodyTransform();
      groundTransform.setTranslation(-10.0, 0.0, 0.0);
      PlanarRegion groundPlaneRegion = new PlanarRegion(groundTransform, new ConvexPolygon2D(
            Vertex2DSupplier.asVertex2DSupplier(groundPlanePointA, groundPlanePointB, groundPlanePointC, groundPlanePointD)));

      // set up wall, 5x2
      Point2D wallPointA = new Point2D(2.0, 0.0);
      Point2D wallPointB = new Point2D(0.0, 0.0);
      Point2D wallPointC = new Point2D(2.0, 5.0);
      Point2D wallPointD = new Point2D(0.0, 5.0);

      RigidBodyTransform wallTransform = new RigidBodyTransform();
      wallTransform.setTranslation(-8.5, 0.0, 0.0);
      wallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion wallRegion = new PlanarRegion(wallTransform,
                                                 new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(wallPointA, wallPointB, wallPointC, wallPointD)));

      Point2D otherWallPointA = new Point2D(2.0, 0.0);
      Point2D otherWallPointB = new Point2D(0.0, 0.0);
      Point2D otherWallPointC = new Point2D(2.0, 4.5);
      Point2D otherWallPointD = new Point2D(0.0, 4.5);

      RigidBodyTransform otherWallTransform = new RigidBodyTransform();
      otherWallTransform.setTranslation(-11.5, 0.5, 0.0);
      otherWallTransform.setRotationPitch(-Math.PI / 2.0);
      PlanarRegion otherWallRegion = new PlanarRegion(otherWallTransform,
                                                 new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(otherWallPointA, otherWallPointB, otherWallPointC,
                                                                                                         otherWallPointD)));

      planarRegions.add(groundPlaneRegion);
      planarRegions.add(wallRegion);
      planarRegions.add(otherWallRegion);

      return planarRegions;
   }

   private static void visualize(List<Point3DReadOnly> path, PlanarRegionsList planarRegionsList, Point3D start, Point3D goal)
   {
      if (!visualize || ContinuousIntegrationTools.isRunningOnContinuousIntegrationServer())
         return;

      SimulationConstructionSet scs = new SimulationConstructionSet();

      Graphics3DObject graphics3DObject = new Graphics3DObject();
      Graphics3DObjectTools.addPlanarRegionsList(graphics3DObject, planarRegionsList, YoAppearance.White(), YoAppearance.Grey(), YoAppearance.DarkGray());
      scs.setGroundVisible(false);

      graphics3DObject.identity();
      graphics3DObject.translate(start);
      graphics3DObject.translate(0.0, 0.0, 0.05);
      graphics3DObject.addCone(0.3, 0.05, YoAppearance.Blue());

      graphics3DObject.identity();
      graphics3DObject.translate(goal);
      graphics3DObject.translate(0.0, 0.0, 0.05);
      graphics3DObject.addCone(0.3, 0.05, YoAppearance.Red());

      if (path != null)
      {
         for (int i = 0; i < path.size(); i++)
         {
            Point3DReadOnly point = path.get(i);

            graphics3DObject.identity();
            graphics3DObject.translate(point);
            graphics3DObject.addSphere(0.1, YoAppearance.Orange());

            if (i != path.size() - 1)
            {
               Point3DReadOnly nextPoint = path.get(i + 1);
               Vector3D direction = new Vector3D(nextPoint);
               direction.sub(point);
               int pathPoints = (int) Math.round(point.distance(nextPoint) / 0.05);

               for (int j = 1; j < pathPoints; j++)
               {
                  Vector3D offset = new Vector3D(direction);
                  offset.scaleAdd(((double) j) / pathPoints, point);

                  graphics3DObject.identity();
                  graphics3DObject.translate(offset);
                  graphics3DObject.addSphere(0.025, YoAppearance.Orange());
               }
            }
         }
      }

      scs.addStaticLinkGraphics(graphics3DObject);

      scs.setCameraPosition(-7.0, -1.0, 25.0);
      scs.setCameraFix(0.0, 0.0, 0.0);
      scs.startOnAThread();

      ThreadTools.sleepForever();
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
               public double computeNavigableExtrusionDistance(PlanarRegion navigableRegionToBeExtruded)
               {
                  return 0.01;
               }
            };
         }
      };
   }

}
