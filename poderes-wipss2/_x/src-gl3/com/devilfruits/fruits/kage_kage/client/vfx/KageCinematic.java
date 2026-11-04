package com.devilfruits.fruits.kage_kage.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public abstract class KageCinematic extends Cinematic implements KageRender.Drawable {
   protected final RimConsumer rim = new RimConsumer();
   protected final Flakes flakes = new Flakes();
   protected final List<ShadowPool> pools = new ArrayList<>();
   protected final RandomSource random;

   protected KageCinematic(VfxAnchor anchor, long seed) {
      super(anchor, VfxParams.NONE);
      this.random = RandomSource.create(seed);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age == 1) {
         KageRender.add(this);
      }

      this.flakes.tick();

      for (ShadowPool pool : this.pools) {
         pool.tick();
      }
   }

   @Override
   public void removed() {
      KageRender.remove(this);

      for (ShadowPool pool : this.pools) {
         pool.close();
      }
   }

   @Override
   public final void draw(boolean glow, PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial, float entityPartial) {
      if (!this.isDone()) {
         int light = light(new Vec3(this.x(), this.y(), this.z()));

         for (ShadowPool pool : this.pools) {
            pool.draw(glow, ps, buffers, cam, light);
         }

         this.flakes.draw(glow, ps, buffers, cam, partial, light, this.rim);
         this.drawModels(glow, ps, buffers, cam, partial, entityPartial);
      }
   }

   protected abstract void drawModels(boolean var1, PoseStack var2, MultiBufferSource var3, Vec3 var4, float var5, float var6);

   protected static int light(Vec3 at) {
      ClientLevel level = Minecraft.getInstance().level;
      return level == null ? 15728880 : LevelRenderer.getLightColor(level, BlockPos.containing(at));
   }

   protected double jit(double a) {
      return (this.random.nextDouble() * 2.0 - 1.0) * a;
   }

   protected void flash(ClientLevel level, Vec3 at, float size, float alpha) {
      if (!VfxSettings.legacy()) {
         VfxParticles.emit(KageLook.FLASH, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(size).tint(14708991).alpha(alpha);
         VfxParticles.emit(KageLook.GLOW, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(size * 1.8F).tint(12868576).alpha(alpha * 0.5F);
      } else {
         for (int i = 0; i < 4; i++) {
            level.addParticle(ParticleTypes.WITCH, at.x + this.jit(0.3), at.y + this.jit(0.3), at.z + this.jit(0.3), 0.0, 0.0, 0.0);
         }
      }
   }

   protected void motes(ClientLevel level, Vec3 at, int n, double speed) {
      if (VfxSettings.legacy()) {
         for (int i = 0; i < Math.max(1, n / 2); i++) {
            level.addParticle(i % 2 == 0 ? ParticleTypes.SQUID_INK : ParticleTypes.WITCH, at.x, at.y, at.z, this.jit(speed), this.jit(speed), this.jit(speed));
         }
      } else {
         for (int i = 0; i < VfxSettings.count((float)n); i++) {
            VfxParticles.emit(KageLook.MOTE, at.x, at.y, at.z, this.jit(speed), this.jit(speed) + 0.01, this.jit(speed)).tint(12868576);
         }
      }
   }

   protected static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, Vec3 d, float t) {
      float u = 1.0F - t;
      return a.scale((double)(u * u * u)).add(b.scale((double)(3.0F * u * u * t))).add(c.scale((double)(3.0F * u * t * t))).add(d.scale((double)(t * t * t)));
   }

   protected static float outBack(float x) {
      x = Mth.clamp(x, 0.0F, 1.0F);
      float c = 1.70158F;
      float y = x - 1.0F;
      return 1.0F + (c + 1.0F) * y * y * y + c * y * y;
   }
}
