package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoPlaneContactState;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.ControllerCoreCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.ControllerCoreOuput;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HighLevelState;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.OneDoFJoint;

public class DoNothingBehavior extends HighLevelBehavior
{
   private static final HighLevelState controllerState = HighLevelState.DO_NOTHING_BEHAVIOR;

   private final MomentumBasedController momentumBasedController;
   private final BipedSupportPolygons bipedSupportPolygons;
   private final SideDependentList<YoPlaneContactState> footContactStates = new SideDependentList<>();

   private final OneDoFJoint[] allRobotJoints;

   private final ControllerCoreCommand controllerCoreCommand = new ControllerCoreCommand(false);

   public DoNothingBehavior(MomentumBasedController momentumBasedController, BipedSupportPolygons bipedSupportPolygons)
   {
      super(controllerState);

      this.bipedSupportPolygons = bipedSupportPolygons;
      this.momentumBasedController = momentumBasedController;
      allRobotJoints = momentumBasedController.getFullRobotModel().getOneDoFJoints();

      for (RobotSide robotSide : RobotSide.values)
      {
         ContactablePlaneBody contactableFoot = momentumBasedController.getContactableFeet().get(robotSide);
         footContactStates.put(robotSide, momentumBasedController.getContactState(contactableFoot));
      }
   }

   @Override
   public void setControllerCoreOuput(ControllerCoreOuput controllerCoreOuput)
   {
   }

   @Override
   public void doAction()
   {
      bipedSupportPolygons.updateUsingContactStates(footContactStates);
      momentumBasedController.callUpdatables();

      for (int i = 0; i < allRobotJoints.length; i++)
      {
         allRobotJoints[i].setTau(0.0);
      }
      controllerCoreCommand.geDesiredOneDoFJointTorqueHolder().extractDesiredTorquesFromInverseDynamicsJoints(allRobotJoints);
   }

   @Override
   public void doTransitionIntoAction()
   {
      // Do nothing

   }

   @Override
   public void doTransitionOutOfAction()
   {
      // Do nothing

   }

   @Override
   public YoVariableRegistry getYoVariableRegistry()
   {
      return null;
   }

   @Override
   public ControllerCoreCommand getControllerCoreCommand()
   {
      return controllerCoreCommand;
   }
}
