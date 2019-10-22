package us.ihmc.humanoidBehaviors;

import us.ihmc.humanoidBehaviors.exploreArea.ExploreAreaBehavior;
import us.ihmc.humanoidBehaviors.fancyPoses.FancyPosesBehavior;
import us.ihmc.humanoidBehaviors.patrol.PatrolBehavior;
import us.ihmc.humanoidBehaviors.patrol.PatrolBehaviorAPI;
import us.ihmc.humanoidBehaviors.tools.BehaviorHelperBuilder;
import us.ihmc.messager.MessagerAPIFactory.MessagerAPI;

public enum BehaviorRegistry
{
   STEP_IN_PLACE(StepInPlaceBehavior::new, StepInPlaceBehavior.API.create()),
   PATROL(PatrolBehavior::new, PatrolBehaviorAPI.create()),
   FANCY_POSES(FancyPosesBehavior::new, FancyPosesBehavior.API.create()),
   EXPLORE(ExploreAreaBehavior::new, ExploreAreaBehavior.ExploreAreaBehaviorAPI.create()),
   ;

   public static final BehaviorRegistry[] values = values();

   private final BehaviorSupplier behaviorSupplier;
   private final MessagerAPI behaviorAPI;

   private BehaviorInterface constructedBehavior;

   BehaviorRegistry(BehaviorSupplier behaviorSupplier, MessagerAPI behaviorAPI)
   {
      this.behaviorSupplier = behaviorSupplier;
      this.behaviorAPI = behaviorAPI;
   }

   public void build(BehaviorHelperBuilder helperBuilder)
   {
      constructedBehavior = behaviorSupplier.build(helperBuilder.build());
   }

   public MessagerAPI getBehaviorAPI()
   {
      return behaviorAPI;
   }

   public BehaviorInterface getConstructedBehavior()
   {
      return constructedBehavior;
   }
}
