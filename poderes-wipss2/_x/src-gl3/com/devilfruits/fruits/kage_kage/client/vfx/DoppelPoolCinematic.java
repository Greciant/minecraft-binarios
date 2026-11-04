package com.devilfruits.fruits.kage_kage.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

public final class DoppelPoolCinematic extends KageCinematic {
   private final Vec3 at;
   private final int open;
   private final ShadowPool pool;

   public DoppelPoolCinematic(ClientLevel level, Vec3 feet, float radius, int open, long seed) {
      super(VfxAnchor.of(null, feet), seed);
      this.at = new Vec3(feet.x, VfxWorld.ground(level, feet.x, feet.y + 0.5, feet.z, 3), feet.z);
      this.open = Math.max(6, open);
      this.pool = new ShadowPool(this.at, radius, radius, seed);
      this.pools.add(this.pool);
      KageLook.sound(level, this.at, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), 0.9F, 0.55F);
      KageLook.sound(level, this.at, SoundEvents.ENDERMAN_TELEPORT, 0.4F, 0.5F);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age == 6) {
         this.pool.pulse(0.6F);
      }

      if (this.age < this.open && this.random.nextFloat() < 0.3F) {
         this.motes(level, this.at.add(this.jit((double)this.pool.radius()), 0.1, this.jit((double)this.pool.radius())), 1, 0.01);
      }

      if (this.age == this.open) {
         this.pool.sink();
      }

      if (this.age > this.open && this.pool.gone() && this.flakes.isEmpty()) {
         this.finish();
      }
   }

   @Override
   protected void drawModels(boolean glow, PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial, float entityPartial) {
   }

   @Override
   public double x() {
      return this.at.x;
   }

   @Override
   public double y() {
      return this.at.y;
   }

   @Override
   public double z() {
      return this.at.z;
   }
}
