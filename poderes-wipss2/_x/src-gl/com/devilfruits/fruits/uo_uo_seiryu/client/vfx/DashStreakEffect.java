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

public class DashStreakEffect extends VfxEffect {
   private static final int HISTORY = 16;
   private static final Vector3f A = new Vector3f();
   private static final Vector3f B = new Vector3f();
   private static final Vector3f W = new Vector3f();
   private final int entityId;
   private final int duration;
   private final double[] hx = new double[16];
   private final double[] hy = new double[16];
   private final double[] hz = new double[16];
   private final float phase;
   private int count;
   private double x;
   private double y;
   private double z;

   public DashStreakEffect(Entity entity, int duration, long seed) {
      this.entityId = entity.getId();
      this.duration = duration;
      this.phase = (float)(seed & 255L) / 40.0F;
      this.x = entity.getX();
      this.y = entity.getY() + (double)entity.getBbHeight() * 0.55;
      this.z = entity.getZ();
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity entity = level.getEntity(this.entityId);
      if (entity != null && this.age <= this.duration + 16) {
         if (this.age <= this.duration) {
            for (int i = 15; i > 0; i--) {
               this.hx[i] = this.hx[i - 1];
               this.hy[i] = this.hy[i - 1];
               this.hz[i] = this.hz[i - 1];
            }

            this.x = entity.getX();
            this.y = entity.getY() + (double)entity.getBbHeight() * 0.55;
            this.z = entity.getZ();
            this.hx[0] = this.x;
            this.hy[0] = this.y;
            this.hz[0] = this.z;
            this.count = Math.min(16, this.count + 1);
            int n = Fx.count(3);

            for (int i = 0; i < n; i++) {
               Fx.spawn(
                  i == 0 ? (ParticleOptions)SeiryuParticles.GOLD_SPARK.get() : (ParticleOptions)SeiryuParticles.SCALE_SHARD.get(),
                  this.x + Fx.spread(0.5),
                  this.y + Fx.spread(0.5),
                  this.z + Fx.spread(0.5),
                  Fx.spread(0.05),
                  0.02,
                  Fx.spread(0.05)
               );
            }
         } else {
            this.count = Math.max(0, this.count - 1);
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void renderGlow(VfxContext ctx) {
      if (this.count >= 2) {
         float time = ctx.time;

         for (int i = 1; i < this.count; i++) {
            float fade = 1.0F - (float)i / 16.0F;
            float sway0 = Mth.sin(time * 0.6F + this.phase + (float)(i - 1) * 0.8F) * 0.35F * (float)(i - 1) / 16.0F;
            float sway1 = Mth.sin(time * 0.6F + this.phase + (float)i * 0.8F) * 0.35F * (float)i / 16.0F;
            A.set(ctx.rx(this.hx[i - 1]), ctx.ry(this.hy[i - 1]) + sway0, ctx.rz(this.hz[i - 1]));
            B.set(ctx.rx(this.hx[i]), ctx.ry(this.hy[i]) + sway1, ctx.rz(this.hz[i]));
            W.set(B).sub(A).cross(A.x + B.x, A.y + B.y, A.z + B.z);
            if (!(W.lengthSquared() < 1.0E-8F)) {
               W.normalize();
               float body = (0.32F + 0.12F * Mth.sin((float)i * 0.9F)) * fade;
               VfxDraw.ribbon(ctx.buffer, ctx.matrix, A, B, W, body * 1.8F, 22, 80, 140, (int)(70.0F * fade));
               VfxDraw.ribbon(ctx.buffer, ctx.matrix, A, B, W, body, 58, 166, 227, (int)(180.0F * fade));
               VfxDraw.ribbon(ctx.buffer, ctx.matrix, A, B, W, body * 0.25F, 242, 210, 90, (int)(220.0F * fade));
            }
         }
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
}
