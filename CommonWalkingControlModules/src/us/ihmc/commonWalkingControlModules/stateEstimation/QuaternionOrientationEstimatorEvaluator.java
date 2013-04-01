package us.ihmc.commonWalkingControlModules.stateEstimation;

import java.util.ArrayList;

import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.commonWalkingControlModules.dynamics.InverseDynamicsJointsFromSCSRobotGenerator;
import us.ihmc.controlFlow.AbstractControlFlowElement;
import us.ihmc.controlFlow.ControlFlowGraph;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.sensorProcessing.signalCorruption.BiasVectorCorruptor;
import us.ihmc.sensorProcessing.signalCorruption.GaussianOrientationCorruptor;
import us.ihmc.sensorProcessing.signalCorruption.GaussianVectorCorruptor;
import us.ihmc.sensorProcessing.simulatedSensors.SimulatedAngularVelocitySensor;
import us.ihmc.sensorProcessing.simulatedSensors.SimulatedOrientationSensor;
import us.ihmc.utilities.Axis;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.RigidBodyInertia;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.FloatingJoint;
import com.yobotics.simulationconstructionset.IMUMount;
import com.yobotics.simulationconstructionset.Joint;
import com.yobotics.simulationconstructionset.Link;
import com.yobotics.simulationconstructionset.PinJoint;
import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.robotController.RobotController;

public class QuaternionOrientationEstimatorEvaluator
{
   private static final boolean INITIALIZE_ANGULAR_VELOCITY_ESTIMATE_TO_ACTUAL = true; //false;
   private static final boolean USE_ANGULAR_ACCELERATION_INPUT = true; 
   private static final boolean CREATE_ORIENTATION_SENSOR = true;
   private static final boolean CREATE_ANGULAR_VELOCITY_SENSOR = true;
   private static final boolean USE_COMPOSABLE_ESTIMATOR = true;
   private static final boolean ADD_ARM_LINKS = true;

   private final double orientationMeasurementStandardDeviation = Math.sqrt(1e-1);    
   private final double angularVelocityMeasurementStandardDeviation = Math.sqrt(1e-1);   
   private final double angularAccelerationProcessNoiseStandardDeviation = Math.sqrt(1.0);
   private final double angularVelocityBiasProcessNoiseStandardDeviation = Math.sqrt(1e-2);

   private final double simDT = 1e-3;
   private final int simTicksPerControlDT = 5;
   private final double controlDT = simDT * simTicksPerControlDT;
   private final int simTicksPerRecord = simTicksPerControlDT;

   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);

   public QuaternionOrientationEstimatorEvaluator()
   {
      QuaternionOrientationEstimatorEvaluatorRobot robot = new QuaternionOrientationEstimatorEvaluatorRobot();
      QuaternionOrientationEstimatorEvaluatorController controller = new QuaternionOrientationEstimatorEvaluatorController(robot, controlDT);
      robot.setController(controller, simTicksPerControlDT);

      SimulationConstructionSet scs = new SimulationConstructionSet(robot, 32000);
      scs.addYoVariableRegistry(registry);

      scs.setDT(simDT, simTicksPerRecord);
      scs.setSimulateDuration(45.0);
      scs.startOnAThread();
   }

   private class QuaternionOrientationEstimatorEvaluatorRobot extends Robot
   {
      private static final long serialVersionUID = 2647791981594204134L;
      private final Link bodyLink;
      private final FloatingJoint rootJoint;
      private final ArrayList<IMUMount> imuMounts = new ArrayList<IMUMount>();
      
      public QuaternionOrientationEstimatorEvaluatorRobot()
      {
         super("QuaternionOrientationEstimatorEvaluatorRobot");

         rootJoint = new FloatingJoint("root", new Vector3d(), this);

         bodyLink = new Link("body");
         bodyLink.setMassAndRadiiOfGyration(10.0, 0.1, 0.2, 0.3);

         Graphics3DObject bodyLinkGraphics = new Graphics3DObject();
         bodyLinkGraphics.translate(0.0, 0.0, -0.15);
         bodyLinkGraphics.addCube(0.1, 0.2, 0.3, YoAppearance.Red());
         bodyLink.setLinkGraphics(bodyLinkGraphics);
         rootJoint.setLink(bodyLink);

         Transform3D imu0Offset = new Transform3D();
         IMUMount imuMount0 = new IMUMount("imuMount0", imu0Offset, this);
         rootJoint.addIMUMount(imuMount0);
         
         this.addRootJoint(rootJoint);
         
         if (ADD_ARM_LINKS)
         {
            PinJoint pinJoint1 = new PinJoint("pinJoint1", new Vector3d(), this, Axis.X);
            Link armLink1 = new Link("armLink1");
            armLink1.setMassAndRadiiOfGyration(0.3, 0.1, 0.1, 0.1);
            armLink1.setComOffset(new Vector3d(0.0, 0.0, 0.5));
            
            Graphics3DObject armLink1Graphics = new Graphics3DObject();
//            armLink1Graphics.rotate(-Math.PI/2.0, Graphics3DObject.X);
            armLink1Graphics.addCylinder(1.0, 0.02, YoAppearance.Green());
            armLink1.setLinkGraphics(armLink1Graphics);
            pinJoint1.setLink(armLink1);
            
            Transform3D imu1Offset = new Transform3D();
            IMUMount imuMount1 = new IMUMount("imuMount1", imu1Offset, this);
            pinJoint1.addIMUMount(imuMount1);
            
            rootJoint.addJoint(pinJoint1);
            
            PinJoint pinJoint2 = new PinJoint("pinJoint2", new Vector3d(0.0, 0.0, 1.0), this, Axis.Z);
            Link armLink2 = new Link("armLink2");
            armLink2.setMassAndRadiiOfGyration(0.2, 0.1, 0.1, 0.1);
            armLink2.setComOffset(new Vector3d(0.5, 0.0, 0.0));

            Graphics3DObject armLink2Graphics = new Graphics3DObject();
            armLink2Graphics.rotate(Math.PI/2.0, Graphics3DObject.Y);
            armLink2Graphics.addCylinder(1.0, 0.02, YoAppearance.Blue());
            armLink2.setLinkGraphics(armLink2Graphics);
            pinJoint2.setLink(armLink2);

            Transform3D imu2Offset = new Transform3D();
            imu2Offset.rotY(Math.PI/8.0);
            imu2Offset.setTranslation(new Vector3d(0.0, 0.0, 0.1));
            IMUMount imuMount2 = new IMUMount("imuMount2", imu2Offset, this);
            pinJoint2.addIMUMount(imuMount2);
            
            pinJoint1.addJoint(pinJoint2);
            
            pinJoint1.setQ(1.2); 
            pinJoint2.setQ(0.8); 
            
            pinJoint1.setQd(-0.5); 
            pinJoint2.setQd(0.77); 

            imuMounts.add(imuMount0);
            imuMounts.add(imuMount1);
            imuMounts.add(imuMount2);
         }            
         
         else
         {
            imuMounts.add(imuMount0);
         }
         
         this.setGravity(0.0);

         if (ADD_ARM_LINKS)
         {
            rootJoint.setPosition(new Point3d(0.0, 0.0, 0.4));
            Matrix3d rotationMatrix = new Matrix3d();
            rotationMatrix.rotX(0.6);
            rootJoint.setRotation(rotationMatrix);
            
            rootJoint.setAngularVelocityInBody(new Vector3d(0.2, 2.2, 0.3)); 
         }
         
         else
         {
            rootJoint.setPosition(new Point3d(0.0, 0.0, 0.4));
            rootJoint.setAngularVelocityInBody(new Vector3d(0.2, 2.5, 0.0));
         }
         
         update();
      }

//      public Link getBodyLink()
//      {
//         return bodyLink;
//      }

      public FloatingJoint getRootJoint()
      {
         return rootJoint;
      }
      
      public ArrayList<IMUMount> getIMUMounts()
      {
         return imuMounts;
      }

      public Vector3d getActualAngularAccelerationInBodyFrame()
      {
         return rootJoint.getAngularAccelerationInBody();
      }
      
      public Quat4d getActualOrientation()
      {
         return rootJoint.getQuaternion();
      }
   }


   private class QuaternionOrientationEstimatorEvaluatorFullRobotModel
   {
      private final InverseDynamicsJointsFromSCSRobotGenerator generator;
      
      private final RigidBody elevator;
      private final SixDoFJoint rootInverseDynamicsJoint;
      private final RigidBody rootBody;
      
      private final ArrayList<IMUMount> imuMounts;

      public QuaternionOrientationEstimatorEvaluatorFullRobotModel(QuaternionOrientationEstimatorEvaluatorRobot robot)
      {
         generator = new InverseDynamicsJointsFromSCSRobotGenerator(robot);
         elevator = generator.getElevator();
             
         rootInverseDynamicsJoint = generator.getRootSixDoFJoint();
         rootBody = generator.getRootBody();
         
         imuMounts = robot.getIMUMounts();
      }

      public SixDoFJoint getRootInverseDynamicsJoint()
      {
         return rootInverseDynamicsJoint;
      }

      public RigidBody getRootBody()
      {
         return rootBody;
      }
      
      public ArrayList<IMUMount> getIMUMounts()
      {
         return imuMounts;
      }
      
      public RigidBody getIMUBody(IMUMount imuMount)
      {
         return generator.getRigidBody(imuMount.getParentJoint());
      }

      public void updateBasedOnRobot(QuaternionOrientationEstimatorEvaluatorRobot robot)
      {
         generator.updateInverseDynamicsRobotModelFromRobot(true);
      }

      public void updateBasedOnEstimator(OrientationEstimator estimator)
      {
         FrameOrientation estimatedOrientation = estimator.getEstimatedOrientation();
         FrameVector estimatedAngularVelocity = estimator.getEstimatedAngularVelocity();

         updateBasedOnEstimator(estimatedOrientation, estimatedAngularVelocity);
      }

      public void updateBasedOnEstimator(FrameOrientation estimatedOrientation, FrameVector estimatedAngularVelocity)
      {
         rootInverseDynamicsJoint.setRotation(estimatedOrientation.getQuaternion());
         generator.updateInverseDynamicsRobotModelFromRobot(false);

         elevator.updateFramesRecursively();

         ReferenceFrame elevatorFrame = rootInverseDynamicsJoint.getFrameBeforeJoint();
         ReferenceFrame bodyFrame = rootInverseDynamicsJoint.getFrameAfterJoint();

         estimatedAngularVelocity.changeFrame(bodyFrame);

         Twist bodyTwist = new Twist(bodyFrame, elevatorFrame, bodyFrame);
         bodyTwist.setAngularPart(estimatedAngularVelocity.getVector());
         rootInverseDynamicsJoint.setJointTwist(bodyTwist);
      }
   }


   private class AngularAccelerationFromRobotStealer extends AbstractControlFlowElement
   {
      private final QuaternionOrientationEstimatorEvaluatorRobot robot;
      private final ControlFlowOutputPort<FrameVector> outputPort;
      private final FrameVector desiredAngularAcceleration;

      public AngularAccelerationFromRobotStealer(QuaternionOrientationEstimatorEvaluatorRobot robot, ReferenceFrame referenceFrame)
      {
         this.robot = robot;

         this.desiredAngularAcceleration = new FrameVector(referenceFrame);
         this.outputPort = createOutputPort();
      }

      public void startComputation()
      {
         desiredAngularAcceleration.set(robot.getActualAngularAccelerationInBodyFrame());

         outputPort.setData(desiredAngularAcceleration);
      }

      public ControlFlowOutputPort<FrameVector> getOutputPort()
      {
         return outputPort;
      }

      public void waitUntilComputationIsDone()
      {
      }

   }


   private class QuaternionOrientationEstimatorEvaluatorController implements RobotController
   {
      private final YoVariableRegistry registry = new YoVariableRegistry("QuaternionOrientationEstimatorEvaluatorController");

      private final DoubleYoVariable orientationErrorAngle = new DoubleYoVariable("orientationErrorAngle", registry);
      
      private final QuaternionOrientationEstimatorEvaluatorRobot robot;

      private final QuaternionOrientationEstimatorEvaluatorFullRobotModel perfectFullRobotModel;
      private final TwistCalculator perfectTwistCalculator;

      private final QuaternionOrientationEstimatorEvaluatorFullRobotModel estimatedFullRobotModel;
      private final TwistCalculator estimatedTwistCalculator;

      private final ControlFlowGraph controlFlowGraph;
      private final OrientationEstimator orientationEstimator;

      public QuaternionOrientationEstimatorEvaluatorController(QuaternionOrientationEstimatorEvaluatorRobot robot, double controlDT)
      {
         this.robot = robot;

         perfectFullRobotModel = new QuaternionOrientationEstimatorEvaluatorFullRobotModel(robot);
         perfectTwistCalculator = new TwistCalculator(ReferenceFrame.getWorldFrame(), perfectFullRobotModel.getRootBody());
         estimatedFullRobotModel = new QuaternionOrientationEstimatorEvaluatorFullRobotModel(robot);
         estimatedTwistCalculator = new TwistCalculator(ReferenceFrame.getWorldFrame(), estimatedFullRobotModel.getRootBody());

         OldOrientationSensorConfiguration<ControlFlowOutputPort<Matrix3d>> orientationSensors = createOrientationSensors(perfectFullRobotModel,
                                                                                                 estimatedFullRobotModel);
         OldAngularVelocitySensorConfiguration<ControlFlowOutputPort<Vector3d>> angularVelocitySensors = createAngularVelocitySensors(perfectFullRobotModel,
                                                                                                         estimatedFullRobotModel);

         controlFlowGraph = new ControlFlowGraph();
//         ReferenceFrame estimationFrame = estimatedFullRobotModel.getRootInverseDynamicsJoint().getFrameAfterJoint();
         RigidBody estimationLink = estimatedFullRobotModel.getRootBody();
         ReferenceFrame estimationFrame = estimationLink.getParentJoint().getFrameAfterJoint();
         
         DenseMatrix64F angularAccelerationNoiseCovariance = createDiagonalCovarianceMatrix(angularAccelerationProcessNoiseStandardDeviation, 3);

         ControlFlowOutputPort<FrameVector> angularAccelerationOutputPort = null;
         if (USE_ANGULAR_ACCELERATION_INPUT)
         {
            AngularAccelerationFromRobotStealer angularAccelerationFromRobotStealer = new AngularAccelerationFromRobotStealer(robot, estimationFrame);
            angularAccelerationOutputPort = angularAccelerationFromRobotStealer.getOutputPort();
         }
         
         if (USE_COMPOSABLE_ESTIMATOR)
         {
            ComposableOrientationEstimatorCreator orientationEstimatorCreator = new ComposableOrientationEstimatorCreator(angularAccelerationNoiseCovariance, estimationLink, estimatedTwistCalculator);
            orientationEstimatorCreator.addOrientationSensorConfigurations(orientationSensors);
            orientationEstimatorCreator.addAngularVelocitySensorConfigurations(angularVelocitySensors);
            orientationEstimator = orientationEstimatorCreator.createOrientationEstimator(controlFlowGraph, controlDT, estimationFrame, angularAccelerationOutputPort, registry);
         }
         else
         {
            orientationEstimator = new QuaternionOrientationEstimator(controlFlowGraph, "orientationEstimator", orientationSensors, angularVelocitySensors,
                  angularAccelerationOutputPort, estimationLink, estimationFrame, estimatedTwistCalculator, controlDT, angularAccelerationNoiseCovariance, registry);            
         }

         controlFlowGraph.initializeAfterConnections();


         if (INITIALIZE_ANGULAR_VELOCITY_ESTIMATE_TO_ACTUAL)
         {
            robot.update();
            estimatedFullRobotModel.updateBasedOnRobot(robot);
            
            Matrix3d rotationMatrix = new Matrix3d();
            robot.getRootJoint().getRotationToWorld(rotationMatrix);
            Vector3d angularVelocityInBody = robot.getRootJoint().getAngularVelocityInBody();

            FrameOrientation estimatedOrientation = orientationEstimator.getEstimatedOrientation();
            estimatedOrientation.set(rotationMatrix);
            orientationEstimator.setEstimatedOrientation(estimatedOrientation);
            
            FrameVector estimatedAngularVelocity = orientationEstimator.getEstimatedAngularVelocity();
            estimatedAngularVelocity.set(angularVelocityInBody);
            orientationEstimator.setEstimatedAngularVelocity(estimatedAngularVelocity);
            
            //TODO: This wasn't doing anything. 
//            DenseMatrix64F x = orientationEstimator.getState();
//            MatrixTools.insertTuple3dIntoEJMLVector(angularVelocityInBody, x, 3);
//            orientationEstimator.setState(x, orientationEstimator.getCovariance());
            
            System.out.println("Estimated orientation = " + orientationEstimator.getEstimatedOrientation());
            System.out.println("Estimated angular velocity = " + estimatedAngularVelocity);
         }

      }

      private OldAngularVelocitySensorConfiguration<ControlFlowOutputPort<Vector3d>> createAngularVelocitySensors(
              QuaternionOrientationEstimatorEvaluatorFullRobotModel perfectFullRobotModel,
              QuaternionOrientationEstimatorEvaluatorFullRobotModel estimatedFullRobotModel)
      {
         OldAngularVelocitySensorConfiguration<ControlFlowOutputPort<Vector3d>> angularVelocitySensorConfiguration =
            new OldAngularVelocitySensorConfiguration<ControlFlowOutputPort<Vector3d>>();

         if (CREATE_ANGULAR_VELOCITY_SENSOR)
         {
            for (IMUMount imuMount : perfectFullRobotModel.getIMUMounts())
            {
               RigidBody perfectBody = perfectFullRobotModel.getIMUBody(imuMount);
               RigidBody estimatedBody = estimatedFullRobotModel.getIMUBody(imuMount);
               
               ReferenceFrame frameUsedForPerfectMeasurement = perfectBody.getParentJoint().getFrameAfterJoint();
               String sensorName = imuMount.getName() + "AngularVelocity";
                     
               SimulatedAngularVelocitySensor angularVelocitySensor = new SimulatedAngularVelocitySensor(sensorName, perfectTwistCalculator,
                     perfectBody, frameUsedForPerfectMeasurement, registry);
               GaussianVectorCorruptor angularVelocityCorruptor = new GaussianVectorCorruptor(1235L, sensorName, registry);
               angularVelocityCorruptor.setStandardDeviation(angularVelocityMeasurementStandardDeviation);
               angularVelocitySensor.addSignalCorruptor(angularVelocityCorruptor);

               BiasVectorCorruptor biasVectorCorruptor = new BiasVectorCorruptor(1236L, sensorName, controlDT, registry);
               biasVectorCorruptor.setStandardDeviation(angularVelocityBiasProcessNoiseStandardDeviation);
               biasVectorCorruptor.setBias(new Vector3d(0.0, 0.0, 0.0));
               angularVelocitySensor.addSignalCorruptor(biasVectorCorruptor);


               DenseMatrix64F angularVelocityCovarianceMatrix = createDiagonalCovarianceMatrix(angularVelocityMeasurementStandardDeviation, 3);
               DenseMatrix64F angularVelocityBiasNoiseCovariance = createDiagonalCovarianceMatrix(angularVelocityBiasProcessNoiseStandardDeviation, 3);

               ReferenceFrame measurementFrame = estimatedBody.getParentJoint().getFrameAfterJoint();

               angularVelocitySensorConfiguration.addSensor(angularVelocitySensor.getAngularVelocityOutputPort(), measurementFrame, estimatedBody,
                     sensorName, angularVelocityCovarianceMatrix, angularVelocityBiasNoiseCovariance);
            }
         }

         return angularVelocitySensorConfiguration;
      }

      private OldOrientationSensorConfiguration<ControlFlowOutputPort<Matrix3d>> createOrientationSensors(
              QuaternionOrientationEstimatorEvaluatorFullRobotModel perfectFullRobotModel,
              QuaternionOrientationEstimatorEvaluatorFullRobotModel estimatedFullRobotModel)
      {
         OldOrientationSensorConfiguration<ControlFlowOutputPort<Matrix3d>> orientationSensorConfiguration =
            new OldOrientationSensorConfiguration<ControlFlowOutputPort<Matrix3d>>();

         if (CREATE_ORIENTATION_SENSOR)
         {
            for (IMUMount imuMount : perfectFullRobotModel.getIMUMounts())
            {
               RigidBody perfectBody = perfectFullRobotModel.getIMUBody(imuMount);
               RigidBody estimatedBody = estimatedFullRobotModel.getIMUBody(imuMount);

               ReferenceFrame frameUsedForPerfectMeasurement = perfectBody.getParentJoint().getFrameAfterJoint();
               String sensorName = imuMount.getName() + "Orientation";

               SimulatedOrientationSensor sensor = new SimulatedOrientationSensor(sensorName, frameUsedForPerfectMeasurement, registry);
               GaussianOrientationCorruptor orientationCorruptor = new GaussianOrientationCorruptor(sensorName, 12345L, registry);
               orientationCorruptor.setStandardDeviation(orientationMeasurementStandardDeviation);
               sensor.addSignalCorruptor(orientationCorruptor);

               DenseMatrix64F orientationCovarianceMatrix = createDiagonalCovarianceMatrix(orientationMeasurementStandardDeviation, 3);

               ReferenceFrame measurementFrame = estimatedBody.getParentJoint().getFrameAfterJoint();

               orientationSensorConfiguration.addSensor(sensor.getOrientationOutputPort(), measurementFrame, sensorName, orientationCovarianceMatrix);
            }
         }

         return orientationSensorConfiguration;
      }

      public void initialize()
      {
      }

      public YoVariableRegistry getYoVariableRegistry()
      {
         return registry;
      }

      public String getName()
      {
         return registry.getName();
      }

      public String getDescription()
      {
         return getName();
      }

      public void doControl()
      {
         perfectFullRobotModel.updateBasedOnRobot(robot);
         perfectTwistCalculator.compute();
         controlFlowGraph.startComputation();
         controlFlowGraph.waitUntilComputationIsDone();
         
         estimatedFullRobotModel.updateBasedOnEstimator(orientationEstimator);
         estimatedFullRobotModel.updateBasedOnRobot(robot);
         
         // TODO: set revolute joint positions and velocities
         estimatedTwistCalculator.compute();
         
         computeOrientationErrorAngle();
      }

      private void computeOrientationErrorAngle()
      {
         FrameOrientation estimatedOrientation = orientationEstimator.getEstimatedOrientation();
         Quat4d estimatedOrientationQuat4d = new Quat4d();
         estimatedOrientation.getQuaternion(estimatedOrientationQuat4d);
         
         Quat4d orientationErrorQuat4d = new Quat4d(robot.getActualOrientation());
         orientationErrorQuat4d.mulInverse(estimatedOrientationQuat4d);
         
         AxisAngle4d orientationErrorAxisAngle = new AxisAngle4d();
         orientationErrorAxisAngle.set(orientationErrorQuat4d);
         
         double errorAngle = orientationErrorAxisAngle.getAngle();
         if (errorAngle > Math.PI) errorAngle = errorAngle - 2.0 * Math.PI;
         if (errorAngle < -Math.PI) errorAngle = errorAngle + 2.0 * Math.PI;
         
         orientationErrorAngle.set(errorAngle);
      }
   }


   private static DenseMatrix64F createDiagonalCovarianceMatrix(double standardDeviation, int size)
   {
      DenseMatrix64F orientationCovarianceMatrix = new DenseMatrix64F(size, size);
      CommonOps.setIdentity(orientationCovarianceMatrix);
      CommonOps.scale(MathTools.square(standardDeviation), orientationCovarianceMatrix);

      return orientationCovarianceMatrix;
   }

   public static void main(String[] args)
   {
      new QuaternionOrientationEstimatorEvaluator();
   }
}
