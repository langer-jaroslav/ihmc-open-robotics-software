package us.ihmc.wholeBodyController;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.junit.Test;

import us.ihmc.SdfLoader.FullRobotModelVisualizer;
import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.AverageDuration;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.trajectory.TrajectoryND;
import us.ihmc.utilities.trajectory.TrajectoryND.WaypointND;
import us.ihmc.wholeBodyController.WholeBodyIkSolver.ComputeOption;
import us.ihmc.wholeBodyController.WholeBodyIkSolver.ComputeResult;
import us.ihmc.wholeBodyController.WholeBodyIkSolver.ControlledDoF;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.graphics.YoGraphicShape;
import us.ihmc.yoUtilities.math.frames.YoFrameOrientation;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;

public abstract class WholeBodyTrajectoryTest
{
   private SDFFullRobotModel actualRobotModel;
   private SDFFullRobotModel desiredRobotModel;
   private WholeBodyIkSolver wbSolver;

   static private final  YoVariableRegistry registry = new YoVariableRegistry("WholeBodyIkSolverTestFactory_Registry"); 
   private final ArrayList<Pair<ReferenceFrame, ReferenceFrame>> handArrayList = new ArrayList<Pair<ReferenceFrame, ReferenceFrame>>();

   static private final boolean DEBUG = false;

   public abstract WholeBodyControllerParameters getRobotModel();
   public abstract FullRobotModelVisualizer getFullRobotModelVisualizer();
   public abstract SimulationConstructionSet getSimulationConstructionSet();


   public WholeBodyTrajectoryTest(SDFFullRobotModel actualRobotModel, WholeBodyIkSolver solver)
   {
      this.actualRobotModel = actualRobotModel;
      desiredRobotModel = getRobotModel().createFullRobotModel();
      this.wbSolver = solver; 

      Vector3d rootPosition = new Vector3d(0,0, 0.93);
      actualRobotModel.getRootJoint().setPosition( rootPosition );   
   }    

  

	@AverageDuration(duration = 4.1)
	@Test(timeout = 22445)
   public void testTrajectory() throws Exception
   {
   
      wbSolver.setVerbosityLevel(1);

      wbSolver.setNumberOfControlledDoF(RobotSide.LEFT, ControlledDoF.DOF_NONE);
      wbSolver.setNumberOfControlledDoF(RobotSide.RIGHT, ControlledDoF.DOF_3P);

      ReferenceFrame soleFrame = actualRobotModel.getSoleFrame(RobotSide.RIGHT);

      ArrayList<Point3d> targetListRight = new ArrayList<Point3d>();

   //   FramePose targetL = new FramePose(soleFrame, new Point3d( 0.5, 0.4, 0.8 ), new Quat4d() );

      targetListRight.add( new Point3d( 0.4, -0.6, 1.4 ));
      targetListRight.add( new Point3d( 0.4, -0.6, 0.8 ));
      targetListRight.add( new Point3d( 0.5, 0.6,  1.0 ));
      targetListRight.add( new Point3d( 0.5, -0.0,  1.0 ));
      targetListRight.add( new Point3d( 0.5, -0.4,  1.4 ));
      targetListRight.add( new Point3d( 0.5, -0.2,  0.6 ));

      for (Point3d rightTarget: targetListRight)
      {
         if( getSimulationConstructionSet() != null )
         {
            for (int a=0; a< 50; a++)
            {
               Thread.sleep(10);
               getFullRobotModelVisualizer().update(0);
            }
         }
         
         FramePose targetR = new FramePose(ReferenceFrame.getWorldFrame(), rightTarget, new Quat4d() );
         
         if( getSimulationConstructionSet() != null )
         {
            visualizePoint(0.04, YoAppearance.Green(),targetR);
         }

       //  wbSolver.setGripperPalmTarget(actualRobotModel, RobotSide.LEFT,  targetL );
         wbSolver.setGripperPalmTarget(actualRobotModel, RobotSide.RIGHT,  targetR );

         ComputeResult ret = wbSolver.compute(actualRobotModel, desiredRobotModel, ComputeOption.USE_ACTUAL_MODEL_JOINTS );

         if( ret == ComputeResult.SUCCEEDED )
         {
            WholeBodyTrajectory trajectoryGenerator = new WholeBodyTrajectory( 1.5, 15, 0.10);
            TrajectoryND trajectory = trajectoryGenerator.createTaskSpaceTrajectory(wbSolver, actualRobotModel, desiredRobotModel);

            Pair<Boolean, WaypointND> result = trajectory.getNextInterpolatedPoints(0.01);

            while( result.first().booleanValue() == false)
            {
               HashMap<String, Double> angles     = new  HashMap<String, Double>();
               HashMap<String, Double> velocities = new  HashMap<String, Double>();
               int index = 0;
               for(OneDoFJoint joint: actualRobotModel.getOneDoFJoints())
               {
                  if( wbSolver.hasJoint( joint.getName() ))
                  {
                     angles.put( joint.getName(),     result.second().position[index]);
                     velocities.put( joint.getName(), result.second().velocity[index]);
                     index++;
                  }
               }

               actualRobotModel.updateJointsStateButKeepOneFootFixed( angles,velocities, RobotSide.RIGHT );
               result = trajectory.getNextInterpolatedPoints(0.01);
               
               if( getSimulationConstructionSet() != null )
               {
                  Thread.sleep(10);
                  getFullRobotModelVisualizer().update(0);
               }
            }

         }
         else{
            fail("no solution found\n");
         }
         
         if( getSimulationConstructionSet() != null )
         {
            for (int a=0; a< 50; a++)
            {
               Thread.sleep(10);
               getFullRobotModelVisualizer().update(0);
            }
         }
      } 
   }
	
	static int count = 0;
	private void visualizePoint(double radius, AppearanceDefinition color, FramePose spherePose )
   {
	   FramePose pose = new FramePose( spherePose );
      count++;
      Graphics3DObject linkGraphics = new Graphics3DObject();
      linkGraphics.addSphere(radius, color);

      YoFramePoint framePoint = new YoFramePoint("point" + count, ReferenceFrame.getWorldFrame(), registry);
      YoFrameOrientation frameOrientation = new YoFrameOrientation("orientation" + count, ReferenceFrame.getWorldFrame(), registry);
      YoGraphicShape yoGraphicsShape = new YoGraphicShape("target" + count, linkGraphics, framePoint, frameOrientation, 1.0);
      
      RigidBodyTransform transform = new RigidBodyTransform( );    
      pose.changeFrame( ReferenceFrame.getWorldFrame());
      pose.getRigidBodyTransform(transform);
      
      yoGraphicsShape.setTransformToWorld( transform );
      
      getSimulationConstructionSet().addYoGraphic(yoGraphicsShape);
   }

}
