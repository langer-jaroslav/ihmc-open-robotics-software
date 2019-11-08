package us.ihmc.footstepPlanning.ui.components;

import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.BodyPathData;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.ComputePath;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.GoalOrientation;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.GoalPosition;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.GoalVisibilityMap;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.InitialSupportSide;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.InterRegionVisibilityMap;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.LowLevelGoalOrientation;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.LowLevelGoalPosition;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.PlanarRegionData;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.PlannerHorizonLength;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.PlannerParameters;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.PlannerStatus;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.PlannerTimeTaken;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.PlannerTimeout;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.PlannerType;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.PlanningResult;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.RequestPlannerStatistics;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.StartOrientation;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.StartPosition;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.StartVisibilityMap;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.VisibilityGraphsParameters;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.VisibilityMapWithNavigableRegionData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.communication.packets.ExecutionMode;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.footstepPlanning.FootstepPlan;
import us.ihmc.footstepPlanning.FootstepPlannerStatus;
import us.ihmc.footstepPlanning.FootstepPlannerType;
import us.ihmc.footstepPlanning.*;
import us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI;
import us.ihmc.footstepPlanning.graphSearch.collision.FootstepNodeBodyCollisionDetector;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapAndWiggler;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.SimplePlanarRegionFootstepNodeSnapper;
import us.ihmc.footstepPlanning.graphSearch.graph.visualization.MessageBasedPlannerListener;
import us.ihmc.footstepPlanning.graphSearch.graph.visualization.MessagerBasedPlannerListener;
import us.ihmc.footstepPlanning.graphSearch.heuristics.DistanceAndYawBasedHeuristics;
import us.ihmc.footstepPlanning.graphSearch.listeners.BipedalFootstepPlannerListener;
import us.ihmc.footstepPlanning.graphSearch.nodeChecking.*;
import us.ihmc.footstepPlanning.graphSearch.nodeExpansion.FootstepNodeExpansion;
import us.ihmc.footstepPlanning.graphSearch.nodeExpansion.ParameterBasedNodeExpansion;
import us.ihmc.footstepPlanning.graphSearch.parameters.DefaultFootstepPlannerParameters;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersReadOnly;
import us.ihmc.footstepPlanning.graphSearch.planners.*;
import us.ihmc.footstepPlanning.graphSearch.stepCost.ConstantFootstepCost;
import us.ihmc.footstepPlanning.graphSearch.stepCost.FootstepCost;
import us.ihmc.footstepPlanning.graphSearch.stepCost.FootstepCostBuilder;
import us.ihmc.footstepPlanning.simplePlanners.PlanThenSnapPlanner;
import us.ihmc.footstepPlanning.simplePlanners.TurnWalkTurnPlanner;
import us.ihmc.footstepPlanning.tools.PlannerTools;
import us.ihmc.footstepPlanning.tools.statistics.GraphSearchStatistics;
import us.ihmc.log.LogTools;
import us.ihmc.messager.Messager;
import us.ihmc.messager.SharedMemoryMessager;
import us.ihmc.pathPlanning.statistics.ListOfStatistics;
import us.ihmc.pathPlanning.statistics.PlannerStatistics;
import us.ihmc.pathPlanning.statistics.VisibilityGraphStatistics;
import us.ihmc.pathPlanning.visibilityGraphs.parameters.DefaultVisibilityGraphParameters;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.InterRegionVisibilityMap;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityMap;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityMapWithNavigableRegion;
import us.ihmc.pathPlanning.visibilityGraphs.parameters.VisibilityGraphsParametersReadOnly;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.VisibilityMapHolder;
import us.ihmc.pathPlanning.visibilityGraphs.tools.BodyPathPlan;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.*;

public class FootstepPathCalculatorModule
{
   private static final boolean VERBOSE = true;

   private final ExecutorService executorService = Executors.newSingleThreadExecutor(ThreadTools.getNamedThreadFactory(getClass().getSimpleName()));

   private final AtomicReference<PlanarRegionsList> planarRegionsReference;
   private final AtomicReference<Point3D> startPositionReference;
   private final AtomicReference<Quaternion> startOrientationReference;
   private final AtomicReference<RobotSide> initialStanceSideReference;
   private final AtomicReference<Point3D> goalPositionReference;
   private final AtomicReference<Quaternion> goalOrientationReference;
   private final AtomicReference<FootstepPlannerType> footstepPlannerTypeReference;

   private final AtomicReference<Double> plannerTimeoutReference;
   private final AtomicReference<Double> plannerBestEffortTimeoutReference;
   private final AtomicReference<Double> plannerHorizonLengthReference;

   private final AtomicReference<FootstepPlannerParametersReadOnly> parameters;
   private final AtomicReference<VisibilityGraphsParametersReadOnly> visibilityGraphsParameters;

   private final Messager messager;

   private BodyPathAndFootstepPlanner planner;

   public FootstepPathCalculatorModule(Messager messager)
   {
      this.messager = messager;

      planarRegionsReference = messager.createInput(PlanarRegionData);
      startPositionReference = messager.createInput(StartPosition);
      startOrientationReference = messager.createInput(StartOrientation, new Quaternion());
      initialStanceSideReference = messager.createInput(InitialSupportSide, RobotSide.LEFT);
      goalPositionReference = messager.createInput(GoalPosition);
      goalOrientationReference = messager.createInput(GoalOrientation, new Quaternion());

      parameters = messager.createInput(PlannerParameters, new DefaultFootstepPlannerParameters());
      visibilityGraphsParameters = messager.createInput(VisibilityGraphsParameters, new DefaultVisibilityGraphParameters());
      footstepPlannerTypeReference = messager.createInput(PlannerType, FootstepPlannerType.A_STAR);
      plannerTimeoutReference = messager.createInput(PlannerTimeout, 5.0);
      plannerBestEffortTimeoutReference = messager.createInput(PlannerBestEffortTimeout, 0.0);
      plannerHorizonLengthReference = messager.createInput(PlannerHorizonLength, 1.0);

      messager.registerTopicListener(ComputePath, request -> computePathOnThread());
      messager.registerTopicListener(RequestPlannerStatistics, request -> sendPlannerStatistics());
   }

   public void clear()
   {
      planarRegionsReference.set(null);
      startPositionReference.set(null);
      startOrientationReference.set(null);
      initialStanceSideReference.set(null);
      goalPositionReference.set(null);
      goalOrientationReference.set(null);
      plannerTimeoutReference.set(null);
      plannerBestEffortTimeoutReference.set(null);
      plannerHorizonLengthReference.set(null);
   }

   public void start()
   {
   }

   public void stop()
   {
      executorService.shutdownNow();
   }

   private void computePathOnThread()
   {
      executorService.submit(this::computePath);
   }

   private void computePath()
   {
      if (VERBOSE)
      {
         LogTools.info("Starting to compute path...");
      }

      PlanarRegionsList planarRegionsList = planarRegionsReference.get();

      if (planarRegionsList == null)
         return;

      Point3D start = startPositionReference.get();

      if (start == null)
         return;

      Point3D goal = goalPositionReference.get();

      if (goal == null)
         return;

      if (VERBOSE)
         LogTools.info("Computing footstep path.");

      try
      {
         planner = createPlanner();

         planner.setPlanarRegions(planarRegionsList);
         planner.setTimeout(plannerTimeoutReference.get());
         planner.setBestEffortTimeout(plannerBestEffortTimeoutReference.get());
         planner.setPlanningHorizonLength(plannerHorizonLengthReference.get());

         planner
               .setInitialStanceFoot(new FramePose3D(ReferenceFrame.getWorldFrame(), start, startOrientationReference.get()), initialStanceSideReference.get());

         FootstepPlannerGoal plannerGoal = new FootstepPlannerGoal();
         plannerGoal.setFootstepPlannerGoalType(FootstepPlannerGoalType.POSE_BETWEEN_FEET);
         plannerGoal.setGoalPoseBetweenFeet(new FramePose3D(ReferenceFrame.getWorldFrame(), goal, goalOrientationReference.get()));
         planner.setGoal(plannerGoal);

         messager.submitMessage(PlannerStatus, FootstepPlannerStatus.PLANNING_PATH);

         FootstepPlanningResult planningResult = planner.planPath();
         if (planningResult.validForExecution())
         {
            BodyPathPlan bodyPathPlan = planner.getPathPlan();
            messager.submitMessage(PlannerStatus, FootstepPlannerStatus.PLANNING_STEPS);

            if (bodyPathPlan != null)
            {
               List<Pose3DReadOnly> bodyPath = new ArrayList<>();
               for (int i = 0; i < bodyPathPlan.getNumberOfWaypoints(); i++)
                  bodyPath.add(bodyPathPlan.getWaypoint(i));
               messager.submitMessage(BodyPathData, bodyPath);
            }
            messager.submitMessage(PlanningResult, planningResult);

            planningResult = planner.plan();
         }

         FootstepPlan footstepPlan = planner.getPlan();

         if (VERBOSE)
         {
            LogTools.info("Planner result: " + planningResult);
            if (planningResult.validForExecution())
               LogTools.info("Planner result: " + planner.getPlan().getNumberOfSteps() + " steps, taking " + planner.getPlanningDuration() + " s.");
         }

         messager.submitMessage(PlanningResult, planningResult);
         messager.submitMessage(PlannerTimeTaken, planner.getPlanningDuration());
         messager.submitMessage(PlannerStatus, FootstepPlannerStatus.IDLE);

         if (planningResult.validForExecution())
         {
            messager.submitMessage(FootstepPlanResponse, FootstepDataMessageConverter.createFootstepDataListFromPlan(footstepPlan, -1.0, -1.0, ExecutionMode.OVERRIDE));
            if (footstepPlan.getLowLevelPlanGoal() != null)
            {
               messager.submitMessage(LowLevelGoalPosition, new Point3D(footstepPlan.getLowLevelPlanGoal().getPosition()));
               messager.submitMessage(LowLevelGoalOrientation, new Quaternion(footstepPlan.getLowLevelPlanGoal().getOrientation()));
            }
         }
      }
      catch (Exception e)
      {
         LogTools.error(e.getMessage());
         e.printStackTrace();
      }
   }

   private void sendPlannerStatistics()
   {
      if (planner == null)
         return;

      PlannerStatistics<?> plannerStatistics = planner.getPlannerStatistics();
      sendPlannerStatisticsMessages(plannerStatistics);
   }

   private void sendPlannerStatisticsMessages(PlannerStatistics plannerStatistics)
   {
      switch (plannerStatistics.getStatisticsType())
      {
      case LIST:
         sendListOfStatisticsMessages((ListOfStatistics) plannerStatistics);
         break;
      case VISIBILITY_GRAPH:
         sendVisibilityGraphStatisticsMessages((VisibilityGraphStatistics) plannerStatistics);
         break;
      case GRAPH_SEARCH:
         sendGraphSearchPlannerStatisticsMessage((GraphSearchStatistics) plannerStatistics);
         break;
      }
   }

   private void sendListOfStatisticsMessages(ListOfStatistics listOfStatistics)
   {
      while (listOfStatistics.getNumberOfStatistics() > 0)
         sendPlannerStatisticsMessages(listOfStatistics.pollStatistics());
   }

   private void sendVisibilityGraphStatisticsMessages(VisibilityGraphStatistics statistics)
   {
      VisibilityMapHolder startMap = new VisibilityMapHolder()
      {
         @Override
         public int getMapId()
         {
            return statistics.getStartMapId();
         }

         @Override
         public VisibilityMap getVisibilityMapInLocal()
         {
            return statistics.getStartVisibilityMap();
         }

         @Override
         public VisibilityMap getVisibilityMapInWorld()
         {
            return statistics.getStartVisibilityMap();
         }
      };
      VisibilityMapHolder goalMap = new VisibilityMapHolder()
      {
         @Override
         public int getMapId()
         {
            return statistics.getGoalMapId();
         }

         @Override
         public VisibilityMap getVisibilityMapInLocal()
         {
            return statistics.getGoalVisibilityMap();
         }

         @Override
         public VisibilityMap getVisibilityMapInWorld()
         {
            return statistics.getGoalVisibilityMap();
         }
      };
      InterRegionVisibilityMap interRegionVisibilityMap = new InterRegionVisibilityMap();
      interRegionVisibilityMap.addConnections(statistics.getInterRegionsVisibilityMap().getConnections());

      List<VisibilityMapWithNavigableRegion> navigableRegionList = new ArrayList<>();
      for (int i = 0; i < statistics.getNumberOfNavigableRegions(); i++)
         navigableRegionList.add(statistics.getNavigableRegion(i));

      messager.submitMessage(StartVisibilityMap, startMap);
      messager.submitMessage(GoalVisibilityMap, goalMap);
      messager.submitMessage(VisibilityMapWithNavigableRegionData, navigableRegionList);
      messager.submitMessage(InterRegionVisibilityMap, interRegionVisibilityMap);
   }

   private void sendGraphSearchPlannerStatisticsMessage(GraphSearchStatistics graphSearchStatistics)
   {
      messager.submitMessage(FootstepPlannerMessagerAPI.ExpandedNodesMap, graphSearchStatistics.getExpandedNodes());
      messager.submitMessage(FootstepPlannerMessagerAPI.FootstepGraphPart, graphSearchStatistics.getFullGraph());
   }

   private BodyPathAndFootstepPlanner createPlanner()
   {
      SideDependentList<ConvexPolygon2D> contactPointsInSoleFrame = PlannerTools.createDefaultFootPolygons();
      YoVariableRegistry registry = new YoVariableRegistry("visualizerRegistry");

      switch (footstepPlannerTypeReference.get())
      {
      case PLAN_THEN_SNAP:
         return new PlanThenSnapPlanner(new TurnWalkTurnPlanner(parameters.get()), contactPointsInSoleFrame);
      case A_STAR:
         return createAStarPlanner(contactPointsInSoleFrame, registry);
      case VIS_GRAPH_WITH_A_STAR:
         SimplePlanarRegionFootstepNodeSnapper snapper = new SimplePlanarRegionFootstepNodeSnapper(contactPointsInSoleFrame);
         long updateFrequency = 1000;
         BipedalFootstepPlannerListener listener = new MessagerBasedPlannerListener(messager, snapper, updateFrequency);
         return new VisibilityGraphWithAStarPlanner(parameters.get(), visibilityGraphsParameters.get(), contactPointsInSoleFrame, null, registry, listener);
      default:
         throw new RuntimeException("Planner type " + footstepPlannerTypeReference.get() + " is not valid!");
      }
   }

   private BodyPathAndFootstepPlanner createAStarPlanner(SideDependentList<ConvexPolygon2D> footPolygons, YoVariableRegistry registry)
   {
      FootstepPlannerParametersReadOnly parameters = this.parameters.get();
      FootstepNodeExpansion expansion = new ParameterBasedNodeExpansion(parameters);
      SimplePlanarRegionFootstepNodeSnapper snapper = new SimplePlanarRegionFootstepNodeSnapper(footPolygons);
      FootstepNodeSnapAndWiggler postProcessingSnapper = new FootstepNodeSnapAndWiggler(footPolygons, parameters);
      FootstepNodeBodyCollisionDetector collisionDetector = new FootstepNodeBodyCollisionDetector(parameters);

      SnapBasedNodeChecker snapBasedNodeChecker = new SnapBasedNodeChecker(parameters, footPolygons, snapper);
      BodyCollisionNodeChecker bodyCollisionNodeChecker = new BodyCollisionNodeChecker(collisionDetector, parameters, snapper);
      PlanarRegionBaseOfCliffAvoider cliffAvoider = new PlanarRegionBaseOfCliffAvoider(parameters, snapper, footPolygons);

      DistanceAndYawBasedHeuristics heuristics = new DistanceAndYawBasedHeuristics(snapper, parameters.getAStarHeuristicsWeight(), parameters);

      FootstepNodeChecker nodeChecker = new FootstepNodeCheckerOfCheckers(Arrays.asList(snapBasedNodeChecker, bodyCollisionNodeChecker, cliffAvoider));

      FootstepCostBuilder costBuilder = new FootstepCostBuilder();
      costBuilder.setFootstepPlannerParameters(parameters);
      costBuilder.setSnapper(snapper);
      costBuilder.setFootPolygons(footPolygons);
      costBuilder.setIncludeHeightCost(true);
      costBuilder.setIncludePitchAndRollCost(true);
      costBuilder.setIncludeAreaCost(true);

      FootstepCost footstepCost = costBuilder.buildCost();

      long updateFrequency = 1000;
      MessageBasedPlannerListener plannerListener = new MessagerBasedPlannerListener(messager, snapper, updateFrequency);

      snapBasedNodeChecker.addPlannerListener(plannerListener);
      bodyCollisionNodeChecker.addPlannerListener(plannerListener);

      return new AStarFootstepPlanner(parameters, nodeChecker, heuristics, expansion, footstepCost, postProcessingSnapper, plannerListener, footPolygons,
                                      registry);
   }

   public static FootstepPathCalculatorModule createMessagerModule(SharedMemoryMessager messager)
   {
      return new FootstepPathCalculatorModule(messager);
   }
}