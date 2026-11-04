package com.devilfruits.client.render.vfx;

import com.devilfruits.client.render.VfxDraw;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class SpinRingsEffect extends VfxEffect {
   private static final int SEGMENTS = 22;
   private static final float ARC = 4.3F;
   private double x;
   private double y;
   private double z;
   private int follow = -1;
   private float followHeight;
   private final float radius;
   private final int duration;
   private final int rings;
   private final Vector3f[] us;
   private final Vector3f[] vs;
   private final float[] speed;
   private final float[] phase;
   private final float[] scale;
   private float width = 0.16F;
   private int core = 16777215;
   private int glow = 11955455;
   private float spin = 1.0F;
   private final Vector3f c = new Vector3f();
   private final long seed;

   public SpinRingsEffect(Vec3 at, float radius, int rings, int duration, long seed) {
      this.x = at.x;
      this.y = at.y;
      this.z = at.z;
      this.radius = radius;
      this.rings = Math.max(1, rings);
      this.duration = Math.max(2, duration);
      this.us = new Vector3f[this.rings];
      this.vs = new Vector3f[this.rings];
      this.speed = new float[this.rings];
      this.phase = new float[this.rings];
      this.scale = new float[this.rings];

      for (int k = 0; k < this.rings; k++) {
         this.us[k] = new Vector3f();
         this.vs[k] = new Vector3f();
      }

      this.seed = seed;
      this.layout(25.0F, 65.0F);
   }

   private void layout(float minDeg, float maxDeg) {
      long s = this.seed;
      Vector3f n = new Vector3f();

      for (int k = 0; k < this.rings; k++) {
         s = s * 6364136223846793005L + 1442695040888963407L;
         float r1 = (float)(s >>> 33 & 65535L) / 65535.0F;
         float r2 = (float)(s >>> 17 & 65535L) / 65535.0F;
         float tilt = (minDeg + (maxDeg - minDeg) * r1) * (float) (Math.PI / 180.0);
         float heading = (float)k * (float) (Math.PI * 2) / (float)this.rings + r2;
         n.set(Mth.sin(tilt) * Mth.cos(heading), Mth.cos(tilt), Mth.sin(tilt) * Mth.sin(heading));
         VfxDraw.basis(n, this.us[k], this.vs[k]);
         this.speed[k] = (k % 2 == 0 ? 1.0F : -1.0F) * (0.55F + 0.3F * r1);
         this.phase[k] = r2 * (float) (Math.PI * 2);
         this.scale[k] = 0.82F + 0.3F * (float)k / (float)Math.max(1, this.rings - 1);
      }
   }

   public SpinRingsEffect tilt(float minDeg, float maxDeg) {
      this.layout(minDeg, maxDeg);
      return this;
   }

   public SpinRingsEffect colors(int core, int glow, float width) {
      this.core = core;
      this.glow = glow;
      this.width = width;
      return this;
   }

   public SpinRingsEffect spin(float spin) {
      this.spin = spin;
      return this;
   }

   public SpinRingsEffect follow(@Nullable Entity entity, float heightFraction) {
      if (entity != null) {
         this.follow = entity.getId();
         this.followHeight = heightFraction;
      }

      return this;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > this.duration) {
         this.finish();
      } else {
         if (this.follow >= 0) {
            Entity e = level.getEntity(this.follow);
            if (e != null) {
               this.x = e.getX();
               this.y = e.getY() + (double)(e.getBbHeight() * this.followHeight);
               this.z = e.getZ();
            }
         }
      }
   }

   @Override
   public void renderGlow(VfxContext ctx) {
      float t = ((float)this.age + ctx.partialTick) / (float)this.duration;
      if (!(t >= 1.0F)) {
         float alpha = (t < 0.12F ? t / 0.12F : (t > 0.6F ? (1.0F - t) / 0.4F : 1.0F)) * ctx.proximity(this.x, this.y, this.z, 1.2F, 4.0F, 0.25F);
         this.c.set(ctx.rx(this.x), ctx.ry(this.y), ctx.rz(this.z));
         float time = ((float)this.age + ctx.partialTick) * this.spin;

         for (int k = 0; k < this.rings; k++) {
            float r = this.radius * this.scale[k] * (0.85F + 0.25F * t);
            float start = this.phase[k] + time * this.speed[k];
            this.band(ctx, this.us[k], this.vs[k], r, start, this.width * 2.6F, this.glow, alpha * 0.45F);
            this.band(ctx, this.us[k], this.vs[k], r, start, this.width, this.core, alpha);
         }
      }
   }

   private void band(VfxContext ctx, Vector3f u, Vector3f v, float r, float start, float w, int rgb, float alpha) {
      VertexConsumer vc = ctx.buffer;
      Matrix4f m = ctx.matrix;
      int cr = rgb >> 16 & 0xFF;
      int cg = rgb >> 8 & 0xFF;
      int cb = rgb & 0xFF;

      for (int i = 0; i < 22; i++) {
         float s0 = (float)i / 22.0F;
         float s1 = (float)(i + 1) / 22.0F;
         float a0 = start + 4.3F * s0;
         float a1 = start + 4.3F * s1;
         float w0 = w * crescent(s0);
         float w1 = w * crescent(s1);
         int al0 = Mth.clamp((int)(alpha * Math.min(1.0F, 0.25F + s0 * 1.2F) * 255.0F), 0, 255);
         int al1 = Mth.clamp((int)(alpha * Math.min(1.0F, 0.25F + s1 * 1.2F) * 255.0F), 0, 255);
         float c0 = Mth.cos(a0);
         float n0 = Mth.sin(a0);
         float c1 = Mth.cos(a1);
         float n1 = Mth.sin(a1);
         float dx0 = u.x * c0 + v.x * n0;
         float dy0 = u.y * c0 + v.y * n0;
         float dz0 = u.z * c0 + v.z * n0;
         float dx1 = u.x * c1 + v.x * n1;
         float dy1 = u.y * c1 + v.y * n1;
         float dz1 = u.z * c1 + v.z * n1;
         vc.addVertex(m, this.c.x + dx0 * (r - w0), this.c.y + dy0 * (r - w0), this.c.z + dz0 * (r - w0)).setColor(cr, cg, cb, al0);
         vc.addVertex(m, this.c.x + dx0 * (r + w0), this.c.y + dy0 * (r + w0), this.c.z + dz0 * (r + w0)).setColor(cr, cg, cb, al0);
         vc.addVertex(m, this.c.x + dx1 * (r + w1), this.c.y + dy1 * (r + w1), this.c.z + dz1 * (r + w1)).setColor(cr, cg, cb, al1);
         vc.addVertex(m, this.c.x + dx1 * (r - w1), this.c.y + dy1 * (r - w1), this.c.z + dz1 * (r - w1)).setColor(cr, cg, cb, al1);
      }
   }

   private static float crescent(float s) {
      return s < 0.85F ? 0.15F + 0.85F * Mth.sin((float) (Math.PI / 2) * s / 0.85F) : Mth.cos((float) (Math.PI / 2) * (s - 0.85F) / 0.15F);
   }

   @Override
   public double x() {
      return this.x;
   }

   @Override
   public double y() {
      return this.y;
   }

   @Override
   public double z() {
      return this.z;
   }
}
