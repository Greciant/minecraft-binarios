package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.BoltStrike;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class RoarCinematic extends HybridCinematic {
   private static final int WAVES = 6;
   private static final int GAP = 2;
   private final float reach;
   private final float spread;
   private final double mx;
   private final double my;
   private final double mz;
   private final float dx;
   private final float dy;
   private final float dz;
   private final float ax;
   private final float ay;
   private final float az;
   private final float bx;
   private final float by;
   private final float bz;
   private final float speed;

   public RoarCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.reach = Math.max(6.0F, params.power());
      this.spread = (float)Math.tan(Math.toRadians((double)Mth.clamp(params.extra(), 10, 160) * 0.5)) * 0.55F;
      Vec3 m = params.pos();
      this.mx = m.x;
      this.my = m.y;
      this.mz = m.z;
      Vec3 d = params.dir().lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : params.dir().normalize();
      this.dx = (float)d.x;
      this.dy = (float)d.y;
      this.dz = (float)d.z;
      Vec3 a = Math.abs(d.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : d.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
      Vec3 b = a.cross(d).normalize();
      this.ax = (float)a.x;
      this.ay = (float)a.y;
      this.az = (float)a.z;
      this.bx = (float)b.x;
      this.by = (float)b.y;
      this.bz = (float)b.z;
      this.speed = this.reach / 12.0F;
      VfxParticles.emit(VfxPresets.THUNDER_FLARE, this.mx, this.my, this.mz, 0.0, 0.0, 0.0)
         .size(3.0F)
         .rotation(this.random.nextFloat() * (float) (Math.PI * 2));
      VfxParticles.emit(VfxPresets.GLOW, this.mx, this.my, this.mz, 0.0, 0.0, 0.0).size(12.0F).tint(11955455).alpha(0.7F).life(2.0F);
      float ex = this.exposure();
      if (ex > 0.0F) {
         float soft = this.firstPersonCaster() ? 0.5F : 1.0F;
         CameraDirector.shake(0.75F * ex, 0.05F, 3.5F, 5.0F);
         CameraDirector.fovPunch(0.1F * ex);
         VfxPost.chromatic(0.45F * ex * soft);
         VfxPost.radialBlur(m.add(d.scale((double)this.reach * 0.3)), 0.22F * ex * soft);
         VfxPost.flash(11955455, 0.08F * ex * soft);
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Vec3 m = new Vec3(this.mx, this.my, this.mz);
      if (this.age < 14 && this.random.nextFloat() < 0.8F) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         float r = 3.0F;
         Vec3 end = m.add(
            ((double)this.ax * Math.cos(a) + (double)this.bx * Math.sin(a)) * (double)r + (double)(this.dx * 2.0F),
            ((double)this.ay * Math.cos(a) + (double)this.by * Math.sin(a)) * (double)r + (double)(this.dy * 2.0F),
            ((double)this.az * Math.cos(a) + (double)this.bz * Math.sin(a)) * (double)r + (double)(this.dz * 2.0F)
         );
         BoltStrike.between(m, end, this.seed + (long)this.age * 71L).width(0.18F).life(3).forks(2).rough(0.25F).endGlow(0.8F).spawn();
      }

      for (int w = 0; w < 6; w++) {
         float d = (float)(this.age - w * 2) * this.speed;
         if (!(d <= 0.0F) && !(d > this.reach)) {
            double px = this.mx + (double)(this.dx * d);
            double py = this.my + (double)(this.dy * d);
            double pz = this.mz + (double)(this.dz * d);
            double g = VfxWorld.ground(level, px, py, pz, 20);
            if (!Double.isNaN(g) && !(py - g > 18.0)) {
               float r = 1.0F + d * this.spread;
               int c = VfxWorld.groundColour(level, px, g - 0.5, pz);
               int dust = this.shade(((c >> 16 & 0xFF) + 308) / 3 << 16 | ((c >> 8 & 0xFF) + 296) / 3 << 8 | ((c & 0xFF) + 280) / 3);

               for (int i = 0; i < VfxSettings.count(4.0F); i++) {
                  double s = this.spread((double)r);
                  VfxParticles.emit(
                        VfxPresets.DUST,
                        px + (double)this.ax * s,
                        g + 0.3,
                        pz + (double)this.az * s,
                        (double)this.dx * 0.5 + this.spread(0.1),
                        0.08,
                        (double)this.dz * 0.5 + this.spread(0.1)
                     )
                     .tint(dust)
                     .size(1.8F);
               }
            }
         }
      }

      if (this.age > 26) {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      if (f.allowMesh()) {
         float t = (float)this.age + f.partial;
         BufferBuilder ribbon = f.mesh(MeshTex.FLAME_RIBBON);
         BufferBuilder crackle = f.mesh(MeshTex.CRACKLE);
         BufferBuilder dist = f.distortion();
         VfxSprites.Sprite ring = VfxSprites.RING_NORMAL;

         for (int w = 0; w < 6; w++) {
            float d = (t - (float)(w * 2)) * this.speed;
            if (!(d <= 0.3F) && !(d > this.reach)) {
               float k = d / this.reach;
               float r = 1.0F + d * this.spread;
               float alpha = (1.0F - k) * Math.min(1.0F, d / 2.0F);
               float cx = f.rx(this.mx + (double)(this.dx * d));
               float cy = f.ry(this.my + (double)(this.dy * d));
               float cz = f.rz(this.mz + (double)(this.dz * d));
               MeshWriter.ring(
                  ribbon,
                  cx,
                  cy,
                  cz,
                  this.ax,
                  this.ay,
                  this.az,
                  this.bx,
                  this.by,
                  this.bz,
                  r,
                  Math.max(0.6F, r * 0.18F),
                  40,
                  t * 0.05F,
                  3.0F,
                  MeshWriter.argb(this.shade(15591668), 0.4F * alpha),
                  -1.0F,
                  0,
                  0.3F,
                  0.0F
               );
               MeshWriter.ring(
                  crackle,
                  cx,
                  cy,
                  cz,
                  this.ax,
                  this.ay,
                  this.az,
                  this.bx,
                  this.by,
                  this.bz,
                  r * 1.04F,
                  Math.max(0.35F, r * 0.08F),
                  40,
                  -t * 0.08F,
                  4.0F,
                  MeshWriter.argb(16777215, alpha),
                  1.45F,
                  1,
                  3.0F,
                  0.0F
               );
               if (dist != null) {
                  MeshWriter.distortQuad(
                     dist,
                     cx,
                     cy,
                     cz,
                     r * 2.3F,
                     1.4F * alpha,
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
            }
         }
      }
   }

   @Override
   public double x() {
      return this.mx + (double)(this.dx * this.reach) * 0.5;
   }

   @Override
   public double y() {
      return this.my + (double)(this.dy * this.reach) * 0.5;
   }

   @Override
   public double z() {
      return this.mz + (double)(this.dz * this.reach) * 0.5;
   }

   @Override
   public double cullDistance() {
      return (double)(96.0F + this.reach);
   }
}
