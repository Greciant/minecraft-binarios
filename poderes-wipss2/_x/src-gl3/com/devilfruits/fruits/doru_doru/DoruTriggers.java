package com.devilfruits.fruits.doru_doru;

import com.devilfruits.registry.DFTriggers;
import java.util.function.Supplier;
import net.minecraft.advancements.critereon.PlayerTrigger;

public final class DoruTriggers {
   public static final Supplier<PlayerTrigger> FIRST_ARCH = DFTriggers.TRIGGERS.register("doru_doru/first_arch", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> SYMMETRY = DFTriggers.TRIGGERS.register("doru_doru/symmetry", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> CAST_MOLD = DFTriggers.TRIGGERS.register("doru_doru/cast_mold", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> ENCASE = DFTriggers.TRIGGERS.register("doru_doru/encase", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> MELT_WALL = DFTriggers.TRIGGERS.register("doru_doru/melt_wall", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> CHAMPION = DFTriggers.TRIGGERS.register("doru_doru/champion", PlayerTrigger::new);

   static void init() {
   }

   private DoruTriggers() {
   }
}
