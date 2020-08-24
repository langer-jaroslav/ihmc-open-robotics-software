package us.ihmc.commonWalkingControlModules.capturePoint.lqrControl;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.robotics.math.trajectories.Trajectory3D;

import java.util.List;

public class AlgebraicS2Function implements S2Function
{
   private final DMatrixRMaj endValueLocal = new DMatrixRMaj(6, 1);
   private final RecyclingArrayList<AlgebraicS2Segment> s2Segments = new RecyclingArrayList<>(AlgebraicS2Segment::new);

   public void set(DMatrixRMaj endValue, List<Trajectory3D> vrpTrajectories, DMatrixRMaj A2, DMatrixRMaj B2)
   {
      s2Segments.clear();
      for (int j = 0; j < vrpTrajectories.size(); j++)
         s2Segments.add();

      endValueLocal.set(endValue);
      int numberOfSegments = vrpTrajectories.size() - 1;
      for (int j = numberOfSegments; j >= 0; j--)
      {
         AlgebraicS2Segment s2Segment = s2Segments.get(j);

         s2Segment.set(endValueLocal, vrpTrajectories.get(j), A2, B2);

         if (j != numberOfSegments)
            CommonOps_DDRM.add(s2Segment.getAlpha(), s2Segment.getBeta(0), endValueLocal);
      }
   }

   public AlgebraicS2Segment getSegment(int i)
   {
      return s2Segments.get(i);
   }

   @Override
   public void compute(double timeInState, DMatrixRMaj s2ToPack)
   {
      s2Segments.get(0).compute(timeInState, s2ToPack);
   }
}
