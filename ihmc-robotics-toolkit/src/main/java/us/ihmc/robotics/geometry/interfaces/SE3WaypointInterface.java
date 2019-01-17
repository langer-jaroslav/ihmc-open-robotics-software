package us.ihmc.robotics.geometry.interfaces;

import us.ihmc.euclid.tuple3D.interfaces.Point3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.euclid.tuple4D.interfaces.QuaternionBasics;
import us.ihmc.euclid.tuple4D.interfaces.QuaternionReadOnly;

public interface SE3WaypointInterface<T extends SE3WaypointInterface<T>> extends EuclideanWaypointInterface<T>, SO3WaypointInterface<T>
{
   public default void set(Point3DReadOnly position, QuaternionReadOnly orientation, Vector3DReadOnly linearVelocity, Vector3DReadOnly angularVelocity)
   {
      setPosition(position);
      setOrientation(orientation);
      setLinearVelocity(linearVelocity);
      setAngularVelocity(angularVelocity);
   }

   public default void get(Point3DBasics positionToPack, QuaternionBasics orientationToPack, Vector3DBasics linearVelocityToPack, Vector3DBasics angularVelocityToPack)
   {
      getPosition(positionToPack);
      getOrientation(orientationToPack);
      getLinearVelocity(linearVelocityToPack);
      getAngularVelocity(angularVelocityToPack);
   }

   @Override
   default boolean epsilonEquals(T other, double epsilon)
   {
      boolean euclideanMatch = EuclideanWaypointInterface.super.epsilonEquals(other, epsilon);
      boolean so3Match = SO3WaypointInterface.super.epsilonEquals(other, epsilon);
      return euclideanMatch && so3Match;
   }

   @Override
   default boolean geometricallyEquals(T other, double epsilon)
   {
      boolean euclideanMatch = EuclideanWaypointInterface.super.geometricallyEquals(other, epsilon);
      boolean so3Match = SO3WaypointInterface.super.geometricallyEquals(other, epsilon);
      return euclideanMatch && so3Match;
   }

   @Override
   default void set(T other)
   {
      EuclideanWaypointInterface.super.set(other);
      SO3WaypointInterface.super.set(other);
   }

   @Override
   default void setToNaN()
   {
      EuclideanWaypointInterface.super.setToNaN();
      SO3WaypointInterface.super.setToNaN();
   }

   @Override
   default void setToZero()
   {
      EuclideanWaypointInterface.super.setToZero();
      SO3WaypointInterface.super.setToZero();
   }

   @Override
   default boolean containsNaN()
   {
      return EuclideanWaypointInterface.super.containsNaN() || SO3WaypointInterface.super.containsNaN();
   }
}
