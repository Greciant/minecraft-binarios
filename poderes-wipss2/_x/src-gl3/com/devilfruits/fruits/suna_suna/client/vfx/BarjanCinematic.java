package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.core.physics.PhysicsWorld;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;

public class BarjanCinematic extends SandCinematic {
   private static final int SEGMENTS = 26;
   private static final int TRAIL = 8;
   private static final float ARC = 1.08F;
   private final Vec3 velocity;
   private final Vec3 dir;
   private final Vec3 side;
   private final Vec3 normal;
   private final float half;
   private final int lifetime;
   private double cx;
   private double cy;
   private double cz;
   private double pcx;
   private double pcy;
   private double pcz;
   private int stoppedAt = -1;
   private final double[] px = new double[27];
   private final double[] py = new double[27];
   private final double[] pz = new double[27];
   private final float[] ax = new float[27];
   private final float[] ay = new float[27];
   private final float[] az = new float[27];
   private final float[] w = new float[27];
   private final float[] a = new float[27];
   private final double[] q00 = new double[3];
   private final double[] q01 = new double[3];
   private final double[] q11 = new double[3];
   private final double[] q10 = new double[3];

   public BarjanCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.velocity = params.dir();
      this.dir = this.velocity.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : this.velocity.normalize();
      Vec3 flat = new Vec3(this.dir.x, 0.0, this.dir.z);
      this.side = flat.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(-flat.z, 0.0, flat.x).normalize();
      this.normal = this.side.cross(this.dir).normalize();
      this.half = Math.max(1.0F, params.power() * 0.5F);
      this.lifetime = params.extra();
      Vec3 s = params.pos();
      this.cx = this.pcx = s.x;
      this.cy = this.pcy = s.y;
      this.cz = this.pcz = s.z;
   }

   @Override
   protected boolean followsCaster() {
      return false;
   }

   public static void stop(long seed, Vec3 at) {
      BarjanCinematic c = VfxDirector.find(BarjanCinematic.class, b -> b.seed == seed);
      if (c != null) {
         c.stopAt(at);
      }
   }

   private void stopAt(Vec3 at) {
      if (this.stoppedAt < 0) {
         this.stoppedAt = this.age;
         this.cx = this.pcx = at.x;
         this.cy = this.pcy = at.y;
         this.cz = this.pcz = at.z;
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            int tint = lit(level, this.cx, this.cy, this.cz);
            float spread = spread((float)this.age);
            int n = VfxSettings.count(16.0F);

            for (int i = 0; i < n; i++) {
               Vec3 p = this.lead(this.random.nextDouble() * 2.0 - 1.0, this.cx, this.cy, this.cz, spread);
               puff(this.random, p.x, p.y, p.z, this.dir.x * 0.08 + this.spread(0.06), 0.03, this.dir.z * 0.08 + this.spread(0.06), tint).size(1.3F);
               grains(
                  this.random,
                  p.x,
                  p.y,
                  p.z,
                  this.dir.x * 0.15 + this.spread(0.2),
                  0.08 + this.random.nextDouble() * 0.15,
                  this.dir.z * 0.15 + this.spread(0.2),
                  tint
               );
            }
         }
      }
   }

   private static float spread(float t) {
      return 0.35F + 0.65F * Math.min(1.0F, t / 3.0F);
   }

   private Vec3 lead(double s, double x0, double y0, double z0, float spread) {
      double h = (double)(this.half * spread);
      double trail = 0.45 * h * s * s;
      return new Vec3(
         x0 + this.side.x * s * h - this.dir.x * trail, y0 + this.side.y * s * h - this.dir.y * trail, z0 + this.side.z * s * h - this.dir.z * trail
      );
   }

   private double depth(double s, float spread) {
      return 0.45 * (double)this.half * (double)spread * Math.pow(Math.max(0.0, 1.0 - s * s), 0.75);
   }

   private Vec3 across(double s, float spread) {
      double h = (double)(this.half * spread);
      Vec3 tangent = this.side.scale(h).subtract(this.dir.scale(0.9 * h * s));
      Vec3 c = this.normal.cross(tangent);
      c = c.lengthSqr() < 1.0E-8 ? this.dir : c.normalize();
      return c.dot(this.dir) < 0.0 ? c.reverse() : c;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.pcx = this.cx;
      this.pcy = this.cy;
      this.pcz = this.cz;
      if (this.stoppedAt < 0 && this.age <= this.lifetime + 10) {
         Vec3 from = new Vec3(this.cx, this.cy, this.cz);
         Vec3 to = from.add(this.velocity);
         if (PhysicsWorld.clip(level, new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, CollisionContext.empty())).getType() != Type.MISS) {
            this.stopAt(from);
         } else {
            this.cx = to.x;
            this.cy = to.y;
            this.cz = to.z;
            int tint = lit(level, this.cx, this.cy, this.cz);
            float sp = spread((float)this.age);
            Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            int n = VfxSettings.count(8.0F);

            for (int i = 0; i < n; i++) {
               double s = this.random.nextDouble() * 2.0 - 1.0;
               Vec3 p = this.lead(s, this.cx, this.cy, this.cz, sp).subtract(this.dir.scale(this.depth(s, sp) * (0.6 + this.random.nextDouble() * 0.5)));
               if (!(p.distanceToSqr(camera) < 4.0)) {
                  if (i % 3 == 0) {
                     puff(this.random, p.x, p.y, p.z, this.velocity.x * 0.15, -0.01, this.velocity.z * 0.15, tint).size(0.9F);
                  } else {
                     grains(this.random, p.x, p.y, p.z, this.velocity.x * 0.3 + this.spread(0.05), -0.03, this.velocity.z * 0.3 + this.spread(0.05), tint);
                  }
               }
            }

            if (this.age % 2 == 0) {
               Vec3 p = this.lead(this.random.nextDouble() * 1.6 - 0.8, this.cx, this.cy, this.cz, sp);
               streak(p.x, p.y, p.z, this.velocity.x * 0.7, 0.0, this.velocity.z * 0.7, tint);
            }

            int m = VfxSettings.count(5.0F);

            for (int ix = 0; ix < m; ix++) {
               double s = ((double)ix + this.random.nextDouble()) / (double)Math.max(1, m) * 2.0 - 1.0;
               Vec3 p = this.lead(s * 0.95, this.cx, this.cy, this.cz, sp).subtract(this.dir.scale(this.depth(s, sp) * 0.3));
               if (p.distanceToSqr(camera) > 6.0) {
                  puff(this.random, p.x, p.y + this.spread(0.15), p.z, this.velocity.x * 0.2, 0.01, this.velocity.z * 0.2, tint)
                     .size(1.1F + 0.5F * (float)(1.0 - s * s));
               }
            }

            for (int ixx = -2; ixx <= 2; ixx++) {
               Vec3 p = this.lead((double)ixx * 0.42, this.cx, this.cy, this.cz, sp).subtract(this.dir.scale(this.depth((double)ixx * 0.42, sp) * 0.4));
               shadow(level, p.x, p.y, p.z, 2.2F, 0.8F);
            }

            double g = VfxWorld.ground(level, this.cx, this.cy, this.cz, 4);
            if (!Double.isNaN(g) && this.cy - g < 3.2) {
               float near = (float)(1.0 - (this.cy - g) / 3.2);
               int lift = VfxSettings.count(3.0F * near);

               for (int ixx = 0; ixx < lift; ixx++) {
                  double s = this.random.nextDouble() * 2.0 - 1.0;
                  Vec3 p = this.lead(s, this.cx, g, this.cz, sp).subtract(this.dir.scale(this.depth(s, sp) * this.random.nextDouble()));
                  plume(
                        this.random,
                        p.x,
                        g + 0.2,
                        p.z,
                        this.velocity.x * 0.08 + this.side.x * s * 0.08,
                        0.05 + 0.05 * (double)near,
                        this.velocity.z * 0.08 + this.side.z * s * 0.08,
                        tint
                     )
                     .size(0.8F + 0.6F * near)
                     .alpha(0.85F);
               }
            }
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      if (this.stoppedAt < 0) {
         float pt = f.partial;
         double x0 = Mth.lerp((double)pt, this.pcx, this.cx);
         double y0 = Mth.lerp((double)pt, this.pcy, this.cy);
         double z0 = Mth.lerp((double)pt, this.pcz, this.cz);
         float rx = f.rx(x0);
         float ry = f.ry(y0);
         float rz = f.rz(z0);
         float d2 = rx * rx + ry * ry + rz * rz;
         if (!(d2 < 4.0F)) {
            float near = Mth.clamp((Mth.sqrt(d2) - 2.0F) / 1.5F, 0.0F, 1.0F);
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
               float t = (float)this.age + pt;
               float sp = spread(t);
               int tint = lit(level, x0, y0, z0);
               float arc = this.half * sp * 1.08F;
               if (f.allowMesh()) {
                  BufferBuilder b = f.mesh(MeshTex.SAND_SHEET);
                  int rows = 4;

                  for (int side2 = -1; side2 <= 1; side2 += 2) {
                     for (int i = 0; i < 26; i++) {
                        double s0 = ((double)i / 26.0 * 2.0 - 1.0) * 0.97;
                        double s1 = ((double)(i + 1) / 26.0 * 2.0 - 1.0) * 0.97;
                        float u0 = (float)Math.abs(s0) * arc * 0.5F - t * 0.25F;
                        float u1 = (float)Math.abs(s1) * arc * 0.5F - t * 0.25F;

                        for (int r = 0; r < rows; r++) {
                           float c0 = (float)r / (float)rows;
                           float c1 = (float)(r + 1) / (float)rows;
                           this.lens(this.q00, s0, c0, side2, x0, y0, z0, sp);
                           this.lens(this.q01, s1, c0, side2, x0, y0, z0, sp);
                           this.lens(this.q11, s1, c1, side2, x0, y0, z0, sp);
                           this.lens(this.q10, s0, c1, side2, x0, y0, z0, sp);
                           float fa = near * (1.0F - smooth(c0));
                           float fb = near * (1.0F - smooth(c1));
                           SandDraw.quad(b, f, this.q00, this.q01, this.q11, this.q10, u0, c0 * 0.6F, u1, c1 * 0.6F, tint, fa, fb);
                        }
                     }
                  }
               }

               for (int i = 0; i <= 26; i++) {
                  double s = ((double)i / 26.0 * 2.0 - 1.0) * 0.95;
                  Vec3 p = this.lead(s, x0, y0, z0, sp).add(this.across(s, sp).scale(0.04));
                  this.px[i] = p.x;
                  this.py[i] = p.y;
                  this.pz[i] = p.z;
                  this.w[i] = (float)(0.05 + 0.07 * (1.0 - Math.abs(s)));
                  this.a[i] = 0.95F * near;
               }

               SandDraw.stream(f, 27, this.px, this.py, this.pz, this.w, this.a, -t * 0.4F, 0.25F, scale(tint, 1.15F));

               for (int i = 0; i <= 26; i++) {
                  double s = ((double)i / 26.0 * 2.0 - 1.0) * 0.95;
                  this.w[i] = (float)(0.16 + 0.22 * (1.0 - s * s));
                  this.a[i] = (float)(0.75 * Math.pow(1.0 - s * s, 0.3)) * near;
                  this.py[i] = this.py[i] + 0.04;
               }

               SandDraw.stream(f, 27, this.px, this.py, this.pz, this.w, this.a, -t * 0.55F + 0.3F, 0.3F, tint);

               for (int layer = 0; layer < 2; layer++) {
                  for (int i = 0; i <= 26; i++) {
                     double s = ((double)i / 26.0 * 2.0 - 1.0) * 0.97;
                     double dep = this.depth(s, sp);
                     Vec3 p = this.lead(s, x0, y0, z0, sp)
                        .subtract(this.dir.scale(dep * (0.55 + 0.15 * (double)layer)))
                        .add(this.normal.scale(((double)layer - 0.5) * 0.12));
                     Vec3 c = this.across(s, sp);
                     this.px[i] = p.x;
                     this.py[i] = p.y;
                     this.pz[i] = p.z;
                     this.ax[i] = (float)c.x;
                     this.ay[i] = (float)c.y;
                     this.az[i] = (float)c.z;
                     this.w[i] = (float)(dep * 0.45 + 0.05);
                     this.a[i] = (float)((0.85 - 0.25 * (double)layer) * Math.pow(1.0 - s * s, 0.4)) * near;
                  }

                  SandDraw.band(
                     f,
                     MeshTex.SAND_STREAM,
                     27,
                     this.px,
                     this.py,
                     this.pz,
                     this.ax,
                     this.ay,
                     this.az,
                     this.w,
                     this.a,
                     -t * (0.45F + 0.1F * (float)layer) + (float)layer,
                     arc / 26.0F * 2.0F,
                     tint,
                     true
                  );
               }

               for (int horn = -1; horn <= 1; horn += 2) {
                  Vec3 tip = this.lead((double)horn * 0.96, x0, y0, z0, sp);

                  for (int i = 0; i < 8; i++) {
                     float q = (float)i / 7.0F;
                     Vec3 p = tip.subtract(this.dir.scale((double)q * 3.0))
                        .add(this.side.scale((double)((float)horn * q) * 0.45))
                        .add(this.normal.scale((double)(Mth.sin(t * 0.7F + q * 4.0F + (float)horn) * 0.12F)));
                     this.px[i] = p.x;
                     this.py[i] = p.y;
                     this.pz[i] = p.z;
                     this.w[i] = 0.1F + 0.2F * q;
                     this.a[i] = (1.0F - q) * 0.75F * near;
                  }

                  SandDraw.stream(f, 8, this.px, this.py, this.pz, this.w, this.a, -t * 0.5F + (float)horn, 0.35F, tint);
               }
            }
         }
      }
   }

   private static float smooth(float c) {
      float k = Mth.clamp((c - 0.55F) / 0.45F, 0.0F, 1.0F);
      return k * k * (3.0F - 2.0F * k);
   }

   private void lens(double[] out, double s, float c, int side, double x0, double y0, double z0, float sp) {
      Vec3 p = this.lead(s, x0, y0, z0, sp).subtract(this.dir.scale(this.depth(s, sp) * (double)c * 0.85));
      double thick = 0.24 * Math.pow(Math.max(0.0, 1.0 - s * s), 0.6) * Math.pow(Math.sin(Math.PI * Math.min(1.0, (double)c * 1.15)), 0.7) * (double)sp;
      out[0] = p.x + this.normal.x * thick * (double)side;
      out[1] = p.y + this.normal.y * thick * (double)side;
      out[2] = p.z + this.normal.z * thick * (double)side;
   }

   private static double[] arr(Vec3 v) {
      return new double[]{v.x, v.y, v.z};
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
}
