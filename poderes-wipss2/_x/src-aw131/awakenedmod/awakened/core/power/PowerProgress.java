package com.awakenedmod.awakened.core.power;

import java.util.List;
import net.minecraft.network.chat.Component;

public record PowerProgress(float barFraction, Component barLeft, Component barRight, List<PowerProgress.Stat> stats) {
   public static final PowerProgress NONE = new PowerProgress(-1.0F, Component.empty(), Component.empty(), List.of());

   public boolean hasBar() {
      return this.barFraction >= 0.0F;
   }

   public boolean isEmpty() {
      return !this.hasBar() && this.stats.isEmpty();
   }

   public static record Stat(Component label, Component value) {
   }
}
