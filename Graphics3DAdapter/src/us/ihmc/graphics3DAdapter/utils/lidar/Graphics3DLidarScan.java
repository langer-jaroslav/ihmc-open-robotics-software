package us.ihmc.graphics3DAdapter.utils.lidar;

import us.ihmc.utilities.math.geometry.Transform3d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import us.ihmc.graphics3DAdapter.Graphics3DWorld;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNodeType;
import us.ihmc.utilities.Axis;
import us.ihmc.utilities.lidar.polarLidar.LidarScan;
import us.ihmc.utilities.math.geometry.Sphere3d;

public class Graphics3DLidarScan
{
   private static final double SPHERE_RADIUS = 0.005;

   private Graphics3DWorld world;
   private String lidarName;
   private int scansPerSweep;
   private double minRange;
   private double maxRange;
   private AppearanceDefinition appearance;
   private boolean showScanRays;
   private boolean showScanPoints;

   private Graphics3DNode[] points;
   private Graphics3DNode[] rays;

   public Graphics3DLidarScan(Graphics3DWorld world, String lidarName, int scansPerSweep, double minRange, double maxRange, boolean showScanRays,
                             boolean showScanPoints, AppearanceDefinition appearance)
   {
      this.world = world;
      this.lidarName = lidarName;
      this.scansPerSweep = scansPerSweep;
      this.minRange = minRange;
      this.maxRange = maxRange;
      this.showScanRays = showScanRays;
      this.showScanPoints = showScanPoints;
      this.appearance = appearance;

      init();
   }

   private void init()
   {
      if (showScanPoints)
      {
         createPoints();
         world.addAllChildren(points);
      }

      if (showScanRays)
      {
         createRays();
         world.addAllChildren(rays);
      }
   }

   public void update(LidarScan lidarScan)
   {
      for (int i = 0; (i < lidarScan.size()) && (i < scansPerSweep); i++)
      {
         if (showScanPoints)
         {
            if ((lidarScan.getRange(i) < minRange) || (lidarScan.getRange(i) > maxRange))
            {
               points[i].setTransform(new Transform3d(new Matrix3d(), new Vector3d(), 0));
            }
            else
            {
               Transform3d pointTransform = new Transform3d();

               Point3d p = new Point3d(lidarScan.getRange(i) + (SPHERE_RADIUS * 1.1), 0.0, 0.0);
               Transform3d transform = new Transform3d();
               lidarScan.packInterpolatedTransform(i, transform);
               transform.mul(lidarScan.getSweepTransform(i));
               transform.transform(p);

               pointTransform.setTranslation(new Vector3f(p));

               points[i].setTransform(pointTransform);
            }
         }

         if (showScanRays)
         {
            Transform3d rayTransform = new Transform3d();
            lidarScan.packInterpolatedTransform(i, rayTransform);
            rayTransform.mul(lidarScan.getSweepTransform(i));
            rays[i].setTransform(rayTransform);
         }
      }
   }

   private void createPoints()
   {
      points = new Graphics3DNode[scansPerSweep];

      for (int i = 0; i < scansPerSweep; i++)
      {
         points[i] = new Graphics3DNode(lidarName + "point" + i, Graphics3DNodeType.VISUALIZATION,
                                        new Graphics3DObject(new Sphere3d(SPHERE_RADIUS), appearance));
      }
   }

   private void createRays()
   {
      rays = new Graphics3DNode[scansPerSweep];

      for (int i = 0; i < scansPerSweep; i++)
      {
         Graphics3DObject rayObject = new Graphics3DObject();
         rayObject.rotate(Math.PI / 2, Axis.Y);
         rayObject.addCylinder(1.0, 0.0005, appearance);

         rays[i] = new Graphics3DNode(lidarName + "ray" + i, Graphics3DNodeType.VISUALIZATION, rayObject);
      }
   }
}
