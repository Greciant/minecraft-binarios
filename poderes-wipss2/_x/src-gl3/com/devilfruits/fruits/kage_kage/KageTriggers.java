package com.devilfruits.fruits.kage_kage;

import com.devilfruits.registry.DFTriggers;
import java.util.function.Supplier;
import net.minecraft.advancements.critereon.PlayerTrigger;

public final class KageTriggers {
   public static final Supplier<PlayerTrigger> SHADOW_TAKEN = DFTriggers.TRIGGERS.register("kage_kage/shadow_taken", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> ZOMBIE_RAISED = DFTriggers.TRIGGERS.register("kage_kage/zombie_raised", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> FULL_CREW = DFTriggers.TRIGGERS.register("kage_kage/full_crew", PlayerTrigger::new);

   static void init() {
   }

   private KageTriggers() {
   }
}
