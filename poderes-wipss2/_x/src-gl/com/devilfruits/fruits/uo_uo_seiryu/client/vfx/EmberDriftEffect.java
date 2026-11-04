package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class EmberDriftEffect extends VfxEffect {
   private final int entityId;
   private final int duration;
   private final boolean flash;
   private double x;
   private double y;
   private double z;
   private float height = 3.6F;
   private boolean stopped;

   public EmberDriftEffect(Entity entity, int duration, boolean flash) {
      this.entityId = entity.getId();
      this.duration = Math.max(1, duration);
      this.flash = flash;
      this.x = entity.getX();
      this.y = entity.getY();
      this.z = entity.getZ();
   }

   public int entityId() {
      return this.entityId;
   }

   public void stop() {
      this.stopped = true;
      this.finish();
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = level.getEntity(this.entityId);
      if (e != null && this.age <= this.duration && !this.stopped) {
         this.x = e.getX();
         this.y = e.getY();
         this.z = e.getZ();
         this.height = e.getBbHeight();
         float left = 1.0F - (float)this.age / (float)this.duration;
         int n = Fx.count(this.flash ? 4.0F : 2.5F * left);

         for (int i = 0; i < n; i++) {
            double a = Fx.random().nextDouble() * Math.PI * 2.0;
            double d = (double)this.height * (0.15 + Fx.random().nextDouble() * 0.3);
            double px = this.x + Math.cos(a) * d;
            double py = this.y + Fx.random().nextDouble() * (double)this.height;
            double pz = this.z + Math.sin(a) * d;
            if (this.flash) {
               Fx.spawn((ParticleOptions)SeiryuParticles.FLAME.get(), px, py, pz, Fx.spread(0.04), 0.06, Fx.spread(0.04));
            }

            Fx.spawn((ParticleOptions)SeiryuParticles.EMBER.get(), px, py, pz, Fx.spread(0.05), 0.04 + Fx.random().nextDouble() * 0.05, Fx.spread(0.05));
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      float t = Mth.clamp(((float)this.age + ctx.partialTick) / (float)this.duration, 0.0F, 1.0F);
      if (this.flash) {
         float near = ctx.proximity(this.x, this.y + (double)this.height * 0.55, this.z, 2.0F, 6.0F, 0.25F);
         SeiryuDraw.flare(
            ctx,
            ctx.rx(this.x),
            ctx.ry(this.y + (double)this.height * 0.55),
            ctx.rz(this.z),
            this.height * (0.35F + 0.45F * t),
            16777215,
            16742938,
            (0.25F + 0.65F * t) * near
         );
      }
   }

   @Override
   public double x() {
      return this.x;
   }

   @Override
   public double y() {
      return this.y + (double)this.height * 0.5;
   }

   @Override
   public double z() {
      return this.z;
   }
}
