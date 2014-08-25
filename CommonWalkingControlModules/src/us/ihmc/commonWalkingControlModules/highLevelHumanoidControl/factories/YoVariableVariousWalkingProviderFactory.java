package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controllers.Updatable;
import us.ihmc.commonWalkingControlModules.desiredFootStep.Footstep;
import us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepProvider;
import us.ihmc.commonWalkingControlModules.desiredFootStep.UserDesiredFootstepProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredChestOrientationProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredComHeightProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredFootPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredFootStateProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredHeadOrientationProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredPelvisLoadBearingProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredPelvisPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredThighLoadBearingProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.HandLoadBearingProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.HandPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.HandstepProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.ReinitializeWalkingControllerProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.UserDesiredHandLoadBearingProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.UserDesiredHandPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.UserDesiredHandstepProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.UserDesiredHeadOrientationProvider;
import us.ihmc.commonWalkingControlModules.packetProviders.ControlStatusProducer;
import us.ihmc.commonWalkingControlModules.packetProviders.DesiredHighLevelStateProvider;
import us.ihmc.commonWalkingControlModules.packetProviders.SystemErrControlStatusProducer;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.commonWalkingControlModules.trajectories.ConstantSwingTimeCalculator;
import us.ihmc.commonWalkingControlModules.trajectories.ConstantTransferTimeCalculator;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.trajectories.providers.TrajectoryParameters;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;

public class YoVariableVariousWalkingProviderFactory implements VariousWalkingProviderFactory
{
   public VariousWalkingProviders createVariousWalkingProviders(DoubleYoVariable yoTime, FullRobotModel fullRobotModel,
           WalkingControllerParameters walkingControllerParameters, CommonWalkingReferenceFrames referenceFrames, SideDependentList<ContactablePlaneBody> feet,
           ConstantTransferTimeCalculator transferTimeCalculator, ConstantSwingTimeCalculator swingTimeCalculator, ArrayList<Updatable> updatables,
           YoVariableRegistry registry, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      HandstepProvider handstepProvider = new UserDesiredHandstepProvider(fullRobotModel, registry, dynamicGraphicObjectsListRegistry);

      HandPoseProvider handPoseProvider = new UserDesiredHandPoseProvider(fullRobotModel,
                                             walkingControllerParameters.getDesiredHandPosesWithRespectToChestFrame(), registry);

      LinkedHashMap<Footstep, TrajectoryParameters> mapFromFootstepsToTrajectoryParameters = new LinkedHashMap<Footstep, TrajectoryParameters>();

      FootstepProvider footstepPovider = new UserDesiredFootstepProvider(feet, referenceFrames.getAnkleZUpReferenceFrames(), registry);

      DesiredHighLevelStateProvider highLevelStateProvider = null;
      DesiredHeadOrientationProvider headOrientationProvider = new UserDesiredHeadOrientationProvider(referenceFrames.getPelvisZUpFrame(), registry);
      DesiredComHeightProvider desiredComHeightProvider = null;
      DesiredPelvisPoseProvider pelvisPoseProvider = null;
      DesiredChestOrientationProvider chestOrientationProvider = null;
      DesiredFootPoseProvider footPoseProvider = null;
      ReinitializeWalkingControllerProvider reinitializeWalkingControllerProvider = null;

      HandLoadBearingProvider handLoadBearingProvider = new UserDesiredHandLoadBearingProvider(registry);
      DesiredFootStateProvider footLoadBearingProvider = null;
      DesiredThighLoadBearingProvider thighLoadBearingProvider = null;
      DesiredPelvisLoadBearingProvider pelvisLoadBearingProvider = null;

      ControlStatusProducer controlStatusProducer = new SystemErrControlStatusProducer();

      VariousWalkingProviders variousProviders = new VariousWalkingProviders(footstepPovider, handstepProvider, mapFromFootstepsToTrajectoryParameters,
                                                    headOrientationProvider, desiredComHeightProvider, pelvisPoseProvider, handPoseProvider,
                                                    handLoadBearingProvider, chestOrientationProvider, footPoseProvider, footLoadBearingProvider,
                                                    highLevelStateProvider, thighLoadBearingProvider, pelvisLoadBearingProvider,
                                                    reinitializeWalkingControllerProvider, controlStatusProducer);

      return variousProviders;
   }

}
