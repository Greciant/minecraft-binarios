package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.VfxDraw;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

public class TransformSwirlEffect extends VfxEffect {
   private static final int STRANDS = 2;
   private static final int STEPS = 22;
   private static final Vector3f A = new Vector3f();
   private static final Vector3f B = new Vector3f();
   private static final Vector3f W = new Vector3f();
   private final int entityId;
   private final int duration;
   private final boolean dragon;
   private int stoppedAt = -1;
   private double x;
   private double y;
   private double z;
   private double px;
   private double py;
   private double pz;
   private float height;

   public TransformSwirlEffect(Entity entity, int duration, boolean dragon) {
      this.entityId = entity.getId();
      this.duration = Math.max(1, duration);
      this.dragon = dragon;
      this.place(entity);
      this.px = this.x;
      this.py = this.y;
      this.pz = this.z;
   }

   public int entityId() {
      return this.entityId;
   }

   public void stop() {
      if (this.stoppedAt < 0) {
         this.stoppedAt = this.age;
      }
   }

   private void place(Entity entity) {
      this.x = entity.getX();
      this.y = entity.getY();
      this.z = entity.getZ();
      this.height = entity.getBbHeight() * (this.dragon ? 1.6F : 1.3F);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity entity = level.getEntity(this.entityId);
      if (entity != null && (this.stoppedAt < 0 || this.age - this.stoppedAt <= 6) && this.age <= this.duration + 40) {
         this.px = this.x;
         this.py = this.y;
         this.pz = this.z;
         this.place(entity);
         if (this.stoppedAt < 0) {
            float progress = Math.min(1.0F, (float)this.age / (float)this.duration);
            float radius = (this.dragon ? 2.2F : 1.4F) * (1.2F - 0.5F * progress);
            int n = Fx.count(this.dragon ? 4 : 3);

            for (int i = 0; i < n; i++) {
               float a = Fx.random().nextFloat() * (float) (Math.PI * 2);
               double hy = (double)(Fx.random().nextFloat() * this.height);
               Fx.spawn(
                  i % 3 == 0 ? (ParticleOptions)SeiryuParticles.BOLT_SPARK.get() : (ParticleOptions)SeiryuParticles.SCALE_SHARD.get(),
                  this.x + (double)(Mth.cos(a) * radius),
                  this.y + hy,
                  this.z + (double)(Mth.sin(a) * radius),
                  (double)(-Mth.sin(a)) * 0.15,
                  0.05,
                  (double)Mth.cos(a) * 0.15
               );
            }

            if (this.age % 4 == 0) {
               Fx.spawn(
                  (ParticleOptions)SeiryuParticles.GOLD_SPARK.get(),
                  this.x + Fx.spread((double)radius),
                  this.y + (double)(Fx.random().nextFloat() * this.height),
                  this.z + Fx.spread((double)radius),
                  0.0,
                  0.04,
                  0.0
               );
            }
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void renderGlow(VfxContext ctx) {
      float t = (float)this.age + ctx.partialTick;
      float fade = this.stoppedAt < 0 ? Math.min(1.0F, t / 5.0F) : Math.max(0.0F, 1.0F - (t - (float)this.stoppedAt) / 6.0F);
      if (!(fade <= 0.0F)) {
         float progress = Math.min(1.0F, t / (float)this.duration);
         float cx = ctx.rx(Mth.lerp((double)ctx.partialTick, this.px, this.x));
         float cy = ctx.ry(Mth.lerp((double)ctx.partialTick, this.py, this.y));
         float cz = ctx.rz(Mth.lerp((double)ctx.partialTick, this.pz, this.z));
         float baseRadius = (this.dragon ? 2.2F : 1.4F) * (1.2F - 0.5F * progress);
         float spin = t * (0.25F + 0.35F * progress);

         for (int s = 0; s < 2; s++) {
            int core = s == 0 ? 8050431 : 15913562;
            float offset = (float)s * (float) Math.PI;

            for (int i = 0; i < 22; i++) {
               float f0 = (float)i / 22.0F;
               float f1 = (float)(i + 1) / 22.0F;
               this.spiral(A, cx, cy, cz, f0, baseRadius, spin + offset);
               this.spiral(B, cx, cy, cz, f1, baseRadius, spin + offset);
               W.set(B).sub(A).cross(A.x + B.x, A.y + B.y, A.z + B.z);
               if (!(W.lengthSquared() < 1.0E-8F)) {
                  W.normalize();
                  float head = 1.0F - Math.abs(f0 - (progress * 0.7F + 0.3F)) * 1.4F;
                  int alpha = (int)(Mth.clamp(head, 0.15F, 1.0F) * fade * 200.0F);
                  VfxDraw.ribbon(ctx.buffer, ctx.matrix, A, B, W, 0.16F, core >> 16 & 0xFF, core >> 8 & 0xFF, core & 0xFF, alpha / 3);
                  VfxDraw.ribbon(ctx.buffer, ctx.matrix, A, B, W, 0.05F, 255, 255, 255, alpha);
               }
            }
         }
      }
   }

   private void spiral(Vector3f out, float cx, float cy, float cz, float f, float radius, float spin) {
      float angle = spin + f * (float) (Math.PI * 2) * 2.2F;
      float r = radius * (1.0F - 0.35F * f);
      out.set(cx + Mth.cos(angle) * r, cy + f * this.height, cz + Mth.sin(angle) * r);
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
