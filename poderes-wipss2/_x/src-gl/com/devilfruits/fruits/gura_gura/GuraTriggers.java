package com.devilfruits.fruits.gura_gura;

import com.devilfruits.registry.DFTriggers;
import java.util.function.Supplier;
import net.minecraft.advancements.critereon.PlayerTrigger;

public final class GuraTriggers {
   public static final Supplier<PlayerTrigger> USED_SHIMA_YURASHI = DFTriggers.TRIGGERS.register("gura_gura/used_shima_yurashi", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> OCEAN_SEAQUAKE = DFTriggers.TRIGGERS.register("gura_gura/ocean_seaquake", PlayerTrigger::new);

   static void init() {
   }

   private GuraTriggers() {
   }
}
