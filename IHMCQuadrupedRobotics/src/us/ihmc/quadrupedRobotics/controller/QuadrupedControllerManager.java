package us.ihmc.quadrupedRobotics.controller;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.communication.streamingData.GlobalDataProducer;
import us.ihmc.quadrupedRobotics.inverseKinematics.QuadrupedLegInverseKinematicsCalculator;
import us.ihmc.quadrupedRobotics.parameters.QuadrupedRobotParameters;
import us.ihmc.quadrupedRobotics.stateEstimator.QuadrupedStateEstimator;
import us.ihmc.quadrupedRobotics.virtualModelController.QuadrupedVirtualModelController;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.dataStructures.variable.EnumYoVariable;
import us.ihmc.robotics.stateMachines.StateMachine;
import us.ihmc.sensorProcessing.model.RobotMotionStatus;
import us.ihmc.sensorProcessing.model.RobotMotionStatusHolder;
import us.ihmc.simulationconstructionset.robotController.RobotController;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;

public class QuadrupedControllerManager implements RobotController
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   
   private final QuadrupedStateEstimator stateEstimator;
   private final QuadrupedVirtualModelController virtualModelController;
   
   private final StateMachine<QuadrupedControllerState> stateMachine;
   private final EnumYoVariable<QuadrupedControllerState> requestedState;
   private final EnumYoVariable<SliderBoardModes> sliderboardMode = new EnumYoVariable<>("sliderboardMode", registry, SliderBoardModes.class);
   
   private final DoubleYoVariable robotTimestamp = new DoubleYoVariable("robotTimestamp", registry);
   
   private final RobotMotionStatusHolder robotMotionStatusHolder = new RobotMotionStatusHolder();
   
   public enum SliderBoardModes
   {
      POSITIONCRAWL_COM_SHIFT, POSITIONCRAWL_FOOTSTEP_CHOOSER, POSITIONCRAWL_ORIENTATION_TUNING
   }
   
   public QuadrupedControllerManager(double simulationDT, QuadrupedRobotParameters quadrupedRobotParameters, SDFFullRobotModel sdfFullRobotModel,
         QuadrupedLegInverseKinematicsCalculator inverseKinematicsCalculators, QuadrupedStateEstimator stateEstimator, GlobalDataProducer globalDataProducer,
         YoGraphicsListRegistry yoGraphicsListRegistry, YoGraphicsListRegistry yoGraphicsListRegistryForDetachedOverhead)
   {
      // configure state machine
      this.stateEstimator = stateEstimator;
      this.virtualModelController = new QuadrupedVirtualModelController(sdfFullRobotModel, quadrupedRobotParameters, registry);
      
      stateMachine = new StateMachine<>("QuadrupedControllerStateMachine", "QuadrupedControllerSwitchTime", QuadrupedControllerState.class, robotTimestamp, registry);
      requestedState = new EnumYoVariable<>("QuadrupedControllerStateMachineRequestedState", registry, QuadrupedControllerState.class, true);
      
      QuadrupedVMCStandController vmcStandController = new QuadrupedVMCStandController(simulationDT, quadrupedRobotParameters, sdfFullRobotModel,
            virtualModelController, robotTimestamp, registry, yoGraphicsListRegistry);
      
      QuadrupedPositionBasedCrawlController positionBasedCrawlController = new QuadrupedPositionBasedCrawlController(simulationDT, quadrupedRobotParameters, sdfFullRobotModel,
            stateEstimator, inverseKinematicsCalculators, globalDataProducer, robotTimestamp, registry, yoGraphicsListRegistry, yoGraphicsListRegistryForDetachedOverhead);
      
      stateMachine.addState(vmcStandController);
      stateMachine.addState(positionBasedCrawlController);

      stateMachine.setCurrentState(QuadrupedControllerState.POSITION_CRAWL);
      requestedState.set(null);
   }

   @Override
   public void initialize()
   {

   }

   @Override
   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   @Override
   public String getName()
   {
      return getClass().getSimpleName();
   }

   @Override
   public String getDescription()
   {
      return null;
   }

   @Override
   public void doControl()
   {
      robotTimestamp.set(stateEstimator.getCurrentTime());
      
      if(requestedState.getEnumValue() != null)
      {
         stateMachine.setCurrentState(requestedState.getEnumValue());
         requestedState.set(null);
      }
      
      if(stateMachine.getCurrentState().getStateEnum() == QuadrupedControllerState.DO_NOTHING)
      {
         robotMotionStatusHolder.setCurrentRobotMotionStatus(RobotMotionStatus.STANDING);
      }
      else
      {
         robotMotionStatusHolder.setCurrentRobotMotionStatus(RobotMotionStatus.IN_MOTION);
      }
      
      stateMachine.checkTransitionConditions();
      stateMachine.doAction();
   }

   public RobotMotionStatusHolder getRobotMotionStatusHolder()
   {
      return robotMotionStatusHolder;
   }
}