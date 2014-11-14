package us.ihmc.simulationconstructionset.util.ground;

import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.utilities.math.geometry.Box3d;
import us.ihmc.utilities.math.geometry.Direction;
import us.ihmc.utilities.math.geometry.TransformTools;


public class RotatableCinderBlockTerrainObject extends RotatableBoxTerrainObject
{
   public RotatableCinderBlockTerrainObject(Box3d box, AppearanceDefinition appearance)
   {
      super(box, appearance);
   }

   protected void addGraphics()
   {      
      RigidBodyTransform transformCenterConventionToBottomConvention = box.getTransformCopy();
      transformCenterConventionToBottomConvention = TransformTools.transformLocalZ(transformCenterConventionToBottomConvention, -box.getDimension(Direction.Z) / 2.0);

      Vector3d vector = new Vector3d(box.getDimension(Direction.X), box.getDimension(Direction.Y), box.getDimension(Direction.Z));
      
      linkGraphics = new Graphics3DObject();
      linkGraphics.transform(transformCenterConventionToBottomConvention);
      linkGraphics.scale(vector);
      linkGraphics.addModelFile("models/cinderblock1Meter.obj");
   }

}
