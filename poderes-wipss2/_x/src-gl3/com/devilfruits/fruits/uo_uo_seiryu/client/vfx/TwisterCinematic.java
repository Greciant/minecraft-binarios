package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.render.vfx.FloorDebrisEffect;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.ArcBurst;
import com.devilfruits.client.vfx.fx.ArcStrip;
import com.devilfruits.client.vfx.fx.BoltStrike;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.Impact;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class TwisterCinematic extends HybridCinematic {
   private static final int BANDS = 9;
   private final float radius;
   private final int longest;
   private double cx;
   private double cy;
   private double cz;
   private int released = -1;
   @Nullable
   private FloorDebrisEffect debris;
   private float spin;

   public TwisterCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.radius = Math.max(3.0F, params.power());
      this.longest = Math.max(10, params.extra());
      this.centre(level);
      if (!Double.isNaN(this.groundY) && this.cy - this.groundY < 12.0) {
         this.debris = new FloorDebrisEffect(level, new Vec3(this.cx, this.groundY, this.cz), 14, 0.2F, 0.5F, 200, this.seed).orbit(this.radius * 0.7F, 0.22F);
         VfxManager.add(this.debris);
      }

      float ex = this.exposure();
      if (ex > 0.0F) {
         CameraDirector.shake(0.3F * ex, 0.08F, 1.6F, 2.5F);
         CameraDirector.fovPunch(0.05F * ex);
      }
   }

   @Nullable
   public static TwisterCinematic of(Entity e) {
      return VfxDirector.find(TwisterCinematic.class, c -> c.follows(e) && c.released < 0);
   }

   private void centre(ClientLevel level) {
      this.cx = this.x;
      this.cz = this.z;
      double g = VfxWorld.ground(level, this.x, this.y + 1.0, this.z, 24);
      this.groundY = g;
      this.cy = Double.isNaN(g) ? this.y - 4.0 : g;
   }

   public void release(ClientLevel level) {
      if (this.released < 0) {
         this.released = this.age;
         Entity e = this.caster(level);
         Vec3 c = new Vec3(this.cx, this.cy + 1.5, this.cz);
         GroundWave.at(this.cx, this.cy, this.cz, this.radius * 1.8F, 16)
            .strength(1.6F)
            .ring(this.shade(9076856), 0.35F)
            .dust(40, this.shade(VfxWorld.groundColour(level, this.cx, this.cy - 0.5, this.cz)), 0.4F, 0.55F, 1.1F, this.seed ^ 119L)
            .air(this.cy + 3.0, this.radius * 1.4F)
            .spawn();
         Impact.at(level, c, this.seed ^ 120L)
            .caster(e)
            .thunder()
            .flash(1.6F)
            .glow(11955455)
            .sparks(40, 0.8F)
            .embers(30)
            .trauma(0.5F, 0.08F, 3.0F)
            .lens(0.3F)
            .play();
         ArcBurst.at(c, this.seed ^ 121L)
            .arcs(4)
            .radius(this.radius * 0.5F, this.radius * 1.6F)
            .life(14)
            .spin(0.6F)
            .arc(3.7699115F)
            .colour(MeshTex.FLAME_RIBBON, this.shade(14736082), 1.2F)
            .alpha(0.6F)
            .tilt(0.0F, 0.15F)
            .spawn();
         if (this.debris != null) {
            this.debris.drop();
            this.debris = null;
         }

         float ex = this.exposure();
         if (ex > 0.0F) {
            VfxPost.radialBlur(c, 0.2F * ex);
         }
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.released >= 0) {
         if (this.age - this.released > 20) {
            this.finish();
         }
      } else if (this.age > this.longest + 20) {
         this.release(level);
      } else {
         this.centre(level);
         float grow = Math.min(1.0F, (float)this.age / 12.0F);
         this.spin += 0.32F;
         double top = Math.max(this.cy + (double)this.radius * 1.6, this.y + 4.0);

         for (int i = 0; i < VfxSettings.count(10.0F * grow); i++) {
            double h = this.random.nextDouble();
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double r = (double)this.radius * (0.45 + 0.55 * h) * (0.85 + this.random.nextDouble() * 0.3);
            double py = Mth.lerp(h, this.cy + 0.3, top);
            boolean fire = i % 3 == 0;
            VfxParticles.emit(fire ? VfxPresets.FIRE_PUFF : VfxPresets.DUST, this.cx + Math.cos(a) * r, py, this.cz + Math.sin(a) * r, 0.0, 0.06, 0.0)
               .orbit(this.cx, this.cz, 0.22F)
               .size(fire ? 1.4F : 1.7F)
               .tint(fire ? 16777215 : this.shade(12432554));
         }

         for (int i = 0; i < VfxSettings.count(3.0F * grow); i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            VfxParticles.emit(
                  VfxPresets.EMBER,
                  this.cx + Math.cos(a) * (double)this.radius,
                  this.cy + 0.5 + this.random.nextDouble() * 4.0,
                  this.cz + Math.sin(a) * (double)this.radius,
                  -Math.cos(a) * 0.15,
                  0.1,
                  -Math.sin(a) * 0.15
               )
               .orbit(this.cx, this.cz, 0.25F);
         }

         if (this.age % 6 == 0) {
            GroundWave.at(this.cx, this.cy, this.cz, this.radius * 1.1F, 10)
               .strength(0.8F)
               .dust(10, this.shade(VfxWorld.groundColour(level, this.cx, this.cy - 0.5, this.cz)), 0.7F, 0.3F, 0.9F, this.seed + (long)this.age)
               .spawn();
         }

         if (this.random.nextFloat() < 0.35F * grow) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double b = a + 1.5 + this.random.nextDouble() * 2.0;
            double h0 = this.random.nextDouble();
            double h1 = this.random.nextDouble();
            Vec3 p0 = new Vec3(
               this.cx + Math.cos(a) * (double)this.radius * (0.5 + 0.5 * h0),
               Mth.lerp(h0, this.cy + 0.5, top),
               this.cz + Math.sin(a) * (double)this.radius * (0.5 + 0.5 * h0)
            );
            Vec3 p1 = new Vec3(
               this.cx + Math.cos(b) * (double)this.radius * (0.5 + 0.5 * h1),
               Mth.lerp(h1, this.cy + 0.5, top),
               this.cz + Math.sin(b) * (double)this.radius * (0.5 + 0.5 * h1)
            );
            BoltStrike.between(p0, p1, this.seed + (long)this.age * 61L).width(0.2F).life(4).forks(3).rough(0.2F).endGlow(1.0F).spawn();
         }

         float ex = this.exposure();
         if (ex > 0.0F && this.age % 8 == 0) {
            CameraDirector.shake(0.12F * ex, 0.15F, 1.0F, 4.0F);
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      float grow = this.released >= 0 ? Math.max(0.0F, 1.0F - (t - (float)this.released) / 5.0F) : Math.min(1.0F, t / 12.0F);
      if (!(grow <= 0.01F)) {
         float rx = f.rx(this.cx);
         float rz = f.rz(this.cz);
         double top = Math.max(this.cy + (double)this.radius * 1.6, this.y + 4.0);
         float a0 = this.spin + 0.32F * f.partial;
         int dust = this.shade(13814464);

         for (int b = 0; b < 9; b++) {
            float h = (float)b / 8.0F;
            float r = this.radius * (0.42F + 0.6F * h) * (this.released >= 0 ? 1.0F + (1.0F - grow) * 0.8F : 1.0F);
            float ry = f.ry(Mth.lerp((double)h, this.cy + 0.6, top));
            float head = a0 * (1.2F - 0.4F * h) + (float)b * 1.7F;
            ArcStrip.draw(
               f,
               MeshTex.FLAME_RIBBON,
               rx,
               ry,
               rz,
               1.0F,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               1.0F,
               r,
               head,
               4.08407F,
               0.9F + 0.5F * h,
               -1.0F,
               0.42F * grow,
               0,
               0.3F,
               dust,
               -t * 0.12F,
               40
            );
            if ((b & 1) == 0) {
               ArcStrip.draw(
                  f,
                  MeshTex.FLAME_RIBBON,
                  rx,
                  ry + 0.3F,
                  rz,
                  1.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  r * 0.92F,
                  head + 2.2F,
                  (float) (Math.PI * 4.0 / 5.0),
                  0.35F + 0.2F * h,
                  1.25F,
                  0.85F * grow,
                  0,
                  2.0F,
                  16777215,
                  -t * 0.25F,
                  32
               );
            } else {
               ArcStrip.draw(
                  f,
                  MeshTex.CRACKLE,
                  rx,
                  ry + 0.2F,
                  rz,
                  1.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  r * 0.96F,
                  head + 3.4F,
                  1.8849558F,
                  0.28F,
                  1.3F,
                  0.7F * grow,
                  1,
                  2.2F,
                  16777215,
                  -t * 0.3F,
                  28
               );
            }
         }

         BufferBuilder d = f.distortion();
         if (d != null) {
            VfxSprites.Sprite glow = VfxSprites.GLOW;

            for (int k = 0; k < 4; k++) {
               double a = (double)(a0 + (float)k * (float) (Math.PI / 2));
               MeshWriter.distortQuad(
                  d,
                  rx + (float)Math.cos(a) * this.radius * 0.7F,
                  f.ry(this.cy + 2.5),
                  rz + (float)Math.sin(a) * this.radius * 0.7F,
                  this.radius * 0.7F,
                  0.6F * grow,
                  1,
                  false,
                  f.left.x,
                  f.left.y,
                  f.left.z,
                  f.up.x,
                  f.up.y,
                  f.up.z,
                  glow.u0(),
                  glow.v0(),
                  glow.u1(),
                  glow.v1(),
                  (float)k * 0.31F + f.time * 0.01F,
                  0.5F
               );
            }
         }
      }
   }

   @Override
   public void removed() {
      if (this.debris != null) {
         this.debris.drop();
      }
   }

   @Override
   public double x() {
      return this.cx;
   }

   @Override
   public double y() {
      return this.cy + 3.0;
   }

   @Override
   public double z() {
      return this.cz;
   }

   @Override
   public double cullDistance() {
      return 160.0;
   }
}
