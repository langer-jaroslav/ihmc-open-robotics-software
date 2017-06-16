package us.ihmc.robotics.geometry.shapes;

import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple4D.interfaces.QuaternionReadOnly;
import us.ihmc.robotics.geometry.Direction;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public class FrameBox3d extends FrameShape3d<FrameBox3d, Box3d>
{
   private Box3d box3d;

   public FrameBox3d(FrameBox3d other)
   {
      this(other.referenceFrame, other.box3d);
   }

   public FrameBox3d(ReferenceFrame referenceFrame)
   {
      super(referenceFrame, new Box3d());
      box3d = getGeometryObject();
   }

   public FrameBox3d(ReferenceFrame referenceFrame, Box3d box3d)
   {
      super(referenceFrame, new Box3d(box3d));
      this.box3d = getGeometryObject();
   }

   public FrameBox3d(ReferenceFrame referenceFrame, double lengthX, double widthY, double heightZ)
   {
      super(referenceFrame, new Box3d(lengthX, widthY, heightZ));
      box3d = getGeometryObject();
   }

   public FrameBox3d(ReferenceFrame referenceFrame, RigidBodyTransform configuration, double lengthX, double widthY, double heightZ)
   {
      super(referenceFrame, new Box3d(configuration, lengthX, widthY, heightZ));
      box3d = getGeometryObject();
   }

   public FrameBox3d(ReferenceFrame referenceFrame, Pose3D pose, double lengthX, double widthY, double heightZ)
   {
      super(referenceFrame, new Box3d(pose.getPosition(), pose.getOrientation(), lengthX, widthY, heightZ));
      box3d = getGeometryObject();
   }

   public Box3d getBox3d()
   {
      return box3d;
   }

   public FramePoint getCenter()
   {
      FramePoint ret = new FramePoint(referenceFrame);
      getCenter(ret);
      return ret;
   }

   public void getCenter(FramePoint pointToPack)
   {
      pointToPack.setToZero(referenceFrame);
      box3d.getCenter(pointToPack.getPoint());
   }

   public FramePoint getCenterCopy()
   {
      FramePoint ret = new FramePoint(referenceFrame);
      getCenter(ret);

      return ret;
   }

   public double getDimension(Direction direction)
   {
      return box3d.getDimension(direction);
   }

   public void getTransform(RigidBodyTransform transformToPack)
   {
      this.box3d.getPose(transformToPack);
   }

   public RigidBodyTransform getTransformCopy()
   {
      RigidBodyTransform ret = new RigidBodyTransform();
      getTransform(ret);

      return ret;
   }

   public void getRotation(RotationMatrix rotationMatrixToPack)
   {
      this.box3d.getOrientation(rotationMatrixToPack);
   }

   public RotationMatrix getRotationCopy()
   {
      RotationMatrix ret = new RotationMatrix();
      getRotation(ret);

      return ret;
   }

   public void setPose(Point3DReadOnly position, QuaternionReadOnly orientation)
   {
      box3d.setPose(position, orientation);
   }
   
   public void setPose(Pose3D pose)
   {
      box3d.setPose(pose.getPosition(), pose.getOrientation());
   }
   
   public void setTransform(RigidBodyTransform transform3D)
   {
      box3d.setPose(transform3D);
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("ReferenceFrame: " + referenceFrame + ")\n");
      builder.append(box3d.toString());

      return builder.toString();
   }

   public void getFramePose(FramePose framePoseToPack)
   {
      framePoseToPack.setToZero(referenceFrame);
      box3d.getPose(framePoseToPack.getGeometryObject());
   }

   public void scale(double scale)
   {
      box3d.scale(scale);
   }

   public void computeVertices(Point3D[] vertices)
   {
      box3d.computeVertices(vertices);
   }
}
