package us.ihmc.behaviors.sequence;

import us.ihmc.behaviors.behaviorTree.BehaviorTreeNodeExecutor;
import us.ihmc.communication.crdt.CRDTInfo;
import us.ihmc.log.LogTools;

import java.util.ArrayList;
import java.util.List;

public class ActionSequenceExecutor extends BehaviorTreeNodeExecutor<ActionSequenceState, ActionSequenceDefinition>
{
   private final ActionSequenceState state;

   private final List<ActionNodeExecutor<?, ?>> executorChildren = new ArrayList<>();
   private final List<ActionNodeExecutor<?, ?>> currentlyExecutingActions = new ArrayList<>();

   public ActionSequenceExecutor(long id, CRDTInfo crdtInfo)
   {
      state = new ActionSequenceState(id, crdtInfo);
   }

   @Override
   public void tick()
   {
      super.tick();

      // TODO: Tick children
   }

   @Override
   public void update()
   {
      super.update();

      // TODO: Go through next for execution concurrent children and set chest action's goal pelvis frames
      //   and the hand pose actions chest frames

      executorChildren.clear();
      currentlyExecutingActions.clear();
      for (BehaviorTreeNodeExecutor<?, ?> child : getChildren())
      {
         ActionNodeExecutor<?, ?> actionNodeExecutor = (ActionNodeExecutor<?, ?>) child;
         executorChildren.add(actionNodeExecutor);
         if (actionNodeExecutor.getState().getIsExecuting())
         {
            currentlyExecutingActions.add(actionNodeExecutor);
         }
      }

      for (int i = 0; i < getChildren().size(); i++)
      {
         BehaviorActionExecutionStatusCalculator.update(executorChildren, i, getState().getExecutionNextIndex());
      }

      for (ActionNodeExecutor<?, ?> currentlyExecutingAction : currentlyExecutingActions)
      {
         currentlyExecutingAction.updateCurrentlyExecuting();
      }

      if (getState().getAutomaticExecution())
      {
         if (isEndOfSequence())
         {
            getState().setAutomaticExecution(false);
         }
         else if (currentlyExecutingActions.isEmpty())
         {
            do
            {
               LogTools.info("Automatically executing action: {}", executorChildren.get(getState().getExecutionNextIndex()).getClass().getSimpleName());
               executeNextAction();
            }
            while (!isEndOfSequence() && getLastExecutingAction().getDefinition().getExecuteWithNextAction());
         }
      }
      else if (getState().pollManualExecutionRequested())
      {
         do
         {
            LogTools.info("Manually executing action: {}", executorChildren.get(getState().getExecutionNextIndex()).getClass().getSimpleName());
            executeNextAction();
         }
         while (!isEndOfSequence() && getLastExecutingAction().getDefinition().getExecuteWithNextAction());
      }
   }

   private void executeNextAction()
   {
      ActionNodeExecutor<?, ?> actionToExecute = executorChildren.get(getState().getExecutionNextIndex());

      // If automatic execution, we want to ensure it's able to execute before we perform the execution.
      // If it's unable to execute, disable automatic execution.
      if (getState().getAutomaticExecution())
      {
         if (!actionToExecute.getState().getCanExecute())
         {
            getState().setAutomaticExecution(false);
            // Early return
            return;
         }
      }
      actionToExecute.update();
      actionToExecute.triggerActionExecution();
      actionToExecute.updateCurrentlyExecuting();
      currentlyExecutingActions.add(actionToExecute);
      getState().stepForwardNextExecutionIndex();
   }

   private ActionNodeExecutor<?, ?> getLastExecutingAction()
   {
      return currentlyExecutingActions.get(currentlyExecutingActions.size() - 1);
   }

   private boolean isEndOfSequence()
   {
      return getState().getExecutionNextIndex() >= executorChildren.size();
   }

   @Override
   public ActionSequenceState getState()
   {
      return state;
   }
}
