package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.core.physics.PhysicsWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;

public class PesadoCinematic extends SandCinematic {
   private static final int TRAIL = 10;
   private final float size;
   private final boolean heavy;
   private final int lifetime;
   private double bx;
   private double by;
   private double bz;
   private double pbx;
   private double pby;
   private double pbz;
   private double vx;
   private double vy;
   private double vz;
   private final double[] hx = new double[10];
   private final double[] hy = new double[10];
   private final double[] hz = new double[10];
   private int recorded;
   private final double[] px = new double[11];
   private final double[] py = new double[11];
   private final double[] pz = new double[11];
   private final float[] w = new float[11];
   private final float[] a = new float[11];
   private final double[] qx = new double[11];
   private final double[] qy = new double[11];
   private final double[] qz = new double[11];
   private final float[] qw = new float[11];

   public PesadoCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      Vec3 s = params.pos();
      this.bx = this.pbx = s.x;
      this.by = this.pby = s.y;
      this.bz = this.pbz = s.z;
      this.vx = params.dir().x;
      this.vy = params.dir().y;
      this.vz = params.dir().z;
      this.size = Math.max(0.2F, params.power());
      this.heavy = params.extra() > 0;
      this.lifetime = Math.max(1, Math.abs(params.extra()));
   }

   @Override
   protected boolean followsCaster() {
      return false;
   }

   public static void impact(long seed) {
      PesadoCinematic c = VfxDirector.find(PesadoCinematic.class, p -> p.seed == seed);
      if (c != null) {
         c.finish();
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > this.lifetime + 1) {
         this.finish();
      } else {
         this.pbx = this.bx;
         this.pby = this.by;
         this.pbz = this.bz;
         System.arraycopy(this.hx, 0, this.hx, 1, 9);
         System.arraycopy(this.hy, 0, this.hy, 1, 9);
         System.arraycopy(this.hz, 0, this.hz, 1, 9);
         this.hx[0] = this.bx;
         this.hy[0] = this.by;
         this.hz[0] = this.bz;
         this.recorded = Math.min(10, this.recorded + 1);
         Vec3 from = new Vec3(this.bx, this.by, this.bz);
         Vec3 to = new Vec3(this.bx + this.vx, this.by + this.vy, this.bz + this.vz);
         if (PhysicsWorld.clip(level, new ClipContext(from, to, Block.COLLIDER, Fluid.ANY, CollisionContext.empty())).getType() != Type.MISS) {
            this.finish();
         } else {
            this.bx = to.x;
            this.by = to.y;
            this.bz = to.z;
            if (this.heavy) {
               this.vy -= 0.003;
            }

            if (!(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(this.bx, this.by, this.bz) < 2.5600000000000005)) {
               int tint = lit(level, this.bx, this.by, this.bz);
               shadow(level, this.bx, this.by, this.bz, this.heavy ? this.size * 2.2F : 0.8F, this.heavy ? 0.9F : 0.4F);
               double speed = Math.sqrt(this.vx * this.vx + this.vy * this.vy + this.vz * this.vz);
               Vec3 dir = speed < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(this.vx / speed, this.vy / speed, this.vz / speed);
               if (this.heavy) {
                  float r = this.size * 0.55F;
                  int n = VfxSettings.count(3.0F + 4.0F * this.size);

                  for (int i = 0; i < n; i++) {
                     Vec3 o = jitter(this.random, Vec3.ZERO, 1.0F).scale((double)r);
                     Vec3 tang = dir.cross(o).scale(0.5);
                     grains(
                        this.random,
                        this.bx + o.x,
                        this.by + o.y,
                        this.bz + o.z,
                        this.vx * 0.35 + tang.x,
                        this.vy * 0.35 + tang.y + 0.02,
                        this.vz * 0.35 + tang.z,
                        tint
                     );
                  }

                  int m = VfxSettings.count(1.5F + this.size);

                  for (int i = 0; i < m; i++) {
                     double f = this.random.nextDouble();
                     puff(
                           this.random,
                           Mth.lerp(f, this.pbx, this.bx) + this.spread((double)r * 0.6),
                           Mth.lerp(f, this.pby, this.by) + this.spread((double)r * 0.6),
                           Mth.lerp(f, this.pbz, this.bz) + this.spread((double)r * 0.6),
                           this.vx * 0.05,
                           0.01,
                           this.vz * 0.05,
                           tint
                        )
                        .size(0.5F + this.size * 0.6F);
                  }

                  if (this.age % 2 == 0) {
                     streak(
                        this.bx + this.spread((double)r),
                        this.by + this.spread((double)r),
                        this.bz + this.spread((double)r),
                        this.vx * 0.7,
                        this.vy * 0.7,
                        this.vz * 0.7,
                        tint
                     );
                  }
               } else {
                  int n = VfxSettings.count(5.0F);

                  for (int i = 0; i < n; i++) {
                     double f = this.random.nextDouble();
                     double sx = Mth.lerp(f, this.pbx, this.bx);
                     double sy = Mth.lerp(f, this.pby, this.by);
                     double sz = Mth.lerp(f, this.pbz, this.bz);
                     if (i % 2 == 0) {
                        streak(sx + this.spread(0.1), sy + this.spread(0.1), sz + this.spread(0.1), this.vx * 0.6, this.vy * 0.6, this.vz * 0.6, tint);
                     } else {
                        grains(
                           this.random,
                           sx + this.spread(0.15),
                           sy + this.spread(0.15),
                           sz + this.spread(0.15),
                           this.vx * 0.2 + this.spread(0.05),
                           this.vy * 0.2 + 0.03,
                           this.vz * 0.2 + this.spread(0.05),
                           tint
                        );
                     }
                  }

                  if (this.age % 2 == 0) {
                     puff(this.random, this.bx, this.by, this.bz, this.vx * 0.04, 0.01, this.vz * 0.04, tint).size(0.45F).alpha(0.7F);
                  }
               }
            }
         }
      }
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
      if (!(rx * rx + ry * ry + rz * rz < (this.heavy ? 1.2F : 2.2F))) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            float t = (float)this.age + pt;
            int tint = lit(level, cx, cy, cz);
            double speed = Math.sqrt(this.vx * this.vx + this.vy * this.vy + this.vz * this.vz);
            float dx = (float)(this.vx / Math.max(1.0E-6, speed));
            float dy = (float)(this.vy / Math.max(1.0E-6, speed));
            float dz = (float)(this.vz / Math.max(1.0E-6, speed));
            float grow = Math.min(1.0F, t / 2.0F);
            int n = 0;
            this.px[n] = cx;
            this.py[n] = cy;
            this.pz[n] = cz;
            this.w[n] = (this.heavy ? this.size * 0.3F : 0.12F) * grow;
            this.a[n] = 0.9F;
            n++;

            for (int i = 0; i < this.recorded; i++) {
               float q = (float)(i + 1) / 10.0F;
               this.px[n] = this.hx[i];
               this.py[n] = this.hy[i];
               this.pz[n] = this.hz[i];
               this.w[n] = (this.heavy ? this.size * (0.32F + 0.4F * q) : 0.12F + 0.22F * q) * grow;
               this.a[n] = (1.0F - q) * (1.0F - q) * 0.85F;
               n++;
            }

            SandDraw.stream(f, n, this.px, this.py, this.pz, this.w, this.a, -t * 0.5F, 0.35F, tint);
            if (this.heavy) {
               float spin = t * 0.09F + (float)(this.seed & 15L);
               SandDraw.billboard(
                  f,
                  VfxSprites.SAND_CLUMP,
                  t * 0.7F + 8.0F,
                  cx - (double)(dx * this.size) * 0.2,
                  cy - (double)(dy * this.size) * 0.2,
                  cz - (double)(dz * this.size) * 0.2,
                  this.size * 1.9F * grow,
                  -spin * 0.8F,
                  scale(tint, 0.62F),
                  0.9F
               );
               SandDraw.billboard(f, VfxSprites.SAND_CLUMP, t * 0.8F, cx, cy, cz, this.size * 1.55F * grow, spin, scale(tint, 0.86F), 1.0F);
               SandDraw.billboard(
                  f,
                  VfxSprites.SAND_CLUMP,
                  t * 0.75F + 4.0F,
                  cx - (double)(dx * this.size) * 0.8,
                  cy - (double)(dy * this.size) * 0.8,
                  cz - (double)(dz * this.size) * 0.8,
                  this.size * 1.35F * grow,
                  spin * 1.4F + 2.0F,
                  scale(tint, 0.75F),
                  0.55F
               );
            } else {
               for (int s = 0; s < 2; s++) {
                  for (int i = 0; i < n; i++) {
                     float q = (float)i / (float)Math.max(1, n - 1);
                     float ang = t * 0.9F + q * 5.0F + (float)s * (float) Math.PI;
                     double ox = Math.cos((double)ang) * 0.13 * (double)q;
                     double oy = Math.sin((double)ang) * 0.13 * (double)q;
                     this.qx[i] = this.px[i] - (double)dz * ox;
                     this.qy[i] = this.py[i] + oy;
                     this.qz[i] = this.pz[i] + (double)dx * ox;
                     this.qw[i] = this.w[i] * 0.45F;
                  }

                  SandDraw.stream(f, n, this.qx, this.qy, this.qz, this.qw, this.a, -t * 0.8F + (float)s, 0.4F, tint);
               }
            }
         }
      }
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

   @Override
   public double cullDistance() {
      return 128.0;
   }
}
