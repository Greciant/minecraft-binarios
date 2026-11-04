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
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuSounds;
import com.devilfruits.registry.DFSounds;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class AwakeningCinematic extends HybridCinematic {
   private static final int STRIKE = 10;
   private static final int AFTER = 60;
   private static final int GRADE = 10128584;
   private static final int TAG_STATIC = 32305;
   private static final int SOOT = 1380890;
   private static final int SOOT_LIT = 3352650;
   private static final int GRIT = 1841696;
   private final boolean death;
   private final Vec3 top;

   public AwakeningCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.death = params.extra() == 1;
      this.top = new Vec3(this.x + this.spread(2.5), this.y + 30.0, this.z + this.spread(2.5));
      SoundDesign.play(new Vec3(this.x, this.y + 1.0, this.z), DFSounds.LAYER_THUNDER, 0.7F, 0.55F, SoundDesign.layer(DFSounds.LAYER_RUMBLE, 0.9F, 0.7F));
   }

   private double floor() {
      return this.onFloor(3.0) ? this.groundY : this.y;
   }

   private Vec3 struck() {
      if (this.firstPersonCaster()) {
         Vector3f look = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
         double l = Math.sqrt((double)(look.x * look.x + look.z * look.z)) + 1.0E-4;
         return new Vec3(this.x + (double)look.x / l * 2.4, this.floor(), this.z + (double)look.z / l * 2.4);
      } else {
         return new Vec3(this.x, this.y + (double)this.height + 0.1, this.z);
      }
   }

   private Vec3 onBody() {
      double a = this.random.nextDouble() * (float) (Math.PI * 2);
      double rr = (double)this.height * 0.18;
      return new Vec3(this.x + Math.cos(a) * rr, this.y + (double)this.height * (0.08 + this.random.nextDouble() * 0.9), this.z + Math.sin(a) * rr);
   }

   private void arc(float width, int life) {
      Vec3 a = this.onBody();
      Vec3 b = a.add(this.spread(0.5), this.spread(0.6), this.spread(0.5));
      BoltStrike.between(a, b, this.random.nextLong()).width(width).life(life).forks(1).rough(0.3F).endGlow(0.25F).spawn();
   }

   private void sparksInView(float count) {
      Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
      Vec3 c = cam.getPosition();
      Vector3f look = cam.getLookVector();

      for (int i = 0; i < VfxSettings.count(count); i++) {
         double d = 2.5 + this.random.nextDouble() * 2.0;
         VfxParticles.emit(
               VfxPresets.THUNDER_SPARK,
               c.x + (double)look.x * d + this.spread(2.2),
               c.y + (double)look.y * d + this.spread(1.4),
               c.z + (double)look.z * d + this.spread(2.2),
               this.spread(0.6),
               this.spread(0.3),
               this.spread(0.6)
            )
            .life(0.6F);
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age < 10) {
         this.gather();
      } else if (this.age == 10) {
         this.strike(level);
      } else {
         this.afterglow(this.age - 10);
      }

      if (this.age == 15) {
         this.restrike(1.0F, 1);
      } else if (this.death && this.age == 21) {
         this.restrike(0.7F, 2);
      }

      if (this.age > 70) {
         this.finish();
      }
   }

   private void gather() {
      float p = (float)this.age / 10.0F;
      boolean fp = this.firstPersonCaster();

      for (int i = 0; i < VfxSettings.count(1.0F + 3.0F * p); i++) {
         Vec3 b = this.onBody();
         VfxParticles.emit(VfxPresets.THUNDER_MOTE, b.x, b.y, b.z, this.spread(0.03), 0.06 + this.random.nextDouble() * 0.08, this.spread(0.03));
      }

      if (this.age >= 3) {
         if (fp) {
            this.sparksInView(1.0F);
         } else {
            this.arc(0.05F, 2);
            if (this.age >= 6) {
               this.arc(0.04F, 2);
            }

            VfxParticles.emit(VfxPresets.GLOW, this.x, this.y + (double)this.height * 0.55, this.z, 0.0, 0.0, 0.0)
               .size(this.height * (0.8F + 1.2F * p))
               .tint(8011007)
               .alpha(0.35F * p)
               .life(0.2F);
         }
      }

      if (this.age == 3 || this.age == 6 || this.age == 8) {
         int k = this.age / 3;
         Vec3 from = this.top.add(this.spread(1.5), 0.0, this.spread(1.5));
         BoltStrike.between(
               from,
               new Vec3(this.x + this.spread(3.0), this.y + (double)this.height + 11.0 - (double)(4 * k), this.z + this.spread(3.0)),
               this.seed + (long)k * 17L
            )
            .width(0.08F)
            .life(2)
            .forks(2)
            .rough(0.16F)
            .endGlow(0.4F)
            .spawn();
         float ex = this.exposure();
         if (ex > 0.0F) {
            VfxPost.flash(11955455, 0.06F * ex);
         }
      }

      if (this.age % 2 == 0) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double d = 0.6 + this.random.nextDouble() * 0.6;
         VfxParticles.emit(
               VfxPresets.SOOT, this.x + Math.cos(a) * d, this.floor() + 0.15, this.z + Math.sin(a) * d, -Math.sin(a) * 0.06, 0.01, Math.cos(a) * 0.06
            )
            .tint(this.shade(1380890))
            .size(0.45F + 0.3F * p)
            .alpha(0.6F)
            .life(0.5F);
      }

      float ex = this.exposure();
      if (ex > 0.0F) {
         VfxPost.grade(10128584, 0.6F * p * ex);
         VfxPost.vignette(0.45F * p * ex);
         if (this.age % 3 == 0) {
            CameraDirector.shake(32305, (0.05F + 0.15F * p) * ex, 0.06F, 1.0F, 3.0F);
         }
      }

      if (this.age == 5) {
         SoundDesign.layerOnly(new Vec3(this.x, this.y + 1.0, this.z), SeiryuSounds.LAYER_CRACKLE, 0.8F, 1.2F);
      }
   }

   private void strike(ClientLevel level) {
      boolean fp = this.firstPersonCaster();
      double gy = this.floor();
      Vec3 end = this.struck();
      Vec3 feet = new Vec3(this.x, gy, this.z);
      Vec3 mid = new Vec3(this.x, this.y + (double)this.height * 0.55, this.z);
      BoltStrike.between(this.top, end, this.seed).width(this.death ? 0.7F : 0.55F).life(10).forks(5).rough(0.1F).heat(1.25F).endGlow(1.8F).spawn();
      if (!fp) {
         BoltStrike.between(end, feet, this.seed ^ 81L).width(0.22F).life(8).forks(2).rough(0.2F).heat(1.3F).endGlow(1.2F).spawn();
      }

      int arcs = this.death ? 8 : 6;
      double base = this.random.nextDouble() * (float) (Math.PI * 2);

      for (int k = 0; k < arcs; k++) {
         double a = base + (double)((float)k * (float) (Math.PI * 2) / (float)arcs) + this.spread(0.3);
         double d = 1.8 + this.random.nextDouble() * 2.4;
         Vec3 to = new Vec3(this.x + Math.cos(a) * d, gy + 0.05, this.z + Math.sin(a) * d);
         BoltStrike.between(feet.add(0.0, 0.05, 0.0), to, this.seed + 7L * (long)k).width(0.09F).life(6).forks(1).rough(0.22F).endGlow(0.5F).spawn();
      }

      for (int k = 0; k < 2; k++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double d = 3.0 + this.random.nextDouble() * 2.5;
         double bx = this.x + Math.cos(a) * d;
         double bz = this.z + Math.sin(a) * d;
         double by = VfxWorld.ground(level, bx, gy + 2.0, bz, 6);
         BoltStrike.between(
               new Vec3(bx + this.spread(2.0), this.y + 26.0, bz + this.spread(2.0)),
               new Vec3(bx, Double.isNaN(by) ? gy : by, bz),
               this.seed ^ (long)(k + 1) * 977L
            )
            .width(0.2F)
            .life(6)
            .forks(2)
            .spawn();
      }

      Vec3 core = fp ? end.add(0.0, 0.5, 0.0) : mid;
      VfxParticles.emit(VfxPresets.THUNDER_FLARE, core.x, core.y, core.z, 0.0, 0.0, 0.0)
         .size(fp ? 0.8F : 1.8F)
         .rotation(this.random.nextFloat() * (float) (Math.PI * 2));
      VfxParticles.emit(VfxPresets.THUNDER_FLARE, core.x, core.y, core.z, 0.0, 0.0, 0.0)
         .size(fp ? 0.5F : 1.1F)
         .heat(1.15F)
         .rotation(this.random.nextFloat() * (float) (Math.PI * 2));
      VfxParticles.emit(VfxPresets.GLOW, core.x, core.y, core.z, 0.0, 0.0, 0.0).size(8.0F).tint(15984895).alpha(0.8F).life(0.8F);
      VfxParticles.emit(VfxPresets.GLOW, core.x, core.y, core.z, 0.0, 0.0, 0.0).size(4.0F).tint(11955455).alpha(0.8F).life(1.4F);

      for (int i = 0; i < VfxSettings.count(this.death ? 52.0F : 40.0F); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double b = (this.random.nextDouble() - 0.3) * 1.2;
         double sp = 0.6 + this.random.nextDouble() * 0.7;
         VfxParticles.emit(
               VfxPresets.THUNDER_SHARD,
               core.x,
               core.y + this.spread(0.5),
               core.z,
               Math.cos(a) * Math.cos(b) * sp,
               Math.sin(b) * sp,
               Math.sin(a) * Math.cos(b) * sp
            )
            .size(1.2F)
            .heat(1.15F);
      }

      for (int i = 0; i < VfxSettings.count(30.0F); i++) {
         VfxParticles.emit(VfxPresets.THUNDER_SPARK, core.x, core.y, core.z, this.spread(0.6), 0.1 + this.random.nextDouble() * 0.5, this.spread(0.6));
      }

      this.blackDust(gy, 1.0F);
      if (this.onFloor(3.0)) {
         int dust = this.shade(VfxWorld.groundColour(level, this.x, gy - 0.5, this.z));
         GroundWave.at(this.x, gy, this.z, this.death ? 9.0F : 7.0F, 14)
            .strength(1.3F)
            .ring(15984895, 0.4F)
            .air(mid.y, 4.5F)
            .dust(12, dust, 0.8F, 0.32F, 0.6F, this.seed)
            .spawn();
         float rot = this.random.nextFloat() * (float) (Math.PI * 2);
         VfxParticles.emit(VfxPresets.SCORCH, this.x, gy + 0.03, this.z, 0.0, 0.0, 0.0).size(this.death ? 4.6F : 3.8F).rotation(rot).life(3.0F);
         VfxParticles.emit(VfxPresets.CRACKS_VIOLET, this.x, gy + 0.035, this.z, 0.0, 0.0, 0.0).size(this.death ? 4.6F : 3.8F).rotation(rot).life(1.6F);
         if (!fp) {
            VfxManager.add(new FloorDebrisEffect(level, feet, this.death ? 9 : 6, 0.18F, 0.42F, 90, this.seed ^ 43L));
         }
      }

      float ex = this.exposure();
      if (ex > 0.0F) {
         float soft = fp ? 0.6F : 1.0F;
         CameraDirector.shake((this.death ? 0.95F : 0.85F) * ex, 0.07F, 4.5F, 3.2F);
         CameraDirector.fovPunch(0.15F * ex);
         VfxPost.chromatic(0.9F * ex * soft);
         VfxPost.radialBlur(core, 0.25F * ex * soft);
         VfxPost.flash(15984895, 0.5F * ex * soft);
         if (ex > 0.5F) {
            VfxClock.hitStop(3);
         }
      }

      SoundDesign.play(
         mid,
         SeiryuSounds.THUNDERFALL_STRIKE,
         2.0F,
         0.8F,
         SoundDesign.layer(SeiryuSounds.AWAKEN, 1.3F, 1.0F),
         SoundDesign.layer(DFSounds.LAYER_THUNDER, 1.4F, 0.85F),
         SoundDesign.layer(DFSounds.LAYER_BOOM, this.death ? 1.4F : 1.0F, 0.75F)
      );
   }

   private void blackDust(double gy, float power) {
      int clouds = Math.max(3, Math.round((float)(this.death ? 9 : 7) * power));
      double base = this.random.nextDouble() * (float) (Math.PI * 2);

      for (int c = 0; c < clouds; c++) {
         double a = base + (double)((float)c * (float) (Math.PI * 2) / (float)clouds) + this.spread(0.3);
         double sp = (0.5 + this.random.nextDouble() * 0.35) * (double)power;
         double ca = Math.cos(a);
         double sa = Math.sin(a);

         for (int i = 0; i < VfxSettings.count(3.0F); i++) {
            double h = this.random.nextDouble();
            double k = 0.75 + this.random.nextDouble() * 0.4;
            VfxParticles.emit(
                  VfxPresets.SOOT,
                  this.x + ca * (0.9 + 0.3 * (double)i),
                  gy + 0.15 + h * 0.9,
                  this.z + sa * (0.9 + 0.3 * (double)i),
                  ca * sp * k + this.spread(0.04),
                  0.015 + 0.04 * this.random.nextDouble(),
                  sa * sp * k + this.spread(0.04)
               )
               .tint(this.shade(i == 0 && c % 3 == 0 ? 3352650 : 1380890))
               .size((0.7F + 0.6F * this.random.nextFloat()) * (0.75F + 0.25F * power))
               .alpha(0.88F)
               .life(0.7F);
         }
      }

      for (int i = 0; i < VfxSettings.count(3.0F * power); i++) {
         VfxParticles.emit(
               VfxPresets.SOOT,
               this.x + this.spread(0.5),
               gy + 0.8 + this.random.nextDouble(),
               this.z + this.spread(0.5),
               this.spread(0.1),
               (0.16 + this.random.nextDouble() * 0.1) * (double)power,
               this.spread(0.1)
            )
            .tint(this.shade(1380890))
            .size(0.9F)
            .alpha(0.7F)
            .life(0.6F);
      }

      for (int i = 0; i < VfxSettings.count(16.0F * power); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double sp = (0.25 + this.random.nextDouble() * 0.3) * (double)power;
         VfxParticles.emit(
               VfxPresets.SAND_GRAINS[i % VfxPresets.SAND_GRAINS.length],
               this.x + this.spread(0.3),
               gy + 0.3 + this.random.nextDouble() * 0.6,
               this.z + this.spread(0.3),
               Math.cos(a) * sp,
               0.06 + this.random.nextDouble() * 0.1,
               Math.sin(a) * sp
            )
            .tint(this.shade(1841696))
            .size(0.6F);
      }
   }

   private void restrike(float power, int number) {
      Vec3 end = this.struck();
      BoltStrike.between(this.top, end, this.seed ^ 40503L * (long)number)
         .width(0.45F * power)
         .life(5)
         .forks(3)
         .rough(0.1F)
         .heat(1.2F)
         .endGlow(1.2F * power)
         .spawn();
      Vec3 core = this.firstPersonCaster() ? end.add(0.0, 0.8, 0.0) : new Vec3(this.x, this.y + (double)this.height * 0.55, this.z);
      VfxParticles.emit(VfxPresets.THUNDER_FLARE, core.x, core.y, core.z, 0.0, 0.0, 0.0)
         .size(1.1F * power)
         .rotation(this.random.nextFloat() * (float) (Math.PI * 2));

      for (int i = 0; i < VfxSettings.count(14.0F * power); i++) {
         VfxParticles.emit(VfxPresets.THUNDER_SPARK, core.x, core.y, core.z, this.spread(0.5), 0.1 + this.random.nextDouble() * 0.4, this.spread(0.5));
      }

      this.blackDust(this.floor(), 0.45F * power);
      float ex = this.exposure();
      if (ex > 0.0F) {
         CameraDirector.shake(0.4F * power * ex, 0.09F, 2.5F, 3.4F);
         VfxPost.flash(11955455, 0.2F * power * ex);
         VfxPost.chromatic(0.4F * power * ex);
      }

      SoundDesign.layerOnly(core, DFSounds.LAYER_THUNDER, 1.0F * power, 1.1F);
   }

   private void afterglow(int s) {
      float fade = Math.max(0.0F, 1.0F - (float)s / 45.0F);
      boolean fp = this.firstPersonCaster();
      if (s < 20 || s < 44 && s % 3 == 0) {
         if (fp) {
            if (s < 20) {
               this.sparksInView(1.2F * fade);
            }
         } else {
            this.arc(0.06F * (1.0F - (float)s / 55.0F), 3);
         }
      }

      for (int i = 0; i < VfxSettings.count(2.5F * fade); i++) {
         Vec3 b = this.onBody();
         VfxParticles.emit(VfxPresets.THUNDER_MOTE, b.x, b.y, b.z, this.spread(0.04), 0.07 + this.random.nextDouble() * 0.1, this.spread(0.04));
      }

      if (this.onFloor(3.0)) {
         VfxParticles.emit(VfxPresets.GROUND_GLOW, this.x, this.groundY + 0.04, this.z, 0.0, 0.0, 0.0).size(3.4F).tint(8011007).alpha(0.6F * fade);
         if (s < 30 && s % 3 == 0) {
            VfxParticles.emit(
                  VfxPresets.SOOT,
                  this.x + this.spread(1.0),
                  this.groundY + 0.15,
                  this.z + this.spread(1.0),
                  this.spread(0.02),
                  0.03 + this.random.nextDouble() * 0.03,
                  this.spread(0.02)
               )
               .tint(this.shade(1380890))
               .size(0.6F)
               .alpha(0.5F)
               .life(0.7F);
         }
      }

      float ex = this.exposure();
      if (ex > 0.0F) {
         VfxPost.grade(10128584, 0.45F * fade * ex);
         VfxPost.vignette(0.3F * fade * ex);
      }
   }

   @Override
   public void render(VfxFrame f) {
   }

   @Override
   public double cullDistance() {
      return 128.0;
   }
}
