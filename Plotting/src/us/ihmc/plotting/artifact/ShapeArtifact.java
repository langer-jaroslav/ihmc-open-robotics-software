package us.ihmc.plotting.artifact;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import us.ihmc.plotting.Graphics2DAdapter;
import us.ihmc.plotting.Plotter2DAdapter;

public class ShapeArtifact extends Artifact
{
   private static final double LEGEND_RADIUS = 20.0;
   
   private Point2d pose;
   private double height;
   private double width;
   
   private final Point2d tempCenter = new Point2d();
   private final Vector2d tempRadii = new Vector2d();

   public ShapeArtifact(String id, String type, double height, double width, Point2d pose)
   {
      super(id);
      setType(type);
      setLevel(1);
      this.pose = pose;
      this.height = height;
      this.width = width;
   }

   public void setPose(Point2d pose)
   {
      this.pose = pose;
   }

   public Point2d getPose()
   {
      return pose;
   }

   /**
    * Must provide a draw method for plotter to render artifact
    */
   @Override
   public void draw(Graphics2DAdapter graphics)
   {
      if (pose == null)
      {
         System.out.println("problem...shape with null pose:" + this.getID());

         return;
      }

      graphics.setColor(color);
      tempCenter.set(pose.getX(), pose.getY());
      tempRadii.set(width / 2.0, height / 2.0);
      
      if (getType().equals("fillcircle"))
      {
         graphics.drawOvalFilled(tempCenter, tempRadii);
      }
      else if (getType().equals("circle"))
      {
         graphics.drawOval(tempCenter, tempRadii);
      }
      else if (getType().equals("fillrectangle"))
      {
         graphics.drawSquareFilled(tempCenter, tempRadii);
      }
      else if (getType().equals("rectangle"))
      {
         graphics.drawSquare(tempCenter, tempRadii);
      }
   }

   @Override
   public void drawLegend(Plotter2DAdapter graphics, Point2d origin)
   {
      graphics.setColor(color);
      tempCenter.set(origin);
      tempRadii.set(LEGEND_RADIUS, LEGEND_RADIUS);
      if (getType().equals("fillcircle"))
      {
         graphics.drawOvalFilled(graphics.getScreenFrame(), tempCenter, tempRadii);
      }
      else if (getType().equals("circle"))
      {
         graphics.drawOval(graphics.getScreenFrame(), tempCenter, tempRadii);
      }
      else if (getType().equals("fillrectangle"))
      {
         graphics.drawSquareFilled(graphics.getScreenFrame(), tempCenter, tempRadii);
      }
      else if (getType().equals("rectangle"))
      {
         graphics.drawRectangle(graphics.getScreenFrame(), tempCenter, tempRadii);
      }
   }

   public void save(PrintWriter printWriter)
   {
      printWriter.println(pose.getX() + " " + pose.getY() + " " + width + " " + height + " " + getType() + " " + id);
   }

   public static ShapeArtifact load(BufferedReader bufferedReader)
   {
      ShapeArtifact shapeArtifact = null;
      try
      {
         String line = bufferedReader.readLine();
         if (line == null)
            return null;
         StringTokenizer s = new StringTokenizer(line, " ");
         double x = Double.parseDouble(s.nextToken());
         double y = Double.parseDouble(s.nextToken());
         Point2d pose = new Point2d(x, y);
         double width = Double.parseDouble(s.nextToken());
         double height = Double.parseDouble(s.nextToken());
         String type = s.nextToken();
         String id = s.nextToken();
         shapeArtifact = new ShapeArtifact(id, type, height, width, pose);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

      return shapeArtifact;
   }

   public ShapeArtifact getCopy()
   {
      ShapeArtifact shapeCopy = new ShapeArtifact(this.getID(), this.getType(), height, width, this.getPose());
      shapeCopy.setColor(this.getColor());

      return shapeCopy;
   }

   @Override
   public void drawHistory(Graphics2DAdapter graphics)
   {
      throw new RuntimeException("Not implemented!");
   }

   @Override
   public void takeHistorySnapshot()
   {
      throw new RuntimeException("Not implemented!");
   }
}
