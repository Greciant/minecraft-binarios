package com.devilfruits.fruits.gura_gura.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.CrackGeometry;
import com.devilfruits.client.render.VfxDraw;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.fruits.gura_gura.GuraParticles;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class BubbleEffect extends VfxEffect {
   private static final float FIRST_PERSON_SCALE = 0.42F;
   private final int entityId;
   private final BubbleEffect.Anchor anchor;
   private final float[] cracks;
   private final float maxRadius;
   private final float growTicks;
   private final int autoShatter;
   private int stopAge = -1;
   private int shatterAge = -1;
   private double shatterX;
   private double shatterY;
   private double shatterZ;
   private double x;
   private double y;
   private double z;
   private double ax;
   private double ay;
   private double az;
   private float anchorScale = 1.0F;
   private static final Vector3f AX = new Vector3f();
   private static final Vector3f AY = new Vector3f();
   private static final Vector3f AZ = new Vector3f();

   public BubbleEffect(Entity entity, BubbleEffect.Anchor anchor, long seed, float maxRadius, float growTicks, int autoShatter, int starts, int steps) {
      this.entityId = entity.getId();
      this.anchor = anchor;
      this.cracks = CrackGeometry.sphere(seed, starts, steps);
      this.maxRadius = maxRadius;
      this.growTicks = Math.max(1.0F, growTicks);
      this.autoShatter = autoShatter;
      this.resolveAnchor(entity, 1.0F, false);
      this.x = this.ax;
      this.y = this.ay;
      this.z = this.az;
   }

   public int entityId() {
      return this.entityId;
   }

   public void shatter(Vec3 dir) {
      if (this.shatterAge < 0) {
         this.shatterAge = this.age;
         this.shatterX = dir.x;
         this.shatterY = dir.y;
         this.shatterZ = dir.z;
         this.spawnShards();
      }
   }

   public void stop() {
      if (this.stopAge < 0) {
         this.stopAge = this.age;
      }
   }

   private float charge(float partial) {
      return Mth.clamp(((float)this.age + partial) / this.growTicks, 0.0F, 1.0F);
   }

   private float radius(float partial, float time) {
      float c = this.charge(partial);
      float r = this.maxRadius * (0.3F + 0.7F * (1.0F - (1.0F - c) * (1.0F - c)));
      if (c >= 1.0F) {
         r *= 1.0F + 0.05F * Mth.sin(time * 2.2F);
      }

      if (this.shatterAge >= 0) {
         float s = Mth.clamp(((float)(this.age - this.shatterAge) + partial) / 3.0F, 0.0F, 1.0F);
         r *= 1.0F + 0.8F * s;
      }

      return r * this.anchorScale;
   }

   private float alpha(float partial) {
      float a = 1.0F;
      if (this.stopAge >= 0) {
         a *= 1.0F - Mth.clamp(((float)(this.age - this.stopAge) + partial) / 5.0F, 0.0F, 1.0F);
      }

      if (this.shatterAge >= 0) {
         a *= 1.0F - Mth.clamp(((float)(this.age - this.shatterAge) + partial) / 3.0F, 0.0F, 1.0F);
      }

      return a;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity entity = level.getEntity(this.entityId);
      if (entity != null && (this.stopAge < 0 || this.age - this.stopAge <= 5) && (this.shatterAge < 0 || this.age - this.shatterAge <= 3)) {
         this.resolveAnchor(entity, 1.0F, false);
         this.x = this.ax;
         this.y = this.ay;
         this.z = this.az;
         if (this.autoShatter >= 0 && this.age >= this.autoShatter && this.shatterAge < 0) {
            this.shatter(new Vec3(0.0, 0.3, 0.0));
         }

         if (this.shatterAge < 0 && this.stopAge < 0) {
            float c = this.charge(0.0F);
            int n = Fx.count(c >= 1.0F ? 2 : 1);

            for (int i = 0; i < n; i++) {
               double r = (double)this.maxRadius * (0.4 + 0.6 * (double)c);
               Fx.spawn((ParticleOptions)GuraParticles.CRACK_SPARK.get(), this.x + Fx.spread(r), this.y + Fx.spread(r), this.z + Fx.spread(r), 0.0, 0.01, 0.0);
            }
         }
      } else {
         this.finish();
      }
   }

   private void spawnShards() {
      int count = Fx.count(Math.max(10, (int)(this.maxRadius * 36.0F)));
      int segments = this.cracks.length / 8;
      float r = this.maxRadius * this.anchorScale;

      for (int i = 0; i < count; i++) {
         int o = i * 7 % Math.max(1, segments) * 8;
         float mx = this.cracks[o];
         float my = this.cracks[o + 1];
         float mz = this.cracks[o + 2];
         double out = 0.12 + Fx.random().nextDouble() * 0.2;
         Fx.spawn(
            (ParticleOptions)GuraParticles.GLASS_SHARD.get(),
            this.x + (double)(mx * r),
            this.y + (double)(my * r),
            this.z + (double)(mz * r),
            (double)mx * out + this.shatterX * 0.5 + Fx.spread(0.05),
            (double)my * out + this.shatterY * 0.5 + 0.05,
            (double)mz * out + this.shatterZ * 0.5 + Fx.spread(0.05)
         );
      }

      for (int i = 0; i < Fx.count(8); i++) {
         Fx.spawn(
            (ParticleOptions)GuraParticles.CRACK_SPARK.get(),
            this.x,
            this.y,
            this.z,
            Fx.spread(0.3) + this.shatterX * 0.3,
            Fx.spread(0.3),
            Fx.spread(0.3) + this.shatterZ * 0.3
         );
      }
   }

   private void resolveAnchor(Entity entity, float partial, boolean renderFrame) {
      double bx = Mth.lerp((double)partial, entity.xo, entity.getX());
      double by = Mth.lerp((double)partial, entity.yo, entity.getY());
      double bz = Mth.lerp((double)partial, entity.zo, entity.getZ());
      this.anchorScale = 1.0F;
      if (this.anchor == BubbleEffect.Anchor.BODY) {
         this.ax = bx;
         this.ay = by + (double)entity.getBbHeight() * 0.55;
         this.az = bz;
      } else {
         Minecraft mc = Minecraft.getInstance();
         Camera cam = mc.gameRenderer.getMainCamera();
         if (renderFrame && entity == mc.player && !cam.isDetached()) {
            Vector3f look = cam.getLookVector();
            Vector3f left = cam.getLeftVector();
            Vector3f up = cam.getUpVector();
            Vec3 c = cam.getPosition();
            this.ax = c.x + (double)look.x * 1.15 - (double)left.x * 0.5 - (double)up.x * 0.38;
            this.ay = c.y + (double)look.y * 1.15 - (double)left.y * 0.5 - (double)up.y * 0.38;
            this.az = c.z + (double)look.z * 1.15 - (double)left.z * 0.5 - (double)up.z * 0.38;
            this.anchorScale = 0.42F;
         } else {
            float yaw = (entity instanceof LivingEntity living ? Mth.rotLerp(partial, living.yBodyRotO, living.yBodyRot) : entity.getYRot())
               * (float) (Math.PI / 180.0);
            double fx = (double)(-Mth.sin(yaw));
            double fz = (double)Mth.cos(yaw);
            double rx = (double)(-Mth.cos(yaw));
            double rz = (double)(-Mth.sin(yaw));
            this.ax = bx + rx * 0.45 + fx * 0.15;
            this.ay = by + (entity.isCrouching() ? 1.0 : 1.3);
            this.az = bz + rz * 0.45 + fz * 0.15;
         }
      }
   }

   private boolean resolveForRender(VfxContext ctx) {
      ClientLevel level = Minecraft.getInstance().level;
      Entity entity = level == null ? null : level.getEntity(this.entityId);
      if (entity == null) {
         return false;
      } else {
         this.resolveAnchor(entity, ctx.partialTick, true);
         return true;
      }
   }

   @Override
   public void renderShell(VfxContext ctx) {
      if (this.resolveForRender(ctx)) {
         float r = this.radius(ctx.partialTick, ctx.time);
         float a = this.alpha(ctx.partialTick) * ctx.proximity(this.ax, this.ay, this.az, 0.6F, 2.0F, 0.45F);
         VfxDraw.shell(ctx, ctx.rx(this.ax), ctx.ry(this.ay), ctx.rz(this.az), r, r, r, 8, 14, 205, 240, 255, 0.03F * a, 0.32F * a);
      }
   }

   @Override
   public void renderGlow(VfxContext ctx) {
      if (this.resolveForRender(ctx)) {
         float r = this.radius(ctx.partialTick, ctx.time);
         float a = this.alpha(ctx.partialTick) * ctx.proximity(this.ax, this.ay, this.az, 0.6F, 2.0F, 0.55F);
         AX.set(r, 0.0F, 0.0F);
         AY.set(0.0F, r, 0.0F);
         AZ.set(0.0F, 0.0F, r);
         float progress = Math.min(1.05F, this.charge(ctx.partialTick) * 1.05F + 0.05F);
         float core = Math.max(0.012F, r * 0.035F);
         QuakeDraw.cracks(
            ctx, this.cracks, ctx.rx(this.ax), ctx.ry(this.ay), ctx.rz(this.az), AX, AY, AZ, QuakeDraw.Orientation.SPHERE, progress, core, core * 4.0F, a
         );
      }
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

   public static enum Anchor {
      FIST,
      BODY;
   }
}
