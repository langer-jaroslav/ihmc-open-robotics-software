package us.ihmc.commonWalkingControlModules.controlModules;

import com.yobotics.simulationconstructionset.DoubleYoVariable;

import us.ihmc.commonWalkingControlModules.RobotSide;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

public interface DesiredHeadingControlModule
{
   public abstract void updateDesiredHeadingFrame();

   public abstract ReferenceFrame getDesiredHeadingFrame();

   public abstract FrameVector getDisplacementWithRespectToFoot(RobotSide robotSide, FramePoint position);

   public abstract FrameVector getFinalHeadingTarget();

   public abstract FramePoint getPositionOffsetFromFoot(RobotSide robotSide, FrameVector offset);

   public abstract void setDesiredHeading(double desiredHeading);

   public abstract DoubleYoVariable getDesiredHeading();
}
