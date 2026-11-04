package com.devilfruits.client.render.vfx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class StarburstEffect extends VfxEffect {
   private final double x;
   private final double y;
   private final double z;
   private final float size;
   private final int duration;
   private final int spikes;
   private final float[] length;
   private final float[] angle;
   private int core = 16777215;
   private int mid = 16765503;
   private int tip = 15217182;
   private float streak;
   private final Vector3f c = new Vector3f();

   public StarburstEffect(Vec3 at, float size, int duration, int spikes, long seed) {
      this.x = at.x;
      this.y = at.y;
      this.z = at.z;
      this.size = size;
      this.duration = Math.max(2, duration);
      this.spikes = Math.max(4, spikes);
      this.length = new float[this.spikes];
      this.angle = new float[this.spikes];
      long s = seed;

      for (int i = 0; i < this.spikes; i++) {
         s = s * 6364136223846793005L + 1442695040888963407L;
         float r = (float)(s >>> 40 & 65535L) / 65535.0F;
         this.length[i] = i % 2 == 0 ? 0.75F + 0.25F * r : 0.35F + 0.25F * r;
         this.angle[i] = ((float)i + (r - 0.5F) * 0.5F) * (float) (Math.PI * 2) / (float)this.spikes;
      }
   }

   public StarburstEffect colors(int core, int mid, int tip) {
      this.core = core;
      this.mid = mid;
      this.tip = tip;
      return this;
   }

   public StarburstEffect streak(float length) {
      this.streak = length;
      return this;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > this.duration) {
         this.finish();
      }
   }

   @Override
   public void renderGlow(VfxContext ctx) {
      float t = Mth.clamp(((float)this.age + ctx.partialTick) / (float)this.duration, 0.0F, 1.0F);
      float grow = t < 0.25F ? t / 0.25F : 1.0F + 0.15F * (t - 0.25F);
      float alpha = (t < 0.4F ? 1.0F : (1.0F - t) / 0.6F) * ctx.proximity(this.x, this.y, this.z, 1.5F, 5.0F, 0.3F);
      if (!(alpha <= 0.01F)) {
         this.c.set(ctx.rx(this.x), ctx.ry(this.y), ctx.rz(this.z));
         Vector3f l = ctx.camLeft;
         Vector3f u = ctx.camUp;
         VertexConsumer vc = ctx.buffer;
         Matrix4f m = ctx.matrix;
         float base = this.size * 0.09F;

         for (int i = 0; i < this.spikes; i++) {
            float a = this.angle[i] + t * 0.15F;
            float ca = Mth.cos(a);
            float sa = Mth.sin(a);
            float len = this.size * this.length[i] * grow;
            float bx = -sa * base;
            float by = ca * base;
            this.vert(vc, m, l, u, ca * base * 0.5F + bx, sa * base * 0.5F + by, this.core, alpha);
            this.vert(vc, m, l, u, ca * len * 0.55F, sa * len * 0.55F, this.mid, alpha * 0.9F);
            this.vert(vc, m, l, u, ca * len, sa * len, this.tip, alpha * 0.6F);
            this.vert(vc, m, l, u, ca * base * 0.5F - bx, sa * base * 0.5F - by, this.core, alpha);
         }

         float r = this.size * 0.22F * grow;

         for (int i = 0; i < 8; i++) {
            float a0 = (float)i * (float) (Math.PI * 2) / 8.0F;
            float a1 = (float)(i + 1) * (float) (Math.PI * 2) / 8.0F;
            this.vert(vc, m, l, u, 0.0F, 0.0F, this.core, alpha);
            this.vert(vc, m, l, u, Mth.cos(a0) * r, Mth.sin(a0) * r, this.mid, alpha * 0.8F);
            this.vert(vc, m, l, u, Mth.cos(a1) * r, Mth.sin(a1) * r, this.mid, alpha * 0.8F);
            this.vert(vc, m, l, u, 0.0F, 0.0F, this.core, alpha);
         }

         if (this.streak > 0.0F) {
            float half = this.size * this.streak * grow;
            float thick = this.size * 0.035F;
            this.vert(vc, m, l, u, -half, -thick, this.mid, 0.0F);
            this.vert(vc, m, l, u, 0.0F, -thick * 2.0F, this.core, alpha);
            this.vert(vc, m, l, u, 0.0F, thick * 2.0F, this.core, alpha);
            this.vert(vc, m, l, u, -half, thick, this.mid, 0.0F);
            this.vert(vc, m, l, u, 0.0F, -thick * 2.0F, this.core, alpha);
            this.vert(vc, m, l, u, half, -thick, this.mid, 0.0F);
            this.vert(vc, m, l, u, half, thick, this.mid, 0.0F);
            this.vert(vc, m, l, u, 0.0F, thick * 2.0F, this.core, alpha);
         }
      }
   }

   private void vert(VertexConsumer vc, Matrix4f m, Vector3f l, Vector3f u, float px, float py, int rgb, float alpha) {
      vc.addVertex(m, this.c.x + l.x * px + u.x * py, this.c.y + l.y * px + u.y * py, this.c.z + l.z * px + u.z * py)
         .setColor(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, Mth.clamp((int)(alpha * 255.0F), 0, 255));
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
