package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.qpInput;

import org.ejml.data.DenseMatrix64F;

public class FeedbackTaskInput extends ICPQPInput
{
   public FeedbackTaskInput()
   {
      quadraticTerm = new DenseMatrix64F(2, 2);
      linearTerm = new DenseMatrix64F(2, 1);
   }
}
