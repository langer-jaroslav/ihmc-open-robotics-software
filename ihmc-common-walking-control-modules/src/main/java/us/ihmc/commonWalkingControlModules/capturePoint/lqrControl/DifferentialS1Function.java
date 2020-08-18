package us.ihmc.commonWalkingControlModules.capturePoint.lqrControl;

import com.google.common.collect.Lists;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.matrixlib.NativeCommonOps;

public class DifferentialS1Function implements S1Function
{
   private double timeAtEnd;
   private final double dt;
   private final DMatrixRMaj R1Inverse = new DMatrixRMaj(3, 3);
   private final DMatrixRMaj NB = new DMatrixRMaj(3, 3);
   private final DMatrixRMaj S1Dot = new DMatrixRMaj(6, 6);

   private final RecyclingArrayList<DMatrixRMaj> S1Trajectory = new RecyclingArrayList<>(() -> new DMatrixRMaj(6, 6));

   public DifferentialS1Function(double dt)
   {
      this.dt = dt;
   }

   public void set(DMatrixRMaj Q1, DMatrixRMaj R1, DMatrixRMaj NTranspose, DMatrixRMaj A, DMatrixRMaj B, DMatrixRMaj S1AtEnd, double duration)
   {
      NativeCommonOps.invert(R1, R1Inverse);

      S1Trajectory.clear();
      S1Trajectory.add().set(S1AtEnd);

      for (double t = dt; t <= duration; t += dt)
      {
         DMatrixRMaj previousS1 = S1Trajectory.getLast();
         DMatrixRMaj newS1 = S1Trajectory.add();

         computeNB(B, NTranspose, previousS1);
         computeS1Dot(Q1, NB, R1Inverse, previousS1, A);

         CommonOps_DDRM.add(previousS1, -dt, S1Dot, newS1);
      }

      Lists.reverse(S1Trajectory);
   }

   @Override
   public void compute(double timeInState, DMatrixRMaj S1ToPack)
   {
      int startIndex = getStartIndex(timeInState);
      DMatrixRMaj start = S1Trajectory.get(startIndex);
      DMatrixRMaj end = S1Trajectory.get(startIndex + 1);
      interpolate(start, end, getAlphaBetweenSegments(timeInState), S1ToPack);
   }

   private int getStartIndex(double timeInState)
   {
      return (int) Math.floor(timeInState / dt);
   }

   private double getAlphaBetweenSegments(double timeInState)
   {
      return (timeInState % dt) / timeInState;
   }

   private static void interpolate(DMatrixRMaj start, DMatrixRMaj end, double alpha, DMatrixRMaj ret)
   {
      CommonOps_DDRM.scale(1.0 - alpha, start, ret);
      CommonOps_DDRM.addEquals(ret, alpha, end);
   }

   private void computeNB(DMatrixRMaj B, DMatrixRMaj NTranspose, DMatrixRMaj S1)
   {
      CommonOps_DDRM.multTransA(B, S1, NB);
      CommonOps_DDRM.addEquals(NB, NTranspose);
   }

   private void computeS1Dot(DMatrixRMaj Q1, DMatrixRMaj NB, DMatrixRMaj R1Inverse, DMatrixRMaj S1, DMatrixRMaj A)
   {
      NativeCommonOps.multQuad(NB, R1Inverse, S1Dot);
      CommonOps_DDRM.addEquals(S1Dot, -1.0, Q1);
      CommonOps_DDRM.multAdd(-1.0, S1, A, S1Dot);
      CommonOps_DDRM.multAddTransA(-1.0, A, S1, S1Dot);
   }
}
