package com.devilfruits.fruits.kilo_kilo.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.VfxSound;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.Ribbon;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.fruits.kilo_kilo.KiloParticles;
import com.devilfruits.fruits.kilo_kilo.KiloSounds;
import com.devilfruits.fruits.kilo_kilo.client.KiloClientState;
import com.devilfruits.fruits.kilo_kilo.weight.BodyWeight;
import com.devilfruits.fruits.kilo_kilo.weight.WeightCurves;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class WeightAuraCinematic extends Cinematic {
   private static final int AIR = 16054527;
   private static final int MOTE = 15660287;
   private static final int STREAK = 15133424;
   private final int playerId;
   private int quiet;
   private int cracks;
   @Nullable
   private Ribbon trailL;
   @Nullable
   private Ribbon trailR;
   private int trailAge = -100;
   @Nullable
   private VfxSound rush;
   private double x;
   private double y;
   private double z;
   private double prevY;

   public WeightAuraCinematic(VfxAnchor anchor, int playerId) {
      super(anchor, VfxParams.NONE);
      this.playerId = playerId;
      this.x = anchor.fallback().x;
      this.y = anchor.fallback().y;
      this.z = anchor.fallback().z;
      this.prevY = this.y;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (level.getEntity(this.playerId) instanceof Player p && p.isAlive()) {
         BodyWeight w = KiloClientState.weight(p);
         double kg = KiloClientState.kg(p, 1.0F);
         double s = WeightCurves.scale(kg);
         boolean ramping = w != null && w.ramping();
         if (w != null && (!(Math.abs(s) < 0.02) || ramping)) {
            this.quiet = 0;
            this.prevY = this.y;
            this.x = p.getX();
            this.y = p.getY();
            this.z = p.getZ();
            double vy = this.y - this.prevY;
            double light = Math.max(0.0, -s);
            double heavy = Math.max(0.0, s);
            float boost = ramping ? 2.0F : 1.0F;
            boolean fp = VfxWorld.firstPersonCaster(this.playerId);
            boolean air = !p.onGround();
            RandomSource r = level.random;
            double h = (double)p.getBbHeight();
            if (VfxSettings.legacy()) {
               this.legacy(level, p, light, heavy, boost, r);
               return;
            }

            if (light > 0.03) {
               int motes = VfxSettings.count((float)(1.4 * light * (double)boost));

               for (int i = 0; i < motes; i++) {
                  double a = r.nextDouble() * (float) (Math.PI * 2);
                  double rad = 0.3 + r.nextDouble() * 0.35;
                  VfxParticles.emit(
                        VfxPresets.GLASS_DUST,
                        this.x + Math.cos(a) * rad,
                        this.y + r.nextDouble() * h,
                        this.z + Math.sin(a) * rad,
                        jitter(r, 0.01),
                        0.025 + r.nextDouble() * 0.03,
                        jitter(r, 0.01)
                     )
                     .tint(15660287)
                     .alpha((float)(0.5 + 0.4 * light))
                     .life(1.6F);
               }

               if ((double)r.nextFloat() < 0.05F * light * (double)boost && !fp) {
                  level.addParticle(
                     (ParticleOptions)KiloParticles.FEATHER.get(),
                     this.x + jitter(r, 0.4),
                     this.y + h * (0.3 + r.nextDouble() * 0.6),
                     this.z + jitter(r, 0.4),
                     jitter(r, 0.03),
                     0.01 + r.nextDouble() * 0.02,
                     jitter(r, 0.03)
                  );
               }

               double gy = VfxWorld.ground(level, this.x, this.y + 0.2, this.z, 4);
               if (!Double.isNaN(gy) && this.age % 16 == 0 && VfxSettings.distortion()) {
                  ShockRing.at(new Vec3(this.x, gy + 0.06, this.z), new Vec3(0.0, 1.0, 0.0))
                     .radius(0.3F, (float)(1.2 + 1.2 * light))
                     .life(18)
                     .strength((float)(0.35 * light))
                     .band(0.5F)
                     .rim(0.0F, 16054527)
                     .twist(0.6F)
                     .spawn();
               }

               if (air && vy < 0.0 && vy > -0.12 && light > 0.4 && this.age % 2 == 0) {
                  for (int i = 0; i < 2; i++) {
                     double a = r.nextDouble() * (float) (Math.PI * 2);
                     VfxParticles.emit(
                           VfxPresets.SPEED_LINE,
                           this.x + Math.cos(a) * 0.9,
                           this.y + r.nextDouble() * h,
                           this.z + Math.sin(a) * 0.9,
                           -Math.sin(a) * 0.15,
                           0.05,
                           Math.cos(a) * 0.15
                        )
                        .tint(16777215)
                        .alpha(0.45F)
                        .life(2.5F)
                        .orbit(this.x, this.z, 0.35F);
                  }
               }

               if (air && vy > 0.12 && light > 0.2) {
                  if (this.trailL == null && this.age - this.trailAge > 2) {
                     this.trailL = Ribbon.create(24);
                     this.trailR = Ribbon.create(24);

                     for (Ribbon rb : new Ribbon[]{this.trailL, this.trailR}) {
                        if (rb != null) {
                           rb.look(0.09F, 14.0F, 1.0F, 0.4F, 0.5F, 2).texture(MeshTex.LANCE, 0.05F).tint(15398143);
                        }
                     }
                  }

                  this.trailAge = this.age;
                  float yaw = p.yBodyRot * (float) (Math.PI / 180.0);
                  double sx = Math.cos((double)yaw) * 0.16;
                  double sz = Math.sin((double)yaw) * 0.16;
                  if (this.trailL != null) {
                     this.trailL.push(this.x + sx, this.y + 0.1, this.z + sz, (float)this.age);
                  }

                  if (this.trailR != null) {
                     this.trailR.push(this.x - sx, this.y + 0.1, this.z - sz, (float)this.age);
                  }
               }
            }

            if (heavy > 0.03) {
               double gyx = VfxWorld.ground(level, this.x, this.y + 0.3, this.z, 14);
               boolean floor = !Double.isNaN(gyx);
               int earth = floor ? KiloLook.earth(level, this.x, gyx, this.z) : 9076851;
               if (!air && floor) {
                  if (--this.cracks <= 0 && heavy > 0.2) {
                     this.cracks = 10;
                     GroundCracks.crack(level, BlockPos.containing(this.x, gyx - 0.5, this.z), (float)(1.0 + 7.0 * heavy), 40);
                     if (heavy > 0.6) {
                        GroundCracks.patch(level, this.x, gyx, this.z, 0.6 + 1.2 * heavy, (float)(5.0 * heavy), 40, r.nextLong());
                     }
                  }

                  for (int i = 0; i < VfxSettings.count((float)(0.7 * heavy * (double)boost)); i++) {
                     double a = r.nextDouble() * (float) (Math.PI * 2);
                     VfxParticles.emit(
                           VfxPresets.DUST, this.x + Math.cos(a) * 0.35, gyx + 0.08, this.z + Math.sin(a) * 0.35, Math.cos(a) * 0.05, 0.004, Math.sin(a) * 0.05
                        )
                        .tint(earth)
                        .size((float)(0.22 + 0.2 * heavy))
                        .alpha(0.75F)
                        .life(0.5F);
                  }
               }

               if (this.age % (ramping ? 8 : 20) == 0 && VfxSettings.distortion()) {
                  ShockRing.at(new Vec3(this.x, this.y + h * 0.9, this.z), new Vec3(0.0, 1.0, 0.0))
                     .radius(0.75F, (float)(0.55 - 0.15 * heavy))
                     .life(10)
                     .move(new Vec3(0.0, -h * 0.085, 0.0))
                     .strength((float)(0.25 + 0.45 * heavy))
                     .band(0.5F)
                     .rim(0.0F, 16054527)
                     .spawn();
               }

               if (air && vy < -0.6) {
                  float speed = (float)Mth.clamp(-vy / 3.0, 0.0, 1.0);

                  for (int i = 0; i < VfxSettings.count(3.0F * speed); i++) {
                     double a = r.nextDouble() * (float) (Math.PI * 2);
                     double rad = 0.5 + r.nextDouble() * 0.9;
                     VfxParticles.emit(
                           VfxPresets.SPEED_LINE,
                           this.x + Math.cos(a) * rad,
                           this.y + h * r.nextDouble(),
                           this.z + Math.sin(a) * rad,
                           0.0,
                           1.2 + 0.6 * (double)speed,
                           0.0
                        )
                        .tint(15133424)
                        .alpha(0.45F + 0.4F * speed)
                        .size(1.4F);
                  }

                  if (this.age % 3 == 0 && VfxSettings.distortion()) {
                     ShockRing.at(new Vec3(this.x, this.y + 0.1, this.z), new Vec3(0.0, 1.0, 0.0))
                        .radius(0.5F, 1.6F)
                        .life(8)
                        .move(new Vec3(0.0, 0.25, 0.0))
                        .strength(0.5F * speed)
                        .band(0.45F)
                        .rim(0.0F, 15133424)
                        .spawn();
                  }

                  if (floor && this.y - gyx < 10.0) {
                     float near = (float)Mth.clamp(1.0 - (this.y - gyx) / 10.0, 0.0, 1.0);

                     for (int i = 0; i < VfxSettings.count(3.0F * near * speed); i++) {
                        double a = r.nextDouble() * (float) (Math.PI * 2);
                        double d = 0.4 + r.nextDouble() * 1.2;
                        VfxParticles.emit(
                              VfxPresets.DUST,
                              this.x + Math.cos(a) * d,
                              gyx + 0.15,
                              this.z + Math.sin(a) * d,
                              Math.cos(a) * 0.2 * (double)near,
                              0.01,
                              Math.sin(a) * 0.2 * (double)near
                           )
                           .tint(earth)
                           .size(0.6F + near)
                           .alpha(0.7F);
                     }
                  }

                  if (this.rush == null || this.rush.isStopped()) {
                     this.rush = VfxSound.play(
                        KiloSounds.FALL_RUSH.get(),
                        p,
                        p.position(),
                        true,
                        ax -> Math.min(0.9F, 0.2F + (float)ax * 0.08F),
                        ax -> 0.8F,
                        400,
                        () -> !this.isDone() && this.fallingFast(level)
                     );
                  }
               }
            }

            return;
         }

         if (++this.quiet > 30) {
            this.finish();
         }

         return;
      }

      this.finish();
   }

   private boolean fallingFast(ClientLevel level) {
      Entity e = level.getEntity(this.playerId);
      return e != null && !e.onGround() && e.getY() - e.yo < -0.4;
   }

   private void legacy(ClientLevel level, Player p, double light, double heavy, float boost, RandomSource r) {
      if (light > 0.05 && (double)r.nextFloat() < 0.4F * light * (double)boost) {
         level.addParticle(
            (ParticleOptions)KiloParticles.MOTE.get(),
            this.x + jitter(r, 0.4),
            this.y + r.nextDouble() * (double)p.getBbHeight(),
            this.z + jitter(r, 0.4),
            0.0,
            0.03,
            0.0
         );
      }

      if (light > 0.05 && (double)r.nextFloat() < 0.04F * light * (double)boost) {
         level.addParticle((ParticleOptions)KiloParticles.FEATHER.get(), this.x + jitter(r, 0.4), this.y + 1.2, this.z + jitter(r, 0.4), 0.0, 0.01, 0.0);
      }

      if (heavy > 0.2 && p.onGround() && --this.cracks <= 0) {
         this.cracks = 10;
         GroundCracks.crack(level, p.blockPosition().below(), (float)(1.0 + 7.0 * heavy), 40);
      }
   }

   @Override
   public void render(VfxFrame f) {
      float fade = Mth.clamp(1.0F - (float)(this.age - this.trailAge) / 14.0F, 0.0F, 1.0F);
      if (this.trailL != null) {
         this.trailL.render(f, this.x, this.y + 0.1, this.z, fade);
      }

      if (this.trailR != null) {
         this.trailR.render(f, this.x, this.y + 0.1, this.z, fade);
      }

      if (fade <= 0.0F && this.age - this.trailAge > 16) {
         this.releaseTrails();
      }
   }

   private void releaseTrails() {
      if (this.trailL != null) {
         this.trailL.release();
         this.trailL = null;
      }

      if (this.trailR != null) {
         this.trailR.release();
         this.trailR = null;
      }
   }

   @Override
   public void removed() {
      this.releaseTrails();
      if (this.rush != null) {
         this.rush.end();
      }
   }

   private static double jitter(RandomSource r, double amount) {
      return (r.nextDouble() * 2.0 - 1.0) * amount;
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
