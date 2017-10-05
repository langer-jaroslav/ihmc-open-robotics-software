package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothCMPBasedICPPlanner;

import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoPlaneContactState;
import us.ihmc.commonWalkingControlModules.configurations.SmoothCMPPlannerParameters;
import us.ihmc.commonWalkingControlModules.controllers.Updatable;
import us.ihmc.commonWalkingControlModules.desiredFootStep.footstepGenerator.FootstepTestHelper;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothCMPBasedICPPlanner.CoPGeneration.CoPPointsInFoot;
import us.ihmc.commons.Epsilons;
import us.ihmc.commons.PrintTools;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.graphicsDescription.Graphics3DObject;
import us.ihmc.graphicsDescription.appearance.YoAppearanceRGBColor;
import us.ihmc.graphicsDescription.yoGraphics.BagOfBalls;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphic;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicShape;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsList;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidRobotics.footstep.FootSpoof;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.humanoidRobotics.footstep.FootstepTiming;
import us.ihmc.robotics.geometry.FrameConvexPolygon2d;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.math.frames.YoFramePose;
import us.ihmc.robotics.referenceFrames.MidFootZUpGroundFrame;
import us.ihmc.robotics.referenceFrames.ZUpFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.SimulationConstructionSetParameters;
import us.ihmc.simulationconstructionset.gui.tools.SimulationOverheadPlotterFactory;
import us.ihmc.tools.thread.ThreadTools;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

public class SmoothCMPBasedICPPlannerTest
{
   private static final String testClassName = "UltimateSmoothCMPBasedICPPlannerTest";
   private static final double epsilon = Epsilons.ONE_TEN_MILLIONTH;
   private final static double spatialEpsilon = Epsilons.ONE_THOUSANDTH; // 1 mm
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private static final boolean visualize = true;
   private static final boolean keepSCSUp = true;
   private static final boolean testAssertions = false;

   // Simulation parameters
   private final double dt = 0.001;
   private final double omega = 3.5;

   // Physical parameters
   private final double robotMass = 150;
   private final double gravity = 9.81;
   private final double xToAnkle = 0.05;
   private final double yToAnkle = 0.20;
   private final double zToAnkle = -0.15;
   private final double toeWidth = 0.085;
   private final double heelWidth = 0.11;
   private final double footLength = 0.22;
   private final double footLengthBack = 0.085;
   private final double footLengthForward = footLength - footLengthBack;
   private final double coefficientOfFriction = 0.1;

   // Robot parameters
   private final FramePose initialPose = new FramePose(worldFrame, new Point3D(0.0, 0.0, 0.0), new Quaternion());

   // Planning parameters
   private final double defaultSwingTime = 0.6;
   private final double defaultTransferTime = 0.1;
   private final double defaultInitialSwingTime = defaultSwingTime;
   private final double defaultInitialTransferTime = 0.2;
   private final double defaultFinalSwingTime = defaultSwingTime;
   private final double defaultFinalTransferTime = 0.2;

   private final double stepWidth = 0.25;
   private final double stepLength = 0.5;
   private final int numberOfFootstepsToConsider = 3;
   private final int numberOfFootstepsToTest = 10;
   private final List<Point2D> contactPointsInFootFrame = Stream.of(new Point2D(footLengthForward, toeWidth / 2.0),
                                                                    new Point2D(footLengthForward, -toeWidth / 2.0),
                                                                    new Point2D(-footLengthBack, -heelWidth / 2.0),
                                                                    new Point2D(-footLengthBack, heelWidth / 2.0))
                                                                .collect(Collectors.toList());

   // Variables for testing and simulation
   private final FramePoint3D comPosition = new FramePoint3D();
   private final FrameVector3D comVelocity = new FrameVector3D();
   private final FrameVector3D comAcceleration = new FrameVector3D();
   private final FramePoint3D icpPosition = new FramePoint3D();
   private final FrameVector3D icpVelocity = new FrameVector3D();
   private final FrameVector3D icpAcceleration = new FrameVector3D();
   private final FramePoint3D cmpPosition = new FramePoint3D();
   private final FrameVector3D cmpVelocity = new FrameVector3D();
   private final FrameVector3D centroidalAngularMomentum = new FrameVector3D();
   private final FrameVector3D centroidalTorque = new FrameVector3D();
   private final FramePoint3D copPosition = new FramePoint3D();
   private final FrameVector3D copVelocity = new FrameVector3D();
   private final FrameVector3D copAcceleration = new FrameVector3D();

   private YoVariableRegistry registry;
   private YoDouble yoTime;
   private YoBoolean inDoubleSupport;
   private SmoothCMPBasedICPPlanner planner;
   private SmoothCMPPlannerParameters plannerParameters;
   private SideDependentList<FootSpoof> feet;
   private BipedSupportPolygons bipedSupportPolygons;
   private SideDependentList<ReferenceFrame> ankleZUpFrames;
   private SideDependentList<ReferenceFrame> soleZUpFrames;
   private ReferenceFrame midFeetZUpFrame;
   private SideDependentList<YoPlaneContactState> contactStates;
   private List<Footstep> footstepList;
   private List<FootstepTiming> timingList;
   private boolean newTestStart;

   // Variables for visualization
   private YoGraphicsListRegistry graphicsListRegistry;
   private int numberOfTrackBalls = 100;
   private double trackBallSize = 0.002;
   private int numberOfCornerPoints = numberOfFootstepsToConsider * 6 + 1;
   private double cornerPointBallSize = 0.005;
   private double footstepHeight = 0.02;
   private BagOfBalls comTrack, icpTrack, cmpTrack, copTrack;
   private BagOfBalls comInitialCornerPoints, icpInitialCornerPoints, comFinalCornerPoints, icpFinalCornerPoints, copCornerPoints;
   private List<YoFramePose> nextFootstepPoses;
   private SideDependentList<YoFramePose> currentFootLocations;
   private ArrayList<Updatable> updatables;
   private SimulationConstructionSet scs;
   private int SCS_BUFFER_SIZE = 100000;
   private YoAppearanceRGBColor nextFootstepColor = new YoAppearanceRGBColor(Color.BLUE, 0.5);
   private YoAppearanceRGBColor leftFootstepColor = new YoAppearanceRGBColor(new Color(0.85f, 0.35f, 0.65f, 1.0f), 0.5);
   private YoAppearanceRGBColor rightFootstepColor = new YoAppearanceRGBColor(new Color(0.15f, 0.8f, 0.15f, 1.0f), 0.5);
   private Color comPointsColor = Color.BLACK;
   private Color icpPointsColor = Color.RED;
   private Color cmpPointsColor = Color.YELLOW;
   private Color copPointsColor = Color.WHITE;

   @Before
   public void setupTest()
   {
      this.newTestStart = true;
      this.registry = new YoVariableRegistry(testClassName);
      this.yoTime = new YoDouble("TestTime", registry);
      this.feet = new SideDependentList<>();
      this.ankleZUpFrames = new SideDependentList<>();
      this.soleZUpFrames = new SideDependentList<>();
      this.contactStates = new SideDependentList<>();
      this.updatables = new ArrayList<>();

      for (RobotSide side : RobotSide.values())
      {
         String footName = side.getCamelCaseName();
         FootSpoof foot = new FootSpoof(footName + "Foot", xToAnkle, yToAnkle, zToAnkle, contactPointsInFootFrame, coefficientOfFriction);
         FramePose footPose = new FramePose(initialPose);
         footPose.appendTranslation(0.0, side.negateIfRightSide(stepWidth / 2.0), 0.0);
         foot.setSoleFrame(footPose);

         this.feet.put(side, foot);
         this.ankleZUpFrames.put(side, new ZUpFrame(worldFrame, foot.getFrameAfterParentJoint(), footName + "AnkleZUpFrame"));
         this.soleZUpFrames.put(side, new ZUpFrame(worldFrame, foot.getSoleFrame(), footName + "SoleZUpFrame"));
         YoPlaneContactState contactState = new YoPlaneContactState(footName + "ContactState", foot.getRigidBody(), foot.getSoleFrame(),
                                                                    foot.getContactPoints2d(), foot.getCoefficientOfFriction(), registry);
         contactState.setFullyConstrained();
         this.contactStates.put(side, contactState);
      }
      this.midFeetZUpFrame = new MidFootZUpGroundFrame("MidFeetFrame", soleZUpFrames.get(RobotSide.LEFT), soleZUpFrames.get(RobotSide.RIGHT));
      this.bipedSupportPolygons = new BipedSupportPolygons(ankleZUpFrames, midFeetZUpFrame, soleZUpFrames, registry, graphicsListRegistry);
      this.bipedSupportPolygons.updateUsingContactStates(contactStates);
      if (visualize)
         setupVisualization();

      updatables.add(new Updatable()
      {
         @Override
         public void update(double time)
         {
            if (visualize)
            {
               for (YoGraphicsList yoGraphicsList : graphicsListRegistry.getYoGraphicsLists())
               {
                  for (YoGraphic yoGraphic : yoGraphicsList.getYoGraphics())
                  {
                     yoGraphic.update();
                  }
               }
            }

            for (RobotSide robotSide : RobotSide.values)
            {
               ankleZUpFrames.get(robotSide).update();
               soleZUpFrames.get(robotSide).update();
            }
            midFeetZUpFrame.update();
         }
      });
   }

   private void setupVisualization()
   {
      this.graphicsListRegistry = new YoGraphicsListRegistry();
      setupTrackBallsVisualization();
      setupCornerPointBallsVisualization();
      setupNextFootstepVisualization();
      setupCurrentFootPoseVisualization();
      setupSCS();
   }

   private void setupSCS()
   {
      SimulationConstructionSetParameters scsParameters = new SimulationConstructionSetParameters(true, SCS_BUFFER_SIZE);
      Robot robot = new Robot("Dummy");
      yoTime = robot.getYoTime();
      scs = new SimulationConstructionSet(robot, scsParameters);
   }

   private void startSCS()
   {
      scs.addYoVariableRegistry(registry);
      scs.addYoGraphicsListRegistry(graphicsListRegistry);
      scs.setPlaybackRealTimeRate(0.025);
      Graphics3DObject linkGraphics = new Graphics3DObject();
      linkGraphics.addCoordinateSystem(0.3);
      scs.addStaticLinkGraphics(linkGraphics);
      scs.setCameraFix(0.0, 0.0, 0.5);
      scs.setCameraPosition(-0.5, 0.0, 1.0);
      SimulationOverheadPlotterFactory simulationOverheadPlotterFactory = scs.createSimulationOverheadPlotterFactory();
      simulationOverheadPlotterFactory.addYoGraphicsListRegistries(graphicsListRegistry);
      simulationOverheadPlotterFactory.createOverheadPlotter();
      scs.startOnAThread();
   }

   private void setupCurrentFootPoseVisualization()
   {
      currentFootLocations = new SideDependentList<>();
      for (RobotSide side : RobotSide.values())
      {
         Graphics3DObject footstepGraphic = new Graphics3DObject();
         footstepGraphic.addExtrudedPolygon(contactPointsInFootFrame, footstepHeight, side == RobotSide.LEFT ? leftFootstepColor : rightFootstepColor);
         YoFramePose footPose = new YoFramePose(side.getCamelCaseName() + "FootPose", worldFrame, registry);
         currentFootLocations.put(side, footPose);
         graphicsListRegistry.registerYoGraphic("currentFootPose", new YoGraphicShape(side.getCamelCaseName() + "FootViz", footstepGraphic, footPose, 1.0));
      }
   }

   private void setupNextFootstepVisualization()
   {
      nextFootstepPoses = new ArrayList<>();
      for (int i = 0; i < numberOfFootstepsToConsider; i++)
      {
         Graphics3DObject nextFootstepGraphic = new Graphics3DObject();
         nextFootstepGraphic.addExtrudedPolygon(contactPointsInFootFrame, footstepHeight, nextFootstepColor);
         YoFramePose nextFootstepPose = new YoFramePose("NextFootstep" + i + "Pose", worldFrame, registry);
         nextFootstepPoses.add(nextFootstepPose);
         graphicsListRegistry.registerYoGraphic("UpcomingFootsteps",
                                                new YoGraphicShape("NextFootstep" + i + "Viz", nextFootstepGraphic, nextFootstepPose, 1.0));
      }
   }

   private void setupCornerPointBallsVisualization()
   {
      comInitialCornerPoints = new BagOfBalls(numberOfCornerPoints, cornerPointBallSize*1.5, "CoMInitialCornerPoint", new YoAppearanceRGBColor(comPointsColor, 0.5), registry, graphicsListRegistry);
      comFinalCornerPoints = new BagOfBalls(numberOfCornerPoints, cornerPointBallSize, "CoMFinalCornerPoint", new YoAppearanceRGBColor(comPointsColor, 0.0), registry, graphicsListRegistry);
      icpInitialCornerPoints = new BagOfBalls(numberOfCornerPoints, cornerPointBallSize*1.5, "ICPInitialCornerPoint", new YoAppearanceRGBColor(icpPointsColor, 0.5), registry, graphicsListRegistry);
      icpFinalCornerPoints = new BagOfBalls(numberOfCornerPoints, cornerPointBallSize, "ICPFinalCornerPoint", new YoAppearanceRGBColor(icpPointsColor, 0.0), registry, graphicsListRegistry);
      copCornerPoints = new BagOfBalls(numberOfCornerPoints, cornerPointBallSize, "CoPCornerPoint", new YoAppearanceRGBColor(copPointsColor, 0.0), registry, graphicsListRegistry);
   }

   private void setupTrackBallsVisualization()
   {
      comTrack = new BagOfBalls(numberOfTrackBalls, trackBallSize, "CoMTrack", new YoAppearanceRGBColor(comPointsColor, 0.0), registry, graphicsListRegistry);
      icpTrack = new BagOfBalls(numberOfTrackBalls, trackBallSize, "ICPTrack", new YoAppearanceRGBColor(icpPointsColor, 0.0), registry, graphicsListRegistry);
      cmpTrack = new BagOfBalls(numberOfTrackBalls, trackBallSize, "CMPTrack", new YoAppearanceRGBColor(cmpPointsColor, 0.0), registry, graphicsListRegistry);
      copTrack = new BagOfBalls(numberOfTrackBalls, trackBallSize, "CoPTrack", new YoAppearanceRGBColor(copPointsColor, 0.0), registry, graphicsListRegistry);
   }

   @After
   public void cleanUpTest()
   {
      if(scs != null)
         scs.closeAndDispose();
   }

   @Test
   public void testForDiscontinuitiesWithoutAngularMomentum()
   {
      boolean isAMOn = false;
      setupPlanner(isAMOn);
      simulate(true, false, true);
   }

   @Test
   public void testForDiscontinuitiesWithAngularMomentum()
   {
      boolean isAMOn = true;
      setupPlanner(isAMOn);
      simulate(true, false, true);
   }

   @Test
   public void testForPlanningConsistencyWithoutAngularMomentum()
   {
      boolean isAMOn = false;
      setupPlanner(isAMOn);
      simulate(false, true, true);
   }

   @Test
   public void testForPlanningConsistencyWithAngularMomentum()
   {
      boolean isAMOn = true;
      setupPlanner(isAMOn);
      simulate(false, true, true);
   }

   private void setupPlanner(boolean isAMOn)
   {
      plannerParameters = new SmoothCMPPlannerParameters()
      {
         @Override
         public boolean planWithAngularMomentum()
         {
            return isAMOn;
         };

         @Override
         public int getNumberOfFootstepsToConsider()
         {
            return numberOfFootstepsToConsider;
         }
      };
      this.planner = new SmoothCMPBasedICPPlanner(robotMass, bipedSupportPolygons, feet, plannerParameters.getNumberOfFootstepsToConsider(),
                                                  plannerParameters.getNumberOfCoPWayPointsPerFoot(), registry, graphicsListRegistry, gravity);
      this.planner.initializeParameters(plannerParameters);
      this.planner.setFinalTransferDuration(defaultFinalTransferTime);
      this.planner.setOmega0(omega);
   }

   // Variables for storing values 
   private final FramePoint3D comPositionForDiscontinuity = new FramePoint3D();
   private final FramePoint3D icpPositionForDiscontinuity = new FramePoint3D();
   private final FramePoint3D cmpPositionForDiscontinuity = new FramePoint3D();
   private final FramePoint3D copPositionForDiscontinuity = new FramePoint3D();

   private void testForDiscontinuities()
   {
      if(!newTestStart)
      {
         assertTrueLocal(comPositionForDiscontinuity.epsilonEquals(comPosition, spatialEpsilon));
         assertTrueLocal(icpPositionForDiscontinuity.epsilonEquals(icpPosition, spatialEpsilon));
         assertTrueLocal(cmpPositionForDiscontinuity.epsilonEquals(cmpPosition, spatialEpsilon));
         assertTrueLocal(copPositionForDiscontinuity.epsilonEquals(copPosition, spatialEpsilon));
      }
      getPredictedValue(comPositionForDiscontinuity, comPosition, comVelocity, dt);
      getPredictedValue(icpPositionForDiscontinuity, icpPosition, comVelocity, dt);
      getPredictedValue(cmpPositionForDiscontinuity, cmpPosition, comVelocity, dt);
      getPredictedValue(copPositionForDiscontinuity, copPosition, comVelocity, dt);
   }
   
   private void getPredictedValue(FramePoint3D predictedValueToPack, FramePoint3D currentValue, FrameVector3D rateOfChange, double deltaT)
   {
      predictedValueToPack.setIncludingFrame(rateOfChange);
      predictedValueToPack.scale(deltaT);
      predictedValueToPack.changeFrame(currentValue.getReferenceFrame());
      predictedValueToPack.add(currentValue);
   }

   private void testForPlanningConsistency()
   {
      
   }

   private void updateVisualizePerTick()
   {
      updateCoMTrack();
      updateICPTrack();
      updateCMPTrack();
      updateCoPTrack();
      scs.tickAndUpdate();
   }

   private void updateUpdatables(double time)
   {
      for (Updatable updatable : updatables)
      {
         updatable.update(time);
      }
   }

   private void updateCoMTrack()
   {
      comTrack.setBallLoop(comPosition);
   }

   private void updateICPTrack()
   {
      icpTrack.setBallLoop(icpPosition);
   }

   private void updateCMPTrack()
   {
      cmpTrack.setBallLoop(cmpPosition);
   }

   private void updateCoPTrack()
   {
      copTrack.setBallLoop(copPosition);
   }

   private void updateVisualization(int stepIndex)
   {
      updateCurrentFootsteps();
      updateNextFootsteps(stepIndex);
      updateCoMCornerPoints();
      updateICPCornerPoints();
      updateCoPCornerPoints();
   }

   private void updateCurrentFootsteps()
   {
      for (RobotSide side : RobotSide.values)
      {
         YoFramePose footPose = currentFootLocations.get(side);
         if (contactStates.get(side).inContact())
         {
            footPose.setFromReferenceFrame(feet.get(side).getSoleFrame());
         }
         else
         {
            footPose.setToNaN();
         }
      }
   }

   FrameConvexPolygon2d tempConvexPolygon = new FrameConvexPolygon2d();

   private void updateNextFootsteps(int stepIndex)
   {
      int numberOfStepToUpdate = (numberOfFootstepsToTest - stepIndex) < numberOfFootstepsToConsider ? (numberOfFootstepsToTest - stepIndex)
            : numberOfFootstepsToConsider;
      int nextStepIndex;
      for (nextStepIndex = 0; nextStepIndex < numberOfStepToUpdate; nextStepIndex++)
      {
         nextFootstepPoses.get(nextStepIndex).set(footstepList.get(stepIndex + nextStepIndex).getFootstepPose());
      }
      
      for(; nextStepIndex < numberOfFootstepsToConsider; nextStepIndex++)
      {
         nextFootstepPoses.get(nextStepIndex).setToNaN();
      }
   }

   private void updateCoMCornerPoints()
   {
      List<FramePoint3D> comInitialDesiredPositions = planner.getInitialDesiredCenterOfMassPositions();
      List<FramePoint3D> comFinalDesiredPositions = planner.getFinalDesiredCenterOfMassPositions();
      comInitialCornerPoints.reset();
      for (int i = 0; i < comInitialDesiredPositions.size(); i++)
      {
         comInitialCornerPoints.setBall(comInitialDesiredPositions.get(i));
      }
      comFinalCornerPoints.reset();
      for (int i = 0; i < comFinalDesiredPositions.size(); i++)
      {
         comFinalCornerPoints.setBall(comFinalDesiredPositions.get(i));
      }
   }

   private void updateICPCornerPoints()
   {
      List<FramePoint3D> icpInitialDesiredPositions = planner.getInitialDesiredCapturePointPositions();
      List<FramePoint3D> icpFinalDesiredPositions = planner.getFinalDesiredCapturePointPositions();
      icpInitialCornerPoints.reset();
      for (int i = 0; i < icpInitialDesiredPositions.size(); i++)
      {
         icpInitialCornerPoints.setBall(icpInitialDesiredPositions.get(i));
      }
      icpFinalCornerPoints.reset();
      for (int i = 0; i < icpFinalDesiredPositions.size(); i++)
      {
         icpFinalCornerPoints.setBall(icpFinalDesiredPositions.get(i));
      }
   }

   FramePoint3D tempFramePoint = new FramePoint3D();
   private void updateCoPCornerPoints()
   {
      List<CoPPointsInFoot> copCornerPointPositions = planner.getCoPWaypoints();
      copCornerPoints.reset();
      for(int i = 0; i < copCornerPointPositions.size(); i++)
      {
         CoPPointsInFoot copPoints = copCornerPointPositions.get(i);
         for (int j = 0; j < copPoints.getCoPPointList().size(); j++)
         {
            copPoints.getWaypointInWorldFrameReadOnly(j).getFrameTuple(tempFramePoint);
            tempFramePoint.add(0.0, 0.0, 0.05);
            copCornerPoints.setBall(tempFramePoint);
         }
      }
   }

   @SuppressWarnings("unused")
   private void simulate(boolean checkForDiscontinuities, boolean checkForPlanningConsistency, boolean checkIfDyanmicsAreSatisfied)
   {
      if (visualize)
         startSCS();
      planFootsteps();
      inDoubleSupport = new YoBoolean("inDoubleSupport", registry);
      inDoubleSupport.set(true);

      for (RobotSide side : RobotSide.values)
         contactStates.get(side).setFullyConstrained();
      bipedSupportPolygons.updateUsingContactStates(contactStates);

      for (int currentStepCount = 0; currentStepCount < numberOfFootstepsToTest;)
      {
         addFootsteps(currentStepCount, footstepList, timingList);
         updateContactState(currentStepCount);
         if(inDoubleSupport.getBooleanValue())
         {
            planner.setTransferToSide(footstepList.get(currentStepCount).getRobotSide().getOppositeSide());
            planner.initializeForTransfer(yoTime.getDoubleValue());
         }
         else
         {
            planner.setSupportLeg(footstepList.get(currentStepCount).getRobotSide().getOppositeSide());
            planner.initializeForSingleSupport(yoTime.getDoubleValue());
         }
         
         if(visualize)
            updateVisualization(currentStepCount);
         if(checkForPlanningConsistency)
            testForPlanningConsistency();
         simulateTicks(checkForDiscontinuities, checkIfDyanmicsAreSatisfied, (inDoubleSupport.getBooleanValue() ? timingList.get(currentStepCount).getTransferTime()
               : timingList.get(currentStepCount).getSwingTime()));
         currentStepCount = updateStateMachine(currentStepCount);
      }

      addFootsteps(numberOfFootstepsToTest, footstepList, timingList);
      updateContactState(-1);
      planner.setTransferToSide(footstepList.get(numberOfFootstepsToTest - 1).getRobotSide());
      planner.initializeForStanding(yoTime.getDoubleValue());

      if(visualize)
         updateVisualization(numberOfFootstepsToTest);
      if(checkForPlanningConsistency)
         testForPlanningConsistency();
      simulateTicks(checkForDiscontinuities, checkIfDyanmicsAreSatisfied, defaultFinalTransferTime);

      if (visualize && keepSCSUp)
         ThreadTools.sleepForever();
   }

   private int updateStateMachine(int currentStepCount)
   {
      if (inDoubleSupport.getBooleanValue())
      {
         inDoubleSupport.set(false);
      }
      else
      {
         inDoubleSupport.set(true);
         currentStepCount++;
      }
      return currentStepCount;
   }

   private void simulateTicks(boolean checkForDiscontinuities, boolean checkIfDyanmicsAreSatisfied, double totalTime)
   {
      for (double timeInState = 0.0; timeInState < totalTime; timeInState += dt)
      {
         simulateOneTick(checkForDiscontinuities, checkIfDyanmicsAreSatisfied);
      }
   }

   private void updateContactState(int currentStepCount)
   {
      if (inDoubleSupport.getBooleanValue())
      {
         for (RobotSide side : RobotSide.values)
            contactStates.get(side).setFullyConstrained();
      }
      else
      {
         RobotSide swingSide = footstepList.get(currentStepCount).getRobotSide();
         contactStates.get(swingSide).clear();
         contactStates.get(swingSide.getOppositeSide()).setFullyConstrained();
         FootSpoof foot = feet.get(swingSide);
         foot.setSoleFrame(footstepList.get(currentStepCount).getFootstepPose());
      }
      bipedSupportPolygons.updateUsingContactStates(contactStates);
   }

   private void simulateOneTick(boolean checkForDiscontinuities, boolean checkIfDyanmicsAreSatisfied)
   {
      getAllVariablesFromPlanner();
      updateUpdatables(yoTime.getDoubleValue());
      if (checkForDiscontinuities)
         testForDiscontinuities();
      if (checkIfDyanmicsAreSatisfied)
         testIfDynamicsAreSatisified();
      if (visualize)
         updateVisualizePerTick();
   }

   private void getAllVariablesFromPlanner()
   {
      yoTime.add(dt);
      planner.compute(yoTime.getDoubleValue());
      planner.getDesiredCenterOfPressurePosition(copPosition);
      planner.getDesiredCenterOfPressureVelocity(copVelocity);
      planner.getDesiredCenterOfPressureVelocity(copAcceleration);
      planner.getDesiredCentroidalAngularMomentum(centroidalAngularMomentum);
      planner.getDesiredCentroidalTorque(centroidalTorque);
      planner.getDesiredCentroidalMomentumPivotPosition(cmpPosition);
      planner.getDesiredCentroidalMomentumPivotVelocity(cmpVelocity);
      planner.getDesiredCapturePointPosition(icpPosition);
      planner.getDesiredCapturePointVelocity(icpVelocity);
      planner.getDesiredCapturePointAcceleration(icpAcceleration);
      planner.getDesiredCenterOfMassPosition(comPosition);
      planner.getDesiredCenterOfMassVelocity(comVelocity);
      planner.getDesiredCenterOfMassAcceleration(comAcceleration);
   }

   private void planFootsteps()
   {
      FootstepTestHelper footstepHelper = new FootstepTestHelper(feet);
      footstepList = footstepHelper.createFootsteps(stepWidth, stepLength, numberOfFootstepsToTest);
      timingList = new ArrayList<>(footstepList.size());
      timingList.add(new FootstepTiming(defaultInitialSwingTime, defaultInitialTransferTime));
      for (int i = 0; i < footstepList.size() - 1; i++)
         timingList.add(new FootstepTiming(defaultSwingTime, defaultTransferTime));
   }

   private void addFootsteps(int currentFootstepIndex, List<Footstep> footstepList, List<FootstepTiming> timingList)
   {
      planner.clearPlan();
      for (int i = currentFootstepIndex; i < Math.min(footstepList.size(), currentFootstepIndex + numberOfFootstepsToConsider); i++)
      {
         planner.addFootstepToPlan(footstepList.get(i), timingList.get(i));
      }
   }

   private void testIfDynamicsAreSatisified()
   {
      assertTrueLocal("CoM dynamics not satisfied, t: " + yoTime.getDoubleValue() + " COM Position: " + comPosition.toString() + " ICP Velocity: "
            + comVelocity.toString() + " ICP Position: " + icpPosition.toString(), checkCoMDynamics(comPosition, comVelocity, icpPosition));
      assertTrueLocal("ICP dynamics not satisfied, t: " + yoTime.getDoubleValue() + " ICP Position: " + icpPosition.toString() + " ICP Velocity: "
            + icpVelocity.toString() + " CMP Position: " + cmpPosition.toString(), checkICPDynamics(icpPosition, icpVelocity, cmpPosition));
   }

   FrameVector3D icpVelocityFromDynamics = new FrameVector3D();

   private boolean checkICPDynamics(FramePoint3D icpPosition, FrameVector3D icpVelocity, FramePoint3D cmpPosition)
   {
      icpVelocityFromDynamics.sub(icpPosition, cmpPosition);
      icpVelocityFromDynamics.scale(omega);
      return icpVelocity.epsilonEquals(icpVelocityFromDynamics, epsilon);
   }

   FrameVector3D comVelocityFromDynamics = new FrameVector3D();

   private boolean checkCoMDynamics(FramePoint3D comPosition, FrameVector3D comVelocity, FramePoint3D icpPosition)
   {
      comVelocityFromDynamics.sub(icpPosition, comPosition);
      comVelocityFromDynamics.scale(omega);
      return comVelocity.epsilonEquals(comVelocityFromDynamics, epsilon);
   }
   
   private void assertTrueLocal(boolean assertion)
   {
      assertTrueLocal(null, assertion);
   }
   
   private void assertTrueLocal(String statement, boolean assertion)
   {
      if(testAssertions)
         assertTrue(statement, assertion);
   }
}