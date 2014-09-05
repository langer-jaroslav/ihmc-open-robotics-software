package us.ihmc.commonWalkingControlModules.packetConsumers;

import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

public interface ChestOrientationProvider
{
   public abstract ReferenceFrame getChestOrientationExpressedInFrame();

   public abstract boolean isNewChestOrientationInformationAvailable();

   public abstract FrameOrientation getDesiredChestOrientation();

}