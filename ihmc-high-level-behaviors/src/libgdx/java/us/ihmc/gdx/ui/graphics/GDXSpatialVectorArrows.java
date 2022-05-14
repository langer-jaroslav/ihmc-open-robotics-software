package us.ihmc.gdx.ui.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.gdx.tools.GDXTools;

public class GDXSpatialVectorArrows
{
   private final GDXArrowGraphic linearPartArrow = new GDXArrowGraphic(Color.RED);
   private final GDXArrowGraphic angularPartArrow = new GDXArrowGraphic(Color.PURPLE);

   private final RigidBodyTransform tempTransform = new RigidBodyTransform();
   private final FramePoint3D origin = new FramePoint3D();
   private final ReferenceFrame originFrame;
   private final int indexOfSensor;

   public GDXSpatialVectorArrows(ReferenceFrame originFrame, int indexOfSensor)
   {
      this.originFrame = originFrame;
      this.indexOfSensor = indexOfSensor;
   }

   public void update(Vector3DReadOnly linearPart, Vector3DReadOnly angularPart)
   {
      origin.setToZero(originFrame);
      origin.changeFrame(ReferenceFrame.getWorldFrame());

      transform(linearPartArrow, linearPart, 0.005);
      transform(angularPartArrow, angularPart, 0.02);
   }

   private void transform(GDXArrowGraphic arrowGraphic, Vector3DReadOnly vector, double scalar)
   {
      double length = vector.length();
      arrowGraphic.update(length * scalar);

      tempTransform.setToZero();
      EuclidGeometryTools.orientation3DFromZUpToVector3D(vector, tempTransform.getRotation());
      tempTransform.getTranslation().set(origin);
      GDXTools.toGDX(tempTransform, arrowGraphic.getDynamicModel().getOrCreateModelInstance().transform);
   }

   public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool)
   {
      linearPartArrow.getRenderables(renderables, pool);
      angularPartArrow.getRenderables(renderables, pool);
   }

   public int getIndexOfSensor()
   {
      return indexOfSensor;
   }
}
