package us.ihmc.robotics.linearAlgebra;

import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class DampedLeastSquaresNullspaceCalculatorTest extends DampedNullspaceCalculatorTest
{
   @Override
   public DampedNullspaceCalculator getDampedNullspaceProjectorCalculator()
   {
      return new DampedLeastSquaresNullspaceCalculator(10, 0.0, new YoVariableRegistry(getClass().getSimpleName()));
   }

   @Override
   public NullspaceCalculator getNullspaceProjectorCalculator()
   {
      return getDampedNullspaceProjectorCalculator();
   }
}
