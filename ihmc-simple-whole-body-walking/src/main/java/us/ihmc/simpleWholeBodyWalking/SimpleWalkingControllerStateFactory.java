package us.ihmc.simpleWholeBodyWalking;

import us.ihmc.commonWalkingControlModules.configurations.ICPWithTimeFreezingPlannerParameters;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.HighLevelControllerFactoryHelper;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.HighLevelControllerStateFactory;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.HighLevelControllerState;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HighLevelControllerName;

public class SimpleWalkingControllerStateFactory implements HighLevelControllerStateFactory
{
   private SimpleWalkingControllerState walkingControllerState;
   private SimpleControlManagerFactory managerFactory;
   private ICPWithTimeFreezingPlannerParameters capturePointPlannerParameters;
   private boolean USE_LQR_MOMENTUM_CONTROLLER = true;
   
   public SimpleWalkingControllerStateFactory(ICPWithTimeFreezingPlannerParameters capturePointPlannerParameters) {
      this.capturePointPlannerParameters = capturePointPlannerParameters;
   }
   
   @Override
   public HighLevelControllerState getOrCreateControllerState(HighLevelControllerFactoryHelper controllerFactoryHelper)
   {
      if (walkingControllerState == null)
      {
         managerFactory = new SimpleControlManagerFactory(controllerFactoryHelper.getHighLevelHumanoidControllerToolbox().getYoVariableRegistry());
         managerFactory.setHighLevelHumanoidControllerToolbox(controllerFactoryHelper.getHighLevelHumanoidControllerToolbox());
         managerFactory.setWalkingControllerParameters(controllerFactoryHelper.getWalkingControllerParameters());
         managerFactory.setCapturePointPlannerParameters(capturePointPlannerParameters);
         
         walkingControllerState = new SimpleWalkingControllerState(controllerFactoryHelper.getCommandInputManager(), controllerFactoryHelper.getStatusMessageOutputManager(),
                                                                   managerFactory, controllerFactoryHelper.getHighLevelHumanoidControllerToolbox(),
                                                                   controllerFactoryHelper.getHighLevelControllerParameters(),
                                                                   controllerFactoryHelper.getWalkingControllerParameters(), USE_LQR_MOMENTUM_CONTROLLER);
      }

      return walkingControllerState;
   }

   @Override
   public HighLevelControllerName getStateEnum()
   {
      return HighLevelControllerName.WALKING;
   }
}
