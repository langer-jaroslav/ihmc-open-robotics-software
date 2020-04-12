package us.ihmc.robotics.math.trajectories;

import org.ejml.data.DenseMatrix64F;
import org.junit.jupiter.api.Test;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

import static us.ihmc.robotics.Assert.assertEquals;

public class YoOptimizedPolynomialTest
{
   private static double EPSILON = 1e-4;

   String namePrefix = "YoPolynomialTest";

   @Test
   public void testLinearDerivativePointManual()
   {
      //linear polynomial: y(x) = a0 + a1*x
      YoVariableRegistry registry = new YoVariableRegistry(namePrefix);
      int numberOfCoefficients = 2;
      YoOptimizedPolynomial linear = new YoOptimizedPolynomial(namePrefix + "Linear", numberOfCoefficients, registry);

      double x0 = 1.0, xf = 2.0;
      double y0 = 0.5, yf = 1.5;

      linear.reshape(2);
      linear.addPositionPoint(x0, y0);
      linear.addPositionPoint(xf, yf);
      linear.fit();

      double x = 2.0/3.0 * (xf - x0);
      double a0 = linear.getCoefficient(0);
      double a1 = linear.getCoefficient(1);

      double scale = xf - x0;
      double offset = -x0;

      double a1Expected = (yf - y0) / scale;
      double a0Expected = y0 - (x0 + offset) * a1Expected;

      assertEquals(a0Expected, a0, EPSILON);
      assertEquals(a1Expected, a1, EPSILON);

      double yLinear = linear.getDerivative(0, x);
      double yManual = a0 + a1 * (x + offset) / scale;
      assertEquals(yLinear, yManual, EPSILON);

      double dyLinear = linear.getDerivative(1, x);
      double dyManual = a1;
      assertEquals(dyLinear, dyManual, EPSILON);

      double ddyLinear = linear.getDerivative(2, x);
      double ddyManual = 0.0;
      assertEquals(ddyLinear, ddyManual, EPSILON);

      linear.compute(x);
      assertEquals(yManual, linear.getPosition(), EPSILON);
      assertEquals(dyManual, linear.getVelocity(), EPSILON);
      assertEquals(ddyManual, linear.getAcceleration(), EPSILON);

      linear.compute(x0);
      assertEquals(y0, linear.getPosition(), EPSILON);
      linear.compute(xf);
      assertEquals(yf, linear.getPosition(), EPSILON);
   }

   @Test
   public void testLinearDerivativePointAndSlope()
   {
      //linear polynomial: y(x) = a0 + a1*x
      YoVariableRegistry registry = new YoVariableRegistry(namePrefix);
      int numberOfCoefficients = 2;
      YoOptimizedPolynomial linear = new YoOptimizedPolynomial(namePrefix + "Linear", numberOfCoefficients, registry);

      double x0 = 1.0;
      double y0 = 0.5;
      double dy0 = 1.0;

      linear.reshape(2);
      linear.addPositionPoint(x0, y0);
      linear.addVelocityPoint(x0, dy0);
      linear.fit();

      double x = 2.0/3.0 * 1.6;
      double a0 = linear.getCoefficient(0);
      double a1 = linear.getCoefficient(1);

      double a1Expected = dy0;
      double a0Expected = y0 - x0 * a1Expected;

      assertEquals(a1Expected, a1, EPSILON);
      assertEquals(a0Expected, a0, EPSILON);

      double yLinear = linear.getDerivative(0, x);
      double yManual = a0 + a1 * x;
      assertEquals(yLinear, yManual, EPSILON);

      double dyLinear = linear.getDerivative(1, x);
      double dyManual = a1;
      assertEquals(dyLinear, dyManual, EPSILON);

      double ddyLinear = linear.getDerivative(2, x);
      double ddyManual = 0.0;
      assertEquals(ddyLinear, ddyManual, EPSILON);

      linear.compute(x);
      assertEquals(yManual, linear.getPosition(), EPSILON);
      assertEquals(dyManual, linear.getVelocity(), EPSILON);
      assertEquals(ddyManual, linear.getAcceleration(), EPSILON);

      linear.compute(x0);
      assertEquals(y0, linear.getPosition(), EPSILON);
      assertEquals(dy0, linear.getVelocity(), EPSILON);
   }


   @Test
   public void testLinearDerivativePointAutomated()
   {
      //linear polynomial: y(x) = a0 + a1*x
      YoVariableRegistry registry = new YoVariableRegistry(namePrefix);
      int numberOfCoefficients = 2;
      YoOptimizedPolynomial linear = new YoOptimizedPolynomial(namePrefix + "Linear", numberOfCoefficients, registry);

      double x0 = 1.0, xf = 2.0;
      double y0 = 0.5, yf = 1.5;

      linear.reshape(2);
      linear.addPositionPoint(x0, y0);
      linear.addPositionPoint(xf, yf);
      linear.fit();

      double x = 2.0/3.0 * (xf - x0);

      compareDerivativesPoint(linear, x);
   }


   @Test
   public void testCubicDerivativePointAutomated()
   {
      //cubic polynomial: y(x) = a0 + a1*x + a2*x^2 + a3*x^3
      YoVariableRegistry registry = new YoVariableRegistry(namePrefix);
      int numberOfCoefficients = 4;
      YoOptimizedPolynomial cubic = new YoOptimizedPolynomial(namePrefix + "Cubic", numberOfCoefficients, registry);

      double x0 = 1.0, xf = 2.0;
      double y0 = 0.5, yf = 1.5;
      double dy0 = -0.5, dyf = 2.0;

      cubic.reshape(4);
      cubic.addPositionPoint(x0, y0);
      cubic.addVelocityPoint(x0, dy0);
      cubic.addPositionPoint(xf, yf);
      cubic.addVelocityPoint(xf, dyf);
      cubic.fit();

      double x = 2.0/3.0 * (xf - x0);

      cubic.compute(x0);
      assertEquals(y0, cubic.getPosition(), EPSILON);
      assertEquals(dy0, cubic.getVelocity(), EPSILON);

      cubic.compute(xf);
      assertEquals(yf, cubic.getPosition(), EPSILON);
      assertEquals(dyf, cubic.getVelocity(), EPSILON);

      compareDerivativesPoint(cubic, x);
   }


   @Test
   public void testXPowersDerivativeVectorCubic()
   {
      //cubic polynomial: y(x) = a0 + a1*x + a2*x^2 + a3*x^3
      YoVariableRegistry registry = new YoVariableRegistry(namePrefix);
      int numberOfCoefficients = 4;
      YoOptimizedPolynomial cubic = new YoOptimizedPolynomial(namePrefix + "Cubic", numberOfCoefficients, registry);

      int numTrials = 9;
      for(int i = 0; i < numTrials; i++)
      {
         double scaleX0 = 1.0 / Math.random(), scaleXf = 1.0 / Math.random();
         double scaleY0 = 1.0 / Math.random(), scaleYf = 1.0 / Math.random();
         double scaleDY0 = 1.0 / Math.random(), scaleDYf = 1.0 / Math.random();

         double x0 = Math.signum(Math.random()) * Math.random() * scaleX0, xf = x0 + Math.random() * scaleXf;
         double y0 = Math.signum(Math.random()) * Math.random() * scaleY0, yf = Math.signum(Math.random()) * Math.random() * scaleYf;
         double dy0 = Math.signum(Math.random()) * Math.random() * scaleDY0, dyf = Math.signum(Math.random()) * Math.random() * scaleDYf;

         cubic.clear();
         cubic.reshape(numberOfCoefficients);
         cubic.addPositionPoint(x0, y0);
         cubic.addPositionPoint(xf, yf);
         cubic.addVelocityPoint(x0, dy0);
         cubic.addVelocityPoint(xf, dyf);
         cubic.fit();

         double x = Math.random() * (xf - x0);

         cubic.compute(x0);
         assertEquals(y0, cubic.getPosition(), EPSILON);
         assertEquals(dy0, cubic.getVelocity(), EPSILON);

         cubic.compute(xf);
         assertEquals(yf, cubic.getPosition(), EPSILON);
         assertEquals(dyf, cubic.getVelocity(), EPSILON);

         compareXPowersDerivativesVector(cubic, x);
      }
   }


   @Test
   public void testDerivativeCoefficients()
   {
      //cubic polynomial: y(x) = a0 + a1*x + a2*x^2 + a3*x^3
      YoVariableRegistry registry = new YoVariableRegistry(namePrefix);
      int numberOfCoefficients = 8;
      YoOptimizedPolynomial septic = new YoOptimizedPolynomial(namePrefix + "Septic", numberOfCoefficients, registry);

      double x0 = 1.0, x1 = 1.2, x2 = 1.9, xf = 2.0;
      double y0 = 0.5, y1 = -0.75, y2 = 1.3, yf = 1.5;
      double dy0 = -0.5, dy1 = 0.5, dy2 = 1.0, dyf = 2.0;

      septic.reshape(numberOfCoefficients);
      septic.addPositionPoint(x0, y0);
      septic.addPositionPoint(x1, y1);
      septic.addPositionPoint(x2, y2);
      septic.addPositionPoint(xf, yf);
      septic.addVelocityPoint(x0, dy0);
      septic.addVelocityPoint(x1, dy1);
      septic.addVelocityPoint(x2, dy2);
      septic.addVelocityPoint(xf, dyf);
      septic.fit();

      int order3Exponent1Func = septic.getDerivativeCoefficient(3, 1);
      int order3Exponent1Hand = 0;
      assertEquals(order3Exponent1Func, order3Exponent1Hand, EPSILON);

      int order6Exponent7Func = septic.getDerivativeCoefficient(6, 7);
      int order6Exponent7Hand = 5040;
      assertEquals(order6Exponent7Func, order6Exponent7Hand, EPSILON);

      int order0Exponent5Func = septic.getDerivativeCoefficient(0, 5);
      int order0Exponent5Hand = 1;
      assertEquals(order0Exponent5Func, order0Exponent5Hand, EPSILON);

      int order3Exponent4Func = septic.getDerivativeCoefficient(3, 4);
      int order3Exponent4Hand = 24;
      assertEquals(order3Exponent4Func, order3Exponent4Hand, EPSILON);

      int order5Exponent2Func = septic.getDerivativeCoefficient(5, 2);
      int order5Exponent2Hand = 0;
      assertEquals(order5Exponent2Func, order5Exponent2Hand, EPSILON);

      int order1Exponent5Func = septic.getDerivativeCoefficient(1, 5);
      int order1Exponent5Hand = 5;
      assertEquals(order1Exponent5Func, order1Exponent5Hand, EPSILON);

      int order11Exponent1Func = septic.getDerivativeCoefficient(11, 1);
      int order11Exponent1Hand = 0;
      assertEquals(order11Exponent1Func, order11Exponent1Hand, EPSILON);

      int order13Exponent8Func = septic.getDerivativeCoefficient(13, 8);
      int order13Exponent8Hand = 0;
      assertEquals(order13Exponent8Func, order13Exponent8Hand, EPSILON);
   }

   @Test
   public void testDerivativeVersionsCubic()
   {
      //cubic polynomial: y(x) = a0 + a1*x + a2*x^2 + a3*x^3
      YoVariableRegistry registry = new YoVariableRegistry(namePrefix);
      int numberOfCoefficients = 4;
      YoOptimizedPolynomial cubic = new YoOptimizedPolynomial(namePrefix + "Cubic", numberOfCoefficients, registry);

      int numTrials = 9;
      for(int i = 0; i < numTrials; i++)
      {
         double scaleX0 = 1.0 / Math.random(), scaleXf = 1.0 / Math.random();
         double scaleY0 = 1.0 / Math.random(), scaleYf = 1.0 / Math.random();
         double scaleDY0 = 1.0 / Math.random(), scaleDYf = 1.0 / Math.random();

         double x0 = Math.signum(Math.random()) * Math.random() * scaleX0, xf = x0 + Math.random() * scaleXf;
         double y0 = Math.signum(Math.random()) * Math.random() * scaleY0, yf = Math.signum(Math.random()) * Math.random() * scaleYf;
         double dy0 = Math.signum(Math.random()) * Math.random() * scaleDY0, dyf = Math.signum(Math.random()) * Math.random() * scaleDYf;

         cubic.clear();
         cubic.reshape(4);
         cubic.addPositionPoint(x0, y0);
         cubic.addPositionPoint(xf, yf);
         cubic.addVelocityPoint(x0, dy0);
         cubic.addVelocityPoint(xf, dyf);
         cubic.fit();

         double x = Math.random() * (xf - x0);

         compareDerivativeVersions(cubic, x);

         cubic.compute(x0);
         assertEquals("trial = " + i, y0, cubic.getPosition(), EPSILON);
         assertEquals("trial = " + i, dy0, cubic.getVelocity(), EPSILON);

         cubic.compute(xf);
         assertEquals("trial = " + i, yf, cubic.getPosition(), EPSILON);
         assertEquals("trial = " + i, dyf, cubic.getVelocity(), EPSILON);
      }
   }


   public void compareDerivativesPoint(YoOptimizedPolynomial polynomial, double x)
   {
      double[] coefficients = polynomial.getCoefficients();

      for(int i = 0; i < coefficients.length + 3; i++)
      {
         double generalizedDYPoly = polynomial.getDerivative(i, x);

         double generalizedDYHand = 0.0;
         if(i < coefficients.length)
         {
            for(int j = i; j < coefficients.length; j++)
            {
               double derivativeCoefficient = polynomial.getDerivativeCoefficient(i, j);
               generalizedDYHand += coefficients[j] * derivativeCoefficient * Math.pow(polynomial.normalizeTime(x), j-i);
            }
         }
         else
         {
            generalizedDYHand = 0.0;
         }
         assertEquals(generalizedDYPoly, generalizedDYHand, EPSILON);
      }
   }

   public void compareXPowersDerivativesVector(YoOptimizedPolynomial polynomial, double x)
   {
      double[] coefficients = polynomial.getCoefficients();
      for(int i = 0; i < coefficients.length + 3; i++)
      {
         DenseMatrix64F generalizedDYPoly = new DenseMatrix64F(polynomial.getNumberOfCoefficients(), 1);
         polynomial.getXPowersDerivativeVector(i, x, generalizedDYPoly);
         DenseMatrix64F generalizedDYHand = new DenseMatrix64F(generalizedDYPoly.getNumRows(), generalizedDYPoly.getNumCols());
         if(i < coefficients.length)
         {
            for(int j = i; j < coefficients.length; j++)
            {
               double derivativeCoefficient = polynomial.getDerivativeCoefficient(i, j);
               generalizedDYHand.set(j, 0, derivativeCoefficient * Math.pow(polynomial.normalizeTime(x), j - i));
            }
         }
         for(int k = 0; k < coefficients.length; k++)
         {
            assertEquals(generalizedDYPoly.get(k, 0), generalizedDYHand.get(k, 0), EPSILON);
         }
      }
   }


   public void compareDerivativeVersions(YoOptimizedPolynomial polynomial, double x)
   {
      double[] coefficients = polynomial.getCoefficients();
      for(int i = 0; i < coefficients.length + 3; i++)
      {
         double generalizedDYPolyScalar = polynomial.getDerivative(i, x);
         double generalizedDYHandScalar = 0.0;

         DenseMatrix64F generalizedDYPolyVector = new DenseMatrix64F(polynomial.getNumberOfCoefficients(), 1);
         polynomial.getXPowersDerivativeVector(i, x, generalizedDYPolyVector);
         for(int j = 0; j < generalizedDYPolyVector.numRows; j++)
         {
            generalizedDYHandScalar += generalizedDYPolyVector.get(j,0) * coefficients[j];
         }
         assertEquals(generalizedDYPolyScalar, generalizedDYHandScalar, EPSILON);
      }
   }

}