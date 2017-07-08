package us.ihmc.commonWalkingControlModules.controlModules.foot;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoPlaneContactState;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.SpatialAccelerationCommand;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.robotics.sensors.FootSwitchInterface;
import us.ihmc.robotics.weightMatrices.SolverWeightLevels;

public class FullyConstrainedState extends AbstractFootControlState
{
   private final FrameVector3D fullyConstrainedNormalContactVector;

   private final SpatialAccelerationCommand spatialAccelerationCommand = new SpatialAccelerationCommand();

   private final FramePoint2D cop = new FramePoint2D();
   private final FramePoint2D desiredCoP = new FramePoint2D();
   private final PartialFootholdControlModule partialFootholdControlModule;

   private final FootSwitchInterface footSwitch;

   public FullyConstrainedState(FootControlHelper footControlHelper, YoVariableRegistry registry)
   {
      super(ConstraintType.FULL, footControlHelper);

      fullyConstrainedNormalContactVector = footControlHelper.getFullyConstrainedNormalContactVector();
      partialFootholdControlModule = footControlHelper.getPartialFootholdControlModule();
      footSwitch = controllerToolbox.getFootSwitches().get(robotSide);
      spatialAccelerationCommand.setWeight(SolverWeightLevels.FOOT_SUPPORT_WEIGHT);
      spatialAccelerationCommand.set(rootBody, contactableFoot.getRigidBody());
      spatialAccelerationCommand.setPrimaryBase(pelvis);
      spatialAccelerationCommand.setSelectionMatrixToIdentity();
   }

   public void setWeight(double weight)
   {
      spatialAccelerationCommand.setWeight(weight);
   }

   public void setWeights(Vector3D angular, Vector3D linear)
   {
      spatialAccelerationCommand.setWeights(angular, linear);
   }

   @Override
   public void doTransitionIntoAction()
   {
      super.doTransitionIntoAction();
      controllerToolbox.setFootContactStateNormalContactVector(robotSide, fullyConstrainedNormalContactVector);
   }

   @Override
   public void doTransitionOutOfAction()
   {
      super.doTransitionOutOfAction();
   }

   @Override
   public void doSpecificAction()
   {
      if (partialFootholdControlModule != null)
      {
         footSwitch.computeAndPackCoP(cop);
         controllerToolbox.getDesiredCenterOfPressure(contactableFoot, desiredCoP);
         partialFootholdControlModule.compute(desiredCoP, cop);
         YoPlaneContactState contactState = controllerToolbox.getFootContactState(robotSide);
         boolean contactStateHasChanged = partialFootholdControlModule.applyShrunkPolygon(contactState);
         if (contactStateHasChanged)
            contactState.notifyContactStateHasChanged();
      }

      footAcceleration.setToZero(contactableFoot.getFrameAfterParentJoint(), rootBody.getBodyFixedFrame(), contactableFoot.getFrameAfterParentJoint());

      ReferenceFrame bodyFixedFrame = contactableFoot.getRigidBody().getBodyFixedFrame();
      footAcceleration.changeBodyFrameNoRelativeAcceleration(bodyFixedFrame);
      footAcceleration.changeFrameNoRelativeMotion(bodyFixedFrame);
      spatialAccelerationCommand.setSpatialAcceleration(bodyFixedFrame, footAcceleration);
   }

   @Override
   public InverseDynamicsCommand<?> getInverseDynamicsCommand()
   {
      return spatialAccelerationCommand;
   }

   @Override
   public FeedbackControlCommand<?> getFeedbackControlCommand()
   {
      return null;
   }
}
