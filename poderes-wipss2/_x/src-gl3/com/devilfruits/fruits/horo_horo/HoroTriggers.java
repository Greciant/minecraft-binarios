package com.devilfruits.fruits.horo_horo;

import com.devilfruits.registry.DFTriggers;
import java.util.function.Supplier;
import net.minecraft.advancements.critereon.PlayerTrigger;

public final class HoroTriggers {
   public static final Supplier<PlayerTrigger> NEGATIVE = DFTriggers.TRIGGERS.register("horo_horo/negative", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> GHOST_RAP = DFTriggers.TRIGGERS.register("horo_horo/ghost_rap", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> GHOST_FORM = DFTriggers.TRIGGERS.register("horo_horo/ghost_form", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> KAMIKAZE = DFTriggers.TRIGGERS.register("horo_horo/kamikaze", PlayerTrigger::new);

   static void init() {
   }

   private HoroTriggers() {
   }
}
