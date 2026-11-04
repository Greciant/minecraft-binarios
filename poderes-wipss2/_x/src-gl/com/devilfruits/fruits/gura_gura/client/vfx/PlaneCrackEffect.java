package com.devilfruits.fruits.gura_gura.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.CrackGeometry;
import com.devilfruits.client.render.VfxDraw;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.fruits.gura_gura.GuraParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class PlaneCrackEffect extends VfxEffect {
   private final double x;
   private final double y;
   private final double z;
   private final Vector3f normal = new Vector3f();
   private final Vector3f u = new Vector3f();
   private final Vector3f v = new Vector3f();
   private final float[] cracks;
   private final float radius;
   private final int growTicks;
   private final int holdTicks;
   private final int fadeTicks;
   private final int shards;
   private final boolean far;
   private final Vec3 shardPush;
   private float coreScale = 1.0F;
   private float fill = 0.05F;
   private boolean shattered;
   private static final Vector3f AX = new Vector3f();
   private static final Vector3f AY = new Vector3f();
   private static final Vector3f AZ = new Vector3f();

   public PlaneCrackEffect(
      Vec3 center, Vec3 normal, float radius, long seed, boolean web, int growTicks, int holdTicks, int fadeTicks, int shards, boolean far, Vec3 shardPush
   ) {
      this.x = center.x;
      this.y = center.y;
      this.z = center.z;
      this.normal.set((float)normal.x, (float)normal.y, (float)normal.z);
      if (this.normal.lengthSquared() < 1.0E-6F) {
         this.normal.set(0.0F, 1.0F, 0.0F);
      }

      this.normal.normalize();
      VfxDraw.basis(this.normal, this.u, this.v);
      this.radius = radius;
      int spokes = Mth.clamp(Math.round(5.0F + radius * 1.3F), 5, 16);
      this.cracks = web
         ? CrackGeometry.web(seed, spokes, Mth.clamp(Math.round(1.0F + radius * 0.6F), 1, 6))
         : CrackGeometry.burst(seed, 5 + (int)(seed & 3L), 0.25F);
      this.growTicks = Math.max(1, growTicks);
      this.holdTicks = holdTicks;
      this.fadeTicks = Math.max(1, fadeTicks);
      this.shards = shards;
      this.far = far;
      this.shardPush = shardPush;
   }

   public PlaneCrackEffect coreScale(float scale) {
      this.coreScale = scale;
      return this;
   }

   public PlaneCrackEffect fill(float alpha) {
      this.fill = alpha;
      return this;
   }

   private float progress(float partial) {
      float t = Mth.clamp(((float)this.age + partial) / (float)this.growTicks, 0.0F, 1.0F);
      return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
   }

   private float fadeT(float partial) {
      return Mth.clamp(((float)this.age + partial - (float)this.growTicks - (float)this.holdTicks) / (float)this.fadeTicks, 0.0F, 1.0F);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (!this.shattered && this.age >= this.growTicks + this.holdTicks) {
         this.shattered = true;
         this.shatter();
      }

      if (this.age > this.growTicks + this.holdTicks + this.fadeTicks) {
         this.finish();
      }
   }

   private void shatter() {
      int count = Fx.count(this.shards);
      int segments = this.cracks.length / 8;
      if (segments != 0) {
         for (int i = 0; i < count; i++) {
            int o = i * 13 % segments * 8;
            float t = Fx.random().nextFloat();
            float lx = Mth.lerp(t, this.cracks[o], this.cracks[o + 3]) * this.radius;
            float ly = Mth.lerp(t, this.cracks[o + 1], this.cracks[o + 4]) * this.radius;
            double px = this.x + (double)(this.u.x * lx) + (double)(this.v.x * ly);
            double py = this.y + (double)(this.u.y * lx) + (double)(this.v.y * ly);
            double pz = this.z + (double)(this.u.z * lx) + (double)(this.v.z * ly);
            double out = 0.04 + (double)this.radius * 0.01;
            double vx = (double)(this.u.x * lx + this.v.x * ly) / Math.max(0.1, (double)this.radius) * out + this.shardPush.x + Fx.spread(0.04);
            double vy = (double)(this.u.y * lx + this.v.y * ly) / Math.max(0.1, (double)this.radius) * out + this.shardPush.y + Fx.spread(0.04);
            double vz = (double)(this.u.z * lx + this.v.z * ly) / Math.max(0.1, (double)this.radius) * out + this.shardPush.z + Fx.spread(0.04);
            if (this.far) {
               Fx.spawnFar((ParticleOptions)GuraParticles.GLASS_SHARD.get(), px, py, pz, vx, vy, vz);
            } else {
               Fx.spawn((ParticleOptions)GuraParticles.GLASS_SHARD.get(), px, py, pz, vx, vy, vz);
            }
         }
      }
   }

   @Override
   public void renderShell(VfxContext ctx) {
      float fade = this.fadeT(ctx.partialTick);
      float a = this.fill * this.progress(ctx.partialTick) * (1.0F - fade) * ctx.proximity(this.x, this.y, this.z, 1.0F, 4.0F, 0.2F);
      if (a > 0.003F) {
         float r = this.radius * (1.0F + 0.15F * fade);
         VfxDraw.ring(ctx, ctx.rx(this.x), ctx.ry(this.y), ctx.rz(this.z), this.u, this.v, r * 0.5F, r, 28, 230, 248, 255, a);
      }
   }

   @Override
   public void renderGlow(VfxContext ctx) {
      float fade = this.fadeT(ctx.partialTick);
      float scale = this.radius * (1.0F + 0.15F * fade);
      AX.set(this.u).mul(scale);
      AY.set(this.v).mul(scale);
      AZ.set(this.normal).mul(scale);
      float near = ctx.proximity(this.x, this.y, this.z, 1.0F, 4.0F, 0.25F);
      float alpha = (1.0F - fade) * near;
      float core = Math.max(0.025F, Math.min(0.12F, this.radius * 0.015F)) * this.coreScale;
      boolean big = this.radius > 4.0F;
      float glow = core * (big ? 2.4F : 4.5F);
      QuakeDraw.cracks(
         ctx,
         this.cracks,
         ctx.rx(this.x),
         ctx.ry(this.y),
         ctx.rz(this.z),
         AX,
         AY,
         AZ,
         QuakeDraw.Orientation.PLANE,
         this.progress(ctx.partialTick) * 1.15F,
         core,
         glow,
         alpha,
         big ? 0.18F : 0.42F
      );
      float flash = 1.0F - Mth.clamp(((float)this.age + ctx.partialTick) / ((float)this.growTicks + 3.0F), 0.0F, 1.0F);
      if (flash > 0.0F) {
         float flashNear = ctx.proximity(this.x, this.y, this.z, 1.5F, 5.0F, 0.0F);
         VfxDraw.billboard(ctx, ctx.rx(this.x), ctx.ry(this.y), ctx.rz(this.z), Math.min(1.4F, this.radius * 0.16F), 220, 245, 255, flash * 0.6F * flashNear);
      }
   }

   @Override
   public double cullDistance() {
      return this.far ? 256.0 : 96.0;
   }

   @Override
   public double x() {
      return this.x;
   }

   @Override
   public double y() {
      return this.y;
   }

   @Override
   public double z() {
      return this.z;
   }
}
