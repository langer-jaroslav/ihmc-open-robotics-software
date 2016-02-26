package us.ihmc.robotics.screwTheory;

import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.kinematics.fourbar.FourBarCalculatorFromFastRunner;

public class FourBarKinematicLoop
{
   /*
    * Representation of the four bar with name correspondences:
    * 
    * masterL masterJ--------J1 |\ /| | \ / | L3| \ / |L1 | \/ | | /\ | | / \ |
    * | / \ | |/ \| J3--------J2 L2
    */

   // private boolean prjIsPartOf4Bar = true;
   private final RevoluteJoint masterJoint;
   private final PassiveRevoluteJoint passiveJoint1, passiveJoint2, passiveJoint3;
   private final String name;

   private final DoubleYoVariable masterJointQ;
   private final DoubleYoVariable masterJointQd;

   private final FramePoint masterJointPosition, joint1Position, joint2Position, joint3Position;
   private double masterL, L1, L2, L3;

   private FourBarCalculatorFromFastRunner fourBarCalculator;
   private double offsetAngle1, offsetAngle2, offsetAngle3;

   private double maxValidMasterJointAngle, minValidMasterJointAngle;

   public FourBarKinematicLoop(String name, YoVariableRegistry registry, RevoluteJoint masterJoint, PassiveRevoluteJoint passiveJoint1, PassiveRevoluteJoint passiveJoint2, PassiveRevoluteJoint passiveJoint3)
   {
      this(name, registry, masterJoint, passiveJoint1, passiveJoint2, passiveJoint3, 0.0, 0.0, 0.0);
   }

   public FourBarKinematicLoop(String name, YoVariableRegistry registry, RevoluteJoint masterJoint, PassiveRevoluteJoint passiveJoint1, PassiveRevoluteJoint passiveJoint2, PassiveRevoluteJoint passiveJoint3, double offsetAngle1, double offsetAngle2,
         double offsetAngle3)
   {
      this.name = name;
      this.masterJoint = masterJoint;
      this.passiveJoint1 = passiveJoint1;
      this.passiveJoint2 = passiveJoint2;
      this.passiveJoint3 = passiveJoint3;

      joint1Position = new FramePoint();
      joint2Position = new FramePoint();
      joint3Position = new FramePoint();
      masterJointPosition = new FramePoint();

      masterJointQ = new DoubleYoVariable(name + "MasterJointQ", registry);
      masterJointQ.set(masterJoint.getQ());

      masterJointQd = new DoubleYoVariable(name + "MasterJointQd", registry);
      masterJointQd.set(masterJoint.getQd());

      this.offsetAngle1 = offsetAngle1;
      this.offsetAngle2 = offsetAngle2;
      this.offsetAngle3 = offsetAngle3;
   }

   private void checkCorrectJointOrder()
   {
      if (masterJoint.getSuccessor() != passiveJoint1.getPredecessor() || passiveJoint1.getSuccessor() != passiveJoint2.getPredecessor() || passiveJoint2.getSuccessor() != passiveJoint3.getPredecessor()
            || passiveJoint3.getSuccessor() != masterJoint.getPredecessor())
      {
         throw new RuntimeException("The joints that form the " + name + " four bar must be passed in clockwise or counterclockwise order");
      }
   }

   private void verifyMasterJointLimits()
   {
      if (masterJoint.getJointLimitLower() == Double.NEGATIVE_INFINITY || masterJoint.getJointLimitUpper() == Double.POSITIVE_INFINITY)
      {
         throw new RuntimeException("Must set the joint limits for the master joint of the " + name + " four bar");
      }

      if (L1 + masterL < L2 + L3)
      {
         maxValidMasterJointAngle = Math.acos((-(L2 + L3) * (L2 + L3) + masterL * masterL + L1 * L1) / (2 * masterL * L1));
         if (masterJoint.getJointLimitUpper() > maxValidMasterJointAngle)
         {
            throw new RuntimeException("The maximum valid joint angle for the master joint of the " + name + " four bar is " + maxValidMasterJointAngle + " to avoid flipping, but was set to " + masterJoint.getJointLimitUpper());
         }
      }
      else if (masterJoint.getJointLimitUpper() > Math.PI)
      {
         throw new RuntimeException("The maximum valid joint angle for the master joint of the " + name + " four bar is " + maxValidMasterJointAngle + " to avoid flipping, but was set to " + masterJoint.getJointLimitUpper());
      }
      
      if (L1 + L2 < masterL + L3)
      {
         minValidMasterJointAngle = Math.acos((-L3 * L3 + masterL * masterL + (L1 + L2) * (L1 + L2)) / (2 * masterL * (L1 + L2)));
         if(masterJoint.getJointLimitLower() < minValidMasterJointAngle)
         {
            throw new RuntimeException("The minimum valid joint angle for the master joint of the " + name + " four bar is " + minValidMasterJointAngle + " to avoid flipping, but was set to " + masterJoint.getJointLimitLower());
         }
      }   
      else if (masterJoint.getJointLimitLower() < 0.0)
      {
         throw new RuntimeException("The minimum valid joint angle for the master joint of the " + name + " four bar is " + minValidMasterJointAngle + " to avoid flipping, but was set to " + masterJoint.getJointLimitLower());
      }
   }

   public void initialize()
   {
      checkCorrectJointOrder();

      masterL = getLinkLength(masterJoint, passiveJoint3, joint1Position);
      L1 = getLinkLength(masterJoint, passiveJoint1, joint2Position);
      L2 = getLinkLength(passiveJoint1, passiveJoint2, joint3Position);
      L3 = getLinkLength(passiveJoint2, passiveJoint3, masterJointPosition);

      verifyMasterJointLimits();
      createCalculatorAndInitializePositionsAndVelocities();
   }

   public void initialize(double masterL, double L1, double L2, double L3)
   {
      this.masterL = masterL;
      this.L1 = L1;
      this.L2 = L2;
      this.L3 = L3;

      createCalculatorAndInitializePositionsAndVelocities();
   }

   private void createCalculatorAndInitializePositionsAndVelocities()
   {
      fourBarCalculator = new FourBarCalculatorFromFastRunner(masterL, L1, L2, L3);
      update();
   }

   public double getLinkLength(RevoluteJoint joint1, RevoluteJoint joint2, FramePoint positionJoint2)
   {
      positionJoint2.setToZero(joint2.getFrameBeforeJoint());
      positionJoint2.changeFrame(joint1.getFrameAfterJoint());
      return Math.sqrt(Math.pow(positionJoint2.getX(), 2) + Math.pow(positionJoint2.getY(), 2) + Math.pow(positionJoint2.getZ(), 2));
   }

   public void update()
   {
      fourBarCalculator.updateAnglesAndVelocitiesGivenAngleDAB(masterJoint.q, masterJoint.qd);
      passiveJoint1.updateQ(fourBarCalculator.getAngleABC() + offsetAngle1);
      passiveJoint2.updateQ(fourBarCalculator.getAngleBCD() + offsetAngle2);
      passiveJoint3.updateQ(fourBarCalculator.getAngleCDA() + offsetAngle3);
      passiveJoint1.updateQd(fourBarCalculator.getAngleDtABC());
      passiveJoint2.updateQd(fourBarCalculator.getAngleDtBCD());
      passiveJoint3.updateQd(fourBarCalculator.getAngleDtCDA());
   }
}
