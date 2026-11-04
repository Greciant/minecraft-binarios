package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.fruits.suna_suna.SunaGround;
import com.devilfruits.fruits.suna_suna.SunaParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class SandRiseEffect extends VfxEffect {
   private static final int LUMPS = 9;
   private final Entity caster;
   private final int duration;
   private int stoppedAt = -1;
   private double x;
   private double y;
   private double z;

   public SandRiseEffect(Entity caster, int duration) {
      this.caster = caster;
      this.duration = Math.max(1, duration);
      this.x = caster.getX();
      this.y = caster.getY();
      this.z = caster.getZ();
   }

   public boolean follows(Entity entity) {
      return this.caster == entity;
   }

   public void stop() {
      if (this.stoppedAt < 0) {
         this.stoppedAt = this.age;
      }
   }

   private float charge(float t) {
      float c = Mth.clamp(t / (float)this.duration, 0.0F, 1.0F);
      return this.stoppedAt < 0 ? c : c * Math.max(0.0F, 1.0F - (float)(this.age - this.stoppedAt) / 6.0F);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (!this.caster.isRemoved() && this.age <= this.duration + 60 && (this.stoppedAt < 0 || this.age - this.stoppedAt <= 6)) {
         this.x = this.caster.getX();
         this.y = this.caster.getY();
         this.z = this.caster.getZ();
         if (this.stoppedAt < 0) {
            RandomSource r = Fx.random();
            float c = this.charge((float)this.age);
            int n = Fx.count(1.0F + 4.0F * c);

            for (int i = 0; i < n; i++) {
               Vec3 p = this.lump(level, r.nextInt(9), 1.0F);
               Fx.spawn(
                  r.nextInt(3) == 0 ? (ParticleOptions)SunaParticles.DUST.get() : (ParticleOptions)SunaParticles.GRAIN.get(),
                  p.x + Fx.spread(0.3),
                  p.y + r.nextDouble() * (double)c * 1.5,
                  p.z + Fx.spread(0.3),
                  Fx.spread(0.04),
                  0.04 + r.nextDouble() * 0.06,
                  Fx.spread(0.04)
               );
            }
         }
      } else {
         this.finish();
      }
   }

   private Vec3 lump(ClientLevel level, int i, float pt) {
      float yaw = Mth.lerp(pt, this.caster.yRotO, this.caster.getYRot()) * (float) (Math.PI / 180.0);
      double fx = (double)(-Mth.sin(yaw));
      double fz = (double)Mth.cos(yaw);
      double a = ((double)i / 8.0 - 0.5) * 1.9;
      double d = 2.2 + 0.4 * Math.cos(a * 2.5);
      double lx = fx * Math.cos(a) - fz * Math.sin(a);
      double lz = fz * Math.cos(a) + fx * Math.sin(a);
      double px = Mth.lerp((double)pt, this.caster.xo, this.caster.getX()) + lx * d;
      double pz = Mth.lerp((double)pt, this.caster.zo, this.caster.getZ()) + lz * d;
      double g = SunaGround.surface(level, px, this.caster.getY(), pz, 2, 3);
      return new Vec3(px, Double.isNaN(g) ? this.caster.getY() : g, pz);
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      float t = (float)this.age + ctx.partialTick;
      float c = this.charge(t);
      if (!(c <= 0.02F) && this.caster.level() instanceof ClientLevel level) {
         VertexConsumer var11 = SandArt.solid(ctx, SandArt.PACKED);

         for (int i = 0; i < 9; i++) {
            Vec3 p = this.lump(level, i, ctx.partialTick);
            float h = c * (0.6F + 0.5F * (float)(i * 7 % 3) / 2.0F) + 0.08F * Mth.sin(t * 0.5F + (float)i);
            int light = SandArt.light(p.x, p.y + 0.5, p.z);
            Quaternionf rot = new Quaternionf().rotateY((float)i * 1.3F).rotateX(0.2F * Mth.sin(t * 0.3F + (float)i));
            SandArt.chunk(ctx, var11, p.x, p.y + (double)h * 0.5 - 0.2, p.z, 0.3F, h * 0.5F + 0.2F, 0.3F, rot, i * 5, light);
            if (c > 0.5F) {
               SandArt.chunk(
                  ctx, var11, p.x, p.y + (double)h + 0.15 * (double)c, p.z, 0.18F, 0.18F, 0.18F, new Quaternionf(rot).rotateY(t * 0.2F), i * 5 + 2, light
               );
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
