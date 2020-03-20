package us.ihmc.commonWalkingControlModules.controlModules.foot.partialFoothold;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoPlaneContactState;
import us.ihmc.euclid.referenceFrame.interfaces.FrameConvexPolygon2DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameLine2DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint2DReadOnly;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.mecano.frames.MovingReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoEnum;

import java.awt.*;
import java.util.EnumMap;
import java.util.List;

public class PartialFootholdCropperModule
{
   private enum RotationDetectorType
   {
      GEOMETRIC, KINEMATIC, VELOCITY, ANY, KINEMATIC_AND_VELOCITY;

      static final RotationDetectorType[] values = {/*GEOMETRIC,*/ KINEMATIC, VELOCITY};
   }

   private final YoEnum<RotationDetectorType> rotationDetectorType;
   private final RotationEdgeCalculator copHistoryEdgeCalculator;
   private final CoPAndVelocityRotationEdgeCalculator copAndVelocityEdgeCalculator;
   private final EnumMap<RotationDetectorType, FootRotationDetector> rotationDetectors = new EnumMap<>(RotationDetectorType.class);
   private final FootholdCropper footholdCropper;
   private final CropVerifier cropVerifier;

   private final YoBoolean isRotating;
   private final YoBoolean isEdgeStable;
   private final YoBoolean shouldShrinkFoothold;

   private final EdgeVisualizer edgeVisualizer;

   public PartialFootholdCropperModule(RobotSide side,
                                       MovingReferenceFrame soleFrame,
                                       List<? extends FramePoint2DReadOnly> defaultContactPoints,
                                       FootholdRotationParameters rotationParameters,
                                       double dt,
                                       YoVariableRegistry parentRegistry,
                                       YoGraphicsListRegistry graphicsRegistry)
   {
      YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName() + side.getPascalCaseName());
      parentRegistry.addChild(registry);

      isRotating = new YoBoolean(side.getLowerCaseName() + "IsRotating", registry);
      isEdgeStable = new YoBoolean(side.getLowerCaseName() + "IsEdgeStable", registry);
      shouldShrinkFoothold = new YoBoolean(side.getLowerCaseName() + "ShouldShrinkFoothold", registry);


      copHistoryEdgeCalculator = new CoPHistoryRotationEdgeCalculator(side, soleFrame, rotationParameters, dt, registry, Color.BLUE, graphicsRegistry);
      copAndVelocityEdgeCalculator = new CoPAndVelocityRotationEdgeCalculator(side, soleFrame, rotationParameters, dt, registry, Color.GRAY, graphicsRegistry);

      FootRotationDetector velocityRotationDetector = new VelocityFootRotationDetector(side, soleFrame, rotationParameters, dt, registry);
      FootRotationDetector kinematicRotationDetector = new KinematicFootRotationDetector(side, soleFrame, rotationParameters, dt, registry);
      rotationDetectors.put(RotationDetectorType.KINEMATIC, kinematicRotationDetector);
      rotationDetectors.put(RotationDetectorType.VELOCITY, velocityRotationDetector);

      rotationDetectorType = YoEnum.create(side.getCamelCaseName() + "RotationDetectorType", RotationDetectorType.class, registry);

      rotationDetectorType.set(RotationDetectorType.KINEMATIC_AND_VELOCITY);

//      if (graphicsRegistry != null)
//         edgeVisualizer = new EdgeVisualizer(side.getLowerCaseName(), Color.RED, registry, graphicsRegistry);
//      else
         edgeVisualizer = null;

      cropVerifier = new CropVerifier(side.getLowerCaseName(), soleFrame, 0.005, rotationParameters, registry, graphicsRegistry);
      footholdCropper = new FootholdCropper(side.getLowerCaseName(), soleFrame, defaultContactPoints, rotationParameters, dt, registry, graphicsRegistry);

      reset();
   }

   public void compute(FramePoint2DReadOnly measuredCoP, FramePoint2DReadOnly desiredCoP)
   {
      boolean wasRotating = isRotating.getBooleanValue();
      isRotating.set(computeIsRotating());
      shouldShrinkFoothold.set(false);

      cropVerifier.update(desiredCoP);
      footholdCropper.update(measuredCoP);

      if (!isRotating.getBooleanValue())
      {
         if (wasRotating)
            resetEdgeCalculators();
         isEdgeStable.set(false);
         return;
      }

      FrameLine2DReadOnly lineOfRotation = computeLineOfRotation(measuredCoP);
      isEdgeStable.set(lineOfRotation != null);
      if (!isEdgeStable.getBooleanValue())
         return;

      if (edgeVisualizer != null)
      {
         edgeVisualizer.visualize(true);
         edgeVisualizer.updateGraphics(lineOfRotation);
      }

      RobotSide sideToCrop = footholdCropper.computeSideToCrop(lineOfRotation);
      if (sideToCrop != null)
      {
         shouldShrinkFoothold.set(cropVerifier.verifyFootholdCrop(desiredCoP, sideToCrop, lineOfRotation));
         if (shouldShrinkFoothold.getBooleanValue())
            footholdCropper.computeShrunkenFoothold(lineOfRotation, sideToCrop);
      }
   }

   public boolean isRotating()
   {
      return isRotating.getBooleanValue();
   }

   public boolean applyShrunkenFoothold(YoPlaneContactState contactStateToModify)
   {
      if (!shouldApplyShrunkenFoothold())
         return false;

      return footholdCropper.applyShrunkenFoothold(contactStateToModify);
   }

   boolean shouldApplyShrunkenFoothold()
   {
      if (!shouldShrinkFoothold.getBooleanValue())
         return false;

      return footholdCropper.shouldApplyShrunkenFoothold();
   }

   public FrameConvexPolygon2DReadOnly getShrunkenFootPolygon()
   {
      return footholdCropper.getShrunkenFootPolygon();
   }

   private FrameLine2DReadOnly computeLineOfRotation(FramePoint2DReadOnly measuredCoP)
   {
      copHistoryEdgeCalculator.compute(measuredCoP);
      copAndVelocityEdgeCalculator.compute(measuredCoP);

      return getLineOfRotation();
   }

   public FrameLine2DReadOnly getLineOfRotation()
   {
      if (copHistoryEdgeCalculator.isRotationEdgeTrusted())
         return copHistoryEdgeCalculator.getLineOfRotation();
      if (copAndVelocityEdgeCalculator.isRotationEdgeTrusted())
         return copAndVelocityEdgeCalculator.getLineOfRotation();
      else
         return null;
   }

   private boolean computeIsRotating()
   {
      if (rotationDetectorType.getEnumValue() == RotationDetectorType.ANY)
      {
         boolean rotationDetected = false;
         for (RotationDetectorType type : RotationDetectorType.values)
         {
            if (rotationDetectors.get(type).compute())
               rotationDetected = true;
         }

         return rotationDetected;
      }
      else if (rotationDetectorType.getEnumValue() == RotationDetectorType.KINEMATIC_AND_VELOCITY)
      {
         boolean rotationDetected = rotationDetectors.get(RotationDetectorType.KINEMATIC).compute();
         rotationDetected |= rotationDetectors.get(RotationDetectorType.VELOCITY).compute();
         return rotationDetected;
      }
      else
      {
         return rotationDetectors.get(rotationDetectorType.getEnumValue()).compute();
      }
   }

   private void resetRotationDetectors()
   {
      for (RotationDetectorType type : RotationDetectorType.values)
         rotationDetectors.get(type).reset();
   }

   private void resetEdgeCalculators()
   {
      if (edgeVisualizer != null)
      {
         edgeVisualizer.visualize(false);
         edgeVisualizer.reset();
      }

      copAndVelocityEdgeCalculator.reset();
      copHistoryEdgeCalculator.reset();
   }

   public void initialize(FrameConvexPolygon2DReadOnly footPolygon)
   {
      cropVerifier.initialize();
      footholdCropper.reset(footPolygon);
   }

   public void reset()
   {
      isRotating.set(false);
      isEdgeStable.set(false);
      resetRotationDetectors();
      resetEdgeCalculators();
      cropVerifier.reset();
      footholdCropper.reset();
   }
}
