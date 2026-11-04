package com.devilfruits.fruits.gomu_gomu;

import com.devilfruits.registry.DFTriggers;
import java.util.function.Supplier;
import net.minecraft.advancements.critereon.PlayerTrigger;

public final class GomuTriggers {
   public static final Supplier<PlayerTrigger> BLUNT = DFTriggers.TRIGGERS.register("gomu_gomu/blunt", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> BULLET_RETURNED = DFTriggers.TRIGGERS.register("gomu_gomu/bullet_returned", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> HIGH_FALL_SURVIVED = DFTriggers.TRIGGERS.register("gomu_gomu/high_fall", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> ROCKET_GAP_CROSSED = DFTriggers.TRIGGERS.register("gomu_gomu/rocket_gap", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> AXE_CRATER = DFTriggers.TRIGGERS.register("gomu_gomu/axe_crater", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> GATLING_FINISHED = DFTriggers.TRIGGERS.register("gomu_gomu/gatling_finished", PlayerTrigger::new);
   public static final float HIGH_FALL = 40.0F;
   public static final double ROCKET_GAP = 15.0;

   static void init() {
   }

   private GomuTriggers() {
   }
}
