package us.ihmc.footstepPlanning.graphSearch.nodeChecking;

import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.graph.visualization.BipedalFootstepPlannerListener;
import us.ihmc.robotics.geometry.PlanarRegionsList;

import java.util.List;

public class FootstepNodeCheckerOfCheckers extends FootstepNodeChecker
{
   private final List<FootstepNodeChecker> nodeCheckers;

   public FootstepNodeCheckerOfCheckers(List<FootstepNodeChecker> nodeCheckers)
   {
      this.nodeCheckers = nodeCheckers;
   }

   @Override
   public void addPlannerListener(BipedalFootstepPlannerListener listener)
   {
      nodeCheckers.forEach(checker -> checker.addPlannerListener(listener));
   }

   @Override
   public void setPlanarRegions(PlanarRegionsList planarRegions)
   {
      nodeCheckers.forEach(checker -> checker.setPlanarRegions(planarRegions));
   }

   @Override
   public boolean isNodeValid(FootstepNode node, FootstepNode previousNode)
   {
      for(FootstepNodeChecker checker : nodeCheckers)
      {
         if(!checker.isNodeValid(node, previousNode))
            return false;
      }
      return true;
   }

   @Override
   public void addStartNode(FootstepNode startNode, RigidBodyTransform startNodeTransform)
   {
      nodeCheckers.forEach((checker) -> checker.addStartNode(startNode, startNodeTransform));
   }
}
