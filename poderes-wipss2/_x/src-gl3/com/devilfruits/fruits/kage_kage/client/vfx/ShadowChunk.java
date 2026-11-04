package com.devilfruits.fruits.kage_kage.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ShadowChunk {
   public static final float CELL = 0.0625F;
   private static final float RIM = 0.016F;
   public Vec3 pos = Vec3.ZERO;
   public float radius;
   public float sx = 1.0F;
   public float sy = 1.0F;
   public float sz = 1.0F;
   public float heat = 1.0F;
   public float alpha = 1.0F;
   private Vec3 prevPos = Vec3.ZERO;
   private float prevRadius;
   private float psx = 1.0F;
   private float psy = 1.0F;
   private float psz = 1.0F;
   private float prevHeat = 1.0F;
   private final long seed;
   private int step;
   private final List<int[]> cells = new ArrayList<>();
   private float builtRadius = -1.0F;
   private int builtStep = -1;

   public ShadowChunk(long seed) {
      this.seed = seed;
   }

   public ShadowChunk snap(Vec3 p) {
      this.pos = p;
      this.prevPos = p;
      return this;
   }

   public void tick() {
      this.prevPos = this.pos;
      this.prevRadius = this.radius;
      this.psx = this.sx;
      this.psy = this.sy;
      this.psz = this.sz;
      this.prevHeat = this.heat;
      this.step++;
   }

   public Vec3 position(float partial) {
      return this.prevPos.lerp(this.pos, (double)partial);
   }

   private float hash(int i, int j, int k, int s) {
      long h = this.seed ^ (long)i * 73856093L ^ (long)j * 19349663L ^ (long)k * 83492791L ^ (long)s * 2654435761L;
      h ^= h >>> 29;
      h *= -4658895280553007687L;
      h ^= h >>> 32;
      return (float)(h & 65535L) / 65535.0F;
   }

   private boolean inside(int i, int j, int k, float r, int s) {
      float cx = (float)i + 0.5F;
      float cy = (float)j + 0.5F;
      float cz = (float)k + 0.5F;
      float d = Mth.sqrt(cx * cx + cy * cy + cz * cz);
      if (d <= r * 0.55F) {
         return true;
      } else {
         float lump = this.hash(i >> 1, j >> 1, k >> 1, s / 3 + 101);
         return d <= r * (0.72F + 0.22F * lump + 0.14F * this.hash(i, j, k, s));
      }
   }

   private void rebuild(float rBlocks) {
      float r = Math.max(0.6F, rBlocks / 0.0625F);
      int s = this.step / 2;
      if (!(Math.abs(r - this.builtRadius) < 0.12F) || s != this.builtStep) {
         this.builtRadius = r;
         this.builtStep = s;
         this.cells.clear();
         int n = Mth.ceil(r) + 1;

         for (int i = -n; i < n; i++) {
            for (int j = -n; j < n; j++) {
               for (int k = -n; k < n; k++) {
                  if (this.inside(i, j, k, r, s)) {
                     int faces = 0;
                     if (!this.inside(i - 1, j, k, r, s)) {
                        faces |= 1;
                     }

                     if (!this.inside(i + 1, j, k, r, s)) {
                        faces |= 2;
                     }

                     if (!this.inside(i, j - 1, k, r, s)) {
                        faces |= 4;
                     }

                     if (!this.inside(i, j + 1, k, r, s)) {
                        faces |= 8;
                     }

                     if (!this.inside(i, j, k - 1, r, s)) {
                        faces |= 16;
                     }

                     if (!this.inside(i, j, k + 1, r, s)) {
                        faces |= 32;
                     }

                     if (faces != 0) {
                        this.cells.add(new int[]{i, j, k, faces, (int)(this.hash(i, j, k, 7) * 255.0F)});
                     }
                  }
               }
            }
         }
      }
   }

   public void draw(boolean glow, PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial, int light, RimConsumer rim) {
      float r = Mth.lerp(partial, this.prevRadius, this.radius);
      if (!(r < 0.02F) && !(this.alpha <= 0.01F)) {
         this.rebuild(this.radius);
         float k = r / (this.builtRadius * 0.0625F);
         Vec3 p = this.position(partial);
         ps.pushPose();
         ps.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
         float ax = Mth.lerp(partial, this.psx, this.sx);
         float ay = Mth.lerp(partial, this.psy, this.sy);
         float az = Mth.lerp(partial, this.psz, this.sz);
         ps.scale(ax * k * 0.0625F, ay * k * 0.0625F, az * k * 0.0625F);
         Pose pose = ps.last();
         if (!glow) {
            VertexConsumer vc = buffers.getBuffer(
               this.alpha < 0.99F ? RenderType.entityTranslucent(KageLook.white()) : RenderType.entityCutout(KageLook.white())
            );
            int a = (int)(this.alpha * 255.0F) << 24;

            for (int[] c : this.cells) {
               int rgb = KageLook.mix(459788, 2495552, (float)c[4] / 255.0F);
               Voxels.box(vc, pose, (float)c[0], (float)c[1], (float)c[2], (float)(c[0] + 1), (float)(c[1] + 1), (float)(c[2] + 1), c[3], a | rgb, light);
            }
         } else {
            float h = Mth.lerp(partial, this.prevHeat, this.heat);
            float t = 0.016F * Math.max(1.0F, h) / (k * 0.0625F * Math.max(0.5F, Math.min(ax, Math.min(ay, az))));
            rim.to(buffers.getBuffer(RenderType.eyes(KageLook.white())), 0.0F, KageLook.scale(KageLook.rim(h), this.alpha), 1.0F);

            for (int[] c : this.cells) {
               Voxels.rim(rim, pose, (float)c[0], (float)c[1], (float)c[2], (float)(c[0] + 1), (float)(c[1] + 1), (float)(c[2] + 1), c[3], t);
            }
         }

         ps.popPose();
      }
   }
}
