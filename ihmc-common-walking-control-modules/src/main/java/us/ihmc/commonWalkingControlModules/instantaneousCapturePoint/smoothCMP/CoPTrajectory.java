package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothCMP;

import us.ihmc.commonWalkingControlModules.configurations.CoPSplineType;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.robotics.math.trajectories.FrameTrajectory3D;
import us.ihmc.robotics.math.trajectories.SegmentedFrameTrajectory3D;

public class CoPTrajectory extends SegmentedFrameTrajectory3D implements CoPTrajectoryInterface
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final CoPSplineType splineType;
   private final WalkingTrajectoryType trajectoryType;
   private final int stepNumber;

   public CoPTrajectory(int stepNumber, CoPSplineType splineType, int maxNumberOfSegments, WalkingTrajectoryType type)
   {      
      super(maxNumberOfSegments, splineType.getNumberOfCoefficients());
      this.splineType = splineType;
      this.trajectoryType = type;
      this.stepNumber = stepNumber;
   }

   public void setNextSegment(double initialTime, double finalTime, FramePoint3D initialPosition, FramePoint3D finalPosition)
   {
      FrameTrajectory3D segment = segments.add();
      switch (this.splineType)
      {
      case CUBIC:
         segment.setCubic(initialTime, finalTime, initialPosition, finalPosition);
         break;
      default:
         segment.setLinear(initialTime, finalTime, initialPosition, finalPosition);
         break;
      }
   }
   
   public WalkingTrajectoryType getTrajectoryType()
   {
      return trajectoryType;
   }
   
   public int getStepNumber()
   {
      return stepNumber;
   }
}
