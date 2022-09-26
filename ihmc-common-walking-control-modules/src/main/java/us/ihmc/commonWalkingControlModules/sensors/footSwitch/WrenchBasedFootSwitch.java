package us.ihmc.commonWalkingControlModules.sensors.footSwitch;

import java.util.List;

import us.ihmc.commonWalkingControlModules.controlModules.CenterOfPressureResolver;
import us.ihmc.commonWalkingControlModules.desiredFootStep.DesiredFootstepCalculatorTools;
import us.ihmc.commons.MathTools;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.mecano.frames.MovingReferenceFrame;
import us.ihmc.mecano.spatial.Wrench;
import us.ihmc.mecano.yoVariables.spatial.YoFixedFrameSpatialVector;
import us.ihmc.robotics.contactable.ContactablePlaneBody;
import us.ihmc.robotics.math.filters.AlphaFilteredYoVariable;
import us.ihmc.robotics.math.filters.GlitchFilteredYoBoolean;
import us.ihmc.robotics.sensors.FootSwitchInterface;
import us.ihmc.robotics.sensors.ForceSensorDataReadOnly;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFramePoint2D;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFrameVector3D;
import us.ihmc.yoVariables.providers.DoubleProvider;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

//TODO Probably make an EdgeSwitch interface that has all the HeelSwitch and ToeSwitch stuff
public class WrenchBasedFootSwitch implements FootSwitchInterface
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private static final double MIN_FORCE_TO_COMPUTE_COP = 5.0;

   private final DoubleProvider contactForceThreshold1;
   private final DoubleProvider contactForceThreshold2;
   private final DoubleProvider contactCoPThreshold;

   private final YoRegistry registry;

   private final ForceSensorDataReadOnly forceSensorData;

   private final YoBoolean isPastForceThreshold1;
   private final GlitchFilteredYoBoolean isPastForceThreshold1Filtered;
   private final YoBoolean isPastForceThreshold2;
   private final YoBoolean hasFootHitGround, isPastCoPThreshold;
   private final GlitchFilteredYoBoolean hasFootHitGroundFiltered;
   private final GlitchFilteredYoBoolean isPastCoPThresholdFiltered;

   private final YoDouble footForceMagnitude;
   private final YoDouble alphaFootLoadFiltering;
   private final AlphaFilteredYoVariable footLoadPercentage;

   private final Wrench footWrench;

   private final YoFramePoint2D centerOfPressure;
   private final CenterOfPressureResolver copResolver = new CenterOfPressureResolver();
   private final ContactablePlaneBody contactablePlaneBody;
   private final double footLength;
   private final double footMinX;
   private final double footMaxX;

   private final YoFixedFrameSpatialVector yoFootForceTorque;
   private final YoFixedFrameSpatialVector yoFootForceTorqueInFoot;
   private final YoFixedFrameSpatialVector yoFootForceTorqueInWorld;

   private final double robotTotalWeight;

   /**
    * @param namePrefix             prefix to use for naming the internal {@code YoVariable}s.
    * @param forceSensorData        the port to reading the sensor measurement.
    * @param robotTotalWeight       the robot weight used to compute the load distribution on this
    *                               foot.
    * @param contactablePlaneBody   the contactable plane body of this foot, use to get the foot length
    *                               and sole frame.
    * @param contactForceThreshold1 the first force threshold. The foot is considered to have hit the
    *                               ground if this threshold is met <b>and</b> the CoP threshold is
    *                               met.
    * @param contactForceThreshold2 the second force threshold. The foot is considered to have hit the
    *                               ground if this threshold is met.
    * @param contactCoPThreshold    the center of pressure threshold. Expressed in percentage of foot
    *                               length, this represents the margin away from the toe/heel line that
    *                               the CoP needs to pass in order to consider that the foot has hit
    *                               the ground.
    * @param yoGraphicsListRegistry
    * @param parentRegistry
    */
   public WrenchBasedFootSwitch(String namePrefix,
                                ForceSensorDataReadOnly forceSensorData,
                                double robotTotalWeight,
                                ContactablePlaneBody contactablePlaneBody,
                                DoubleProvider contactForceThreshold1,
                                DoubleProvider contactForceThreshold2,
                                DoubleProvider contactCoPThreshold,
                                YoGraphicsListRegistry yoGraphicsListRegistry,
                                YoRegistry parentRegistry)
   {
      this.forceSensorData = forceSensorData;
      this.robotTotalWeight = robotTotalWeight;
      this.contactablePlaneBody = contactablePlaneBody;
      this.contactForceThreshold1 = contactForceThreshold1;
      this.contactForceThreshold2 = contactForceThreshold2;
      this.contactCoPThreshold = contactCoPThreshold;

      registry = new YoRegistry(namePrefix + getClass().getSimpleName());

      ReferenceFrame measurementFrame = forceSensorData.getMeasurementFrame();
      MovingReferenceFrame parentFrame = contactablePlaneBody.getFrameAfterParentJoint();
      yoFootForceTorque = new YoFixedFrameSpatialVector(new YoFrameVector3D(namePrefix + "Torque", measurementFrame, registry),
                                                        new YoFrameVector3D(namePrefix + "Force", measurementFrame, registry));
      yoFootForceTorqueInFoot = new YoFixedFrameSpatialVector(new YoFrameVector3D(namePrefix + "TorqueFootFrame", parentFrame, registry),
                                                              new YoFrameVector3D(namePrefix + "ForceFootFrame", parentFrame, registry));
      yoFootForceTorqueInWorld = new YoFixedFrameSpatialVector(new YoFrameVector3D(namePrefix + "TorqueWorldFrame", worldFrame, registry),
                                                               new YoFrameVector3D(namePrefix + "ForceWorldFrame", worldFrame, registry));

      footForceMagnitude = new YoDouble(namePrefix + "FootForceMag", registry);

      isPastForceThreshold1 = new YoBoolean(namePrefix + "IsPastForceThreshold1", registry);
      isPastForceThreshold1Filtered = new GlitchFilteredYoBoolean(namePrefix + "IsPastForceThreshold1Filtered", registry, isPastForceThreshold1, 2);
      isPastForceThreshold2 = new YoBoolean(namePrefix + "IsPastForceThreshold2", registry);
      isPastCoPThreshold = new YoBoolean(namePrefix + "IsPastCoPThreshold", registry);
      isPastCoPThresholdFiltered = new GlitchFilteredYoBoolean(namePrefix + "IsPastCoPThresholdFiltered", registry, isPastCoPThreshold, 3);

      hasFootHitGround = new YoBoolean(namePrefix + "FootHitGround", registry);
      // Final variable to identify if the foot has hit the ground
      hasFootHitGroundFiltered = new GlitchFilteredYoBoolean(namePrefix + "HasFootHitGroundFiltered", registry, hasFootHitGround, 2);

      alphaFootLoadFiltering = new YoDouble(namePrefix + "AlphaFootLoadFiltering", registry);
      alphaFootLoadFiltering.set(0.1);
      footLoadPercentage = new AlphaFilteredYoVariable(namePrefix + "FootLoadPercentage", registry, alphaFootLoadFiltering);

      centerOfPressure = new YoFramePoint2D(namePrefix + "CenterOfPressure", "", contactablePlaneBody.getSoleFrame(), registry);

      footWrench = new Wrench(measurementFrame, (ReferenceFrame) null);

      footMinX = computeMinX(contactablePlaneBody);
      footMaxX = computeMaxX(contactablePlaneBody);
      footLength = computeLength(contactablePlaneBody);

      parentRegistry.addChild(registry);
   }

   @Override
   public void update()
   {
      forceSensorData.getWrench(footWrench);

      yoFootForceTorque.set(footWrench);
      yoFootForceTorqueInFoot.setMatchingFrame(footWrench);
      yoFootForceTorqueInWorld.setMatchingFrame(footWrench);

      footForceMagnitude.set(footWrench.getLinearPart().norm());

      // Using the force in foot frame to ensure z is up when the foot is flat.
      // Sometimes the sensor can be mounted such that z is down.
      double forceZUp = yoFootForceTorqueInFoot.getLinearPartZ();

      double fZPlus = MathTools.clamp(forceZUp, 0.0, Double.POSITIVE_INFINITY);
      footLoadPercentage.update(fZPlus / robotTotalWeight);

      isPastForceThreshold1.set(forceZUp > contactForceThreshold1.getValue());
      isPastForceThreshold1Filtered.update();

      if (contactForceThreshold2 != null)
      {
         isPastForceThreshold2.set(forceZUp > contactForceThreshold2.getValue());
      }
      else
      {
         isPastForceThreshold2.set(false);
      }

      // Computing Center of Pressure
      if (fZPlus < MIN_FORCE_TO_COMPUTE_COP)
         centerOfPressure.setToNaN();
      else
         copResolver.resolveCenterOfPressureAndNormalTorque(centerOfPressure, footWrench, contactablePlaneBody.getSoleFrame());

      // Testing CoP threshold
      if (Double.isNaN(contactCoPThreshold.getValue()))
      {
         isPastCoPThreshold.set(true);
         isPastCoPThresholdFiltered.set(true);
      }
      else
      {
         /*
          * FIXME If we wanted to do the CoP filter properly, we should use make a ConvexPolygon2D from the
          * ContactablePlaneBody points and assert that the CoP is inside the polygon at a min distance from
          * the perimeter.
          */
         double copThreshold = contactCoPThreshold.getValue() * footLength;
         double minThresholdX = (footMinX + copThreshold);
         double maxThresholdX = (footMaxX - copThreshold);
         isPastCoPThreshold.set(centerOfPressure.getX() >= minThresholdX && centerOfPressure.getX() <= maxThresholdX);
         isPastCoPThresholdFiltered.update();
      }

      hasFootHitGround.set((isPastForceThreshold1Filtered.getValue() && isPastCoPThresholdFiltered.getValue()) || isPastForceThreshold2.getValue());
      hasFootHitGroundFiltered.update();

   }

   @Override
   public boolean hasFootHitGround()
   {
      return hasFootHitGroundFiltered.getValue();
   }

   @Override
   public void reset()
   {
      isPastCoPThresholdFiltered.set(false);
   }

   @Override
   public double computeFootLoadPercentage()
   {
      return footLoadPercentage.getDoubleValue();
   }

   @Override
   public void computeAndPackFootWrench(Wrench footWrenchToPack)
   {
      footWrenchToPack.setIncludingFrame(footWrench);
   }

   @Override
   public ReferenceFrame getMeasurementFrame()
   {
      return forceSensorData.getMeasurementFrame();
   }

   @Override
   public void computeAndPackCoP(FramePoint2D copToPack)
   {
      copToPack.setIncludingFrame(centerOfPressure);
   }

   @Override
   public void updateCoP()
   {
   }

   private static double computeLength(ContactablePlaneBody contactablePlaneBody)
   {
      FrameVector3D forward = new FrameVector3D(contactablePlaneBody.getSoleFrame(), 1.0, 0.0, 0.0);
      List<FramePoint3D> maxForward = DesiredFootstepCalculatorTools.computeMaximumPointsInDirection(contactablePlaneBody.getContactPointsCopy(), forward, 1);

      FrameVector3D back = new FrameVector3D(contactablePlaneBody.getSoleFrame(), -1.0, 0.0, 0.0);
      List<FramePoint3D> maxBack = DesiredFootstepCalculatorTools.computeMaximumPointsInDirection(contactablePlaneBody.getContactPointsCopy(), back, 1);

      return maxForward.get(0).getX() - maxBack.get(0).getX();
   }

   private static double computeMinX(ContactablePlaneBody contactablePlaneBody)
   {
      FrameVector3D back = new FrameVector3D(contactablePlaneBody.getSoleFrame(), -1.0, 0.0, 0.0);
      List<FramePoint3D> maxBack = DesiredFootstepCalculatorTools.computeMaximumPointsInDirection(contactablePlaneBody.getContactPointsCopy(), back, 1);

      return maxBack.get(0).getX();
   }

   private static double computeMaxX(ContactablePlaneBody contactablePlaneBody)
   {
      FrameVector3D front = new FrameVector3D(contactablePlaneBody.getSoleFrame(), 1.0, 0.0, 0.0);
      List<FramePoint3D> maxFront = DesiredFootstepCalculatorTools.computeMaximumPointsInDirection(contactablePlaneBody.getContactPointsCopy(), front, 1);

      return maxFront.get(0).getX();
   }

   @Override
   public boolean getForceMagnitudePastThreshhold()
   {
      return isPastForceThreshold1.getBooleanValue();
   }

   public ContactablePlaneBody getContactablePlaneBody()
   {
      return contactablePlaneBody;
   }

   public YoRegistry getRegistry()
   {
      return registry;
   }
}
