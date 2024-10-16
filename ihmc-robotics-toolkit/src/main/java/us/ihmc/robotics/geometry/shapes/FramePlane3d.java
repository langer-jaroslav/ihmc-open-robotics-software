package us.ihmc.robotics.geometry.shapes;

import us.ihmc.euclid.Axis3D;
import us.ihmc.euclid.geometry.Plane3D;
import us.ihmc.euclid.geometry.interfaces.Line3DReadOnly;
import us.ihmc.euclid.referenceFrame.FrameLine3D;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.ReferenceFrameHolder;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;

public class FramePlane3d implements ReferenceFrameHolder
{
   private ReferenceFrame referenceFrame;
   private final Plane3D plane3d;

   private final RigidBodyTransform temporaryTransformToDesiredFrame = new RigidBodyTransform();
   private final Vector3D temporaryVector = new Vector3D();
   private final Point3D temporaryPoint = new Point3D();
   
   public FramePlane3d()
   {
      this(ReferenceFrame.getWorldFrame());
   }
   
   public FramePlane3d(ReferenceFrame referenceFrame, Plane3D plane3d)
   {
      this.referenceFrame = referenceFrame;
      this.plane3d = plane3d;
   }

   public FramePlane3d(ReferenceFrame referenceFrame)
   {
      this.referenceFrame = referenceFrame;
      this.plane3d = new Plane3D();
   }

   public FramePlane3d(FramePlane3d framePlane3d)
   {
      this.referenceFrame = framePlane3d.referenceFrame;
      this.plane3d = new Plane3D(framePlane3d.plane3d);
   }

   public FramePlane3d(FrameVector3D normal, FramePoint3D point)
   {
      normal.checkReferenceFrameMatch(point);
      this.referenceFrame = normal.getReferenceFrame();
      this.plane3d = new Plane3D(point, normal);
   }

   public FramePlane3d(ReferenceFrame referenceFrame, Point3DReadOnly point, Vector3DReadOnly normal)
   {
      this.referenceFrame = referenceFrame;
      this.plane3d = new Plane3D(point, normal);
   }

   @Override
   public ReferenceFrame getReferenceFrame()
   {
      return referenceFrame;
   }

   public void getNormal(FrameVector3D normalToPack)
   {
      checkReferenceFrameMatch(normalToPack.getReferenceFrame());
      temporaryVector.set(this.plane3d.getNormal());
      normalToPack.set(temporaryVector);
   }

   public FrameVector3D getNormalCopy()
   {
      FrameVector3D returnVector = new FrameVector3D(this.referenceFrame);
      getNormal(returnVector);
      return returnVector;
   }
   
   public Vector3DBasics getNormal()
   {
      return plane3d.getNormal();
   }

   public void setNormal(double x, double y, double z)
   {
      plane3d.getNormal().set(x, y, z);
   }

   public void setNormal(Vector3DReadOnly normal)
   {
      plane3d.getNormal().set(normal);
   }

   public void getPoint(FramePoint3D pointToPack)
   {
      checkReferenceFrameMatch(pointToPack.getReferenceFrame());
      temporaryPoint.set(this.plane3d.getPoint());
      pointToPack.set(temporaryPoint);
   }

   public FramePoint3D getPointCopy()
   {
      FramePoint3D pointToReturn = new FramePoint3D(this.getReferenceFrame());
      this.getPoint(pointToReturn);
      return pointToReturn;
   }
   
   public Point3DBasics getPoint()
   {
      return plane3d.getPoint();
   }

   public void setPoint(double x, double y, double z)
   {
      plane3d.getPoint().set(x, y, z);
   }

   public void setPoint(Point3DReadOnly point)
   {
      plane3d.getPoint().set(point);
   }

   public void setPoints(FramePoint3D pointA, FramePoint3D pointB, FramePoint3D pointC)
   {
      pointA.checkReferenceFrameMatch(referenceFrame);
      pointB.checkReferenceFrameMatch(referenceFrame);
      pointC.checkReferenceFrameMatch(referenceFrame);

      plane3d.set(pointA, pointB, pointC);
   }
   
   public void changeFrame(ReferenceFrame desiredFrame)
   {
      if (desiredFrame != referenceFrame)
      {
         referenceFrame.getTransformToDesiredFrame(temporaryTransformToDesiredFrame, desiredFrame);
         plane3d.applyTransform(temporaryTransformToDesiredFrame);
         referenceFrame = desiredFrame;
      }

      // otherwise: in the right frame already, so do nothing
   }

   public boolean isOnOrAbove(FramePoint3D pointToTest)
   {
      checkReferenceFrameMatch(pointToTest);

      return plane3d.isOnOrAbove(pointToTest);
   }

   public boolean isOnOrAbove(Point3DReadOnly pointToTest)
   {
      return plane3d.isOnOrAbove(pointToTest);
   }

   public boolean isOnOrAbove(Point3DReadOnly pointToTest, double epsilon)
   {
      return plane3d.isOnOrAbove(pointToTest, epsilon);
   }

   public boolean isOnOrBelow(FramePoint3D pointToTest)
   {
      checkReferenceFrameMatch(pointToTest);

      return plane3d.isOnOrBelow(pointToTest);
   }

   /**
    * Tests if the two planes are parallel by testing if their normals are collinear.
    * The latter is done given a tolerance on the angle between the two normal axes in the range ]0; <i>pi</i>/2[.
    * 
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the length of either normal is below {@code 1.0E-7}, this method fails and returns {@code false}.
    * </ul>
    * </p>
    * 
    * @param otherPlane the other plane to do the test with. Not modified.
    * @param angleEpsilon tolerance on the angle in radians.
    * @return {@code true} if the two planes are parallel, {@code false} otherwise.
    */
   public boolean isParallel(FramePlane3d otherPlane, double angleEpsilon)
   {
      checkReferenceFrameMatch(otherPlane);
      return plane3d.isParallel(otherPlane.plane3d, angleEpsilon);
   }

   /**
    * Tests if this plane and the given plane are coincident:
    * <ul>
    *    <li> {@code this.normal} and {@code otherPlane.normal} are collinear given the tolerance {@code angleEpsilon}.
    *    <li> the distance of {@code otherPlane.point} from the this plane is less than {@code distanceEpsilon}.
    * </ul>
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the length of either normal is below {@code 1.0E-7}, this method fails and returns {@code false}.
    * </ul>
    * </p>
    * 
    * @param otherPlane the other plane to do the test with. Not modified.
    * @param angleEpsilon tolerance on the angle in radians to determine if the plane normals are collinear. 
    * @param distanceEpsilon tolerance on the distance to determine if {@code otherPlane.point} belongs to this plane.
    * @return {@code true} if the two planes are coincident, {@code false} otherwise.
    */
   public boolean isCoincident(FramePlane3d otherPlane, double angleEpsilon, double distanceEpsilon)
   {
      checkReferenceFrameMatch(otherPlane);
      return plane3d.isCoincident(otherPlane.plane3d, angleEpsilon, distanceEpsilon);
   }

   public FramePoint3D orthogonalProjectionCopy(FramePoint3D point)
   {
      checkReferenceFrameMatch(point);
      FramePoint3D returnPoint = new FramePoint3D(point);
      orthogonalProjection(returnPoint);

      return returnPoint;
   }

   public void orthogonalProjection(FramePoint3D point)
   {
      checkReferenceFrameMatch(point);
      plane3d.orthogonalProjection(point);
   }

   public double getZOnPlane(FramePoint2D xyPoint)
   {
      checkReferenceFrameMatch(xyPoint.getReferenceFrame());
      return plane3d.getZOnPlane(xyPoint.getX(), xyPoint.getY());
   }
   
   public double distance(FramePoint3D point)
   {
      checkReferenceFrameMatch(point);
      return plane3d.distance(point);
   }

   public boolean epsilonEquals(FramePlane3d plane, double epsilon)
   {
      checkReferenceFrameMatch(plane.getReferenceFrame());

      return ((referenceFrame == plane.getReferenceFrame()) && (plane.plane3d.epsilonEquals(this.plane3d, epsilon)));
   }

   public FramePlane3d applyTransformCopy(RigidBodyTransform transformation)
   {
      FramePlane3d returnPlane = new FramePlane3d(this);
      returnPlane.applyTransform(transformation);

      return returnPlane;
   }

   public void applyTransform(RigidBodyTransform transformation)
   {
      plane3d.applyTransform(transformation);
   }

   public void setIncludingFrame(ReferenceFrame referenceFrame, double pointX, double pointY, double pointZ, double normalX, double normalY, double normalZ)
   {
      this.referenceFrame = referenceFrame;
      plane3d.getPoint().set(pointX, pointY, pointZ);
      plane3d.getNormal().set(normalX, normalY, normalZ);
   }
   
   public void getIntersectionWithLine(FramePoint3D pointToPack, FrameLine3D line)
   {
	   checkReferenceFrameMatch(line.getReferenceFrame());
	   checkReferenceFrameMatch(pointToPack.getReferenceFrame());
	   
	   Point3D intersectionToPack = new Point3D();
	   plane3d.intersectionWith(intersectionToPack, line.getPoint(), line.getDirection());
	   pointToPack.set(intersectionToPack);
   }

   public void getIntersectionWithLine(Point3D pointToPack, Line3DReadOnly line)
   {
      plane3d.intersectionWith(pointToPack, line.getPoint(), line.getDirection());
   }

   public void setToZero()
   {
      getPoint().setToZero();
      getNormal().set(Axis3D.Z);
   }

   public void setToZero(ReferenceFrame referenceFrame)
   {
      setReferenceFrame(referenceFrame);
      setToZero();
   }

   public void setReferenceFrame(ReferenceFrame referenceFrame)
   {
      this.referenceFrame = referenceFrame;
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();
      builder.append("ReferenceFrame = " + referenceFrame + ", " + plane3d.toString());

      return builder.toString();
   }
}
