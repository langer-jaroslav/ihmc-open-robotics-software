package us.ihmc.commonWalkingControlModules.packetConsumers;

import java.util.concurrent.atomic.AtomicBoolean;

import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.listener.VariableChangedListener;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;

import com.google.common.util.concurrent.AtomicDouble;

public class UserDesiredHeadOrientationProvider implements HeadOrientationProvider
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final DoubleYoVariable userDesiredHeadPitch, userDesiredHeadYaw, userDesiredNeckPitch, userDesiredHeadRoll;
   private final ReferenceFrame headOrientationFrame;

   private final AtomicBoolean isNewHeadOrientationInformationAvailable = new AtomicBoolean(true);
   private final AtomicDouble desiredJointForExtendedNeckPitchRangeAngle = new AtomicDouble(0.0);
   private final FrameOrientation desiredHeadOrientation = new FrameOrientation();

   public UserDesiredHeadOrientationProvider(ReferenceFrame headOrientationFrame, YoVariableRegistry parentRegistry)
   {
      this.headOrientationFrame = headOrientationFrame;

      userDesiredHeadPitch = new DoubleYoVariable("userDesiredHeadPitch", registry);
      userDesiredHeadYaw = new DoubleYoVariable("userDesiredHeadYaw", registry);
      userDesiredNeckPitch = new DoubleYoVariable("userDesiredNeckPitch", registry);
      userDesiredHeadRoll = new DoubleYoVariable("userDesiredHeadRoll", registry);

      userDesiredHeadPitch.addVariableChangedListener(new VariableChangedListener()
      {
         public void variableChanged(YoVariable<?> v)
         {
            desiredJointForExtendedNeckPitchRangeAngle.set(userDesiredHeadPitch.getDoubleValue());
         }
      });

      setupListeners();

      parentRegistry.addChild(registry);
   }

   private void setupListeners()
   {
      VariableChangedListener variableChangedListener = new VariableChangedListener()
      {
         public void variableChanged(YoVariable<?> v)
         {
            isNewHeadOrientationInformationAvailable.set(true);
            desiredHeadOrientation.setIncludingFrame(headOrientationFrame, userDesiredHeadYaw.getDoubleValue(), userDesiredNeckPitch.getDoubleValue(),
                  userDesiredHeadRoll.getDoubleValue());
         }
      };

      userDesiredHeadPitch.addVariableChangedListener(variableChangedListener);
      userDesiredHeadYaw.addVariableChangedListener(variableChangedListener);
      userDesiredNeckPitch.addVariableChangedListener(variableChangedListener);
      userDesiredHeadRoll.addVariableChangedListener(variableChangedListener);

      variableChangedListener.variableChanged(null);
   }

   public double getDesiredExtendedNeckPitchJointAngle()
   {
      return desiredJointForExtendedNeckPitchRangeAngle.getAndSet(Double.NaN);
   }

   public boolean isNewHeadOrientationInformationAvailable()
   {
      return isNewHeadOrientationInformationAvailable.getAndSet(false);
   }

   public FrameOrientation getDesiredHeadOrientation()
   {
      return desiredHeadOrientation;
   }

   @Override
   public boolean isNewLookAtInformationAvailable()
   {
      return false;
   }

   @Override
   public FramePoint getLookAtPoint()
   {
      return null;
   }

   @Override
   public ReferenceFrame getHeadOrientationExpressedInFrame()
   {
      return headOrientationFrame;
   }
}
