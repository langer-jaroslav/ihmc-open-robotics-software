package us.ihmc.commonWalkingControlModules.dynamicPlanning;

import org.ejml.data.DenseMatrix64F;

/** This class defines the Linear Inverted Pendulum Dynamics.
 * The state variables are X = [x, y, z, xdot, ydot, zdot], the center of mass
 * position and velocity.
 * The control variables are U = [px, py, Fz], the CoP position and vertical force.
 */
public class SimpleLIPMDynamics implements DiscreteHybridDynamics<LIPMState>
{
   static final int stateVectorSize = 4;
   static final int controlVectorSize = 2;

   private double deltaT;
   private double deltaT2;
   private final double pendulumHeight;
   private final double gravityZ;

   public SimpleLIPMDynamics(double deltaT, double pendulumHeight, double gravityZ)
   {
      this.deltaT = deltaT;
      this.deltaT2 = deltaT * deltaT;
      this.pendulumHeight = pendulumHeight;
      this.gravityZ = gravityZ;
   }

   @Override
   public void setTimeStepSize(double deltaT)
   {
      this.deltaT = deltaT;
      deltaT2 = deltaT * deltaT;
   }

   public int getStateVectorSize()
   {
      return stateVectorSize;
   }

   public int getControlVectorSize()
   {
      return controlVectorSize;
   }

   /** Returns the current dynamics, of the form
    * X_k+1 = f(X_k, U_k)
    * @param hybridState state of the current dynamics. For LIPM, this doesn't change
    * @param currentState X_k in the above equation
    * @param currentControl U_k in the above equation
    * @param matrixToPack f(X_k, U_k) in the above equation
    */
   public void getNextState(LIPMState hybridState, DenseMatrix64F currentState, DenseMatrix64F currentControl, DenseMatrix64F matrixToPack)
   {
      if (matrixToPack.numRows != stateVectorSize)
         throw new RuntimeException("The state matrix size is wrong.");
      if (matrixToPack.numCols != 1)
         throw new RuntimeException("The state matrix size is wrong.");

      double x_k = currentState.get(0);
      double y_k = currentState.get(1);
      double xdot_k = currentState.get(2);
      double ydot_k = currentState.get(3);

      double px_k = currentControl.get(0);
      double py_k = currentControl.get(1);

      double x_k1 = x_k + deltaT * xdot_k + 0.5 * deltaT2 * (x_k - px_k) * gravityZ / pendulumHeight;
      double y_k1 = y_k + deltaT * ydot_k + 0.5 * deltaT2 * (y_k - py_k) * gravityZ / pendulumHeight;

      double xdot_k1 = xdot_k + deltaT * (x_k - px_k) * gravityZ / pendulumHeight;
      double ydot_k1 = ydot_k + deltaT * (y_k - py_k) * gravityZ / pendulumHeight;

      matrixToPack.set(0, 0, x_k1);
      matrixToPack.set(1, 0, y_k1);
      matrixToPack.set(2, 0, xdot_k1);
      matrixToPack.set(3, 0, ydot_k1);
   }

   public void getDynamicsStateGradient(LIPMState hybridState, DenseMatrix64F currentState, DenseMatrix64F currentControl, DenseMatrix64F matrixToPack)
   {
      if (matrixToPack.numRows != stateVectorSize)
         throw new RuntimeException("The state matrix size is wrong.");
      if (matrixToPack.numCols != stateVectorSize)
         throw new RuntimeException("The state matrix size is wrong.");

      matrixToPack.zero();

      double value1 = deltaT2 * gravityZ /  pendulumHeight;
      double value2 = deltaT * gravityZ / pendulumHeight;

      matrixToPack.set(0, 0, 1 + value1);
      matrixToPack.set(0, 2, deltaT);

      matrixToPack.set(1, 1, matrixToPack.get(0, 0));
      matrixToPack.set(1, 3, deltaT);

      matrixToPack.set(2, 0, value2);
      matrixToPack.set(2, 2, 1.0);

      matrixToPack.set(3, 1, matrixToPack.get(2, 0));
      matrixToPack.set(3, 3, 1.0);
   }

   public void getDynamicsControlGradient(LIPMState hybridState, DenseMatrix64F currentState, DenseMatrix64F currentControl, DenseMatrix64F matrixToPack)
   {
      if (matrixToPack.numRows != stateVectorSize)
         throw new RuntimeException("The state matrix size is wrong.");
      if (matrixToPack.numCols != controlVectorSize)
         throw new RuntimeException("The state matrix size is wrong.");

      matrixToPack.zero();

      double value2 = -deltaT2  * gravityZ / (2 * pendulumHeight);

      matrixToPack.set(0, 0, value2);
      matrixToPack.set(1, 1, value2);
   }

   public void getDynamicsStateHessian(LIPMState hybridState, int stateVariable, DenseMatrix64F currentState, DenseMatrix64F currentControl, DenseMatrix64F matrixToPack)
   {
      if (matrixToPack.numRows != stateVectorSize)
         throw new RuntimeException("The state matrix size is wrong.");
      if (matrixToPack.numCols != stateVectorSize)
         throw new RuntimeException("The state matrix size is wrong.");
      if (stateVariable >= stateVectorSize)
         throw new RuntimeException("Too big a state variable.");

      matrixToPack.zero();
   }

   public void getDynamicsControlHessian(LIPMState hybridState, int controlVariable, DenseMatrix64F currentState, DenseMatrix64F currentControl, DenseMatrix64F matrixToPack)
   {
      if (matrixToPack.numRows != stateVectorSize)
         throw new RuntimeException("The state matrix size is wrong.");
      if (matrixToPack.numCols != controlVectorSize)
         throw new RuntimeException("The state matrix size is wrong.");
      if (controlVariable >= controlVectorSize)
         throw new RuntimeException("Too big a state variable.");

      matrixToPack.zero();
   }

   /** f_ux */
   public void getDynamicsStateGradientOfControlGradient(LIPMState hybridState, int stateVariable, DenseMatrix64F currentState, DenseMatrix64F currentControl, DenseMatrix64F matrixToPack)
   {
      if (matrixToPack.numRows != stateVectorSize)
         throw new RuntimeException("The state matrix size is wrong.");
      if (matrixToPack.numCols != controlVectorSize)
         throw new RuntimeException("The state matrix size is wrong.");
      if (stateVariable >= stateVectorSize)
         throw new RuntimeException("Too big a state variable.");

      matrixToPack.zero();
   }

   /** f_xu */
   public void getDynamicsControlGradientOfStateGradient(LIPMState hybridState, int controlVariable, DenseMatrix64F currentState, DenseMatrix64F currentControl, DenseMatrix64F matrixToPack)
   {
      if (matrixToPack.numRows != stateVectorSize)
         throw new RuntimeException("The state matrix size is wrong.");
      if (matrixToPack.numCols != stateVectorSize)
         throw new RuntimeException("The state matrix size is wrong.");
      if (controlVariable >= controlVectorSize)
         throw new RuntimeException("Too big a control variable.");

      matrixToPack.zero();
   }

   public void getContinuousAMatrix(DenseMatrix64F matrixToPack)
   {
      matrixToPack.set(0, 2, 1.0);
      matrixToPack.set(1, 3, 1.0);
      matrixToPack.set(2, 0, gravityZ / pendulumHeight);
      matrixToPack.set(3, 1, gravityZ / pendulumHeight);
   }

   public void getContinuousBMatrix(DenseMatrix64F matrixToPack)
   {
      matrixToPack.set(2, 0, -gravityZ / pendulumHeight);
      matrixToPack.set(3, 1, -gravityZ / pendulumHeight);
   }
}
