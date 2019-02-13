package us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.nodeChecking;

import us.ihmc.commons.MathTools;
import us.ihmc.commons.PrintTools;
import us.ihmc.euclid.axisAngle.AxisAngle;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.LineSegment3D;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.QuadrupedFootstepPlannerNodeRejectionReason;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapData;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapper;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;

import java.util.List;

public class SnapBasedNodeChecker extends FootstepNodeChecker
{
   private static final boolean DEBUG = false;

   private final FootstepPlannerParameters parameters;
   private final FootstepNodeSnapper snapper;

   public SnapBasedNodeChecker(FootstepPlannerParameters parameters, FootstepNodeSnapper snapper)
   {
      this.parameters = parameters;
      this.snapper = snapper;
   }

   @Override
   public void setPlanarRegions(PlanarRegionsList planarRegions)
   {
      super.setPlanarRegions(planarRegions);
      snapper.setPlanarRegions(planarRegions);
   }

   @Override
   public boolean isNodeValid(FootstepNode node, FootstepNode previousNode)
   {
      if (previousNode != null && node.completelyEquals(previousNode))
      {
         throw new IllegalArgumentException("Checking node assuming it is following itself.");
      }

      FootstepNodeSnapData snapData = snapper.snapFootstepNode(node);
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         RigidBodyTransform snapTransform = snapData.getSnapTransform(robotQuadrant);
         if (snapTransform.containsNaN())
         {
            if (DEBUG)
            {
               PrintTools.debug("Was not able to snap node:\n" + node);
            }
            rejectNode(node, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.COULD_NOT_SNAP);
            return false;
         }
      }

      if (previousNode == null)
      {
         return true;
      }

      RobotQuadrant movingQuadrant = node.getMovingQuadrant();

      Vector2D clearanceVector = new Vector2D(parameters.getMinXClearanceFromFoot(), parameters.getMinYClearanceFromFoot());
      AxisAngle previousOrientation = new AxisAngle(previousNode.getNominalYaw(), 0.0, 0.0);
      previousOrientation.transform(clearanceVector);

      if (MathTools.epsilonEquals(node.getX(movingQuadrant), previousNode.getX(movingQuadrant), Math.abs(clearanceVector.getX())) && MathTools
            .epsilonEquals(node.getY(movingQuadrant), previousNode.getY(movingQuadrant), Math.abs(clearanceVector.getY())))
      {
         if (DEBUG)
         {
            PrintTools.info("The node " + node + " is trying to step in place.");
         }
         rejectNode(node, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_IN_PLACE);
         return false;
      }

      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         if (robotQuadrant == movingQuadrant)
            continue;

         if (MathTools.epsilonEquals(node.getX(movingQuadrant), previousNode.getX(robotQuadrant), Math.abs(clearanceVector.getX())) && MathTools
               .epsilonEquals(node.getY(movingQuadrant), previousNode.getY(robotQuadrant), Math.abs(clearanceVector.getY())))
         {
            if (DEBUG)
            {
               PrintTools.info("The node " + node + " is stepping on another foot.");
            }
            rejectNode(node, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_ON_OTHER_FOOT);
            return false;
         }
      }

      FootstepNodeSnapData previousNodeSnapData = snapper.snapFootstepNode(previousNode);

      RigidBodyTransform previousFootSnapTransform = previousNodeSnapData.getSnapTransform(movingQuadrant);
      RigidBodyTransform snapTransform = snapData.getSnapTransform(movingQuadrant);

      Point3D newStepPosition = new Point3D(node.getX(movingQuadrant), node.getY(movingQuadrant), 0.0);
      Point3D previousStepPosition = new Point3D(previousNode.getX(movingQuadrant), previousNode.getY(movingQuadrant), 0.0);

      snapTransform.transform(newStepPosition);
      previousFootSnapTransform.transform(previousStepPosition);

      double heightChange = Math.abs(newStepPosition.getZ() - previousStepPosition.getZ());
      if (heightChange > parameters.getMaximumStepChangeZ())
      {
         if (DEBUG)
         {
            PrintTools.debug("Too much height difference (" + Math.round(100.0 * heightChange) + "cm) to previous node:\n" + node);
         }
         rejectNode(node, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_TOO_HIGH_OR_LOW);
         return false;
      }

      if (hasPlanarRegions() && isObstacleBetweenSteps(newStepPosition, previousStepPosition, planarRegionsList.getPlanarRegionsAsList(),
                                                       parameters.getBodyGroundClearance()))
      {
         if (DEBUG)
         {
            PrintTools.debug("Found an obstacle between the nodes " + node + " and " + previousNode);
         }
         rejectNode(node, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.OBSTACLE_BLOCKING_STEP);
         return false;
      }

      if (hasPlanarRegions() && isObstacleBetweenFeet(newStepPosition, movingQuadrant, previousNode, previousNodeSnapData,
                                                      planarRegionsList.getPlanarRegionsAsList(), parameters.getBodyGroundClearance()))
      {
         if (DEBUG)
         {
            PrintTools.debug("Found an obstacle between the nodes " + node + " and " + previousNode);
         }
         rejectNode(node, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.OBSTACLE_BLOCKING_BODY);
         return false;
      }

      return true;
   }

   /**
    * This is meant to test if there is a wall that the body of the robot would run into when shifting
    * from one step to the next. It is not meant to eliminate swing overs.
    */
   private static boolean isObstacleBetweenSteps(Point3DReadOnly footPosition, Point3DReadOnly previousFootPosition, List<PlanarRegion> planarRegions,
                                                 double groundClearance)
   {
      PlanarRegion bodyPath = createBodyCollisionRegionFromTwoFeet(footPosition, previousFootPosition, groundClearance, 2.0);

      for (PlanarRegion region : planarRegions)
      {
         List<LineSegment3D> intersections = region.intersect(bodyPath);
         if (!intersections.isEmpty())
         {
            return true;
         }
      }

      return false;
   }

   /**
    * This is meant to test if there is a wall that the body of the robot would run into when shifting
    * from one step to the next. It is not meant to eliminate swing overs.
    */
   private static boolean isObstacleBetweenFeet(Point3DReadOnly newFootPosition, RobotQuadrant newFootQuadrant, FootstepNode previousNode,
                                                FootstepNodeSnapData previousNodeSnapData, List<PlanarRegion> planarRegions, double groundClearance)
   {
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         if (robotQuadrant == newFootQuadrant)
            continue;

         RigidBodyTransform previousFootSnapTransform = previousNodeSnapData.getSnapTransform(robotQuadrant);
         Point3D previousFootPosition = new Point3D(previousNode.getX(robotQuadrant), previousNode.getY(robotQuadrant), 0.0);
         previousFootSnapTransform.transform(previousFootPosition);

         PlanarRegion bodyPath = createBodyCollisionRegionFromTwoFeet(newFootPosition, previousFootPosition, groundClearance, 2.0);

         for (PlanarRegion region : planarRegions)
         {
            List<LineSegment3D> intersections = region.intersect(bodyPath);
            if (!intersections.isEmpty())
            {
               return true;
            }
         }
      }

      return false;
   }

   /**
    * Given two footstep positions this will create a vertical planar region above the points. The region
    * will be aligned with the vector connecting the nodes. It's lower edge will be the specified
    * distance above the higher of the two nodes and the plane will have the specified height.
    */
   public static PlanarRegion createBodyCollisionRegionFromTwoFeet(Point3DReadOnly footA, Point3DReadOnly footB, double clearance, double height)
   {
      if (footA.epsilonEquals(footB, 1e-3))
         throw new RuntimeException("These points should be different.");

      double lowerZ = Math.max(footA.getZ(), footB.getZ()) + clearance;
      Point3D point0 = new Point3D(footA.getX(), footA.getY(), lowerZ);
      Point3D point1 = new Point3D(footA.getX(), footA.getY(), lowerZ + height);
      Point3D point2 = new Point3D(footB.getX(), footB.getY(), lowerZ);
      Point3D point3 = new Point3D(footB.getX(), footB.getY(), lowerZ + height);

      Vector3D xAxisInPlane = new Vector3D();
      xAxisInPlane.sub(point2, point0);
      xAxisInPlane.normalize();
      Vector3D yAxisInPlane = new Vector3D(0.0, 0.0, 1.0);
      Vector3D zAxis = new Vector3D();
      zAxis.cross(xAxisInPlane, yAxisInPlane);

      RigidBodyTransform transform = new RigidBodyTransform();
      transform.setRotation(xAxisInPlane.getX(), xAxisInPlane.getY(), xAxisInPlane.getZ(), yAxisInPlane.getX(), yAxisInPlane.getY(), yAxisInPlane.getZ(),
                            zAxis.getX(), zAxis.getY(), zAxis.getZ());
      transform.setTranslation(point0);
      transform.invertRotation();

      point0.applyInverseTransform(transform);
      point1.applyInverseTransform(transform);
      point2.applyInverseTransform(transform);
      point3.applyInverseTransform(transform);

      ConvexPolygon2D polygon = new ConvexPolygon2D();
      polygon.addVertex(point0.getX(), point0.getY());
      polygon.addVertex(point1.getX(), point1.getY());
      polygon.addVertex(point2.getX(), point2.getY());
      polygon.addVertex(point3.getX(), point3.getY());
      polygon.update();

      return new PlanarRegion(transform, polygon);
   }

   @Override
   public void addStartNode(FootstepNode startNode, QuadrantDependentList<RigidBodyTransform> startNodeTransforms)
   {
      snapper.addSnapData(startNode, new FootstepNodeSnapData(startNodeTransforms));
   }
}
