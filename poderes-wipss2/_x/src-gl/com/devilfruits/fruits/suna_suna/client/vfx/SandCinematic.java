package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.ParticleStyle;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class SandCinematic extends Cinematic {
   protected final RandomSource random;
   protected final long seed;
   protected double x;
   protected double y;
   protected double z;
   protected float height = 1.8F;
   protected double groundY = Double.NaN;

   protected SandCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(anchor, params);
      this.seed = params.seed();
      this.random = RandomSource.create(this.seed ^ 625341585L);
      Entity e = anchor.entity(level);
      if (e != null) {
         this.x = e.getX();
         this.y = e.getY();
         this.z = e.getZ();
         this.height = e.getBbHeight();
      } else {
         this.x = anchor.fallback().x;
         this.y = anchor.fallback().y;
         this.z = anchor.fallback().z;
      }

      this.groundY = VfxWorld.ground(level, this.x, this.y + 0.5, this.z, 8);
   }

   protected boolean followsCaster() {
      return true;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.followsCaster()) {
         Entity e = this.caster(level);
         if (e != null) {
            this.x = e.getX();
            this.y = e.getY();
            this.z = e.getZ();
            this.height = e.getBbHeight();
         }

         if (this.age % 4 == 1) {
            this.groundY = VfxWorld.ground(level, this.x, this.y + 0.5, this.z, 8);
         }
      }
   }

   @Nullable
   protected Entity caster(ClientLevel level) {
      return this.anchor.entity(level);
   }

   @Nullable
   protected Entity caster() {
      ClientLevel level = Minecraft.getInstance().level;
      return level != null ? this.anchor.entity(level) : null;
   }

   public boolean follows(Entity e) {
      return this.anchor.entityId() == e.getId();
   }

   protected boolean local() {
      return VfxWorld.local(this.anchor.entityId());
   }

   protected boolean firstPersonCaster() {
      return VfxWorld.firstPersonCaster(this.anchor.entityId());
   }

   protected float exposure(Vec3 at) {
      return VfxWorld.exposure(this.caster(), at, 8.0, 40.0);
   }

   protected double spread(double amount) {
      return (this.random.nextDouble() * 2.0 - 1.0) * amount;
   }

   public static int lit(Level level, double px, double py, double pz) {
      int g = Mth.clamp((int)(VfxWorld.surfaceLight(level, px, py, pz) * 255.0F), 0, 255);
      return g << 16 | g << 8 | g;
   }

   public static int scale(int rgb, float k) {
      int r = Mth.clamp((int)((float)(rgb >> 16 & 0xFF) * k), 0, 255);
      int g = Mth.clamp((int)((float)(rgb >> 8 & 0xFF) * k), 0, 255);
      int b = Mth.clamp((int)((float)(rgb & 0xFF) * k), 0, 255);
      return r << 16 | g << 8 | b;
   }

   public static VfxParticles.Handle grains(RandomSource r, double px, double py, double pz, double vx, double vy, double vz, int tint) {
      ParticleStyle s = VfxPresets.SAND_GRAINS[r.nextInt(VfxPresets.SAND_GRAINS.length)];
      return VfxParticles.emit(s, px, py, pz, vx, vy, vz).tint(tint).rotation(r.nextFloat() * (float) (Math.PI * 2));
   }

   public static VfxParticles.Handle plume(RandomSource r, double px, double py, double pz, double vx, double vy, double vz, int tint) {
      return VfxParticles.emit(VfxPresets.SAND_PLUME, px, py, pz, vx, vy, vz).tint(tint).rotation(r.nextFloat() * (float) (Math.PI * 2));
   }

   public static VfxParticles.Handle puff(RandomSource r, double px, double py, double pz, double vx, double vy, double vz, int tint) {
      return VfxParticles.emit(VfxPresets.SAND_PUFF, px, py, pz, vx, vy, vz).tint(tint).rotation(r.nextFloat() * (float) (Math.PI * 2));
   }

   public static VfxParticles.Handle haze(RandomSource r, double px, double py, double pz, double vx, double vy, double vz, int tint) {
      return VfxParticles.emit(VfxPresets.SAND_HAZE, px, py, pz, vx, vy, vz).tint(tint).rotation(r.nextFloat() * (float) (Math.PI * 2));
   }

   public static VfxParticles.Handle streak(double px, double py, double pz, double vx, double vy, double vz, int tint) {
      return VfxParticles.emit(VfxPresets.SAND_STREAK, px, py, pz, vx, vy, vz).tint(tint);
   }

   public static VfxParticles.Handle ring(RandomSource r, double px, double py, double pz, double vx, double vy, double vz, int tint) {
      return VfxParticles.emit(VfxPresets.SAND_RING, px, py, pz, vx, vy, vz).tint(tint).rotation(r.nextFloat() * (float) (Math.PI * 2));
   }

   public static void clouds(RandomSource r, double px, double py, double pz, double vx, double vy, double vz, float size, float count, int tint) {
      int n = Math.max(1, VfxSettings.count(count));

      for (int i = 0; i < n; i++) {
         Vec3 d = jitter(r, Vec3.ZERO, 1.0F).scale((double)size * (0.15 + r.nextDouble() * 0.3));
         double k = 0.6 + r.nextDouble() * 0.6;
         VfxParticles.Handle h = i % 2 == 0
            ? puff(r, px + d.x, py + d.y, pz + d.z, vx * k + d.x * 0.15, vy * k + d.y * 0.15 + 0.01, vz * k + d.z * 0.15, tint)
            : plume(r, px + d.x, py + d.y, pz + d.z, vx * k * 0.7 + d.x * 0.1, vy * k * 0.7 + d.y * 0.1 + 0.01, vz * k * 0.7 + d.z * 0.1, tint);
         h.size(size * (0.55F + r.nextFloat() * 0.5F));
      }
   }

   public static void decal(RandomSource r, double px, double gy, double pz, float size, int tint) {
      VfxParticles.emit(VfxPresets.SAND_DECAL, px, gy + 0.03, pz, 0.0, 0.0, 0.0).size(size).tint(tint).rotation(r.nextFloat() * (float) (Math.PI * 2));
   }

   public static void shadow(Level level, double px, double py, double pz, float size, float strength) {
      double g = VfxWorld.ground(level, px, py, pz, 8);
      if (!Double.isNaN(g)) {
         float k = (float)Math.max(0.0, 1.0 - (py - g) / 7.0) * strength * VfxWorld.surfaceLight(level, px, g + 1.0, pz);
         if (k > 0.02F) {
            VfxParticles.emit(VfxPresets.SHADOW, px, g + 0.02, pz, 0.0, 0.0, 0.0).size(size * (1.3F - 0.3F * k)).tint(0).alpha(0.55F * k);
         }
      }
   }

   public static void burst(RandomSource r, Vec3 at, Vec3 dir, float cone, float speed, float grains, float puffs, int tint) {
      int n = VfxSettings.count(grains);

      for (int i = 0; i < n; i++) {
         Vec3 d = jitter(r, dir, cone);
         double s = (double)speed * (0.4 + r.nextDouble() * 0.8);
         grains(r, at.x, at.y, at.z, d.x * s, d.y * s + 0.05, d.z * s, tint).life(0.8F + r.nextFloat() * 0.6F);
      }

      int m = VfxSettings.count(puffs);

      for (int i = 0; i < m; i++) {
         Vec3 d = jitter(r, dir, cone);
         double s = (double)speed * (0.15 + r.nextDouble() * 0.35);
         puff(r, at.x, at.y, at.z, d.x * s, d.y * s, d.z * s, tint).size(0.8F + r.nextFloat() * 0.6F);
      }
   }

   public static Vec3 jitter(RandomSource r, Vec3 dir, float cone) {
      Vec3 rand = new Vec3(r.nextDouble() * 2.0 - 1.0, r.nextDouble() * 2.0 - 1.0, r.nextDouble() * 2.0 - 1.0);
      Vec3 d = dir.lengthSqr() < 1.0E-6 ? rand : dir.normalize().scale((double)(1.0F - cone)).add(rand.scale((double)cone * 1.4));
      return d.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : d.normalize();
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
