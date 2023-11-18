package us.ihmc.footstepPlanning.monteCarloPlanning;

import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.footstepPlanning.FootstepPlan;
import us.ihmc.footstepPlanning.MonteCarloFootstepPlannerParameters;
import us.ihmc.log.LogTools;
import us.ihmc.robotics.robotSide.RobotSide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MonteCarloFootstepPlanner
{
   private MonteCarloFootstepPlannerParameters plannerParameters;
   private MonteCarloFootstepPlanningDebugger debugger;

   private MonteCarloPlanningWorld world;
   private MonteCarloWaypointAgent agent;
   private MonteCarloFootstepNode root;

   private HashMap<MonteCarloFootstepNode, MonteCarloFootstepNode> visitedNodes = new HashMap<>();

   private int searchIterations = 100;
   private int simulationIterations = 8;
   private int uniqueNodeId = 0;
   private int worldHeight = 200;
   private int worldWidth = 200;
   private int goalMargin = 5;

   private boolean planning = false;

   public MonteCarloFootstepPlanner(MonteCarloFootstepPlannerParameters plannerParameters)
   {
      this.debugger = new MonteCarloFootstepPlanningDebugger(this);
      this.plannerParameters = plannerParameters;
      this.world = new MonteCarloPlanningWorld(goalMargin, worldHeight, worldWidth);
   }

   public FootstepPlan generateFootstepPlan(MonteCarloFootstepPlannerRequest request)
   {
      if (root == null)
      {
         Point2D position = new Point2D(request.getStartFootPoses().get(RobotSide.LEFT).getPosition().getX() * 50,
                                        request.getStartFootPoses().get(RobotSide.LEFT).getPosition().getY() * 50);
         float yaw = (float) request.getStartFootPoses().get(RobotSide.LEFT).getYaw();
         Point3D state = new Point3D(position.getX(), position.getY(), yaw);
         root = new MonteCarloFootstepNode(state, null, RobotSide.LEFT, uniqueNodeId++);
      }

      planning = true;
      debugger.setRequest(request);

      for (int i = 0; i < searchIterations; i++)
      {
         updateTree(root, request);
      }

      MonteCarloPlannerTools.printLayerCounts(root);
      FootstepPlan plan = MonteCarloPlannerTools.getFootstepPlanFromTree(root, request);

      planning = false;
      return plan;
   }

   public void updateTree(MonteCarloFootstepNode node, MonteCarloFootstepPlannerRequest request)
   {
      if (node == null)
      {
         LogTools.warn("Node is null");
         return;
      }

      node.prune();

      if (node.getVisits() <= 1 || node.getChildren().isEmpty())
      {
         MonteCarloFootstepNode childNode = expand(node, request);
         double score = simulate(childNode, request);
         childNode.setValue((float) score);
         backPropagate(node, (float) score);
      }
      else
      {
         float bestScore = 0;
         MonteCarloFootstepNode bestNode = null;
         for (MonteCarloTreeNode child : node.getChildren())
         {
            child.updateUpperConfidenceBound();
            if (child.getUpperConfidenceBound() >= bestScore)
            {
               bestScore = child.getUpperConfidenceBound();
               bestNode = (MonteCarloFootstepNode) child;
            }
         }
         updateTree(bestNode, request);
      }
   }

   public MonteCarloFootstepNode expand(MonteCarloFootstepNode node, MonteCarloFootstepPlannerRequest request)
   {
      ArrayList<?> availableStates = node.getAvailableStates(world, request);

      for (Object newStateObj : availableStates)
      {
         MonteCarloFootstepNode newState = (MonteCarloFootstepNode) newStateObj;
         double score = MonteCarloPlannerTools.scoreFootstepNode(node, newState, request);

         if (node.getLevel() < 8)
         {
            if (visitedNodes.getOrDefault(newState, null) != null)
            {
               MonteCarloFootstepNode existingNode = visitedNodes.get(newState);
               node.addChild(existingNode);
               existingNode.getParents().add(node);
            }
            else
            {
               MonteCarloFootstepNode postNode = new MonteCarloFootstepNode(newState.getState(), node, newState.getRobotSide(), uniqueNodeId++);
               visitedNodes.put(newState, postNode);
               node.addChild(postNode);
            }
         }
      }

      return (MonteCarloFootstepNode) node.getMaxQueueNode();
   }

   public double simulate(MonteCarloFootstepNode node, MonteCarloFootstepPlannerRequest request)
   {
      double score = 0;

      MonteCarloFootstepNode simulationState = new MonteCarloFootstepNode(node.getState(), null, node.getRobotSide().getOppositeSide(), 0);

      for (int i = 0; i < simulationIterations; i++)
      {
         ArrayList<MonteCarloFootstepNode> nextStates = simulationState.getAvailableStates(world, request);
         int actionIndex = (int) (Math.random() * nextStates.size());
         simulationState = nextStates.get(actionIndex);

         //LogTools.info(String.format("Simulation %d, Random State: %s, Actions: %d, Side:%s", i, randomState.getPosition(), nextStates.size(), randomState.getRobotSide()));
         score += MonteCarloPlannerTools.scoreFootstepNode(node, simulationState, request);
         //LogTools.info("Action Taken: {}, Score: {}", actionIndex, score);
      }

      return score;
   }

   public void backPropagate(MonteCarloFootstepNode node, float score)
   {
      node.addValue(score);
      node.incrementVisits();

      if (!node.getParents().isEmpty())
      {
         for (MonteCarloTreeNode parent : node.getParents())
         {
            backPropagate((MonteCarloFootstepNode) parent, score);
         }
      }
   }

   public void reset()
   {
      uniqueNodeId = 0;
      visitedNodes.clear();
      root = new MonteCarloFootstepNode(new Point3D(), null, RobotSide.LEFT, uniqueNodeId++);
   }

   public MonteCarloPlanningWorld getWorld()
   {
      return world;
   }

   public MonteCarloTreeNode getRoot()
   {
      return root;
   }

   public List<MonteCarloFootstepNode> getVisitedNodes()
   {
      return new ArrayList<>(visitedNodes.values());
   }

   public MonteCarloFootstepPlanningDebugger getDebugger()
   {
      return debugger;
   }

   public boolean isPlanning()
   {
      return planning;
   }
}
