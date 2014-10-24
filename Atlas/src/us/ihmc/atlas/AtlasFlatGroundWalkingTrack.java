package us.ihmc.atlas;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.darpaRoboticsChallenge.DRCFlatGroundWalkingTrack;
import us.ihmc.darpaRoboticsChallenge.DRCGuiInitialSetup;
import us.ihmc.darpaRoboticsChallenge.DRCSCSInitialSetup;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;
import us.ihmc.darpaRoboticsChallenge.util.OscillateFeetPerturber;
import us.ihmc.graphics3DAdapter.GroundProfile3D;
import us.ihmc.utilities.robotSide.RobotSide;

import com.martiansoftware.jsap.JSAPException;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.util.ground.BumpyGroundProfile;
import com.yobotics.simulationconstructionset.util.ground.FlatGroundProfile;

public class AtlasFlatGroundWalkingTrack
{
   private static final DRCRobotModel defaultModelForGraphicSelector = new AtlasRobotModel(AtlasRobotVersion.DRC_NO_HANDS, AtlasRobotModel.AtlasTarget.SIM, false);

   private static final boolean USE_BUMPY_GROUND = false;
   private static final boolean USE_FEET_PERTURBER = false;

   public static void main(String[] args) throws JSAPException
   {

      DRCRobotModel model = null;
      model = AtlasRobotModelFactory.selectSimulationModelFromFlag(args);

      if (model == null)
         model = AtlasRobotModelFactory.selectModelFromGraphicSelector(defaultModelForGraphicSelector);

      if (model == null)
         throw new RuntimeException("No robot model selected");

      DRCGuiInitialSetup guiInitialSetup = new DRCGuiInitialSetup(true, false);

      final double groundHeight = 0.0;
      GroundProfile3D groundProfile;
      if (USE_BUMPY_GROUND)
      {
         groundProfile = createBumpyGroundProfile();
      }
      else
      {
         groundProfile = new FlatGroundProfile(groundHeight);
      }

      DRCSCSInitialSetup scsInitialSetup = new DRCSCSInitialSetup(groundProfile, model.getSimulateDT());
      scsInitialSetup.setDrawGroundProfile(true);
      scsInitialSetup.setInitializeEstimatorToActual(true);

      double initialYaw = 0.3;
      DRCRobotInitialSetup<SDFRobot> robotInitialSetup = model.getDefaultRobotInitialSetup(groundHeight, initialYaw);

      boolean useVelocityAndHeadingScript = true;
      boolean cheatWithGroundHeightAtForFootstep = false;

      DRCFlatGroundWalkingTrack drcFlatGroundWalkingTrack = new DRCFlatGroundWalkingTrack(robotInitialSetup, guiInitialSetup, scsInitialSetup,
            useVelocityAndHeadingScript, cheatWithGroundHeightAtForFootstep, model);

      if (USE_FEET_PERTURBER)
         createOscillateFeetPerturber(drcFlatGroundWalkingTrack);
   }

   private static void createOscillateFeetPerturber(DRCFlatGroundWalkingTrack drcFlatGroundWalkingTrack)
   {
      SimulationConstructionSet simulationConstructionSet = drcFlatGroundWalkingTrack.getSimulationConstructionSet();
      SDFRobot robot = drcFlatGroundWalkingTrack.getDrcSimulation().getRobot();

      int ticksPerPerturbation = 10;
      OscillateFeetPerturber oscillateFeetPerturber = new OscillateFeetPerturber(robot, simulationConstructionSet.getDT() * ((double) ticksPerPerturbation));
      oscillateFeetPerturber.setTranslationMagnitude(new double[] { 0.01, 0.015, 0.005 });
      oscillateFeetPerturber.setRotationMagnitudeYawPitchRoll(new double[] { 0.017, 0.012, 0.011 });

      oscillateFeetPerturber.setTranslationFrequencyHz(RobotSide.LEFT, new double[] { 0.0, 0, 3.3 });
      oscillateFeetPerturber.setTranslationFrequencyHz(RobotSide.RIGHT, new double[] { 0.0, 0, 1.3 });

      oscillateFeetPerturber.setRotationFrequencyHzYawPitchRoll(RobotSide.LEFT, new double[] { 0.0, 0, 7.3 });
      oscillateFeetPerturber.setRotationFrequencyHzYawPitchRoll(RobotSide.RIGHT, new double[] { 0., 0, 1.11 });

      robot.setController(oscillateFeetPerturber, ticksPerPerturbation);
   }

   private static BumpyGroundProfile createBumpyGroundProfile()
   {
      double xAmp1 = 0.05, xFreq1 = 0.5, xAmp2 = 0.01, xFreq2 = 0.5;
      double yAmp1 = 0.01, yFreq1 = 0.07, yAmp2 = 0.05, yFreq2 = 0.37;
      BumpyGroundProfile groundProfile = new BumpyGroundProfile(xAmp1, xFreq1, xAmp2, xFreq2, yAmp1, yFreq1, yAmp2, yFreq2);
      return groundProfile;
   }
}
