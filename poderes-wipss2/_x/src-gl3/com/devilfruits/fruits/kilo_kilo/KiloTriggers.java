package com.devilfruits.fruits.kilo_kilo;

import com.devilfruits.registry.DFTriggers;
import java.util.function.Supplier;
import net.minecraft.advancements.critereon.PlayerTrigger;

public final class KiloTriggers {
   public static final Supplier<PlayerTrigger> FEATHER_JUMP = DFTriggers.TRIGGERS.register("kilo_kilo/feather_jump", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> WRECKING_BALL = DFTriggers.TRIGGERS.register("kilo_kilo/wrecking_ball", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> SANK_RAFT = DFTriggers.TRIGGERS.register("kilo_kilo/sank_raft", PlayerTrigger::new);

   static void init() {
   }

   private KiloTriggers() {
   }
}
