package com.devilfruits.fruits.gura_gura.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.vfx.FloorDebrisEffect;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.GlassCrack;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.fruits.gura_gura.GuraSounds;
import com.devilfruits.registry.DFParticles;
import com.devilfruits.registry.DFSounds;
import com.mojang.blaze3d.vertex.BufferBuilder;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class AwakeningCinematic extends QuakeCinematic {
   private static final int GATHER = 7;
   private static final int AFTER = 60;
   private static final int MARKS = 3;
   private static final int TAG_STRAIN = 27169;
   private static final int DRAIN = 14280434;
   private final boolean death;
   private final int burstAt;
   private final float big;
   @Nullable
   private GlassCrack crack;
   private final GlassCrack[] marks = new GlassCrack[3];
   private final double[] markOff = new double[9];
   private final List<AwakeningCinematic.Pane> panes = new ArrayList<>();
   private int burst = -1;
   private final ShockRing.Band band = new ShockRing.Band();
   private double cx;
   private double cy;
   private double cz;
   private float r;
   private final MutableBlockPos pos = new MutableBlockPos();

   public AwakeningCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.death = params.extra() == 1;
      this.burstAt = this.death ? 32 : 24;
      this.big = (this.death ? 1.75F : 1.4F) * this.height / 1.8F;
      this.centre(1.0F);
      this.r = this.radius(0.0F);
      if (this.onFloor()) {
         GroundWave.at(this.x, this.groundY, this.z, 2.8F, 10).strength(0.7F).spawn();
      }

      SoundDesign.play(
         new Vec3(this.cx, this.cy, this.cz), GuraSounds.KAISHIN_CHARGE, 1.2F, this.death ? 0.5F : 0.65F, SoundDesign.layer(DFSounds.LAYER_RUMBLE, 1.1F, 0.6F)
      );
   }

   private boolean onFloor() {
      return !Double.isNaN(this.groundY) && this.y - this.groundY < 2.5;
   }

   private void centre(float partial) {
      Entity e = this.caster();
      if (e != null && this.firstPersonCaster()) {
         Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
         Vec3 c = cam.getPosition();
         Vector3f look = cam.getLookVector();
         this.cx = c.x + (double)look.x * 1.6;
         this.cy = c.y + (double)look.y * 1.6;
         this.cz = c.z + (double)look.z * 1.6;
      } else if (e != null) {
         this.cx = Mth.lerp((double)partial, e.xo, e.getX());
         this.cy = Mth.lerp((double)partial, e.yo, e.getY()) + (double)e.getBbHeight() * 0.52;
         this.cz = Mth.lerp((double)partial, e.zo, e.getZ());
      } else {
         this.cx = this.x;
         this.cy = this.y + (double)this.height * 0.52;
         this.cz = this.z;
      }
   }

   private float strain(float t) {
      return Mth.clamp((t - 7.0F) / (float)(this.burstAt - 7), 0.0F, 1.0F);
   }

   private float radius(float t) {
      float s = this.big * (this.firstPersonCaster() ? 0.8F : 1.0F);
      if (t < 7.0F) {
         float k = t / 7.0F - 1.0F;
         return s * (0.25F + 0.75F * (1.0F + 2.70158F * k * k * k + 1.70158F * k * k));
      } else {
         float p = this.strain(t);
         return s * (1.0F + 0.04F * p + (0.012F + 0.035F * p) * Mth.sin(t * 5.3F) * Mth.sin(t * 2.1F + 1.0F));
      }
   }

   private int markAt(int k) {
      return 10 + k * (this.burstAt - 7 - 4) / 3;
   }

   private Vec3 randomUnit() {
      double a = this.random.nextDouble() * (float) (Math.PI * 2);
      double h = this.random.nextDouble() * 2.0 - 1.0;
      double c = Math.sqrt(1.0 - h * h);
      return new Vec3(Math.cos(a) * c, h, Math.sin(a) * c);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.centre(1.0F);
      if (this.burst < 0) {
         this.r = this.radius((float)this.age);
         if (this.age == 7) {
            this.crack = GlassCrack.at(new Vec3(this.cx, this.cy, this.cz), new Vec3(0.0, 0.0, 1.0), this.r, this.seed)
               .billboard()
               .pattern(this.death ? 16 : 14, 4)
               .grow(this.burstAt - 7 - 3)
               .hold(600)
               .refraction(0.8F)
               .lines(0.016F)
               .sheen(1.5F)
               .tint(15398655)
               .burst(3.4F)
               .spawn();
            SoundDesign.layerOnly(new Vec3(this.cx, this.cy, this.cz), GuraSounds.LAYER_CRACK, 1.0F, 0.6F);
         }

         for (int k = 0; k < 3; k++) {
            if (this.age == this.markAt(k)) {
               this.mark(k);
            }
         }

         this.follow();
         this.pressure(level);
         if (this.age >= this.burstAt) {
            this.burst(level);
         }
      } else {
         int s = this.age - this.burst;

         for (int i = this.panes.size() - 1; i >= 0; i--) {
            AwakeningCinematic.Pane p = this.panes.get(i);
            if (s >= p.due()) {
               this.pane(p, i);
               this.panes.remove(i);
            }
         }

         if (s < 26 && s % 6 == 3 && !this.firstPersonCaster()) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            Vec3 d = new Vec3(Math.cos(a), this.spread(0.3), Math.sin(a)).normalize();
            ShockRing.at(
                  new Vec3(
                     this.x + d.x * (double)this.big * 0.75,
                     this.y + (double)this.height * (0.3 + this.random.nextDouble() * 0.5),
                     this.z + d.z * (double)this.big * 0.75
                  ),
                  d
               )
               .radius(0.15F, 0.8F * this.big)
               .life(9)
               .strength(1.1F * (1.0F - (float)s / 26.0F))
               .band(0.5F)
               .twist((this.random.nextFloat() - 0.5F) * 2.0F)
               .rim(0.0F, 15398655)
               .spawn();
         }

         if (s > 60) {
            this.finish();
         }
      }
   }

   private void follow() {
      if (this.crack != null) {
         this.crack.moveTo(this.cx, this.cy, this.cz);
         this.crack.radius(this.r);
      }

      for (int k = 0; k < 3; k++) {
         if (this.marks[k] != null) {
            this.marks[k]
               .moveTo(
                  this.cx + this.markOff[k * 3] * (double)this.r,
                  this.cy + this.markOff[k * 3 + 1] * (double)this.r,
                  this.cz + this.markOff[k * 3 + 2] * (double)this.r
               );
         }
      }
   }

   private void mark(int k) {
      Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
      Vector3f look = cam.getLookVector();
      Vector3f left = cam.getLeftVector();
      Vector3f up = cam.getUpVector();
      Vec3 n;
      if (this.firstPersonCaster()) {
         double a = this.spread(0.6);
         double b = this.spread(0.42);
         this.markOff[k * 3] = (double)left.x * a + (double)up.x * b;
         this.markOff[k * 3 + 1] = (double)left.y * a + (double)up.y * b;
         this.markOff[k * 3 + 2] = (double)left.z * a + (double)up.z * b;
         n = new Vec3((double)(-look.x), (double)(-look.y), (double)(-look.z));
      } else {
         Vec3 to = cam.getPosition().subtract(this.cx, this.cy, this.cz).normalize();
         n = to.scale(0.85).add(this.spread(0.75), this.spread(0.6), this.spread(0.75)).normalize();
         this.markOff[k * 3] = n.x * 0.93;
         this.markOff[k * 3 + 1] = n.y * 0.93;
         this.markOff[k * 3 + 2] = n.z * 0.93;
      }

      Vec3 at = new Vec3(
         this.cx + this.markOff[k * 3] * (double)this.r, this.cy + this.markOff[k * 3 + 1] * (double)this.r, this.cz + this.markOff[k * 3 + 2] * (double)this.r
      );
      this.marks[k] = GlassCrack.at(at, n, this.r * (0.42F + 0.1F * (float)k), this.seed + 31L * (long)(k + 1))
         .pattern(8 + k, 2)
         .grow(2)
         .hold(600)
         .refraction(0.7F)
         .lines(0.026F)
         .sheen(1.3F)
         .tint(15398655)
         .spawn();

      for (int i = 0; i < VfxSettings.count(6.0F); i++) {
         VfxParticles.emit(
               VfxPresets.GLASS_GLINT, at.x, at.y, at.z, n.x * 0.15 + this.spread(0.12), n.y * 0.15 + this.spread(0.12) + 0.05, n.z * 0.15 + this.spread(0.12)
            )
            .tint(14217983);
      }

      float ex = this.exposure(at);
      if (ex > 0.0F) {
         VfxPost.chromatic(0.22F * ex);
         CameraDirector.shake(0.2F * ex, 0.12F, 1.6F, 4.0F);
      }

      SoundDesign.layerOnly(at, GuraSounds.LAYER_CRACK, 0.9F, 0.8F + 0.18F * (float)k);
   }

   private void pressure(ClientLevel level) {
      float p = this.strain((float)this.age);

      for (int i = 0; i < VfxSettings.count(1.5F + 3.0F * p); i++) {
         Vec3 d = this.randomUnit();
         double from = (double)this.r * (1.8 + this.random.nextDouble());
         VfxParticles.emit(
               i % 2 == 0 ? VfxPresets.GLASS_DUST : VfxPresets.GLASS_GLINT,
               this.cx + d.x * from,
               this.cy + d.y * from,
               this.cz + d.z * from,
               -d.x * from * 0.08,
               -d.y * from * 0.08,
               -d.z * from * 0.08
            )
            .tint(14217983)
            .life(0.5F);
      }

      if (this.onFloor() && this.age % 2 == 0) {
         for (int i = 0; i < Fx.count(1 + Math.round(3.0F * p)); i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double d = (double)this.big * (0.8 + this.random.nextDouble() * 1.6);
            double px = this.x + Math.cos(a) * d;
            double pz = this.z + Math.sin(a) * d;
            BlockState ground = Fx.groundAt(level, px, this.groundY - 0.5, pz, this.pos);
            if (ground != null) {
               Fx.spawn(
                  new BlockParticleOption(DFParticles.BLOCK_DEBRIS.get(), ground),
                  px,
                  (double)this.pos.getY() + 1.02,
                  pz,
                  this.spread(0.03),
                  0.08 + this.random.nextDouble() * 0.12 * (0.4 + (double)p),
                  this.spread(0.03)
               );
            }
         }

         int dust = QuakeCinematic.dust(level, this.x, this.groundY - 0.5, this.z);

         for (int ix = 0; ix < VfxSettings.count(1.0F + 2.0F * p); ix++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double d = (double)this.big * (1.0 + this.random.nextDouble() * 0.8);
            VfxParticles.emit(
                  VfxPresets.DUST,
                  this.x + Math.cos(a) * d,
                  this.groundY + 0.15,
                  this.z + Math.sin(a) * d,
                  Math.cos(a) * 0.03,
                  0.015 + 0.02 * (double)p,
                  Math.sin(a) * 0.03
               )
               .tint(dust)
               .size(0.7F + 0.5F * p)
               .alpha(0.55F);
         }
      }

      if (this.age > 7 && this.age % 5 == 0 && !this.firstPersonCaster()) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         Vec3 d = new Vec3(Math.cos(a), this.spread(0.5), Math.sin(a)).normalize();
         ShockRing.at(new Vec3(this.cx + d.x * (double)this.r, this.cy + d.y * (double)this.r, this.cz + d.z * (double)this.r), d)
            .radius(0.1F, 0.6F * this.big)
            .life(7)
            .strength(1.1F)
            .band(0.5F)
            .twist((this.random.nextFloat() - 0.5F) * 2.0F)
            .rim(0.0F, 15398655)
            .spawn();
      }

      float ex = this.exposure(new Vec3(this.cx, this.cy, this.cz));
      if (ex > 0.0F) {
         if (this.age % 3 == 0) {
            CameraDirector.shake(27169, (0.06F + 0.3F * p) * ex, 0.05F, 0.8F + 2.2F * p, 3.5F);
         }

         VfxPost.chromatic((0.03F + 0.22F * p * p) * ex);
         VfxPost.vignette(0.3F * p * ex);
         VfxPost.grade(14280434, 0.18F * p * ex);
      }
   }

   private void burst(ClientLevel level) {
      this.burst = this.age;
      boolean fp = this.firstPersonCaster();
      Vec3 c = new Vec3(this.cx, this.cy, this.cz);
      Vector3f lv = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
      Vec3 look = new Vec3((double)lv.x, (double)lv.y, (double)lv.z);
      if (this.crack != null) {
         this.crack.shatterNow(fp ? look.scale(0.08) : new Vec3(0.0, 0.03, 0.0));
      }

      for (int k = 0; k < 3; k++) {
         if (this.marks[k] != null) {
            Vec3 n = new Vec3(this.markOff[k * 3], this.markOff[k * 3 + 1], this.markOff[k * 3 + 2]);
            this.marks[k].shatterNow(fp ? look.scale(0.1) : n.normalize().scale(0.32));
         }
      }

      VfxParticles.emit(VfxPresets.GLOW, this.cx, this.cy, this.cz, 0.0, 0.0, 0.0).size(this.big * 4.5F).tint(15398655).alpha(0.75F).life(0.7F);
      VfxParticles.emit(VfxPresets.GLOW, this.cx, this.cy, this.cz, 0.0, 0.0, 0.0).size(this.big * 1.6F).tint(16777215).alpha(0.9F).life(0.35F);
      Vec3 body = new Vec3(this.x, this.y + (double)this.height * 0.5, this.z);
      float reach = this.death ? 11.0F : 9.0F;
      ShockRing.at(body, new Vec3(0.0, 1.0, 0.0)).radius(this.big, reach).life(13).strength(1.5F).band(0.3F).rim(0.22F, 15398655).spawn();
      ShockRing.at(fp ? c : body, look).radius(this.big * 0.9F, fp ? 4.0F : 7.0F).life(10).strength(fp ? 0.9F : 1.6F).band(0.4F).rim(0.2F, 15398655).spawn();
      double tilt = this.random.nextDouble() * (float) (Math.PI * 2);

      for (int kx = 0; kx < 2; kx++) {
         double a = tilt + (double)kx * Math.PI * 0.5;
         Vec3 n = new Vec3(Math.cos(a), kx == 0 ? 0.9 : -0.9, Math.sin(a)).normalize();
         ShockRing.at(body, n).radius(this.big, 6.0F).life(11).strength(1.1F).band(0.35F).twist(kx == 0 ? 0.6F : -0.6F).rim(0.12F, 15398655).spawn();
      }

      for (int i = 0; i < VfxSettings.count(this.death ? 56.0F : 40.0F); i++) {
         Vec3 d = this.randomUnit();
         double sp = 0.45 + this.random.nextDouble() * 0.5;
         VfxParticles.emit(
               i % 3 == 0 ? VfxPresets.GLASS_DUST : VfxPresets.GLASS_GLINT,
               this.cx + d.x * (double)this.r,
               this.cy + d.y * (double)this.r,
               this.cz + d.z * (double)this.r,
               d.x * sp,
               d.y * sp * 0.7 + 0.1,
               d.z * sp
            )
            .tint(14217983)
            .size(1.2F);
      }

      if (this.onFloor()) {
         int dust = QuakeCinematic.dust(level, this.x, this.groundY - 0.5, this.z);
         GroundWave.at(this.x, this.groundY, this.z, reach, 16)
            .strength(1.5F)
            .ring(15398655, 0.5F)
            .air(body.y, 5.5F)
            .dust(18, dust, 0.9F, 0.42F, 0.75F, this.seed ^ 29L)
            .spawn();
         GlassCrack.at(new Vec3(this.x, this.groundY + 0.03, this.z), new Vec3(0.0, 1.0, 0.0), this.death ? 4.4F : 3.6F, this.seed ^ 119L)
            .pattern(16, 4)
            .grow(3)
            .hold(38)
            .fade(20)
            .refraction(0.9F)
            .lines(0.014F)
            .tint(15398655)
            .spawn();
         if (!fp) {
            VfxManager.add(new FloorDebrisEffect(level, new Vec3(this.x, this.groundY, this.z), this.death ? 10 : 7, 0.16F, 0.36F, 80, this.seed ^ 47L));
         }
      }

      int n = this.death ? 7 : 5;
      double base = this.random.nextDouble() * (float) (Math.PI * 2);
      Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

      for (int kx = 0; kx < n; kx++) {
         double a = base + (double)((float)kx * (float) (Math.PI * 2) / (float)n) + this.spread(0.35);
         double d = (double)this.big * (1.7 + this.random.nextDouble() * 0.9);
         Vec3 at = new Vec3(this.x + Math.cos(a) * d, this.y + (double)this.height * (0.3 + this.random.nextDouble() * 0.9), this.z + Math.sin(a) * d);
         if (!(at.distanceTo(eye) < 2.6)) {
            Vec3 dir = new Vec3(Math.cos(a), this.spread(0.25), Math.sin(a)).scale(0.45).add(eye.subtract(at).normalize()).normalize();
            this.panes
               .add(new AwakeningCinematic.Pane(at, dir, (float)(0.35 + this.random.nextDouble() * 0.3) * this.big, (int)Math.round(d / 1.1), kx % 2 == 0));
         }
      }

      if (fp) {
         Vec3 ahead = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().add(look.scale(3.4));
         this.panes.add(new AwakeningCinematic.Pane(ahead, look.scale(-1.0), 0.9F, 2, false));
      }

      float ex = this.exposure(c);
      if (ex > 0.0F) {
         float soft = fp ? 0.6F : 1.0F;
         CameraDirector.shake((this.death ? 0.9F : 0.75F) * ex, 0.06F, 4.5F, 3.2F);
         CameraDirector.fovPunch(0.14F * ex);
         VfxPost.chromatic(ex * soft);
         VfxPost.radialBlur(c, 0.28F * ex * soft);
         VfxPost.flash(15398655, 0.3F * ex * soft);
         if (ex > 0.5F) {
            VfxClock.hitStop(3);
         }
      }

      SoundDesign.play(
         c,
         DFSounds.FRUIT_AWAKEN,
         1.4F,
         this.death ? 0.6F : 0.85F,
         SoundDesign.layer(GuraSounds.LAYER_SHATTER, 1.3F, 0.6F),
         SoundDesign.layer(GuraSounds.LAYER_SHATTER_HIGH, 0.8F, 0.9F),
         SoundDesign.layer(DFSounds.LAYER_THUNDER, 0.6F, 0.9F),
         SoundDesign.layer(DFSounds.LAYER_BOOM, this.death ? 1.5F : 1.0F, 0.65F)
      );
   }

   private void pane(AwakeningCinematic.Pane p, int k) {
      GlassCrack g = GlassCrack.at(p.at(), p.normal(), p.radius(), this.seed + 101L * (long)(k + 1))
         .pattern(9 + k % 4, 3)
         .grow(2)
         .hold(p.breaks() ? 10 + this.random.nextInt(8) : 18 + this.random.nextInt(12))
         .fade(14)
         .refraction(0.85F)
         .lines(0.014F)
         .sheen(1.2F)
         .alpha(0.8F)
         .tint(15398655);
      if (p.breaks()) {
         g.shatter(p.normal().scale(0.1));
      }

      g.spawn();

      for (int i = 0; i < VfxSettings.count(4.0F); i++) {
         VfxParticles.emit(
               VfxPresets.GLASS_GLINT,
               p.at().x + this.spread(0.3),
               p.at().y + this.spread(0.3),
               p.at().z + this.spread(0.3),
               p.normal().x * 0.12 + this.spread(0.08),
               0.05 + this.spread(0.08),
               p.normal().z * 0.12 + this.spread(0.08)
            )
            .tint(14217983);
      }

      if (k % 2 == 0) {
         SoundDesign.layerOnly(p.at(), GuraSounds.LAYER_SHATTER_HIGH, 0.5F, 1.1F + this.random.nextFloat() * 0.3F);
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      boolean fp = this.firstPersonCaster();
      if (this.burst < 0) {
         this.centre(f.entityPartial);
         float rr = this.radius(t);
         this.r = rr;
         this.follow();
         float p = this.strain(t);
         float in = Math.min(1.0F, t / 3.0F);
         float fcx = f.rx(this.cx);
         float fcy = f.ry(this.cy);
         float fcz = f.rz(this.cz);
         BufferBuilder d = f.distortion();
         if (d != null) {
            VfxSprites.Sprite ring = VfxSprites.RING_NORMAL;
            MeshWriter.distortQuad(
               d,
               fcx,
               fcy,
               fcz,
               rr * 1.32F,
               (0.45F + 0.6F * p) * in * (fp ? 0.6F : 1.0F),
               0,
               false,
               f.left.x,
               f.left.y,
               f.left.z,
               f.up.x,
               f.up.y,
               f.up.z,
               ring.u0(),
               ring.v0(),
               ring.u1(),
               ring.v1(),
               0.0F,
               0.0F
            );
         }

         this.band.draw(f, fcx, fcy, fcz, -f.left.x, -f.left.y, -f.left.z, f.up.x, f.up.y, f.up.z, rr, rr * 0.2F, (0.3F + 0.45F * p) * in, t * 0.04F);
         if (f.allowMesh()) {
            if (!fp) {
               MeshWriter.sphere(
                  f.mesh(MeshTex.NOISE),
                  fcx,
                  fcy,
                  fcz,
                  rr,
                  12,
                  24,
                  t * 0.01F,
                  t * 0.004F,
                  2.0F,
                  MeshWriter.argb(15398655, (0.11F + 0.08F * p) * in),
                  -1.0F,
                  0,
                  0.6F,
                  0.0F,
                  1.0F
               );
            }

            BufferBuilder b = f.mesh(MeshTex.LANCE);
            float jit = (0.01F + 0.02F * p) * Mth.sin(t * 9.1F);
            MeshWriter.ring(
               b,
               fcx,
               fcy,
               fcz,
               -f.left.x,
               -f.left.y,
               -f.left.z,
               f.up.x,
               f.up.y,
               f.up.z,
               rr * (1.0F + jit),
               Math.max(0.012F, rr * 0.018F),
               48,
               t * 0.03F,
               1.0F,
               MeshWriter.argb(15398655, (0.3F + 0.2F * p) * in),
               -1.0F,
               0,
               1.4F,
               0.0F
            );
            MeshWriter.ring(
               b,
               fcx,
               fcy,
               fcz,
               -f.left.x,
               -f.left.y,
               -f.left.z,
               f.up.x,
               f.up.y,
               f.up.z,
               rr * (0.975F - jit),
               Math.max(0.008F, rr * 0.01F),
               48,
               -t * 0.02F,
               1.0F,
               MeshWriter.argb(14217983, (0.06F + 0.12F * p) * in),
               -1.0F,
               0,
               1.0F,
               0.0F
            );
         }
      } else {
         float a = Math.max(0.0F, 1.0F - (t - (float)this.burst) / 22.0F);
         if (a > 0.01F && !fp) {
            Entity e = this.caster();
            double ex = e != null ? Mth.lerp((double)f.entityPartial, e.xo, e.getX()) : this.x;
            double ey = e != null ? Mth.lerp((double)f.entityPartial, e.yo, e.getY()) : this.y;
            double ez = e != null ? Mth.lerp((double)f.entityPartial, e.zo, e.getZ()) : this.z;
            this.band
               .draw(
                  f,
                  f.rx(ex),
                  f.ry(ey + (double)this.height * 0.52),
                  f.rz(ez),
                  -f.left.x,
                  -f.left.y,
                  -f.left.z,
                  f.up.x,
                  f.up.y,
                  f.up.z,
                  this.height * 0.62F,
                  this.height * 0.18F,
                  0.45F * a,
                  t * 0.05F
               );
         }
      }
   }

   @Override
   public double x() {
      return this.cx;
   }

   @Override
   public double y() {
      return this.cy;
   }

   @Override
   public double z() {
      return this.cz;
   }

   private static record Pane(Vec3 at, Vec3 normal, float radius, int due, boolean breaks) {
   }
}
