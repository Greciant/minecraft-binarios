package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.model.geo.GeoAnimation;
import com.devilfruits.client.model.geo.GeoResources;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.VfxSound;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.ArcBurst;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.Impact;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.Ribbon;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuSounds;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.HybridAnchors;
import com.devilfruits.registry.DFSounds;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class AscensionCinematic extends HybridCinematic {
   private static final int[] FLARING = new int[]{0, 1, 2};
   private static final int WARM_GRADE = 16769724;
   private static final int AFTER = 50;
   private final int launchAt;
   private int flightLeft = -1;
   private int landed = -1;
   private Vec3 pillar = Vec3.ZERO;
   private float facingX;
   private float facingZ = 1.0F;
   @Nullable
   private final Ribbon trail;
   @Nullable
   private final Ribbon tailTrail;
   @Nullable
   private VfxSound swell;

   public AscensionCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      GeoAnimation clip = GeoResources.animation(UoUoSeiryu.anim("hybrid/draconic_ascension"));
      this.launchAt = Math.max(1, Math.round(clip != null ? clip.marker("launch", (float)params.extra()) : (float)params.extra()));
      this.trail = Ribbon.create(36);
      if (this.trail != null) {
         this.trail.look(0.6F * this.height / 3.6F + 0.1F, 16.0F, 1.35F, 0.3F, 1.7F, 0).texture(MeshTex.FLAME_RIBBON, 0.2F);
      }

      this.tailTrail = Ribbon.create(24);
      if (this.tailTrail != null) {
         this.tailTrail.look(0.3F, 10.0F, 1.3F, 0.4F, 2.0F, 0).texture(MeshTex.FLAME_RIBBON, 0.26F);
      }

      Entity e = anchor.entity(level);
      if (e != null && params.extra() > 0) {
         this.swell = VfxSound.play(
            SeiryuSounds.ASCENSION_CHARGE.get(),
            e,
            e.position(),
            false,
            a -> 0.3F + 0.7F * Math.min(1.0F, (float)a / (float)this.launchAt),
            a -> 0.8F + 0.3F * Math.min(1.0F, (float)a / (float)this.launchAt),
            this.launchAt + 4,
            () -> !this.isDone() && this.flightLeft < 0 && this.landed < 0
         );
      } else {
         this.flightLeft = 0;
      }
   }

   public void launch(ClientLevel level, Vec3 from, Vec3 to, int flight) {
      if (this.swell != null) {
         this.swell.end();
      }

      this.flightLeft = Math.max(1, flight) + 2;
      Vec3 d = to.subtract(from);
      double dl = Math.sqrt(d.x * d.x + d.z * d.z);
      if (dl > 0.001) {
         this.facingX = (float)(d.x / dl);
         this.facingZ = (float)(d.z / dl);
      }

      double gy = VfxWorld.ground(level, from.x, from.y + 0.5, from.z, 4);
      if (!Double.isNaN(gy)) {
         GroundWave.at(from.x, gy, from.z, 3.0F, 10)
            .strength(1.1F)
            .ring(16777215, 0.7F)
            .dust(14, this.shade(VfxWorld.groundColour(level, from.x, gy - 0.5, from.z)), 0.7F, 0.32F, 0.7F, this.seed ^ 165L)
            .spawn();

         for (int i = 0; i < VfxSettings.count(14.0F); i++) {
            double a = (double)((float) (Math.PI * 2) * (float)i / 14.0F) + this.random.nextDouble() * 0.3;
            VfxParticles.emit(
                  VfxPresets.FIRE_PUFF, from.x + Math.cos(a) * 0.6, gy + 0.3, from.z + Math.sin(a) * 0.6, Math.cos(a) * 0.3, 0.04, Math.sin(a) * 0.3
               )
               .size(0.9F);
         }
      }

      float ex = this.exposure();
      if (ex > 0.0F) {
         float lens = this.firstPersonCaster() ? 0.4F : 1.0F;
         CameraDirector.snapDolly();
         CameraDirector.fovPunch(0.08F * ex);
         CameraDirector.shake(0.3F * ex, 0.1F, 1.8F, 3.0F);
         VfxPost.radialBlur(to.add(0.0, 1.0, 0.0), 0.14F * ex * lens);
      }

      SoundDesign.play(
         from,
         SeiryuSounds.ASCENSION_LAUNCH,
         1.3F,
         1.0F,
         SoundDesign.layer(DFSounds.LAYER_WIND, 0.9F, 1.1F),
         SoundDesign.layer(SeiryuSounds.LAYER_FLAME, 0.7F, 1.2F)
      );
   }

   public void impact(ClientLevel level, Vec3 at, float radius) {
      if (this.swell != null) {
         this.swell.end();
      }

      this.flightLeft = -1;
      this.landed = 0;
      this.pillar = at;
      Entity e = this.caster(level);
      Vec3 mid = at.add(0.0, 1.0, 0.0);
      Impact.at(level, mid, this.seed ^ 26L)
         .caster(e)
         .flash(1.0F)
         .glow(16774338)
         .sparks(20, 0.55F)
         .embers(14)
         .debris(14, 0.22F)
         .decal(radius * 2.1F)
         .trauma(0.72F, 0.1F, 4.0F)
         .hitStop(2)
         .fov(0.1F)
         .lens(0.55F)
         .play();

      for (int i = 0; i < VfxSettings.count(22.0F); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double sp = 0.8 + this.random.nextDouble() * 0.6;
         VfxParticles.emit(VfxPresets.SPARK, mid.x, mid.y, mid.z, Math.cos(a) * sp, 0.05 + this.random.nextDouble() * 0.2, Math.sin(a) * sp)
            .heat(1.5F)
            .size(2.0F)
            .life(0.5F);
      }

      ArcBurst.at(at.add(0.0, 0.12, 0.0), this.seed ^ 81L)
         .arcs(5)
         .radius(0.7F, radius * 2.6F)
         .life(16)
         .spin(0.6F)
         .arc((float) (Math.PI * 8.0 / 5.0))
         .tilt(0.0F, 0.1F)
         .colour(MeshTex.FLAME_RIBBON, 16777215, 0.55F)
         .alpha(1.0F)
         .points(48)
         .spawn();
      ArcBurst.at(at.add(0.0, 0.7, 0.0), this.seed ^ 82L)
         .arcs(2)
         .radius(0.6F, radius * 1.4F)
         .life(12)
         .spin(-0.75F)
         .arc((float) Math.PI * 11.0F / 10.0F)
         .tilt(0.18F, 0.35F)
         .colour(MeshTex.FLAME_RIBBON, 16777215, 0.3F)
         .alpha(0.75F)
         .spawn();
      GroundWave.at(at.x, at.y, at.z, radius * 2.6F, 14)
         .strength(1.5F)
         .ring(16777215, 0.3F)
         .air(mid.y, radius * 1.4F)
         .dust(12, this.shade(15131370), 1.0F, 0.5F, 0.5F, this.seed ^ 83L)
         .spawn();
      SoundDesign.play(
         at,
         SeiryuSounds.ASCENSION_IMPACT,
         1.8F,
         0.9F,
         SoundDesign.layer(DFSounds.LAYER_BOOM, 1.0F, 0.9F),
         SoundDesign.layer(DFSounds.LAYER_IMPACT, 0.9F, 0.8F)
      );
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.caster(level);
      if (this.firstPersonCaster()) {
         VfxParticles.nearFade(3.5F);
      }

      float t = VfxClock.time();
      if (this.flightLeft < 0 && this.landed < 0) {
         this.tickCrouch(e);
      }

      if (this.flightLeft > 0 && e != null) {
         this.flightLeft--;
         HybridAnchors.get(e, 4, 1.0F, this.anchor3);
         if (this.trail != null) {
            this.trail.push(this.anchor3.x, this.anchor3.y, this.anchor3.z, t);
         }

         VfxParticles.emit(
               VfxPresets.FIRE_TONGUE,
               this.anchor3.x + this.spread(0.2),
               this.anchor3.y + this.spread(0.2),
               this.anchor3.z + this.spread(0.2),
               (double)(-this.facingX) * 0.1,
               0.02,
               (double)(-this.facingZ) * 0.1
            )
            .size(1.2F);
         VfxParticles.emit(VfxPresets.EMBER, this.anchor3.x, this.anchor3.y, this.anchor3.z, this.spread(0.08), this.spread(0.05), this.spread(0.08));
         HybridAnchors.get(e, 2, 1.0F, this.anchor3);
         if (this.tailTrail != null) {
            this.tailTrail.push(this.anchor3.x, this.anchor3.y, this.anchor3.z, t);
         }

         float ex = this.exposure();
         if (ex > 0.0F) {
            VfxPost.grade(16769724, 0.2F * ex);
         }
      }

      if (this.landed >= 0) {
         this.landed++;
         if (this.landed <= 12) {
            float grow = (float)this.landed / 12.0F;

            for (int i = 0; i < VfxSettings.count(3.0F); i++) {
               double h = this.random.nextDouble() * 6.5 * (double)grow;
               VfxParticles.emit(
                     VfxPresets.FIRE_TONGUE,
                     this.pillar.x + this.spread(0.12),
                     this.pillar.y + 0.4 + h,
                     this.pillar.z + this.spread(0.12),
                     this.spread(0.02),
                     0.32 + this.random.nextDouble() * 0.2,
                     this.spread(0.02)
                  )
                  .size(0.9F)
                  .heat(1.1F);
            }

            VfxParticles.emit(VfxPresets.FIRE_PUFF, this.pillar.x, this.pillar.y + 0.4, this.pillar.z, this.spread(0.02), 0.4, this.spread(0.02))
               .size(0.6F)
               .heat(1.2F);
         }

         if (this.landed < 30) {
            VfxParticles.emit(VfxPresets.GROUND_GLOW, this.pillar.x, this.pillar.y + 0.04, this.pillar.z, 0.0, 0.0, 0.0)
               .size(5.0F)
               .tint(16742938)
               .alpha(0.6F * (1.0F - (float)this.landed / 30.0F));
         }

         if (this.landed > 50) {
            this.finish();
         }
      }

      if (this.age > this.launchAt + 60 && this.landed < 0) {
         this.finish();
      }
   }

   private void tickCrouch(@Nullable Entity e) {
      float p = Math.min(1.0F, (float)this.age / (float)this.launchAt);
      if (e != null) {
         for (int id : FLARING) {
            HybridAnchors.get(e, id, 1.0F, this.anchor3);

            for (int i = 0; i < VfxSettings.count(1.0F + 2.0F * p); i++) {
               VfxParticles.emit(
                     VfxPresets.FIRE_TONGUE,
                     this.anchor3.x + this.spread(0.15),
                     this.anchor3.y + this.spread(0.15),
                     this.anchor3.z + this.spread(0.15),
                     this.spread(0.03),
                     0.04 + 0.05 * (double)p,
                     this.spread(0.03)
                  )
                  .size(0.7F + 0.6F * p);
            }

            VfxParticles.emit(VfxPresets.GLOW, this.anchor3.x, this.anchor3.y, this.anchor3.z, 0.0, 0.0, 0.0)
               .size(0.8F + 1.3F * p)
               .tint(16742938)
               .alpha(0.6F + 0.4F * p);
         }
      }

      for (int i = 0; i < VfxSettings.count(2.0F + 3.0F * p); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double r = (double)this.height * (0.3 + this.random.nextDouble() * 0.25);
         double px = this.x + Math.cos(a) * r;
         double pz = this.z + Math.sin(a) * r;
         double py = this.y + 0.2 + this.random.nextDouble() * (double)this.height * 0.6;
         VfxParticles.emit(VfxPresets.EMBER, px, py, pz, (this.x - px) * 0.08, 0.02, (this.z - pz) * 0.08).heat(1.1F);
      }

      if (this.onFloor(2.0)) {
         VfxParticles.emit(VfxPresets.GROUND_GLOW, this.x, this.groundY + 0.04, this.z, 0.0, 0.0, 0.0)
            .size(this.height * (0.9F + 0.6F * p))
            .tint(16742938)
            .alpha(0.3F + 0.35F * p);
      }

      if (this.local()) {
         CameraDirector.dolly(1.0F - 0.06F * p, 0.3F);
      }
   }

   @Override
   public void render(VfxFrame f) {
      if (!this.firstPersonCaster()) {
         Entity e = this.caster();
         if (e != null) {
            boolean flying = this.flightLeft > 0;
            float fade = flying ? 1.0F : (this.landed >= 0 ? Math.max(0.0F, 1.0F - ((float)this.landed + f.partial) / 14.0F) : 0.0F);
            if (!(fade <= 0.01F)) {
               if (this.trail != null) {
                  HybridAnchors.get(e, 4, f.entityPartial, this.anchor3);
                  this.trail.render(f, this.anchor3.x, this.anchor3.y, this.anchor3.z, fade);
               }

               if (this.tailTrail != null) {
                  HybridAnchors.get(e, 2, f.entityPartial, this.anchor3);
                  this.tailTrail.render(f, this.anchor3.x, this.anchor3.y, this.anchor3.z, fade);
               }
            }
         }
      }
   }

   @Override
   public void removed() {
      if (this.trail != null) {
         this.trail.release();
      }

      if (this.tailTrail != null) {
         this.tailTrail.release();
      }

      if (this.swell != null) {
         this.swell.end();
      }
   }
}
