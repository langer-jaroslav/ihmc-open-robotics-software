package us.ihmc.sensorProcessing.pointClouds.testbed;

import boofcv.gui.image.ShowImages;
import bubo.clouds.FactoryFitting;
import bubo.clouds.FactoryPointCloudShape;
import bubo.clouds.detect.CloudShapeTypes;
import bubo.clouds.detect.PointCloudShapeFinder;
import bubo.clouds.detect.wrapper.ConfigMultiShapeRansac;
import bubo.clouds.detect.wrapper.ConfigSurfaceNormals;
import bubo.clouds.filter.UniformDensityCloudOctree;
import bubo.clouds.fit.MatchCloudToCloud;
import bubo.gui.FactoryVisualization3D;
import bubo.gui.UtilDisplayBubo;
import bubo.gui.d3.PointCloudPanel;
import bubo.struct.StoppingCondition;
import com.thoughtworks.xstream.XStream;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static us.ihmc.sensorProcessing.pointClouds.GeometryOps.loadCloud;
import static us.ihmc.sensorProcessing.pointClouds.GeometryOps.loadScanLines;
import static us.ihmc.sensorProcessing.pointClouds.testbed.CreateCloudFromFilteredScanApp.filter;

/**
 * @author Peter Abeles
 */
public class AutomaticAlignTestbedToPlanes {


   public static void main(String[] args) {

      String directory = "../SensorProcessing/data/testbed/2014-08-01/";

      Se3_F64 estimatedToModel = (Se3_F64) new XStream().fromXML(directory.getClass().
              getResourceAsStream("/testbed/estimatedToModel.xml"));
      TestbedAutomaticAlignment alg = new TestbedAutomaticAlignment(3,estimatedToModel);

      System.out.println("Loading and filtering point clouds");
      List<List<Point3D_F64>> scans0 = loadScanLines(directory+"cloud12_scans.txt");
      for (int i = 0; i < scans0.size(); i++) {
         alg.addScan(scans0.get(i));
      }

      System.out.println("Detecting the testbed");
      ManualAlignTestbedToCloud display = new ManualAlignTestbedToCloud();

      long before = System.currentTimeMillis();
      if( alg.process() ) {
         Se3_F64 modelToWorld = alg.getModelToWorld();

         System.out.println("Rendering results");

         display.addTestBedModel();
         display.setTestbedToWorld(modelToWorld);
      }
      long after = System.currentTimeMillis();
      System.out.println("Elapsed Time: "+(after-before));

      display.addPoints(alg.getCloud1(),0xFF0000,3);

      JPanel gui = new JPanel();
      gui.add( display.getCanvas() );
      gui.setPreferredSize( new Dimension(800,800));

      ShowImages.showWindow(gui, "Automatic Alignment");
      gui.requestFocus();
   }
}
