package com.devilfruits.fruits.uo_uo_seiryu;

import com.devilfruits.registry.DFTriggers;
import java.util.function.Supplier;
import net.minecraft.advancements.critereon.PlayerTrigger;

public final class SeiryuTriggers {
   public static final Supplier<PlayerTrigger> USED_RAGNARAKU = DFTriggers.TRIGGERS.register("uo_uo_seiryu/used_ragnaraku", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> USED_THUNDER_STORM = DFTriggers.TRIGGERS.register("uo_uo_seiryu/used_thunder_storm", PlayerTrigger::new);

   static void init() {
   }

   private SeiryuTriggers() {
   }
}
