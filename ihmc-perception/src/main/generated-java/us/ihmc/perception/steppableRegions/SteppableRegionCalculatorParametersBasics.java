package us.ihmc.perception.steppableRegions;

import us.ihmc.tools.property.StoredPropertySetBasics;

/**
 * This class was auto generated. Do not edit by hand. Edit the cooresponding JSON file
 * and run the main in super to regenerate.
 */
public interface SteppableRegionCalculatorParametersBasics extends SteppableRegionCalculatorParametersReadOnly, StoredPropertySetBasics
{
   default void setDistanceFromCliffBottoms(double distanceFromCliffBottoms)
   {
      set(SteppableRegionCalculatorParameters.distanceFromCliffBottoms, distanceFromCliffBottoms);
   }

   default void setDistanceFromCliffTops(double distanceFromCliffTops)
   {
      set(SteppableRegionCalculatorParameters.distanceFromCliffTops, distanceFromCliffTops);
   }

   default void setYawDiscretizations(int yawDiscretizations)
   {
      set(SteppableRegionCalculatorParameters.yawDiscretizations, yawDiscretizations);
   }

   default void setFootWidth(double footWidth)
   {
      set(SteppableRegionCalculatorParameters.footWidth, footWidth);
   }

   default void setFootLength(double footLength)
   {
      set(SteppableRegionCalculatorParameters.footLength, footLength);
   }

   default void setCliffStartHeightToAvoid(double cliffStartHeightToAvoid)
   {
      set(SteppableRegionCalculatorParameters.cliffStartHeightToAvoid, cliffStartHeightToAvoid);
   }

   default void setCliffEndHeightToAvoid(double cliffEndHeightToAvoid)
   {
      set(SteppableRegionCalculatorParameters.cliffEndHeightToAvoid, cliffEndHeightToAvoid);
   }
}
