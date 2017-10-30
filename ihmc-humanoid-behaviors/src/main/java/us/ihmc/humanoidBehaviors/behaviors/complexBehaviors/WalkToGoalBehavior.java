package us.ihmc.humanoidBehaviors.behaviors.complexBehaviors;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.humanoidBehaviors.behaviors.AbstractBehavior;
import us.ihmc.humanoidBehaviors.behaviors.primitives.FootstepListBehavior;
import us.ihmc.humanoidBehaviors.communication.CommunicationBridge;
import us.ihmc.humanoidBehaviors.communication.ConcurrentListeningQueue;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataListMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepPlanningRequestPacket;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepPlanningToolboxOutputStatus;
import us.ihmc.robotics.stateMachines.conditionBasedStateMachine.State;
import us.ihmc.robotics.stateMachines.conditionBasedStateMachine.StateMachine;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

public class WalkToGoalBehavior extends AbstractBehavior
{
   enum WalkToGoalBehaviorStates
   {
      WAITING_FOR_REQUEST, PLANNING, EXECUTING_PLAN
   }

   private final ConcurrentListeningQueue<FootstepPlanningRequestPacket> planningRequestQueue = new ConcurrentListeningQueue<>(20);

   private final StateMachine<WalkToGoalBehaviorStates> stateMachine;

   private final FootstepListBehavior footstepListBehavior;

   private final YoBoolean isDone;
   private final YoBoolean havePlanToExecute;
   private final YoBoolean transitionBackToWaitingState;

   private FootstepDataListMessage planToExecute;

   public WalkToGoalBehavior(CommunicationBridge outgoingCommunicationBridge, WalkingControllerParameters walkingControllerParameters, YoDouble yoTime)
   {
      super(outgoingCommunicationBridge);

      stateMachine = new StateMachine<WalkToGoalBehaviorStates>("WalkToGoalBehaviorStateMachine", "WalkToGoalBehaviorStateMachineSwitchTime",
            WalkToGoalBehaviorStates.class, yoTime, registry);

      footstepListBehavior = new FootstepListBehavior(outgoingCommunicationBridge, walkingControllerParameters);

      isDone = new YoBoolean("isDone", registry);
      havePlanToExecute = new YoBoolean("havePlanToExecute", registry);
      transitionBackToWaitingState = new YoBoolean("transitionBackToWaitingState", registry);

      attachNetworkListeningQueue(planningRequestQueue, FootstepPlanningRequestPacket.class);
      attachNetworkListeningQueue(planningRequestQueue, FootstepPlanningToolboxOutputStatus.class);

      setupStateMachine();
   }

   private void setupStateMachine()
   {
      WaitingForRequestState waitingForRequestState = new WaitingForRequestState();
      waitingForRequestState.addStateTransition(WalkToGoalBehaviorStates.PLANNING, planningRequestQueue::isNewPacketAvailable);
      stateMachine.addState(waitingForRequestState);

      PlanningState planningState = new PlanningState();
      planningState.addStateTransition(WalkToGoalBehaviorStates.EXECUTING_PLAN, havePlanToExecute::getBooleanValue);
      planningState.addStateTransition(WalkToGoalBehaviorStates.WAITING_FOR_REQUEST, transitionBackToWaitingState::getBooleanValue);
      stateMachine.addState(planningState);

      ExecutingPlanState executingPlanState = new ExecutingPlanState();
      executingPlanState.addStateTransition(WalkToGoalBehaviorStates.WAITING_FOR_REQUEST, footstepListBehavior::isDone);
      stateMachine.addState(executingPlanState);

      stateMachine.setCurrentState(WalkToGoalBehaviorStates.WAITING_FOR_REQUEST);
   }

   @Override
   public void doControl()
   {
      stateMachine.checkTransitionConditions();
      stateMachine.doAction();
   }

   @Override
   public void onBehaviorEntered()
   {
   }

   @Override
   public void onBehaviorAborted()
   {

   }

   @Override
   public void onBehaviorPaused()
   {

   }

   @Override
   public void onBehaviorResumed()
   {

   }

   @Override
   public void onBehaviorExited()
   {

   }

   @Override
   public boolean isDone()
   {
      return isDone.getBooleanValue();
   }

   class WaitingForRequestState extends State<WalkToGoalBehaviorStates>
   {
      public WaitingForRequestState()
      {
         super(WalkToGoalBehaviorStates.WAITING_FOR_REQUEST);
      }

      @Override
      public void doAction()
      {
         // Waiting for plan request
      }

      @Override
      public void doTransitionIntoAction()
      {
         // Make sure there aren't any old plan requests hanging around
         planningRequestQueue.clear();
      }

      @Override
      public void doTransitionOutOfAction()
      {
      }
   }

   class PlanningState extends State<WalkToGoalBehaviorStates>
   {
      private final ConcurrentListeningQueue<FootstepPlanningToolboxOutputStatus> planningOutputStatusQueue = new ConcurrentListeningQueue<>(5);

      public PlanningState()
      {
         super(WalkToGoalBehaviorStates.PLANNING);
      }

      @Override
      public void doAction()
      {
         // Wait for plan
         boolean newPacketAvailable = planningOutputStatusQueue.isNewPacketAvailable();

         if(newPacketAvailable)
         {
            FootstepPlanningToolboxOutputStatus latestPacket = planningOutputStatusQueue.getLatestPacket();
            boolean validForExecution = latestPacket.planningResult.validForExecution();
            if(validForExecution)
            {
               planToExecute = latestPacket.footstepDataList;
               havePlanToExecute.set(true);
            }
         }
      }

      @Override
      public void doTransitionIntoAction()
      {
         isDone.set(false);

         FootstepPlanningRequestPacket footstepPlanningRequestPacket = planningRequestQueue.poll();
         footstepPlanningRequestPacket.setDestination(PacketDestination.FOOTSTEP_PLANNING_TOOLBOX_MODULE);

         communicationBridge.sendPacket(footstepPlanningRequestPacket);
      }

      @Override
      public void doTransitionOutOfAction()
      {

      }
   }

   class ExecutingPlanState extends State<WalkToGoalBehaviorStates>
   {
      public ExecutingPlanState()
      {
         super(WalkToGoalBehaviorStates.EXECUTING_PLAN);
      }

      @Override
      public void doAction()
      {
         footstepListBehavior.doControl();
      }

      @Override
      public void doTransitionIntoAction()
      {
         footstepListBehavior.onBehaviorEntered();
         footstepListBehavior.set(planToExecute);
      }

      @Override
      public void doTransitionOutOfAction()
      {
         footstepListBehavior.onBehaviorExited();
         isDone.set(true);
      }
   }
}
