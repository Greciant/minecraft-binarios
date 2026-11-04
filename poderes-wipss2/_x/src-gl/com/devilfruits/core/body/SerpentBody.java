package com.devilfruits.core.body;

import net.minecraft.util.Mth;

public final class SerpentBody {
   public static final int COIL_AFTER_TICKS = 60;
   private static final double TWO_PI = Math.PI * 2;
   public final float length;
   public final float spacing;
   public final float neck;
   public final float anchorHeight;
   public final float thickness;
   private final int capacity;
   private final double[] px;
   private final double[] py;
   private final double[] pz;
   private final double[] sx;
   private final double[] sy;
   private final double[] sz;
   private int newest;
   private int count;
   private double ax;
   private double ay;
   private double az;
   private double pax;
   private double pay;
   private double paz;
   private double travel;
   private double prevTravel;
   private int idleTicks;
   private float coil;
   private float prevCoil;
   private boolean coilFlat;
   private double coilCx;
   private double coilCy;
   private double coilCz;
   private double coilAngle;
   private double coilDir;
   private double coilY0;
   private boolean flying = true;
   private int age;
   private int action = 0;
   private int actionAge;
   private int actionTicks;
   private float actionSide = 1.0F;
   private float crushRadius;
   private double acx;
   private double acy;
   private double acz;
   private boolean twisting;
   private double twistFloor = -Double.MAX_VALUE;
   private float twist;
   private float prevTwist;
   private final double[] pivot = new double[3];
   private final double[] tmpC = new double[3];
   private final double[] tmpD = new double[3];
   private final double[] tmp = new double[3];
   private final double[] tmpA = new double[3];
   private final double[] tmpB = new double[3];
   public static final int NONE = 0;
   public static final int SWEEP = 1;
   public static final int CRUSH = 2;
   public static final int SWEEP_STRIKE_TICKS = 10;
   public static final float SWEEP_PIVOT = 0.35F;
   private static final int SWEEP_HOLD_TICKS = 4;
   private static final double SWEEP_DEGREES = 150.0;
   private static final double CRUSH_TURNS = 2.0;

   public SerpentBody(float length, float thickness) {
      this.length = length;
      this.thickness = thickness;
      this.spacing = 0.5F;
      this.neck = thickness * 1.25F;
      this.anchorHeight = thickness * 0.85F;
      this.capacity = (int)Math.ceil((double)((length + this.neck) / this.spacing)) + 6;
      this.px = new double[this.capacity];
      this.py = new double[this.capacity];
      this.pz = new double[this.capacity];
      this.sx = new double[this.capacity];
      this.sy = new double[this.capacity];
      this.sz = new double[this.capacity];
   }

   public void reset(double x, double y, double z, float yawDegrees, boolean flying) {
      this.flying = flying;
      this.ax = this.pax = x;
      this.ay = this.pay = y + (double)this.anchorHeight;
      this.az = this.paz = z;
      this.travel = this.prevTravel = 0.0;
      double fx = (double)(-Mth.sin(yawDegrees * (float) (Math.PI / 180.0)));
      double fz = (double)Mth.cos(yawDegrees * (float) (Math.PI / 180.0));
      this.count = this.capacity;
      this.newest = 0;

      for (int j = 0; j < this.capacity; j++) {
         this.px[j] = this.ax - fx * (double)this.spacing * (double)j;
         this.py[j] = this.ay;
         this.pz[j] = this.az - fz * (double)this.spacing * (double)j;
      }

      this.startCoil(yawDegrees);
      this.coil = this.prevCoil = 1.0F;
      this.idleTicks = 60;
      this.action = 0;
   }

   public void tick(double x, double y, double z, float yawDegrees, boolean flying) {
      this.age++;
      this.flying = flying;
      this.pax = this.ax;
      this.pay = this.ay;
      this.paz = this.az;
      this.prevTravel = this.travel;
      this.prevCoil = this.coil;
      this.prevTwist = this.twist;
      this.ax = x;
      this.ay = y + (double)this.anchorHeight;
      this.az = z;
      double moved = Math.sqrt(
         (this.ax - this.pax) * (this.ax - this.pax) + (this.ay - this.pay) * (this.ay - this.pay) + (this.az - this.paz) * (this.az - this.paz)
      );
      if (moved > 16.0) {
         this.reset(x, y, z, yawDegrees, flying);
      } else {
         this.travel += moved;
         this.idleTicks = moved < 0.03 ? this.idleTicks + 1 : 0;
         if (this.action != 0 && ++this.actionAge > this.actionTicks) {
            this.action = 0;
         }

         this.twist = this.twisting ? Math.min(1.0F, this.twist + 0.1F) : Math.max(0.0F, this.twist - 0.08F);
         if (this.action != 0 || this.twist > 0.0F) {
            this.idleTicks = 0;
         }

         if (this.coil > 0.0F && moved > 0.05) {
            this.reseedFromShape();
            this.coil = this.prevCoil = 0.0F;
         }

         this.record();
         if (this.idleTicks > 60) {
            if (this.coil <= 0.0F) {
               this.startCoil(yawDegrees);
            }

            this.coil = Math.min(1.0F, this.coil + 0.02F);
         }
      }
   }

   private void record() {
      double lx = this.px[this.newest];
      double ly = this.py[this.newest];
      double lz = this.pz[this.newest];
      double dx = this.ax - lx;
      double dy = this.ay - ly;
      double dz = this.az - lz;

      for (double d = Math.sqrt(dx * dx + dy * dy + dz * dz); d >= (double)this.spacing; d -= (double)this.spacing) {
         double k = (double)this.spacing / d;
         lx += dx * k;
         ly += dy * k;
         lz += dz * k;
         this.newest = (this.newest + 1) % this.capacity;
         this.px[this.newest] = lx;
         this.py[this.newest] = ly;
         this.pz[this.newest] = lz;
         this.count = Math.min(this.capacity, this.count + 1);
         dx = this.ax - lx;
         dy = this.ay - ly;
         dz = this.az - lz;
      }
   }

   private void startCoil(float yawDegrees) {
      double fx = (double)(-Mth.sin(yawDegrees * (float) (Math.PI / 180.0)));
      double fz = (double)Mth.cos(yawDegrees * (float) (Math.PI / 180.0));
      this.coilFlat = !this.flying;
      double radius = this.coilFlat ? 2.6 * (double)this.thickness : 3.0 * (double)this.thickness;
      double nx = this.ax - fx * (double)this.neck;
      double nz = this.az - fz * (double)this.neck;
      this.coilCx = nx - fx * radius;
      this.coilCz = nz - fz * radius;
      this.coilCy = this.ay;
      this.coilY0 = this.coilFlat ? this.ay - (double)this.anchorHeight : this.ay;
      this.coilAngle = Math.atan2(nz - this.coilCz, nx - this.coilCx);
      this.coilDir = 1.0;
   }

   private void coilPoint(double d, double[] out) {
      double s = Math.max(0.0, d - (double)this.neck);
      double r0 = (this.coilFlat ? 2.6 : 3.0) * (double)this.thickness;
      if (d < (double)this.neck) {
         double t = d / (double)this.neck;
         double cx = this.coilCx + Math.cos(this.coilAngle) * r0;
         double cz = this.coilCz + Math.sin(this.coilAngle) * r0;
         out[0] = Mth.lerp(t, this.ax, cx);
         out[1] = Mth.lerp(t, this.ay, this.coilFlat ? this.coilY0 + (double)this.thickness : this.ay);
         out[2] = Mth.lerp(t, this.az, cz);
      } else {
         if (this.coilFlat) {
            double b = 2.3 * (double)this.thickness / (Math.PI * 2);
            double theta = (Math.sqrt(r0 * r0 + 2.0 * b * s) - r0) / b;
            double r = r0 + b * theta;
            double a = this.coilAngle + this.coilDir * theta;
            out[0] = this.coilCx + Math.cos(a) * r;
            out[1] = this.coilY0 + (double)(this.thickness * (1.0F - 0.6F * (float)(s / (double)this.length)));
            out[2] = this.coilCz + Math.sin(a) * r;
         } else {
            double a = this.coilAngle + this.coilDir * s / r0;
            out[0] = this.coilCx + Math.cos(a) * r0;
            out[1] = this.coilCy - s * 2.2 * (double)this.thickness / ((Math.PI * 2) * r0);
            out[2] = this.coilCz + Math.sin(a) * r0;
         }
      }
   }

   private void reseedFromShape() {
      for (int j = 0; j < this.capacity; j++) {
         this.basePoint((double)((float)j * this.spacing), 1.0F, this.tmp);
         this.sx[j] = this.tmp[0];
         this.sy[j] = this.tmp[1];
         this.sz[j] = this.tmp[2];
      }

      for (int j = 0; j < this.capacity; j++) {
         int i = Math.floorMod(-j, this.capacity);
         this.px[i] = this.sx[j];
         this.py[i] = this.sy[j];
         this.pz[i] = this.sz[j];
      }

      this.newest = 0;
      this.count = this.capacity;
   }

   private void basePoint(double d, float partial, double[] out) {
      double hx = Mth.lerp((double)partial, this.pax, this.ax);
      double hy = Mth.lerp((double)partial, this.pay, this.ay);
      double hz = Mth.lerp((double)partial, this.paz, this.az);
      double nx = this.px[this.newest];
      double ny = this.py[this.newest];
      double nz = this.pz[this.newest];
      double r = Math.sqrt((hx - nx) * (hx - nx) + (hy - ny) * (hy - ny) + (hz - nz) * (hz - nz));
      if (d <= r && r > 1.0E-6) {
         double t = d / r;
         out[0] = hx + (nx - hx) * t;
         out[1] = hy + (ny - hy) * t;
         out[2] = hz + (nz - hz) * t;
      } else {
         double f = (d - r) / (double)this.spacing;
         int j = (int)Math.floor(f);
         double t = f - (double)j;
         int last = this.count - 1;
         if (j >= last) {
            int a = this.index(last - 1);
            int b = this.index(last);
            double over = (f - (double)last) * (double)this.spacing;
            double dx = this.px[b] - this.px[a];
            double dy = this.py[b] - this.py[a];
            double dz = this.pz[b] - this.pz[a];
            double len = Math.max(1.0E-6, Math.sqrt(dx * dx + dy * dy + dz * dz));
            out[0] = this.px[b] + dx / len * over;
            out[1] = this.py[b] + dy / len * over;
            out[2] = this.pz[b] + dz / len * over;
         } else {
            int i1 = this.index(j);
            int i2 = this.index(j + 1);
            int i3 = this.index(Math.min(last, j + 2));
            double p0x;
            double p0y;
            double p0z;
            if (j == 0) {
               p0x = hx;
               p0y = hy;
               p0z = hz;
            } else {
               int i0 = this.index(j - 1);
               p0x = this.px[i0];
               p0y = this.py[i0];
               p0z = this.pz[i0];
            }

            out[0] = catmull(p0x, this.px[i1], this.px[i2], this.px[i3], t);
            out[1] = catmull(p0y, this.py[i1], this.py[i2], this.py[i3], t);
            out[2] = catmull(p0z, this.pz[i1], this.pz[i2], this.pz[i3], t);
         }
      }

      float c = Mth.lerp(partial, this.prevCoil, this.coil);
      if (c > 0.0F) {
         this.coilPoint(d, this.tmpB);
         out[0] += (this.tmpB[0] - out[0]) * (double)c;
         out[1] += (this.tmpB[1] - out[1]) * (double)c;
         out[2] += (this.tmpB[2] - out[2]) * (double)c;
      }
   }

   private int index(int j) {
      return Math.floorMod(this.newest - j, this.capacity);
   }

   private static double catmull(double p0, double p1, double p2, double p3, double t) {
      double t2 = t * t;
      double t3 = t2 * t;
      return 0.5 * (2.0 * p1 + (-p0 + p2) * t + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3);
   }

   public float radiusAt(float t) {
      float r;
      if (t < 0.06F) {
         r = Mth.lerp(t / 0.06F, 0.72F, 0.9F);
      } else if (t < 0.32F) {
         r = Mth.lerp((t - 0.06F) / 0.26F, 0.9F, 1.0F);
      } else {
         float u = (t - 0.32F) / 0.68F;
         r = Mth.lerp(u * u, 1.0F, 0.18F);
      }

      return r * this.thickness;
   }

   public void point(double d, float partial, double[] out) {
      this.prepare(partial);
      this.shapePoint(d, partial, out);
      double ox = out[0];
      double oy = out[1];
      double oz = out[2];
      this.shapePoint(Math.max(0.0, d - 0.5), partial, this.tmpA);
      this.shapePoint(d + 0.5, partial, this.tmpD);
      this.offset(d, partial, this.tmpA[0] - this.tmpD[0], this.tmpA[1] - this.tmpD[1], this.tmpA[2] - this.tmpD[2], this.tmp);
      out[0] = ox + this.tmp[0];
      out[1] = oy + this.tmp[1];
      out[2] = oz + this.tmp[2];
   }

   private void offset(double d, float partial, double tx, double ty, double tz, double[] out) {
      out[0] = out[1] = out[2] = 0.0;
      float c = Mth.lerp(partial, this.prevCoil, this.coil);
      double dd = d - (double)this.neck;
      if (!(dd <= 0.0) && !(c >= 1.0F)) {
         double len = Math.sqrt(tx * tx + ty * ty + tz * tz);
         if (!(len < 1.0E-6)) {
            tx /= len;
            ty /= len;
            tz /= len;
            double sx = -tz;
            double sl = Math.sqrt(sx * sx + tx * tx);
            if (!(sl < 1.0E-4)) {
               sx /= sl;
               double sz = tx / sl;
               double ux = -sz * ty;
               double uy = sz * tx - sx * tz;
               double uz = sx * ty;
               double env = smooth(dd / (3.5 * (double)this.thickness))
                  * (0.75 + 0.25 * (1.0 - dd / (double)this.length))
                  * (double)(1.0F - c)
                  * (double)(1.0F - Math.max(this.actionWeight(partial), Mth.lerp(partial, this.prevTwist, this.twist)));
               double tr = Mth.lerp((double)partial, this.prevTravel, this.travel);
               double time = (double)((float)this.age + partial);
               double lateral = (this.flying ? 0.85 : 1.2) * (double)this.thickness;
               double lambda = (this.flying ? 20.0 : 12.0) * (double)this.thickness;
               double side = lateral * Math.sin((Math.PI * 2) * (tr - d) / lambda)
                  + 0.25 * (double)this.thickness * Math.sin(time * 0.06 + dd * 0.35 / (double)this.thickness);
               double up = this.flying ? 0.5 * (double)this.thickness * Math.sin((Math.PI * 2) * (tr - d) / (27.0 * (double)this.thickness) + 1.3) : 0.0;
               out[0] = (sx * side + ux * up) * env;
               out[1] = uy * up * env;
               out[2] = (sz * side + uz * up) * env;
            }
         }
      }
   }

   private static double smooth(double x) {
      if (x <= 0.0) {
         return 0.0;
      } else {
         return x >= 1.0 ? 1.0 : x * x * (3.0 - 2.0 * x);
      }
   }

   public void sample(BodyFrames out, int sections, float partial) {
      int n = Math.min(sections, out.capacity);
      out.count = n;
      this.prepare(partial);

      for (int i = 0; i < n; i++) {
         double d = (double)this.neck + (double)(this.length * (float)i) / ((double)n - 1.0);
         this.shapePoint(d, partial, this.tmp);
         out.x[i] = this.tmp[0];
         out.y[i] = this.tmp[1];
         out.z[i] = this.tmp[2];
         out.distance[i] = (float)d;
         out.radius[i] = this.radiusAt((float)i / ((float)n - 1.0F));
      }

      for (int i = 0; i < n; i++) {
         int a = Math.max(0, i - 1);
         int b = Math.min(n - 1, i + 1);
         this.offset((double)out.distance[i], partial, out.x[a] - out.x[b], out.y[a] - out.y[b], out.z[a] - out.z[b], this.tmp);
         out.ux[i] = (float)this.tmp[0];
         out.uy[i] = (float)this.tmp[1];
         out.uz[i] = (float)this.tmp[2];
      }

      for (int i = 0; i < n; i++) {
         out.x[i] = out.x[i] + (double)out.ux[i];
         out.y[i] = out.y[i] + (double)out.uy[i];
         out.z[i] = out.z[i] + (double)out.uz[i];
      }

      this.frames(out);
   }

   private void frames(BodyFrames f) {
      int n = f.count;

      for (int i = 0; i < n; i++) {
         int a = Math.max(0, i - 1);
         int b = Math.min(n - 1, i + 1);
         float tx = (float)(f.x[a] - f.x[b]);
         float ty = (float)(f.y[a] - f.y[b]);
         float tz = (float)(f.z[a] - f.z[b]);
         float len = Mth.sqrt(tx * tx + ty * ty + tz * tz);
         if (len < 1.0E-5F) {
            tx = i > 0 ? f.tx[i - 1] : 0.0F;
            ty = i > 0 ? f.ty[i - 1] : 0.0F;
            tz = i > 0 ? f.tz[i - 1] : 1.0F;
            len = 1.0F;
         }

         f.tx[i] = tx / len;
         f.ty[i] = ty / len;
         f.tz[i] = tz / len;
      }

      float ux = 0.0F;
      float uy = 1.0F;
      float uz = 0.0F;

      for (int i = 0; i < n; i++) {
         float tx = f.tx[i];
         float ty = f.ty[i];
         float tz = f.tz[i];
         float dot = ux * tx + uy * ty + uz * tz;
         ux -= dot * tx;
         uy -= dot * ty;
         uz -= dot * tz;
         float len = Mth.sqrt(ux * ux + uy * uy + uz * uz);
         if (len < 1.0E-4F) {
            float var19 = Math.abs(tx) < 0.9F ? 1.0F : 0.0F;
            float var23 = 0.0F;
            float var27 = Math.abs(tx) < 0.9F ? 0.0F : 1.0F;
            dot = var19 * tx + var23 * ty + var27 * tz;
            ux = var19 - dot * tx;
            uy = var23 - dot * ty;
            uz = var27 - dot * tz;
            len = Mth.sqrt(ux * ux + uy * uy + uz * uz);
         }

         ux /= len;
         uy /= len;
         uz /= len;
         float wl = Mth.sqrt(Math.max(0.0F, 1.0F - ty * ty));
         float k = (float)smooth((double)((wl - 0.15F) / 0.35F));
         if (k > 0.0F) {
            float wx = -ty * tx / wl;
            float wy = (1.0F - ty * ty) / wl;
            float wz = -ty * tz / wl;
            ux += (wx - ux) * k;
            uy += (wy - uy) * k;
            uz += (wz - uz) * k;
            len = Mth.sqrt(ux * ux + uy * uy + uz * uz);
            ux /= len;
            uy /= len;
            uz /= len;
         }

         f.ux[i] = ux;
         f.uy[i] = uy;
         f.uz[i] = uz;
         f.sx[i] = ty * uz - tz * uy;
         f.sy[i] = tz * ux - tx * uz;
         f.sz[i] = tx * uy - ty * ux;
      }
   }

   public void sweep(int ticks, float side) {
      this.begin(1, ticks);
      this.actionSide = side < 0.0F ? -1.0F : 1.0F;
   }

   public void crush(int ticks, double x, double y, double z, float radius) {
      this.begin(2, ticks);
      this.acx = x;
      this.acy = y;
      this.acz = z;
      this.crushRadius = radius;
   }

   public void setTwisting(boolean on, double floorY) {
      this.twisting = on;
      this.twistFloor = floorY;
   }

   private void begin(int type, int ticks) {
      if (this.coil > 0.0F) {
         this.reseedFromShape();
         this.coil = this.prevCoil = 0.0F;
      }

      this.action = type;
      this.actionAge = 0;
      this.actionTicks = Math.max(2, ticks);
   }

   public int action() {
      return this.action;
   }

   public float actionTime(float partial) {
      return this.action == 0 ? 0.0F : (float)this.actionAge + partial;
   }

   public float twist(float partial) {
      return Mth.lerp(partial, this.prevTwist, this.twist);
   }

   public static double sweepCurve(float t, int ticks) {
      if (t <= 10.0F) {
         double u = 1.0 - (double)t / 10.0;
         return 1.0 - u * u * u;
      } else {
         return t <= 14.0F ? 1.0 : 1.0 - smooth((double)(t - 10.0F - 4.0F) / (double)Math.max(1, ticks - 10 - 4));
      }
   }

   private float actionWeight(float partial) {
      if (this.action == 0) {
         return 0.0F;
      } else {
         float t = (float)this.actionAge + partial;
         return (float)Math.min(smooth((double)t / (this.action == 1 ? 3.0 : 6.0)), smooth((double)((float)this.actionTicks - t) / 8.0));
      }
   }

   private void prepare(float partial) {
      if (this.action == 1) {
         this.basePoint((double)(this.neck + this.length * 0.35F), partial, this.pivot);
      }
   }

   private void shapePoint(double d, float partial, double[] out) {
      this.basePoint(d, partial, out);
      if (this.action == 1) {
         this.sweepShape(d, partial, out);
      } else if (this.action == 2) {
         this.crushShape(d, partial, out);
      }

      float tw = Mth.lerp(partial, this.prevTwist, this.twist);
      if (tw > 0.0F) {
         this.twisterShape(d, partial, tw, out);
      }
   }

   private void sweepShape(double d, float partial, double[] out) {
      double dp = (double)(this.neck + this.length * 0.35F);
      if (!(d <= dp)) {
         float t = (float)this.actionAge + partial;
         double k = smooth((d - dp) / ((double)this.length * 0.25));
         double angle = (double)this.actionSide * Math.toRadians(150.0) * sweepCurve(t, this.actionTicks) * k;
         double vx = out[0] - this.pivot[0];
         double vz = out[2] - this.pivot[2];
         double c = Math.cos(angle);
         double s = Math.sin(angle);
         out[0] = this.pivot[0] + vx * c + vz * s;
         out[2] = this.pivot[2] - vx * s + vz * c;
         out[1] += 1.6 * (double)this.thickness * Math.sin(Math.PI * Math.min(1.0, (double)(t / (float)this.actionTicks))) * k;
      }
   }

   private void crushShape(double d, float partial, double[] out) {
      float t = (float)this.actionAge + partial;
      double w = Math.min(smooth((double)t / 6.0), smooth((double)((float)this.actionTicks - t) / 8.0));
      if (!(w <= 0.0)) {
         double hx = Mth.lerp((double)partial, this.pax, this.ax);
         double hy = Mth.lerp((double)partial, this.pay, this.ay);
         double hz = Mth.lerp((double)partial, this.paz, this.az);
         double rc = Math.max((double)this.crushRadius + (double)this.thickness * 0.55, (double)this.thickness * 1.9)
            * (1.0 + 1.4 * (1.0 - smooth((double)t / 10.0)));
         if (t > 10.0F) {
            rc *= 1.0 - 0.05 * Math.sin((double)(t - 10.0F) * 0.8);
         }

         double rise = 2.3 * (double)this.thickness;
         double turn = Math.sqrt((Math.PI * 2) * rc * (Math.PI * 2) * rc + rise * rise);
         double helix = 2.0 * turn;
         double a0 = Math.atan2(hz - this.acz, hx - this.acx) + (double)t * 0.04;
         double bottom = this.acy + (double)this.thickness * 0.8;
         double top = bottom + 2.0 * rise;
         double sx0 = this.acx + Math.cos(a0) * rc;
         double sz0 = this.acz + Math.sin(a0) * rc;
         double approach = Math.max(1.0, Math.sqrt((sx0 - hx) * (sx0 - hx) + (top - hy) * (top - hy) + (sz0 - hz) * (sz0 - hz)));
         double px;
         double py;
         double pz;
         if (d <= approach) {
            double u = d / approach;
            px = Mth.lerp(u, hx, sx0);
            py = Mth.lerp(u, hy, top);
            pz = Mth.lerp(u, hz, sz0);
         } else if (d <= approach + helix) {
            double u = (d - approach) / turn;
            double a = a0 + (double)this.actionSide * u * (Math.PI * 2);
            px = this.acx + Math.cos(a) * rc;
            py = top - u * rise;
            pz = this.acz + Math.sin(a) * rc;
         } else {
            double rest = d - approach - helix;
            double a = a0 + (double)this.actionSide * 2.0 * (Math.PI * 2);
            this.basePoint(rest, partial, this.tmpC);
            double f = 1.0 - smooth(rest / (double)(6.0F * this.thickness));
            px = this.tmpC[0] + (this.acx + Math.cos(a) * rc - hx) * f;
            py = this.tmpC[1] + (bottom - hy) * f;
            pz = this.tmpC[2] + (this.acz + Math.sin(a) * rc - hz) * f;
         }

         out[0] += (px - out[0]) * w;
         out[1] += (py - out[1]) * w;
         out[2] += (pz - out[2]) * w;
      }
   }

   private void twisterShape(double d, float partial, float w, double[] out) {
      double hx = Mth.lerp((double)partial, this.pax, this.ax);
      double hy = Mth.lerp((double)partial, this.pay, this.ay);
      double hz = Mth.lerp((double)partial, this.paz, this.az);
      double s = Math.max(0.0, d - (double)this.neck * 0.5);
      double r0 = 2.6 * (double)this.thickness;
      double r = r0 * smooth(s / (2.0 * r0)) + 0.05 * s + 0.6;
      double a = (double)((float)this.age + partial) * 0.3 + s / (r0 + 0.05 * s);
      double px = hx + Math.cos(a) * r;
      double py = Math.max(this.twistFloor + (double)this.thickness, hy - 0.22 * s);
      double pz = hz + Math.sin(a) * r;
      out[0] += (px - out[0]) * (double)w;
      out[1] += (py - out[1]) * (double)w;
      out[2] += (pz - out[2]) * (double)w;
   }

   public float coil(float partial) {
      return Mth.lerp(partial, this.prevCoil, this.coil);
   }

   public boolean flying() {
      return this.flying;
   }

   public double travel() {
      return this.travel;
   }

   public int idleTicks() {
      return this.idleTicks;
   }
}
