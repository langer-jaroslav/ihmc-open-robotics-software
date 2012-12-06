package us.ihmc.graphics3DAdapter;

import us.ihmc.graphics3DAdapter.camera.CameraAdapter;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;

public interface Graphics3DAdapter
{
   public void addRootNode(Graphics3DNode rootNode);
   public void removeRootNode(Graphics3DNode rootNode);
   
   public CameraAdapter getDefaultCamera();
   public CameraAdapter createNewCamera();
   
   public void setHeightMap(HeightMap heightMap);
   
   public Object getGraphicsConch();
   public void setGroundVisible(boolean isVisible);
   
   public RayCollisionAdapter getRayCollisionAdapter();
   
   public void addSelectedListener(SelectedListener selectedListener);
}
