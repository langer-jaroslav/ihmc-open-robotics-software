package us.ihmc.pathPlanning.visibilityGraphs;

import us.ihmc.euclid.interfaces.EpsilonComparable;
import us.ihmc.euclid.interfaces.Transformable;
import us.ihmc.euclid.transform.interfaces.Transform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;

public class Connection implements Transformable, EpsilonComparable<Connection>
{
   private ConnectionPoint3D source;
   private ConnectionPoint3D target;

   public Connection(Point2DReadOnly source, Point2DReadOnly target)
   {
      this.source = new ConnectionPoint3D(source);
      this.target = new ConnectionPoint3D(target);
   }

   public Connection(Point3DReadOnly source, Point3DReadOnly target)
   {
      this.source = new ConnectionPoint3D(source);
      this.target = new ConnectionPoint3D(target);
   }

   public ConnectionPoint3D getSourcePoint()
   {
      return source;
   }

   public ConnectionPoint3D getTargetPoint()
   {
      return target;
   }

   public Point2D getSourcePoint2D()
   {
      return new Point2D(source);
   }
   
   public Point2D getTargetPoint2D()
   {
      return new Point2D(target);
   }

   public void applyTransform(Transform transform)
   {
      source.applyTransform(transform);
      target.applyTransform(transform);
   }

   @Override
   public void applyInverseTransform(Transform transform)
   {
      source.applyInverseTransform(transform);
      target.applyInverseTransform(transform);
   }

   @Override
   public boolean epsilonEquals(Connection other, double epsilon)
   {
      return source.epsilonEquals(other.source, epsilon) && target.epsilonEquals(other.target, epsilon);
   }
}