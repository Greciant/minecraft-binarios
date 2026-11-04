package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.model.geo.GeoAnimation;
import com.devilfruits.client.model.geo.GeoResources;
import com.devilfruits.client.render.vfx.FloorDebrisEffect;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.BoltStrike;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.Impact;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.Ribbon;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuSounds;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.HybridAnchors;
import com.devilfruits.registry.DFSounds;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class CataclysmCinematic extends HybridCinematic {
   private static final int[] FLARING = new int[]{0, 1, 2};
   private static final int WARM_GRADE = 16768180;
   private static final int AFTER = 70;
   private final boolean veiled;
   private final int leapAt;
   private final int slamAt;
   private final float radius;
   private int landed = -1;
   private double landX;
   private double landY;
   private double landZ;
   private double markX;
   private double markY = Double.NaN;
   private double markZ;
   private final Ribbon[] trails = new Ribbon[3];

   public CataclysmCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.veiled = params.power() > 0.0F;
      GeoAnimation clip = GeoResources.animation(UoUoSeiryu.anim("hybrid/draconic_cataclysm"));
      this.leapAt = Math.round(clip != null ? clip.marker("leap", (float)params.extra()) : (float)params.extra());
      this.slamAt = Math.round(clip != null ? clip.marker("slam", 29.0F) : 29.0F);
      this.radius = 8.0F;

      for (int i = 0; i < 3; i++) {
         this.trails[i] = Ribbon.create(20);
         if (this.trails[i] != null) {
            this.trails[i].look(i == 2 ? 0.42F : 0.36F, 9.0F, 1.3F, 0.45F, 1.8F, 0).texture(MeshTex.FLAME_RIBBON, 0.22F);
         }
      }
   }

   public void impact(ClientLevel level, Vec3 at, float r) {
      this.landed = 0;
      this.landX = at.x;
      this.landY = at.y;
      this.landZ = at.z;
      Entity e = this.caster(level);
      Vec3 mid = at.add(0.0, 1.2, 0.0);
      Impact.at(level, mid, this.seed ^ 193L)
         .caster(e)
         .flash(1.7F)
         .glow(16742938)
         .sparks(30, 0.75F)
         .embers(24)
         .smoke(12, 2760742, 1.6F)
         .debris(18, 0.3F)
         .trauma(1.0F, 0.035F, 5.0F)
         .hitStop(3)
         .fov(0.16F)
         .lens(1.0F)
         .screenFlash(0.25F)
         .play();

      for (int i = 0; i < VfxSettings.count(4.0F) + 2; i++) {
         VfxParticles.emit(
               VfxPresets.FIREBALL,
               mid.x + this.spread(1.4),
               mid.y + this.spread(0.8),
               mid.z + this.spread(1.4),
               this.spread(0.12),
               0.05 + this.random.nextDouble() * 0.08,
               this.spread(0.12)
            )
            .size(1.3F + this.random.nextFloat() * 0.6F);
      }

      for (int i = 0; i < VfxSettings.count(36.0F); i++) {
         double a = (double)((float) (Math.PI * 2) * (float)i / 36.0F) + this.random.nextDouble() * 0.2;
         VfxParticles.emit(VfxPresets.FIRE_PUFF, at.x + Math.cos(a) * 0.8, at.y + 0.4, at.z + Math.sin(a) * 0.8, Math.cos(a) * 0.55, 0.03, Math.sin(a) * 0.55)
            .size(1.5F)
            .life(1.3F);
      }

      int bolts = this.veiled ? 8 : 5;

      for (int i = 0; i < bolts; i++) {
         double a = (double)((float)i * (float) (Math.PI * 2) / (float)bolts) + this.random.nextDouble() * 0.5;
         double reach = (double)r * (0.6 + this.random.nextDouble() * 0.45);
         BoltStrike.between(at.add(0.0, 0.4, 0.0), at.add(Math.cos(a) * reach, 0.25, Math.sin(a) * reach), this.seed + (long)i * 17L)
            .width(this.veiled ? 0.14F : 0.1F)
            .life(7)
            .forks(3)
            .rough(0.16F)
            .spawn();
      }

      if (this.veiled) {
         BoltStrike.between(at.add(this.spread(2.0), 26.0, this.spread(2.0)), at, this.seed ^ 24301L)
            .width(0.55F)
            .life(10)
            .forks(5)
            .heat(1.2F)
            .endGlow(2.0F)
            .spawn();
         Impact.at(level, mid, this.seed ^ 194L).thunder().flash(1.1F).sparks(30, 0.7F).play();
      }

      GroundWave.at(at.x, at.y, at.z, r * 2.2F, 18)
         .strength(1.4F)
         .air(mid.y, r * 1.0F)
         .dust(16, this.shade(VfxWorld.groundColour(level, at.x, at.y - 0.5, at.z)), 1.2F, 0.3F, 0.65F, this.seed ^ 195L)
         .spawn();
      float rot = this.random.nextFloat() * (float) (Math.PI * 2);
      VfxParticles.emit(VfxPresets.SCORCH, at.x, at.y + 0.03, at.z, 0.0, 0.0, 0.0).size(r * 2.2F).rotation(rot).life(2.6F);
      VfxParticles.emit(VfxPresets.SCORCH_RING, at.x, at.y + 0.032, at.z, 0.0, 0.0, 0.0).size(r * 2.4F).tint(657414).alpha(0.9F).life(1.6F);
      VfxParticles.emit(this.veiled ? VfxPresets.CRACKS_VIOLET : VfxPresets.SCORCH_GLOW, at.x, at.y + 0.036, at.z, 0.0, 0.0, 0.0)
         .size(r * 2.2F)
         .rotation(rot)
         .life(1.8F);
      if (!this.firstPersonCaster()) {
         VfxManager.add(new FloorDebrisEffect(level, at, 12, 0.12F, 0.95F, 90, this.seed ^ 196L));
      }

      for (int i = 0; i < VfxSettings.count(16.0F); i++) {
         VfxParticles.emit(
               VfxPresets.DUST,
               at.x + this.spread(1.2),
               at.y + 0.5,
               at.z + this.spread(1.2),
               this.spread(0.06),
               0.35 + this.random.nextDouble() * 0.35,
               this.spread(0.06)
            )
            .tint(this.shade(VfxWorld.groundColour(level, at.x, at.y - 0.5, at.z)))
            .size(1.4F)
            .life(1.4F);
      }

      SoundDesign.play(
         at,
         SeiryuSounds.RAGNARAKU_SLAM,
         2.2F,
         0.8F,
         SoundDesign.layer(DFSounds.LAYER_BOOM, 1.2F, 0.7F),
         SoundDesign.layer(this.veiled ? DFSounds.LAYER_THUNDER : SeiryuSounds.LAYER_BLAST, 1.2F, 0.9F),
         SoundDesign.layer(SeiryuSounds.LAYER_GROWL, 0.7F, 0.8F)
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
      if (this.landed < 0) {
         if (this.age < this.leapAt) {
            this.tickCrouch(e);
         } else if (this.age == this.leapAt) {
            this.leap(level);
         }

         if (this.age > this.slamAt && e != null) {
            for (int k = 0; k < VfxSettings.count(4.0F); k++) {
               double f = this.random.nextDouble();
               double px = Mth.lerp(f, this.ox, this.x);
               double py = Mth.lerp(f, this.oy, this.y) + (double)this.height * 0.5;
               double pz = Mth.lerp(f, this.oz, this.z);
               VfxParticles.emit(
                     VfxPresets.FIRE_TONGUE, px + this.spread(0.3), py + this.spread(0.3), pz + this.spread(0.3), this.spread(0.03), 0.06, this.spread(0.03)
                  )
                  .size(1.6F);
            }

            VfxParticles.emit(VfxPresets.FIRE_PUFF, this.x, this.y + (double)this.height * 0.4, this.z, 0.0, 0.05, 0.0).size(1.2F).heat(1.2F);
         }

         if (this.age >= this.leapAt && this.age <= this.slamAt && e != null) {
            for (int i = 0; i < 3; i++) {
               if (this.trails[i] != null) {
                  HybridAnchors.get(e, FLARING[i], 1.0F, this.anchor3);
                  this.trails[i].push(this.anchor3.x, this.anchor3.y, this.anchor3.z, t);
                  if (this.random.nextFloat() < 0.6F * VfxSettings.spawnScale()) {
                     VfxParticles.emit(VfxPresets.FIRE_TONGUE, this.anchor3.x, this.anchor3.y, this.anchor3.z, this.spread(0.03), 0.02, this.spread(0.03))
                        .size(0.9F);
                  }
               }
            }
         }

         if (this.age > this.leapAt + 2) {
            this.tickMark(level);
         }

         if (this.age > this.slamAt + 60) {
            this.finish();
         }
      } else {
         this.landed++;
         float p = Math.min(1.0F, (float)this.landed / 70.0F);
         if (this.landed < 40) {
            for (int ix = 0; ix < VfxSettings.count(1.5F * (1.0F - p)); ix++) {
               VfxParticles.emit(
                     VfxPresets.SMOKE,
                     this.landX + this.spread(1.5),
                     this.landY + 0.6 + this.random.nextDouble() * 2.0,
                     this.landZ + this.spread(1.5),
                     this.spread(0.03),
                     0.08,
                     this.spread(0.03)
                  )
                  .tint(this.shade(3023904))
                  .size(1.2F)
                  .life(1.1F);
            }

            for (int ix = 0; ix < VfxSettings.count(2.5F * (1.0F - p)); ix++) {
               VfxParticles.emit(
                  VfxPresets.EMBER,
                  this.landX + this.spread((double)this.radius * 0.6),
                  this.landY + 0.3,
                  this.landZ + this.spread((double)this.radius * 0.6),
                  this.spread(0.03),
                  0.05 + this.random.nextDouble() * 0.05,
                  this.spread(0.03)
               );
            }
         }

         VfxParticles.emit(VfxPresets.GROUND_GLOW, this.landX, this.landY + 0.04, this.landZ, 0.0, 0.0, 0.0)
            .size(this.radius * 1.6F)
            .tint(this.veiled ? 8011007 : 16742938)
            .alpha(0.55F * (1.0F - p));
         float ex = this.exposure();
         if (ex > 0.0F) {
            VfxPost.grade(16768180, 0.3F * (1.0F - p) * ex);
         }

         if (this.landed > 70) {
            this.finish();
         }
      }
   }

   private void tickCrouch(@Nullable Entity e) {
      float p = Math.min(1.0F, (float)this.age / (float)Math.max(1, this.leapAt));
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
               .size(0.8F + 1.2F * p)
               .tint(16742938)
               .alpha(0.5F + 0.4F * p);
            if (this.veiled) {
               VfxParticles.emit(VfxPresets.THUNDER_SPARK, this.anchor3.x, this.anchor3.y, this.anchor3.z, this.spread(0.2), 0.1, this.spread(0.2));
            }
         }

         if (this.local()) {
            CameraDirector.dolly(1.0F - 0.06F * p, 0.3F);
         }
      }
   }

   private void leap(ClientLevel level) {
      if (this.onFloor(2.0)) {
         GroundWave.at(this.x, this.groundY, this.z, 3.4F, 10)
            .strength(1.2F)
            .ring(16777215, 0.6F)
            .dust(16, this.shade(VfxWorld.groundColour(level, this.x, this.groundY - 0.5, this.z)), 0.8F, 0.36F, 0.7F, this.seed ^ 197L)
            .spawn();

         for (int i = 0; i < VfxSettings.count(16.0F); i++) {
            double a = (double)((float) (Math.PI * 2) * (float)i / 16.0F);
            VfxParticles.emit(
                  VfxPresets.FIRE_PUFF,
                  this.x + Math.cos(a) * 0.7,
                  this.groundY + 0.3,
                  this.z + Math.sin(a) * 0.7,
                  Math.cos(a) * 0.32,
                  0.05,
                  Math.sin(a) * 0.32
               )
               .size(1.0F);
         }
      }

      float ex = this.exposure();
      if (ex > 0.0F) {
         CameraDirector.snapDolly();
         CameraDirector.shake(0.35F * ex, 0.09F, 2.0F, 3.0F);
         CameraDirector.fovPunch(0.06F * ex);
      }
   }

   private void tickMark(ClientLevel level) {
      double gy = VfxWorld.ground(level, this.x, this.y, this.z, 24);
      if (!Double.isNaN(gy)) {
         this.markX = this.x;
         this.markY = gy;
         this.markZ = this.z;
         float p = Mth.clamp((float)(this.age - this.leapAt) / (float)Math.max(1, this.slamAt - this.leapAt), 0.0F, 1.0F);
         if (this.age % 4 == 0) {
            float rot = this.random.nextFloat() * (float) (Math.PI * 2);
            VfxParticles.emit(this.veiled ? VfxPresets.CRACKS_VIOLET : VfxPresets.SCORCH_GLOW, this.markX, gy + 0.03, this.markZ, 0.0, 0.0, 0.0)
               .size(this.radius * (0.5F + 1.3F * p))
               .rotation(rot)
               .life(0.25F)
               .heat(0.8F + 0.4F * p);
         }

         VfxParticles.emit(VfxPresets.GROUND_GLOW, this.markX, gy + 0.04, this.markZ, 0.0, 0.0, 0.0)
            .size(this.radius * (0.8F + 1.4F * p))
            .tint(this.veiled ? 8011007 : 16742938)
            .alpha(0.25F + 0.4F * p);
         if (this.random.nextFloat() < 0.5F) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double d = (double)(this.radius * p);
            VfxParticles.emit(
               this.veiled ? VfxPresets.THUNDER_MOTE : VfxPresets.EMBER, this.markX + Math.cos(a) * d, gy + 0.1, this.markZ + Math.sin(a) * d, 0.0, 0.08, 0.0
            );
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      if (this.landed < 0 && this.age > this.leapAt + 2 && !Double.isNaN(this.markY) && f.allowMesh()) {
         float p = Mth.clamp(((float)this.age + f.partial - (float)this.leapAt) / (float)Math.max(1, this.slamAt - this.leapAt), 0.0F, 1.0F);
         float r = this.radius * (0.25F + 0.75F * (1.0F - (1.0F - p) * (1.0F - p)));
         float pulse = 0.85F + 0.15F * Mth.sin(f.time * 1.3F);
         MeshWriter.ring(
            f.mesh(this.veiled ? MeshTex.CRACKLE : MeshTex.FLAME_RIBBON),
            f.rx(this.markX),
            f.ry(this.markY + 0.06),
            f.rz(this.markZ),
            1.0F,
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            1.0F,
            r,
            0.55F,
            48,
            -f.time * 0.03F,
            Math.max(2.0F, r * 1.2F),
            MeshWriter.argb(16777215, (0.5F + 0.5F * p) * pulse),
            1.1F + 0.2F * p,
            this.veiled ? 1 : 0,
            2.0F,
            0.0F
         );
      }

      if (!this.firstPersonCaster()) {
         Entity e = this.caster();
         if (e != null && this.age >= this.leapAt) {
            float fade = Math.max(0.0F, 1.0F - Math.max(0.0F, (float)this.age + f.partial - (float)this.slamAt) / 4.0F);

            for (int i = 0; i < 3; i++) {
               if (this.trails[i] != null && fade > 0.01F) {
                  HybridAnchors.get(e, FLARING[i], f.entityPartial, this.anchor3);
                  this.trails[i].render(f, this.anchor3.x, this.anchor3.y, this.anchor3.z, fade);
               }
            }
         }
      }
   }

   @Override
   public void removed() {
      for (Ribbon r : this.trails) {
         if (r != null) {
            r.release();
         }
      }
   }

   @Override
   public double cullDistance() {
      return 128.0;
   }
}
