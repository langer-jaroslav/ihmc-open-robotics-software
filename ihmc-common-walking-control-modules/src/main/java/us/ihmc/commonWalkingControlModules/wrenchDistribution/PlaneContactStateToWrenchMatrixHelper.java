package us.ihmc.commonWalkingControlModules.wrenchDistribution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoContactPoint;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoPlaneContactState;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.CenterOfPressureCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.PlaneContactStateCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.WrenchObjectiveCommand;
import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.euclid.axisAngle.AxisAngle;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.robotics.linearAlgebra.MatrixTools;
import us.ihmc.robotics.math.frames.YoMatrix;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.SelectionMatrix6D;
import us.ihmc.robotics.screwTheory.SpatialForceVector;
import us.ihmc.robotics.screwTheory.Wrench;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoFramePoint2D;
import us.ihmc.yoVariables.variable.YoFramePoint3D;

public class PlaneContactStateToWrenchMatrixHelper
{
   /**
    * This is used when determining whether two contact points are at the same location. If that is the case
    * one of them will rotate it's friction cone approximation to get better coverage of the cone through the
    * basis vectors.
    */
   private static final double distanceThresholdBetweenTwoContactPoint = 0.01;

   private final int maxNumberOfContactPoints;
   private final int numberOfBasisVectorsPerContactPoint;
   private final double basisVectorAngleIncrement;

   private final int rhoSize;

   private final DenseMatrix64F rhoMatrix;
   private final DenseMatrix64F rhoJacobianMatrix;
   private final DenseMatrix64F copJacobianMatrix;
   private final DenseMatrix64F wrenchJacobianMatrix;

   private final DenseMatrix64F desiredCoPMatrix = new DenseMatrix64F(2, 1);
   private final DenseMatrix64F previousCoPMatrix = new DenseMatrix64F(2, 1);

   private final DenseMatrix64F rhoMaxMatrix;
   private final DenseMatrix64F rhoWeightMatrix;
   private final DenseMatrix64F rhoRateWeightMatrix;
   private final DenseMatrix64F desiredCoPWeightMatrix = new DenseMatrix64F(2, 2);
   private final DenseMatrix64F copRateWeightMatrix = new DenseMatrix64F(2, 2);

   private final DenseMatrix64F activeRhoMatrix;

   private final YoPlaneContactState yoPlaneContactState;

   private final YoBoolean hasReset;
   private final YoBoolean resetRequested;

   private final YoMatrix yoRho;

   private final FrameVector3D contactNormalVector = new FrameVector3D();
   private final AxisAngle normalContactVectorRotation = new AxisAngle();

   private final ReferenceFrame centerOfMassFrame;
   private final PoseReferenceFrame planeFrame;

   private final YoFramePoint3D desiredCoP;
   private final YoFramePoint3D previousCoP;

   private final YoBoolean hasReceivedCenterOfPressureCommand;
   private final YoBoolean isFootholdAreaLargeEnough;
   private final YoBoolean deactivateRhoWhenNotInContact;
   private final YoFramePoint2D desiredCoPCommandInSoleFrame;
   private final Vector2D desiredCoPCommandWeightInSoleFrame = new Vector2D();

   private final List<FramePoint3D> basisVectorsOrigin = new ArrayList<>();
   private final List<FrameVector3D> basisVectors = new ArrayList<>();
   private final HashMap<YoContactPoint, YoDouble> maxContactForces = new HashMap<>();
   private final HashMap<YoContactPoint, YoDouble> rhoWeights = new HashMap<>();

   private final RotationMatrix normalContactVectorRotationMatrix = new RotationMatrix();

   private final FramePoint2D contactPoint2d = new FramePoint2D();
   private final FrictionConeRotationCalculator coneRotationCalculator;

   public PlaneContactStateToWrenchMatrixHelper(ContactablePlaneBody contactablePlaneBody, ReferenceFrame centerOfMassFrame, int maxNumberOfContactPoints,
                                                int numberOfBasisVectorsPerContactPoint, FrictionConeRotationCalculator coneRotationCalculator,
                                                YoVariableRegistry parentRegistry)
   {
      List<FramePoint2D> contactPoints2d = contactablePlaneBody.getContactPoints2d();

      if (contactPoints2d.size() > maxNumberOfContactPoints)
         throw new RuntimeException("Unexpected number of contact points: " + contactPoints2d.size());

      this.centerOfMassFrame = centerOfMassFrame;
      this.maxNumberOfContactPoints = maxNumberOfContactPoints;
      this.numberOfBasisVectorsPerContactPoint = numberOfBasisVectorsPerContactPoint;
      this.coneRotationCalculator = coneRotationCalculator;

      rhoSize = maxNumberOfContactPoints * numberOfBasisVectorsPerContactPoint;
      basisVectorAngleIncrement = 2.0 * Math.PI / numberOfBasisVectorsPerContactPoint;

      rhoMatrix = new DenseMatrix64F(rhoSize, 1);
      rhoJacobianMatrix = new DenseMatrix64F(SpatialForceVector.SIZE, rhoSize);
      copJacobianMatrix = new DenseMatrix64F(2, rhoSize);
      wrenchJacobianMatrix = new DenseMatrix64F(Wrench.SIZE, rhoSize);

      rhoMaxMatrix = new DenseMatrix64F(rhoSize, 1);
      rhoWeightMatrix = new DenseMatrix64F(rhoSize, rhoSize);
      rhoRateWeightMatrix = new DenseMatrix64F(rhoSize, rhoSize);

      activeRhoMatrix = new DenseMatrix64F(rhoSize, 1);
      CommonOps.fill(activeRhoMatrix, 1.0);

      CommonOps.fill(rhoMaxMatrix, Double.POSITIVE_INFINITY);

      String bodyName = contactablePlaneBody.getName();
      String namePrefix = bodyName + "WrenchMatrixHelper";
      YoVariableRegistry registry = new YoVariableRegistry(namePrefix);

      RigidBody rigidBody = contactablePlaneBody.getRigidBody();
      planeFrame = new PoseReferenceFrame(namePrefix + "ContactFrame", rigidBody.getBodyFixedFrame());
      planeFrame.setPoseAndUpdate(contactablePlaneBody.getSoleFrame().getTransformToDesiredFrame(rigidBody.getBodyFixedFrame()));
      yoPlaneContactState = new YoPlaneContactState(namePrefix, rigidBody, planeFrame, contactPoints2d, 0.0, registry);
      yoPlaneContactState.clear();
      yoPlaneContactState.computeSupportPolygon();

      hasReset = new YoBoolean(namePrefix + "HasReset", registry);
      resetRequested = new YoBoolean(namePrefix + "ResetRequested", registry);
      deactivateRhoWhenNotInContact = new YoBoolean(namePrefix + "DeactivateRhoWhenNotInContact", registry);

      for (int i = 0; i < contactPoints2d.size(); i++)
      {
         YoDouble rhoWeight = new YoDouble(namePrefix + "RhoWeight" + i, registry);
         YoDouble maxContactForce = new YoDouble(namePrefix + "MaxContactForce" + i, registry);
         maxContactForce.set(Double.POSITIVE_INFINITY);

         rhoWeights.put(yoPlaneContactState.getContactPoints().get(i), rhoWeight);
         maxContactForces.put(yoPlaneContactState.getContactPoints().get(i), maxContactForce);
      }

      hasReceivedCenterOfPressureCommand = new YoBoolean(namePrefix + "HasReceivedCoPCommand", registry);
      isFootholdAreaLargeEnough = new YoBoolean(namePrefix + "isFootholdAreaLargeEnough", registry);
      desiredCoPCommandInSoleFrame = new YoFramePoint2D(namePrefix + "DesiredCoPCommand", planeFrame, registry);

      yoRho = new YoMatrix(namePrefix + "Rho", rhoSize, 1, registry);

      for (int i = 0; i < rhoSize; i++)
      {
         basisVectors.add(new FrameVector3D(centerOfMassFrame));
         basisVectorsOrigin.add(new FramePoint3D(centerOfMassFrame));
      }

      desiredCoP = new YoFramePoint3D(namePrefix + "DesiredCoP", planeFrame, registry);
      previousCoP = new YoFramePoint3D(namePrefix + "PreviousCoP", planeFrame, registry);
      ReferenceFrame bodyFixedFrame = rigidBody.getBodyFixedFrame();
      wrenchFromRho.setToZero(bodyFixedFrame, centerOfMassFrame);

      parentRegistry.addChild(registry);
   }

   public void setDeactivateRhoWhenNotInContact(boolean deactivateRhoWhenNotInContact)
   {
      this.deactivateRhoWhenNotInContact.set(deactivateRhoWhenNotInContact);
   }

   public void setPlaneContactStateCommand(PlaneContactStateCommand command)
   {
      RigidBodyTransform contactFramePose = command.getContactFramePoseInBodyFixedFrame();
      if (!contactFramePose.containsNaN())
         planeFrame.setPoseAndUpdate(contactFramePose);

      yoPlaneContactState.updateFromPlaneContactStateCommand(command);
      yoPlaneContactState.computeSupportPolygon();

      if (yoPlaneContactState.pollContactHasChangedNotification())
      {
         resetRequested.set(true);
      }

      for (int i = 0; i < command.getNumberOfContactPoints(); i++)
      {
         rhoWeights.get(yoPlaneContactState.getContactPoints().get(i)).set(command.getRhoWeight(i));
         if (command.hasMaxContactPointNormalForce())
         {
            maxContactForces.get(yoPlaneContactState.getContactPoints().get(i)).set(command.getMaxContactPointNormalForce(i));
         }
      }
   }

   public void setCenterOfPressureCommand(CenterOfPressureCommand command)
   {
      desiredCoPCommandInSoleFrame.set(command.getDesiredCoPInSoleFrame());
      desiredCoPCommandWeightInSoleFrame.set(command.getWeightInSoleFrame());
      hasReceivedCenterOfPressureCommand.set(true);
   }

   private final RecyclingArrayList<WrenchObjectiveCommand> wrenchObjectiveCommands = new RecyclingArrayList<>(WrenchObjectiveCommand.class);
   private final MutableInt wrenchTaskSize = new MutableInt(0);

   public void submitWrenchObjectiveCommand(WrenchObjectiveCommand command)
   {
      // Make sure the wrench is for this body!
      command.getWrench().getBodyFrame().checkReferenceFrameMatch(getRigidBody().getBodyFixedFrame());

      // At this point we can not yet compute the matrices since the order of the inverse dynamics commands is not guaranteed. This means
      // the contact state might change. So we need to hold on to the command and wait with computing the task matrices until all inverse
      // dynamics commands are handled.
      wrenchObjectiveCommands.add().set(command);
      wrenchTaskSize.add(command.getSelectionMatrix().getNumberOfSelectedAxes());
   }

   public int getWrenchTaskSize()
   {
      return wrenchTaskSize.intValue();
   }

   private final DenseMatrix64F commandTaskJacobian = new DenseMatrix64F(0, 0);
   private final DenseMatrix64F commandTaskObjective = new DenseMatrix64F(0, 0);
   private final DenseMatrix64F commandTaskWeight = new DenseMatrix64F(0, 0);

   public void getAdditionalRhoTasks(DenseMatrix64F rhoTaskJacobian, DenseMatrix64F rhoTaskObjective, DenseMatrix64F rhoTaskWeight)
   {
      int taskSize = getWrenchTaskSize();
      rhoTaskJacobian.reshape(taskSize, rhoSize);
      rhoTaskObjective.reshape(taskSize, 1);
      rhoTaskWeight.reshape(taskSize, taskSize);
      CommonOps.fill(rhoTaskJacobian, 0.0);
      CommonOps.fill(rhoTaskObjective, 0.0);
      CommonOps.fill(rhoTaskWeight, 0.0);

      taskSize = 0;
      for (int wrenchObjectiveIndex = 0; wrenchObjectiveIndex < wrenchObjectiveCommands.size(); wrenchObjectiveIndex++)
      {
         WrenchObjectiveCommand command = wrenchObjectiveCommands.get(wrenchObjectiveIndex);
         computeCommandMatrices(command, commandTaskJacobian, commandTaskObjective, commandTaskWeight);
         int bodyTaskSize = commandTaskObjective.getNumRows();

         CommonOps.insert(commandTaskJacobian, rhoTaskJacobian, taskSize, 0);
         CommonOps.insert(commandTaskObjective, rhoTaskObjective, taskSize, 0);
         CommonOps.insert(commandTaskWeight, rhoTaskWeight, taskSize, taskSize);

         taskSize = taskSize + bodyTaskSize;
      }

      // Quick sanity check on the task size computation. These numbers should be the same.
      if (taskSize != getWrenchTaskSize())
      {
         throw new RuntimeException("Something went wrong.");
      }

      wrenchObjectiveCommands.clear();
      wrenchTaskSize.setValue(0);
   }

   private final DenseMatrix64F tempTaskJacobian = new DenseMatrix64F(0, 0);
   private final DenseMatrix64F tempTaskObjective = new DenseMatrix64F(Wrench.SIZE, 1);
   private final DenseMatrix64F tempTaskWeight = new DenseMatrix64F(Wrench.SIZE, Wrench.SIZE);
   private final RigidBodyTransform tempTransform = new RigidBodyTransform();
   private final DenseMatrix64F tempRotationMatrix = new DenseMatrix64F(3, 3);
   private final DenseMatrix64F selectionFrameTransform = new DenseMatrix64F(6, 6);

   private void computeCommandMatrices(WrenchObjectiveCommand command, DenseMatrix64F taskJacobian, DenseMatrix64F taskObjective, DenseMatrix64F taskWeight)
   {
      // Get the matrices without considering the selection:
      Wrench wrench = command.getWrench();
      wrench.changeFrame(planeFrame);
      wrench.getMatrix(tempTaskObjective);
      command.getWeightMatrix().getFullWeightMatrixInFrame(planeFrame, tempTaskWeight);
      tempTaskJacobian.set(wrenchJacobianMatrix);

      // Pack the transformation matrix to the selection frame:
      MatrixTools.setToZero(selectionFrameTransform);
      SelectionMatrix6D selectionMatrix = command.getSelectionMatrix();
      // Angular part:
      ReferenceFrame angularSelectionFrame = selectionMatrix.getAngularSelectionFrame();
      angularSelectionFrame.getTransformToDesiredFrame(tempTransform, planeFrame);
      tempTransform.getRotation(tempRotationMatrix);
      CommonOps.insert(tempRotationMatrix, selectionFrameTransform, 0, 0);
      // Linear part:
      ReferenceFrame linearSelectionFrame = selectionMatrix.getLinearSelectionFrame();
      linearSelectionFrame.getTransformToDesiredFrame(tempTransform, planeFrame);
      tempTransform.getRotation(tempRotationMatrix);
      CommonOps.insert(tempRotationMatrix, selectionFrameTransform, 3, 3);

      // Now transform all matrices to the selection frame:
      taskJacobian.reshape(tempTaskJacobian.getNumRows(), tempTaskJacobian.getNumCols());
      taskObjective.reshape(tempTaskObjective.getNumRows(), tempTaskObjective.getNumCols());
      taskWeight.reshape(tempTaskWeight.getNumRows(), tempTaskWeight.getNumCols());
      CommonOps.mult(selectionFrameTransform, tempTaskJacobian, taskJacobian);
      CommonOps.mult(selectionFrameTransform, tempTaskObjective, taskObjective);
      CommonOps.mult(selectionFrameTransform, tempTaskWeight, taskWeight);

      // Remove the rows that are not selected. For this we need to start from the bottom so the row indices do not get mixed up as rows get removed.
      if (!selectionMatrix.isLinearZSelected())
      {
         MatrixTools.removeRow(taskJacobian, 5);
         MatrixTools.removeRow(taskObjective, 5);
         MatrixTools.removeRow(taskWeight, 5);
         MatrixTools.removeColumn(taskWeight, 5);
      }
      if (!selectionMatrix.isLinearYSelected())
      {
         MatrixTools.removeRow(taskJacobian, 4);
         MatrixTools.removeRow(taskObjective, 4);
         MatrixTools.removeRow(taskWeight, 4);
         MatrixTools.removeColumn(taskWeight, 4);
      }
      if (!selectionMatrix.isLinearXSelected())
      {
         MatrixTools.removeRow(taskJacobian, 3);
         MatrixTools.removeRow(taskObjective, 3);
         MatrixTools.removeRow(taskWeight, 3);
         MatrixTools.removeColumn(taskWeight, 3);
      }
      if (!selectionMatrix.isAngularZSelected())
      {
         MatrixTools.removeRow(taskJacobian, 2);
         MatrixTools.removeRow(taskObjective, 2);
         MatrixTools.removeRow(taskWeight, 2);
         MatrixTools.removeColumn(taskWeight, 2);
      }
      if (!selectionMatrix.isAngularYSelected())
      {
         MatrixTools.removeRow(taskJacobian, 1);
         MatrixTools.removeRow(taskObjective, 1);
         MatrixTools.removeRow(taskWeight, 1);
         MatrixTools.removeColumn(taskWeight, 1);
      }
      if (!selectionMatrix.isAngularYSelected())
      {
         MatrixTools.removeRow(taskJacobian, 0);
         MatrixTools.removeRow(taskObjective, 0);
         MatrixTools.removeRow(taskWeight, 0);
         MatrixTools.removeColumn(taskWeight, 0);
      }
   }

   public void computeMatrices(double defaultRhoWeight, double rhoRateWeight, Vector2D desiredCoPWeight, Vector2D copRateWeight)
   {
      int numberOfContactPointsInContact = yoPlaneContactState.getNumberOfContactPointsInContact();
      if (numberOfContactPointsInContact > maxNumberOfContactPoints)
         throw new RuntimeException("Unhandled number of contact points: " + numberOfContactPointsInContact);

      // Compute the orientation of the normal contact vector and the corresponding transformation matrix
      computeNormalContactVectorRotation(normalContactVectorRotationMatrix);

      List<YoContactPoint> contactPoints = yoPlaneContactState.getContactPoints();

      int rhoIndex = 0;

      for (int contactPointIndex = 0; contactPointIndex < yoPlaneContactState.getTotalNumberOfContactPoints(); contactPointIndex++)
      {
         YoContactPoint contactPoint = contactPoints.get(contactPointIndex);
         boolean inContact = contactPoint.isInContact();

         // rotate each friction cone approximation to point one vector towards the center of the foot
         double angleOffset = coneRotationCalculator.computeConeRotation(yoPlaneContactState, contactPointIndex);

         // in case the contact point is close to another point rotate it
         if (inContact)
         {
            int matches = 0;
            for (int j = contactPointIndex + 1; j < contactPoints.size(); j++)
            {
               YoContactPoint candidateForMatch = contactPoints.get(j);
               candidateForMatch.getPosition2d(contactPoint2d);
               if (candidateForMatch.isInContact() && contactPoint.epsilonEquals(contactPoint2d, distanceThresholdBetweenTwoContactPoint))
               {
                  matches++;
               }
            }
            // TODO: If there are more then two contacts in the same spot we should probably disable them.
            if (matches > 0)
            {
               angleOffset += basisVectorAngleIncrement / 2.0;
            }
         }

         for (int basisVectorIndex = 0; basisVectorIndex < numberOfBasisVectorsPerContactPoint; basisVectorIndex++)
         {
            FramePoint3D basisVectorOrigin = basisVectorsOrigin.get(rhoIndex);
            FrameVector3D basisVector = basisVectors.get(rhoIndex);

            if (inContact)
            {
               contactPoint.getPosition(basisVectorOrigin);
               computeBasisVector(basisVectorIndex, angleOffset, normalContactVectorRotationMatrix, basisVector);

               DenseMatrix64F singleRhoJacobian = computeSingleRhoJacobian(basisVectorOrigin, basisVector);
               CommonOps.insert(singleRhoJacobian, rhoJacobianMatrix, 0, rhoIndex);

               DenseMatrix64F singleRhoWrenchJacobian = computeSingleRhoWrenchJacobian(basisVectorOrigin, basisVector);
               CommonOps.insert(singleRhoWrenchJacobian, wrenchJacobianMatrix, 0, rhoIndex);

               DenseMatrix64F singleRhoCoPJacobian = computeSingleRhoCoPJacobian(basisVectorOrigin, basisVector);
               CommonOps.insert(singleRhoCoPJacobian, copJacobianMatrix, 0, rhoIndex);

               double rhoWeight = rhoWeights.get(yoPlaneContactState.getContactPoints().get(contactPointIndex)).getDoubleValue();
               if(Double.isNaN(rhoWeight))
               {
                  rhoWeight = defaultRhoWeight;
               }

               rhoWeightMatrix.set(rhoIndex, rhoIndex, rhoWeight * maxNumberOfContactPoints / numberOfContactPointsInContact);

               if (resetRequested.getBooleanValue())
                  rhoRateWeightMatrix.set(rhoIndex, rhoIndex, 0.0);
               else
                  rhoRateWeightMatrix.set(rhoIndex, rhoIndex, rhoRateWeight);

               activeRhoMatrix.set(rhoIndex, 0, 1.0);
            }
            else
            {
               clear(rhoIndex);

               if (deactivateRhoWhenNotInContact.getBooleanValue())
                  activeRhoMatrix.set(rhoIndex, 0, 0.0);
            }

            //// TODO: 6/5/17 scale this by the vertical magnitude
            rhoMaxMatrix.set(rhoIndex, 0, maxContactForces.get(yoPlaneContactState.getContactPoints().get(contactPointIndex)).getDoubleValue() / numberOfBasisVectorsPerContactPoint);

            rhoIndex++;
         }

      }

      isFootholdAreaLargeEnough.set(yoPlaneContactState.getFootholdArea() > 1.0e-3);
      if (yoPlaneContactState.inContact() && !resetRequested.getBooleanValue() && isFootholdAreaLargeEnough.getBooleanValue())
      {
         if (hasReceivedCenterOfPressureCommand.getBooleanValue())
         {
            desiredCoPMatrix.set(0, 0, desiredCoPCommandInSoleFrame.getX());
            desiredCoPMatrix.set(1, 0, desiredCoPCommandInSoleFrame.getY());
            desiredCoPWeightMatrix.set(0, 0, desiredCoPCommandWeightInSoleFrame.getX());
            desiredCoPWeightMatrix.set(1, 1, desiredCoPCommandWeightInSoleFrame.getY());

            hasReceivedCenterOfPressureCommand.set(false);
         }
         else
         {
            // // FIXME: 6/5/17 Is this ever even used now?
            desiredCoPMatrix.set(0, 0, desiredCoP.getX());
            desiredCoPMatrix.set(1, 0, desiredCoP.getY());
            desiredCoPWeightMatrix.set(0, 0, desiredCoPWeight.getX());
            desiredCoPWeightMatrix.set(1, 1, desiredCoPWeight.getY());
         }
         copRateWeightMatrix.set(0, 0, copRateWeight.getX());
         copRateWeightMatrix.set(1, 1, copRateWeight.getY());
      }
      else
      {
         desiredCoPMatrix.zero();
         desiredCoPWeightMatrix.zero();
         copRateWeightMatrix.zero();
      }

      hasReset.set(resetRequested.getBooleanValue()); // So it is visible from SCS when the reset has been processed.
      resetRequested.set(false);

      // Should not get there as long as the number of contact points of the contactable body is less or equal to maxNumberOfContactPoints.
      for (; rhoIndex < rhoSize; rhoIndex++)
         clear(rhoIndex);
   }

   private void clear(int rhoIndex)
   {
      FramePoint3D basisVectorOrigin = basisVectorsOrigin.get(rhoIndex);
      FrameVector3D basisVector = basisVectors.get(rhoIndex);

      basisVectorOrigin.setToZero(centerOfMassFrame);
      basisVector.setToZero(centerOfMassFrame);

      MatrixTools.zeroColumn(rhoIndex, rhoJacobianMatrix);
      MatrixTools.zeroColumn(rhoIndex, copJacobianMatrix);
      MatrixTools.zeroColumn(rhoIndex, wrenchJacobianMatrix);

      rhoMaxMatrix.set(rhoIndex, 0, Double.POSITIVE_INFINITY);
      rhoWeightMatrix.set(rhoIndex, rhoIndex, 1.0); // FIXME why is this setting to 1.0????
      rhoRateWeightMatrix.set(rhoIndex, rhoIndex, 0.0);
   }

   private final Wrench wrenchFromRho = new Wrench();
   private final DenseMatrix64F totalWrenchMatrix = new DenseMatrix64F(SpatialForceVector.SIZE, 1);

   public void computeWrenchFromRho(int startIndex, DenseMatrix64F allRobotRho)
   {
      CommonOps.extract(allRobotRho, startIndex, startIndex + rhoSize, 0, 1, rhoMatrix, 0, 0);
      yoRho.set(rhoMatrix);

      if (yoPlaneContactState.inContact())
      {
         ReferenceFrame bodyFixedFrame = getRigidBody().getBodyFixedFrame();
         CommonOps.mult(wrenchJacobianMatrix, rhoMatrix, totalWrenchMatrix);
         wrenchFromRho.set(bodyFixedFrame, planeFrame, totalWrenchMatrix);

         CommonOps.mult(copJacobianMatrix, rhoMatrix, previousCoPMatrix);
         previousCoP.setX(previousCoPMatrix.get(0, 0));
         previousCoP.setY(previousCoPMatrix.get(1, 0));
      }
      else
      {
         wrenchFromRho.setToZero();
      }
   }

   private void computeNormalContactVectorRotation(RotationMatrix normalContactVectorRotationMatrixToPack)
   {
      yoPlaneContactState.getContactNormalFrameVector(contactNormalVector);
      contactNormalVector.changeFrame(planeFrame);
      contactNormalVector.normalize();
      EuclidGeometryTools.axisAngleFromZUpToVector3D(contactNormalVector, normalContactVectorRotation);
      normalContactVectorRotationMatrixToPack.set(normalContactVectorRotation);
   }

   private void computeBasisVector(int basisVectorIndex, double rotationOffset, RotationMatrix normalContactVectorRotationMatrix, FrameVector3D basisVectorToPack)
   {
      double angle = rotationOffset + basisVectorIndex * basisVectorAngleIncrement;
      double mu = yoPlaneContactState.getCoefficientOfFriction();

      // Compute the linear part considering a normal contact vector pointing z-up
      basisVectorToPack.setIncludingFrame(planeFrame, Math.cos(angle) * mu, Math.sin(angle) * mu, 1.0);

      // Transforming the result to consider the actual normal contact vector
      normalContactVectorRotationMatrix.transform(basisVectorToPack);
      basisVectorToPack.normalize();
   }

   private final SpatialForceVector unitSpatialForceVector = new SpatialForceVector();
   private final DenseMatrix64F singleRhoJacobian = new DenseMatrix64F(SpatialForceVector.SIZE, 1);

   private DenseMatrix64F computeSingleRhoJacobian(FramePoint3D basisVectorOrigin, FrameVector3D basisVector)
   {
      basisVectorOrigin.changeFrame(centerOfMassFrame);
      basisVector.changeFrame(centerOfMassFrame);

      // Compute the unit wrench corresponding to the basis vector
      unitSpatialForceVector.setIncludingFrame(basisVector, basisVectorOrigin);
      unitSpatialForceVector.getMatrix(singleRhoJacobian);
      return singleRhoJacobian;
   }

   private final DenseMatrix64F singleRhoWrenchJacobian = new DenseMatrix64F(Wrench.SIZE, 1);

   private DenseMatrix64F computeSingleRhoWrenchJacobian(FramePoint3D basisVectorOrigin, FrameVector3D basisVector)
   {
      basisVectorOrigin.changeFrame(planeFrame);
      basisVector.changeFrame(planeFrame);

      // Compute the unit wrench corresponding to the basis vector
      unitSpatialForceVector.setIncludingFrame(basisVector, basisVectorOrigin);
      unitSpatialForceVector.getMatrix(singleRhoWrenchJacobian);
      return singleRhoWrenchJacobian;
   }

   private final FrameVector3D forceFromRho = new FrameVector3D();
   private final DenseMatrix64F singleRhoCoPJacobian = new DenseMatrix64F(2, 1);

   private DenseMatrix64F computeSingleRhoCoPJacobian(FramePoint3D basisVectorOrigin, FrameVector3D basisVector)
   {
      wrenchFromRho.getLinearPartIncludingFrame(forceFromRho);
      forceFromRho.changeFrame(planeFrame);

      if (forceFromRho.getZ() > 1.0e-1)
      {
         basisVectorOrigin.changeFrame(planeFrame);
         basisVector.changeFrame(planeFrame);

         unitSpatialForceVector.setIncludingFrame(basisVector, basisVectorOrigin);

         singleRhoCoPJacobian.set(0, 0, -unitSpatialForceVector.getAngularPartY() / forceFromRho.getZ());
         singleRhoCoPJacobian.set(1, 0, unitSpatialForceVector.getAngularPartX() / forceFromRho.getZ());
      }
      else
      {
         singleRhoCoPJacobian.zero();
      }

      return singleRhoCoPJacobian;
   }

   public RigidBody getRigidBody()
   {
      return yoPlaneContactState.getRigidBody();
   }

   public int getRhoSize()
   {
      return rhoSize;
   }

   public DenseMatrix64F getLastRho()
   {
      return rhoMatrix;
   }

   public DenseMatrix64F getRhoJacobian()
   {
      return rhoJacobianMatrix;
   }

   public DenseMatrix64F getActiveRhoMatrix()
   {
      return activeRhoMatrix;
   }

   public DenseMatrix64F getRhoMax()
   {
      return rhoMaxMatrix;
   }

   public DenseMatrix64F getRhoWeight()
   {
      return rhoWeightMatrix;
   }

   public DenseMatrix64F getRhoRateWeight()
   {
      return rhoRateWeightMatrix;
   }

   public Wrench getWrenchFromRho()
   {
      return wrenchFromRho;
   }

   public DenseMatrix64F getCopJacobianMatrix()
   {
      return copJacobianMatrix;
   }

   public DenseMatrix64F getDesiredCoPMatrix()
   {
      return desiredCoPMatrix;
   }

   public DenseMatrix64F getPreviousCoPMatrix()
   {
      return previousCoPMatrix;
   }

   public DenseMatrix64F getDesiredCoPWeightMatrix()
   {
      return desiredCoPWeightMatrix;
   }

   public DenseMatrix64F getCoPRateWeightMatrix()
   {
      return copRateWeightMatrix;
   }

   public List<FramePoint3D> getBasisVectorsOrigin()
   {
      return basisVectorsOrigin;
   }

   public List<FrameVector3D> getBasisVectors()
   {
      return basisVectors;
   }

   public boolean hasReset()
   {
      return hasReset.getBooleanValue();
   }
}
