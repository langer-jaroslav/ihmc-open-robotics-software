package us.ihmc.darpaRoboticsChallenge.environment;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.GroundProfile3D;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.simulationconstructionset.ExternalForcePoint;
import us.ihmc.simulationconstructionset.GroundContactModel;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.robotController.ContactController;
import us.ihmc.simulationconstructionset.util.LinearStickSlipGroundContactModel;
import us.ihmc.simulationconstructionset.util.environments.ContactableSelectableBoxRobot;
import us.ihmc.simulationconstructionset.util.environments.SelectableObjectListener;
import us.ihmc.simulationconstructionset.util.ground.CombinedTerrainObject3D;
import us.ihmc.simulationconstructionset.util.ground.TerrainObject3D;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.math.geometry.RotationFunctions;

public class DRCDebrisEnvironment implements CommonAvatarEnvironmentInterface
{
   private final CombinedTerrainObject3D combinedTerrainObject;
   private final ArrayList<ExternalForcePoint> contactPoints = new ArrayList<ExternalForcePoint>();
   private final List<ContactableSelectableBoxRobot> debrisRobots = new ArrayList<ContactableSelectableBoxRobot>();

   private final String debrisName = "Debris";

   private final double debrisDepth = 0.0508;
   private final double debrisWidth = 0.1016;
   private final double debrisLength = 0.9144;
   private final double debrisMass = 1.0;

   public DRCDebrisEnvironment()
   {
      double forceVectorScale = 1.0 / 50.0;
      combinedTerrainObject = new CombinedTerrainObject3D(getClass().getSimpleName());

      combinedTerrainObject.addTerrainObject(setUpGround("Ground"));
      
      combinedTerrainObject.addTerrainObject(createSupports(forceVectorScale));

      createBoxes(forceVectorScale, combinedTerrainObject);
      
   }

   private CombinedTerrainObject3D createSupports(double forceVectorScale)
   {
      CombinedTerrainObject3D combinedTerrainObject3D = new CombinedTerrainObject3D("Supports");

//      debrisRobots.add(createDebris(2.0, 0.1, 0.5, Math.toRadians(90.0), Math.toRadians(90.0), Math.toRadians(10.0)));
      combinedTerrainObject3D.addBox(2.0, 0.6, 2.2, 0.41 , 0.67, YoAppearance.Chocolate());
      combinedTerrainObject3D.addBox(1.8, -0.44, 2.2, -0.6 , 0.67, YoAppearance.Yellow());
      
      Quat4d quat = new Quat4d();
      RotationFunctions.setQuaternionBasedOnYawPitchRoll(quat, Math.toRadians(12), 0.0, 0.0);
      Vector3d vector = new Vector3d(3.29,0.3,0.6);
      
      RigidBodyTransform configuration = new RigidBodyTransform(quat, vector);
      combinedTerrainObject3D.addRotatableBox(configuration , 0.2, 0.6, 1.2, YoAppearance.Brown());
      
      
      return combinedTerrainObject3D;
   }

   private CombinedTerrainObject3D setUpGround(String name)
   {
      CombinedTerrainObject3D combinedTerrainObject = new CombinedTerrainObject3D(name);

      combinedTerrainObject.addBox(-10.0, -10.0, 10.0, 10.0, -0.05, 0.0, YoAppearance.DarkBlue());
      
      return combinedTerrainObject;
   }

   private void createBoxes(double forceVectorScale, GroundProfile3D groundProfile)
   {
      debrisRobots.add(createDebris(1.0, -0.4, debrisLength / 2.0, 0.0, 0.0, 0.0));
      debrisRobots.add(createDebris(2.0, 0.0, 0.7, Math.toRadians(90.0), Math.toRadians(90.0), Math.toRadians(10.0)));
      debrisRobots.add(createDebris(3.0, 0.3, debrisLength / 2.0 - 0.02, Math.toRadians(12.0), Math.toRadians(20.0), 0.0));

      for (int i = 0; i < debrisRobots.size(); i++)
      {
         ContactableSelectableBoxRobot debrisRobot = debrisRobots.get(i);
         GroundContactModel groundContactModel = createGroundContactModel(debrisRobot, groundProfile);
         debrisRobot.createAvailableContactPoints(1, 15, forceVectorScale, false);
         debrisRobot.setGroundContactModel(groundContactModel);
      }
   }

   private int id = 0;

   public ContactableSelectableBoxRobot createDebris(double x, double y, double z, double yaw, double pitch, double roll)
   {
      ContactableSelectableBoxRobot debris = ContactableSelectableBoxRobot.createContactable2By4Robot(debrisName + String.valueOf(id++), debrisDepth,
            debrisWidth, debrisLength, debrisMass);
      debris.setPosition(x, y, z);
      debris.setYawPitchRoll(yaw, pitch, roll);

      return debris;
   }

   private GroundContactModel createGroundContactModel(Robot robot, GroundProfile3D groundProfile)
   {
      double kXY = 5000.0;
      double bXY = 100.0;
      double kZ = 1000.0;
      double bZ = 100.0;
      double alphaStick = 0.7;
      double alphaSlip = 0.5;

      GroundContactModel groundContactModel = new LinearStickSlipGroundContactModel(robot, kXY, bXY, kZ, bZ, alphaSlip, alphaStick,
            robot.getRobotsYoVariableRegistry());
      groundContactModel.setGroundProfile3D(groundProfile);

      return groundContactModel;
   }

   @Override
   public TerrainObject3D getTerrainObject3D()
   {
      return combinedTerrainObject;
   }

   @Override
   public List<ContactableSelectableBoxRobot> getEnvironmentRobots()
   {
      return debrisRobots;
   }

   @Override
   public void createAndSetContactControllerToARobot()
   {
      // add contact controller to any robot so it gets called
      ContactController contactController = new ContactController();
      contactController.setContactParameters(10000.0, 1000.0, 0.5, 0.3);
      contactController.addContactPoints(contactPoints);
      contactController.addContactables(debrisRobots);
      debrisRobots.get(0).setController(contactController);

      // add contact controller to any robot so it gets called
      //      ContactController contactController2 = new ContactController("2");
      //      contactController2.setContactParameters(10000.0, 1000.0, 0.5, 0.3);
      //      contactController2.addContactPoints(table.getAllGroundContactPoints());
      //      contactController2.addContactables(debrisRobots.subList(0, debrisRobots.size()-1));
      //      table.setController(contactController2);
   }

   @Override
   public void addContactPoints(List<? extends ExternalForcePoint> externalForcePoints)
   {
      this.contactPoints.addAll(externalForcePoints);
   }

   @Override
   public void addSelectableListenerToSelectables(SelectableObjectListener selectedListener)
   {
   }
}
