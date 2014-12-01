package us.ihmc.exampleSimulations.lidar;

import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.simulationconstructionset.CameraMount;
import us.ihmc.simulationconstructionset.GimbalJoint;
import us.ihmc.simulationconstructionset.Link;
import us.ihmc.simulationconstructionset.PinJoint;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.simulatedSensors.LidarMount;
import us.ihmc.utilities.Axis;
import us.ihmc.utilities.lidar.polarLidar.geometry.LidarScanParameters;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;

public class ExampleLidarRobot extends Robot
{
   private final LidarScanParameters lidarScanParameters;
   private final GimbalJoint gimbalJoint;

   public ExampleLidarRobot()
   {
      super("ExampleLidarRobot");

      double height = 0.2;
      double radius = 0.05;

      gimbalJoint = new GimbalJoint("gimbalZ", "gimbalX", "gimbalY", new Vector3d(0.0, 0.0, 1.0), this, Axis.Z, Axis.X, Axis.Y);
      Link link = new Link("lidar");
      link.setMassAndRadiiOfGyration(1.0, radius, radius, radius);
      Graphics3DObject linkGraphics = new Graphics3DObject();
      link.setLinkGraphics(linkGraphics);
      gimbalJoint.setLink(link);
      gimbalJoint.setDamping(1.0);

      CameraMount robotCam = new CameraMount("camera", new Vector3d(radius + 0.001, 0.0, height / 2.0), this);
      gimbalJoint.addCameraMount(robotCam);

      RigidBodyTransform transform = new RigidBodyTransform();
      transform.setTranslation(new Vector3d(radius + 0.001, 0.0, height / 2.0));
      lidarScanParameters = new LidarScanParameters(720, (float) (-Math.PI / 2), (float) (Math.PI / 2), 0f, 0.1f, 30.0f, 0f);
      LidarMount lidarMount = new LidarMount(transform, lidarScanParameters, "lidar");

      gimbalJoint.addLidarMount(lidarMount);

      linkGraphics.addModelFile("models/hokuyo.dae", YoAppearance.Black());
      linkGraphics.translate(0, 0, -0.1);
      link.setLinkGraphics(linkGraphics);
      this.addRootJoint(gimbalJoint);
   }

   public GimbalJoint getLidarZJoint()
   {
      return gimbalJoint;
   }

   public PinJoint getLidarXJoint()
   {
      return (PinJoint) gimbalJoint.getChildrenJoints().get(0);
   }

   public LidarScanParameters getLidarScanParameters()
   {
      return lidarScanParameters;
   }
}
