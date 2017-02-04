package us.ihmc.exampleSimulations.collidingArms;

import javax.vecmath.Vector3d;

import us.ihmc.graphicsDescription.appearance.AppearanceDefinition;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.robotics.Axis;
import us.ihmc.robotics.robotDescription.CollisionMeshDescription;
import us.ihmc.robotics.robotDescription.LinkDescription;
import us.ihmc.robotics.robotDescription.LinkGraphicsDescription;
import us.ihmc.robotics.robotDescription.PinJointDescription;
import us.ihmc.robotics.robotDescription.RobotDescription;

public class CollidingArmRobotDescription extends RobotDescription
{
   private final double baseMass = 1.0;
   private final double baseRadius = 0.4;
   private final double baseHeight = 0.10;

   private final double baseRadiusOfGyrationX = baseRadius;
   private final double baseRadiusOfGyrationY = baseRadius;
   private final double baseRadiusOfGyrationZ = baseHeight;

   private final double shoulderMotorWidth = 0.16;
   private final double shoulderMotorRadius = 0.1;
   private final double shoulderOffsetZ = baseHeight + shoulderMotorRadius;

   private final double upperArmMass = 1.0;
   private final double upperArmRadius = 0.05;
   private final double upperArmLength = 0.8;

   private final double upperArmRadiusOfGyrationX = upperArmRadius;
   private final double upperArmRadiusOfGyrationY = upperArmRadius;
   private final double upperArmRadiusOfGyrationZ = upperArmLength;

   private final double elbowMotorWidth = 0.10;
   private final double elbowMotorRadius = 0.05;

   private final double lowerArmMass = 1.0;
   private final double lowerArmRadius = 0.03;
   private final double lowerArmLength = 0.6;

   private final double lowerArmRadiusOfGyrationX = lowerArmRadius;
   private final double lowerArmRadiusOfGyrationY = lowerArmRadius;
   private final double lowerArmRadiusOfGyrationZ = lowerArmLength;

   private final double handRadius = 0.12;

   public CollidingArmRobotDescription(String name, Vector3d baseOffset)
   {
      super(name);

      // Base:
      PinJointDescription baseJoint = new PinJointDescription("base", baseOffset, Axis.Z);
      LinkDescription baseLink = new LinkDescription("baseLink");
      baseLink.setMassAndRadiiOfGyration(baseMass, baseRadiusOfGyrationX, baseRadiusOfGyrationY, baseRadiusOfGyrationZ);

      LinkGraphicsDescription baseLinkGraphics = new LinkGraphicsDescription();
      AppearanceDefinition baseAppearance = YoAppearance.AliceBlue();
      baseLinkGraphics.addCylinder(baseHeight, baseRadius, baseAppearance);
      baseLinkGraphics.translate(new Vector3d(0.0, 0.0, shoulderOffsetZ));
      baseLinkGraphics.rotate(Math.PI / 2.0, Axis.X);
      baseLinkGraphics.translate(0.0, 0.0, -shoulderMotorWidth / 2.0);
      baseLinkGraphics.addCylinder(shoulderMotorWidth, shoulderMotorRadius, YoAppearance.Black());
      baseLink.setLinkGraphics(baseLinkGraphics);

      CollisionMeshDescription baseCollisionMesh = new CollisionMeshDescription();
      baseCollisionMesh.addCylinderReferencedAtBottomMiddle(baseRadius, baseHeight);
      baseLink.setCollisionMesh(baseCollisionMesh);

      baseJoint.setLink(baseLink);
      this.addRootJoint(baseJoint);

      // Upper Arm:
      PinJointDescription shoulderJoint = new PinJointDescription("shoulder", new Vector3d(0.0, 0.0, shoulderOffsetZ), Axis.Y);
      LinkDescription upperArm = new LinkDescription("upperArm");
      upperArm.setCenterOfMassOffset(new Vector3d(0.0, 0.0, upperArmLength / 2.0));
      upperArm.setMassAndRadiiOfGyration(upperArmMass, upperArmRadiusOfGyrationX, upperArmRadiusOfGyrationY, upperArmRadiusOfGyrationZ);

      LinkGraphicsDescription upperArmGraphics = new LinkGraphicsDescription();
      AppearanceDefinition upperArmAppearance = YoAppearance.Red();
      upperArmGraphics.addCylinder(upperArmLength, upperArmRadius, upperArmAppearance);
      upperArmGraphics.translate(new Vector3d(0.0, 0.0, upperArmLength));
      upperArmGraphics.rotate(Math.PI / 2.0, Axis.X);
      upperArmGraphics.translate(0.0, 0.0, -elbowMotorWidth / 2.0);
      upperArmGraphics.addCylinder(elbowMotorWidth, elbowMotorRadius, YoAppearance.Black());
      upperArm.setLinkGraphics(upperArmGraphics);

      CollisionMeshDescription upperArmCollisionMesh = new CollisionMeshDescription();
      upperArmCollisionMesh.addCylinderReferencedAtBottomMiddle(upperArmRadius, upperArmLength - elbowMotorRadius);
      upperArm.setCollisionMesh(upperArmCollisionMesh);
      
      shoulderJoint.setLink(upperArm);
      baseJoint.addJoint(shoulderJoint);

      // Lower arm:
      PinJointDescription elbowJoint = new PinJointDescription("elbow", new Vector3d(0.0, 0.0, upperArmLength), Axis.Y);
      LinkDescription lowerArm = new LinkDescription("lowerArm");
      lowerArm.setCenterOfMassOffset(new Vector3d(0.0, 0.0, lowerArmLength / 2.0));
      lowerArm.setMassAndRadiiOfGyration(lowerArmMass, lowerArmRadiusOfGyrationX, lowerArmRadiusOfGyrationY, lowerArmRadiusOfGyrationZ);

      LinkGraphicsDescription lowerArmGraphics = new LinkGraphicsDescription();
      AppearanceDefinition lowerArmAppearance = YoAppearance.Green();
      lowerArmGraphics.addCylinder(lowerArmLength, lowerArmRadius, lowerArmAppearance);
      lowerArmGraphics.identity();
      lowerArmGraphics.translate(new Vector3d(0.0, 0.0, lowerArmLength));
      lowerArmGraphics.addSphere(handRadius);
      lowerArm.setLinkGraphics(lowerArmGraphics);

      CollisionMeshDescription lowerArmCollisionMesh = new CollisionMeshDescription();
      lowerArmCollisionMesh.translate(0.0, 0.0, elbowMotorRadius);
      lowerArmCollisionMesh.addCylinderReferencedAtBottomMiddle(lowerArmRadius, lowerArmLength - elbowMotorRadius);
      lowerArmCollisionMesh.translate(0.0, 0.0, lowerArmLength);
      lowerArmCollisionMesh.addSphere(handRadius);
      lowerArm.setCollisionMesh(lowerArmCollisionMesh);

      elbowJoint.setLink(lowerArm);
      shoulderJoint.addJoint(elbowJoint);
   }

}
