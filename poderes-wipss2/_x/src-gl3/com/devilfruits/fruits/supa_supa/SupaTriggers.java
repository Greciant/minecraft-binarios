package com.devilfruits.fruits.supa_supa;

import com.devilfruits.registry.DFTriggers;
import java.util.function.Supplier;
import net.minecraft.advancements.critereon.PlayerTrigger;

public final class SupaTriggers {
   public static final Supplier<PlayerTrigger> STEEL_WALL = DFTriggers.TRIGGERS.register("supa_supa/steel_wall", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> SHIP_IN_TWO = DFTriggers.TRIGGERS.register("supa_supa/ship_in_two", PlayerTrigger::new);

   static void init() {
   }

   private SupaTriggers() {
   }
}
