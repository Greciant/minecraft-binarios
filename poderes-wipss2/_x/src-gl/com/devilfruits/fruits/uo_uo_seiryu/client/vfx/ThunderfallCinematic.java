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
import com.devilfruits.client.vfx.fx.BoltStrike;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.Ribbon;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuSounds;
import com.devilfruits.registry.DFSounds;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ThunderfallCinematic extends HybridCinematic {
   private static final int STORM_GRADE = 10128584;
   private static final int TAG_STORM = 32545;
   private static final int AFTER = 60;
   private static final int STREAKS = 6;
   private final float range;
   private final int maxTicks;
   private int strikes;
   private boolean released;
   private int releasedAt = -1;
   private int burstAge = -1;
   private final double cx;
   private final double cz;
   @Nullable
   private final FloorDebrisEffect whirl;
   private final Ribbon[] streaks = new Ribbon[6];
   private final double[] chunk = new double[3];
   private static final float[] SX = new float[9];
   private static final float[] SY = new float[9];
   private static final float[] SZ = new float[9];
   private static final float[] SW = new float[9];
   private static final float[] SH = new float[9];
   private static final float[] SA = new float[9];

   public ThunderfallCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.range = Math.max(3.0F, params.power());
      this.maxTicks = Math.max(20, params.extra());
      this.cx = this.x;
      this.cz = this.z;
      Vec3 feet = new Vec3(this.x, this.onFloor(3.0) ? this.groundY : this.y, this.z);
      this.whirl = this.firstPersonCaster()
         ? null
         : VfxManager.add(new FloorDebrisEffect(level, feet, 18, 0.12F, 0.35F, 110, this.seed).orbit(this.range * 0.42F, 0.32F));

      for (int i = 0; i < 6; i++) {
         this.streaks[i] = Ribbon.create(12);
         if (this.streaks[i] != null) {
            this.streaks[i].look(0.08F, 8.0F, -1.0F, -1.0F, 0.5F, 0).texture(MeshTex.FLAME_RIBBON, 0.3F).tint(15789304);
         }
      }
   }

   public boolean released() {
      return this.released;
   }

   public void mark(Vec3 at, int lead) {
      VfxParticles.emit(VfxPresets.TELEGRAPH, at.x, at.y + 0.05, at.z, 0.0, 0.0, 0.0).size(4.6F).tint(11955455).life((float)Math.max(1, lead) / 3.0F + 0.34F);
      VfxParticles.emit(VfxPresets.GROUND_GLOW, at.x, at.y + 0.04, at.z, 0.0, 0.0, 0.0)
         .size(3.2F)
         .tint(8011007)
         .alpha(0.7F)
         .life((float)Math.max(1, lead) / 3.0F + 0.4F);

      for (int i = 0; i < VfxSettings.count(4.0F); i++) {
         VfxParticles.emit(
               VfxPresets.THUNDER_MOTE, at.x + this.spread(0.9), at.y + 0.1, at.z + this.spread(0.9), 0.0, 0.12 + this.random.nextDouble() * 0.1, 0.0
            )
            .life(0.6F);
      }
   }

   public void strike(ClientLevel level, Vec3 at, int number) {
      this.strikes = Math.max(this.strikes, number);
      Vec3 top = at.add(this.spread(2.0), 22.0 + this.random.nextDouble() * 6.0, this.spread(2.0));
      BoltStrike.between(top, at, this.seed + (long)number * 131L).width(0.42F).life(8).forks(4).rough(0.1F).heat(1.2F).endGlow(1.5F).spawn();
      double a = this.random.nextDouble() * (float) (Math.PI * 2);
      Vec3 ring = new Vec3(this.cx + Math.cos(a) * (double)this.range * 0.65, at.y, this.cz + Math.sin(a) * (double)this.range * 0.65);
      double gy = VfxWorld.ground(level, ring.x, ring.y + 2.0, ring.z, 6);
      if (!Double.isNaN(gy)) {
         BoltStrike.between(ring.add(this.spread(2.0), 20.0, this.spread(2.0)), new Vec3(ring.x, gy, ring.z), this.seed ^ (long)number * 977L)
            .width(0.14F)
            .life(5)
            .forks(2)
            .spawn();
      }

      Vec3 mid = at.add(0.0, 0.7, 0.0);
      VfxParticles.emit(VfxPresets.THUNDER_FLARE, mid.x, mid.y, mid.z, 0.0, 0.0, 0.0).size(0.65F).rotation(this.random.nextFloat() * 6.28F);
      VfxParticles.emit(VfxPresets.GLOW, mid.x, mid.y, mid.z, 0.0, 0.0, 0.0).size(4.5F).tint(15984895).alpha(0.75F).life(0.8F);

      for (int i = 0; i < VfxSettings.count(14.0F); i++) {
         VfxParticles.emit(VfxPresets.THUNDER_SPARK, mid.x, mid.y, mid.z, this.spread(0.5), 0.1 + this.random.nextDouble() * 0.45, this.spread(0.5));
      }

      for (int i = 0; i < VfxSettings.count(4.0F); i++) {
         VfxParticles.emit(VfxPresets.SMOKE, at.x + this.spread(0.8), at.y + 0.4, at.z + this.spread(0.8), this.spread(0.04), 0.05, this.spread(0.04))
            .tint(this.shade(1708080))
            .size(0.9F)
            .life(0.6F);
      }

      GroundWave.at(at.x, at.y, at.z, 3.4F, 9)
         .strength(1.1F)
         .ring(15984895, 0.8F)
         .dust(10, this.shade(VfxWorld.groundColour(level, at.x, at.y - 0.5, at.z)), 0.7F, 0.3F, 0.5F, this.seed ^ (long)number)
         .spawn();
      if (!this.firstPersonCaster()) {
         VfxManager.add(new FloorDebrisEffect(level, at, 6, 0.14F, 0.55F, 90, this.seed ^ (long)number * 31L));
      }

      float rot = this.random.nextFloat() * (float) (Math.PI * 2);
      VfxParticles.emit(VfxPresets.SCORCH, at.x, at.y + 0.03, at.z, 0.0, 0.0, 0.0).size(2.8F).rotation(rot).life(2.2F);
      VfxParticles.emit(VfxPresets.CRACKS_VIOLET, at.x, at.y + 0.035, at.z, 0.0, 0.0, 0.0).size(2.8F).rotation(rot).life(1.2F);
      if (number % 3 == 1 && this.onFloor(3.0)) {
         float size = Math.min(9.0F, 3.4F + (float)number * 0.32F);
         VfxParticles.emit(VfxPresets.SCORCH, this.x, this.groundY + 0.025, this.z, 0.0, 0.0, 0.0)
            .size(size)
            .rotation(this.random.nextFloat() * 6.28F)
            .life(3.0F)
            .alpha(0.7F);
      }

      float ex = VfxWorld.exposure(this.caster(), at, 8.0, 44.0);
      if (ex > 0.0F) {
         CameraDirector.shake(0.32F * ex, 0.12F, 2.0F, 3.4F);
         VfxPost.flash(11955455, 0.1F * ex);
      }

      VeilAuraCinematic.pulseOf(this.caster());
      SoundDesign.play(
         at,
         SeiryuSounds.THUNDERFALL_STRIKE,
         2.0F,
         0.85F + this.random.nextFloat() * 0.3F,
         true,
         SoundDesign.layer(DFSounds.LAYER_THUNDER, 1.2F, 1.0F),
         SoundDesign.layer(DFSounds.LAYER_BOOM, 0.5F, 1.2F)
      );
   }

   public void end(ClientLevel level, boolean burst) {
      if (!this.released) {
         this.released = true;
         this.releasedAt = this.age;
         if (this.whirl != null) {
            this.whirl.drop();
         }

         if (burst) {
            this.burstAge = 0;
            double cy = this.y + (double)this.height * 0.5;
            Vec3 center = new Vec3(this.x, cy, this.z);
            VfxParticles.emit(VfxPresets.THUNDER_FLARE, this.x, cy, this.z, 0.0, 0.0, 0.0).size(1.5F).rotation(this.random.nextFloat() * 6.28F);
            VfxParticles.emit(VfxPresets.THUNDER_FLARE, this.x, cy, this.z, 0.0, 0.0, 0.0).size(1.0F).heat(1.15F).rotation(this.random.nextFloat() * 6.28F);
            VfxParticles.emit(VfxPresets.GLOW, this.x, cy, this.z, 0.0, 0.0, 0.0).size(this.range * 0.9F).tint(11955455).alpha(0.7F).life(1.6F);

            for (int i = 0; i < VfxSettings.count(56.0F); i++) {
               double a = this.random.nextDouble() * (float) (Math.PI * 2);
               double b = (this.random.nextDouble() - 0.35) * 1.2;
               double sp = 0.9 + this.random.nextDouble() * 0.8;
               VfxParticles.emit(
                     VfxPresets.THUNDER_SHARD,
                     this.x,
                     cy + this.spread(0.6),
                     this.z,
                     Math.cos(a) * Math.cos(b) * sp,
                     Math.sin(b) * sp,
                     Math.sin(a) * Math.cos(b) * sp
                  )
                  .size(1.4F)
                  .heat(1.15F);
            }

            for (int i = 0; i < 6; i++) {
               double a = (double)((float)i * (float) (Math.PI * 2) / 6.0F) + this.random.nextDouble() * 0.5;
               Vec3 from = center.add(Math.cos(a) * 0.6, this.spread(0.6), Math.sin(a) * 0.6);
               double len = 2.5 + this.random.nextDouble() * 2.0;
               BoltStrike.between(from, from.add(Math.cos(a) * len, this.spread(1.2), Math.sin(a) * len), this.seed + (long)i * 7L)
                  .width(0.12F)
                  .life(6)
                  .forks(2)
                  .spawn();
            }

            if (this.onFloor(3.0)) {
               GroundWave.at(this.x, this.groundY, this.z, this.range * 1.0F, 16).strength(1.4F).ring(11955455, 0.5F).air(cy, this.range * 0.8F).spawn();
            }

            float ex = this.exposure();
            if (ex > 0.0F) {
               float lens = this.firstPersonCaster() ? 0.35F : 1.0F;
               CameraDirector.shake(0.75F * ex, 0.07F, 4.0F, 3.0F);
               CameraDirector.fovPunch(0.1F * ex);
               VfxPost.flash(11955455, 0.18F * ex * lens);
               VfxPost.chromatic(0.9F * ex * lens);
               VfxPost.radialBlur(center, 0.2F * ex * lens);
            }

            SoundDesign.play(
               center,
               SeiryuSounds.THUNDERFALL_END,
               1.8F,
               1.0F,
               SoundDesign.layer(DFSounds.LAYER_BOOM, 1.0F, 0.8F),
               SoundDesign.layer(DFSounds.LAYER_SONIC, 0.5F, 1.2F)
            );
         }
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.burstAge >= 0) {
         this.burstAge++;
      }

      float storm = this.released ? Math.max(0.0F, 1.0F - (float)(this.age - this.releasedAt) / 60.0F) : Math.min(1.0F, (float)this.age / 10.0F);
      if ((!this.released || this.age - this.releasedAt <= 60) && this.age <= this.maxTicks + 80) {
         double cy = this.y + (double)this.height * 0.5;
         if (!this.released) {
            for (int i = 0; i < VfxSettings.count(2.5F + (float)this.strikes * 0.2F); i++) {
               double a = this.random.nextDouble() * (float) (Math.PI * 2);
               double d = (double)this.height * (0.2 + this.random.nextDouble() * 0.5);
               VfxParticles.emit(
                  VfxPresets.THUNDER_MOTE,
                  this.x + Math.cos(a) * d,
                  this.y + this.random.nextDouble() * (double)this.height,
                  this.z + Math.sin(a) * d,
                  this.spread(0.03),
                  0.09 + this.random.nextDouble() * 0.1,
                  this.spread(0.03)
               );
            }

            if (this.firstPersonCaster()) {
               Minecraft mc = Minecraft.getInstance();
               Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
               Vec3 look = new Vec3(mc.gameRenderer.getMainCamera().getLookVector());

               for (int i = 0; i < VfxSettings.count(1.5F); i++) {
                  Vec3 p = cam.add(look.scale(3.0 + this.random.nextDouble() * 2.0)).add(this.spread(2.5), this.spread(1.5), this.spread(2.5));
                  VfxParticles.emit(VfxPresets.THUNDER_SPARK, p.x, p.y, p.z, this.spread(0.6), this.spread(0.3), this.spread(0.6)).life(0.6F);
               }
            }

            float ex = this.exposure();
            if (ex > 0.0F) {
               VfxPost.grade(10128584, 0.38F * storm * ex);
               VfxPost.vignette(0.35F * storm * ex);
               if (this.age % 3 == 0) {
                  CameraDirector.shake(32545, Math.min(0.62F, 0.16F + 0.035F * (float)this.strikes) * ex, 0.05F, 1.6F, 2.8F);
               }
            }
         } else {
            float ex = this.exposure();
            if (ex > 0.0F) {
               VfxPost.grade(10128584, 0.3F * storm * ex);
            }

            if (this.onFloor(3.0)) {
               VfxParticles.emit(VfxPresets.GROUND_GLOW, this.x, this.groundY + 0.04, this.z, 0.0, 0.0, 0.0)
                  .size(this.range * 1.2F)
                  .tint(8011007)
                  .alpha(0.5F * storm);
            }
         }

         if (this.whirl != null && this.whirl.orbiting()) {
            float t = VfxClock.time();

            for (int i = 0; i < 6 && i * 3 < this.whirl.count(); i++) {
               if (this.streaks[i] != null) {
                  this.whirl.chunk(i * 3, this.chunk);
                  this.streaks[i].push(this.chunk[0], this.chunk[1], this.chunk[2], t);
               }
            }
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      if (this.whirl != null) {
         float fade = this.released ? Math.max(0.0F, 1.0F - ((float)(this.age - this.releasedAt) + f.partial) / 6.0F) : Math.min(1.0F, (float)this.age / 8.0F);

         for (int i = 0; i < 6 && i * 3 < this.whirl.count(); i++) {
            if (this.streaks[i] != null && fade > 0.01F) {
               this.whirl.chunk(i * 3, this.chunk);
               this.streaks[i].render(f, this.chunk[0], this.chunk[1], this.chunk[2], fade * 0.8F);
            }
         }
      }

      if (this.burstAge >= 0 && this.burstAge < 9 && f.allowMesh()) {
         float p = ((float)this.burstAge + f.partial) / 9.0F;
         float len = this.range * (0.8F + 0.7F * p);
         float a = 1.0F - p * p;
         float bx = f.rx(this.x);
         float by = f.ry(this.y + (double)this.height * 0.5);
         float bz = f.rz(this.z);
         int n = 9;
         float lx = f.left.x;
         float ly = 0.0F;
         float lz = f.left.z;
         float ll = Mth.sqrt(lx * lx + lz * lz) + 1.0E-4F;
         lx /= ll;
         lz /= ll;

         for (int k = 0; k < n; k++) {
            float s = (float)k / (float)(n - 1) * 2.0F - 1.0F;
            SX[k] = bx + lx * s * len;
            SY[k] = by + ly;
            SZ[k] = bz + lz * s * len;
            SW[k] = 0.32F * (1.0F - Math.abs(s) * 0.85F);
            SH[k] = 1.25F * (1.0F - Math.abs(s) * 0.4F);
            SA[k] = a * (1.0F - Math.abs(s) * 0.6F) * VfxWorld.nearFade(SX[k], SY[k], SZ[k]);
         }

         MeshWriter.strip(f.mesh(MeshTex.LANCE), n, SX, SY, SZ, SW, SH, SA, 0.0F, 0.4F, 16777215, 1, 4.0F);
      }
   }

   @Override
   public void removed() {
      for (Ribbon r : this.streaks) {
         if (r != null) {
            r.release();
         }
      }

      if (this.whirl != null && !this.released) {
         this.whirl.drop();
      }
   }

   @Override
   public double cullDistance() {
      return 128.0;
   }
}
