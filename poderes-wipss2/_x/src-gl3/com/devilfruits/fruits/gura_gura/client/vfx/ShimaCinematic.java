package com.devilfruits.fruits.gura_gura.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class ShimaCinematic extends QuakeCinematic {
   private static final int HAZE = 10;
   private final Vec3 warp;
   private final Vec3 facing;
   private final float warpRadius;
   private int tremorAge = -1;
   private Vec3 epicentre = Vec3.ZERO;
   private float radius;
   private int duration;
   private int interval = 10;
   private final double[] hx = new double[10];
   private final double[] hy = new double[10];
   private final double[] hz = new double[10];
   private final float[] hs = new float[10];

   public ShimaCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params, boolean grab) {
      super(level, anchor, params);
      this.warp = params.pos();
      Vec3 d = params.dir();
      this.facing = grab && d.lengthSqr() > 1.0E-6 ? d.normalize().scale(-1.0) : new Vec3(0.0, 0.0, 1.0);
      this.warpRadius = Math.max(3.0F, params.power());
      if (grab) {
         this.grab(level);
      }
   }

   private void grab(ClientLevel level) {
      Entity e = this.caster(level);
      Vec3 chest = new Vec3(this.x, this.y + (double)this.height * 0.6, this.z);

      for (int side = -1; side <= 1; side += 2) {
         Vec3 f = e != null ? fist(e, 1.0F, (float)side).add(this.facing.scale(0.4)) : chest.add(this.facing.scale(0.8));
         Vec3 out = f.subtract(chest).normalize();
         ShockRing.at(f, this.facing).radius(0.3F, 2.2F).life(16).strength(1.5F).band(0.45F).twist((float)side * 1.3F).rim(0.0F, 15398655).spawn();
         ShockRing.at(f, out).radius(0.2F, 1.4F).life(10).strength(1.1F).band(0.5F).twist((float)(-side) * 0.9F).rim(0.0F, 15398655).spawn();
      }

      Vec3 front = chest.add(this.facing.scale(4.5)).add(0.0, 1.0, 0.0);
      ShockRing.at(front, this.facing).radius(1.0F, 7.0F).life(24).strength(1.4F).band(0.35F).twist(1.25F).rim(0.12F, 15398655).spawn();
      ShockRing.at(front, this.facing).radius(0.6F, 4.5F).life(18).strength(1.1F).band(0.4F).twist(-1.1F).rim(0.0F, 15398655).spawn();
      ShockRing.at(this.warp, this.facing).radius(2.0F, this.warpRadius * 1.4F).life(34).strength(1.2F).band(0.4F).twist(0.8F).rim(0.08F, 15398655).spawn();
      float ex = this.exposure(chest);
      if (ex > 0.0F) {
         float soft = this.firstPersonCaster() ? 0.6F : 1.0F;
         CameraDirector.shake(0.4F * ex, 0.05F, 2.5F, 2.5F);
         VfxPost.chromatic(0.35F * ex * soft);
         VfxPost.flash(15398655, 0.12F * ex * soft);
      }
   }

   public void tremor(Vec3 at, float r, int ticks, int every) {
      this.tremorAge = 0;
      this.epicentre = at;
      this.radius = Math.max(4.0F, r);
      this.duration = Math.max(10, ticks);
      this.interval = Math.max(2, every);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      float ex = this.exposure(new Vec3(this.x, this.y + 1.0, this.z));
      if (this.tremorAge < 0) {
         if (this.local() && ex > 0.0F) {
            VfxPost.grade(13162728, 0.25F * ex);
         }

         if (this.age > 60) {
            this.finish();
         }
      } else {
         this.tremorAge++;
         if (this.tremorAge > this.duration + 10) {
            this.finish();
         } else {
            double gy = VfxWorld.ground(level, this.epicentre.x, this.epicentre.y + 1.0, this.epicentre.z, 6);
            double fy = Double.isNaN(gy) ? this.epicentre.y : gy;
            float fade = Math.min(1.0F, (float)(this.duration - this.tremorAge) / 20.0F + 0.1F);
            if (this.tremorAge % this.interval == 1 && this.tremorAge < this.duration) {
               int dust = QuakeCinematic.dust(level, this.epicentre.x, fy - 0.5, this.epicentre.z);
               GroundWave.at(this.epicentre.x, fy, this.epicentre.z, this.radius, 18)
                  .strength(1.2F)
                  .ring(15398655, 0.18F)
                  .dust(26, dust, 0.15F, 0.6F, 1.4F, this.seed + (long)this.tremorAge)
                  .spawn();
               ShockRing.at(new Vec3(this.epicentre.x, fy + 1.6, this.epicentre.z), new Vec3(0.0, 1.0, 0.0))
                  .radius(2.0F, this.radius)
                  .life(18)
                  .strength(0.9F)
                  .band(0.12F)
                  .rim(0.08F, 15398655)
                  .spawn();
            }

            if (this.tremorAge % 4 == 2 && this.tremorAge < this.duration) {
               double a = this.random.nextDouble() * (float) (Math.PI * 2);
               double d = 3.0 + this.random.nextDouble() * ((double)this.radius * 0.8 - 3.0);
               Vec3 p = new Vec3(this.epicentre.x + Math.cos(a) * d, fy + 1.5 + this.random.nextDouble() * 5.0, this.epicentre.z + Math.sin(a) * d);
               Vec3 n = new Vec3(Math.cos(a), 0.0, Math.sin(a));
               ShockRing.at(p, n)
                  .radius(0.4F, 2.5F + this.random.nextFloat() * 2.0F)
                  .life(12)
                  .strength(1.2F)
                  .band(0.45F)
                  .twist((this.random.nextFloat() - 0.5F) * 2.0F)
                  .rim(0.0F, 15398655)
                  .spawn();
            }

            for (int i = 0; i < VfxSettings.count(3.0F * fade); i++) {
               double a = this.random.nextDouble() * (float) (Math.PI * 2);
               double d = this.random.nextDouble() * (double)this.radius;
               double px = this.epicentre.x + Math.cos(a) * d;
               double pz = this.epicentre.z + Math.sin(a) * d;
               VfxParticles.emit(VfxPresets.DUST, px, fy + 0.2, pz, this.spread(0.03), 0.04 + this.random.nextDouble() * 0.04, this.spread(0.03))
                  .tint(VfxWorld.shade(10129544, VfxWorld.light(level, px, fy + 1.0, pz)))
                  .size(1.6F);
            }

            for (int k = 0; k < 10; k++) {
               double a = this.random.nextDouble() * (float) (Math.PI * 2);
               double d = this.random.nextDouble() * (double)this.radius;
               this.hx[k] = this.epicentre.x + Math.cos(a) * d;
               this.hy[k] = fy + 0.8 + this.random.nextDouble() * 4.0;
               this.hz[k] = this.epicentre.z + Math.sin(a) * d;
               this.hs[k] = 2.5F + this.random.nextFloat() * 3.0F;
            }
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      if (this.tremorAge >= 0) {
         BufferBuilder d = f.distortion();
         if (d != null) {
            float fade = Math.min(1.0F, (float)(this.duration - this.tremorAge) / 20.0F + 0.1F) * Math.min(1.0F, (float)this.tremorAge / 6.0F);
            VfxSprites.Sprite glow = VfxSprites.GLOW;

            for (int k = 0; k < 10; k++) {
               MeshWriter.distortQuad(
                  d,
                  f.rx(this.hx[k]),
                  f.ry(this.hy[k]),
                  f.rz(this.hz[k]),
                  this.hs[k],
                  0.7F * fade,
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
                  (float)k * 0.19F + f.time * 0.012F,
                  0.5F
               );
            }
         }
      }
   }

   @Override
   public double x() {
      return this.tremorAge >= 0 ? this.epicentre.x : this.x;
   }

   @Override
   public double y() {
      return this.tremorAge >= 0 ? this.epicentre.y + 2.0 : this.y + 1.0;
   }

   @Override
   public double z() {
      return this.tremorAge >= 0 ? this.epicentre.z : this.z;
   }

   @Override
   public double cullDistance() {
      return 160.0;
   }
}
