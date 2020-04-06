package us.ihmc.footstepPlanning;

import controller_msgs.msg.dds.BodyPathPlanMessage;
import us.ihmc.commons.MathTools;
import us.ihmc.commons.time.Stopwatch;
import us.ihmc.communication.packets.PlanarRegionMessageConverter;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.footstepPlanning.graphSearch.VisibilityGraphPathPlanner;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.SimplePlanarRegionFootstepNodeSnapper;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.nodeChecking.FootstepNodeChecker;
import us.ihmc.footstepPlanning.graphSearch.parameters.DefaultFootstepPlannerParameters;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersBasics;
import us.ihmc.footstepPlanning.log.FootstepPlannerEdgeData;
import us.ihmc.footstepPlanning.log.FootstepPlannerIterationData;
import us.ihmc.footstepPlanning.simplePlanners.PlanThenSnapPlanner;
import us.ihmc.footstepPlanning.tools.PlannerTools;
import us.ihmc.log.LogTools;
import us.ihmc.pathPlanning.bodyPathPlanner.WaypointDefinedBodyPathPlanHolder;
import us.ihmc.pathPlanning.graph.search.AStarIterationData;
import us.ihmc.pathPlanning.graph.search.AStarPathPlanner;
import us.ihmc.pathPlanning.graph.structure.GraphEdge;
import us.ihmc.pathPlanning.visibilityGraphs.parameters.DefaultVisibilityGraphParameters;
import us.ihmc.pathPlanning.visibilityGraphs.parameters.VisibilityGraphsParametersBasics;
import us.ihmc.pathPlanning.visibilityGraphs.postProcessing.BodyPathPostProcessor;
import us.ihmc.pathPlanning.visibilityGraphs.postProcessing.ObstacleAvoidanceProcessor;
import us.ihmc.pathPlanning.visibilityGraphs.tools.BodyPathPlan;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.ros2.Ros2Node;
import us.ihmc.tools.thread.CloseableAndDisposable;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class FootstepPlanningModule implements CloseableAndDisposable
{
   private final String name;
   private Ros2Node ros2Node;
   private final FootstepPlannerParametersBasics footstepPlannerParameters;
   private final VisibilityGraphsParametersBasics visibilityGraphParameters;
   private BodyPathPlanMessage bodyPathPlanMessage;

   private final PlanThenSnapPlanner planThenSnapPlanner;
   private final AStarFootstepPlanner aStarFootstepPlanner;
   private final VisibilityGraphPathPlanner bodyPathPlanner;
   private final WaypointDefinedBodyPathPlanHolder bodyPathPlanHolder = new WaypointDefinedBodyPathPlanHolder();

   private final AtomicBoolean isPlanning = new AtomicBoolean();
   private final FootstepPlannerRequest request = new FootstepPlannerRequest();
   private final FootstepPlannerOutput output = new FootstepPlannerOutput();
   private final Stopwatch stopwatch = new Stopwatch();

   private final FramePose3D startMidFootPose = new FramePose3D();
   private final FramePose3D goalMidFootPose = new FramePose3D();

   private Consumer<FootstepPlannerRequest> requestCallback = request -> {};
   private Consumer<BodyPathPlanMessage> bodyPathResultCallback = bodyPathPlanMessage -> {};
   private Consumer<FootstepPlannerOutput> statusCallback = result -> {};

   public FootstepPlanningModule(String name)
   {
      this(name, new DefaultFootstepPlannerParameters(), new DefaultVisibilityGraphParameters(), PlannerTools.createDefaultFootPolygons());
   }

   public FootstepPlanningModule(String name,
                                 FootstepPlannerParametersBasics footstepPlannerParameters,
                                 VisibilityGraphsParametersBasics visibilityGraphParameters,
                                 SideDependentList<ConvexPolygon2D> footPolygons)
   {
      this.name = name;
      this.footstepPlannerParameters = footstepPlannerParameters;
      this.visibilityGraphParameters = visibilityGraphParameters;

      BodyPathPostProcessor pathPostProcessor = new ObstacleAvoidanceProcessor(visibilityGraphParameters);
      this.bodyPathPlanner = new VisibilityGraphPathPlanner(visibilityGraphParameters,
                                                            pathPostProcessor,
                                                            new YoVariableRegistry(getClass().getSimpleName()));

      this.planThenSnapPlanner = new PlanThenSnapPlanner(footstepPlannerParameters, footPolygons);
      this.aStarFootstepPlanner = new AStarFootstepPlanner(footstepPlannerParameters, footPolygons, bodyPathPlanHolder);

      addBodyPathPlanCallback(bodyPathMessage -> output.getPlannerTimings().setTimePlanningBodyPathSeconds(stopwatch.lap()));
      addStatusCallback(output -> output.getPlannerTimings().setTimePlanningStepsSeconds(stopwatch.lapElapsed()));
      addStatusCallback(output -> output.getPlannerTimings().setTotalElapsedSeconds(stopwatch.totalElapsed()));
      aStarFootstepPlanner.addIterationCallback(iterationData -> output.getPlannerTimings().setStepPlanningIterations(aStarFootstepPlanner.getIterations()));
   }

   public FootstepPlannerOutput handleRequest(FootstepPlannerRequest request)
   {
      if (isPlanning.get())
      {
         LogTools.info("Received planning request packet but planner is currently running");
         return null;
      }

      stopwatch.start();
      output.clear();

      try
      {
         handleRequestInternal(request);
         return output;
      }
      catch (Exception exception)
      {
         exception.printStackTrace();

         output.clear();
         output.setPlanId(request.getRequestId());
         output.setFootstepPlanningResult(FootstepPlanningResult.EXCEPTION);
         output.setException(exception);
         statusCallback.accept(output);
         return output;
      }
   }

   private void handleRequestInternal(FootstepPlannerRequest request) throws Exception
   {
      this.request.set(request);
      requestCallback.accept(request);
      output.setPlanId(request.getRequestId());
      isPlanning.set(true);
      bodyPathPlanMessage = new BodyPathPlanMessage();
      bodyPathPlanHolder.getPlan().clear();

      startMidFootPose.interpolate(request.getStartFootPoses().get(RobotSide.LEFT), request.getStartFootPoses().get(RobotSide.RIGHT), 0.5);
      goalMidFootPose.interpolate(request.getGoalFootPoses().get(RobotSide.LEFT), request.getGoalFootPoses().get(RobotSide.RIGHT), 0.5);

      // Update planar regions
      boolean flatGroundMode = request.getAssumeFlatGround() || request.getPlanarRegionsList() == null || request.getPlanarRegionsList().isEmpty();
      PlanarRegionsList planarRegionsList = flatGroundMode ? null : request.getPlanarRegionsList();
      bodyPathPlanner.setPlanarRegionsList(planarRegionsList);

      // record time
      output.getPlannerTimings().setTimeBeforePlanningSeconds(stopwatch.lap());

      if (request.getPlanBodyPath() && !flatGroundMode)
      {
         bodyPathPlanner.setStanceFootPoses(request.getStartFootPoses().get(RobotSide.LEFT), request.getStartFootPoses().get(RobotSide.RIGHT));
         bodyPathPlanner.setGoal(goalMidFootPose);

         BodyPathPlanningResult bodyPathPlannerResult = bodyPathPlanner.planWaypoints();
         List<Pose3DReadOnly> waypoints = bodyPathPlanner.getWaypoints();

         if (!bodyPathPlannerResult.validForExecution() || (waypoints.size() < 2 && !footstepPlannerParameters.getReturnBestEffortPlan()))
         {
            reportBodyPathPlan(bodyPathPlannerResult);
            output.setBodyPathPlanningResult(bodyPathPlannerResult);
            statusCallback.accept(output);
            isPlanning.set(false);
            return;
         }
         else if (waypoints.size() < 2 && footstepPlannerParameters.getReturnBestEffortPlan())
         {
            double horizonLength = Double.POSITIVE_INFINITY;
            bodyPathPlanner.computeBestEffortPlan(horizonLength);
         }

         bodyPathPlanHolder.setPoseWaypoints(waypoints);
         double pathLength = bodyPathPlanHolder.computePathLength(0.0);
         if (MathTools.intervalContains(request.getHorizonLength(), 0.0, pathLength))
         {
            double alphaIntermediateGoal = request.getHorizonLength() / pathLength;
            bodyPathPlanHolder.getPointAlongPath(alphaIntermediateGoal, goalMidFootPose);
         }

         reportBodyPathPlan(BodyPathPlanningResult.FOUND_SOLUTION);
      }
      else
      {
         List<Pose3DReadOnly> waypoints = new ArrayList<>();
         waypoints.add(startMidFootPose);
         waypoints.addAll(request.getBodyPathWaypoints());
         waypoints.add(goalMidFootPose);

         bodyPathPlanHolder.setPoseWaypoints(waypoints);
         reportBodyPathPlan(BodyPathPlanningResult.FOUND_SOLUTION);
      }

      if (request.getPerformAStarSearch())
      {
         aStarFootstepPlanner.setStatusCallback(statusCallback);
         aStarFootstepPlanner.handleRequest(request, output);
      }
      else
      {
         RobotSide initialStanceSide = request.getRequestedInitialStanceSide();
         FramePose3D initialStancePose = new FramePose3D(request.getStartFootPoses().get(initialStanceSide));
         planThenSnapPlanner.setInitialStanceFoot(initialStancePose, initialStanceSide);
         planThenSnapPlanner.setPlanarRegions(planarRegionsList);

         FootstepPlannerGoal goal = new FootstepPlannerGoal();
         goal.setGoalPoseBetweenFeet(goalMidFootPose);
         planThenSnapPlanner.setGoal(goal);

         FootstepPlanningResult result = planThenSnapPlanner.plan();

         FootstepPlan plan = planThenSnapPlanner.getPlan();
         output.setFootstepPlanningResult(result);
         output.getFootstepPlan().clear();
         for (int i = 0; i < plan.getNumberOfSteps(); i++)
         {
            output.getFootstepPlan().addFootstep(plan.getFootstep(i));
         }

         output.getPlannerTimings().setTimePlanningStepsSeconds(stopwatch.lap());
         statusCallback.accept(output);
      }

      isPlanning.set(false);
   }

   private void reportBodyPathPlan(BodyPathPlanningResult result)
   {
      BodyPathPlan bodyPathPlan = bodyPathPlanHolder.getPlan();
      for (int i = 0; i < bodyPathPlan.getNumberOfWaypoints(); i++)
      {
         bodyPathPlanMessage.getBodyPath().add().set(bodyPathPlan.getWaypoint(i));
      }

      bodyPathPlanMessage.getPathPlannerStartPose().set(bodyPathPlan.getStartPose());
      bodyPathPlanMessage.getPathPlannerGoalPose().set(bodyPathPlan.getGoalPose());
      bodyPathPlanMessage.setPlanId(request.getRequestId());
      bodyPathPlanMessage.setFootstepPlanningResult(result.toByte());
      if (request.getPlanarRegionsList() != null)
      {
         bodyPathPlanMessage.getPlanarRegionsList().set(PlanarRegionMessageConverter.convertToPlanarRegionsListMessage(request.getPlanarRegionsList()));
      }

      bodyPathResultCallback.accept(bodyPathPlanMessage);
   }

   public void addRequestCallback(Consumer<FootstepPlannerRequest> callback)
   {
      requestCallback = requestCallback.andThen(callback);
   }

   public void addIterationCallback(Consumer<AStarIterationData<FootstepNode>> callback)
   {
      aStarFootstepPlanner.addIterationCallback(callback);
   }

   public void addBodyPathPlanCallback(Consumer<BodyPathPlanMessage> callback)
   {
      bodyPathResultCallback = bodyPathResultCallback.andThen(callback);
   }

   public void addStatusCallback(Consumer<FootstepPlannerOutput> callback)
   {
      statusCallback = statusCallback.andThen(callback);
   }

   public boolean registerRosNode(Ros2Node ros2Node)
   {
      if (this.ros2Node != null)
         return false;
      this.ros2Node = ros2Node;
      return true;
   }

   public void halt()
   {
      aStarFootstepPlanner.halt();
   }

   public String getName()
   {
      return name;
   }

   public boolean isPlanning()
   {
      return isPlanning.get();
   }

   public FootstepPlannerRequest getRequest()
   {
      return request;
   }

   public FootstepPlannerOutput getOutput()
   {
      return output;
   }

   public FootstepPlannerParametersBasics getFootstepPlannerParameters()
   {
      return footstepPlannerParameters;
   }

   public VisibilityGraphsParametersBasics getVisibilityGraphParameters()
   {
      return visibilityGraphParameters;
   }

   public SimplePlanarRegionFootstepNodeSnapper getSnapper()
   {
      return aStarFootstepPlanner.getSnapper();
   }

   public FootstepNodeChecker getChecker()
   {
      return aStarFootstepPlanner.getChecker();
   }

   public AStarPathPlanner<FootstepNode> getLowLevelStepPlanner()
   {
      return aStarFootstepPlanner.getLowLevelStepPlanner();
   }

   public VisibilityGraphPathPlanner getBodyPathPlanner()
   {
      return bodyPathPlanner;
   }

   public BodyPathPlan getBodyPathPlan()
   {
      return bodyPathPlanHolder.getBodyPathPlan();
   }

   public FootstepNode getEndNode()
   {
      return aStarFootstepPlanner.getEndNode();
   }

   public HashMap<GraphEdge<FootstepNode>, FootstepPlannerEdgeData> getEdgeDataMap()
   {
      return aStarFootstepPlanner.getEdgeDataMap();
   }

   public List<FootstepPlannerIterationData> getIterationData()
   {
      return aStarFootstepPlanner.getIterationData();
   }

   public void setStatusPublishPeriod(double statusPublishPeriod)
   {
      this.aStarFootstepPlanner.setStatusPublishPeriod(statusPublishPeriod);
   }

   @Override
   public void closeAndDispose()
   {
      if (ros2Node != null)
      {
         ros2Node.destroy();
         ros2Node = null;
      }
   }
}
