package com.devilfruits.fruits.kage_kage.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class AegisBlockCinematic extends KageCinematic {
   private static final float SIZE = 0.34F;
   private final Player user;
   private final ShadowChunk chunk;
   private final Vec3 at;

   public AegisBlockCinematic(Player user, Vec3 at, Vec3 velocity, long seed) {
      super(VfxAnchor.of(user, at), seed);
      this.user = user;
      this.at = at;
      this.chunk = new ShadowChunk(seed).snap(at);
      ClientLevel level = (ClientLevel)user.level();
      Vec3 dir = velocity.lengthSqr() > 1.0E-6 ? velocity.normalize() : at.subtract(user.getBoundingBox().getCenter()).normalize().scale(-1.0);
      this.flash(level, at, 1.1F, 0.8F);
      this.motes(level, at, 6, 0.06);

      for (int i = 0; i < 7; i++) {
         Vec3 v = dir.scale(-0.05 - this.random.nextDouble() * 0.05).add(this.jit(0.12), this.random.nextDouble() * 0.12, this.jit(0.12));
         this.flakes.burst(at.add(this.jit(0.15), this.jit(0.15), this.jit(0.15)), v, 0.05F + this.random.nextFloat() * 0.05F, 12 + this.random.nextInt(8));
      }

      KageLook.sound(level, at, SoundEvents.AMETHYST_BLOCK_HIT, 1.0F, 0.55F);
      KageLook.sound(level, at, SoundEvents.SOUL_SAND_BREAK, 0.8F, 0.8F);
      KageLook.sound(level, at, SoundEvents.BAT_HURT, 0.45F, 0.6F);
      if (VfxWorld.local(user.getId())) {
         VfxClock.hitStop(2);
         CameraDirector.shake(0.15F, 0.08F, 1.2F, 18.0F);
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.chunk.tick();
      int t = this.age;
      if (t <= 2) {
         this.chunk.radius = 0.34F * outBack((float)t / 2.0F);
      } else if (t <= 6) {
         this.chunk.radius = 0.34F * (1.0F + 0.04F * Mth.sin((float)t * 2.1F));
      } else {
         float k = Mth.clamp((float)(t - 6) / 8.0F, 0.0F, 1.0F);
         this.chunk.radius = 0.34F * (1.0F - k * k);
         this.chunk.pos = this.chunk.pos.lerp(this.user.getBoundingBox().getCenter(), 0.16);
      }

      float sq = t <= 2 ? 0.7F : 1.0F - 0.3F * (float)Math.exp((double)((float)(-(t - 2)) * 0.6F)) * Mth.cos((float)(t - 2) * 1.4F);
      this.chunk.sy = sq;
      this.chunk.sx = this.chunk.sz = 1.0F / Mth.sqrt(sq);
      this.chunk.heat = 1.0F + Mth.clamp(1.1F - (float)t * 0.18F, 0.0F, 1.1F);
      if (t > 16 && this.flakes.isEmpty()) {
         this.finish();
      }
   }

   @Override
   protected void drawModels(boolean glow, PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial, float entityPartial) {
      this.chunk.draw(glow, ps, buffers, cam, partial, light(this.at), this.rim);
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
