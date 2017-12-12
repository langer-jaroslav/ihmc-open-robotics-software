package us.ihmc.wanderer.controlParameters;

import us.ihmc.commonWalkingControlModules.configurations.CoPPointName;
import us.ihmc.commonWalkingControlModules.configurations.ContinuousCMPICPPlannerParameters;
import us.ihmc.euclid.tuple2D.Vector2D;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** {@inheritDoc} */
public class WandererCapturePointPlannerParameters extends ContinuousCMPICPPlannerParameters
{
   private boolean runningOnRealRobot;
   // TODO Try using new ICP planner with two CMPs.
   private final boolean useTwoCMPsPerSupport;

   private EnumMap<CoPPointName, Vector2D> copOffsets;
   private EnumMap<CoPPointName, Vector2D> copForwardOffsetBounds;

   public WandererCapturePointPlannerParameters(boolean runningOnRealRobot)
   {
      this.runningOnRealRobot = runningOnRealRobot;
      useTwoCMPsPerSupport = true;
   }

   /** {@inheritDoc} */
   @Override
   public int getNumberOfCoPWayPointsPerFoot()
   {
      if (useTwoCMPsPerSupport)
         return 2;
      else
         return 1;
   }

   /**{@inheritDoc} */
   @Override
   public CoPPointName getExitCoPName()
   {
      return exitCoPName;
   }

   /**{@inheritDoc} */
   @Override
   public CoPPointName getEntryCoPName()
   {
      return entryCoPName;
   }

   /** {@inheritDoc} */
   @Override
   public EnumMap<CoPPointName, Vector2D> getCoPOffsetsInFootFrame()
   {
      if (copOffsets != null)
         return copOffsets;

      Vector2D entryOffset = new Vector2D(0.015, 0.005);
      Vector2D exitOffset = new Vector2D(0.015, 0.025);

      copOffsets = new EnumMap<>(CoPPointName.class);
      copOffsets.put(entryCoPName, entryOffset);
      copOffsets.put(exitCoPName, exitOffset);

      return copOffsets;
   }

   /** {@inheritDoc} */
   @Override
   public EnumMap<CoPPointName, Vector2D> getCoPForwardOffsetBoundsInFoot()
   {
      if (copForwardOffsetBounds != null)
         return copForwardOffsetBounds;

      Vector2D entryBounds = new Vector2D(0.0, 0.03);
      Vector2D exitBounds = new Vector2D(-0.04, 0.08);

      copForwardOffsetBounds = new EnumMap<>(CoPPointName.class);
      copForwardOffsetBounds.put(entryCoPName, entryBounds);
      copForwardOffsetBounds.put(exitCoPName, exitBounds);

      return copForwardOffsetBounds;
   }

   /** {@inheritDoc} */
   @Override
   public double getCoPSafeDistanceAwayFromSupportEdges()
   {
      return 0.02;//0.03
   }
}
