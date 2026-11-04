package com.awakenedmod.awakened.core.relic;

import net.minecraft.world.entity.player.Player;

public final class RelicContext {
   private final Player player;

   public RelicContext(Player player) {
      this.player = player;
   }

   public Player player() {
      return this.player;
   }

   public float healthFraction() {
      float max = this.player.getMaxHealth();
      return max <= 0.0F ? 0.0F : Math.min(1.0F, this.player.getHealth() / max);
   }

   public float missingHealthFraction() {
      return 1.0F - this.healthFraction();
   }

   public boolean inWater() {
      return this.player.isInWater();
   }

   public boolean sprinting() {
      return this.player.isSprinting();
   }

   public boolean airborne() {
      return !this.player.onGround() && !this.player.isInWater() && !this.player.onClimbable();
   }

   public boolean recentlyHurt() {
      return this.player.hurtTime > 0;
   }
}
