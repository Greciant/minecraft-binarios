package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.core.body.SerpentBody;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import com.devilfruits.fruits.uo_uo_seiryu.entity.SeiryuDragonEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;

public class TailTrailEffect extends VfxEffect {
   private static final int TRAIL = 10;
   private static final float[] AT = new float[]{0.97F, 0.82F};
   private final SeiryuDragonEntity shell;
   private final int duration;
   private final float[][] trail = new float[AT.length][30];
   private final float[] scratch = new float[30];
   private final double[] p = new double[3];
   private int count;
   private double x;
   private double y;
   private double z;

   public TailTrailEffect(SeiryuDragonEntity shell, int duration) {
      this.shell = shell;
      this.duration = duration;
      this.x = shell.getX();
      this.y = shell.getY();
      this.z = shell.getZ();
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      SerpentBody body = this.shell.body();
      if (!this.shell.isRemoved() && body != null && this.age <= this.duration + 10) {
         boolean swinging = body.action() == 1 && this.age <= this.duration;

         for (int k = 0; k < AT.length; k++) {
            float[] t = this.trail[k];
            System.arraycopy(t, 0, t, 3, 27);
            if (swinging) {
               body.point((double)(body.neck + body.length * AT[k]), 1.0F, this.p);
            } else {
               this.p[0] = (double)t[0];
               this.p[1] = (double)t[1];
               this.p[2] = (double)t[2];
            }

            t[0] = (float)this.p[0];
            t[1] = (float)this.p[1];
            t[2] = (float)this.p[2];
            if (k == 0) {
               this.x = this.p[0];
               this.y = this.p[1];
               this.z = this.p[2];
            }
         }

         this.count = Math.min(10, this.count + 1);
         if (swinging && this.age <= 12) {
            for (int i = 0; i < Fx.count(3); i++) {
               Fx.spawnFar(
                  (ParticleOptions)SeiryuParticles.CLOUD.get(), this.x + Fx.spread(1.2), this.y + Fx.spread(0.8), this.z + Fx.spread(1.2), 0.0, 0.03, 0.0
               );
            }
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void renderGlow(VfxContext ctx) {
      if (this.count >= 2) {
         float fade = this.age <= this.duration ? 1.0F : Math.max(0.0F, 1.0F - ((float)(this.age - this.duration) + ctx.partialTick) / 10.0F);

         for (int k = 0; k < AT.length; k++) {
            float[] t = this.trail[k];
            float ox = t[0];
            float oy = t[1];
            float oz = t[2];

            for (int i = 0; i < this.count; i++) {
               this.scratch[i * 3] = t[i * 3] - ox;
               this.scratch[i * 3 + 1] = t[i * 3 + 1] - oy;
               this.scratch[i * 3 + 2] = t[i * 3 + 2] - oz;
            }

            SeiryuDraw.ribbons(
               ctx,
               this.scratch,
               this.count,
               ctx.rx((double)ox),
               ctx.ry((double)oy),
               ctx.rz((double)oz),
               k == 0 ? 0.35F : 0.25F,
               1.0F,
               16777215,
               13165823,
               8040680,
               0.35F * fade * Mth.clamp((float)this.count / 3.0F, 0.0F, 1.0F)
            );
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

   @Override
   public double cullDistance() {
      return 160.0;
   }
}
