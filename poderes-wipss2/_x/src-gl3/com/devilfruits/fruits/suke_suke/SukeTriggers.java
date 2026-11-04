package com.devilfruits.fruits.suke_suke;

import com.devilfruits.registry.DFTriggers;
import java.util.function.Supplier;
import net.minecraft.advancements.critereon.PlayerTrigger;

public final class SukeTriggers {
   public static final Supplier<PlayerTrigger> VANISH = DFTriggers.TRIGGERS.register("suke_suke/vanish", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> SHARED = DFTriggers.TRIGGERS.register("suke_suke/shared", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> GHOST_SHIP = DFTriggers.TRIGGERS.register("suke_suke/ghost_ship", PlayerTrigger::new);

   static void init() {
   }

   private SukeTriggers() {
   }
}
