package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.fruits.suna_suna.SunaParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class SandGatherEffect extends VfxEffect {
   private final Entity caster;
   private final int duration;
   private final RandomSource random = RandomSource.create();
   private double x;
   private double y;
   private double z;

   public SandGatherEffect(Entity caster, int duration) {
      this.caster = caster;
      this.duration = Math.max(1, duration);
      Vec3 hand = hand(caster, 1.0F);
      this.x = hand.x;
      this.y = hand.y;
      this.z = hand.z;
   }

   public boolean follows(Entity entity) {
      return this.caster == entity;
   }

   public static Vec3 hand(Entity entity, float partialTick) {
      Vec3 look = entity.getViewVector(partialTick);
      Vec3 right = new Vec3(-look.z, 0.0, look.x);
      right = right.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
      return ownFirstPerson(entity)
         ? entity.getEyePosition(partialTick).add(look.scale(1.1)).add(right.scale(0.5)).add(0.0, -0.5, 0.0)
         : entity.getEyePosition(partialTick).add(look.scale(0.55)).add(right.scale(0.32)).add(0.0, -0.3, 0.0);
   }

   private static boolean ownFirstPerson(Entity entity) {
      Minecraft mc = Minecraft.getInstance();
      return entity == mc.player && mc.options.getCameraType().isFirstPerson();
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (!this.caster.isRemoved() && this.age <= this.duration + 40) {
         Vec3 hand = hand(this.caster, 1.0F);
         this.x = hand.x;
         this.y = hand.y;
         this.z = hand.z;
         float charge = Mth.clamp((float)this.age / (float)this.duration, 0.0F, 1.0F);
         boolean firstPerson = ownFirstPerson(this.caster);
         float scale = firstPerson ? 0.4F : 1.0F;
         int in = firstPerson ? 0 : Fx.count(2.0F + 3.0F * charge);

         for (int i = 0; i < in; i++) {
            double a = this.random.nextDouble() * Math.PI * 2.0;
            double b = (this.random.nextDouble() - 0.5) * Math.PI;
            double d = 1.2 + this.random.nextDouble() * 0.8;
            double ox = Math.cos(a) * Math.cos(b) * d;
            double oy = Math.sin(b) * d;
            double oz = Math.sin(a) * Math.cos(b) * d;
            Fx.spawn((ParticleOptions)SunaParticles.GRAIN.get(), this.x + ox, this.y + oy, this.z + oz, -ox * 0.16, -oy * 0.16 + 0.05, -oz * 0.16);
         }

         double r = 0.12 + 0.28 * (double)charge;
         int whirl = Fx.count((2.0F + 5.0F * charge) * scale);

         for (int i = 0; i < whirl; i++) {
            double a = (double)this.age * 0.8 + (double)i * 2.1;
            Fx.spawn(
               i % 3 == 0 && !firstPerson ? (ParticleOptions)SunaParticles.CLUMP.get() : (ParticleOptions)SunaParticles.GRAIN.get(),
               this.x + Math.cos(a) * r,
               this.y + Fx.spread(r * 0.5),
               this.z + Math.sin(a) * r,
               -Math.sin(a) * 0.08,
               0.03,
               Math.cos(a) * 0.08
            );
         }
      } else {
         this.finish();
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
