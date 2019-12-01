package us.ihmc.robotics.linearAlgebra;

import java.util.Random;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.RandomMatrices;
import org.junit.jupiter.api.Test;

import us.ihmc.robotics.testing.MatrixTestTools;

public class MatrixOfCofactorsCalculatorInefficientTest
{

	@Test
   public void test()
   {
      int n = 5;
      
      DenseMatrix64F mat = RandomMatrices.createRandom(n, n, new Random());
      DenseMatrix64F inverse = new DenseMatrix64F(n, n);
      CommonOps.invert(mat, inverse);
      
      DenseMatrix64F matrixOfCoFactors = MatrixOfCofactorsCalculatorInefficient.computeMatrixOfCoFactors(mat);
      DenseMatrix64F inverseViaMatrixOfCoFactors = new DenseMatrix64F(matrixOfCoFactors.getNumCols(), matrixOfCoFactors.getNumRows());
      CommonOps.transpose(matrixOfCoFactors, inverseViaMatrixOfCoFactors);
      
      CommonOps.scale(1.0/CommonOps.det(mat), inverseViaMatrixOfCoFactors);

      MatrixTestTools.assertMatrixEquals(inverse, inverseViaMatrixOfCoFactors, 1e-5);
   }

}
