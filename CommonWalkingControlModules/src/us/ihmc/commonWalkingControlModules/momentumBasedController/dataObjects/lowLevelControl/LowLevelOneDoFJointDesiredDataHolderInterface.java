package us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.lowLevelControl;

import us.ihmc.robotics.screwTheory.OneDoFJoint;

public interface LowLevelOneDoFJointDesiredDataHolderInterface
{

   /**
    * Complete the information held in this using other.
    * Does not overwrite the data already set in this.
    */
   public abstract void completeWith(LowLevelOneDoFJointDesiredDataHolderInterface other);

   /**
    * Clear this and copy the data held in other.
    */
   public abstract void overwriteWith(LowLevelOneDoFJointDesiredDataHolderInterface other);

   public abstract LowLevelJointControlMode getJointControlMode(OneDoFJoint joint);

   public abstract double getDesiredJointTorque(OneDoFJoint joint);

   public abstract double getDesiredJointPosition(OneDoFJoint joint);

   public abstract double getDesiredJointVelocity(OneDoFJoint joint);

   public abstract double getDesiredJointAcceleration(OneDoFJoint joint);

   public abstract boolean pollResetJointIntegrators(OneDoFJoint joint);

   public abstract boolean peekResetJointIntegrators(OneDoFJoint joint);

   public abstract boolean hasDataForJoint(OneDoFJoint joint);

   public abstract boolean hasControlModeForJoint(OneDoFJoint joint);

   public abstract boolean hasDesiredTorqueForJoint(OneDoFJoint joint);

   public abstract boolean hasDesiredPositionForJoint(OneDoFJoint joint);

   public abstract boolean hasDesiredVelocityForJoint(OneDoFJoint joint);

   public abstract boolean hasDesiredAcceleration(OneDoFJoint joint);

   public abstract OneDoFJoint getOneDoFJoint(int index);

   public abstract LowLevelJointDataReadOnly getLowLevelJointData(OneDoFJoint joint);

   public abstract int getNumberOfJointsWithLowLevelData();

}