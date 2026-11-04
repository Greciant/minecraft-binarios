package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.VfxSound;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.BoltStrike;
import com.devilfruits.client.vfx.fx.Impact;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.Ribbon;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuSounds;
import com.devilfruits.registry.DFParticles;
import com.devilfruits.registry.DFSounds;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ChargedJumpCinematic extends HybridCinematic {
   private static final int TAG_COIL = 32321;
   private static final int GRADE = 10128584;
   private final int chargeTicks;
   private int launched = -1;
   private int landed = -1;
   private int cancelled = -1;
   private boolean full;
   private int cracks;
   private float charge;
   @Nullable
   private final Ribbon feetTrail;
   @Nullable
   private final Ribbon leftTrail;
   @Nullable
   private final Ribbon rightTrail;
   @Nullable
   private VfxSound swell;
   private final ShockRing.Band band = new ShockRing.Band();
   private final MutableBlockPos pos = new MutableBlockPos();
   private float trailFade;

   public ChargedJumpCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.chargeTicks = Math.max(1, Math.round(params.power()));
      this.feetTrail = Ribbon.create(28);
      if (this.feetTrail != null) {
         this.feetTrail.look(0.5F * this.scale() + 0.08F, 7.0F, 1.25F, 0.6F, 2.0F, 1).texture(MeshTex.FLAME_RIBBON, 0.3F);
      }

      this.leftTrail = Ribbon.create(18);
      this.rightTrail = Ribbon.create(18);

      for (Ribbon r : new Ribbon[]{this.leftTrail, this.rightTrail}) {
         if (r != null) {
            r.look(0.07F * this.scale() + 0.02F, 8.0F, -1.0F, -1.0F, 0.6F, 0).texture(MeshTex.FLAME_RIBBON, 0.35F).tint(15789304);
         }
      }

      Entity e = anchor.entity(level);
      if (e != null) {
         this.swell = VfxSound.play(
            SeiryuSounds.ASCENSION_CHARGE.get(),
            e,
            e.position(),
            false,
            a -> 0.3F + 0.7F * Math.min(1.0F, (float)a / (float)this.chargeTicks),
            a -> 0.8F + 0.35F * Math.min(1.0F, (float)a / (float)this.chargeTicks),
            this.chargeTicks * 8,
            () -> !this.isDone() && this.launched < 0 && this.cancelled < 0
         );
      }
   }

   @Nullable
   public static ChargedJumpCinematic of(Entity e) {
      return VfxDirector.find(ChargedJumpCinematic.class, c -> c.follows(e) && c.landed < 0 && c.cancelled < 0);
   }

   private float scale() {
      return Math.max(0.3F, this.height / 3.6F);
   }

   private double floor() {
      return this.onFloor(2.0) ? this.groundY : this.y;
   }

   private void endSwell() {
      if (this.swell != null) {
         this.swell.end();
         this.swell = null;
      }
   }

   public void cancel() {
      this.endSwell();
      if (this.launched < 0 && this.cancelled < 0) {
         this.cancelled = this.age;
      }
   }

   public void launch(ClientLevel level, Vec3 feet, Vec3 v, float charge) {
      this.endSwell();
      this.launched = 0;
      this.charge = charge;
      this.trailFade = 1.0F;
      float s = 0.4F + 0.6F * charge;
      float sc = this.scale();
      Entity e = this.caster(level);
      double gy = VfxWorld.ground(level, feet.x, feet.y + 0.5, feet.z, 3);
      Vec3 base = new Vec3(feet.x, Double.isNaN(gy) ? feet.y : gy, feet.z);
      Impact.at(level, base.add(0.0, 0.3, 0.0), this.seed ^ 58L)
         .caster(e)
         .thunder()
         .flash(0.8F + 0.8F * s)
         .wave((3.0F + 5.0F * s) * sc, 12, 15984895)
         .dust(Math.round(10.0F + 14.0F * s))
         .sparks(Math.round(14.0F + 18.0F * s), 0.5F)
         .debris(Math.round(6.0F + 10.0F * s), 0.25F)
         .decal((2.4F + 2.0F * s) * sc)
         .trauma(0.2F + 0.4F * s, 0.09F, 3.0F)
         .fov(0.1F * s)
         .lens(0.6F * s)
         .screenFlash(0.12F * s)
         .play();
      int arcs = 4 + Math.round(4.0F * s);
      double turn = this.random.nextDouble() * (float) (Math.PI * 2);

      for (int k = 0; k < arcs; k++) {
         double a = turn + (double)((float)k * (float) (Math.PI * 2) / (float)arcs) + this.spread(0.3);
         double d = (1.5 + this.random.nextDouble() * 2.0) * (0.6 + 0.6 * (double)s) * (double)sc;
         BoltStrike.between(base.add(0.0, 0.05, 0.0), base.add(Math.cos(a) * d, 0.05, Math.sin(a) * d), this.seed + 11L * (long)k)
            .width(0.08F * sc)
            .life(6)
            .forks(1)
            .rough(0.22F)
            .endGlow(0.4F)
            .spawn();
      }

      Vec3 dir = v.lengthSqr() > 1.0E-4 ? v.normalize() : new Vec3(0.0, 1.0, 0.0);

      for (int k = 0; k < 2; k++) {
         Vec3 to = base.add(dir.scale((2.5 + 1.5 * (double)k) * (double)sc)).add(this.spread(0.6), 0.0, this.spread(0.6));
         BoltStrike.between(base.add(0.0, 0.1, 0.0), to, this.seed ^ (long)(k + 3) * 31L).width(0.1F * sc).life(5).forks(2).rough(0.18F).endGlow(0.0F).spawn();
      }

      ShockRing.at(base.add(0.0, 0.4, 0.0), new Vec3(0.0, 1.0, 0.0))
         .radius(0.6F * sc, (4.0F + 4.0F * s) * sc)
         .life(9)
         .strength(1.5F)
         .band(0.3F)
         .rim(0.2F, 15984895)
         .spawn();
      double speed = v.length();

      for (int k = 0; k < 2; k++) {
         ShockRing.at(base.add(dir.scale((1.0 + 1.2 * (double)k) * (double)sc)), dir)
            .radius((1.0F + 0.3F * (float)k) * sc, (2.0F + 0.5F * (float)k) * sc)
            .life(7 + 2 * k)
            .move(dir.scale(speed * 0.55))
            .strength(1.1F - 0.2F * (float)k)
            .band(0.35F)
            .rim(0.12F, 15984895)
            .spawn();
      }

      double hl = Math.sqrt(v.x * v.x + v.z * v.z);
      if (hl > 0.2 && !Double.isNaN(gy)) {
         int dust = this.shade(VfxWorld.groundColour(level, base.x, base.y - 0.5, base.z));

         for (int i = 0; i < VfxSettings.count(10.0F); i++) {
            VfxParticles.emit(
                  VfxPresets.DUST,
                  base.x + this.spread(0.6),
                  base.y + 0.2,
                  base.z + this.spread(0.6),
                  -v.x / hl * (0.25 + this.random.nextDouble() * 0.3) + this.spread(0.08),
                  0.06 + this.random.nextDouble() * 0.08,
                  -v.z / hl * (0.25 + this.random.nextDouble() * 0.3) + this.spread(0.08)
               )
               .tint(dust)
               .size(1.2F * sc);
         }
      }

      float ex = this.exposure();
      if (ex > 0.0F && this.local()) {
         CameraDirector.snapDolly();
         CameraDirector.fovPunch(0.06F * s * ex);
      }

      SoundDesign.play(
         base,
         SeiryuSounds.ASCENSION_LAUNCH,
         1.4F,
         0.95F,
         SoundDesign.layer(DFSounds.LAYER_BOOM, 0.6F + 0.6F * s, 0.8F),
         SoundDesign.layer(DFSounds.LAYER_THUNDER, 0.5F + 0.5F * s, 1.1F),
         SoundDesign.layer(DFSounds.LAYER_WIND, 1.0F, 1.0F)
      );
   }

   public void land(ClientLevel level, Vec3 feet, float fall) {
      this.endSwell();
      if (this.launched < 0) {
         this.launched = 0;
      }

      this.landed = 0;
      float s = Mth.clamp(fall / 30.0F, 0.15F, 1.0F);
      float sc = this.scale();
      Entity e = this.caster(level);
      double gy = VfxWorld.ground(level, feet.x, feet.y + 0.5, feet.z, 3);
      Vec3 base = new Vec3(feet.x, Double.isNaN(gy) ? feet.y : gy, feet.z);
      Impact.at(level, base.add(0.0, 0.3, 0.0), this.seed ^ 75L)
         .caster(e)
         .thunder()
         .flash(0.6F + 0.9F * s)
         .wave((3.0F + 6.0F * s) * sc, 12 + Math.round(4.0F * s), 15984895)
         .dust(Math.round(8.0F + 16.0F * s))
         .sparks(Math.round(10.0F + 20.0F * s), 0.5F)
         .debris(Math.round(4.0F + 12.0F * s), 0.2F + 0.1F * s)
         .decal((2.0F + 3.0F * s) * sc)
         .trauma(0.2F + 0.55F * s, 0.08F, 2.0F + 3.0F * s)
         .fov(0.04F + 0.1F * s)
         .lens(0.3F + 0.5F * s)
         .hitStop(s > 0.6F ? 2 : 0)
         .screenFlash(0.08F * s)
         .play();
      int arcs = 3 + Math.round(5.0F * s);
      double turn = this.random.nextDouble() * (float) (Math.PI * 2);

      for (int k = 0; k < arcs; k++) {
         double a = turn + (double)((float)k * (float) (Math.PI * 2) / (float)arcs) + this.spread(0.3);
         double d = (1.5 + this.random.nextDouble() * 2.5) * (0.6 + 0.6 * (double)s) * (double)sc;
         BoltStrike.between(base.add(0.0, 0.05, 0.0), base.add(Math.cos(a) * d, 0.05, Math.sin(a) * d), this.seed + 17L * (long)k)
            .width(0.09F * sc)
            .life(7)
            .forks(1)
            .rough(0.22F)
            .endGlow(0.5F)
            .spawn();
      }

      ShockRing.at(base.add(0.0, 0.35, 0.0), new Vec3(0.0, 1.0, 0.0))
         .radius(0.8F * sc, (3.0F + 5.0F * s) * sc)
         .life(10)
         .strength(1.4F)
         .band(0.3F)
         .rim(0.18F, 15984895)
         .spawn();
      if (!Double.isNaN(gy)) {
         int dust = this.shade(VfxWorld.groundColour(level, base.x, base.y - 0.5, base.z));

         for (int i = 0; i < VfxSettings.count(12.0F + 18.0F * s); i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double sp = 0.2 + this.random.nextDouble() * (0.25 + 0.3 * (double)s);
            VfxParticles.emit(
                  VfxPresets.DUST,
                  base.x + Math.cos(a) * 0.8 * (double)sc,
                  base.y + 0.2,
                  base.z + Math.sin(a) * 0.8 * (double)sc,
                  Math.cos(a) * sp,
                  0.03,
                  Math.sin(a) * sp
               )
               .tint(dust)
               .size((1.0F + 0.6F * s) * sc);
         }
      }

      SoundDesign.play(
         base,
         SeiryuSounds.ASCENSION_IMPACT,
         1.2F + 0.6F * s,
         1.0F - 0.15F * s,
         SoundDesign.layer(DFSounds.LAYER_BOOM, 0.5F + 0.8F * s, 0.8F),
         SoundDesign.layer(DFSounds.LAYER_THUNDER, 0.4F + 0.5F * s, 1.0F),
         SoundDesign.layer(DFSounds.LAYER_IMPACT, 0.8F, 0.9F)
      );
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.cancelled >= 0) {
         if (this.age - this.cancelled > 6) {
            this.finish();
         }
      } else if (this.landed >= 0) {
         this.landed++;
         this.trailFade = Math.max(0.0F, this.trailFade - 0.25F);
         if (this.landed > 30) {
            this.finish();
         }
      } else if (this.launched >= 0) {
         this.flight(level);
      } else {
         this.coil(level);
      }
   }

   private void coil(ClientLevel level) {
      if (this.age > this.chargeTicks * 20) {
         this.finish();
      } else {
         float p = Math.min(1.0F, (float)this.age / (float)this.chargeTicks);
         float sc = this.scale();
         double gy = this.floor();
         boolean floor = this.onFloor(2.0);
         float pulse = this.full ? 0.85F + 0.15F * Mth.sin((float)this.age * 1.3F) : 1.0F;
         if (floor) {
            VfxParticles.emit(VfxPresets.GROUND_GLOW, this.x, gy + 0.04, this.z, 0.0, 0.0, 0.0)
               .size((1.8F + 2.8F * p) * sc)
               .tint(11955455)
               .alpha((0.5F + 0.45F * p) * pulse);

            while (this.cracks < 3 && p >= (float)(this.cracks + 1) / 3.0F - 0.01F) {
               this.cracks++;
               VfxParticles.emit(VfxPresets.CRACKS_VIOLET, this.x, gy + 0.035, this.z, 0.0, 0.0, 0.0)
                  .size((1.6F + 1.2F * (float)this.cracks) * sc)
                  .rotation(this.random.nextFloat() * (float) (Math.PI * 2))
                  .life(1.2F);
               SoundDesign.layerOnly(
                  new Vec3(this.x, gy, this.z), SeiryuSounds.LAYER_CRACKLE, 0.5F + 0.2F * (float)this.cracks, 0.8F + 0.15F * (float)this.cracks
               );
            }

            if (this.age % 2 == 0) {
               for (int i = 0; i < Fx.count(1 + Math.round(3.0F * p)); i++) {
                  double a = this.random.nextDouble() * (float) (Math.PI * 2);
                  double d = (0.8 + this.random.nextDouble() * 1.4) * (double)sc;
                  double px = this.x + Math.cos(a) * d;
                  double pz = this.z + Math.sin(a) * d;
                  BlockState ground = Fx.groundAt(level, px, gy - 0.5, pz, this.pos);
                  if (ground != null) {
                     Fx.spawn(
                        new BlockParticleOption(DFParticles.BLOCK_DEBRIS.get(), ground),
                        px,
                        (double)this.pos.getY() + 1.02,
                        pz,
                        this.spread(0.03),
                        0.08 + this.random.nextDouble() * 0.1 * (0.5 + (double)p),
                        this.spread(0.03)
                     );
                  }
               }
            }

            int dust = this.shade(VfxWorld.groundColour(level, this.x, gy - 0.5, this.z));

            for (int ix = 0; ix < VfxSettings.count(0.6F + p); ix++) {
               double a = this.random.nextDouble() * (float) (Math.PI * 2);
               double d = (2.4 + this.random.nextDouble()) * (double)sc;
               VfxParticles.emit(
                     VfxPresets.DUST,
                     this.x + Math.cos(a) * d,
                     gy + 0.12,
                     this.z + Math.sin(a) * d,
                     -Math.cos(a) * 0.09 * (double)sc,
                     0.005,
                     -Math.sin(a) * 0.09 * (double)sc
                  )
                  .tint(dust)
                  .size(0.6F * sc)
                  .alpha(0.5F)
                  .life(0.8F);
            }

            if (this.age % 7 == 3) {
               ShockRing.at(new Vec3(this.x, gy + 0.3, this.z), new Vec3(0.0, 1.0, 0.0))
                  .radius(2.6F * sc, 0.5F * sc)
                  .life(7)
                  .strength(0.8F + 0.5F * p)
                  .band(0.4F)
                  .rim(0.08F, 11955455)
                  .spawn();
            }
         }

         for (int k = 0; k < (this.full ? 2 : 1) && this.age % (!this.full && !(p > 0.5F) ? 2 : 1) == 0; k++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double b = this.random.nextDouble() * (float) (Math.PI * 2);
            Vec3 leg = new Vec3(
               this.x + Math.cos(a) * 0.25 * (double)sc,
               this.y + this.random.nextDouble() * (double)this.height * 0.35,
               this.z + Math.sin(a) * 0.25 * (double)sc
            );
            double d = (1.2 + this.random.nextDouble() * 1.4) * (double)sc;
            BoltStrike.between(leg, new Vec3(this.x + Math.cos(b) * d, gy + 0.03, this.z + Math.sin(b) * d), this.random.nextLong())
               .width((0.07F + 0.06F * p) * sc)
               .life(3)
               .forks(1)
               .rough(0.28F)
               .endGlow(0.3F)
               .spawn();
         }

         for (int ix = 0; ix < VfxSettings.count(0.5F + p); ix++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double d = (0.5 + this.random.nextDouble() * 0.4) * (double)sc;
            VfxParticles.emit(
                  VfxPresets.THUNDER_PUFF,
                  this.x + Math.cos(a) * d,
                  gy + 0.1 + this.random.nextDouble() * 0.4 * (double)sc,
                  this.z + Math.sin(a) * d,
                  -Math.sin(a) * 0.08 - Math.cos(a) * 0.02,
                  0.05 + 0.05 * (double)p,
                  Math.cos(a) * 0.08 - Math.sin(a) * 0.02
               )
               .size((0.35F + 0.25F * p) * sc)
               .life(0.7F);
         }

         for (int ix = 0; ix < VfxSettings.count(1.0F + 2.0F * p); ix++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double d = (0.6 + this.random.nextDouble() * 0.6) * (double)sc;
            VfxParticles.emit(
               VfxPresets.THUNDER_SPARK,
               this.x + Math.cos(a) * d,
               this.y + this.random.nextDouble() * (double)this.height * 0.7,
               this.z + Math.sin(a) * d,
               this.spread(0.05),
               0.15 + this.random.nextDouble() * 0.2,
               this.spread(0.05)
            );
         }

         if (this.full) {
            for (int ix = 0; ix < VfxSettings.count(2.0F); ix++) {
               VfxParticles.emit(
                  VfxPresets.THUNDER_SPARK,
                  this.x + this.spread(0.8 * (double)sc),
                  gy + 0.1,
                  this.z + this.spread(0.8 * (double)sc),
                  this.spread(0.15),
                  0.1 + this.random.nextDouble() * 0.2,
                  this.spread(0.15)
               );
            }
         }

         for (int ix = 0; ix < VfxSettings.count(1.0F + 2.0F * p); ix++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double d = (double)this.height * (0.1 + this.random.nextDouble() * 0.15);
            VfxParticles.emit(
               VfxPresets.THUNDER_MOTE,
               this.x + Math.cos(a) * d,
               this.y + this.random.nextDouble() * (double)this.height * 0.5,
               this.z + Math.sin(a) * d,
               this.spread(0.02),
               0.05 + this.random.nextDouble() * 0.06,
               this.spread(0.02)
            );
         }

         if (!this.full && p >= 1.0F) {
            this.full = true;
            Vec3 mid = new Vec3(this.x, gy + 0.6 * (double)sc, this.z);
            VfxParticles.emit(VfxPresets.THUNDER_FLARE, mid.x, mid.y, mid.z, 0.0, 0.0, 0.0)
               .size(0.55F * sc)
               .rotation(this.random.nextFloat() * (float) (Math.PI * 2));
            VfxParticles.emit(VfxPresets.GLOW, mid.x, mid.y, mid.z, 0.0, 0.0, 0.0).size(3.0F * sc).tint(11955455).alpha(0.8F).life(0.6F);
            ShockRing.at(mid, new Vec3(0.0, 1.0, 0.0)).radius(0.5F * sc, 2.6F * sc).life(7).strength(1.0F).band(0.35F).rim(0.15F, 15984895).spawn();
            SoundDesign.play(mid, SeiryuSounds.LAYER_CRACKLE, 0.9F, 1.5F, SoundDesign.layer(DFSounds.LAYER_THUNDER, 0.35F, 1.6F));
         }

         float ex = this.exposure();
         if (this.local()) {
            CameraDirector.dolly(1.0F - 0.15F * p, 0.15F);
         }

         if (ex > 0.0F) {
            VfxPost.vignette(0.3F * p * ex);
            VfxPost.grade(10128584, 0.18F * p * ex);
            if (this.age % 3 == 0) {
               CameraDirector.shake(32321, (0.04F + 0.14F * p) * ex, 0.06F, 0.6F + 1.2F * p, 3.5F);
            }
         }
      }
   }

   private void flight(ClientLevel level) {
      this.launched++;
      if (this.launched > 200) {
         this.finish();
      } else {
         double vx = this.x - this.ox;
         double vy = this.y - this.oy;
         double vz = this.z - this.oz;
         double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
         float target = (float)Mth.clamp((speed - 0.25) / 0.5, 0.0, 1.0);
         this.trailFade = this.trailFade + (target - this.trailFade) * 0.3F;
         float t = VfxClock.time();
         float sc = this.scale();
         Entity e = this.caster(level);
         float yaw = e != null ? e.getYRot() * (float) (Math.PI / 180.0) : 0.0F;
         double rx = (double)(-Mth.cos(yaw));
         double rz = (double)(-Mth.sin(yaw));
         if (this.feetTrail != null) {
            this.feetTrail.push(this.x, this.y + 0.2 * (double)sc, this.z, t);
         }

         if (this.leftTrail != null) {
            this.leftTrail.push(this.x - rx * 0.55 * (double)sc, this.y + (double)this.height * 0.72, this.z - rz * 0.55 * (double)sc, t);
         }

         if (this.rightTrail != null) {
            this.rightTrail.push(this.x + rx * 0.55 * (double)sc, this.y + (double)this.height * 0.72, this.z + rz * 0.55 * (double)sc, t);
         }

         if (speed > 0.4) {
            boolean fp = this.firstPersonCaster();

            for (int i = 0; i < VfxSettings.count((fp ? 5.0F : 3.5F) * this.trailFade); i++) {
               double a = this.random.nextDouble() * (float) (Math.PI * 2);
               double d = (fp ? 1.5 + this.random.nextDouble() * 2.0 : 1.0 + this.random.nextDouble() * 1.5) * (double)sc;
               double h = fp ? (double)this.height * 0.9 + this.spread(1.5) : this.random.nextDouble() * (double)this.height;
               VfxParticles.emit(
                     VfxPresets.SPEED_LINE,
                     this.x + Math.cos(a) * d + vx * (double)(fp ? 3 : 0),
                     this.y + h + vy * (double)(fp ? 3 : 0),
                     this.z + Math.sin(a) * d + vz * (double)(fp ? 3 : 0),
                     -vx * 0.8,
                     -vy * 0.8,
                     -vz * 0.8
                  )
                  .tint(15261951)
                  .alpha(0.75F);
            }

            if (this.random.nextFloat() < this.trailFade) {
               VfxParticles.emit(
                  VfxPresets.THUNDER_MOTE,
                  this.x + this.spread(0.4 * (double)sc),
                  this.y + this.random.nextDouble() * (double)this.height,
                  this.z + this.spread(0.4 * (double)sc),
                  this.spread(0.03),
                  -0.02,
                  this.spread(0.03)
               );
            }
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      double hx = this.rx(f.entityPartial);
      double hy = this.ry(f.entityPartial);
      double hz = this.rz(f.entityPartial);
      float sc = this.scale();
      if (this.launched < 0 && this.cancelled < 0 && this.onFloor(2.0)) {
         float p = Math.min(1.0F, t / (float)this.chargeTicks);
         this.band
            .draw(
               f,
               f.rx(hx),
               f.ry(this.groundY + 0.25),
               f.rz(hz),
               1.0F,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               1.0F,
               (1.1F + 0.3F * Mth.sin(t * 0.8F)) * sc,
               0.35F * sc,
               (0.25F + 0.45F * p) * (this.full ? 1.2F : 1.0F),
               t * 0.1F
            );
      } else {
         if (this.launched >= 0 && this.trailFade > 0.01F && !this.firstPersonCaster()) {
            if (this.feetTrail != null) {
               this.feetTrail.render(f, hx, hy + 0.2 * (double)sc, hz, this.trailFade);
            }

            Entity e = this.caster();
            float yaw = e != null ? e.getYRot() * (float) (Math.PI / 180.0) : 0.0F;
            double rx = (double)(-Mth.cos(yaw));
            double rz = (double)(-Mth.sin(yaw));
            if (this.leftTrail != null) {
               this.leftTrail.render(f, hx - rx * 0.55 * (double)sc, hy + (double)this.height * 0.72, hz - rz * 0.55 * (double)sc, this.trailFade * 0.8F);
            }

            if (this.rightTrail != null) {
               this.rightTrail.render(f, hx + rx * 0.55 * (double)sc, hy + (double)this.height * 0.72, hz + rz * 0.55 * (double)sc, this.trailFade * 0.8F);
            }
         }
      }
   }

   @Override
   public void removed() {
      this.endSwell();

      for (Ribbon r : new Ribbon[]{this.feetTrail, this.leftTrail, this.rightTrail}) {
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
