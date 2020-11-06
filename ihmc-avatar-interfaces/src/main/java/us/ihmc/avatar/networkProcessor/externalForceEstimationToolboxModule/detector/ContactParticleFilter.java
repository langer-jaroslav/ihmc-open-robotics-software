package us.ihmc.avatar.networkProcessor.externalForceEstimationToolboxModule.detector;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import us.ihmc.avatar.networkProcessor.externalForceEstimationToolboxModule.ContactPointParticle;
import us.ihmc.avatar.networkProcessor.externalForceEstimationToolboxModule.ExternalForceEstimationTools;
import us.ihmc.avatar.networkProcessor.externalForceEstimationToolboxModule.ForceEstimatorDynamicMatrixUpdater;
import us.ihmc.avatar.networkProcessor.externalForceEstimationToolboxModule.JointspaceExternalContactEstimator;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tools.EuclidCoreRandomTools;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DReadOnly;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicVector;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsList;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.matrixlib.MatrixTools;
import us.ihmc.mecano.multiBodySystem.interfaces.JointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.JointReadOnly;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.robotics.physics.Collidable;
import us.ihmc.simulationconstructionset.util.RobotController;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFramePoint3D;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFrameVector3D;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoInteger;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Implementation of the particle-filter based external contact estimator presented here:
 * http://groups.csail.mit.edu/robotics-center/public_papers/Manuelli16.pdf
 */
public class ContactParticleFilter implements RobotController
{
   private static final int numberOfParticles = 150;
   private static final int estimationVariables = 3;

   private final String name = getClass().getSimpleName();
   private final YoRegistry registry = new YoRegistry(name);
   private final Random random = new Random(34098);

   private final YoDouble coefficientOfFriction = new YoDouble("coefficientOfFriction", registry);
   private final YoDouble maximumSampleStandardDeviation = new YoDouble("maximumSampleStandardDeviation", registry);
   private final YoDouble minimumSampleStandardDeviation = new YoDouble("minimumSampleStandardDeviation", registry);
   private final YoDouble upperMotionBoundForSamplingAdjustment = new YoDouble("upperMotionBoundForSamplingAdjustment", registry);

   private final JointBasics[] joints;
   private final int dofs;
   private final JointspaceExternalContactEstimator jointspaceExternalContactEstimator;
   private final DMatrixRMaj systemJacobian;
   private final DMatrixRMaj jointNoiseVariance;

   private final ContactPointEvaluator contactPointEvaluator = new ContactPointEvaluator();
   private final ContactPointProjector contactPointProjector;

   private final ContactPointParticle[] contactPointParticles = new ContactPointParticle[numberOfParticles];
   private final YoDouble[] contactPointProbabilities = new YoDouble[numberOfParticles];
   private final FramePoint3D[] sampledContactPositions = new FramePoint3D[numberOfParticles];
   private final YoFramePoint3D[] contactPointPositions = new YoFramePoint3D[numberOfParticles];
   private final YoFrameVector3D[] scaledSurfaceNormal = new YoFrameVector3D[numberOfParticles];

   private final FramePoint3D estimatedContactPosition = new FramePoint3D();
   private final FrameVector3D estimatedContactNormal = new FrameVector3D();
   private final YoFramePoint3D yoEstimatedContactPosition = new YoFramePoint3D("estimatedContactPosition", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector3D yoEstimatedContactNormal = new YoFrameVector3D("estimatedContactNormal", ReferenceFrame.getWorldFrame(), registry);

   // Resampling variables
   private final double[] histogramValues = new double[numberOfParticles];
   private final int[] contactPointIndicesToSample = new int[numberOfParticles];
   private final FramePoint3D pointToProject = new FramePoint3D();

   // average particle data
   private final YoFramePoint3D averageParticlePosition = new YoFramePoint3D("averageParticlePosition", ReferenceFrame.getWorldFrame(), registry);
   private final YoFramePoint3D filteredAverageParticlePosition = new YoFramePoint3D("filteredAverageParticlePosition", ReferenceFrame.getWorldFrame(), registry);
   private final YoDouble alphaAverageParticlePosition = new YoDouble("alphaAverageParticlePosition", registry);
   private final YoDouble filteredAverageParticleVelocity = new YoDouble("filteredAverageParticleVelocity", registry);
   private final FramePoint3D previousFilteredAverageParticlePosition = new FramePoint3D();

   // termination conditions
   private final YoInteger iterations = new YoInteger("iterations", registry);
   private final YoInteger maxIterations = new YoInteger("maxIterations", registry);
   private final YoInteger minIterations = new YoInteger("minIterations", registry);
   private final YoDouble terminalParticleVelocity = new YoDouble("terminalParticleVelocity", registry);
   private final YoBoolean hasConverged = new YoBoolean("hasConverged", registry);

   // debugging variables
   private final YoDouble pClosestParticle = new YoDouble("pClosestParticle", registry);
   private final YoDouble pMax = new YoDouble("pMax", registry);
   private final YoDouble distOfPMax = new YoDouble("distOfPMax", registry);
   private final YoDouble estimatedPointError = new YoDouble("estimatedPointError", registry);

   private RigidBodyBasics actualContactingBody;
   private final Vector3D actualContactPointInParentJointFrame = new Vector3D();
   private final FramePoint3D actualContactPoint = new FramePoint3D();
   private final YoFramePoint3D yoActualContactPoint = new YoFramePoint3D("debugContactPoint", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector3D yoActualSurfaceNormal = new YoFrameVector3D("debugSurfaceNormal", ReferenceFrame.getWorldFrame(), registry);

   private boolean firstTick = true;

   public ContactParticleFilter(JointBasics[] joints,
                                double dt,
                                ForceEstimatorDynamicMatrixUpdater dynamicMatrixUpdater,
                                List<Collidable> collidables,
                                YoGraphicsListRegistry graphicsListRegistry,
                                YoRegistry parentRegistry)
   {
      this.joints = joints;
      this.jointspaceExternalContactEstimator = new JointspaceExternalContactEstimator(joints, dt, dynamicMatrixUpdater, registry);
      this.dofs = Arrays.stream(joints).mapToInt(JointReadOnly::getDegreesOfFreedom).sum();
      this.systemJacobian = new DMatrixRMaj(estimationVariables, dofs);
      this.jointNoiseVariance = CommonOps_DDRM.identity(dofs);
      this.contactPointProjector = new ContactPointProjector(collidables);

      for (int i = 0; i < numberOfParticles; i++)
      {
         contactPointProbabilities[i] = new YoDouble("cpProb_" + i, registry);
         contactPointPositions[i] = new YoFramePoint3D("cpPosition_" + i, ReferenceFrame.getWorldFrame(), registry);
         scaledSurfaceNormal[i] = new YoFrameVector3D("cpNorm_" + i, ReferenceFrame.getWorldFrame(), registry);
         sampledContactPositions[i] = new FramePoint3D();
         contactPointParticles[i] = new ContactPointParticle("particle" + i, joints);
      }

      if (graphicsListRegistry != null)
      {
         YoGraphicsList actualContactList = new YoGraphicsList("actualContact");
         YoGraphicPosition actualContactPointGraphic = new YoGraphicPosition("cpPositionVizDebug", yoActualContactPoint, 0.025, YoAppearance.Red());
         YoGraphicVector actualContactVectorGraphic = new YoGraphicVector("cpNormVizDebug", yoActualContactPoint, yoActualSurfaceNormal, 1.0, YoAppearance.Red());
         actualContactList.add(actualContactPointGraphic);
         actualContactList.add(actualContactVectorGraphic);
         graphicsListRegistry.registerYoGraphicsList(actualContactList);

         YoGraphicPosition averageParticlePositionGraphic = new YoGraphicPosition("averageParticleViz", filteredAverageParticlePosition, 0.025, YoAppearance.DarkBlue());
         YoGraphicPosition estimatedContactPositionGraphic = new YoGraphicPosition("estimatedCPViz", yoEstimatedContactPosition, 0.025, YoAppearance.Green());
         YoGraphicVector estimatedContactNormalGraphic = new YoGraphicVector("estimatedCPNormalViz", yoEstimatedContactPosition, yoEstimatedContactNormal, 0.07, YoAppearance.Green());
         graphicsListRegistry.registerYoGraphic(name, averageParticlePositionGraphic);
         graphicsListRegistry.registerYoGraphic(name, estimatedContactPositionGraphic);
         graphicsListRegistry.registerYoGraphic(name, estimatedContactNormalGraphic);

         for (int i = 0; i < numberOfParticles; i++)
         {
            YoGraphicPosition contactPointGraphic = new YoGraphicPosition("cpPositionViz_" + i, contactPointPositions[i], 0.005, YoAppearance.Blue());
            graphicsListRegistry.registerYoGraphic(name, contactPointGraphic);
         }
      }

      coefficientOfFriction.set(0.5);
      maximumSampleStandardDeviation.set(0.1);
      minimumSampleStandardDeviation.set(0.02);
      upperMotionBoundForSamplingAdjustment.set(0.1);
      filteredAverageParticleVelocity.set(upperMotionBoundForSamplingAdjustment.getDoubleValue());
      alphaAverageParticlePosition.set(0.2);

      double maxTime = 10.0;
      minIterations.set(20);
      maxIterations.set((int) (maxTime / dt));
      terminalParticleVelocity.set(1.0e-4);
      firstTick = true;

      if (parentRegistry != null)
      {
         parentRegistry.addChild(registry);
      }
   }

   @Override
   public void initialize()
   {
      jointspaceExternalContactEstimator.initialize();
      contactPointEvaluator.setCoefficientOfFriction(coefficientOfFriction.getDoubleValue());
      computeInitialProjection();

      iterations.set(0);
      hasConverged.set(false);
   }

   private void computeInitialProjection()
   {
      double initialProjectionRadius = 1.0;
      RigidBodyBasics[] rigidBodies = contactPointProjector.getCollidableRigidBodies();

      for (int i = 0; i < numberOfParticles; i++)
      {
         RigidBodyBasics rigidBody = rigidBodies[i % rigidBodies.length];

         Vector3D randomVector = EuclidCoreRandomTools.nextVector3DWithFixedLength(random, initialProjectionRadius);
         pointToProject.setIncludingFrame(rigidBody.getBodyFixedFrame(), randomVector);
         pointToProject.changeFrame(ReferenceFrame.getWorldFrame());

         contactPointParticles[i].setRigidBody(rigidBody);
         contactPointProjector.projectToSpecificLink(pointToProject, contactPointParticles[i].getContactPointPosition(), contactPointParticles[i].getSurfaceNormal(), rigidBody);
         contactPointParticles[i].update();
      }
   }

   @Override
   public void doControl()
   {
      jointspaceExternalContactEstimator.doControl();
      DMatrixRMaj observedExternalJointTorque = jointspaceExternalContactEstimator.getObservedExternalJointTorque();

      double totalWeight = 0.0;

      // Evaluate likelihood of each point
      for (int i = 0; i < numberOfParticles; i++)
      {
         ContactPointParticle contactPoint = contactPointParticles[i];
         DMatrixRMaj contactPointJacobian = contactPoint.computeContactJacobian();

         for (int j = 0; j < contactPointJacobian.getNumCols(); j++)
         {
            int column = contactPoint.getSystemJacobianIndex(j);
            MatrixTools.setMatrixBlock(systemJacobian, 0, column, contactPointJacobian, 0, j, estimationVariables, 1, 1.0);
         }

         double likelihoodCost = contactPointEvaluator.computeMaximumLikelihoodForce(observedExternalJointTorque,
                                                                                     systemJacobian,
                                                                                     jointNoiseVariance,
                                                                                     contactPointParticles[i].getContactPointFrame());
         double likelihoodWeight = Math.exp(-0.5 * likelihoodCost);
         contactPointProbabilities[i].set(likelihoodWeight);
         totalWeight += likelihoodWeight;
      }

      for (int i = 0; i < numberOfParticles; i++)
      {
         contactPointProbabilities[i].mul(1.0 / totalWeight);

         double previousHistogramValue = i == 0 ? 0.0 : histogramValues[i - 1];
         histogramValues[i] = previousHistogramValue + contactPointProbabilities[i].getDoubleValue();
      }

      // Select points to resample
      for (int i = 0; i < numberOfParticles; i++)
      {
         double randomDouble = EuclidCoreRandomTools.nextDouble(random, ((double) i) / numberOfParticles, ((double) (i + 1)) / numberOfParticles);

         for (int j = 0; j < histogramValues.length; j++)
         {
            if (j == histogramValues.length - 1 || histogramValues[j] > randomDouble)
            {
               contactPointIndicesToSample[i] = j;
               break;
            }
         }
      }

      updateDebugVariables();

      // Perform resample
      for (int i = 0; i < numberOfParticles; i++)
      {
         int resampleIndex = contactPointIndicesToSample[i];
         ContactPointParticle contactPointToSample = contactPointParticles[resampleIndex];

         boolean foundPointOutsideMesh = false;
         while (!foundPointOutsideMesh)
         {
            Vector3D offset = generateSamplingOffset();
            FramePoint3D sampledContactPosition = sampledContactPositions[i];
            sampledContactPosition.setIncludingFrame(contactPointToSample.getContactPointFrame(), offset);

            if (!contactPointProjector.isPointInsideAnyRigidBody(sampledContactPosition))
            {
               foundPointOutsideMesh = true;
            }
         }
      }

      // Project to surface
      for (int i = 0; i < numberOfParticles; i++)
      {
         RigidBodyBasics closestLink = contactPointProjector.projectToClosestLink(sampledContactPositions[i],
                                                                                  contactPointParticles[i].getContactPointPosition(),
                                                                                  contactPointParticles[i].getSurfaceNormal());
         contactPointParticles[i].setRigidBody(closestLink);
         contactPointParticles[i].update();

         contactPointParticles[i].getContactPointPosition().changeFrame(ReferenceFrame.getWorldFrame());
         contactPointParticles[i].getSurfaceNormal().changeFrame(ReferenceFrame.getWorldFrame());

         contactPointPositions[i].set(contactPointParticles[i].getContactPointPosition());
         scaledSurfaceNormal[i].set(contactPointParticles[i].getSurfaceNormal());
         scaledSurfaceNormal[i].scale(contactPointProbabilities[i].getDoubleValue());
      }

      estimatedContactPosition.setToZero(ReferenceFrame.getWorldFrame());
      for (int i = 0; i < numberOfParticles; i++)
      {
         estimatedContactPosition.add(contactPointPositions[i]);
      }
      estimatedContactPosition.scale(1.0 / numberOfParticles);

      if (firstTick)
      {
         filteredAverageParticlePosition.set(estimatedContactPosition);
      }

      previousFilteredAverageParticlePosition.setIncludingFrame(filteredAverageParticlePosition);
      averageParticlePosition.set(estimatedContactPosition);
      filteredAverageParticlePosition.interpolate(averageParticlePosition, alphaAverageParticlePosition.getValue());

      if (!firstTick)
      {
         filteredAverageParticleVelocity.set(previousFilteredAverageParticlePosition.distance(filteredAverageParticlePosition));
      }

      projectEstimatedPoint();

      estimatedContactPosition.changeFrame(ReferenceFrame.getWorldFrame());
      estimatedContactNormal.changeFrame(ReferenceFrame.getWorldFrame());
      yoEstimatedContactPosition.set(estimatedContactPosition);
      yoEstimatedContactNormal.set(estimatedContactNormal);

      if (actualContactingBody != null)
         estimatedPointError.set(estimatedContactPosition.distance(actualContactPoint));

      hasConverged.set(checkTerminationConditions());
      firstTick = false;
   }

   public void setDoFVariance(int dofIndex, double variance)
   {
      jointNoiseVariance.set(dofIndex, dofIndex, variance);
   }

   /**
    * Tries to project average particle onto collision mesh. If the point is inside the meshes of multiple rigid bodies is doesn't perform a projection
    */
   private void projectEstimatedPoint()
   {
      List<RigidBodyBasics> collidingRigidBodies = contactPointProjector.computeCollidingRigidBodies(estimatedContactPosition);

      if (collidingRigidBodies.isEmpty())
      {
         pointToProject.setIncludingFrame(estimatedContactPosition);
         contactPointProjector.projectToClosestLink(pointToProject, estimatedContactPosition, estimatedContactNormal);
      }
      else if (collidingRigidBodies.size() == 1)
      {
         RigidBodyBasics rigidBody = collidingRigidBodies.get(0);
         Vector3D sphericalQuery = new Vector3D();

         estimatedContactPosition.changeFrame(rigidBody.getBodyFixedFrame());
         ExternalForceEstimationTools.transformToSphericalCoordinates(estimatedContactPosition, sphericalQuery);

         boolean pointIsInside = true;
         double radiusIncrement = 0.01;

         while (pointIsInside)
         {
            sphericalQuery.addX(radiusIncrement);
            estimatedContactPosition.changeFrame(rigidBody.getBodyFixedFrame());
            ExternalForceEstimationTools.transformToCartesianCoordinates(sphericalQuery, estimatedContactPosition);
            pointIsInside = contactPointProjector.isPointInside(estimatedContactPosition, rigidBody);
         }

         pointToProject.setIncludingFrame(estimatedContactPosition);
         contactPointProjector.projectToClosestLink(pointToProject, estimatedContactPosition, estimatedContactNormal);
      }
      else
      {
         // if inside inside multiple rigid bodys' meshes, don't project
         return;
      }
   }

   private Vector3D generateSamplingOffset()
   {
      double clampedEstimatedPositionMotion = EuclidCoreTools.clamp(filteredAverageParticleVelocity.getDoubleValue(),
                                                                    0.0,
                                                                    upperMotionBoundForSamplingAdjustment.getDoubleValue());
      double standardDeviation = EuclidCoreTools.interpolate(minimumSampleStandardDeviation.getDoubleValue(),
                                                             maximumSampleStandardDeviation.getDoubleValue(),
                                                             clampedEstimatedPositionMotion / upperMotionBoundForSamplingAdjustment.getDoubleValue());

      // perform sampling in spherical coordinates along a hemisphere
      double offsetRadius = standardDeviation * Math.abs(random.nextGaussian());
      double theta = 2.0 * Math.PI * random.nextDouble();
      double phi = Math.acos(random.nextDouble());

      double offsetX = offsetRadius * Math.cos(theta) * Math.sin(phi);
      double offsetY = offsetRadius * Math.sin(theta) * Math.sin(phi);
      double offsetZ = offsetRadius * Math.cos(phi);

      return new Vector3D(offsetX, offsetY, offsetZ);
   }

   public void setActualContactingBodyForDebugging(String parentJointName, Tuple3DReadOnly offsetInParentJointFrame)
   {
      for (int i = 0; i < joints.length; i++)
      {
         if (joints[i].getName().equals(parentJointName))
         {
            actualContactingBody = joints[i].getSuccessor();
            break;
         }
      }

      this.actualContactPointInParentJointFrame.set(offsetInParentJointFrame);
   }

   private boolean checkTerminationConditions()
   {
      iterations.increment();
      return iterations.getValue() > minIterations.getValue() && (iterations.getValue() >= maxIterations.getValue()
                                                                  || filteredAverageParticleVelocity.getValue() < terminalParticleVelocity.getValue());
   }

   public boolean hasConverged()
   {
      return hasConverged.getBooleanValue();
   }

   private void updateDebugVariables()
   {
      if (actualContactingBody == null)
         return;

      actualContactPoint.setIncludingFrame(actualContactingBody.getParentJoint().getFrameAfterJoint(), actualContactPointInParentJointFrame);
      actualContactPoint.changeFrame(ReferenceFrame.getWorldFrame());
      yoActualContactPoint.set(actualContactPoint);

      double maxProbability = -1.0;
      double distanceOfMaxProbability = 0.0;
      double closestParticleDistance = Double.POSITIVE_INFINITY;
      int indexClosestParticle = -1;

      for (int i = 0; i < numberOfParticles; i++)
      {
         contactPointParticles[i].getContactPointPosition().changeFrame(ReferenceFrame.getWorldFrame());

         if (contactPointProbabilities[i].getValue() > maxProbability)
         {
            maxProbability = contactPointProbabilities[i].getValue();
            distanceOfMaxProbability = contactPointParticles[i].getContactPointPosition().distance(actualContactPoint);
         }

         double distance = contactPointParticles[i].getContactPointPosition().distance(actualContactPoint);
         if (distance < closestParticleDistance)
         {
            closestParticleDistance = distance;
            indexClosestParticle = i;
         }
      }

      pMax.set(maxProbability);
      distOfPMax.set(distanceOfMaxProbability);
      pClosestParticle.set(contactPointProbabilities[indexClosestParticle].getValue());
   }

   @Override
   public YoRegistry getYoRegistry()
   {
      return registry;
   }
}
