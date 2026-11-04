package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.core.physics.PhysicsWorld;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;

public class LaSpadaCinematic extends SandCinematic {
   private static final float LENGTH = 2.6F;
   private static final float WIDTH = 0.15F;
   private static final float THICK = 0.06F;
   private static final int SEG = 10;
   private static final int TRAIL = 8;
   private static final int FORM = 4;
   private final Vec3 velocity;
   private final Vec3 fwd;
   private final Vec3 side;
   private final Vec3 up;
   private final int lifetime;
   private double bx;
   private double by;
   private double bz;
   private double pbx;
   private double pby;
   private double pbz;
   private final double startX;
   private final double startY;
   private final double startZ;
   private final double[] hx = new double[8];
   private final double[] hy = new double[8];
   private final double[] hz = new double[8];
   private int recorded;
   private final double[] px = new double[24];
   private final double[] py = new double[24];
   private final double[] pz = new double[24];
   private final float[] w = new float[24];
   private final float[] a = new float[24];
   private final double[][] ring = new double[44][3];

   public LaSpadaCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.velocity = params.dir();
      this.lifetime = params.extra();
      this.fwd = this.velocity.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : this.velocity.normalize();
      Vec3 s = new Vec3(-this.fwd.z, 0.0, this.fwd.x);
      this.side = s.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : s.normalize();
      this.up = this.side.cross(this.fwd).normalize();
      Vec3 p = params.pos();
      this.bx = this.pbx = this.startX = p.x;
      this.by = this.pby = this.startY = p.y;
      this.bz = this.pbz = this.startZ = p.z;
      if (Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(p) > 6.25) {
         int tint = lit(level, this.bx, this.by, this.bz);

         for (int i = 0; i < VfxSettings.count(10.0F); i++) {
            Vec3 d = jitter(this.random, Vec3.ZERO, 1.0F).scale(0.7 + this.random.nextDouble() * 0.5);
            Vec3 t = d.cross(this.fwd).scale(0.15);
            grains(this.random, this.bx + d.x, this.by + d.y, this.bz + d.z, -d.x * 0.3 + t.x, -d.y * 0.3 + t.y, -d.z * 0.3 + t.z, tint).life(0.3F);
         }

         clouds(this.random, this.bx, this.by, this.bz, this.fwd.x * 0.04, 0.01, this.fwd.z * 0.04, 0.7F, 2.0F, tint);
      }
   }

   @Override
   protected boolean followsCaster() {
      return false;
   }

   public static void shatter(ClientLevel level, long seed, Vec3 at, Vec3 velocity, boolean body) {
      LaSpadaCinematic c = VfxDirector.find(LaSpadaCinematic.class, sx -> sx.seed == seed);
      if (c != null) {
         c.finish();
      }

      int tint = lit(level, at.x, at.y, at.z);
      RandomSource r = RandomSource.create(seed);
      Vec3 dir = velocity.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : velocity.normalize();
      burst(r, at, dir.scale(body ? 0.8 : -0.5).add(0.0, 0.35, 0.0), 0.55F, 0.42F, body ? 22.0F : 16.0F, 0.0F, tint);

      for (int i = 0; i < VfxSettings.count(body ? 7.0F : 5.0F); i++) {
         Vec3 d = jitter(r, body ? dir : dir.reverse().add(0.0, 0.5, 0.0), 0.5F);
         double s = 0.45 + r.nextDouble() * 0.4;
         streak(at.x, at.y, at.z, d.x * s, d.y * s + 0.05, d.z * s, tint).size(1.3F);
      }

      clouds(r, at.x, at.y, at.z, dir.x * (body ? 0.1 : -0.04), 0.02, dir.z * (body ? 0.1 : -0.04), body ? 1.3F : 1.0F, body ? 5.0F : 4.0F, tint);
      double g = VfxWorld.ground(level, at.x, at.y + 0.5, at.z, 3);
      if (!Double.isNaN(g) && at.y - g < 1.2) {
         decal(r, at.x, g, at.z, 1.6F, tint);
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.pbx = this.bx;
      this.pby = this.by;
      this.pbz = this.bz;
      if (this.age > this.lifetime + 2) {
         this.finish();
      } else {
         System.arraycopy(this.hx, 0, this.hx, 1, 7);
         System.arraycopy(this.hy, 0, this.hy, 1, 7);
         System.arraycopy(this.hz, 0, this.hz, 1, 7);
         this.hx[0] = this.bx;
         this.hy[0] = this.by;
         this.hz[0] = this.bz;
         this.recorded = Math.min(8, this.recorded + 1);
         Vec3 from = new Vec3(this.bx, this.by, this.bz);
         Vec3 to = from.add(this.velocity);
         if (PhysicsWorld.clip(level, new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, CollisionContext.empty())).getType() != Type.MISS) {
            this.finish();
         } else {
            this.bx = to.x;
            this.by = to.y;
            this.bz = to.z;
            if (Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(this.bx, this.by, this.bz) > 6.25) {
               int tint = lit(level, this.bx, this.by, this.bz);

               for (int i = 0; i < VfxSettings.count(4.0F); i++) {
                  double k = this.random.nextDouble();
                  double off = (double)((float)(this.random.nextBoolean() ? 1 : -1) * 0.15F) * 0.8;
                  grains(
                        this.random,
                        Mth.lerp(k, this.pbx, this.bx) + this.side.x * off,
                        Mth.lerp(k, this.pby, this.by) + this.spread(0.08),
                        Mth.lerp(k, this.pbz, this.bz) + this.side.z * off,
                        this.velocity.x * 0.12 + this.side.x * off * 0.3,
                        0.02,
                        this.velocity.z * 0.12 + this.side.z * off * 0.3,
                        tint
                     )
                     .size(0.8F);
               }

               if (this.age % 2 == 0) {
                  puff(this.random, this.pbx, this.pby, this.pbz, this.velocity.x * 0.05, 0.01, this.velocity.z * 0.05, tint).size(0.55F).alpha(0.8F);
               }

               streak(this.bx, this.by, this.bz, this.velocity.x * 0.9, this.velocity.y * 0.9, this.velocity.z * 0.9, tint).size(1.2F);
               shadow(level, this.bx, this.by, this.bz, 0.9F, 0.5F);
            }
         }
      }
   }

   private static float profile(float s) {
      return s < 0.22F ? 0.35F + 0.65F * (s / 0.22F) : (float)Math.pow((double)((1.0F - s) / 0.78F), 0.7);
   }

   @Override
   public void render(VfxFrame f) {
      float pt = f.partial;
      double cx = Mth.lerp((double)pt, this.pbx, this.bx);
      double cy = Mth.lerp((double)pt, this.pby, this.by);
      double cz = Mth.lerp((double)pt, this.pbz, this.bz);
      float rx = f.rx(cx);
      float ry = f.ry(cy);
      float rz = f.rz(cz);
      float d2 = rx * rx + ry * ry + rz * rz;
      if (!(d2 < 1.2F)) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            float t = (float)this.age + pt;
            float form = Mth.clamp(t / 4.0F, 0.0F, 1.0F);
            float grow = 1.0F - (1.0F - form) * (1.0F - form);
            int tint = lit(level, cx, cy, cz);
            float len = 2.6F * grow;
            float roll = t * 0.35F + (float)(this.seed & 7L);
            Vec3 ax = this.up.scale((double)Mth.cos(roll)).add(this.side.scale((double)Mth.sin(roll)));
            Vec3 bxv = this.up.scale((double)(-Mth.sin(roll))).add(this.side.scale((double)Mth.cos(roll)));
            if (form < 1.0F) {
               float k = 1.0F - form;
               SandDraw.billboard(
                  f,
                  VfxSprites.SAND_CLUMP,
                  t * 1.2F,
                  cx - this.fwd.x * (double)len * 0.3,
                  cy - this.fwd.y * (double)len * 0.3,
                  cz - this.fwd.z * (double)len * 0.3,
                  0.9F + 0.5F * k,
                  t * 0.4F,
                  scale(tint, 0.8F),
                  0.85F * k
               );
            }

            if (f.allowMesh()) {
               BufferBuilder b = f.mesh(MeshTex.SAND_SHEET);

               for (int i = 0; i <= 10; i++) {
                  float s = (float)i / 10.0F;
                  double l = ((double)s - 0.42) * (double)len;
                  float wd = 0.15F * profile(s) * (0.6F + 0.4F * grow);
                  float th = 0.06F * profile(s) * (0.6F + 0.4F * grow);
                  double ox = cx + this.fwd.x * l;
                  double oy = cy + this.fwd.y * l;
                  double oz = cz + this.fwd.z * l;
                  set(this.ring[i * 4], ox + bxv.x * (double)wd, oy + bxv.y * (double)wd, oz + bxv.z * (double)wd);
                  set(this.ring[i * 4 + 1], ox + ax.x * (double)th, oy + ax.y * (double)th, oz + ax.z * (double)th);
                  set(this.ring[i * 4 + 2], ox - bxv.x * (double)wd, oy - bxv.y * (double)wd, oz - bxv.z * (double)wd);
                  set(this.ring[i * 4 + 3], ox - ax.x * (double)th, oy - ax.y * (double)th, oz - ax.z * (double)th);
               }

               for (int i = 0; i < 10; i++) {
                  float s0 = (float)i / 10.0F;
                  float s1 = (float)(i + 1) / 10.0F;
                  float d0 = 0.08F + 0.6F * Math.max(0.0F, 1.0F - s0 / 0.25F) + 0.4F * (1.0F - grow);
                  float d1 = 0.08F + 0.6F * Math.max(0.0F, 1.0F - s1 / 0.25F) + 0.4F * (1.0F - grow);
                  float u0 = s0 * 1.4F + t * 0.45F;
                  float u1 = s1 * 1.4F + t * 0.45F;

                  for (int k = 0; k < 4; k++) {
                     int k1 = (k + 1) % 4;
                     float lit = k != 0 && k != 3 ? 0.92F : 1.18F;
                     SandDraw.quadD(
                        b,
                        f,
                        this.ring[i * 4 + k],
                        this.ring[(i + 1) * 4 + k],
                        this.ring[(i + 1) * 4 + k1],
                        this.ring[i * 4 + k1],
                        u0,
                        (float)k * 0.25F,
                        u1,
                        (float)k * 0.25F + 0.25F,
                        scale(tint, lit * (0.92F + 0.2F * s1)),
                        1.0F,
                        1.0F,
                        d0,
                        d1,
                        d1,
                        d0
                     );
                  }
               }
            }

            for (int k = -1; k <= 1; k += 2) {
               int n = 6;

               for (int i = 0; i < n; i++) {
                  float s = 0.25F + 0.75F * (float)i / ((float)n - 1.0F);
                  double l = ((double)s - 0.42) * (double)len;
                  float wd = 0.15F * profile(s) * (0.6F + 0.4F * grow) * (float)k;
                  this.px[i] = cx + this.fwd.x * l + bxv.x * (double)wd;
                  this.py[i] = cy + this.fwd.y * l + bxv.y * (double)wd;
                  this.pz[i] = cz + this.fwd.z * l + bxv.z * (double)wd;
                  this.w[i] = 0.045F * (1.0F - s * 0.5F);
                  this.a[i] = grow;
               }

               SandDraw.stream(f, n, this.px, this.py, this.pz, this.w, this.a, t * 0.5F, 0.3F, scale(tint, 1.5F));
            }

            int n = 8;

            for (int i = 0; i < n; i++) {
               float s = (float)i / ((float)n - 1.0F);
               double l = ((double)s - 0.42) * (double)len * 1.08 - 0.15;
               this.px[i] = cx + this.fwd.x * l;
               this.py[i] = cy + this.fwd.y * l;
               this.pz[i] = cz + this.fwd.z * l;
               this.w[i] = (0.285F * profile(Math.min(1.0F, s * 1.05F)) + 0.04F) * grow;
               this.a[i] = 0.6F * Math.min(1.0F, s * 4.0F + 0.3F) * grow;
            }

            SandDraw.stream(f, n, this.px, this.py, this.pz, this.w, this.a, -t * 1.2F, 0.3F, scale(tint, 1.08F));

            for (int k = 0; k < 2; k++) {
               int nx = 12;

               for (int i = 0; i < nx; i++) {
                  float q = (float)i / ((float)nx - 1.0F);
                  double l = (0.62 - (double)q * 1.15) * (double)len;
                  float ang = roll * 2.0F + q * 6.0F + (float)k * (float) Math.PI;
                  float rad = (0.16F + 0.14F * q) * grow;
                  this.px[i] = cx + this.fwd.x * l + (ax.x * (double)Mth.cos(ang) + bxv.x * (double)Mth.sin(ang)) * (double)rad;
                  this.py[i] = cy + this.fwd.y * l + (ax.y * (double)Mth.cos(ang) + bxv.y * (double)Mth.sin(ang)) * (double)rad;
                  this.pz[i] = cz + this.fwd.z * l + (ax.z * (double)Mth.cos(ang) + bxv.z * (double)Mth.sin(ang)) * (double)rad;
                  this.w[i] = 0.07F + 0.1F * q;
                  this.a[i] = Math.min(1.0F, q * 6.0F) * (1.0F - q * 0.7F) * 0.6F * grow;
               }

               SandDraw.stream(f, nx, this.px, this.py, this.pz, this.w, this.a, -t * 0.9F + (float)k, 0.25F, tint);
            }

            double gx = cx - this.fwd.x * (double)len * 0.42;
            double gy = cy - this.fwd.y * (double)len * 0.42;
            double gz = cz - this.fwd.z * (double)len * 0.42;
            int nx = 0;
            this.px[nx] = gx;
            this.py[nx] = gy;
            this.pz[nx] = gz;
            this.w[nx] = 0.1F * grow;
            this.a[nx] = 0.9F;
            nx++;

            for (int i = 0; i < this.recorded && nx < 20; i++) {
               float q = (float)(i + 1) / 8.0F;
               double hx0 = this.hx[i] - this.fwd.x * (double)len * 0.42;
               double hy0 = this.hy[i] - this.fwd.y * (double)len * 0.42;
               double hz0 = this.hz[i] - this.fwd.z * (double)len * 0.42;
               if ((hx0 - this.startX) * this.fwd.x + (hy0 - this.startY) * this.fwd.y + (hz0 - this.startZ) * this.fwd.z < 0.0) {
                  break;
               }

               this.px[nx] = hx0;
               this.py[nx] = hy0;
               this.pz[nx] = hz0;
               this.w[nx] = (0.16F + 0.42F * q) * grow;
               this.a[nx] = (1.0F - q) * (1.0F - q * 0.6F) * 0.95F;
               nx++;
            }

            SandDraw.stream(f, nx, this.px, this.py, this.pz, this.w, this.a, -t * 0.6F, 0.35F, scale(tint, 1.02F));

            for (int i = 0; i < nx; i++) {
               this.w[i] = this.w[i] * 0.22F;
               this.a[i] = this.a[i] * 0.9F;
            }

            SandDraw.stream(f, nx, this.px, this.py, this.pz, this.w, this.a, -t * 1.1F + 0.5F, 0.3F, scale(tint, 1.4F));
         }
      }
   }

   private static void set(double[] p, double x, double y, double z) {
      p[0] = x;
      p[1] = y;
      p[2] = z;
   }

   @Override
   public double x() {
      return this.bx;
   }

   @Override
   public double y() {
      return this.by;
   }

   @Override
   public double z() {
      return this.bz;
   }
}
