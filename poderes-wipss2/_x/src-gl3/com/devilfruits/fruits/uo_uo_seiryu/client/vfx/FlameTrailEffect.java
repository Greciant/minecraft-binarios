package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class FlameTrailEffect extends VfxEffect {
   private final double ax;
   private final double ay;
   private final double az;
   private final double bx;
   private final double by;
   private final double bz;
   private final float width;
   private final int linger;

   public FlameTrailEffect(Vec3 from, Vec3 to, float width, int linger) {
      this.ax = from.x;
      this.ay = from.y;
      this.az = from.z;
      this.bx = to.x;
      this.by = to.y;
      this.bz = to.z;
      this.width = width;
      this.linger = Math.max(10, linger);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > this.linger) {
         this.finish();
      } else {
         float strength = 1.0F - (float)this.age / (float)this.linger;
         double len = Math.sqrt(
            (this.bx - this.ax) * (this.bx - this.ax) + (this.by - this.ay) * (this.by - this.ay) + (this.bz - this.az) * (this.bz - this.az)
         );
         int n = Fx.count(Math.max(1, (int)(len * 1.6 * (double)strength)));

         for (int i = 0; i < n; i++) {
            double t = Fx.random().nextDouble();
            double x = Mth.lerp(t, this.ax, this.bx) + Fx.spread((double)this.width * 0.6);
            double y = Mth.lerp(t, this.ay, this.by) + Fx.spread((double)this.width * 0.3);
            double z = Mth.lerp(t, this.az, this.bz) + Fx.spread((double)this.width * 0.6);
            if (i % 3 == 0) {
               Fx.spawn((ParticleOptions)SeiryuParticles.CLOUD.get(), x, y, z, Fx.spread(0.01), 0.01, Fx.spread(0.01));
            } else {
               Fx.spawn((ParticleOptions)SeiryuParticles.FLAME.get(), x, y, z, Fx.spread(0.02), 0.03, Fx.spread(0.02));
            }
         }
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      float strength = 1.0F - ((float)this.age + ctx.partialTick) / (float)this.linger;
      if (!(strength <= 0.0F)) {
         int steps = 6;

         for (int i = 0; i <= steps; i++) {
            double t = (double)i / (double)steps;
            float x = ctx.rx(Mth.lerp(t, this.ax, this.bx));
            float y = ctx.ry(Mth.lerp(t, this.ay, this.by));
            float z = ctx.rz(Mth.lerp(t, this.az, this.bz));
            float flicker = 0.75F + 0.25F * Mth.sin(ctx.time * 1.7F + (float)i * 1.3F);
            SeiryuDraw.flare(ctx, x, y, z, this.width * 0.9F * flicker, 16758574, 14041882, strength * 0.35F);
         }
      }
   }

   @Override
   public double x() {
      return (this.ax + this.bx) * 0.5;
   }

   @Override
   public double y() {
      return (this.ay + this.by) * 0.5;
   }

   @Override
   public double z() {
      return (this.az + this.bz) * 0.5;
   }
}
