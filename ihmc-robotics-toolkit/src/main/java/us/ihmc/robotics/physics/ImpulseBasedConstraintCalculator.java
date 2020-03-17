package us.ihmc.robotics.physics;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.mecano.algorithms.interfaces.RigidBodyTwistProvider;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.mecano.tools.JointStateType;

public interface ImpulseBasedConstraintCalculator
{
   void reset();

   void initialize();

   default void computeImpulse()
   {
      reset();
      updateImpulse(1.0);
   }

   void updateImpulse(double alpha);

   double getImpulseUpdate();

   double getVelocityUpdate();

   boolean isConstraintActive();

   default void setExternalTwistModifiers(RigidBodyTwistProvider externalRigidBodyTwistModifier, JointStateProvider externalJointTwistModifier)
   {
      if (externalJointTwistModifier.getState() != JointStateType.VELOCITY)
         throw new IllegalArgumentException("Unexpect joint state providers, expected: VELOCITY, was " + externalJointTwistModifier.getState());

      setExternalTwistModifier(externalRigidBodyTwistModifier);
      setExternalTwistModifier(externalJointTwistModifier);
   }

   default void setExternalTwistModifier(RigidBodyTwistProvider externalRigidBodyTwistModifier)
   {

   }

   default void setExternalTwistModifier(JointStateProvider externalJointTwistModifier)
   {

   }
   
   void applyImpulseLazy();
   
   double getDT();

   int getNumberOfRobotsInvolved();

   RigidBodyTwistProvider getRigidBodyTwistChangeProvider(int index);

   JointStateProvider getJointTwistChangeProvider(int index);

   RigidBodyBasics getRootBody(int index);

   DenseMatrix64F getJointVelocityChange(int index);
}
