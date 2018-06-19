package us.ihmc.quadrupedRobotics.controlModules;

import org.apache.commons.lang3.mutable.MutableInt;
import us.ihmc.commonWalkingControlModules.controllerCore.command.virtualModelControl.VirtualModelControlCommand;
import us.ihmc.commons.MathTools;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FrameVector3DReadOnly;
import us.ihmc.graphicsDescription.appearance.AppearanceDefinition;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.yoGraphics.BagOfBalls;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsList;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.graphicsDescription.yoGraphics.plotting.ArtifactList;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.QuadrupedBodyHeightCommand;
import us.ihmc.quadrupedRobotics.controller.QuadrupedControllerToolbox;
import us.ihmc.quadrupedRobotics.controller.toolbox.LinearInvertedPendulumModel;
import us.ihmc.quadrupedRobotics.estimator.referenceFrames.QuadrupedReferenceFrames;
import us.ihmc.quadrupedRobotics.model.QuadrupedPhysicalProperties;
import us.ihmc.quadrupedRobotics.model.QuadrupedRuntimeEnvironment;
import us.ihmc.quadrupedRobotics.planning.QuadrupedStep;
import us.ihmc.quadrupedRobotics.planning.QuadrupedTimedStep;
import us.ihmc.quadrupedRobotics.planning.YoQuadrupedTimedStep;
import us.ihmc.quadrupedRobotics.planning.trajectory.DCMPlanner;
import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.*;

import java.util.ArrayList;
import java.util.List;

import static us.ihmc.graphicsDescription.appearance.YoAppearance.*;
import static us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition.GraphicType.BALL_WITH_CROSS;

public class QuadrupedBalanceManager
{
   private static final boolean debug = false;
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private static final int NUMBER_OF_STEPS_TO_CONSIDER = 8;

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final YoDouble robotTimestamp;

   private final LinearInvertedPendulumModel linearInvertedPendulumModel;

   private final QuadrupedCenterOfMassHeightManager centerOfMassHeightManager;
   private final QuadrupedMomentumRateOfChangeModule momentumRateOfChangeModule;
   private final QuadrupedStepAdjustmentController stepAdjustmentController;

   private final DCMPlanner dcmPlanner;

   private final FramePoint3D dcmPositionEstimate = new FramePoint3D();

   private final YoFramePoint3D yoDesiredDCMPosition = new YoFramePoint3D("desiredDCMPosition", worldFrame, registry);
   private final YoFrameVector3D yoDesiredDCMVelocity = new YoFrameVector3D("desiredDCMVelocity", worldFrame, registry);
   private final YoFramePoint3D yoFinalDesiredDCM = new YoFramePoint3D("finalDesiredDCMPosition", worldFrame, registry);
   private final YoFramePoint3D yoVrpPositionSetpoint = new YoFramePoint3D("vrpPositionSetpoint", ReferenceFrame.getWorldFrame(), registry);
   private final YoFramePoint3D yoDesiredCMP = new YoFramePoint3D("desiredCMP", ReferenceFrame.getWorldFrame(), registry);
   private final YoFramePoint2D yoAchievedCMP = new YoFramePoint2D("achievedCMP", worldFrame, registry);

   private final YoInteger numberOfStepsToConsider = new YoInteger("numberOfStepsToConsider", registry);

   private final ReferenceFrame supportFrame;

   private final QuadrupedControllerToolbox controllerToolbox;

   private final RecyclingArrayList<QuadrupedStep> adjustedActiveSteps;

   private final List<QuadrupedTimedStep> activeSteps = new ArrayList<>();

   // footstep graphics
   private static final int maxNumberOfFootstepGraphicsPerQuadrant = 4;
   private final FramePoint3D stepSequenceVisualizationPosition = new FramePoint3D();
   private final QuadrantDependentList<BagOfBalls> stepSequenceVisualization = new QuadrantDependentList<>();
   private final QuadrantDependentList<MutableInt> stepVisualizationCounter = new QuadrantDependentList<>(new MutableInt(0), new MutableInt(0),
                                                                                                          new MutableInt(0), new MutableInt(0));
   private static final QuadrantDependentList<AppearanceDefinition> stepSequenceAppearance = new QuadrantDependentList<>(YoAppearance.Red(),
                                                                                                                         YoAppearance.Blue(),
                                                                                                                         YoAppearance.RGBColor(1, 0.5, 0.0),
                                                                                                                         YoAppearance.RGBColor(0.0, 0.5, 1.0));

   public QuadrupedBalanceManager(QuadrupedControllerToolbox controllerToolbox, QuadrupedPhysicalProperties physicalProperties,
                                  YoVariableRegistry parentRegistry, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      this.controllerToolbox = controllerToolbox;

      numberOfStepsToConsider.set(NUMBER_OF_STEPS_TO_CONSIDER);

      QuadrupedReferenceFrames referenceFrames = controllerToolbox.getReferenceFrames();
      QuadrupedRuntimeEnvironment runtimeEnvironment = controllerToolbox.getRuntimeEnvironment();
      supportFrame = referenceFrames.getCenterOfFeetZUpFrameAveragingLowestZHeightsAcrossEnds();
      robotTimestamp = runtimeEnvironment.getRobotTimestamp();

      double nominalHeight = physicalProperties.getNominalCoMHeight();
      ReferenceFrame supportFrame = referenceFrames.getCenterOfFeetZUpFrameAveragingLowestZHeightsAcrossEnds();
      dcmPlanner = new DCMPlanner(runtimeEnvironment.getGravity(), nominalHeight, robotTimestamp, supportFrame, referenceFrames.getSoleFrames(), registry,
                                  yoGraphicsListRegistry, debug);

      linearInvertedPendulumModel = controllerToolbox.getLinearInvertedPendulumModel();

      centerOfMassHeightManager = new QuadrupedCenterOfMassHeightManager(controllerToolbox, physicalProperties, parentRegistry);
      momentumRateOfChangeModule = new QuadrupedMomentumRateOfChangeModule(controllerToolbox, registry);
      stepAdjustmentController = new QuadrupedStepAdjustmentController(controllerToolbox, registry);

      adjustedActiveSteps = new RecyclingArrayList<>(10, QuadrupedStep::new);
      adjustedActiveSteps.clear();

      if (yoGraphicsListRegistry != null)
         setupGraphics(yoGraphicsListRegistry);

      parentRegistry.addChild(registry);
   }

   private void setupGraphics(YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      String graphicsListName = getClass().getSimpleName();

      YoGraphicsList graphicsList = new YoGraphicsList(graphicsListName);
      ArtifactList artifactList = new ArtifactList(graphicsListName);

      YoGraphicPosition desiredDCMViz = new YoGraphicPosition("Desired DCM", yoDesiredDCMPosition, 0.01, Yellow(),
                                                              YoGraphicPosition.GraphicType.BALL_WITH_ROTATED_CROSS);
      YoGraphicPosition finalDesiredDCMViz = new YoGraphicPosition("Final Desired DCM", yoFinalDesiredDCM, 0.01, Beige(),
                                                                   YoGraphicPosition.GraphicType.BALL_WITH_ROTATED_CROSS);
      YoGraphicPosition yoCmpPositionSetpointViz = new YoGraphicPosition("Desired CMP", yoDesiredCMP, 0.012, YoAppearance.Purple(), BALL_WITH_CROSS);
      YoGraphicPosition achievedCMPViz = new YoGraphicPosition("Achieved CMP", yoAchievedCMP, 0.005, DarkRed(), YoGraphicPosition.GraphicType.BALL_WITH_CROSS);

      graphicsList.add(desiredDCMViz);
      graphicsList.add(finalDesiredDCMViz);
      graphicsList.add(yoCmpPositionSetpointViz);

      artifactList.add(desiredDCMViz.createArtifact());
      artifactList.add(finalDesiredDCMViz.createArtifact());
      artifactList.add(yoCmpPositionSetpointViz.createArtifact());
      artifactList.add(achievedCMPViz.createArtifact());

      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         AppearanceDefinition appearance = stepSequenceAppearance.get(robotQuadrant);
         String prefix = "timedStepController" + robotQuadrant.getPascalCaseName() + "GoalPositions";
         stepSequenceVisualization
               .set(robotQuadrant, new BagOfBalls(maxNumberOfFootstepGraphicsPerQuadrant, 0.015, prefix, appearance, registry, yoGraphicsListRegistry));
      }

      yoGraphicsListRegistry.registerYoGraphicsList(graphicsList);
      yoGraphicsListRegistry.registerArtifactList(artifactList);
   }

   private void updateFootstepGraphics(List<? extends QuadrupedTimedStep> steps)
   {
      for (RobotQuadrant quadrant : RobotQuadrant.values)
      {
         stepSequenceVisualization.get(quadrant).hideAll();
         stepVisualizationCounter.get(quadrant).setValue(0);
      }

      for (int i = 0; i < Math.min(steps.size(), numberOfStepsToConsider.getValue()); i++)
      {
         QuadrupedTimedStep step = steps.get(i);
         RobotQuadrant quadrant = step.getRobotQuadrant();
         int count = stepVisualizationCounter.get(quadrant).getValue();
         if (count == maxNumberOfFootstepGraphicsPerQuadrant)
            continue;

         step.getGoalPosition(stepSequenceVisualizationPosition);
         stepSequenceVisualization.get(quadrant).setBall(stepSequenceVisualizationPosition, count);
         stepVisualizationCounter.get(quadrant).setValue(count + 1);
      }
   }

   private void adjustActiveFootstepGraphics(List<? extends QuadrupedTimedStep> steps)
   {
      // should be at most two steps
      for (int i = 0; i < steps.size(); i++)
      {
         QuadrupedTimedStep step = steps.get(i);
         RobotQuadrant quadrant = step.getRobotQuadrant();
         step.getGoalPosition(stepSequenceVisualizationPosition);
         stepSequenceVisualization.get(quadrant).setBall(stepSequenceVisualizationPosition, 0);
      }
   }

   public void handleBodyHeightCommand(QuadrupedBodyHeightCommand command)
   {
      centerOfMassHeightManager.handleBodyHeightCommand(command);
   }

   public void clearStepSequence()
   {
      dcmPlanner.clearStepSequence();
   }

   public void addStepsToSequence(List<? extends QuadrupedTimedStep> steps)
   {
      for (int i = 0; i < Math.min(steps.size(), numberOfStepsToConsider.getIntegerValue()); i++)
         dcmPlanner.addStepToSequence(steps.get(i));

      updateFootstepGraphics(steps);

      updateActiveSteps(steps);
      centerOfMassHeightManager.setActiveSteps(activeSteps);
   }

   private void updateActiveSteps(List<? extends  QuadrupedTimedStep> steps)
   {
      activeSteps.clear();

      for (int i = 0; i < steps.size(); i++)
      {
         double currentTime = robotTimestamp.getDoubleValue();
         double startTime = steps.get(i).getTimeInterval().getStartTime();
         double endTime = steps.get(i).getTimeInterval().getEndTime();

         if (MathTools.intervalContains(currentTime, startTime, endTime))
         {
            activeSteps.add(steps.get(i));
         }
      }
   }

   private void initialize()
   {
      // update model
      centerOfMassHeightManager.update();
      linearInvertedPendulumModel.setComHeight(centerOfMassHeightManager.getDesiredHeight(supportFrame));

      // update dcm estimate
      controllerToolbox.getDCMPositionEstimate(dcmPositionEstimate);

      yoDesiredDCMPosition.set(dcmPositionEstimate);
      yoDesiredDCMVelocity.setToZero();

      momentumRateOfChangeModule.initialize();
   }

   public void initializeForStanding()
   {
      centerOfMassHeightManager.initialize();

      initialize();
      dcmPlanner.initializeForStanding();
   }

   public void initializeForStepping()
   {
      initialize();
      dcmPlanner.initializeForStepping(controllerToolbox.getContactStates(), dcmPositionEstimate);
   }

   public void completedStep(RobotQuadrant robotQuadrant)
   {
      stepAdjustmentController.completedStep(robotQuadrant);
   }

   public void compute()
   {
      centerOfMassHeightManager.update();
      linearInvertedPendulumModel.setComHeight(centerOfMassHeightManager.getDesiredHeight(supportFrame));

      // update dcm estimate
      controllerToolbox.getDCMPositionEstimate(dcmPositionEstimate);
      dcmPlanner.setCoMHeight(linearInvertedPendulumModel.getComHeight());

      dcmPlanner.computeDcmSetpoints(controllerToolbox.getContactStates(), yoDesiredDCMPosition, yoDesiredDCMVelocity);
      dcmPlanner.getFinalDesiredDCM(yoFinalDesiredDCM);

      if (debug)
         runDebugChecks();

      double desiredCenterOfMassHeightAcceleration = centerOfMassHeightManager.computeDesiredCenterOfMassHeightAcceleration();

      momentumRateOfChangeModule.setDCMEstimate(dcmPositionEstimate);
      momentumRateOfChangeModule.setDCMSetpoints(yoDesiredDCMPosition, yoDesiredDCMVelocity);
      momentumRateOfChangeModule.setDesiredCenterOfMassHeightAcceleration(desiredCenterOfMassHeightAcceleration);
      momentumRateOfChangeModule.compute(yoVrpPositionSetpoint, yoDesiredCMP);
   }

   private void runDebugChecks()
   {
      if (yoDesiredDCMPosition.containsNaN())
         throw new IllegalArgumentException("Desired DCM Position contains NaN");

      if (yoDesiredDCMVelocity.containsNaN())
         throw new IllegalArgumentException("Desired DCM Velocity contains NaN");
   }

   public RecyclingArrayList<QuadrupedStep> computeStepAdjustment(ArrayList<YoQuadrupedTimedStep> activeSteps)
   {
      RecyclingArrayList<QuadrupedStep> adjustedActiveSteps = stepAdjustmentController.computeStepAdjustment(activeSteps, yoDesiredDCMPosition);

      adjustActiveFootstepGraphics(activeSteps);
      return adjustedActiveSteps;
   }

   public void computeAchievedCMP(FrameVector3DReadOnly achievedLinearMomentumRate)
   {
      momentumRateOfChangeModule.computeAchievedCMP(achievedLinearMomentumRate, yoAchievedCMP);
   }

   public VirtualModelControlCommand<?> getVirtualModelControlCommand()
   {
      return momentumRateOfChangeModule.getMomentumRateCommand();
   }

   public FrameVector3DReadOnly getStepAdjustment(RobotQuadrant robotQuadrant)
   {
      return stepAdjustmentController.getStepAdjustment(robotQuadrant);
   }
}
