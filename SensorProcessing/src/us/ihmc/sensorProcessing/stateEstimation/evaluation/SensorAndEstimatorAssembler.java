package us.ihmc.sensorProcessing.stateEstimation.evaluation;

import java.util.Collection;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.controlFlow.ControlFlowGraph;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.sensorProcessing.simulatedSensors.JointAndIMUSensorMap;
import us.ihmc.sensorProcessing.simulatedSensors.SensorFilterParameters;
import us.ihmc.sensorProcessing.simulatedSensors.SensorNoiseParameters;
import us.ihmc.sensorProcessing.simulatedSensors.StateEstimatorSensorDefinitions;
import us.ihmc.sensorProcessing.stateEstimation.ComposableOrientationAndCoMEstimatorCreator;
import us.ihmc.sensorProcessing.stateEstimation.IMUSelectorAndDataConverter;
import us.ihmc.sensorProcessing.stateEstimation.JointAndIMUSensorDataSource;
import us.ihmc.sensorProcessing.stateEstimation.JointStateFullRobotModelUpdater;
import us.ihmc.sensorProcessing.stateEstimation.OrientationStateRobotModelUpdater;
import us.ihmc.sensorProcessing.stateEstimation.PointMeasurementNoiseParameters;
import us.ihmc.sensorProcessing.stateEstimation.SimplePelvisStateEstimatorRobotModelUpdater;
import us.ihmc.sensorProcessing.stateEstimation.SimplePositionStateCalculatorInterface;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimationDataFromController;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorWithPorts;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.AngularVelocitySensorConfiguration;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.LinearAccelerationSensorConfiguration;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.OrientationSensorConfiguration;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.SensorConfigurationFactory;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.AfterJointReferenceFrameNameMap;
import us.ihmc.utilities.screwTheory.RigidBody;

import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class SensorAndEstimatorAssembler
{
   private static final boolean VISUALIZE_CONTROL_FLOW_GRAPH = false; //false;
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final ControlFlowGraph controlFlowGraph;

   // The following are the elements added to the controlFlowGraph:
   private final JointAndIMUSensorDataSource jointSensorDataSource;
   @SuppressWarnings("unused")
   private final StateEstimationDataFromController stateEstimatorDataFromControllerSource;
   private final JointStateFullRobotModelUpdater jointStateFullRobotModelUpdater;
   private final ComposableOrientationAndCoMEstimatorCreator.ComposableOrientationAndCoMEstimator fancyEstimator;
   private final SimplePelvisStateEstimatorRobotModelUpdater simpleEstimator;
   private final OrientationStateRobotModelUpdater orientationStateRobotModelUpdater;
   private final IMUSelectorAndDataConverter imuSelectorAndDataConverter;
   
   private final boolean useSimplePelvisPositionEstimator;

   public SensorAndEstimatorAssembler(StateEstimationDataFromController stateEstimatorDataFromControllerSource,
         StateEstimatorSensorDefinitions stateEstimatorSensorDefinitions, SensorNoiseParameters sensorNoiseParametersForEstimator,
         SensorFilterParameters sensorFilterParameters, PointMeasurementNoiseParameters pointMeasurementNoiseParameters,
         double gravitationalAcceleration, FullInverseDynamicsStructure inverseDynamicsStructure, AfterJointReferenceFrameNameMap estimatorReferenceFrameMap,
         RigidBodyToIndexMap estimatorRigidBodyToIndexMap, double controlDT, boolean assumePerfectIMU, boolean useSimplePelvisPositionEstimator, SimplePositionStateCalculatorInterface simplePositionStateRobotModelUpdater, YoVariableRegistry parentRegistry)
   {

      if (useSimplePelvisPositionEstimator && !assumePerfectIMU)
         throw new RuntimeException("ASSUME_PERFECT_IMU should be true if USE_SIMPLE_COM_ESTIMATOR is true.");
      
      this.useSimplePelvisPositionEstimator = useSimplePelvisPositionEstimator;
      
      this.stateEstimatorDataFromControllerSource = stateEstimatorDataFromControllerSource;
      SensorConfigurationFactory sensorConfigurationFactory = new SensorConfigurationFactory(sensorNoiseParametersForEstimator, gravitationalAcceleration);

      jointSensorDataSource = new JointAndIMUSensorDataSource(stateEstimatorSensorDefinitions, sensorFilterParameters, registry);
      JointAndIMUSensorMap jointAndIMUSensorMap = jointSensorDataSource.getSensorMap();

      ReferenceFrame estimationFrame = inverseDynamicsStructure.getEstimationFrame();

      // Sensor configurations for estimator
      Collection<OrientationSensorConfiguration> orientationSensorConfigurations = sensorConfigurationFactory
            .createOrientationSensorConfigurations(jointAndIMUSensorMap.getOrientationSensors());

      
      Collection<AngularVelocitySensorConfiguration> angularVelocitySensorConfigurations = sensorConfigurationFactory
            .createAngularVelocitySensorConfigurations(jointAndIMUSensorMap.getAngularVelocitySensors());

      Collection<LinearAccelerationSensorConfiguration> linearAccelerationSensorConfigurations = sensorConfigurationFactory
            .createLinearAccelerationSensorConfigurations(jointAndIMUSensorMap.getLinearAccelerationSensors());

      controlFlowGraph = new ControlFlowGraph();
      jointStateFullRobotModelUpdater = new JointStateFullRobotModelUpdater(controlFlowGraph, jointAndIMUSensorMap, inverseDynamicsStructure);

      ControlFlowOutputPort<FullInverseDynamicsStructure> inverseDynamicsStructureOutputPort = null;
     
      if (!assumePerfectIMU)
      {
    	  imuSelectorAndDataConverter = null;
    	  orientationStateRobotModelUpdater = null;
    	  inverseDynamicsStructureOutputPort = jointStateFullRobotModelUpdater.getInverseDynamicsStructureOutputPort();
      }
      else
      {
         imuSelectorAndDataConverter = new IMUSelectorAndDataConverter(controlFlowGraph, orientationSensorConfigurations, angularVelocitySensorConfigurations, jointStateFullRobotModelUpdater.getInverseDynamicsStructureOutputPort(), registry);
       
         orientationStateRobotModelUpdater = new OrientationStateRobotModelUpdater(controlFlowGraph,
        		 imuSelectorAndDataConverter.getInverseDynamicsStructureOutputPort(), imuSelectorAndDataConverter.getOrientationOutputPort(),
        		 imuSelectorAndDataConverter.getAngularVelocityOutputPort());
         
         inverseDynamicsStructureOutputPort = orientationStateRobotModelUpdater.getInverseDynamicsStructureOutputPort();
      }

      double angularAccelerationProcessNoiseStandardDeviation = sensorNoiseParametersForEstimator.getAngularAccelerationProcessNoiseStandardDeviation();
      DenseMatrix64F angularAccelerationNoiseCovariance = createDiagonalCovarianceMatrix(angularAccelerationProcessNoiseStandardDeviation, 3);

      RigidBody estimationLink = inverseDynamicsStructure.getEstimationLink();

      double comAccelerationProcessNoiseStandardDeviation = sensorNoiseParametersForEstimator.getComAccelerationProcessNoiseStandardDeviation();
      DenseMatrix64F comAccelerationNoiseCovariance = createDiagonalCovarianceMatrix(comAccelerationProcessNoiseStandardDeviation, 3);

      if (!useSimplePelvisPositionEstimator)
      {
         ComposableOrientationAndCoMEstimatorCreator orientationAndCoMEstimatorCreator = new ComposableOrientationAndCoMEstimatorCreator(
               pointMeasurementNoiseParameters, angularAccelerationNoiseCovariance, comAccelerationNoiseCovariance, estimationLink,
               inverseDynamicsStructureOutputPort, assumePerfectIMU);

         if (!assumePerfectIMU)
         {
            orientationAndCoMEstimatorCreator.addOrientationSensorConfigurations(orientationSensorConfigurations);
            orientationAndCoMEstimatorCreator.addAngularVelocitySensorConfigurations(angularVelocitySensorConfigurations);
            orientationAndCoMEstimatorCreator.addLinearAccelerationSensorConfigurations(linearAccelerationSensorConfigurations);
         }

         // TODO: Not sure if we need to do this here:
         inverseDynamicsStructure.updateInternalState();

         fancyEstimator = orientationAndCoMEstimatorCreator.createOrientationAndCoMEstimator(controlFlowGraph, controlDT, estimationFrame, estimatorReferenceFrameMap,
               estimatorRigidBodyToIndexMap, registry);
         stateEstimatorDataFromControllerSource.connectDesiredAccelerationPorts(controlFlowGraph, fancyEstimator);
         fancyEstimator.initialize();
         
         simpleEstimator = null;
      }
      else
      {
         fancyEstimator = null;
         simpleEstimator = new SimplePelvisStateEstimatorRobotModelUpdater("simpleCoMEstimator", controlDT, estimationFrame, estimatorReferenceFrameMap, estimatorRigidBodyToIndexMap,
               controlFlowGraph, inverseDynamicsStructureOutputPort, simplePositionStateRobotModelUpdater, registry, assumePerfectIMU);
         simpleEstimator.initialize();
      }
      
      parentRegistry.addChild(registry);

      controlFlowGraph.initializeAfterConnections();
      controlFlowGraph.startComputation();
      controlFlowGraph.waitUntilComputationIsDone();

      if (VISUALIZE_CONTROL_FLOW_GRAPH)
      {
         controlFlowGraph.visualize();
      }
   }

   private static DenseMatrix64F createDiagonalCovarianceMatrix(double standardDeviation, int size)
   {
      DenseMatrix64F orientationCovarianceMatrix = new DenseMatrix64F(size, size);
      CommonOps.setIdentity(orientationCovarianceMatrix);
      CommonOps.scale(MathTools.square(standardDeviation), orientationCovarianceMatrix);

      return orientationCovarianceMatrix;
   }

   public ControlFlowGraph getControlFlowGraph()
   {
      return controlFlowGraph;
   }

   public StateEstimatorWithPorts getEstimator()
   {
      if (!useSimplePelvisPositionEstimator)
         return fancyEstimator;
      else
         return simpleEstimator;
   }

   public JointAndIMUSensorDataSource getJointAndIMUSensorDataSource()
   {
      return jointSensorDataSource;
   }

}
