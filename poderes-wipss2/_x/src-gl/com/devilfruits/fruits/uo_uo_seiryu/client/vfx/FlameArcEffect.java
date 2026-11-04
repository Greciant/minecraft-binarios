package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class FlameArcEffect extends VfxEffect {
   private static final int SWEEP = 5;
   private static final int LINGER = 40;
   private static final int POINTS = 25;
   private final double cx;
   private final double cy;
   private final double cz;
   private final float radius;
   private final float[] arc = new float[75];
   private final float[] scratch = new float[75];

   public FlameArcEffect(Vec3 center, Vec3 facing, float radius, float degrees) {
      this.cx = center.x;
      this.cy = center.y;
      this.cz = center.z;
      this.radius = radius;
      float base = (float)Math.atan2(facing.z, facing.x);
      float half = degrees * 0.5F * (float) (Math.PI / 180.0);

      for (int i = 0; i < 25; i++) {
         float t = (float)i / 24.0F;
         float a = base - half + 2.0F * half * t;
         this.arc[i * 3] = Mth.cos(a) * radius * 0.85F;
         this.arc[i * 3 + 1] = 0.4F - 0.8F * t;
         this.arc[i * 3 + 2] = Mth.sin(a) * radius * 0.85F;
      }
   }

   private float front(float age) {
      return Mth.clamp(age / 5.0F, 0.0F, 1.0F) * 24.0F;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > 45) {
         this.finish();
      } else if (this.age <= 5) {
         int head = (int)this.front((float)this.age);

         for (int i = Math.max(0, head - 4); i <= head; i++) {
            double x = this.cx + (double)this.arc[i * 3];
            double y = this.cy + (double)this.arc[i * 3 + 1];
            double z = this.cz + (double)this.arc[i * 3 + 2];

            for (int k = 0; k < Fx.count(3); k++) {
               Fx.spawn(
                  (ParticleOptions)SeiryuParticles.FLAME.get(),
                  x + Fx.spread(0.5),
                  y + Fx.spread(0.5),
                  z + Fx.spread(0.5),
                  Fx.spread(0.05),
                  0.04,
                  Fx.spread(0.05)
               );
            }

            Fx.spawn((ParticleOptions)SeiryuParticles.EMBER.get(), x, y, z, Fx.spread(0.2), 0.1 + (double)Fx.random().nextFloat() * 0.1, Fx.spread(0.2));
         }
      } else {
         float strength = 1.0F - (float)(this.age - 5) / 40.0F;
         int n = Fx.count(Math.max(1, (int)(6.0F * strength)));

         for (int k = 0; k < n; k++) {
            int i = Fx.random().nextInt(25);
            double x = this.cx + (double)this.arc[i * 3] * 1.05 + Fx.spread(0.6);
            double z = this.cz + (double)this.arc[i * 3 + 2] * 1.05 + Fx.spread(0.6);
            Fx.spawn((ParticleOptions)SeiryuParticles.FLAME.get(), x, this.cy - 1.2 + Fx.spread(0.2), z, 0.0, 0.03, 0.0);
         }
      }
   }

   @Override
   public void renderGlow(VfxContext ctx) {
      float age = (float)this.age + ctx.partialTick;
      float head = this.front(age);
      float fade = age <= 5.0F ? 1.0F : 1.0F - Mth.clamp((age - 5.0F) / 8.0F, 0.0F, 1.0F);
      if (!(fade <= 0.0F)) {
         int end = Math.min(24, (int)Math.ceil((double)head));
         int start = Math.max(0, end - 10);
         int count = 0;

         for (int i = end; i >= start; i--) {
            this.scratch[count * 3] = this.arc[i * 3];
            this.scratch[count * 3 + 1] = this.arc[i * 3 + 1];
            this.scratch[count * 3 + 2] = this.arc[i * 3 + 2];
            count++;
         }

         SeiryuDraw.ribbons(
            ctx,
            this.scratch,
            count,
            ctx.rx(this.cx),
            ctx.ry(this.cy),
            ctx.rz(this.cz),
            0.22F * this.radius / 6.0F + 0.12F,
            0.85F,
            16776160,
            16758574,
            14041882,
            0.9F * fade
         );
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      float age = (float)this.age + ctx.partialTick;
      float head = this.front(age);
      float fade = age <= 5.0F ? 1.0F : 1.0F - Mth.clamp((age - 5.0F) / 10.0F, 0.0F, 1.0F);
      if (!(fade <= 0.0F)) {
         int end = Math.min(24, (int)Math.ceil((double)head));

         for (int i = Math.max(0, end - 8); i <= end; i += 2) {
            float x = ctx.rx(this.cx + (double)this.arc[i * 3]);
            float y = ctx.ry(this.cy + (double)this.arc[i * 3 + 1]);
            float z = ctx.rz(this.cz + (double)this.arc[i * 3 + 2]);
            float lead = 1.0F - (float)(end - i) / 9.0F;
            float near = ctx.proximity(this.cx + (double)this.arc[i * 3], this.cy, this.cz + (double)this.arc[i * 3 + 2], 1.5F, 4.0F, 0.25F);
            SeiryuDraw.flare(ctx, x, y, z, (0.7F + 0.8F * lead) * Math.max(1.0F, this.radius / 6.0F), 16776160, 16758574, 0.55F * lead * fade * near);
         }
      }
   }

   @Override
   public double x() {
      return this.cx;
   }

   @Override
   public double y() {
      return this.cy;
   }

   @Override
   public double z() {
      return this.cz;
   }
}
