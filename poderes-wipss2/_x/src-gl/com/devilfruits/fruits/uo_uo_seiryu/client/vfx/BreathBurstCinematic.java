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
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class BreathBurstCinematic extends HybridCinematic {
   private static final int WARM_GRADE = 16769724;
   private static final int TRAVEL = 2;
   private final int chargeTicks;
   private final float forced;
   private float charge;
   private int released = -1;
   private Vec3 from = Vec3.ZERO;
   private float dx;
   private float dy;
   private float dz = 1.0F;
   private float length = 6.0F;
   private float width = 1.0F;
   @Nullable
   private Vec3 impactAt;
   private float impactSize;
   private boolean impactDone;
   private double mx;
   private double my;
   private double mz;
   private final float[] sx = new float[8];
   private final float[] sy = new float[8];
   private final float[] sz = new float[8];
   private final float[] sw = new float[8];
   private final float[] sh = new float[8];
   private final float[] sa = new float[8];

   public BreathBurstCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.chargeTicks = Math.max(1, params.extra());
      this.forced = params.power();
      Entity e = this.caster(level);
      if (e != null) {
         this.mouthPoint(e, 1.0F);
      }
   }

   @Nullable
   public static BreathBurstCinematic charging(Entity e) {
      return VfxDirector.find(BreathBurstCinematic.class, c -> c.follows(e) && c.released < 0);
   }

   @Nullable
   public static BreathBurstCinematic fired(Entity e) {
      return VfxDirector.find(BreathBurstCinematic.class, c -> c.follows(e) && c.released >= 0 && c.impactAt == null);
   }

   private void mouthPoint(Entity e, float partial) {
      Vec3 m = BreathEffect.mouth(e, partial);
      if (this.firstPersonCaster()) {
         m = m.add(e.getViewVector(partial).scale(1.4));
      }

      this.mx = m.x;
      this.my = m.y;
      this.mz = m.z;
   }

   public void release(ClientLevel level, Vec3 mouth, Vec3 dir, float dist) {
      this.released = this.age;
      this.charge = Math.max(this.charge, 0.35F);
      Vec3 d = dir.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
      this.dx = (float)d.x;
      this.dy = (float)d.y;
      this.dz = (float)d.z;
      this.length = Math.max(1.5F, dist);
      this.width = 0.5F + this.length * 0.09F;
      this.from = this.firstPersonCaster() ? mouth.add(d.scale(1.2)) : mouth;
      VfxParticles.emit(VfxPresets.FLARE, this.from.x, this.from.y, this.from.z, 0.0, 0.0, 0.0)
         .size(0.6F + 0.35F * this.charge)
         .rotation(this.random.nextFloat() * (float) (Math.PI * 2));
      VfxParticles.emit(VfxPresets.GLOW, this.from.x, this.from.y, this.from.z, 0.0, 0.0, 0.0).size(2.6F).tint(15217182).alpha(0.55F).life(1.5F);
      this.puffs(level, 30);
      float ex = this.exposure();
      if (ex > 0.0F) {
         float soft = this.firstPersonCaster() ? 0.5F : 1.0F;
         CameraDirector.shake(0.28F * ex, 0.1F, 1.6F, 3.0F);
         CameraDirector.fovPunch(0.05F * ex);
         VfxPost.radialBlur(this.from.add(d.scale((double)this.length * 0.5)), 0.12F * ex * soft);
         VfxPost.chromatic(0.15F * ex * soft);
      }
   }

   public void handOver() {
      this.finish();
   }

   public void impact(Vec3 at, float size) {
      this.impactAt = at;
      this.impactSize = size;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.caster(level);
      if (e != null) {
         this.mouthPoint(e, 1.0F);
      }

      if (this.released < 0) {
         this.tickCharge(level);
         if (this.age > this.chargeTicks + 60) {
            this.finish();
         }
      } else {
         int s = this.age - this.released;
         if (s <= 4) {
            this.puffs(level, 14 - s * 3);
         }

         if (this.impactAt != null && !this.impactDone && s >= 2) {
            this.impactDone = true;
            this.explode(level, e);
         }

         if (s > 40) {
            this.finish();
         }
      }
   }

   private void tickCharge(ClientLevel level) {
      this.charge = Math.min(1.0F, (float)this.age / (float)this.chargeTicks);
      float p = this.charge;
      float sc = this.forced > 0.0F ? this.forced : this.height / 1.8F;
      float reach = (0.7F + 0.6F * p) * sc;

      for (int i = 0; i < VfxSettings.count(2.0F + 4.0F * p); i++) {
         double ox = this.spread((double)reach);
         double oy = this.spread((double)reach);
         double oz = this.spread((double)reach);
         VfxParticles.emit(VfxPresets.FIRE_TONGUE, this.mx + ox, this.my + oy, this.mz + oz, -ox * 0.2, -oy * 0.2, -oz * 0.2)
            .size((0.45F + 0.3F * p) * sc)
            .life(0.6F);
      }

      for (int i = 0; i < VfxSettings.count(1.0F + 2.0F * p); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double r = 0.6 + this.random.nextDouble() * 0.5;
         double ox = Math.cos(a) * r;
         double oz = Math.sin(a) * r;
         double oy = this.spread(0.4);
         VfxParticles.emit(VfxPresets.EMBER, this.mx + ox, this.my + oy, this.mz + oz, -ox * 0.12 - oz * 0.1, -oy * 0.12, -oz * 0.12 + ox * 0.1).life(0.5F);
      }

      VfxParticles.emit(VfxPresets.GLOW, this.mx, this.my, this.mz, 0.0, 0.0, 0.0).size((0.9F + 2.2F * p) * sc).tint(16742938).alpha(0.5F + 0.4F * p);
      if (p > 0.5F && this.random.nextFloat() < 0.35F * p) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         Vec3 m = new Vec3(this.mx, this.my, this.mz);
         BoltStrike.between(
               m.add(this.spread(0.15), this.spread(0.15), this.spread(0.15)),
               m.add(Math.cos(a) * 0.7, this.spread(0.5), Math.sin(a) * 0.7),
               this.seed + (long)this.age * 29L
            )
            .width(0.045F)
            .life(2)
            .forks(1)
            .rough(0.35F)
            .endGlow(0.3F)
            .spawn();
      }

      float ex = this.exposure();
      if (this.local() && ex > 0.0F) {
         VfxPost.grade(16769724, 0.25F * p * ex);
         if (p >= 1.0F && this.age % 4 == 0) {
            CameraDirector.shake(0.06F * ex, 0.2F, 0.6F, 6.0F);
         }
      }
   }

   private void puffs(ClientLevel level, int n) {
      float speed = this.length / 7.0F;

      for (int i = 0; i < VfxSettings.count((float)n); i++) {
         double along = this.random.nextDouble() * 0.35;
         double sx = this.spread(1.0);
         double sy = this.spread(1.0);
         double sz = this.spread(1.0);
         double spreadV = (double)(this.width / this.length * speed) * 0.9;
         double v = (double)speed * (0.6 + this.random.nextDouble() * 0.55);
         VfxParticles.emit(
               i % 3 == 0 ? VfxPresets.FIRE_TONGUE : VfxPresets.FIRE_PUFF,
               this.from.x + (double)this.dx * along,
               this.from.y + (double)this.dy * along,
               this.from.z + (double)this.dz * along,
               (double)this.dx * v + sx * spreadV,
               (double)this.dy * v + sy * spreadV,
               (double)this.dz * v + sz * spreadV
            )
            .size(0.9F + 0.9F * this.charge + this.random.nextFloat() * 0.6F)
            .life(0.75F + this.random.nextFloat() * 0.35F);
      }

      for (int i = 0; i < VfxSettings.count((float)n * 0.3F); i++) {
         double v = (double)speed * (0.8 + this.random.nextDouble() * 0.4);
         VfxParticles.emit(
            VfxPresets.SPARK,
            this.from.x,
            this.from.y,
            this.from.z,
            (double)this.dx * v + this.spread(0.12),
            (double)this.dy * v + this.spread(0.12),
            (double)this.dz * v + this.spread(0.12)
         );
      }
   }

   private void explode(ClientLevel level, @Nullable Entity e) {
      Vec3 at = this.impactAt;
      float k = Mth.clamp(this.impactSize, 0.6F, 1.6F);
      if (!level.getFluidState(BlockPos.containing(at.x, at.y - 0.05, at.z)).isEmpty()) {
         for (int i = 0; i < VfxSettings.count(14.0F); i++) {
            VfxParticles.emit(
                  VfxPresets.SMOKE,
                  at.x + this.spread(0.8),
                  at.y + 0.2,
                  at.z + this.spread(0.8),
                  this.spread(0.05),
                  0.08 + this.random.nextDouble() * 0.06,
                  this.spread(0.05)
               )
               .tint(14210256)
               .size(1.1F)
               .alpha(0.6F);
         }
      } else {
         Vec3 c = at.add((double)(-this.dx) * 0.3, 0.3, (double)(-this.dz) * 0.3);
         Impact.at(level, c, this.seed ^ 225L)
            .caster(e)
            .flash(1.0F * k)
            .glow(16742938)
            .wave(1.9F * k, 11, 16774338)
            .dust(10)
            .sparks(Math.round(28.0F * k), 0.5F)
            .embers(Math.round(22.0F * k))
            .smoke(Math.round(8.0F * k), 2761760, 1.1F * k)
            .debris(Math.round(4.0F * k), 0.2F)
            .decal(1.5F * k)
            .trauma(0.32F * k, 0.1F, 2.0F)
            .fov(0.03F)
            .screenFlash(0.1F)
            .play();
         VfxParticles.emit(VfxPresets.FIREBALL, c.x, c.y + 0.2, c.z, 0.0, 0.03, 0.0).size(0.45F * k);

         for (int i = 0; i < VfxSettings.count(12.0F * k); i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            VfxParticles.emit(
                  VfxPresets.FIRE_PUFF, c.x, c.y, c.z, Math.cos(a) * 0.28 * (double)k, 0.04 + this.random.nextDouble() * 0.12, Math.sin(a) * 0.28 * (double)k
               )
               .size(0.9F * k);
         }

         for (int i = 0; i < 2; i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            BoltStrike.between(
                  c, c.add(Math.cos(a) * 1.4 * (double)k, 0.3 + this.random.nextDouble() * 0.9, Math.sin(a) * 1.4 * (double)k), this.seed + (long)i * 61L
               )
               .width(0.06F)
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
      float t = (float)this.age + f.partial;
      if (this.released < 0) {
         this.renderCharge(f, t);
      } else {
         this.renderBurst(f, t - (float)this.released);
      }
   }

   private void renderCharge(VfxFrame f, float t) {
      float p = Math.min(1.0F, t / (float)this.chargeTicks);
      float cx = f.rx(this.mx);
      float cy = f.ry(this.my);
      float cz = f.rz(this.mz);
      float sc = this.forced > 0.0F ? this.forced : this.height / 1.8F;
      float r = (0.07F + 0.2F * p) * (0.92F + 0.08F * Mth.sin(t * 1.3F)) * sc;
      if (f.allowMesh()) {
         MeshWriter.sphere(
            f.mesh(MeshTex.NOISE), cx, cy, cz, r, 8, 12, t * 0.03F, -t * 0.05F, 2.0F, MeshWriter.argb(16777215, 0.95F), 1.25F + 0.3F * p, 0, 2.4F, 0.0F, 0.6F
         );
      }

      BufferBuilder d = f.distortion();
      if (d != null) {
         VfxSprites.Sprite glow = VfxSprites.GLOW;
         MeshWriter.distortQuad(
            d,
            cx,
            cy,
            cz,
            (0.6F + 0.7F * p) * sc,
            0.35F + 0.4F * p,
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
            f.time * 0.006F,
            0.4F
         );
      }
   }

   private void renderBurst(VfxFrame f, float s) {
      if (!(s > 11.0F)) {
         float g = Math.min(1.0F, s / 2.0F);
         g = 1.0F - (1.0F - g) * (1.0F - g);
         float alpha = s < 3.0F ? 1.0F : Math.max(0.0F, 1.0F - (s - 3.0F) / 8.0F);
         float len = this.length * g;
         float bx = f.rx(this.from.x);
         float by = f.ry(this.from.y);
         float bz = f.rz(this.from.z);
         if (f.allowMesh() && alpha > 0.01F) {
            MeshWriter.cone(
               f.mesh(MeshTex.NOISE),
               bx,
               by,
               bz,
               this.dx,
               this.dy,
               this.dz,
               len,
               0.18F,
               this.width * (0.6F + 0.4F * g) * (1.0F + 0.25F * Math.max(0.0F, s - 3.0F) / 8.0F),
               28,
               s * 0.03F,
               2.0F,
               -s * 0.3F,
               1.6F,
               MeshWriter.argb(16777215, 0.8F * alpha),
               MeshWriter.argb(16777215, 0.2F * alpha),
               1.2F,
               0.78F,
               0,
               1.3F,
               0.0F,
               0.35F
            );
            MeshWriter.cone(
               f.mesh(MeshTex.NOISE),
               bx,
               by,
               bz,
               this.dx,
               this.dy,
               this.dz,
               len * 0.9F,
               0.1F,
               this.width * 0.45F * g,
               20,
               s * 0.05F,
               1.5F,
               -s * 0.45F,
               1.2F,
               MeshWriter.argb(16777215, 0.65F * alpha),
               MeshWriter.argb(16777215, 0.2F * alpha),
               1.38F,
               1.0F,
               0,
               1.9F,
               0.0F,
               0.2F
            );
         }

         float core = Math.max(0.0F, 1.0F - s / 5.0F);
         if (core > 0.01F && f.allowMesh()) {
            int n = 8;
            float[] xs = this.sx;
            float[] ys = this.sy;
            float[] zs = this.sz;
            float[] w = this.sw;
            float[] h = this.sh;
            float[] a = this.sa;

            for (int i = 0; i < n; i++) {
               float u = (float)i / (float)(n - 1);
               float dd = u * len * 0.85F;
               xs[i] = bx + this.dx * dd;
               ys[i] = by + this.dy * dd;
               zs[i] = bz + this.dz * dd;
               w[i] = (0.16F + this.width * 0.12F) * (1.0F - 0.6F * u);
               h[i] = 1.5F - 0.3F * u;
               a[i] = core * (1.0F - u * 0.7F) * VfxWorld.nearFade(xs[i], ys[i], zs[i]);
            }

            MeshWriter.strip(f.mesh(MeshTex.LANCE), n, xs, ys, zs, w, h, a, -s * 0.5F, 0.3F, 16777215, 0, 2.2F);
         }

         BufferBuilder d = f.distortion();
         if (d != null) {
            VfxSprites.Sprite glow = VfxSprites.GLOW;
            float haze = Math.max(0.0F, 1.0F - s / 11.0F);

            for (int k = 1; k <= 3; k++) {
               float u = (float)k / 3.0F * len;
               MeshWriter.distortQuad(
                  d,
                  bx + this.dx * u,
                  by + this.dy * u,
                  bz + this.dz * u,
                  0.8F + this.width * 0.8F * (float)k / 3.0F,
                  0.8F * haze,
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
                  (float)k * 0.27F + f.time * 0.006F,
                  0.3F
               );
            }
         }
      }
   }

   @Override
   public double x() {
      return this.released >= 0 ? this.from.x + (double)(this.dx * this.length) * 0.5 : this.mx;
   }

   @Override
   public double y() {
      return this.released >= 0 ? this.from.y + (double)(this.dy * this.length) * 0.5 : this.my;
   }

   @Override
   public double z() {
      return this.released >= 0 ? this.from.z + (double)(this.dz * this.length) * 0.5 : this.mz;
   }
}
