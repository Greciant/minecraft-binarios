package com.devilfruits.client.vfx.fx;

import com.devilfruits.client.render.vfx.FloorDebrisEffect;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.particle.ParticleStyle;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class Impact {
   private final ClientLevel level;
   private final Vec3 at;
   private final RandomSource random;
   @Nullable
   private Entity caster;
   private int palette = 0;
   private float flash;
   private int glowRgb = 16742938;
   private float waveRadius;
   private int waveTicks = 12;
   private int waveRing = -1;
   private int sparks;
   private float sparkSpeed = 0.4F;
   private int embers;
   private int smoke;
   private int smokeRgb = 2761760;
   private float smokeSize = 1.0F;
   private int debris;
   private float debrisSpeed = 0.22F;
   private float decal;
   private float trauma;
   private float traumaDecay = 0.08F;
   private float traumaDegrees = 3.5F;
   private int hitStop;
   private float fov;
   private float lens;
   private float screenFlash;
   private int dust;

   private Impact(ClientLevel level, Vec3 at, long seed) {
      this.level = level;
      this.at = at;
      this.random = RandomSource.create(seed);
   }

   public static Impact at(ClientLevel level, Vec3 at, long seed) {
      return new Impact(level, at, seed);
   }

   public Impact caster(@Nullable Entity e) {
      this.caster = e;
      return this;
   }

   public Impact thunder() {
      this.palette = 1;
      this.glowRgb = 11955455;
      return this;
   }

   public Impact flash(float size) {
      this.flash = size;
      return this;
   }

   public Impact glow(int rgb) {
      this.glowRgb = rgb;
      return this;
   }

   public Impact wave(float radius, int ticks, int ringRgb) {
      this.waveRadius = radius;
      this.waveTicks = ticks;
      this.waveRing = ringRgb;
      return this;
   }

   public Impact dust(int count) {
      this.dust = count;
      return this;
   }

   public Impact sparks(int count, float speed) {
      this.sparks = count;
      this.sparkSpeed = speed;
      return this;
   }

   public Impact embers(int count) {
      this.embers = count;
      return this;
   }

   public Impact smoke(int count, int rgb, float size) {
      this.smoke = count;
      this.smokeRgb = rgb;
      this.smokeSize = size;
      return this;
   }

   public Impact debris(int count, float speed) {
      this.debris = count;
      this.debrisSpeed = speed;
      return this;
   }

   public Impact decal(float size) {
      this.decal = size;
      return this;
   }

   public Impact trauma(float amount, float decayPerTick, float degrees) {
      this.trauma = amount;
      this.traumaDecay = decayPerTick;
      this.traumaDegrees = degrees;
      return this;
   }

   public Impact hitStop(int ticks) {
      this.hitStop = ticks;
      return this;
   }

   public Impact fov(float punch) {
      this.fov = punch;
      return this;
   }

   public Impact lens(float amount) {
      this.lens = amount;
      return this;
   }

   public Impact screenFlash(float amount) {
      this.screenFlash = amount;
      return this;
   }

   public void play() {
      boolean thunder = this.palette == 1;
      boolean firstPerson = this.caster != null && VfxWorld.firstPersonCaster(this.caster.getId());
      if (this.flash > 0.0F) {
         ParticleStyle star = thunder ? VfxPresets.THUNDER_FLARE : VfxPresets.FLARE;
         VfxParticles.emit(star, this.at.x, this.at.y, this.at.z, 0.0, 0.0, 0.0).size(this.flash).rotation(this.random.nextFloat() * (float) (Math.PI * 2));
         VfxParticles.emit(VfxPresets.GLOW, this.at.x, this.at.y, this.at.z, 0.0, 0.0, 0.0).size(this.flash * 4.0F).tint(this.glowRgb).alpha(0.75F).life(1.6F);
      }

      double gy = VfxWorld.ground(this.level, this.at.x, this.at.y + 0.5, this.at.z, 6);
      boolean floor = !Double.isNaN(gy) && this.at.y - gy < 4.5;
      float light = VfxWorld.light(this.level, this.at.x, this.at.y + 0.5, this.at.z);
      if (this.waveRadius > 0.0F && floor) {
         GroundWave w = GroundWave.at(this.at.x, gy, this.at.z, this.waveRadius, this.waveTicks);
         if (this.waveRing >= 0) {
            w.ring(this.waveRing, 0.85F);
         }

         if (this.dust > 0) {
            w.dust(
               this.dust, VfxWorld.shade(VfxWorld.groundColour(this.level, this.at.x, gy - 0.5, this.at.z), light), 0.9F, 0.34F, 0.6F, this.random.nextLong()
            );
         }

         w.spawn();
      }

      ParticleStyle spark = thunder ? VfxPresets.THUNDER_SPARK : VfxPresets.SPARK;

      for (int i = 0; i < VfxSettings.count((float)this.sparks); i++) {
         VfxParticles.emit(
            spark,
            this.at.x,
            this.at.y,
            this.at.z,
            this.spread((double)this.sparkSpeed),
            0.08 + this.random.nextDouble() * (double)this.sparkSpeed,
            this.spread((double)this.sparkSpeed)
         );
      }

      ParticleStyle ember = thunder ? VfxPresets.THUNDER_MOTE : VfxPresets.EMBER;

      for (int i = 0; i < VfxSettings.count((float)this.embers); i++) {
         VfxParticles.emit(ember, this.at.x, this.at.y, this.at.z, this.spread(0.35), this.random.nextDouble() * 0.3, this.spread(0.35)).life(1.2F);
      }

      for (int i = 0; i < VfxSettings.count((float)this.smoke); i++) {
         double h = this.random.nextDouble();
         VfxParticles.emit(
               VfxPresets.SMOKE,
               this.at.x + this.spread(0.6),
               this.at.y + h * 0.8 - 0.3,
               this.at.z + this.spread(0.6),
               this.spread(0.08),
               0.03 + h * 0.07,
               this.spread(0.08)
            )
            .tint(VfxWorld.shade(this.smokeRgb, light))
            .size(this.smokeSize * (0.8F + 0.5F * (float)h))
            .alpha(0.85F);
      }

      if (this.debris > 0 && floor && !firstPerson) {
         VfxManager.add(
            new FloorDebrisEffect(
               this.level, new Vec3(this.at.x, gy, this.at.z), this.debris, this.debrisSpeed, this.debrisSpeed * 2.4F, 70, this.random.nextLong()
            )
         );
      }

      if (this.decal > 0.0F && floor) {
         float rot = this.random.nextFloat() * (float) (Math.PI * 2);
         VfxParticles.emit(VfxPresets.SCORCH, this.at.x, gy + 0.03, this.at.z, 0.0, 0.0, 0.0).size(this.decal).rotation(rot).life(1.6F);
         VfxParticles.emit(thunder ? VfxPresets.CRACKS_VIOLET : VfxPresets.SCORCH_GLOW, this.at.x, gy + 0.035, this.at.z, 0.0, 0.0, 0.0)
            .size(this.decal)
            .rotation(rot)
            .life(1.4F);
      }

      float ex = VfxWorld.exposure(this.caster, this.at, 8.0, 40.0);
      if (ex > 0.0F) {
         float soften = firstPerson ? 0.35F : 1.0F;
         if (this.trauma > 0.0F) {
            CameraDirector.shake(this.trauma * ex, this.traumaDecay, this.traumaDegrees, 3.2F);
         }

         if (this.fov != 0.0F) {
            CameraDirector.fovPunch(this.fov * ex);
         }

         if (this.lens > 0.0F) {
            VfxPost.chromatic(this.lens * ex * soften);
            VfxPost.radialBlur(this.at, this.lens * 0.12F * ex * soften);
         }

         if (this.screenFlash > 0.0F) {
            VfxPost.flash(this.glowRgb, this.screenFlash * ex * soften);
         }

         if (this.hitStop > 0 && ex > 0.5F) {
            VfxClock.hitStop(this.hitStop);
         }
      }
   }

   private double spread(double amount) {
      return (this.random.nextDouble() * 2.0 - 1.0) * amount;
   }
}
