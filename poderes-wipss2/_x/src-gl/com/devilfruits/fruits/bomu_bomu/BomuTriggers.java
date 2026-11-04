package com.devilfruits.fruits.bomu_bomu;

import com.devilfruits.registry.DFTriggers;
import java.util.function.Supplier;
import net.minecraft.advancements.critereon.PlayerTrigger;

public final class BomuTriggers {
   public static final Supplier<PlayerTrigger> TNT_UNTOUCHED = DFTriggers.TRIGGERS.register("bomu_bomu/tnt_untouched", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> SELF_DESTRUCTION = DFTriggers.TRIGGERS.register("bomu_bomu/self_destruction", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> SHIP_HOLE = DFTriggers.TRIGGERS.register("bomu_bomu/ship_hole", PlayerTrigger::new);

   static void init() {
   }

   private BomuTriggers() {
   }
}
