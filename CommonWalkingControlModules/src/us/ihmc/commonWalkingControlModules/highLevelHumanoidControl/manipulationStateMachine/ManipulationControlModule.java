package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulationStateMachine;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicReferenceFrame;
import com.yobotics.simulationconstructionset.util.statemachines.State;
import com.yobotics.simulationconstructionset.util.statemachines.StateMachine;
import com.yobotics.simulationconstructionset.util.statemachines.StateTransition;
import com.yobotics.simulationconstructionset.util.statemachines.StateTransitionCondition;
import us.ihmc.commonWalkingControlModules.configurations.ManipulationControllerParameters;
import us.ihmc.commonWalkingControlModules.controllers.HandControllerInterface;
import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulationStateMachine.fingerToroidManipulation.HighLevelFingerToroidManipulationState;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.GeometricJacobian;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.TwistCalculator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author twan
 *         Date: 5/13/13
 */
public class ManipulationControlModule
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final StateMachine<ManipulationState> stateMachine;
   private final List<DynamicGraphicReferenceFrame> dynamicGraphicReferenceFrames = new ArrayList<DynamicGraphicReferenceFrame>();

   public ManipulationControlModule(DoubleYoVariable yoTime, FullRobotModel fullRobotModel, TwistCalculator twistCalculator,
                                    ManipulationControllerParameters parameters, final DesiredHandPoseProvider handPoseProvider,
                                    final TorusPoseProvider torusPoseProvider, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry,
                                    SideDependentList<HandControllerInterface> handControllers, MomentumBasedController momentumBasedController,
                                    YoVariableRegistry parentRegistry)
   {
      stateMachine = new StateMachine<ManipulationState>("manipulationState", "manipulationStateSwitchTime", ManipulationState.class, yoTime, registry);

      SideDependentList<ReferenceFrame> handPositionControlFrames = new SideDependentList<ReferenceFrame>();
      SideDependentList<GeometricJacobian> jacobians = new SideDependentList<GeometricJacobian>();

      for (RobotSide robotSide : RobotSide.values)
      {
         RigidBody endEffector = fullRobotModel.getHand(robotSide);

         GeometricJacobian jacobian = new GeometricJacobian(fullRobotModel.getChest(), endEffector, endEffector.getBodyFixedFrame());
         jacobians.put(robotSide, jacobian);

         String frameName = endEffector.getName() + "PositionControlFrame";
         final ReferenceFrame frameAfterJoint = endEffector.getParentJoint().getFrameAfterJoint();
         ReferenceFrame handPositionControlFrame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent(frameName, frameAfterJoint,
                                                      parameters.getHandControlFramesWithRespectToFrameAfterWrist().get(robotSide));
         handPositionControlFrames.put(robotSide, handPositionControlFrame);

         if (dynamicGraphicObjectsListRegistry != null)
         {
            DynamicGraphicObjectsList list = new DynamicGraphicObjectsList("handPositionControlFrames");

            DynamicGraphicReferenceFrame dynamicGraphicReferenceFrame = new DynamicGraphicReferenceFrame(handPositionControlFrame, registry, 0.3);
            dynamicGraphicReferenceFrames.add(dynamicGraphicReferenceFrame);
            list.add(dynamicGraphicReferenceFrame);

            dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjectsList(list);
            list.hideDynamicGraphicObjects();
         }
      }

      setUpStateMachine(yoTime, fullRobotModel, twistCalculator, parameters, handPoseProvider, torusPoseProvider, dynamicGraphicObjectsListRegistry,
            handControllers, momentumBasedController, parentRegistry, handPositionControlFrames, jacobians);

      parentRegistry.addChild(registry);
   }

   private void setUpStateMachine(DoubleYoVariable yoTime, FullRobotModel fullRobotModel, TwistCalculator twistCalculator, ManipulationControllerParameters parameters, final DesiredHandPoseProvider handPoseProvider, final TorusPoseProvider torusPoseProvider, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry, SideDependentList<HandControllerInterface> handControllers, MomentumBasedController momentumBasedController, YoVariableRegistry parentRegistry, SideDependentList<ReferenceFrame> handPositionControlFrames, SideDependentList<GeometricJacobian> jacobians)
   {
      HighLevelDirectControlManipulationState directControlManipulationState = new HighLevelDirectControlManipulationState(yoTime, fullRobotModel, twistCalculator, parameters, handPoseProvider,
              dynamicGraphicObjectsListRegistry, handControllers, handPositionControlFrames, jacobians, momentumBasedController, registry);
      stateMachine.addState(directControlManipulationState);

      HighLevelToroidManipulationState toroidManipulationState = new HighLevelToroidManipulationState(yoTime, fullRobotModel, twistCalculator, handPositionControlFrames, handControllers,
              jacobians, torusPoseProvider, momentumBasedController, dynamicGraphicObjectsListRegistry, parentRegistry);
      stateMachine.addState(toroidManipulationState);

      State<ManipulationState> fingerToroidManipulationState = new HighLevelFingerToroidManipulationState(twistCalculator, handPositionControlFrames, jacobians,
                                                                        momentumBasedController, fullRobotModel.getElevator(), torusPoseProvider, handControllers, registry, dynamicGraphicObjectsListRegistry);
      stateMachine.addState(fingerToroidManipulationState);

      StateTransitionCondition stateTransitionCondition = new StateTransitionCondition()
      {
         public boolean checkCondition()
         {
            return torusPoseProvider.checkForNewPose();
         }
      };

//    StateTransition<ManipulationState> toToroidManipulation = new StateTransition<ManipulationState>(toroidManipulationState.getStateEnum(),
//          stateTransitionCondition);

      StateTransition<ManipulationState> toToroidManipulation = new StateTransition<ManipulationState>(fingerToroidManipulationState.getStateEnum(),
                                                                   stateTransitionCondition);
      directControlManipulationState.addStateTransition(toToroidManipulation);

      StateTransitionCondition toDirectManipulationCondition = new StateTransitionCondition()
      {
         public boolean checkCondition()
         {
            // TODO: hack
            boolean defaultRequested = handPoseProvider.checkForNewPose(RobotSide.LEFT) &&!handPoseProvider.isRelativeToWorld();

            return defaultRequested;
         }
      };
      StateTransition<ManipulationState> toDirectManipulation = new StateTransition<ManipulationState>(directControlManipulationState.getStateEnum(),
                                                                   toDirectManipulationCondition);
      toroidManipulationState.addStateTransition(toDirectManipulation);


//    toroidManipulationState.addStateTransition(toToroidManipulation);
   }

   public void initialize()
   {
      stateMachine.setCurrentState(ManipulationState.DIRECT_CONTROL);
   }

   public void doControl()
   {
      updateGraphics();
      stateMachine.checkTransitionConditions();
      stateMachine.doAction();
   }

   private void updateGraphics()
   {
      for (int i = 0; i < dynamicGraphicReferenceFrames.size(); i++)
      {
         dynamicGraphicReferenceFrames.get(i).update();
      }
   }
}
