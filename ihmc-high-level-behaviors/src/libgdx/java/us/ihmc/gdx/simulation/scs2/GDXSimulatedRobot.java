package us.ihmc.gdx.simulation.scs2;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.tools.ReferenceFrameTools;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.scs2.definition.robot.RobotDefinition;
import us.ihmc.scs2.sharedMemory.LinkedYoRegistry;
import us.ihmc.scs2.sharedMemory.LinkedYoVariable;
import us.ihmc.scs2.sharedMemory.tools.SharedMemoryTools;
import us.ihmc.scs2.simulation.SimulationSession;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoVariable;

public class GDXSimulatedRobot
{
   private final RobotDefinition robotDefinition;
   private final YoRegistry mirroredBoxRegistry;
   private final RigidBodyBasics originalRigidBody;
   private GDXRigidBody rootBody;
   private LinkedYoRegistry robotLinkedYoRegistry;
   private boolean initialize = true;

   public GDXSimulatedRobot(RobotDefinition robotDefinition)
   {
      this.robotDefinition = robotDefinition;
      mirroredBoxRegistry = SharedMemoryTools.newRegistryFromNamespace(SimulationSession.ROOT_REGISTRY_NAME, robotDefinition.getName());
      originalRigidBody = robotDefinition.newInstance(ReferenceFrameTools.constructARootFrame("dummy"));
   }

   public void create(GDXYoManager yoManager)
   {
      rootBody = GDXMultiBodySystemFactories.toYoGDXMultiBodySystem(originalRigidBody,
                                                                    ReferenceFrame.getWorldFrame(),
                                                                    robotDefinition,
                                                                    mirroredBoxRegistry);
      robotLinkedYoRegistry = yoManager.newLinkedYoRegistry(mirroredBoxRegistry);
      mirroredBoxRegistry.getVariables().forEach(var ->
      {
         LinkedYoVariable<YoVariable> linkYoVariable = robotLinkedYoRegistry.linkYoVariable(var);
         linkYoVariable.addUser(this);
      });
   }

   public void update()
   {
      if (robotLinkedYoRegistry.pull() || initialize)
      {
         rootBody.updateFramesRecursively();
         rootBody.updateSubtreeGraphics();
         initialize = false;
      }
   }

   public void getRealRenderables(Array<Renderable> renderables, Pool<Renderable> pool)
   {
      for (GDXRigidBody rigidBody : rootBody.subtreeIterable())
      {
         if (rigidBody.getVisualGraphicsNode() != null)
         {
            rigidBody.getVisualGraphicsNode().getRenderables(renderables, pool);
         }
      }
   }

   public void getCollisionMeshRenderables(Array<Renderable> renderables, Pool<Renderable> pool)
   {
      for (GDXRigidBody rigidBody : rootBody.subtreeIterable())
      {
         if (rigidBody.getCollisionGraphicsNode() != null)
         {
            rigidBody.getCollisionGraphicsNode().getRenderables(renderables, pool);
         }
      }
   }
}
