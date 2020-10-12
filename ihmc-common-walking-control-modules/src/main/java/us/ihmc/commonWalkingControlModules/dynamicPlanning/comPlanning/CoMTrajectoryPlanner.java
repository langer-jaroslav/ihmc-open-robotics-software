package us.ihmc.commonWalkingControlModules.dynamicPlanning.comPlanning;

import java.util.ArrayList;
import java.util.List;

import org.ejml.data.*;
import org.ejml.dense.row.CommonOps_DDRM;

import org.ejml.interfaces.linsol.LinearSolverSparse;
import org.ejml.sparse.FillReducing;
import org.ejml.sparse.csc.CommonOps_DSCC;
import org.ejml.sparse.csc.factory.LinearSolverFactory_DSCC;
import us.ihmc.commonWalkingControlModules.capturePoint.CapturePointTools;
import us.ihmc.commons.MathTools;
import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.euclid.geometry.LineSegment3D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.*;
import us.ihmc.log.LogTools;
import us.ihmc.matrixlib.NativeCommonOps;
import us.ihmc.robotics.math.trajectories.Trajectory3D;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFramePoint3D;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFrameVector3D;
import us.ihmc.yoVariables.providers.DoubleProvider;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

/**
 * <p>
 *    This is the main class of the trajectory-based CoM Trajectory Planner.
 * </p>
 * <p>
 *    This class assumes that the final phase is always the "stopping" phase, where the CoM is supposed to come to rest.
 *    This means that the final VRP is the terminal DCM location
 *  </p>
 *  <p>
 *     The CoM has the following definitions:
 *     <li>      x(t) = c<sub>0</sub> e<sup>&omega; t</sup> + c<sub>1</sub> e<sup>-&omega; t</sup> + c<sub>2</sub> t<sup>3</sup> + c<sub>3</sub> t<sup>2</sup> +
 *     c<sub>4</sub> t + c<sub>5</sub></li>
 *     <li> d/dt x(t) = &omega; c<sub>0</sub> e<sup>&omega; t</sup> - &omega; c<sub>1</sub> e<sup>-&omega; t</sup> + 3 c<sub>2</sub> t<sup>2</sup> +
 *     2 c<sub>3</sub> t+ c<sub>4</sub>
 *     <li> d<sup>2</sup> / dt<sup>2</sup> x(t) = &omega;<sup>2</sup> c<sub>0</sub> e<sup>&omega; t</sup> + &omega;<sup>2</sup> c<sub>1</sub> e<sup>-&omega; t</sup>
 *     + 6 c<sub>2</sub> t + 2 c<sub>3</sub>  </li>
 *  </p>
 *
 *
 *    <p> From this, it follows that the VRP has the trajectory
 *    <li> v(t) =  c<sub>2</sub> t<sup>3</sup> + c<sub>3</sub> t<sup>2</sup> + (c<sub>4</sub> - 6/&omega;<sup>2</sup> c<sub>2</sub>) t - 2/&omega; c<sub>3</sub> + c<sub>5</sub></li>
 *    </p>
 */
public class CoMTrajectoryPlanner implements CoMTrajectoryProvider
{
   private static boolean verbose = false;
   private static final int maxCapacity = 10;
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoRegistry registry = new YoRegistry(getClass().getSimpleName());

   private final DMatrixRMaj coefficientMultipliers = new DMatrixRMaj(0, 0);
   private final DMatrixRMaj coefficientMultipliersInv = new DMatrixRMaj(0, 0);

   private final DMatrixSparseCSC coefficientMultipliersSparse = new DMatrixSparseCSC(0, 0);

   private final DMatrixRMaj xEquivalents = new DMatrixRMaj(0, 1);
   private final DMatrixRMaj yEquivalents = new DMatrixRMaj(0, 1);
   private final DMatrixRMaj zEquivalents = new DMatrixRMaj(0, 1);

   private final DMatrixSparseCSC tempSparse = new DMatrixSparseCSC(0, 1);
   private final DMatrixSparseCSC xEquivalentsSparse = new DMatrixSparseCSC(0, 1);
   private final DMatrixSparseCSC yEquivalentsSparse = new DMatrixSparseCSC(0, 1);
   private final DMatrixSparseCSC zEquivalentsSparse = new DMatrixSparseCSC(0, 1);

   private final DMatrixRMaj xConstants = new DMatrixRMaj(0, 1);
   private final DMatrixRMaj yConstants = new DMatrixRMaj(0, 1);
   private final DMatrixRMaj zConstants = new DMatrixRMaj(0, 1);

   private final DMatrixSparseCSC xConstantsSparse = new DMatrixSparseCSC(0, 1);
   private final DMatrixSparseCSC yConstantsSparse = new DMatrixSparseCSC(0, 1);
   private final DMatrixSparseCSC zConstantsSparse = new DMatrixSparseCSC(0, 1);

   private final DMatrixRMaj vrpWaypointJacobian = new DMatrixRMaj(0, 1);
   private final DMatrixSparseCSC vrpWaypointJacobianSparse = new DMatrixSparseCSC(0, 1);

   private final DMatrixRMaj vrpXWaypoints = new DMatrixRMaj(0, 1);
   private final DMatrixRMaj vrpYWaypoints = new DMatrixRMaj(0, 1);
   private final DMatrixRMaj vrpZWaypoints = new DMatrixRMaj(0, 1);

   private final DMatrixSparseCSC vrpXWaypointsSparse = new DMatrixSparseCSC(0, 1);
   private final DMatrixSparseCSC vrpYWaypointsSparse = new DMatrixSparseCSC(0, 1);
   private final DMatrixSparseCSC vrpZWaypointsSparse = new DMatrixSparseCSC(0, 1);

   // FIXME fill reducing?
   private final LinearSolverSparse<DMatrixSparseCSC, DMatrixRMaj> luSolver = LinearSolverFactory_DSCC.lu(FillReducing.NONE);
   private final LinearSolverSparse<DMatrixSparseCSC, DMatrixRMaj> cholSolver = LinearSolverFactory_DSCC.cholesky(FillReducing.NONE);
   private final LinearSolverSparse<DMatrixSparseCSC, DMatrixRMaj> qrSolver = LinearSolverFactory_DSCC.qr(FillReducing.NONE);

   final DMatrixRMaj xCoefficientVector = new DMatrixRMaj(0, 1);
   final DMatrixRMaj yCoefficientVector = new DMatrixRMaj(0, 1);
   final DMatrixRMaj zCoefficientVector = new DMatrixRMaj(0, 1);

   final DMatrixSparseCSC xCoefficientVectorSparse = new DMatrixSparseCSC(0, 1);
   final DMatrixSparseCSC yCoefficientVectorSparse = new DMatrixSparseCSC(0, 1);
   final DMatrixSparseCSC zCoefficientVectorSparse = new DMatrixSparseCSC(0, 1);

   private final DoubleProvider omega;
   private final YoDouble comHeight = new YoDouble("comHeightForPlanning", registry);
   private final double gravityZ;

   private final CoMTrajectoryPlannerIndexHandler indexHandler = new CoMTrajectoryPlannerIndexHandler();

   private final FixedFramePoint3DBasics desiredCoMPosition = new FramePoint3D(worldFrame);
   private final FixedFrameVector3DBasics desiredCoMVelocity = new FrameVector3D(worldFrame);
   private final FixedFrameVector3DBasics desiredCoMAcceleration = new FrameVector3D(worldFrame);

   private final FixedFramePoint3DBasics desiredDCMPosition = new FramePoint3D(worldFrame);
   private final FixedFrameVector3DBasics desiredDCMVelocity = new FrameVector3D(worldFrame);

   private final FixedFramePoint3DBasics desiredVRPPosition = new FramePoint3D(worldFrame);
   private final FixedFramePoint3DBasics desiredECMPPosition = new FramePoint3D(worldFrame);

   private final RecyclingArrayList<FramePoint3D> startVRPPositions = new RecyclingArrayList<>(FramePoint3D::new);
   private final RecyclingArrayList<FramePoint3D> endVRPPositions = new RecyclingArrayList<>(FramePoint3D::new);

   private final YoFramePoint3D finalDCMPosition = new YoFramePoint3D("goalDCMPosition", worldFrame, registry);

   private final YoFramePoint3D currentCoMPosition = new YoFramePoint3D("currentCoMPosition", worldFrame, registry);
   private final YoFrameVector3D currentCoMVelocity = new YoFrameVector3D("currentCoMVelocity", worldFrame, registry);

   private final YoFramePoint3D yoFirstCoefficient = new YoFramePoint3D("comFirstCoefficient", worldFrame, registry);
   private final YoFramePoint3D yoSecondCoefficient = new YoFramePoint3D("comSecondCoefficient", worldFrame, registry);
   private final YoFramePoint3D yoThirdCoefficient = new YoFramePoint3D("comThirdCoefficient", worldFrame, registry);
   private final YoFramePoint3D yoFourthCoefficient = new YoFramePoint3D("comFourthCoefficient", worldFrame, registry);
   private final YoFramePoint3D yoFifthCoefficient = new YoFramePoint3D("comFifthCoefficient", worldFrame, registry);
   private final YoFramePoint3D yoSixthCoefficient = new YoFramePoint3D("comSixthCoefficient", worldFrame, registry);

   private final RecyclingArrayList<FramePoint3D> dcmCornerPoints = new RecyclingArrayList<>(FramePoint3D::new);
   private final RecyclingArrayList<FramePoint3D> comCornerPoints = new RecyclingArrayList<>(FramePoint3D::new);

   private final RecyclingArrayList<Trajectory3D> vrpTrajectoryPool = new RecyclingArrayList<>(() -> new Trajectory3D(4));
   private final RecyclingArrayList<LineSegment3D> vrpSegments = new RecyclingArrayList<>(LineSegment3D::new);
   private final List<Trajectory3D> vrpTrajectories = new ArrayList<>();

   private int numberOfConstraints = 0;
   private boolean maintainInitialCoMVelocityContinuity = false;

   private CornerPointViewer viewer = null;

   public CoMTrajectoryPlanner(double gravityZ, double nominalCoMHeight, YoRegistry parentRegistry)
   {
      this.gravityZ = Math.abs(gravityZ);
      YoDouble omega = new YoDouble("omegaForPlanning", registry);

      comHeight.addListener(v -> omega.set(Math.sqrt(Math.abs(gravityZ) / comHeight.getDoubleValue())));
      comHeight.set(nominalCoMHeight);

      this.omega = omega;

      if (parentRegistry != null)
         parentRegistry.addChild(registry);
   }

   public CoMTrajectoryPlanner(double gravityZ, YoDouble omega, YoRegistry parentRegistry)
   {
      this.omega = omega;
      this.gravityZ = Math.abs(gravityZ);

      omega.addListener(v -> comHeight.set(gravityZ / MathTools.square(omega.getValue())));
      omega.notifyListeners();

      if (parentRegistry != null)
         parentRegistry.addChild(registry);
   }

   public void setMaintainInitialCoMVelocityContinuity(boolean maintainInitialCoMVelocityContinuity)
   {
      this.maintainInitialCoMVelocityContinuity = maintainInitialCoMVelocityContinuity;
   }

   public void setCornerPointViewer(CornerPointViewer viewer)
   {
      this.viewer = viewer;
   }

   /** {@inheritDoc} */
   @Override
   public void setNominalCoMHeight(double nominalCoMHeight)
   {
      this.comHeight.set(nominalCoMHeight);
   }

   /** {@inheritDoc} */
   @Override
   public double getNominalCoMHeight()
   {
      return comHeight.getDoubleValue();
   }

   private final List<ContactStateProvider> contactSequenceInternal = new ArrayList<>();

   /** {@inheritDoc} */
   @Override
   public void solveForTrajectory(List<? extends ContactStateProvider> contactSequence)
   {
      if (!ContactStateProviderTools.checkContactSequenceIsValid(contactSequence))
         throw new IllegalArgumentException("The contact sequence is not valid.");

      if (maintainInitialCoMVelocityContinuity)
      {
         insertKnotForContinuity(contactSequence);
         contactSequence = contactSequenceInternal;
      }

      indexHandler.update(contactSequence);

      resetMatrices();

      CoMTrajectoryPlannerTools.computeVRPWaypoints(comHeight.getDoubleValue(), gravityZ, omega.getValue(), currentCoMVelocity, contactSequence, startVRPPositions,
                                                    endVRPPositions, false);

      solveForCoefficientConstraintMatrix(contactSequence);

//      xEquivalents.set(xConstants);
//      yEquivalents.set(yConstants);
//      zEquivalents.set(zConstants);
//
//      CommonOps_DDRM.multAdd(vrpWaypointJacobian, vrpXWaypoints, xEquivalents);
//      CommonOps_DDRM.multAdd(vrpWaypointJacobian, vrpYWaypoints, yEquivalents);
//      CommonOps_DDRM.multAdd(vrpWaypointJacobian, vrpZWaypoints, zEquivalents);
//
//
//      CommonOps_DDRM.mult(coefficientMultipliersInv, xEquivalents, xCoefficientVector);
//      CommonOps_DDRM.mult(coefficientMultipliersInv, yEquivalents, yCoefficientVector);
//      CommonOps_DDRM.mult(coefficientMultipliersInv, zEquivalents, zCoefficientVector);

      // update coefficient holders
      int firstCoefficientIndex = 0;
      int secondCoefficientIndex = 1;
      int thirdCoefficientIndex = 2;
      int fourthCoefficientIndex = 3;
      int fifthCoefficientIndex = 4;
      int sixthCoefficientIndex = 5;

      yoFirstCoefficient.setX(xCoefficientVector.get(firstCoefficientIndex));
      yoFirstCoefficient.setY(yCoefficientVector.get(firstCoefficientIndex));
      yoFirstCoefficient.setZ(zCoefficientVector.get(firstCoefficientIndex));

      yoSecondCoefficient.setX(xCoefficientVector.get(secondCoefficientIndex));
      yoSecondCoefficient.setY(yCoefficientVector.get(secondCoefficientIndex));
      yoSecondCoefficient.setZ(zCoefficientVector.get(secondCoefficientIndex));

      yoThirdCoefficient.setX(xCoefficientVector.get(thirdCoefficientIndex));
      yoThirdCoefficient.setY(yCoefficientVector.get(thirdCoefficientIndex));
      yoThirdCoefficient.setZ(zCoefficientVector.get(thirdCoefficientIndex));

      yoFourthCoefficient.setX(xCoefficientVector.get(fourthCoefficientIndex));
      yoFourthCoefficient.setY(yCoefficientVector.get(fourthCoefficientIndex));
      yoFourthCoefficient.setZ(zCoefficientVector.get(fourthCoefficientIndex));

      yoFifthCoefficient.setX(xCoefficientVector.get(fifthCoefficientIndex));
      yoFifthCoefficient.setY(yCoefficientVector.get(fifthCoefficientIndex));
      yoFifthCoefficient.setZ(zCoefficientVector.get(fifthCoefficientIndex));

      yoSixthCoefficient.setX(xCoefficientVector.get(sixthCoefficientIndex));
      yoSixthCoefficient.setY(yCoefficientVector.get(sixthCoefficientIndex));
      yoSixthCoefficient.setZ(zCoefficientVector.get(sixthCoefficientIndex));

      updateCornerPoints(contactSequence);

      if (viewer != null)
      {
         viewer.updateDCMCornerPoints(dcmCornerPoints);
         viewer.updateCoMCornerPoints(comCornerPoints);
         viewer.updateVRPWaypoints(vrpSegments);
      }
   }

   private final SettableContactStateProvider startContactSequenceForContinuity = new SettableContactStateProvider();
   private final SettableContactStateProvider nextContactSequenceForContinuity = new SettableContactStateProvider();
   private static final double defaultContinuitySegmentDuration = 0.1;
   private final FramePoint3D fakeContinuityPosition = new FramePoint3D();

   private void insertKnotForContinuity(List<? extends ContactStateProvider> contactSequence)
   {
      double initialSegmentDuration = contactSequence.get(0).getTimeInterval().getDuration();
      double startTime = contactSequence.get(0).getTimeInterval().getStartTime();
      double endTime = contactSequence.get(0).getTimeInterval().getEndTime();
      FramePoint3DReadOnly startPosition = contactSequence.get(0).getCopStartPosition();
      FramePoint3DReadOnly endPosition = contactSequence.get(0).getCopEndPosition();

      double continuitySegmentDuration = Math.min(defaultContinuitySegmentDuration, 0.5 * initialSegmentDuration);
      fakeContinuityPosition.interpolate(startPosition, endPosition, continuitySegmentDuration / initialSegmentDuration);
      startContactSequenceForContinuity.setStartCopPosition(startPosition);
      startContactSequenceForContinuity.setEndCopPosition(fakeContinuityPosition);
      startContactSequenceForContinuity.getTimeInterval().setInterval(startTime, startTime + continuitySegmentDuration);

      nextContactSequenceForContinuity.setStartCopPosition(fakeContinuityPosition);
      nextContactSequenceForContinuity.setEndCopPosition(endPosition);
      nextContactSequenceForContinuity.getTimeInterval().setInterval(startTime + continuitySegmentDuration, endTime);

      contactSequenceInternal.clear();
      contactSequenceInternal.add(startContactSequenceForContinuity);
      contactSequenceInternal.add(nextContactSequenceForContinuity);
      for (int i = 1; i < contactSequence.size(); i++)
         contactSequenceInternal.add(contactSequence.get(i));
   }

   private final IGrowArray gw = new IGrowArray();
   private final DGrowArray gx = new DGrowArray();

   private void solveForCoefficientConstraintMatrix(List<? extends ContactStateProvider> contactSequence)
   {
      int numberOfPhases = contactSequence.size();
      int numberOfTransitions = numberOfPhases - 1;


      numberOfConstraints = 0;

      long startTime = System.nanoTime();


      // set initial constraint
      setCoMPositionConstraint(coefficientMultipliers, xConstants, yConstants, zConstants, currentCoMPosition);
      setDynamicsInitialConstraint(coefficientMultipliers, vrpWaypointJacobian, vrpXWaypoints, vrpYWaypoints, vrpZWaypoints,
                                   zConstants, contactSequence, 0);

      int transition = 0;
      // add a moveable waypoint for the center of mass velocity constraint
      if (maintainInitialCoMVelocityContinuity && contactSequence.get(0).getContactState().isLoadBearing())
      {
         setCoMVelocityConstraint(coefficientMultipliers, xConstants, yConstants, zConstants, currentCoMVelocity);
         setCoMPositionContinuity(coefficientMultipliers, contactSequence, 0, 1);
         setCoMVelocityContinuity(coefficientMultipliers, contactSequence, 0, 1);

         setDynamicsContinuityConstraint(coefficientMultipliers, vrpWaypointJacobian, vrpXWaypoints, vrpYWaypoints, vrpZWaypoints,
                                         contactSequence, 0, 1);
         transition++;
      }


      // add transition continuity constraints
      for (; transition < numberOfTransitions; transition++)
      {
         int previousSequence = transition;
         int nextSequence = transition + 1;
         setCoMPositionContinuity(coefficientMultipliers, contactSequence, previousSequence, nextSequence);
         setCoMVelocityContinuity(coefficientMultipliers, contactSequence, previousSequence, nextSequence);
         setDynamicsFinalConstraint(coefficientMultipliers, vrpWaypointJacobian, vrpXWaypoints, vrpYWaypoints, vrpZWaypoints, zConstants, contactSequence, previousSequence);
         setDynamicsInitialConstraint(coefficientMultipliers, vrpWaypointJacobian, vrpXWaypoints, vrpYWaypoints, vrpZWaypoints, zConstants, contactSequence, nextSequence);
      }

      // set terminal constraint
      ContactStateProvider lastContactPhase = contactSequence.get(numberOfPhases - 1);
      finalDCMPosition.set(endVRPPositions.getLast());
      double finalDuration = lastContactPhase.getTimeInterval().getDuration();
      setDCMPositionConstraint(coefficientMultipliers, xConstants, yConstants, zConstants,
                               numberOfPhases - 1, finalDuration, finalDCMPosition);
      setDynamicsFinalConstraint(coefficientMultipliers, vrpWaypointJacobian, vrpXWaypoints, vrpYWaypoints, vrpZWaypoints, zConstants, contactSequence, numberOfPhases - 1);



      NativeCommonOps.invert(coefficientMultipliers, coefficientMultipliersInv);

      xEquivalents.set(xConstants);
      yEquivalents.set(yConstants);
      zEquivalents.set(zConstants);

      CommonOps_DDRM.multAdd(vrpWaypointJacobian, vrpXWaypoints, xEquivalents);
      CommonOps_DDRM.multAdd(vrpWaypointJacobian, vrpYWaypoints, yEquivalents);
      CommonOps_DDRM.multAdd(vrpWaypointJacobian, vrpZWaypoints, zEquivalents);

      CommonOps_DDRM.mult(coefficientMultipliersInv, xEquivalents, xCoefficientVector);
      CommonOps_DDRM.mult(coefficientMultipliersInv, yEquivalents, yCoefficientVector);
      CommonOps_DDRM.mult(coefficientMultipliersInv, zEquivalents, zCoefficientVector);

      System.out.println("Full inverse time: " + (System.nanoTime() - startTime));

      startTime = System.nanoTime();


      numberOfConstraints = 0;

      // set initial constraint
      setCoMPositionConstraint(coefficientMultipliersSparse, xConstantsSparse, yConstantsSparse, zConstantsSparse, currentCoMPosition);
      setDynamicsInitialConstraint(coefficientMultipliersSparse, vrpWaypointJacobianSparse, vrpXWaypointsSparse, vrpYWaypointsSparse, vrpZWaypointsSparse,
                                   zConstantsSparse, contactSequence, 0);

      transition = 0;
      // add a moveable waypoint for the center of mass velocity constraint
      if (maintainInitialCoMVelocityContinuity && contactSequence.get(0).getContactState().isLoadBearing())
      {
         setCoMVelocityConstraint(coefficientMultipliersSparse, xConstantsSparse, yConstantsSparse, zConstantsSparse, currentCoMVelocity);
         setCoMPositionContinuity(coefficientMultipliersSparse, contactSequence, 0, 1);
         setCoMVelocityContinuity(coefficientMultipliersSparse, contactSequence, 0, 1);

         setDynamicsContinuityConstraint(coefficientMultipliersSparse, vrpWaypointJacobianSparse, vrpXWaypointsSparse, vrpYWaypointsSparse, vrpZWaypointsSparse,
                                         contactSequence, 0, 1);
         transition++;
      }


      // add transition continuity constraints
      for (; transition < numberOfTransitions; transition++)
      {
         int previousSequence = transition;
         int nextSequence = transition + 1;
         setCoMPositionContinuity(coefficientMultipliersSparse, contactSequence, previousSequence, nextSequence);
         setCoMVelocityContinuity(coefficientMultipliersSparse, contactSequence, previousSequence, nextSequence);
         setDynamicsFinalConstraint(coefficientMultipliersSparse, vrpWaypointJacobianSparse, vrpXWaypointsSparse, vrpYWaypointsSparse, vrpZWaypointsSparse, zConstantsSparse, contactSequence, previousSequence);
         setDynamicsInitialConstraint(coefficientMultipliersSparse, vrpWaypointJacobianSparse, vrpXWaypointsSparse, vrpYWaypointsSparse, vrpZWaypointsSparse, zConstantsSparse, contactSequence, nextSequence);
      }

      // set terminal constraint
      lastContactPhase = contactSequence.get(numberOfPhases - 1);
      finalDCMPosition.set(endVRPPositions.getLast());
      finalDuration = lastContactPhase.getTimeInterval().getDuration();
      setDCMPositionConstraint(coefficientMultipliersSparse, xConstantsSparse, yConstantsSparse, zConstantsSparse,
                               numberOfPhases - 1, finalDuration, finalDCMPosition);
      setDynamicsFinalConstraint(coefficientMultipliersSparse, vrpWaypointJacobianSparse, vrpXWaypointsSparse, vrpYWaypointsSparse, vrpZWaypointsSparse, zConstantsSparse,
                                 contactSequence, numberOfPhases - 1);

      luSolver.setA(coefficientMultipliersSparse);

      assertMatrixEquals(vrpWaypointJacobian, vrpWaypointJacobianSparse);
      assertMatrixEquals(vrpXWaypoints, vrpXWaypointsSparse);
      assertMatrixEquals(vrpYWaypoints, vrpYWaypointsSparse);
      assertMatrixEquals(vrpZWaypoints, vrpZWaypointsSparse);


      // TODO make an add equals method. Also don't pass in null, as that apparently makes garbage.
      CommonOps_DSCC.mult(vrpWaypointJacobianSparse, vrpXWaypointsSparse, tempSparse);
      CommonOps_DSCC.add(1.0, tempSparse, 1.0, xConstantsSparse, xEquivalentsSparse, gw, gx);

      CommonOps_DSCC.mult(vrpWaypointJacobianSparse, vrpYWaypointsSparse, tempSparse);
      CommonOps_DSCC.add(1.0, tempSparse, 1.0, yConstantsSparse, yEquivalentsSparse, gw, gx);

      CommonOps_DSCC.mult(vrpWaypointJacobianSparse, vrpZWaypointsSparse, tempSparse);
      CommonOps_DSCC.add(1.0, tempSparse, 1.0, zConstantsSparse, zEquivalentsSparse, gw, gx);

      assertMatrixEquals(xEquivalents, xEquivalentsSparse);
      assertMatrixEquals(yEquivalents, yEquivalentsSparse);
      assertMatrixEquals(zEquivalents, zEquivalentsSparse);

      luSolver.solveSparse(xEquivalentsSparse, xCoefficientVectorSparse);
      luSolver.solveSparse(yEquivalentsSparse, yCoefficientVectorSparse);
      luSolver.solveSparse(zEquivalentsSparse, zCoefficientVectorSparse);

      xCoefficientVector.set(xCoefficientVectorSparse);
      yCoefficientVector.set(yCoefficientVectorSparse);
      zCoefficientVector.set(zCoefficientVectorSparse);

      System.out.println("LU sparse inverse time: " + (System.nanoTime() - startTime));
   }

   private static void assertMatrixEquals(DMatrix matrixA, DMatrix matrixB)
   {
      if (matrixA.getNumCols() != matrixB.getNumCols() || matrixA.getNumRows() != matrixB.getNumRows())
         throw new IllegalArgumentException("Matrices aren't the same size.");

      for (int i = 0; i < matrixA.getNumRows(); i++)
      {
         for (int j = 0; j < matrixA.getNumCols(); j++)
         {
            if (!MathTools.epsilonEquals(matrixA.get(i,j), matrixB.get(i, j), 1e-5))
               throw new IllegalArgumentException("Matrices don't match in value. Expected " + matrixA.get(i, j) + ", got " + matrixB.get(i, j));
         }
      }
   }


   private final FramePoint3D comPositionToThrowAway = new FramePoint3D();
   private final FramePoint3D dcmPositionToThrowAway = new FramePoint3D();

   private final FrameVector3D comVelocityToThrowAway = new FrameVector3D();
   private final FrameVector3D comAccelerationToThrowAway = new FrameVector3D();
   private final FrameVector3D dcmVelocityToThrowAway = new FrameVector3D();
   private final FramePoint3D vrpStartPosition = new FramePoint3D();
   private final FramePoint3D vrpEndPosition = new FramePoint3D();
   private final FramePoint3D ecmpPositionToThrowAway = new FramePoint3D();

   private void updateCornerPoints(List<? extends ContactStateProvider> contactSequence)
   {
      vrpTrajectoryPool.clear();
      vrpTrajectories.clear();

      comCornerPoints.clear();
      dcmCornerPoints.clear();
      vrpSegments.clear();

      boolean verboseBefore = verbose;
      verbose = false;
      for (int segmentId = 0; segmentId < Math.min(contactSequence.size(), maxCapacity + 1); segmentId++)
      {
         double duration = contactSequence.get(segmentId).getTimeInterval().getDuration();

         compute(segmentId, 0.0, comCornerPoints.add(), comVelocityToThrowAway, comAccelerationToThrowAway, dcmCornerPoints.add(),
                 dcmVelocityToThrowAway, vrpStartPosition, ecmpPositionToThrowAway);
         compute(segmentId, duration, comPositionToThrowAway, comVelocityToThrowAway, comAccelerationToThrowAway, dcmPositionToThrowAway,
                 dcmVelocityToThrowAway, vrpEndPosition, ecmpPositionToThrowAway);

         Trajectory3D trajectory3D = vrpTrajectoryPool.add();
         trajectory3D.setLinear(0.0, duration, vrpStartPosition, vrpEndPosition);
         vrpTrajectories.add(trajectory3D);

         vrpSegments.add().set(vrpStartPosition, vrpEndPosition);
      }

      verbose = verboseBefore;
   }

   /** {@inheritDoc} */
   @Override
   public void compute(int segmentId, double timeInPhase)
   {
      compute(segmentId, timeInPhase, desiredCoMPosition, desiredCoMVelocity, desiredCoMAcceleration, desiredDCMPosition, desiredDCMVelocity,
              desiredVRPPosition, desiredECMPPosition);

      if (verbose)
      {
         LogTools.info("At time " + timeInPhase + ", Desired DCM = " + desiredDCMPosition + ", Desired CoM = " + desiredCoMPosition);
      }
   }

   private final FramePoint3D firstCoefficient = new FramePoint3D();
   private final FramePoint3D secondCoefficient = new FramePoint3D();
   private final FramePoint3D thirdCoefficient = new FramePoint3D();
   private final FramePoint3D fourthCoefficient = new FramePoint3D();
   private final FramePoint3D fifthCoefficient = new FramePoint3D();
   private final FramePoint3D sixthCoefficient = new FramePoint3D();

   @Override
   public void compute(int segmentId, double timeInPhase, FixedFramePoint3DBasics comPositionToPack, FixedFrameVector3DBasics comVelocityToPack,
                       FixedFrameVector3DBasics comAccelerationToPack, FixedFramePoint3DBasics dcmPositionToPack, FixedFrameVector3DBasics dcmVelocityToPack,
                       FixedFramePoint3DBasics vrpPositionToPack, FixedFramePoint3DBasics ecmpPositionToPack)
   {
      if (segmentId < 0)
         throw new IllegalArgumentException("time is invalid.");

      int startIndex = indexHandler.getContactSequenceStartIndex(segmentId);
      firstCoefficient.setX(xCoefficientVector.get(startIndex));
      firstCoefficient.setY(yCoefficientVector.get(startIndex));
      firstCoefficient.setZ(zCoefficientVector.get(startIndex));

      int secondCoefficientIndex = startIndex + 1;
      secondCoefficient.setX(xCoefficientVector.get(secondCoefficientIndex));
      secondCoefficient.setY(yCoefficientVector.get(secondCoefficientIndex));
      secondCoefficient.setZ(zCoefficientVector.get(secondCoefficientIndex));

      int thirdCoefficientIndex = startIndex + 2;
      thirdCoefficient.setX(xCoefficientVector.get(thirdCoefficientIndex));
      thirdCoefficient.setY(yCoefficientVector.get(thirdCoefficientIndex));
      thirdCoefficient.setZ(zCoefficientVector.get(thirdCoefficientIndex));

      int fourthCoefficientIndex = startIndex + 3;
      fourthCoefficient.setX(xCoefficientVector.get(fourthCoefficientIndex));
      fourthCoefficient.setY(yCoefficientVector.get(fourthCoefficientIndex));
      fourthCoefficient.setZ(zCoefficientVector.get(fourthCoefficientIndex));

      int fifthCoefficientIndex = startIndex + 4;
      fifthCoefficient.setX(xCoefficientVector.get(fifthCoefficientIndex));
      fifthCoefficient.setY(yCoefficientVector.get(fifthCoefficientIndex));
      fifthCoefficient.setZ(zCoefficientVector.get(fifthCoefficientIndex));

      int sixthCoefficientIndex = startIndex + 5;
      sixthCoefficient.setX(xCoefficientVector.get(sixthCoefficientIndex));
      sixthCoefficient.setY(yCoefficientVector.get(sixthCoefficientIndex));
      sixthCoefficient.setZ(zCoefficientVector.get(sixthCoefficientIndex));

      double omega = this.omega.getValue();

      CoMTrajectoryPlannerTools.constructDesiredCoMPosition(comPositionToPack, firstCoefficient, secondCoefficient, thirdCoefficient, fourthCoefficient, fifthCoefficient,
                                  sixthCoefficient, timeInPhase, omega);
      CoMTrajectoryPlannerTools.constructDesiredCoMVelocity(comVelocityToPack, firstCoefficient, secondCoefficient, thirdCoefficient, fourthCoefficient, fifthCoefficient,
                                  sixthCoefficient, timeInPhase, omega);
      CoMTrajectoryPlannerTools.constructDesiredCoMAcceleration(comAccelerationToPack, firstCoefficient, secondCoefficient, thirdCoefficient, fourthCoefficient, fifthCoefficient,
                                      sixthCoefficient, timeInPhase, omega);

      CapturePointTools.computeCapturePointPosition(comPositionToPack, comVelocityToPack, omega, dcmPositionToPack);
      CapturePointTools.computeCapturePointVelocity(comVelocityToPack, comAccelerationToPack, omega, dcmVelocityToPack);
      CapturePointTools.computeCentroidalMomentumPivot(dcmPositionToPack, dcmVelocityToPack, omega, vrpPositionToPack);

      ecmpPositionToPack.set(vrpPositionToPack);
      ecmpPositionToPack.subZ(comHeight.getDoubleValue());
   }

   /** {@inheritDoc} */
   @Override
   public void setInitialCenterOfMassState(FramePoint3DReadOnly centerOfMassPosition, FrameVector3DReadOnly centerOfMassVelocity)
   {
      this.currentCoMPosition.setMatchingFrame(centerOfMassPosition);
      this.currentCoMVelocity.setMatchingFrame(centerOfMassVelocity);
   }

   /** {@inheritDoc} */
   @Override
   public FramePoint3DReadOnly getDesiredDCMPosition()
   {
      return desiredDCMPosition;
   }

   /** {@inheritDoc} */
   @Override
   public FrameVector3DReadOnly getDesiredDCMVelocity()
   {
      return desiredDCMVelocity;
   }

   /** {@inheritDoc} */
   @Override
   public FramePoint3DReadOnly getDesiredCoMPosition()
   {
      return desiredCoMPosition;
   }

   /** {@inheritDoc} */
   @Override
   public FrameVector3DReadOnly getDesiredCoMVelocity()
   {
      return desiredCoMVelocity;
   }

   /** {@inheritDoc} */
   @Override
   public FrameVector3DReadOnly getDesiredCoMAcceleration()
   {
      return desiredCoMAcceleration;
   }

   /** {@inheritDoc} */
   @Override
   public FramePoint3DReadOnly getDesiredVRPPosition()
   {
      return desiredVRPPosition;
   }

   /** {@inheritDoc} */
   @Override
   public FramePoint3DReadOnly getDesiredECMPPosition()
   {
      return desiredECMPPosition;
   }

   /**
    * Resets and resizes the internal matrices.
    */
   private void resetMatrices()
   {
      int size = indexHandler.getTotalNumberOfCoefficients();
      int numberOfVRPWaypoints = indexHandler.getNumberOfVRPWaypoints();

      coefficientMultipliers.reshape(size, size);
      coefficientMultipliersInv.reshape(size, size);
      xEquivalents.reshape(size, 1);
      yEquivalents.reshape(size, 1);
      zEquivalents.reshape(size, 1);
      xConstants.reshape(size, 1);
      yConstants.reshape(size, 1);
      zConstants.reshape(size, 1);
      vrpWaypointJacobian.reshape(size, numberOfVRPWaypoints); // only position
      vrpXWaypoints.reshape(numberOfVRPWaypoints, 1);
      vrpYWaypoints.reshape(numberOfVRPWaypoints, 1);
      vrpZWaypoints.reshape(numberOfVRPWaypoints, 1);
      xCoefficientVector.reshape(size, 1);
      yCoefficientVector.reshape(size, 1);
      zCoefficientVector.reshape(size, 1);

      coefficientMultipliers.zero();
      coefficientMultipliersInv.zero();
      xEquivalents.zero();
      yEquivalents.zero();
      zEquivalents.zero();
      xConstants.zero();
      yConstants.zero();
      zConstants.zero();
      vrpWaypointJacobian.zero();
      vrpXWaypoints.zero();
      vrpYWaypoints.zero();
      vrpZWaypoints.zero();
      xCoefficientVector.zero();
      yCoefficientVector.zero();
      zCoefficientVector.zero();


      coefficientMultipliersSparse.reshape(size, size);
      tempSparse.reshape(size, 1);
      xEquivalentsSparse.reshape(size, 1);
      yEquivalentsSparse.reshape(size, 1);
      zEquivalentsSparse.reshape(size, 1);
      xConstantsSparse.reshape(size, 1);
      yConstantsSparse.reshape(size, 1);
      zConstantsSparse.reshape(size, 1);
      vrpWaypointJacobianSparse.reshape(size, numberOfVRPWaypoints); // only position
      vrpXWaypointsSparse.reshape(numberOfVRPWaypoints, 1);
      vrpYWaypointsSparse.reshape(numberOfVRPWaypoints, 1);
      vrpZWaypointsSparse.reshape(numberOfVRPWaypoints, 1);
      xCoefficientVectorSparse.reshape(size, 1);
      yCoefficientVectorSparse.reshape(size, 1);
      zCoefficientVectorSparse.reshape(size, 1);

      coefficientMultipliersSparse.zero();
      xEquivalentsSparse.zero();
      yEquivalentsSparse.zero();
      zEquivalentsSparse.zero();
      xConstantsSparse.zero();
      yConstantsSparse.zero();
      zConstantsSparse.zero();
      vrpWaypointJacobianSparse.zero();
      vrpXWaypointsSparse.zero();
      vrpYWaypointsSparse.zero();
      vrpZWaypointsSparse.zero();
   }

   /**
    * <p> Sets the continuity constraint on the initial CoM position. This DOES result in a initial discontinuity on the desired DCM location,
    * coming from a discontinuity on the desired CoM Velocity. </p>
    * <p> This constraint should be used for the initial position of the center of mass to properly initialize the trajectory. </p>
    * <p> Recall that the equation for the center of mass is defined by </p>
    * <p>
    *    x<sub>i</sub>(t<sub>i</sub>) = c<sub>0,i</sub> e<sup>&omega; t<sub>i</sub></sup> + c<sub>1,i</sub> e<sup>-&omega; t<sub>i</sub></sup> +
    *    c<sub>2,i</sub> t<sub>i</sub><sup>3</sup> + c<sub>3,i</sub> t<sub>i</sub><sup>2</sup> +
    *    c<sub>4,i</sub> t<sub>i</sub> + c<sub>5,i</sub>.
    * </p>
    * <p>
    *    This constraint defines
    * </p>
    * <p>
    *    x<sub>0</sub>(0) = x<sub>d</sub>,
    * </p>
    * <p>
    *    substituting in the coefficients into the constraint matrix.
    * </p>
    * @param centerOfMassLocationForConstraint x<sub>d</sub> in the above equations
    */
   private void setCoMPositionConstraint(DMatrix matrixToPack,
                                         DMatrix xConstantsToPack,
                                         DMatrix yConstantsToPack,
                                         DMatrix zConstantsToPack,
                                         FramePoint3DReadOnly centerOfMassLocationForConstraint)
   {
      CoMTrajectoryPlannerTools.addCoMPositionConstraint(centerOfMassLocationForConstraint, omega.getValue(), 0.0, 0, numberOfConstraints,
                                                         matrixToPack, xConstantsToPack, yConstantsToPack, zConstantsToPack);
      numberOfConstraints++;
   }

   /**
    * <p> Sets the continuity constraint on the initial CoM velocity.
    * <p> This constraint should be used for the initial velocity of the center of mass to properly initialize the trajectory. </p>
    * <p> Recall that the equation for the center of mass is defined by </p>
    * <p>
    *    x<sub>i</sub>(t<sub>i</sub>) = c<sub>0,i</sub> e<sup>&omega; t<sub>i</sub></sup> + c<sub>1,i</sub> e<sup>-&omega; t<sub>i</sub></sup> +
    *    c<sub>2,i</sub> t<sub>i</sub><sup>3</sup> + c<sub>3,i</sub> t<sub>i</sub><sup>2</sup> +
    *    c<sub>4,i</sub> t<sub>i</sub> + c<sub>5,i</sub>.
    * </p>
    * <p>
    *    This constraint defines
    * </p>
    * <p>
    *    x<sub>0</sub>(0) = x<sub>d</sub>,
    * </p>
    * <p>
    *    substituting in the coefficients into the constraint matrix.
    * </p>
    * @param centerOfMassVelocityForConstraint x<sub>d</sub> in the above equations
    */
   private void setCoMVelocityConstraint(DMatrix matrixToPack,
                                         DMatrix xConstantsToPack,
                                         DMatrix yConstantsToPack,
                                         DMatrix zConstantsToPack,
                                         FrameVector3DReadOnly centerOfMassVelocityForConstraint)
   {
      CoMTrajectoryPlannerTools.addCoMVelocityConstraint(centerOfMassVelocityForConstraint, omega.getValue(), 0.0, 0, numberOfConstraints,
                                                         matrixToPack, xConstantsToPack, yConstantsToPack, zConstantsToPack);
      numberOfConstraints++;
   }

   /**
    * <p> Sets a constraint on the desired DCM position. This constraint is useful for constraining the terminal location of the DCM trajectory. </p>
    * <p> Recall that the equation for the center of mass position is defined by </p>
    * <p>
    *    x<sub>i</sub>(t<sub>i</sub>) = c<sub>0,i</sub> e<sup>&omega; t<sub>i</sub></sup> + c<sub>1,i</sub> e<sup>-&omega; t<sub>i</sub></sup> +
    *    c<sub>2,i</sub> t<sub>i</sub><sup>3</sup> + c<sub>3,i</sub> t<sub>i</sub><sup>2</sup> +
    *    c<sub>4,i</sub> t<sub>i</sub> + c<sub>5,i</sub>.
    * </p>
    * <p> and the center of mass velocity is defined by </p>
    * <p>
    *    d/dt x<sub>i</sub>(t<sub>i</sub>) = &omega; c<sub>0,i</sub> e<sup>&omega; t<sub>i</sub></sup> -
    *    &omega; c<sub>1,i</sub> e<sup>-&omega; t<sub>i</sub></sup> + 3 c<sub>2,i</sub> t<sub>i</sub><sup>2</sup> +
    *     2 c<sub>3,i</sub> t<sub>i</sub> + c<sub>4,i</sub>
    * </p>
    * <p>
    *    This constraint is then combining these two, saying
    * </p>
    * <p> x<sub>i</sub>(t<sub>i</sub>) + 1 / &omega; d/dt x<sub>i</sub>(t<sub>i</sub>) = &xi;<sub>d</sub>,</p>
    * <p> substituting in the appropriate coefficients. </p>
    * @param sequenceId i in the above equations
    * @param time t<sub>i</sub> in the above equations
    * @param desiredDCMPosition desired DCM location. &xi;<sub>d</sub> in the above equations.
    */
   private void setDCMPositionConstraint(DMatrix matrixToPack,
                                         DMatrix xConstantsToPack,
                                         DMatrix yConstantsToPack,
                                         DMatrix zConstantsToPack,
                                         int sequenceId,
                                         double time,
                                         FramePoint3DReadOnly desiredDCMPosition)
   {
      CoMTrajectoryPlannerTools.addDCMPositionConstraint(sequenceId, numberOfConstraints, time, omega.getValue(), desiredDCMPosition, matrixToPack,
                                                         xConstantsToPack, yConstantsToPack, zConstantsToPack);
      numberOfConstraints++;
   }

   /**
    * <p> Set a continuity constraint on the CoM position at a state change, aka a trajectory knot.. </p>
    * <p> Recall that the equation for the center of mass position is defined by </p>
    * <p>
    *    x<sub>i</sub>(t<sub>i</sub>) = c<sub>0,i</sub> e<sup>&omega; t<sub>i</sub></sup> + c<sub>1,i</sub> e<sup>-&omega; t<sub>i</sub></sup> +
    *    c<sub>2,i</sub> t<sub>i</sub><sup>3</sup> + c<sub>3,i</sub> t<sub>i</sub><sup>2</sup> +
    *    c<sub>4,i</sub> t<sub>i</sub> + c<sub>5,i</sub>.
    * </p>
    * <p> This constraint is then defined as </p>
    * <p> x<sub>i-1</sub>(T<sub>i-1</sub>) = x<sub>i</sub>(0), </p>
    * <p> substituting in the trajectory coefficients. </p>
    *
    * @param contactSequence current contact sequence.
    * @param previousSequence i-1 in the above equations.
    * @param nextSequence i in the above equations.
    */
   private void setCoMPositionContinuity(DMatrix matrixToPack,
                                         List<? extends ContactStateProvider> contactSequence,
                                         int previousSequence,
                                         int nextSequence)
   {
      double previousDuration = contactSequence.get(previousSequence).getTimeInterval().getDuration();
      CoMTrajectoryPlannerTools.addCoMPositionContinuityConstraint(previousSequence, nextSequence, numberOfConstraints, omega.getValue(), previousDuration,
                                                                   matrixToPack);
      numberOfConstraints++;
   }

   /**
    * <p> Set a continuity constraint on the CoM velocity at a state change, aka a trajectory knot.. </p>
    * <p> Recall that the equation for the center of mass position is defined by </p>
    * <p>
    *    d/dt x<sub>i</sub>(t<sub>i</sub>) = &omega; c<sub>0,i</sub> e<sup>&omega; t<sub>i</sub></sup> -
    *    &omega; c<sub>1,i</sub> e<sup>-&omega; t<sub>i</sub></sup> + 3 c<sub>2,i</sub> t<sub>i</sub><sup>2</sup> +
    *     2 c<sub>3,i</sub> t<sub>i</sub> + c<sub>4,i</sub>.
    * </p>
    * <p> This constraint is then defined as </p>
    * <p> d / dt x<sub>i-1</sub>(T<sub>i-1</sub>) = d / dt x<sub>i</sub>(0), </p>
    * <p> substituting in the trajectory coefficients. </p>
    *
    * @param contactSequence current contact sequence.
    * @param previousSequence i-1 in the above equations.
    * @param nextSequence i in the above equations.
    */
   private void setCoMVelocityContinuity(DMatrix matrixToPack,
                                         List<? extends ContactStateProvider> contactSequence,
                                         int previousSequence,
                                         int nextSequence)
   {
      double previousDuration = contactSequence.get(previousSequence).getTimeInterval().getDuration();
      CoMTrajectoryPlannerTools.addCoMVelocityContinuityConstraint(previousSequence, nextSequence, numberOfConstraints, omega.getValue(), previousDuration,
                                                                   matrixToPack);
      numberOfConstraints++;
   }

   private final FrameVector3D desiredVelocity = new FrameVector3D();

   /**
    * Used to enforce the dynamics at the beginning of the trajectory segment {@param sequenceId}.
    *
    * @param contactSequence current contact sequence.
    * @param sequenceId desired trajectory segment.
    */
   private void setDynamicsInitialConstraint(DMatrix matrixToPack,
                                             DMatrix vrpWaypointJacobianToPack,
                                             DMatrix vrpXWaypointsToPack,
                                             DMatrix vrpYWaypointsToPack,
                                             DMatrix vrpZWaypointsToPack,
                                             DMatrix zConstantsToPack,
                                             List<? extends ContactStateProvider> contactSequence, int sequenceId)
   {
      ContactStateProvider contactStateProvider = contactSequence.get(sequenceId);
      ContactState contactState = contactStateProvider.getContactState();
      if (contactState.isLoadBearing())
      {
         double duration = contactStateProvider.getTimeInterval().getDuration();
         desiredVelocity.sub(endVRPPositions.get(sequenceId), startVRPPositions.get(sequenceId));
         desiredVelocity.scale(1.0 / duration);
         constrainVRPPosition(matrixToPack, vrpWaypointJacobianToPack, vrpXWaypointsToPack, vrpYWaypointsToPack, vrpZWaypointsToPack,
                              sequenceId, indexHandler.getVRPWaypointStartPositionIndex(sequenceId), 0.0, startVRPPositions.get(sequenceId));
         constrainVRPVelocity(matrixToPack, vrpWaypointJacobianToPack, vrpXWaypointsToPack, vrpYWaypointsToPack, vrpZWaypointsToPack,
                              sequenceId, indexHandler.getVRPWaypointStartVelocityIndex(sequenceId), 0.0, desiredVelocity);
      }
      else
      {
         constrainCoMAccelerationToGravity(matrixToPack, zConstantsToPack, sequenceId, 0.0);
         constrainCoMJerkToZero(matrixToPack, sequenceId, 0.0);
      }
   }

   /**
    * Used to enforce the dynamics at the end of the trajectory segment {@param sequenceId}.
    *
    * @param contactSequence current contact sequence.
    * @param sequenceId desired trajectory segment.
    */
   private void setDynamicsFinalConstraint(DMatrix matrixToPack,
                                           DMatrix vrpWaypointJacobianToPack,
                                           DMatrix vrpXWaypointsToPack,
                                           DMatrix vrpYWaypointsToPack,
                                           DMatrix vrpZWaypointsToPack,
                                           DMatrix zConstantsToPack,
                                           List<? extends ContactStateProvider> contactSequence,
                                           int sequenceId)
   {
      ContactStateProvider contactStateProvider = contactSequence.get(sequenceId);
      ContactState contactState = contactStateProvider.getContactState();
      double duration = contactStateProvider.getTimeInterval().getDuration();
      if (contactState.isLoadBearing())
      {
         desiredVelocity.sub(endVRPPositions.get(sequenceId), startVRPPositions.get(sequenceId));
         desiredVelocity.scale(1.0 / duration);
         constrainVRPPosition(matrixToPack, vrpWaypointJacobianToPack, vrpXWaypointsToPack, vrpYWaypointsToPack, vrpZWaypointsToPack,
                              sequenceId, indexHandler.getVRPWaypointFinalPositionIndex(sequenceId), duration, endVRPPositions.get(sequenceId));
         constrainVRPVelocity(matrixToPack, vrpWaypointJacobianToPack, vrpXWaypointsToPack, vrpYWaypointsToPack, vrpZWaypointsToPack,
                              sequenceId, indexHandler.getVRPWaypointFinalVelocityIndex(sequenceId), duration, desiredVelocity);
      }
      else
      {
         constrainCoMAccelerationToGravity(matrixToPack, zConstantsToPack, sequenceId, duration);
         constrainCoMJerkToZero(matrixToPack, sequenceId, duration);
      }
   }

   private void setDynamicsContinuityConstraint(DMatrix matrixToPack,
                                                DMatrix vrpWaypointJacobianToPack,
                                                DMatrix vrpXWaypointsToPack,
                                                DMatrix vrpYWaypointsToPack,
                                                DMatrix vrpZWaypointsToPack,
                                                List<? extends ContactStateProvider> contactSequence,
                                                int previousSequenceId,
                                                int nextSequenceId)
   {
      ContactStateProvider previousSequence = contactSequence.get(previousSequenceId);
      ContactStateProvider nextSequence = contactSequence.get(nextSequenceId);
      if (previousSequence.getContactState().isLoadBearing() != nextSequence.getContactState().isLoadBearing())
         throw new IllegalArgumentException("Cannot constrain two sequences of different types to have equivalent dynamics.");

      setVRPPositionContinuity(matrixToPack, contactSequence, previousSequenceId, nextSequenceId);

      double previousDuration = previousSequence.getTimeInterval().getDuration();
      double nextDuration = nextSequence.getTimeInterval().getDuration();
      addImplicitVRPVelocityConstraint(matrixToPack, vrpWaypointJacobianToPack, vrpXWaypointsToPack, vrpYWaypointsToPack, vrpZWaypointsToPack,
                                       previousSequenceId, indexHandler.getVRPWaypointStartPositionIndex(previousSequenceId), previousDuration, 0.0, startVRPPositions.get(previousSequenceId));
      addImplicitVRPVelocityConstraint(matrixToPack, vrpWaypointJacobianToPack, vrpXWaypointsToPack, vrpYWaypointsToPack, vrpZWaypointsToPack,
                                       nextSequenceId, indexHandler.getVRPWaypointFinalPositionIndex(nextSequenceId), 0.0, nextDuration, endVRPPositions.get(nextSequenceId));
   }

   /**
    * <p> Adds a constraint for the desired VRP position.</p>
    * <p> Recall that the VRP is defined as </p>
    * <p> v<sub>i</sub>(t<sub>i</sub>) =  c<sub>2,i</sub> t<sub>i</sub><sup>3</sup> + c<sub>3,i</sub> t<sub>i</sub><sup>2</sup> +
    * (c<sub>4,i</sub> - 6/&omega;<sup>2</sup> c<sub>2,i</sub>) t<sub>i</sub> - 2/&omega; c<sub>3,i</sub> + c<sub>5,i</sub></p>.
    * <p> This constraint then says </p>
    * <p> v<sub>i</sub>(t<sub>i</sub>) = J v<sub>d</sub> </p>
    * <p> where J is a Jacobian that maps from a vector of desired VRP waypoints to the constraint form, and </p>
    * <p> v<sub>d,j</sub> = v<sub>r</sub> </p>
    * @param sequenceId segment of interest, i in the above equations
    * @param vrpWaypointPositionIndex current vrp waypoint index, j in the above equations
    * @param time time in the segment, t<sub>i</sub> in the above equations
    * @param desiredVRPPosition reference VRP position, v<sub>r</sub> in the above equations.
    */
   private void constrainVRPPosition(DMatrix matrixToPack,
                                     DMatrix vrpWaypointJacobianToPack,
                                     DMatrix vrpXWaypointsToPack,
                                     DMatrix vrpYWaypointsToPack,
                                     DMatrix vrpZWaypointsToPack,
                                     int sequenceId,
                                     int vrpWaypointPositionIndex,
                                     double time,
                                     FramePoint3DReadOnly desiredVRPPosition)
   {
      CoMTrajectoryPlannerTools.addVRPPositionConstraint(sequenceId, numberOfConstraints, vrpWaypointPositionIndex, time, omega.getValue(), desiredVRPPosition,
                                                         matrixToPack, vrpXWaypointsToPack, vrpYWaypointsToPack, vrpZWaypointsToPack, vrpWaypointJacobianToPack);
      numberOfConstraints++;
   }

   /**
    * <p> Adds a constraint for the desired VRP velocity.</p>
    * <p> Recall that the VRP velocity is defined as </p>
    * <p> d/dt v<sub>i</sub>(t<sub>i</sub>) =  3 c<sub>2,i</sub> t<sub>i</sub><sup>2</sup> + 2 c<sub>3,i</sub> t<sub>i</sub> +
    * (c<sub>4,i</sub> - 6/&omega;<sup>2</sup> c<sub>2,i</sub>).
    * <p> This constraint then says </p>
    * <p> d/dt v<sub>i</sub>(t<sub>i</sub>) = J v<sub>d</sub> </p>
    * <p> where J is a Jacobian that maps from a vector of desired VRP waypoints to the constraint form, and </p>
    * <p> v<sub>d,j</sub> = d/dt v<sub>r</sub> </p>
    * @param sequenceId segment of interest, i in the above equations
    * @param vrpWaypointVelocityIndex current vrp waypoint index, j in the above equations
    * @param time time in the segment, t<sub>i</sub> in the above equations
    * @param desiredVRPVelocity reference VRP veloctiy, d/dt v<sub>r</sub> in the above equations.
    */
   private void constrainVRPVelocity(DMatrix matrixToPack,
                                     DMatrix vrpWaypointJacobianToPack,
                                     DMatrix vrpXWaypointsToPack,
                                     DMatrix vrpYWaypointsToPack,
                                     DMatrix vrpZWaypointsToPack,
                                     int sequenceId,
                                     int vrpWaypointVelocityIndex,
                                     double time,
                                     FrameVector3DReadOnly desiredVRPVelocity)
   {
      CoMTrajectoryPlannerTools.addVRPVelocityConstraint(sequenceId, numberOfConstraints, vrpWaypointVelocityIndex, omega.getValue(), time, desiredVRPVelocity,
                                                         matrixToPack, vrpXWaypointsToPack, vrpYWaypointsToPack, vrpZWaypointsToPack, vrpWaypointJacobianToPack);
      numberOfConstraints++;
   }

   /**
    * <p> Adds a constraint for the CoM trajectory to have an acceleration equal to gravity at time t.</p>
    * <p> Recall that the CoM acceleration is defined as </p>
    * d<sup>2</sup> / dt<sup>2</sup> x<sub>i</sub>(t<sub>i</sub>) = &omega;<sup>2</sup> c<sub>0,i</sub> e<sup>&omega; t<sub>i</sub></sup> +
    * &omega;<sup>2</sup> c<sub>1,i</sub> e<sup>-&omega; t<sub>i</sub></sup> + 6 c<sub>2,i</sub> t<sub>i</sub> + 2 c<sub>3,i</sub>
    * <p> This constraint then states that </p>
    * <p> d<sup>2</sup> / dt<sup>2</sup> x<sub>i</sub>(t<sub>i</sub>) = -g, </p>
    * <p> substituting in the appropriate coefficients. </p>
    * @param sequenceId segment of interest, i in the above equations.
    * @param time time for the constraint, t<sub>i</sub> in the above equations.
    */
   private void constrainCoMAccelerationToGravity(DMatrix matrixToPack, DMatrix zConstantsToPack, int sequenceId, double time)
   {
      CoMTrajectoryPlannerTools.constrainCoMAccelerationToGravity(sequenceId, numberOfConstraints, omega.getValue(), time, gravityZ, matrixToPack,
                                                                  zConstantsToPack);
      numberOfConstraints++;
   }

   /**
    * <p> Adds a constraint for the CoM trajectory to have a jerk equal to 0.0 at time t.</p>
    * <p> Recall that the CoM jerk is defined as </p>
    * d<sup>3</sup> / dt<sup>3</sup> x<sub>i</sub>(t<sub>i</sub>) = &omega;<sup>3</sup> c<sub>0,i</sub> e<sup>&omega; t<sub>i</sub></sup> -
    * &omega;<sup>3</sup> c<sub>1,i</sub> e<sup>-&omega; t<sub>i</sub></sup> + 6 c<sub>2,i</sub>
    * <p> This constraint then states that </p>
    * <p> d<sup>3</sup> / dt<sup>3</sup> x<sub>i</sub>(t<sub>i</sub>) = 0.0, </p>
    * <p> substituting in the appropriate coefficients. </p>
    * @param sequenceId segment of interest, i in the above equations.
    * @param time time for the constraint, t<sub>i</sub> in the above equations.
    */
   private void constrainCoMJerkToZero(DMatrix matrixToPack, int sequenceId, double time)
   {
      CoMTrajectoryPlannerTools.constrainCoMJerkToZero(time, omega.getValue(), sequenceId, numberOfConstraints, matrixToPack);
      numberOfConstraints++;
   }

   private void setVRPPositionContinuity(DMatrix matrixToPack, List<? extends ContactStateProvider> contactSequence, int previousSequence, int nextSequence)
   {
      double previousDuration = contactSequence.get(previousSequence).getTimeInterval().getDuration();
      CoMTrajectoryPlannerTools.addVRPPositionContinuityConstraint(previousSequence, nextSequence, numberOfConstraints, omega.getValue(), previousDuration,
                                                                   matrixToPack);
      numberOfConstraints++;
   }

   private void addImplicitVRPVelocityConstraint(DMatrix matrixToPack,
                                                 DMatrix vrpWaypointJacobianToPack,
                                                 DMatrix vrpXWaypointsToPack,
                                                 DMatrix vrpYWaypointsToPack,
                                                 DMatrix vrpZWaypointsToPack,
                                                 int sequenceId,
                                                 int vrpWaypointPositionIndex,
                                                 double time,
                                                 double timeAtWaypoint,
                                                 FramePoint3DReadOnly desiredVRPPosition)
   {
      CoMTrajectoryPlannerTools.addImplicitVRPVelocityConstraint(sequenceId, numberOfConstraints, vrpWaypointPositionIndex, time, timeAtWaypoint, omega.getValue(),
                                                                 desiredVRPPosition, matrixToPack, vrpXWaypointsToPack, vrpYWaypointsToPack, vrpZWaypointsToPack, vrpWaypointJacobianToPack);
      numberOfConstraints++;
   }

   @Override
   public List<Trajectory3D> getVRPTrajectories()
   {
      return vrpTrajectories;
   }
}
