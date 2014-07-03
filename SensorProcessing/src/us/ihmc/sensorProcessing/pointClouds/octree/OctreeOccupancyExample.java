package us.ihmc.sensorProcessing.pointClouds.octree;

import georegression.struct.point.Point3D_F64;
import georegression.struct.shapes.Cube3D_F64;

import java.util.List;

import bubo.ptcloud.Octree;

/**
 * @author Peter Abeles
 */
public class OctreeOccupancyExample {

    public static int NUMBER_OCCUPIED = 10;
    public static double CELL_SIZE = 0.05;
    

    public static OctreeOccupancyMap mainMethod(List<Point3D_F64> cloud) {
        //List<Point3D_F64> cloud = PointCloudTools.readPointCloud("../SensorProcessing/data/kinectcloud.txt",-1);


        OctreeOccupancyMap map = new OctreeOccupancyMap(CELL_SIZE,new Cube3D_F64(-10,-10,-10,10,10,10));

        map.initialize(cloud);

        // Assign probability to all the leafs
        int totalOccupied = 0;
        for( Octree n : map.getLeafs() ) {
            if( n.isLeaf() ) {
                OccupancyCell c = n.getUserData();
                if( n.points.size() > NUMBER_OCCUPIED ) {
                    totalOccupied++;
                    c.setProbability(0.8);
                    
                } else {
                    c.setProbability(0.4);
                }
            }
        }
        System.out.println("Leafs occupied = " + totalOccupied + "  out of " + map.getLeafs().size());

       return map;
    }



}
