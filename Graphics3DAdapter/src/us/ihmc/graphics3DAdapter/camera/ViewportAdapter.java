package us.ihmc.graphics3DAdapter.camera;

import java.awt.Canvas;
import java.nio.FloatBuffer;


public interface ViewportAdapter
{

   public abstract Canvas getCanvas();
   public abstract CaptureDevice getCaptureDevice();

   public abstract void setCameraController(CameraController cameraController);
   public abstract CameraController getCameraController();

   public abstract void setClipDistances(double near, double far);

   public abstract void setFieldOfView(double fieldOfView);

   public abstract double getFieldOfView();
   public abstract double getPhysicalWidth();
   public abstract double getPhysicalHeight();
   public abstract void setupOffscreenView(int width, int height);
   public abstract FloatBuffer getZBuffer();


}