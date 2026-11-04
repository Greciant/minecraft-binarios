package com.awakenedmod.awakened.client.render.limitless.infinity;

import com.awakenedmod.awakened.client.render.limitless.Ease;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record InfinityContact(Vec3 at, Vec3 from, float approach, float t, float hold, boolean melee, @Nullable Entity projectile) {
   public float released(float ticks) {
      return Ease.clamp01((this.t - this.hold) / ticks);
   }

   public boolean live(float tail) {
      return this.approach > 0.0F && this.t < this.hold + tail;
   }
}
