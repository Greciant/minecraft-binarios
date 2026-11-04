package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.render.vfx.FloorDebrisEffect;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.ArcBurst;
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
import com.devilfruits.fruits.uo_uo_seiryu.client.render.HybridAnchors;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.HybridGhosts;
import com.devilfruits.registry.DFSounds;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class RushCinematic extends HybridCinematic {
   public static final int FIRE_WHITE = 16774338;
   public static final int FIRE_ORANGE = 16742938;
   public static final int FIRE_RED = 15217182;
   private static final int DARK_GRADE = 8023198;
   private static final int LINGER = 50;
   private final boolean veiled;
   private float facingX;
   private float facingZ;
   private int dashLeft = -1;
   private int dashTicks;
   private int lastEvent;
   private boolean anticipating = true;
   private int sphere = -1;
   private double sx;
   private double sy;
   private double sz;
   private float sphereRadius;
   private int darken = -1;
   @Nullable
   private Ribbon streakA;
   @Nullable
   private Ribbon streakB;
   private final double[] headA = new double[3];
   private final double[] headB = new double[3];
   private final double[] liveA = new double[3];
   private final double[] liveB = new double[3];

   public RushCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.veiled = params.power() > 0.0F;
      Vec3 d = params.dir();
      double dl = Math.sqrt(d.x * d.x + d.z * d.z);
      this.facingX = dl > 1.0E-4 ? (float)(d.x / dl) : 0.0F;
      this.facingZ = dl > 1.0E-4 ? (float)(d.z / dl) : 1.0F;
      this.streakA = Ribbon.create(20);
      if (this.streakA != null) {
         this.streakA.look(0.5F, 9.0F, 1.25F, 0.45F, 1.6F, 0).texture(MeshTex.FLAME_RIBBON, 0.22F);
      }

      this.streakB = Ribbon.create(20);
      if (this.streakB != null) {
         this.streakB.look(0.36F, 8.0F, 1.25F, 0.5F, 2.2F, this.veiled ? 1 : 0).texture(MeshTex.FLAME_RIBBON, 0.26F);
      }

      if (params.extra() <= 0) {
         this.anticipating = false;
      }
   }

   public boolean veiled() {
      return this.veiled;
   }

   public void dash(ClientLevel level, Vec3 from, Vec3 to, int ticks) {
      this.anticipating = false;
      this.dashTicks = Math.max(1, ticks);
      this.dashLeft = this.dashTicks + 1;
      this.lastEvent = this.age;
      Vec3 d = to.subtract(from);
      double dl = Math.sqrt(d.x * d.x + d.z * d.z);
      if (dl > 0.001) {
         this.facingX = (float)(d.x / dl);
         this.facingZ = (float)(d.z / dl);
      }

      Entity e = this.caster(level);
      if (e != null) {
         HybridGhosts.start(e, ticks + 2, this.veiled ? -1716097793 : -1711302086);
      }

      double gy = VfxWorld.ground(level, from.x, from.y + 0.5, from.z, 4);
      if (!Double.isNaN(gy)) {
         GroundWave.at(from.x, gy, from.z, 2.4F, 9)
            .strength(1.0F)
            .ring(16777215, 0.6F)
            .dust(12, this.shade(VfxWorld.groundColour(level, from.x, gy - 0.5, from.z)), 0.6F, 0.3F, 0.55F, this.seed ^ 213L)
            .spawn();
      }

      float ex = this.exposure();
      if (ex > 0.0F) {
         CameraDirector.snapDolly();
         CameraDirector.fovPunch(0.06F * ex);
         VfxPost.radialBlur(to.add(0.0, (double)this.height * 0.5, 0.0), 0.16F * ex * (this.firstPersonCaster() ? 0.5F : 1.0F));
         CameraDirector.shake(0.2F * ex, 0.1F, 1.2F, 3.0F);
      }

      SoundDesign.play(
         from,
         SeiryuSounds.RUSH_DASH,
         1.2F,
         1.0F,
         SoundDesign.layer(DFSounds.LAYER_WIND, 0.8F, 1.3F),
         SoundDesign.layer(this.veiled ? SeiryuSounds.LAYER_CRACKLE : SeiryuSounds.LAYER_FLAME, 0.6F, 1.2F)
      );
   }

   public void claw(ClientLevel level, int n, Vec3 at, Vec3 dir, @Nullable Entity target) {
      this.anticipating = false;
      this.lastEvent = this.age;
      Entity e = this.caster(level);
      switch (n) {
         case 1:
            this.clawFire(level, at, e);
            break;
         case 2:
            this.clawVortex(level, at, target, e);
            break;
         default:
            this.clawSmash(level, at, dir, e);
      }
   }

   private void clawFire(ClientLevel level, Vec3 at, @Nullable Entity e) {
      VfxParticles.emit(VfxPresets.FLARE, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(1.25F).heat(1.15F).rotation(this.random.nextFloat() * 6.28F);
      VfxParticles.emit(VfxPresets.GLOW, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(5.5F).tint(16742938).alpha(0.8F).life(1.8F);

      for (int i = 0; i < VfxSettings.count(3.0F) + 1; i++) {
         VfxParticles.emit(
               VfxPresets.FIREBALL, at.x + this.spread(0.4), at.y + this.spread(0.4), at.z + this.spread(0.4), this.spread(0.06), 0.03, this.spread(0.06)
            )
            .size(0.85F + this.random.nextFloat() * 0.35F)
            .life(0.42F);
      }

      for (int i = 0; i < VfxSettings.count(24.0F); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double b = (this.random.nextDouble() - 0.4) * 1.4;
         double sp = 0.7 + this.random.nextDouble() * 0.6;
         VfxParticles.emit(VfxPresets.SPARK, at.x, at.y, at.z, Math.cos(a) * Math.cos(b) * sp, Math.sin(b) * sp, Math.sin(a) * Math.cos(b) * sp)
            .heat(0.62F)
            .size(2.3F)
            .life(0.6F);
      }

      for (int i = 0; i < VfxSettings.count(20.0F); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         VfxParticles.emit(VfxPresets.FIRE_TONGUE, at.x, at.y, at.z, Math.cos(a) * 0.4, this.spread(0.3), Math.sin(a) * 0.4).size(1.7F);
      }

      Impact.at(level, at, this.seed ^ 17L)
         .caster(e)
         .wave(2.8F, 10, 16777215)
         .sparks(this.veiled ? 6 : 0, 0.4F)
         .embers(10)
         .trauma(0.42F, 0.1F, 2.6F)
         .hitStop(2)
         .fov(0.04F)
         .lens(0.35F)
         .play();
      this.whiteSmokeRing(level, at);
      GroundWave.at(at.x, at.y - 0.6, at.z, 0.1F, 8).air(at.y, 1.8F).spawn();
      VeilAuraCinematic.pulseOf(e);
      SoundDesign.play(
         at, SeiryuSounds.RUSH_CLAW, 1.2F, 1.0F, SoundDesign.layer(SeiryuSounds.LAYER_FLAME, 0.8F, 1.2F), SoundDesign.layer(DFSounds.LAYER_IMPACT, 0.7F, 1.1F)
      );
   }

   private void whiteSmokeRing(ClientLevel level, Vec3 at) {
      double gy = VfxWorld.ground(level, at.x, at.y + 0.5, at.z, 5);
      if (!Double.isNaN(gy)) {
         for (int i = 0; i < VfxSettings.count(16.0F); i++) {
            double a = (double)((float) (Math.PI * 2) * (float)i / 16.0F) + this.random.nextDouble() * 0.3;
            VfxParticles.emit(VfxPresets.DUST, at.x + Math.cos(a) * 0.9, gy + 0.25, at.z + Math.sin(a) * 0.9, Math.cos(a) * 0.48, 0.015, Math.sin(a) * 0.48)
               .tint(this.shade(14999786))
               .size(0.7F)
               .alpha(0.75F);
         }
      }
   }

   private void clawVortex(ClientLevel level, Vec3 at, @Nullable Entity target, @Nullable Entity e) {
      int palette = this.veiled ? 1 : 0;
      ArcBurst.at(at, this.seed ^ 34L)
         .follow(target, 0.5F)
         .arcs(3)
         .radius(1.1F, 2.0F)
         .life(16)
         .spin(this.veiled ? 0.95F : 0.8F)
         .arc((float) (Math.PI * 8.0 / 5.0))
         .tilt(1.05F, 1.5F)
         .look(MeshTex.FLAME_RIBBON, palette, 0.62F, 1.45F, this.veiled ? 3.6F : 2.2F)
         .spawn();
      if (this.veiled) {
         ArcBurst.at(at, this.seed ^ 35L)
            .follow(target, 0.5F)
            .arcs(2)
            .radius(1.5F, 2.3F)
            .life(13)
            .spin(-1.2F)
            .arc((float) Math.PI * 11.0F / 10.0F)
            .tilt(1.15F, 1.5F)
            .look(MeshTex.CRACKLE, 1, 0.7F, 1.35F, 3.0F)
            .spawn();

         for (int i = 0; i < 3; i++) {
            double a = (double)((float)i * (float) (Math.PI * 2) / 3.0F) + this.random.nextDouble();
            Vec3 from = at.add(Math.cos(a) * 0.7, this.spread(0.5), Math.sin(a) * 0.7);
            BoltStrike.between(from, from.add(Math.cos(a + 1.4) * 1.3, this.spread(0.8), Math.sin(a + 1.4) * 1.3), this.seed + (long)i * 37L)
               .width(0.08F)
               .life(4)
               .forks(2)
               .spawn();
         }
      } else {
         ArcBurst.at(at, this.seed ^ 36L)
            .follow(target, 0.5F)
            .arcs(2)
            .radius(1.0F, 1.6F)
            .life(12)
            .spin(-1.0F)
            .arc((float) Math.PI)
            .tilt(1.0F, 1.4F)
            .colour(MeshTex.FLAME_RIBBON, 15790324, 0.4F)
            .alpha(0.7F)
            .spawn();
      }

      Impact.at(level, at, this.seed ^ 37L)
         .caster(e)
         .flash(0.45F)
         .glow(this.veiled ? 11955455 : 16742938)
         .sparks(16, 0.5F)
         .trauma(0.3F, 0.1F, 1.8F)
         .lens(0.3F)
         .play();
      if (this.veiled) {
         Impact.at(level, at, this.seed ^ 38L).thunder().sparks(14, 0.55F).play();
      }

      VeilAuraCinematic.pulseOf(e);
      SoundDesign.play(
         at,
         SeiryuSounds.RUSH_CLAW,
         1.2F,
         1.15F,
         SoundDesign.layer(this.veiled ? DFSounds.LAYER_THUNDER : SeiryuSounds.LAYER_FLAME, 0.6F, 1.6F),
         SoundDesign.layer(DFSounds.LAYER_SONIC, 0.3F, 1.6F)
      );
   }

   private void clawSmash(ClientLevel level, Vec3 at, Vec3 dir, @Nullable Entity e) {
      Vec3 d = dir.lengthSqr() < 1.0E-4 ? new Vec3((double)this.facingX, 0.0, (double)this.facingZ) : dir.normalize();

      for (int i = 0; i < VfxSettings.count(30.0F); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double sp = 0.1 + this.random.nextDouble() * 0.16;
         double ox = Math.cos(a) * sp * 0.8 + d.x * sp;
         double oz = Math.sin(a) * sp * 0.8 + d.z * sp;
         VfxParticles.emit(
               VfxPresets.SMOKE, at.x + this.spread(0.8), at.y + this.spread(0.9), at.z + this.spread(0.8), ox, 0.02 + this.random.nextDouble() * 0.06, oz
            )
            .tint(this.shade(1182746))
            .size(1.2F + this.random.nextFloat() * 0.7F)
            .life(0.6F);
      }

      Impact.at(level, at, this.seed ^ 49L)
         .caster(e)
         .flash(0.35F)
         .glow(16742938)
         .sparks(18, 0.55F)
         .embers(12)
         .debris(6, 0.18F)
         .trauma(0.5F, 0.1F, 3.2F)
         .hitStop(2)
         .fov(0.05F)
         .lens(0.4F)
         .wave(2.6F, 10, -1)
         .play();
      if (this.veiled) {
         Impact.at(level, at, this.seed ^ 50L).thunder().sparks(16, 0.55F).play();
      }

      double gy = VfxWorld.ground(level, at.x, at.y + 0.5, at.z, 6);
      if (!Double.isNaN(gy)) {
         double cx = at.x + d.x * 0.8;
         double cz = at.z + d.z * 0.8;
         float rot = (float)Math.atan2(d.z, d.x) + 0.8F;
         VfxParticles.emit(VfxPresets.CLAW_SOOT, cx, gy + 0.03, cz, 0.0, 0.0, 0.0).size(3.4F).rotation(rot);
         VfxParticles.emit(VfxPresets.CLAW_GLOW, cx, gy + 0.035, cz, 0.0, 0.0, 0.0).size(3.4F).rotation(rot);
      }

      VeilAuraCinematic.pulseOf(e);
      SoundDesign.play(
         at, SeiryuSounds.RUSH_SMASH, 1.3F, 1.0F, SoundDesign.layer(DFSounds.LAYER_BOOM, 0.6F, 1.3F), SoundDesign.layer(DFSounds.LAYER_IMPACT, 0.8F, 0.9F)
      );
   }

   public void finisher(ClientLevel level, Vec3 at, float radius) {
      this.anticipating = false;
      this.lastEvent = this.age;
      this.darken = 0;
      Entity e = this.caster(level);
      Vec3 top = at.add(this.spread(1.2), 26.0, this.spread(1.2));
      BoltStrike.between(top, at, this.seed ^ 241L).width(0.62F).life(11).forks(5).rough(0.09F).heat(1.2F).endGlow(2.2F).spawn();
      BoltStrike.between(top.add(this.spread(3.0), -2.0, this.spread(3.0)), at.add(this.spread(1.2), 0.0, this.spread(1.2)), this.seed ^ 242L)
         .width(0.22F)
         .life(7)
         .forks(3)
         .spawn();
      BoltStrike.between(top.add(this.spread(4.0), -4.0, this.spread(4.0)), at.add(this.spread(2.0), 0.0, this.spread(2.0)), this.seed ^ 243L)
         .width(0.14F)
         .life(5)
         .forks(2)
         .spawn();
      Vec3 mid = at.add(0.0, 1.1, 0.0);
      this.sphere = 0;
      this.sx = mid.x;
      this.sy = mid.y;
      this.sz = mid.z;
      this.sphereRadius = radius * 0.75F;

      for (int i = 0; i < 6; i++) {
         double a = (double)((float)i * (float) (Math.PI * 2) / 6.0F) + this.random.nextDouble() * 0.5;
         BoltStrike.between(
               mid,
               mid.add(Math.cos(a) * (double)radius * 1.2, this.spread((double)radius * 0.5), Math.sin(a) * (double)radius * 1.2),
               this.seed + (long)i * 53L
            )
            .width(0.09F)
            .life(6)
            .forks(2)
            .spawn();
      }

      Impact.at(level, mid, this.seed ^ 244L)
         .caster(e)
         .thunder()
         .flash(1.3F)
         .sparks(36, 0.7F)
         .embers(30)
         .smoke(12, 1708080, 1.3F)
         .debris(16, 0.24F)
         .trauma(0.95F, 0.12F, 5.0F)
         .hitStop(3)
         .fov(0.1F)
         .lens(1.0F)
         .screenFlash(0.3F)
         .play();
      GroundWave.at(at.x, at.y, at.z, radius * 2.6F, 14)
         .strength(1.4F)
         .ring(15984895, 0.9F)
         .air(mid.y, radius * 1.8F)
         .dust(18, this.shade(VfxWorld.groundColour(level, at.x, at.y - 0.5, at.z)), radius * 0.6F, 0.34F, this.seed ^ 245L)
         .spawn();
      float rot = this.random.nextFloat() * (float) (Math.PI * 2);
      VfxParticles.emit(VfxPresets.SCORCH, at.x, at.y + 0.03, at.z, 0.0, 0.0, 0.0).size(radius * 2.4F).rotation(rot).life(1.8F);
      VfxParticles.emit(VfxPresets.SCORCH_RING, at.x, at.y + 0.032, at.z, 0.0, 0.0, 0.0).size(radius * 2.2F).tint(460044).alpha(0.95F);
      VfxParticles.emit(VfxPresets.CRACKS_VIOLET, at.x, at.y + 0.036, at.z, 0.0, 0.0, 0.0).size(radius * 2.4F).rotation(rot).life(1.4F);

      for (int i = 0; i < VfxSettings.count(14.0F); i++) {
         VfxParticles.emit(
               VfxPresets.EMBER,
               at.x + this.spread((double)radius),
               at.y + 0.3,
               at.z + this.spread((double)radius),
               this.spread(0.06),
               0.06 + this.random.nextDouble() * 0.1,
               this.spread(0.06)
            )
            .life(1.4F);
      }

      if (!this.firstPersonCaster()) {
         VfxManager.add(new FloorDebrisEffect(level, at, 8, 0.3F, 0.7F, 80, this.seed ^ 246L));
      }

      VeilAuraCinematic.pulseOf(e);
      SoundDesign.play(
         at,
         SeiryuSounds.RUSH_FINISHER,
         2.2F,
         0.95F,
         true,
         SoundDesign.layer(DFSounds.LAYER_THUNDER, 1.4F, 0.9F),
         SoundDesign.layer(DFSounds.LAYER_BOOM, 0.9F, 0.8F)
      );
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.caster(level);
      if (this.firstPersonCaster()) {
         VfxParticles.nearFade(3.0F);
      }

      if (this.anticipating && this.age <= Math.max(1, this.params.extra()) + 2) {
         this.tickCrouch(e);
      }

      if (this.dashLeft >= 0) {
         float t = VfxClock.time();
         this.streakHead(this.x, this.y, this.z, this.headA, this.headB);
         if (this.streakA != null) {
            this.streakA.push(this.headA[0], this.headA[1], this.headA[2], t);
         }

         if (this.streakB != null) {
            this.streakB.push(this.headB[0], this.headB[1], this.headB[2], t);
         }
      }

      if (this.dashLeft >= 0) {
         this.tickDash(level);
         this.dashLeft--;
      }

      if (this.sphere >= 0 && ++this.sphere > 12) {
         this.sphere = -1;
      }

      if (this.darken >= 0) {
         this.darken++;
         float ex = this.exposure();
         if (ex > 0.0F) {
            float k = this.darken < 3 ? (float)this.darken / 3.0F : Math.max(0.0F, 1.0F - (float)(this.darken - 3) / 12.0F);
            VfxPost.grade(8023198, 0.55F * k * ex);
            VfxPost.vignette(0.35F * k * ex);
         }

         if (this.darken > 16) {
            this.darken = -1;
         }
      }

      if (this.age - this.lastEvent > 50) {
         this.finish();
      }
   }

   private void streakHead(double bx, double by, double bz, double[] a, double[] b) {
      a[0] = bx - (double)this.facingZ * 0.35;
      a[1] = by + (double)this.height * 0.45;
      a[2] = bz + (double)this.facingX * 0.35;
      b[0] = bx + (double)this.facingZ * 0.3;
      b[1] = by + (double)this.height * 0.28;
      b[2] = bz - (double)this.facingX * 0.3;
   }

   private void tickCrouch(@Nullable Entity e) {
      if (e != null) {
         HybridAnchors.get(e, 0, 1.0F, this.anchor3);
         Vec3 right = new Vec3(this.anchor3.x, this.anchor3.y, this.anchor3.z);
         HybridAnchors.get(e, 1, 1.0F, this.anchor3);
         Vec3 left = new Vec3(this.anchor3.x, this.anchor3.y, this.anchor3.z);
         if (this.veiled) {
            BoltStrike.between(right, left, this.seed + (long)this.age * 11L).width(0.1F).life(2).forks(2).rough(0.3F).rejitter(1).endGlow(0.8F).spawn();
            if (this.age % 2 == 0) {
               BoltStrike.between(left, right.add(this.spread(0.4), this.spread(0.4), this.spread(0.4)), this.seed + (long)this.age * 13L)
                  .width(0.05F)
                  .life(2)
                  .forks(1)
                  .rough(0.35F)
                  .endGlow(0.0F)
                  .spawn();
            }

            for (Vec3 w : new Vec3[]{right, left}) {
               VfxParticles.emit(VfxPresets.GLOW, w.x, w.y, w.z, 0.0, 0.0, 0.0).size(1.1F).tint(11955455).alpha(0.7F);
               VfxParticles.emit(VfxPresets.THUNDER_SPARK, w.x, w.y, w.z, this.spread(0.15), 0.1, this.spread(0.15));
            }
         } else {
            for (Vec3 w : new Vec3[]{right, left}) {
               VfxParticles.emit(
                     VfxPresets.FIRE_TONGUE, w.x + this.spread(0.1), w.y + this.spread(0.1), w.z + this.spread(0.1), this.spread(0.03), 0.05, this.spread(0.03)
                  )
                  .size(1.1F);
               VfxParticles.emit(VfxPresets.GLOW, w.x, w.y, w.z, 0.0, 0.0, 0.0).size(1.0F).tint(16742938).alpha(0.6F);
            }
         }

         if (this.local()) {
            CameraDirector.dolly(0.93F, 0.4F);
         }
      }
   }

   private void tickDash(ClientLevel level) {
      if (!Double.isNaN(this.groundY) && this.y - this.groundY < 2.5) {
         for (int i = 0; i < VfxSettings.count(2.0F); i++) {
            VfxParticles.emit(
                  VfxPresets.DUST,
                  this.x + this.spread(0.5),
                  this.groundY + 0.2,
                  this.z + this.spread(0.5),
                  (double)(-this.facingX) * 0.08 + this.spread(0.04),
                  0.03,
                  (double)(-this.facingZ) * 0.08 + this.spread(0.04)
               )
               .tint(this.shade(VfxWorld.groundColour(level, this.x, this.groundY - 0.5, this.z)))
               .size(0.8F);
         }
      }

      VfxParticles.emit(
            this.veiled ? VfxPresets.THUNDER_PUFF : VfxPresets.FIRE_PUFF,
            this.headA[0],
            this.headA[1],
            this.headA[2],
            (double)(-this.facingX) * 0.1,
            0.02,
            (double)(-this.facingZ) * 0.1
         )
         .size(0.8F);
      if (this.veiled) {
         VfxParticles.emit(
            VfxPresets.THUNDER_SPARK,
            this.headB[0],
            this.headB[1],
            this.headB[2],
            (double)(-this.facingX) * 0.3 + this.spread(0.1),
            0.1,
            (double)(-this.facingZ) * 0.3 + this.spread(0.1)
         );
      }

      if (this.local()) {
         Minecraft mc = Minecraft.getInstance();
         Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();

         for (int i = 0; i < VfxSettings.count(3.0F); i++) {
            double side = (double)(this.random.nextBoolean() ? 1 : -1) * (2.2 + this.random.nextDouble() * 2.2);
            double px = cam.x + (double)this.facingX * (4.0 + this.random.nextDouble() * 3.0) - (double)this.facingZ * side;
            double pz = cam.z + (double)this.facingZ * (4.0 + this.random.nextDouble() * 3.0) + (double)this.facingX * side;
            double py = cam.y + this.spread(2.2);
            VfxParticles.emit(VfxPresets.SPEED_LINE, px, py, pz, (double)(-this.facingX) * 1.2, 0.0, (double)(-this.facingZ) * 1.2)
               .tint(this.veiled ? 14207231 : 16771280)
               .alpha(0.3F);
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      float since = (float)(this.age - this.lastEvent) + f.partial;
      float fade = this.dashLeft >= 0 ? 1.0F : Math.max(0.0F, 1.0F - since / 6.0F);
      if (fade > 0.01F && !this.firstPersonCaster() && this.dashTicks > 0) {
         if (this.dashLeft >= 0) {
            float p = f.entityPartial;
            this.streakHead(this.rx(p), this.ry(p), this.rz(p), this.liveA, this.liveB);
         }

         if (this.streakA != null) {
            this.streakA.render(f, this.liveA[0], this.liveA[1], this.liveA[2], fade);
         }

         if (this.streakB != null) {
            this.streakB.render(f, this.liveB[0], this.liveB[1], this.liveB[2], fade);
         }
      }

      if (this.sphere >= 0 && f.allowMesh()) {
         float p = Mth.clamp(((float)this.sphere + f.partial) / 12.0F, 0.0F, 1.0F);
         float r = this.sphereRadius * (0.5F + 0.6F * (1.0F - (1.0F - p) * (1.0F - p)));
         float a = p < 0.2F ? 1.0F : (1.0F - p) / 0.8F;
         float cx = f.rx(this.sx);
         float cy = f.ry(this.sy);
         float cz = f.rz(this.sz);
         float d = Mth.sqrt(cx * cx + cy * cy + cz * cz) + 1.0E-4F;
         boolean inside = d < r * 1.05F;
         float ax = inside ? 0.0F : -cx / d;
         float ay = inside ? 1.0F : -cy / d;
         float az = inside ? 0.0F : -cz / d;
         float cap = inside ? (float) Math.PI : (float) (Math.PI / 2);
         MeshWriter.dome(
            f.mesh(MeshTex.NOISE),
            cx,
            cy,
            cz,
            ax,
            ay,
            az,
            r,
            cap,
            8,
            24,
            f.time * 0.02F,
            0.0F,
            2.0F,
            1.0F,
            MeshWriter.argb(16777215, a),
            1.2F,
            1,
            2.6F,
            0.0F,
            1.0F
         );
         if (f.allowMesh()) {
            MeshWriter.dome(
               f.mesh(MeshTex.CRACKLE),
               cx,
               cy,
               cz,
               ax,
               ay,
               az,
               r * 0.97F,
               cap,
               8,
               24,
               this.random.nextFloat(),
               this.random.nextFloat(),
               2.0F,
               1.0F,
               MeshWriter.argb(16777215, a),
               1.3F,
               1,
               3.0F,
               0.0F,
               0.0F
            );
         }
      }
   }

   @Override
   public void removed() {
      if (this.streakA != null) {
         this.streakA.release();
      }

      if (this.streakB != null) {
         this.streakB.release();
      }
   }
}
