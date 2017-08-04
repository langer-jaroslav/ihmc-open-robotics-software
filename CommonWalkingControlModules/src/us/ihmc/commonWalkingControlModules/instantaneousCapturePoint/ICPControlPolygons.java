package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint;

import static us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepListVisualizer.defaultFeetColors;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactPointInterface;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.graphicsDescription.yoGraphics.plotting.ArtifactList;
import us.ihmc.graphicsDescription.yoGraphics.plotting.YoArtifactPolygon;
import us.ihmc.robotics.geometry.FrameConvexPolygon2d;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.math.frames.YoFrameConvexPolygon2d;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

import java.awt.*;

public class ICPControlPolygons
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private static boolean VISUALIZE = true;
   private static final int maxNumberOfContactPointsPerFoot = 6;

   private final YoVariableRegistry registry = new YoVariableRegistry("ICPControlPolygons");

   // Reference frames:
   private final ReferenceFrame midFeetZUp;

   // Polygons:
   private final SideDependentList<FrameConvexPolygon2d> footPolygonsInWorldFrame = new SideDependentList<>();
   private final SideDependentList<FrameConvexPolygon2d> footPolygonsInMidFeetZUp = new SideDependentList<>();

   private final FrameConvexPolygon2d controlPolygonInMidFeetZUp = new FrameConvexPolygon2d();
   private final FrameConvexPolygon2d controlPolygonInWorld = new FrameConvexPolygon2d();

   private final YoFrameConvexPolygon2d controlPolygonViz;
   private final SideDependentList<YoFrameConvexPolygon2d> controlFootPolygonsViz = new SideDependentList<>();

   private final ICPControlPlane icpControlPlane;

   public ICPControlPolygons(ICPControlPlane icpControlPlane, ReferenceFrame midFeetZUpFrame, YoVariableRegistry parentRegistry, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      this.icpControlPlane = icpControlPlane;
      this.midFeetZUp = midFeetZUpFrame;

      controlPolygonViz = new YoFrameConvexPolygon2d("combinedPolygon", "", worldFrame, 2 * maxNumberOfContactPointsPerFoot, registry);

      ArtifactList artifactList = new ArtifactList(getClass().getSimpleName());

      YoArtifactPolygon controlPolygonArtifact = new YoArtifactPolygon("Combined Control Polygon", controlPolygonViz, Color.red, false);
      artifactList.add(controlPolygonArtifact);

      for (RobotSide robotSide : RobotSide.values)
      {
         footPolygonsInWorldFrame.put(robotSide, new FrameConvexPolygon2d());
         footPolygonsInMidFeetZUp.put(robotSide, new FrameConvexPolygon2d());
         String robotSidePrefix = robotSide.getCamelCaseNameForStartOfExpression();

         YoFrameConvexPolygon2d controlFootPolygonViz = new YoFrameConvexPolygon2d(robotSidePrefix + "controlFootPolygon", "", worldFrame, maxNumberOfContactPointsPerFoot, registry);
         controlFootPolygonsViz.put(robotSide, controlFootPolygonViz);
         YoArtifactPolygon footPolygonArtifact = new YoArtifactPolygon(robotSide.getCamelCaseNameForMiddleOfExpression() + " Control Foot Polygon", controlFootPolygonViz, defaultFeetColors.get(robotSide), false);
         artifactList.add(footPolygonArtifact);
      }

      if (yoGraphicsListRegistry != null)
      {
         yoGraphicsListRegistry.registerArtifactList(artifactList);
      }

      parentRegistry.addChild(registry);
   }

   private final FramePoint tempFramePoint = new FramePoint();

   public void updateUsingContactStates(SideDependentList<? extends PlaneContactState> contactStates)
   {
      boolean inDoubleSupport = true;
      boolean neitherFootIsSupportingFoot = true;
      RobotSide supportSide = null;

      for (RobotSide robotSide : RobotSide.values)
      {
         PlaneContactState contactState = contactStates.get(robotSide);

         FrameConvexPolygon2d footPolygonInWorldFrame = footPolygonsInWorldFrame.get(robotSide);
         FrameConvexPolygon2d footPolygonInMidFeetZUp = footPolygonsInMidFeetZUp.get(robotSide);

         footPolygonInWorldFrame.clearAndUpdate(worldFrame);
         footPolygonInMidFeetZUp.clearAndUpdate(midFeetZUp);

         if (contactState.inContact())
         {
            supportSide = robotSide;
            neitherFootIsSupportingFoot = false;

            for (int i = 0; i < contactState.getTotalNumberOfContactPoints(); i++)
            {
               ContactPointInterface contactPoint = contactState.getContactPoints().get(i);
               if (!contactPoint.isInContact())
                  continue;

               icpControlPlane.projectPointOntoControlPlane(contactPoint.getPosition(), tempFramePoint);
               footPolygonInWorldFrame.addVertexByProjectionOntoXYPlane(tempFramePoint);
               footPolygonInMidFeetZUp.addVertexByProjectionOntoXYPlane(tempFramePoint);
            }

            footPolygonInWorldFrame.update();
            footPolygonInMidFeetZUp.update();
         }
         else
         {
            inDoubleSupport = false;
         }
      }

      updateSupportPolygon(inDoubleSupport, neitherFootIsSupportingFoot, supportSide);

      if (VISUALIZE)
         visualize();
   }

   private void updateSupportPolygon(boolean inDoubleSupport, boolean neitherFootIsSupportingFoot, RobotSide supportSide)
   {
      // Get the support polygon. If in double support, it is the combined polygon.
      // FIXME: Assumes the individual feet polygons are disjoint for faster computation. Will crash if the feet overlap.
      // If in single support, then the support polygon is just the foot polygon of the supporting foot.
      if (neitherFootIsSupportingFoot)
         throw new RuntimeException("neither foot is a supporting foot!");

      if (inDoubleSupport)
      {
         controlPolygonInMidFeetZUp.setIncludingFrameAndUpdate(footPolygonsInMidFeetZUp.get(RobotSide.LEFT), footPolygonsInMidFeetZUp.get(RobotSide.RIGHT));
      }
      else
      {
         controlPolygonInMidFeetZUp.setIncludingFrameAndUpdate(footPolygonsInMidFeetZUp.get(supportSide));
      }

      controlPolygonInWorld.setIncludingFrameAndUpdate(controlPolygonInMidFeetZUp);
      controlPolygonInWorld.changeFrameAndProjectToXYPlane(worldFrame);
   }

   private void visualize()
   {
      controlPolygonViz.setFrameConvexPolygon2d(controlPolygonInWorld);

      for (RobotSide robotSide : RobotSide.values)
      {
         YoFrameConvexPolygon2d footPolygonViz = controlFootPolygonsViz.get(robotSide);
         FrameConvexPolygon2d footPolygon = footPolygonsInWorldFrame.get(robotSide);
         if (footPolygon.isEmpty())
            footPolygonViz.hide();
         else
            footPolygonViz.setFrameConvexPolygon2d(footPolygon);
      }
   }
}
