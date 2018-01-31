package us.ihmc.commonWalkingControlModules.momentumBasedController.optimization;

import java.util.List;

import us.ihmc.commonWalkingControlModules.configurations.GroupParameter;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;

public abstract class MomentumOptimizationSettings implements ControllerCoreOptimizationSettings
{
   public abstract Vector3D getLinearMomentumWeight();

   public abstract Vector3D getHighLinearMomentumWeightForRecovery();

   public abstract Vector3D getAngularMomentumWeight();

   /**
    * Returns the optimization weight for the linear objective of the foot whenever the foot is
    * in support (loaded). When the foot is not loaded the default weight from
    * {@link #getTaskspaceLinearWeights()} will be used.
    */
   public abstract Vector3DReadOnly getLoadedFootLinearWeight();

   /**
    * Returns the optimization weight for the angular objective of the foot whenever the foot is
    * in support (loaded).When the foot is not loaded the default weight from
    * {@link #getTaskspaceAngularWeights()} will be used.
    */
   public abstract Vector3DReadOnly getLoadedFootAngularWeight();

   /**
    * The map returned contains all optimization weights for jointspace objectives. The key of the map
    * is the joint name as defined in the robot joint map. If a joint is not contained in the map,
    * jointspace control is not supported for that joint.
    *
    * @return map containing jointspace QP weights by joint name
    */
   public abstract List<GroupParameter<Double>> getJointspaceWeights();

   /**
    * The map returned contains all optimization weights for user desired acceleration objectives. The
    * key of the map is the joint name as defined in the robot joint map. If a joint is not contained
    * in the map, user desired acceleration commands are not supported for that joint.
    *
    * @return map containing user desired acceleration QP weights by joint name
    */
   public abstract List<GroupParameter<Double>> getUserModeWeights();

   /**
    * The map returned contains all optimization weights for taskspace orientation objectives. The key
    * of the map is the rigid body name as defined in the robot joint map. If a rigid body is not
    * contained in the map, taskspace orientation objectives are not supported for that body.
    *
    * @return map containing taskspace orientation QP weights by rigid body name
    */
   public abstract List<GroupParameter<Vector3DReadOnly>> getTaskspaceAngularWeights();

   /**
    * The map returned contains all optimization weights for taskspace position objectives. The key
    * of the map is the rigid body name as defined in the robot joint map. If a rigid body is not
    * contained in the map, taskspace position objectives are not supported for that body.
    *
    * @return map containing taskspace position QP weights by rigid body name
    */
   public abstract List<GroupParameter<Vector3DReadOnly>> getTaskspaceLinearWeights();
}
