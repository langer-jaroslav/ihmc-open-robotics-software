package us.ihmc.commonWalkingControlModules.capturePoint.controller;

import us.ihmc.commonWalkingControlModules.capturePoint.ICPControlGainsReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameConvexPolygon2DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint2DReadOnly;
import us.ihmc.yoVariables.providers.DoubleProvider;

/**
 * Parameters to tune the ICP Optimization based controller for each robot. The ICP Optimization
 * based controller encodes the ICP plan based on the upcoming footsteps, and can either do control
 * with adjusting the feet or without adjusting the feet, using a feedback-based convex
 * optimization.
 */
public abstract class ICPControllerParameters
{
   /**
    * The weight for tracking the nominal desired CMP. This weight penalizes using a large amount of
    * CMP control. Setting this weight high will make the robot behave similar to using point feet
    * control / minimal ankle torques and angular momentum.
    */
   public abstract double getFeedbackForwardWeight();

   /**
    * The weight for tracking the nominal desired CMP. This weight penalizes using a large amount of
    * CMP control. Setting this weight high will make the robot behave similar to using point feet
    * control / minimal ankle torques and angular momentum.
    */
   public abstract double getFeedbackLateralWeight();

   /**
    * Penalization on changes in the feedback CoP and CMP between control ticks. This weight is
    * normalized by the control DT.
    */
   public double getCoPCMPFeedbackRateWeight()
   {
      return 0.0;
   }

   /**
    * Penalization on changes in the total feedback between control ticks. This weight is normalized by
    * the control DT.
    */
   public abstract double getFeedbackRateWeight();

   /**
    * Gains for the proportional ICP controller that is encoded into the optimization. Also includes
    * gains for the smart integrator that is used when the controller is stuck.
    */
   public abstract ICPControlGainsReadOnly getICPFeedbackGains();

   /**
    * Sets whether the integration gains returned by {@link #getICPFeedbackGains()} is used to perform
    * a smart integration when the robot is stuck.
    */
   public boolean useSmartICPIntegrator()
   {
      return false;
   }

   /**
    * Sets the maximum ICP velocity for it to be considered "stuck".
    */
   public double getICPVelocityThresholdForStuck()
   {
      return 0.01;
   }

   /**
    * Weight on the slack variable introduced for the ICP dynamics. This slack variable is required for
    * the CoP to be constrained inside the support polygon when not using step adjustment, and the step
    * lengths to be constrained when allowing step adjustment.
    */
   public abstract double getDynamicsObjectiveWeight();

   /**
    * Weight on the use of angular momentum minimization. This is only utilized when it is specified to
    * use angular momentum in the feedback controller.
    */
   public abstract double getAngularMomentumMinimizationWeight();

   /**
    * Enabling this boolean causes the {@link #getFeedbackForwardWeight()} and
    * {@link #getFeedbackLateralWeight()} to be decreased with an increasing feedback weight. This
    * allows tuning of the tendency to use feedback vs. step adjustment to be separated from the
    * feedback controller.
    */
   public abstract boolean scaleFeedbackWeightWithGain();

   /**
    * Enabling this boolean allows modifying the CMP offset from the CoP in the optimization.
    */
   public boolean useCMPFeedback()
   {
      return true;
   }

   /**
    * Enabling this boolean allows the CMP to exit the support polygon. The CoP will still be
    * constrained to lie inside the support polygon, however.
    */
   public abstract boolean useAngularMomentum();

   public double getFeedbackDirectionWeight()
   {
      return 0.0;
   }

   /**
    * Sets the minimum distance inside the support polygon for the CoP to be located.
    */
   public double getSafeCoPDistanceToEdge()
   {
      return 0.002;
   }

   /**
    * Specifies whether or not to use the ICP control polygon for the CMP, rather than the actual
    * support polygon.
    */
   public boolean getUseICPControlPolygons()
   {
      return true;
   }

   /**
    * Specifies whether or not to use the HeuristicICPController instead of the optimization based
    * ICPController.
    */
   public boolean getUseHeuristicICPController()
   {
      return false;
   }

   /**
    * Threshold for when to ignore the feedforward ICP controller, specified by the perfect CoP and
    * CMP, and instead just use feedback and ignore the feedforward because the error is so large that
    * the feedforward is probably fairly irrelevant at that high of an error.
    */
   public double getPureFeedbackErrorThreshold()
   {
      return 0.06;
   }

   /**
    * Minimum distance that the CMP can get towards the ICP when being projected into the foot, away
    * from an edge. Once the CoP is inside the foot, and once the CMP gets this close to the ICP, do
    * not push the CMP any closer towards the ICP.
    */
   public double getMinICPPushDelta()
   {
      return 0.05;
   }

   /**
    * The maximum distance to project the CoP into the foot, away from the edge. Once it is projected
    * this far, then do not push it any further inside the foot.
    */
   public double getMaxCoPProjectionInside()
   {
      return 0.04;
   }

   public DoubleProvider getFeedForwardAlphaCalculator()
   {
      return null;
   }

   public FeedbackAlphaCalculator getFeedbackAlphaCalculator()
   {
      return null;
   }

   public interface FeedbackAlphaCalculator
   {
      double computeAlpha(FramePoint2DReadOnly currentICP, FrameConvexPolygon2DReadOnly supportPolygon);
   }
}
