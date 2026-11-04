package com.devilfruits.client.render.vfx;

import com.devilfruits.client.particle.Fx;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class FloorDebrisEffect extends VfxEffect {
   private static int alive;
   private static final int CAP = 260;
   private final int n;
   private final float[] px;
   private final float[] py;
   private final float[] pz;
   private final float[] vx;
   private final float[] vy;
   private final float[] vz;
   private final float[] rx;
   private final float[] ry;
   private final float[] rz;
   private final float[] wx;
   private final float[] wy;
   private final float[] wz;
   private final float[] hx;
   private final float[] hy;
   private final float[] hz;
   private final float[] u0;
   private final float[] v0;
   private final boolean[] resting;
   private final int[] light;
   private final TextureAtlasSprite sprite;
   private final int rest;
   private final double ox;
   private final double oy;
   private final double oz;
   private int landedAge = -1;
   private int maxAge;
   private boolean orbiting;
   private float orbitRadius;
   private float orbitSpeed;
   private int dropAge = -1;
   private final MutableBlockPos probe = new MutableBlockPos();
   private float m00;
   private float m01;
   private float m02;
   private float m10;
   private float m11;
   private float m12;
   private float m20;
   private float m21;
   private float m22;

   public FloorDebrisEffect(ClientLevel level, Vec3 at, int count, float speed, float up, int rest, long seed) {
      BlockState ground = Fx.groundAt(level, at.x, at.y - 0.5, at.z, this.probe);
      if (ground == null || ground.isAir()) {
         ground = Blocks.STONE.defaultBlockState();
      }

      this.sprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(ground);
      int wanted = Math.min(Fx.count(count), Math.max(0, 260 - alive));
      this.n = wanted;
      alive = alive + this.n;
      this.rest = rest;
      this.ox = at.x;
      this.oy = at.y;
      this.oz = at.z;
      this.px = new float[this.n];
      this.py = new float[this.n];
      this.pz = new float[this.n];
      this.vx = new float[this.n];
      this.vy = new float[this.n];
      this.vz = new float[this.n];
      this.rx = new float[this.n];
      this.ry = new float[this.n];
      this.rz = new float[this.n];
      this.wx = new float[this.n];
      this.wy = new float[this.n];
      this.wz = new float[this.n];
      this.hx = new float[this.n];
      this.hy = new float[this.n];
      this.hz = new float[this.n];
      this.u0 = new float[this.n];
      this.v0 = new float[this.n];
      this.resting = new boolean[this.n];
      this.light = new int[this.n];
      long s = seed;

      for (int i = 0; i < this.n; i++) {
         s = s * 6364136223846793005L + 1442695040888963407L;
         float a = rnd(s, 0) * (float) (Math.PI * 2);
         float r = 0.3F + rnd(s, 1) * 0.9F;
         float k = 0.4F + 0.6F * rnd(s, 2);
         this.px[i] = Mth.cos(a) * r;
         this.pz[i] = Mth.sin(a) * r;
         this.py[i] = 0.1F;
         this.vx[i] = Mth.cos(a) * speed * k;
         this.vz[i] = Mth.sin(a) * speed * k;
         this.vy[i] = up * (0.6F + 0.7F * rnd(s, 3));
         float w = 0.16F + 0.18F * rnd(s, 4);
         this.hx[i] = w;
         this.hz[i] = w * (0.55F + 0.6F * rnd(s, 5));
         this.hy[i] = 0.05F + 0.05F * rnd(s, 6);
         this.rx[i] = rnd(s, 7) * (float) (Math.PI * 2);
         this.ry[i] = rnd(s, 8) * (float) (Math.PI * 2);
         this.wx[i] = (rnd(s, 9) - 0.5F) * 0.9F;
         this.wy[i] = (rnd(s, 10) - 0.5F) * 0.5F;
         this.wz[i] = (rnd(s, 11) - 0.5F) * 0.9F;
         this.u0[i] = rnd(s, 12) * 0.5F;
         this.v0[i] = rnd(s, 13) * 0.5F;
         this.light[i] = LevelRenderer.getLightColor(level, BlockPos.containing(this.ox, this.oy + 0.5, this.oz));
      }

      this.maxAge = 70 + rest + 30;
   }

   private static float rnd(long s, int k) {
      long h = s + (long)k * -7046029254386353131L;
      h = (h ^ h >>> 30) * -4658895280553007687L;
      h = (h ^ h >>> 27) * -7723592293110705685L;
      return (float)((h ^ h >>> 31) & 16777215L) / 1.6777216E7F;
   }

   public FloorDebrisEffect orbit(float radius, float speed) {
      this.orbiting = true;
      this.orbitRadius = radius;
      this.orbitSpeed = speed;
      this.maxAge = 1200;
      return this;
   }

   public int count() {
      return this.n;
   }

   public boolean orbiting() {
      return this.orbiting && this.age > 6;
   }

   public void chunk(int i, double[] out) {
      out[0] = this.ox + (double)this.px[i];
      out[1] = this.oy + (double)this.py[i];
      out[2] = this.oz + (double)this.pz[i];
   }

   public void drop() {
      if (this.orbiting) {
         this.orbiting = false;
         this.dropAge = this.age;
         this.maxAge = this.age + 70 + this.rest + 30;

         for (int i = 0; i < this.n; i++) {
            float d = Mth.sqrt(this.px[i] * this.px[i] + this.pz[i] * this.pz[i]) + 0.001F;
            this.vx[i] = this.px[i] / d * 0.35F - this.pz[i] / d * this.orbitSpeed * 0.5F;
            this.vz[i] = this.pz[i] / d * 0.35F + this.px[i] / d * this.orbitSpeed * 0.5F;
            this.vy[i] = 0.25F;
         }
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.n != 0 && this.age <= this.maxAge) {
         boolean allRest = true;

         for (int i = 0; i < this.n; i++) {
            if (this.orbiting && this.age > 6) {
               float d = Mth.sqrt(this.px[i] * this.px[i] + this.pz[i] * this.pz[i]) + 0.001F;
               float target = this.orbitRadius * (0.7F + 0.6F * (float)(i % 5) / 4.0F);
               float a = (float)Math.atan2((double)this.pz[i], (double)this.px[i]) + this.orbitSpeed / Math.max(0.5F, d);
               float nd = d + (target - d) * 0.15F;
               this.px[i] = Mth.cos(a) * nd;
               this.pz[i] = Mth.sin(a) * nd;
               float lift = 1.2F + (float)(i % 7) * 0.35F + Mth.sin((float)(this.age + i * 9) * 0.15F) * 0.4F;
               this.py[i] = this.py[i] + (lift - this.py[i]) * 0.12F;
               this.rx[i] = this.rx[i] + this.wx[i];
               this.ry[i] = this.ry[i] + this.wy[i];
               this.rz[i] = this.rz[i] + this.wz[i];
               allRest = false;
            } else if (!this.resting[i]) {
               allRest = false;
               this.vy[i] = this.vy[i] - 0.06F;
               this.px[i] = this.px[i] + this.vx[i];
               this.py[i] = this.py[i] + this.vy[i];
               this.pz[i] = this.pz[i] + this.vz[i];
               this.rx[i] = this.rx[i] + this.wx[i];
               this.ry[i] = this.ry[i] + this.wy[i];
               this.rz[i] = this.rz[i] + this.wz[i];
               float floor = this.floorAt(level, i);
               if (this.py[i] - this.hy[i] <= floor && this.vy[i] < 0.0F) {
                  this.py[i] = floor + this.hy[i];
                  if (this.vy[i] > -0.18F) {
                     this.resting[i] = true;
                     this.vx[i] = this.vz[i] = this.vy[i] = 0.0F;
                     this.rx[i] = this.rz[i] = 0.0F;
                     this.light[i] = LevelRenderer.getLightColor(
                        level, BlockPos.containing(this.ox + (double)this.px[i], this.oy + (double)this.py[i] + 0.3, this.oz + (double)this.pz[i])
                     );
                  } else {
                     this.vy[i] = -this.vy[i] * 0.3F;
                     this.vx[i] = this.vx[i] * 0.5F;
                     this.vz[i] = this.vz[i] * 0.5F;
                     this.wx[i] = this.wx[i] * 0.4F;
                     this.wz[i] = this.wz[i] * 0.4F;
                  }
               }
            }
         }

         if (allRest && this.landedAge < 0) {
            this.landedAge = this.age;
            this.maxAge = Math.min(this.maxAge, this.age + this.rest + 30);
         }
      } else {
         this.finish();
      }
   }

   private float floorAt(ClientLevel level, int i) {
      int bx = Mth.floor(this.ox + (double)this.px[i]);
      int bz = Mth.floor(this.oz + (double)this.pz[i]);
      int top = Mth.floor(this.oy + (double)this.py[i]);

      for (int y = top; y > top - 4; y--) {
         this.probe.set(bx, y, bz);
         BlockState state = level.getBlockState(this.probe);
         if (!state.getCollisionShape(level, this.probe).isEmpty()) {
            return (float)(state.getCollisionShape(level, this.probe).max(Axis.Y) + (double)y - this.oy);
         }
      }

      return (float)((double)Mth.floor(this.oy) - this.oy) - 3.0F;
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      if (this.n != 0) {
         float sink = Mth.clamp(((float)(this.maxAge - this.age) - ctx.partialTick) / 30.0F, 0.0F, 1.0F);
         if (!(sink <= 0.0F)) {
            VertexConsumer vc = ctx.buffers.getBuffer(RenderType.text(TextureAtlas.LOCATION_BLOCKS));
            Matrix4f m = ctx.matrix;
            float du = this.sprite.getU1() - this.sprite.getU0();
            float dv = this.sprite.getV1() - this.sprite.getV0();

            for (int i = 0; i < this.n; i++) {
               boolean moving = !this.resting[i];
               float p = moving ? ctx.partialTick : 0.0F;
               float cx = ctx.rx(this.ox + (double)this.px[i] + (double)(this.vx[i] * p));
               float cy = ctx.ry(this.oy + (double)this.py[i] + (double)(this.vy[i] * p));
               float cz = ctx.rz(this.oz + (double)this.pz[i] + (double)(this.vz[i] * p));
               float sx = this.hx[i] * sink;
               float sy = this.hy[i] * sink;
               float sz = this.hz[i] * sink;
               float ua = this.sprite.getU0() + du * this.u0[i];
               float ub = ua + du * 0.5F;
               float va = this.sprite.getV0() + dv * this.v0[i];
               float vb = va + dv * 0.5F;
               this.box(vc, m, cx, cy, cz, sx, sy, sz, this.rx[i], this.ry[i], this.rz[i], ua, ub, va, vb, this.light[i]);
            }
         }
      }
   }

   private void box(
      VertexConsumer vc,
      Matrix4f m,
      float cx,
      float cy,
      float cz,
      float sx,
      float sy,
      float sz,
      float ax,
      float ay,
      float az,
      float ua,
      float ub,
      float va,
      float vb,
      int light
   ) {
      float cxr = Mth.cos(ax);
      float sxr = Mth.sin(ax);
      float cyr = Mth.cos(ay);
      float syr = Mth.sin(ay);
      float czr = Mth.cos(az);
      float szr = Mth.sin(az);
      this.m00 = cyr * czr + syr * sxr * szr;
      this.m01 = -cyr * szr + syr * sxr * czr;
      this.m02 = syr * cxr;
      this.m10 = cxr * szr;
      this.m11 = cxr * czr;
      this.m12 = -sxr;
      this.m20 = -syr * czr + cyr * sxr * szr;
      this.m21 = syr * szr + cyr * sxr * czr;
      this.m22 = cyr * cxr;
      this.face(vc, m, cx, cy, cz, sx, sy, sz, 0, 1, 0, 1.0F, ua, ub, va, vb, light);
      this.face(vc, m, cx, cy, cz, sx, sy, sz, 0, -1, 0, 0.5F, ua, ub, va, vb, light);
      this.face(vc, m, cx, cy, cz, sx, sy, sz, 1, 0, 0, 0.6F, ua, ub, va, vb, light);
      this.face(vc, m, cx, cy, cz, sx, sy, sz, -1, 0, 0, 0.6F, ua, ub, va, vb, light);
      this.face(vc, m, cx, cy, cz, sx, sy, sz, 0, 0, 1, 0.8F, ua, ub, va, vb, light);
      this.face(vc, m, cx, cy, cz, sx, sy, sz, 0, 0, -1, 0.8F, ua, ub, va, vb, light);
   }

   private void face(
      VertexConsumer vc,
      Matrix4f m,
      float cx,
      float cy,
      float cz,
      float sx,
      float sy,
      float sz,
      int nx,
      int ny,
      int nz,
      float shade,
      float ua,
      float ub,
      float va,
      float vb,
      int light
   ) {
      int tx = ny != 0 ? 1 : 0;
      int ty = 0;
      int tz = ny != 0 ? 0 : (nx != 0 ? 1 : 0);
      if (nz != 0) {
         tx = 1;
         tz = 0;
      }

      int bx = ny * tz - nz * ty;
      int by = nz * tx - nx * tz;
      int bz = nx * ty - ny * tx;
      int c = Mth.clamp((int)(shade * 255.0F), 0, 255);
      this.corner(vc, m, cx, cy, cz, sx, sy, sz, nx - tx - bx, ny - ty - by, nz - tz - bz, ua, va, c, light);
      this.corner(vc, m, cx, cy, cz, sx, sy, sz, nx - tx + bx, ny - ty + by, nz - tz + bz, ua, vb, c, light);
      this.corner(vc, m, cx, cy, cz, sx, sy, sz, nx + tx + bx, ny + ty + by, nz + tz + bz, ub, vb, c, light);
      this.corner(vc, m, cx, cy, cz, sx, sy, sz, nx + tx - bx, ny + ty - by, nz + tz - bz, ub, va, c, light);
   }

   private void corner(
      VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float sx, float sy, float sz, int lx, int ly, int lz, float u, float v, int c, int light
   ) {
      float x = (float)lx * sx;
      float y = (float)ly * sy;
      float z = (float)lz * sz;
      vc.addVertex(
            m,
            cx + this.m00 * x + this.m01 * y + this.m02 * z,
            cy + this.m10 * x + this.m11 * y + this.m12 * z,
            cz + this.m20 * x + this.m21 * y + this.m22 * z
         )
         .setColor(c, c, c, 255)
         .setUv(u, v)
         .setLight(light);
   }

   @Override
   public void removed() {
      alive = Math.max(0, alive - this.n);
   }

   @Override
   public double x() {
      return this.ox;
   }

   @Override
   public double y() {
      return this.oy;
   }

   @Override
   public double z() {
      return this.oz;
   }

   @Override
   public double cullDistance() {
      return 64.0;
   }
}
