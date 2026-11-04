package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.BoltStrike;
import com.devilfruits.client.vfx.fx.Impact;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.fruits.uo_uo_seiryu.DragonHead;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class PlasmaBeamCinematic extends HybridCinematic {
   static final int WHITE = 16774338;
   static final int YELLOW = 16765503;
   static final int ORANGE = 16742938;
   private static final int WARM_GRADE = 16768176;
   private static final int GROW = 3;
   private static final int FADE = 6;
   private static final int N = 64;
   private final int duration;
   private final float maxLength;
   private final float w;
   private double mx;
   private double my;
   private double mz;
   private float dx;
   private float dy;
   private float dz = 1.0F;
   private float odx;
   private float ody;
   private float odz = 1.0F;
   private float length;
   private float oLength;
   private boolean hitting;
   private double hx;
   private double hy;
   private double hz;
   @Nullable
   private Direction face;
   private final float[] sx = new float[64];
   private final float[] sy = new float[64];
   private final float[] sz = new float[64];
   private final float[] sw = new float[64];
   private final float[] sh = new float[64];
   private final float[] sa = new float[64];

   public PlasmaBeamCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params, float scale) {
      super(level, anchor, params);
      this.duration = Math.max(1, params.extra());
      this.maxLength = Math.max(2.0F, params.power());
      this.w = scale;
      Vec3 m = params.pos();
      this.mx = m.x;
      this.my = m.y;
      this.mz = m.z;
      Vec3 d = params.dir().lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : params.dir().normalize();
      this.dx = this.odx = (float)d.x;
      this.dy = this.ody = (float)d.y;
      this.dz = this.odz = (float)d.z;
      VfxParticles.emit(VfxPresets.FLARE, this.mx, this.my, this.mz, 0.0, 0.0, 0.0)
         .size(1.3F * this.w)
         .rotation(this.random.nextFloat() * (float) (Math.PI * 2));
      VfxParticles.emit(VfxPresets.GLOW, this.mx, this.my, this.mz, 0.0, 0.0, 0.0).size(5.0F * this.w).tint(16765503).alpha(0.8F).life(1.6F);
      float ex = this.exposure();
      if (ex > 0.0F) {
         float soft = this.firstPersonCaster() ? 0.5F : 1.0F;
         CameraDirector.shake(0.35F * ex, 0.09F, 2.0F, 3.0F);
         CameraDirector.fovPunch(0.07F * ex);
         VfxPost.radialBlur(m.add(d.scale((double)this.maxLength * 0.5)), 0.16F * ex * soft);
         VfxPost.flash(16774338, 0.18F * ex * soft);
      }
   }

   @Nullable
   public static PlasmaBeamCinematic of(Entity e) {
      return VfxDirector.find(PlasmaBeamCinematic.class, c -> c.follows(e));
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.caster(level);
      if (e != null && e.isAlive()) {
         Vec3 m = BreathEffect.mouth(e, 1.0F);
         Vec3 look = DragonHead.aim(level, e, m, (double)this.maxLength, 1.0F);
         this.odx = this.dx;
         this.ody = this.dy;
         this.odz = this.dz;
         this.oLength = this.length;
         this.mx = m.x;
         this.my = m.y;
         this.mz = m.z;
         this.dx = (float)look.x;
         this.dy = (float)look.y;
         this.dz = (float)look.z;
         if (this.age <= this.duration) {
            Vec3 to = m.add(look.scale((double)this.maxLength));
            BlockHitResult hit = PhysicsWorld.clip(level, new ClipContext(m, to, Block.COLLIDER, Fluid.ANY, e));
            boolean miss = hit.getType() == Type.MISS;
            float target = miss ? this.maxLength : (float)m.distanceTo(hit.getLocation());
            this.length = Math.min(target, this.length + this.maxLength / 3.0F);
            this.hitting = !miss && this.length >= target - 0.01F;
            if (this.hitting) {
               this.hx = hit.getLocation().x;
               this.hy = hit.getLocation().y;
               this.hz = hit.getLocation().z;
               this.face = hit.getDirection();
               this.splash(level, e);
            }

            this.tickBeam(level, e, m);
         } else {
            this.length = Math.max(0.0F, this.length - this.maxLength / 6.0F);
            this.hitting = false;
            if (this.age == this.duration + 1) {
               VfxParticles.emit(VfxPresets.FLARE, this.mx, this.my, this.mz, 0.0, 0.0, 0.0)
                  .size(0.9F * this.w)
                  .rotation(this.random.nextFloat() * (float) (Math.PI * 2));

               for (int i = 0; i < VfxSettings.count(12.0F * this.w); i++) {
                  double u = this.random.nextDouble() * (double)this.oLength;
                  VfxParticles.emit(
                     VfxPresets.EMBER,
                     this.mx + (double)this.dx * u,
                     this.my + (double)this.dy * u,
                     this.mz + (double)this.dz * u,
                     this.spread(0.08),
                     0.04 + this.random.nextDouble() * 0.06,
                     this.spread(0.08)
                  );
               }
            }
         }

         if (this.age > this.duration + 6 + 30) {
            this.finish();
         }
      } else {
         this.finish();
      }
   }

   private void tickBeam(ClientLevel level, Entity e, Vec3 m) {
      float L = this.length;
      VfxParticles.emit(VfxPresets.GLOW, this.mx, this.my, this.mz, 0.0, 0.0, 0.0).size(2.8F * this.w).tint(16765503).alpha(0.75F);
      if (this.age % 2 == 0) {
         VfxParticles.emit(VfxPresets.FLARE, this.mx, this.my, this.mz, 0.0, 0.0, 0.0)
            .size(0.55F * this.w)
            .rotation(this.random.nextFloat() * (float) (Math.PI * 2))
            .life(0.6F);
      }

      for (int i = 0; i < VfxSettings.count(4.0F * Math.min(this.w, 2.0F)); i++) {
         double u = 0.6 + this.random.nextDouble() * Math.max(0.1, (double)L - 0.6);
         double ox = this.spread(0.3 * (double)this.w);
         double oy = this.spread(0.3 * (double)this.w);
         double oz = this.spread(0.3 * (double)this.w);
         VfxParticles.emit(
            i % 2 == 0 ? VfxPresets.SPARK : VfxPresets.EMBER,
            this.mx + (double)this.dx * u + ox,
            this.my + (double)this.dy * u + oy,
            this.mz + (double)this.dz * u + oz,
            ox * 0.3 + (double)this.dx * 0.15,
            oy * 0.3 + 0.03,
            oz * 0.3 + (double)this.dz * 0.15
         );
      }

      for (int i = 0; i < VfxSettings.count(2.0F * Math.min(this.w, 2.0F)); i++) {
         double u = this.random.nextDouble() * (double)L;
         VfxParticles.emit(
               VfxPresets.FIRE_TONGUE,
               this.mx + (double)this.dx * u + this.spread(0.35 * (double)this.w),
               this.my + (double)this.dy * u + this.spread(0.35 * (double)this.w),
               this.mz + (double)this.dz * u + this.spread(0.35 * (double)this.w),
               (double)this.dx * 0.2,
               (double)this.dy * 0.2 + 0.03,
               (double)this.dz * 0.2
            )
            .size(0.9F * this.w)
            .life(0.7F);
      }

      for (int k = 1; k <= 4; k++) {
         double u = (double)(L * (float)k) / 5.0;
         double px = this.mx + (double)this.dx * u;
         double py = this.my + (double)this.dy * u;
         double pz = this.mz + (double)this.dz * u;
         double gy = VfxWorld.ground(level, px, py, pz, (int)(6.0F * this.w));
         if (!Double.isNaN(gy) && py - gy < (double)(6.0F * this.w)) {
            float near = 1.0F - (float)((py - gy) / (double)(6.0F * this.w));
            VfxParticles.emit(VfxPresets.GROUND_GLOW, px, gy + 0.05, pz, 0.0, 0.0, 0.0).size(3.2F * this.w).tint(16742938).alpha(0.55F * near);
         }
      }

      float ex = this.exposure();
      if (ex > 0.0F && this.local()) {
         VfxPost.grade(16768176, 0.3F * ex);
         VfxPost.chromatic(0.05F * ex * (this.firstPersonCaster() ? 0.5F : 1.0F));
         if (this.age % 3 == 0) {
            CameraDirector.shake(0.1F * ex, 0.18F, 0.8F, 6.0F);
         }
      }

      if (this.hitting && this.age % 4 == 2) {
         this.pop(level, e);
      }
   }

   private void splash(ClientLevel level, Entity e) {
      double nx = this.face != null ? (double)this.face.getStepX() : (double)(-this.dx);
      double ny = this.face != null ? (double)this.face.getStepY() : (double)(-this.dy);
      double nz = this.face != null ? (double)this.face.getStepZ() : (double)(-this.dz);
      VfxParticles.emit(VfxPresets.GLOW, this.hx + nx * 0.2, this.hy + ny * 0.2, this.hz + nz * 0.2, 0.0, 0.0, 0.0)
         .size(3.2F * this.w)
         .tint(16774338)
         .alpha(0.85F);
      float m = Math.min(this.w, 1.8F);

      for (int i = 0; i < VfxSettings.count(5.0F * m); i++) {
         VfxParticles.emit(
            VfxPresets.SPARK,
            this.hx + nx * 0.1,
            this.hy + ny * 0.1,
            this.hz + nz * 0.1,
            nx * 0.25 + this.spread(0.3),
            ny * 0.25 + 0.12 + this.random.nextDouble() * 0.2,
            nz * 0.25 + this.spread(0.3)
         );
      }

      for (int i = 0; i < VfxSettings.count(2.0F * m); i++) {
         VfxParticles.emit(
               VfxPresets.FIRE_TONGUE,
               this.hx + this.spread(0.4 * (double)this.w),
               this.hy + ny * 0.2 + 0.1,
               this.hz + this.spread(0.4 * (double)this.w),
               this.spread(0.05),
               0.08,
               this.spread(0.05)
            )
            .size(1.1F * this.w);
      }

      if (this.age % 3 == 0) {
         VfxParticles.emit(VfxPresets.SMOKE, this.hx + this.spread(0.5), this.hy + 0.6, this.hz + this.spread(0.5), this.spread(0.04), 0.07, this.spread(0.04))
            .tint(this.shade(2761760))
            .size(1.2F * this.w)
            .alpha(0.7F);
      }

      if (this.face == Direction.UP) {
         VfxParticles.emit(VfxPresets.GROUND_GLOW, this.hx, this.hy + 0.04, this.hz, 0.0, 0.0, 0.0).size(2.6F * this.w).tint(16756816).alpha(0.9F);
      }
   }

   private void pop(ClientLevel level, Entity e) {
      Vec3 at = new Vec3(this.hx - (double)this.dx * 0.3, this.hy + 0.25, this.hz - (double)this.dz * 0.3);
      float k = Math.min(this.w, 2.5F);
      Impact.at(level, at, this.seed + (long)this.age * 131L)
         .caster(e)
         .flash(0.55F * k)
         .glow(16742938)
         .sparks(Math.round(12.0F * Math.min(k, 1.8F)), 0.45F)
         .embers(Math.round(8.0F * Math.min(k, 1.8F)))
         .play();
      VfxParticles.emit(VfxPresets.FIREBALL, at.x, at.y, at.z, 0.0, 0.03, 0.0).size(0.28F * k);
   }

   public void explode(ClientLevel level, Vec3 at, float size) {
      Entity e = this.caster(level);
      float k = Mth.clamp(size / 1.4F, 0.7F, 4.0F) * Math.min(this.w, 1.7F);
      float n = Math.min(k, 1.8F);
      if (!level.getFluidState(BlockPos.containing(at.x, at.y - 0.05, at.z)).isEmpty()) {
         for (int i = 0; i < VfxSettings.count(10.0F * k); i++) {
            VfxParticles.emit(
                  VfxPresets.SMOKE,
                  at.x + this.spread(0.8 * (double)k),
                  at.y + 0.2,
                  at.z + this.spread(0.8 * (double)k),
                  this.spread(0.05),
                  0.1 + this.random.nextDouble() * 0.06,
                  this.spread(0.05)
               )
               .tint(14210256)
               .size(1.2F * k)
               .alpha(0.55F);
         }
      } else {
         Vec3 c = at.add((double)(-this.dx) * 0.4, 0.3, (double)(-this.dz) * 0.4);
         Impact.at(level, c, this.seed ^ (long)this.age * 7919L)
            .caster(e)
            .flash(1.05F * k)
            .glow(16742938)
            .wave(2.2F * k, 10, 16765503)
            .dust(Math.round(8.0F * n))
            .sparks(Math.round(30.0F * n), 0.6F * k / n)
            .embers(Math.round(18.0F * n))
            .smoke(Math.round(5.0F * n), 2761760, 1.2F * k)
            .debris(Math.round(4.0F * n), 0.26F)
            .decal(2.2F * k)
            .trauma(0.14F * k, 0.12F, 1.2F)
            .lens(0.12F)
            .play();
         VfxParticles.emit(VfxPresets.FIREBALL, c.x, c.y + 0.2, c.z, 0.0, 0.04, 0.0).size(0.5F * k);

         for (int i = 0; i < VfxSettings.count(10.0F * n); i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            VfxParticles.emit(
                  VfxPresets.FIRE_PUFF, c.x, c.y, c.z, Math.cos(a) * 0.3 * (double)k, 0.05 + this.random.nextDouble() * 0.14, Math.sin(a) * 0.3 * (double)k
               )
               .size(1.0F * k);
         }

         for (int i = 0; i < 2; i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            BoltStrike.between(
                  c,
                  c.add(Math.cos(a) * 1.6 * (double)k, 0.4 + this.random.nextDouble() * 1.2 * (double)k, Math.sin(a) * 1.6 * (double)k),
                  this.seed + (long)this.age * 31L + (long)i
               )
               .width(0.07F * k)
               .life(3)
               .forks(1)
               .rough(0.3F)
               .endGlow(0.5F)
               .spawn();
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      float p = f.partial;
      float L = Mth.lerp(p, this.oLength, this.length);
      if (!(L < 0.05F) && f.allowMesh()) {
         float t = (float)this.age + p;
         Entity e = this.caster();
         Vec3 m = e != null ? BreathEffect.mouth(e, f.entityPartial) : new Vec3(this.mx, this.my, this.mz);
         float ddx = Mth.lerp(p, this.odx, this.dx);
         float ddy = Mth.lerp(p, this.ody, this.dy);
         float ddz = Mth.lerp(p, this.odz, this.dz);
         float dl = Mth.sqrt(ddx * ddx + ddy * ddy + ddz * ddz) + 1.0E-5F;
         ddx /= dl;
         ddy /= dl;
         ddz /= dl;
         float start = this.firstPersonCaster() ? 1.4F + 0.4F * this.w : 0.0F;
         float len = L - start;
         if (!(len < 0.1F)) {
            float thin = t <= (float)(this.duration + 1) ? Math.min(1.0F, t / 3.0F + 0.3F) : Math.max(0.0F, 1.0F - (t - (float)this.duration - 1.0F) / 6.0F);
            float flick = 0.93F + 0.07F * Mth.sin(t * 2.7F) + 0.04F * Mth.sin(t * 7.3F);
            float W = this.w * thin * flick * (this.w > 3.0F ? 1.0F + 0.35F * Math.min(1.0F, t / 60.0F) : 1.0F);
            float bx = f.rx(m.x) + ddx * start;
            float by = f.ry(m.y) + ddy * start;
            float bz = f.rz(m.z) + ddz * start;
            float ax;
            float ay;
            float az;
            if (Math.abs(ddy) > 0.95F) {
               ax = 1.0F;
               ay = 0.0F;
               az = 0.0F;
            } else {
               ax = -ddz;
               ay = 0.0F;
               float al = Mth.sqrt(ax * ax + ddx * ddx);
               ax /= al;
               az = ddx / al;
            }

            float cx = ay * ddz - az * ddy;
            float cy = az * ddx - ax * ddz;
            float cz = ax * ddy - ay * ddx;
            MeshWriter.cone(
               f.mesh(MeshTex.NOISE),
               bx,
               by,
               bz,
               ddx,
               ddy,
               ddz,
               len,
               0.32F * W,
               0.66F * W,
               32,
               t * 0.02F,
               3.0F,
               -t * 0.55F,
               Math.max(1.0F, len / 2.5F),
               MeshWriter.argb(16777215, 0.75F),
               MeshWriter.argb(16777215, 0.45F),
               1.2F,
               0.95F,
               0,
               1.5F,
               0.0F,
               0.55F
            );
            MeshWriter.cone(
               f.mesh(MeshTex.NOISE),
               bx,
               by,
               bz,
               ddx,
               ddy,
               ddz,
               len,
               0.18F * W,
               0.27F * W,
               28,
               -t * 0.03F,
               2.0F,
               -t * 0.9F,
               Math.max(1.0F, len / 2.0F),
               MeshWriter.argb(16777215, 0.92F),
               MeshWriter.argb(16777215, 0.7F),
               1.5F,
               1.38F,
               0,
               2.6F,
               0.0F,
               0.3F
            );
            int n = Math.max(8, Math.min(64, (int)(len * 3.0F)));

            for (int i = 0; i < n; i++) {
               float s = (float)i / (float)(n - 1);
               float d = s * len;
               this.sx[i] = bx + ddx * d;
               this.sy[i] = by + ddy * d;
               this.sz[i] = bz + ddz * d;
               this.sw[i] = 0.24F * W;
               this.sh[i] = 1.4F;
               this.sa[i] = 0.75F * VfxWorld.nearFade(this.sx[i], this.sy[i], this.sz[i]);
            }

            BufferBuilder lance = f.mesh(MeshTex.LANCE);
            MeshWriter.strip(lance, n, this.sx, this.sy, this.sz, this.sw, this.sh, this.sa, -t * 0.7F, 0.18F, 16777215, 0, 2.6F);

            for (int i = 0; i < n; i++) {
               this.sw[i] = 0.1F * W;
               this.sh[i] = 1.8F;
               this.sa[i] = VfxWorld.nearFade(this.sx[i], this.sy[i], this.sz[i]);
            }

            MeshWriter.strip(lance, n, this.sx, this.sy, this.sz, this.sw, this.sh, this.sa, -t * 1.1F, 0.18F, 16777215, 0, 5.0F);

            for (int fil = 0; fil < 2; fil++) {
               for (int i = 0; i < n; i++) {
                  float s = (float)i / (float)(n - 1);
                  float d = s * len;
                  float ang = d * 2.4F / this.w - t * 0.85F + (float)fil * (float) Math.PI;
                  float r = 0.36F * W * (1.0F + 0.3F * s);
                  float c = Mth.cos(ang) * r;
                  float sn = Mth.sin(ang) * r;
                  this.sx[i] = bx + ddx * d + ax * c + cx * sn;
                  this.sy[i] = by + ddy * d + ay * c + cy * sn;
                  this.sz[i] = bz + ddz * d + az * c + cz * sn;
                  this.sw[i] = 0.05F * W;
                  this.sh[i] = fil == 0 ? 1.55F : 1.4F;
                  this.sa[i] = 0.85F * Math.min(1.0F, s * 6.0F) * VfxWorld.nearFade(this.sx[i], this.sy[i], this.sz[i]);
               }

               MeshWriter.strip(lance, n, this.sx, this.sy, this.sz, this.sw, this.sh, this.sa, -t * 0.8F + (float)fil, 0.2F, 16777215, fil == 0 ? 0 : 1, 3.2F);
            }

            BufferBuilder noise = f.mesh(MeshTex.NOISE);

            for (int i = 0; i < 4; i++) {
               float ph = (t * 0.085F + (float)i / 4.0F) % 1.0F;
               float d = ph * len;
               float a = Mth.sin(ph * (float) Math.PI) * 0.75F * thin;
               if (a > 0.03F) {
                  MeshWriter.ring(
                     noise,
                     bx + ddx * d,
                     by + ddy * d,
                     bz + ddz * d,
                     ax,
                     ay,
                     az,
                     cx,
                     cy,
                     cz,
                     0.42F * W * (1.0F + 0.6F * ph),
                     0.12F * W,
                     24,
                     t * 0.05F + (float)i,
                     2.0F,
                     MeshWriter.argb(16777215, a),
                     1.45F,
                     0,
                     2.2F,
                     0.0F
                  );
               }
            }

            if (!this.firstPersonCaster()) {
               MeshWriter.sphere(noise, bx, by, bz, 0.55F * W, 8, 12, t * 0.04F, -t * 0.08F, 2.0F, MeshWriter.argb(16777215, 0.95F), 1.6F, 0, 3.0F, 0.0F, 0.5F);
            }

            float ex = bx + ddx * len;
            float ey = by + ddy * len;
            float ez = bz + ddz * len;
            if (this.hitting) {
               MeshWriter.sphere(
                  noise,
                  ex,
                  ey,
                  ez,
                  0.62F * W * (0.9F + 0.1F * Mth.sin(t * 3.1F)),
                  8,
                  12,
                  -t * 0.05F,
                  t * 0.07F,
                  2.0F,
                  MeshWriter.argb(16777215, 0.9F),
                  1.55F,
                  0,
                  2.6F,
                  0.0F,
                  0.6F
               );
            }

            BufferBuilder dist = f.distortion();
            if (dist != null) {
               VfxSprites.Sprite glow = VfxSprites.GLOW;
               int q = Math.max(2, Math.min(10, (int)(len / (1.3F * this.w))));

               for (int k = 0; k <= q; k++) {
                  float d = (float)k / (float)q * len;
                  MeshWriter.distortQuad(
                     dist,
                     bx + ddx * d,
                     by + ddy * d,
                     bz + ddz * d,
                     1.5F * W,
                     1.0F * thin,
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
                     (float)k * 0.23F + f.time * 0.008F,
                     0.4F
                  );
               }

               if (this.hitting) {
                  VfxSprites.Sprite ring = VfxSprites.RING_NORMAL;
                  float pulse = t * 0.2F % 1.0F;
                  MeshWriter.distortQuad(
                     dist,
                     ex,
                     ey,
                     ez,
                     (1.0F + 2.0F * pulse) * this.w,
                     0.8F * (1.0F - pulse),
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
      return this.mx + (double)(this.dx * this.length) * 0.5;
   }

   @Override
   public double y() {
      return this.my + (double)(this.dy * this.length) * 0.5;
   }

   @Override
   public double z() {
      return this.mz + (double)(this.dz * this.length) * 0.5;
   }

   @Override
   public double cullDistance() {
      return (double)(96.0F + this.maxLength);
   }
}
