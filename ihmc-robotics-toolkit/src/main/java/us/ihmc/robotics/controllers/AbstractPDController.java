package us.ihmc.robotics.controllers;

import us.ihmc.robotics.geometry.AngleTools;
import us.ihmc.robotics.math.DeadbandTools;
import us.ihmc.yoVariables.providers.DoubleProvider;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

public abstract class AbstractPDController
{
   private final YoDouble positionError;
   private final YoDouble rateError;
   private final YoDouble actionP;
   private final YoDouble actionD;

   protected AbstractPDController(String suffix, YoRegistry registry)
   {
      positionError = new YoDouble("positionError_" + suffix, registry);
      positionError.set(0.0);

      rateError = new YoDouble("rateError_" + suffix, registry);
      rateError.set(0.0);

      actionP = new YoDouble("action_P_" + suffix, registry);
      actionP.set(0.0);

      actionD = new YoDouble("action_D_" + suffix, registry);
      actionD.set(0.0);
   }

   public abstract double getProportionalGain();

   public abstract double getDerivativeGain();

   public abstract double getPositionDeadband();

   public double getPositionError()
   {
      return positionError.getDoubleValue();
   }

   public double getRateError()
   {
      return rateError.getDoubleValue();
   }

   public double compute(double currentPosition, double desiredPosition, double currentRate, double desiredRate)
   {
      positionError.set(DeadbandTools.applyDeadband(getPositionDeadband(), desiredPosition - currentPosition));
      rateError.set(desiredRate - currentRate);

      actionP.set(getProportionalGain() * positionError.getDoubleValue());
      actionD.set(getDerivativeGain() * rateError.getDoubleValue());

      return actionP.getDoubleValue() + actionD.getDoubleValue();
   }

   public double computeForAngles(double currentPosition, double desiredPosition, double currentRate, double desiredRate)
   {
      //      System.out.println("PGain: " + proportionalGain.getDoubleValue() + "DGain: " + derivativeGain.getDoubleValue());
      this.positionError.set(DeadbandTools.applyDeadband(getPositionDeadband(), AngleTools.computeAngleDifferenceMinusPiToPi(desiredPosition, currentPosition)));
      rateError.set(desiredRate - currentRate);

      actionP.set(getProportionalGain() * positionError.getDoubleValue());
      actionD.set(getDerivativeGain() * rateError.getDoubleValue());

      return actionP.getDoubleValue() + actionD.getDoubleValue();
   }

   public static AbstractPDController createPDController(String suffix, DoubleProvider proportionalGain, DoubleProvider derivativeGain, DoubleProvider deadband,
                                                         YoRegistry registry)
   {
      return new AbstractPDController(suffix, registry)
      {
         @Override
         public double getProportionalGain()
         {
            return proportionalGain.getValue();
         }

         @Override
         public double getPositionDeadband()
         {
            return deadband.getValue();
         }

         @Override
         public double getDerivativeGain()
         {
            return derivativeGain.getValue();
         }
      };
   }
}
