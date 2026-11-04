package com.devilfruits.fruits.yomi_yomi;

import com.devilfruits.registry.DFTriggers;
import java.util.function.Supplier;
import net.minecraft.advancements.critereon.PlayerTrigger;

public final class YomiTriggers {
   public static final Supplier<PlayerTrigger> SOUL_DEPARTED = DFTriggers.TRIGGERS.register("yomi_yomi/soul_departed", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> REVIVED = DFTriggers.TRIGGERS.register("yomi_yomi/revived", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> SKELETON = DFTriggers.TRIGGERS.register("yomi_yomi/skeleton", PlayerTrigger::new);

   static void init() {
   }

   private YomiTriggers() {
   }
}
