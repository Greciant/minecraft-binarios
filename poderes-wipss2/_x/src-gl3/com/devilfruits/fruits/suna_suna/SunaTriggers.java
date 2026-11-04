package com.devilfruits.fruits.suna_suna;

import com.devilfruits.registry.DFTriggers;
import java.util.function.Supplier;
import net.minecraft.advancements.critereon.PlayerTrigger;

public final class SunaTriggers {
   public static final Supplier<PlayerTrigger> USED_SPADA = DFTriggers.TRIGGERS.register("suna_suna/used_desert_spada", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> OPENED_GIRASOLE = DFTriggers.TRIGGERS.register("suna_suna/opened_desert_girasole", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> USED_SABLES = DFTriggers.TRIGGERS.register("suna_suna/used_sables", PlayerTrigger::new);

   static void init() {
   }

   private SunaTriggers() {
   }
}
