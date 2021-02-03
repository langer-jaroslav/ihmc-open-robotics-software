package us.ihmc.commonWalkingControlModules.dynamicPlanning.bipedPlanning;

import us.ihmc.commons.InterpolationTools;
import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameVector3DReadOnly;
import us.ihmc.robotics.math.trajectories.generators.MultipleWaypointsPoseTrajectoryGenerator;
import us.ihmc.robotics.math.trajectories.generators.MultipleWaypointsPositionTrajectoryGenerator;
import us.ihmc.robotics.math.trajectories.trajectorypoints.SE3TrajectoryPoint;
import us.ihmc.robotics.math.trajectories.trajectorypoints.YoFrameEuclideanTrajectoryPoint;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.yoVariables.parameters.DoubleParameter;
import us.ihmc.yoVariables.providers.DoubleProvider;
import us.ihmc.yoVariables.registry.YoRegistry;

import java.util.List;

import static us.ihmc.commonWalkingControlModules.dynamicPlanning.comPlanning.CoMTrajectoryPlannerTools.sufficientlyLongTime;

public class FootTrajectoryPredictor
{
   private static final double defaultSwingHeight = 0.15;
   private static final double defaultPredictorWaypointProportion = 0.25;

   private final YoRegistry registry = new YoRegistry(getClass().getSimpleName());

   private final MultipleWaypointsPositionTrajectoryGenerator leftFootTrajectory = new MultipleWaypointsPositionTrajectoryGenerator("leftFootPredictedTrajectory", ReferenceFrame.getWorldFrame(), registry);
   private final MultipleWaypointsPositionTrajectoryGenerator rightFootTrajectory = new MultipleWaypointsPositionTrajectoryGenerator("rightFootPredictedTrajectory", ReferenceFrame.getWorldFrame(), registry);
   private final SideDependentList<MultipleWaypointsPositionTrajectoryGenerator> footTrajectories = new SideDependentList<>(leftFootTrajectory, rightFootTrajectory);

   private final DoubleProvider predictorSwingHeight = new DoubleParameter("predictorSwingHeight", registry, defaultSwingHeight);
   private final DoubleProvider predictorWaypointProportion = new DoubleParameter("predictorWaypointProportion", registry, defaultPredictorWaypointProportion);

   private final RecyclingArrayList<SE3TrajectoryPoint> swingWaypoints = new RecyclingArrayList<>(SE3TrajectoryPoint::new);

   public FootTrajectoryPredictor(YoRegistry parentRegistry)
   {
      parentRegistry.addChild(registry);
   }

   public void setSwingTrajectory(MultipleWaypointsPoseTrajectoryGenerator swingTrajectory)
   {
      if (swingTrajectory == null)
         return;

      swingWaypoints.clear();
      for (int i = 0; i < swingTrajectory.getPositionTrajectory().getCurrentNumberOfWaypoints(); i++)
      {
         swingWaypoints.add().set(swingTrajectory.getPositionTrajectory().getWaypoint(i));
      }
   }

   public void clearSwingTrajectory()
   {
      swingWaypoints.clear();
   }

   public void compute(CoPTrajectoryGeneratorState state)
   {
      if (state.getNumberOfFootstep() > 0)
         computeWalking(state);
      else
         computeStanding(state);
   }

   private final FrameVector3DReadOnly zeroVector = new FrameVector3D();

   private void computeStanding(CoPTrajectoryGeneratorState state)
   {
      swingWaypoints.clear();

      for (RobotSide robotSide : RobotSide.values)
      {
         MultipleWaypointsPositionTrajectoryGenerator footTrajectory = footTrajectories.get(robotSide);
         footTrajectory.clear();
         footTrajectory.appendWaypoint(0.0, state.getFootPose(robotSide).getPosition(), zeroVector);
         footTrajectory.appendWaypoint(sufficientlyLongTime, state.getFootPose(robotSide).getPosition(), zeroVector);
         footTrajectory.initialize();
      }
   }

   private void computeWalking(CoPTrajectoryGeneratorState state)
   {
      PlanningTiming timing = state.getTiming(0);
      DynamicPlanningFootstep footstep = state.getFootstep(0);

      double transferDuration = Math.min(timing.getTransferTime(), sufficientlyLongTime);
      double swingDuration = Math.min(timing.getSwingTime(), sufficientlyLongTime);

      for (RobotSide robotSide : RobotSide.values)
      {
         MultipleWaypointsPositionTrajectoryGenerator footTrajectory = footTrajectories.get(robotSide);
         footTrajectory.clear();
         footTrajectory.appendWaypoint(0.0, state.getFootPose(robotSide).getPosition(), zeroVector);
         footTrajectory.appendWaypoint(transferDuration, state.getFootPose(robotSide).getPosition(), zeroVector);
      }

      RobotSide swingSide = footstep.getRobotSide();
      RobotSide stanceSide = swingSide.getOppositeSide();

      footTrajectories.get(stanceSide)
                      .appendWaypoint(transferDuration + swingDuration, state.getFootPose(stanceSide).getPosition(), zeroVector);

      if (swingWaypoints.isEmpty())
      {
         predictSwingFootTrajectory(transferDuration,
                                    transferDuration + swingDuration,
                                    predictorSwingHeight.getValue(),
                                    state.getFootPose(swingSide).getPosition(),
                                    footstep.getFootstepPose().getPosition(),
                                    footTrajectories.get(swingSide));
      }
      else
      {
         setSwingFootTrajectory(swingWaypoints, footTrajectories.get(swingSide));
      }

      leftFootTrajectory.initialize();
      rightFootTrajectory.initialize();
   }

   public MultipleWaypointsPositionTrajectoryGenerator getPredictedLeftFootTrajectories()
   {
      return leftFootTrajectory;
   }

   public MultipleWaypointsPositionTrajectoryGenerator getPredictedRightFootTrajectories()
   {
      return rightFootTrajectory;
   }

   private final FramePoint3D midpoint1 = new FramePoint3D();
   private final FramePoint3D midpoint2 = new FramePoint3D();

   private final FrameVector3D velocityVector1 = new FrameVector3D();
   private final FrameVector3D velocityVector2 = new FrameVector3D();

   void predictSwingFootTrajectory(double startTime,
                                   double endTime,
                                   double swingHeight,
                                   FramePoint3DReadOnly startPosition,
                                   FramePoint3DReadOnly endPosition,
                                   MultipleWaypointsPositionTrajectoryGenerator trajectoryToPack)
   {
      double time1 = InterpolationTools.linearInterpolate(startTime, endTime, predictorWaypointProportion.getValue());
      midpoint1.interpolate(startPosition, endPosition, predictorWaypointProportion.getValue());
      midpoint1.addZ(swingHeight);

      double time2 = InterpolationTools.linearInterpolate(endTime, startTime, predictorWaypointProportion.getValue());
      midpoint2.interpolate(endPosition, startPosition, predictorWaypointProportion.getValue());
      midpoint2.addZ(swingHeight);

      velocityVector1.sub(midpoint2, startPosition);
      velocityVector1.scale(1.0 / (time2 - startTime));

      velocityVector2.sub(endPosition, midpoint1);
      velocityVector2.scale(1.0 / (endTime - time1));

      trajectoryToPack.appendWaypoint(time1, midpoint1, velocityVector1);
      trajectoryToPack.appendWaypoint(time2, midpoint2, velocityVector2);
      trajectoryToPack.appendWaypoint(endTime, endPosition, zeroVector);
   }

   private static void setSwingFootTrajectory(List<SE3TrajectoryPoint> swingWaypoints,
                                              MultipleWaypointsPositionTrajectoryGenerator trajectoriesToPack)
   {

      double timeShift = trajectoriesToPack.getLastWaypointTime() - swingWaypoints.get(0).getTime();
      for (int waypointIdx = 0; waypointIdx < swingWaypoints.size(); waypointIdx++)
      {
         SE3TrajectoryPoint waypoint = swingWaypoints.get(waypointIdx);
         trajectoriesToPack.appendWaypoint(waypoint.getTime() + timeShift, waypoint.getPosition(), waypoint.getLinearVelocity());
      }
   }
}
