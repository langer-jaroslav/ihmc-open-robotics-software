package us.ihmc.stateEstimation.humanoid;

import us.ihmc.euclid.tuple3D.interfaces.Tuple3DReadOnly;
import us.ihmc.euclid.tuple4D.interfaces.QuaternionReadOnly;
import us.ihmc.simulationconstructionset.util.RobotController;

public interface DRCStateEstimatorInterface extends RobotController
{
   public void initializeEstimatorToActual(Tuple3DReadOnly initialCoMPosition, QuaternionReadOnly initialEstimationLinkOrientation);
}
