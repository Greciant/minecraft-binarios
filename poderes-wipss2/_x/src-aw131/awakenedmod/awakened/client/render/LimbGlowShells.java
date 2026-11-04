package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

final class LimbGlowShells {
   private static final float[] INFLATE_PX = new float[]{0.45F, 0.95F, 1.6F, 2.4F};
   private static final int SHELLS_MIN = 2;
   private static final int SHELLS_GAIN = 2;
   private static final float SHELL_ALPHA = 150.0F;
   private static final float BODY_SHARE = 0.55F;
   private static final float GLOW_SHARE = 0.2F;
   private static final float RIM_ALPHA_FRAC = 0.22F;
   private static final float BRIGHT_MIN = 0.3F;
   private static final float CORE_LIGHTEN = 0.8F;
   private static final float TOP_LIGHT = 0.12F;
   private static final float BOTTOM_SHADE = 0.86F;
   private static final float BREATHE_RATE_MIN = 0.12F;
   private static final float BREATHE_RATE_GAIN = 0.45F;
   private static final float BREATHE_AMOUNT = 0.12F;
   private static final float CRACKLE_FROM = 0.65F;
   private static final int CRACKLE_MAX = 7;
   private static final float CRACKLE_HALF_PX_MIN = 0.35F;
   private static final float CRACKLE_HALF_PX_SPAN = 0.4F;
   private static final float CRACKLE_LIFT_PX = 0.6F;
   private static final float CRACKLE_ALPHA = 170.0F;
   private static final float JITTER_WINDOW_TICKS = 2.0F;
   private static final float FLASH_PUSH = 1.6F;
   private static final float FLASH_ALPHA_BOOST = 0.7F;
   private static final Map<ModelPart, float[][]> BOXES = new IdentityHashMap<>();
   private static final int BOXES_CAP = 64;

   private LimbGlowShells() {
   }

   static void forgetAll() {
      BOXES.clear();
   }

   static void draw(PoseStack ps, VfxDraw.Sink sink, ModelPart part, float intensity, float flash, float age, int seed, int color) {
      float[][] boxes = boxes(part);
      if (boxes.length != 0) {
         float in = Mth.clamp(intensity, 0.0F, 1.0F);
         float fl = Mth.clamp(flash, 0.0F, 1.0F);
         float breathe = 1.0F + 0.12F * Mth.sin(age * (0.12F + 0.45F * in) + (float)seed * 0.31F);
         int shells = fl > 0.0F ? INFLATE_PX.length : Math.min(INFLATE_PX.length, 2 + Math.round(in * 2.0F));
         float bright = (0.3F + 0.7F * in) * breathe;
         if (fl > 0.0F) {
            bright = Math.max(bright, fl) * (1.0F + 0.7F * fl);
         }

         int core = VfxColor.lighten(color, 0.8F);
         int[] rgb = new int[shells];
         float[] inflate = new float[shells];
         float[] alpha = new float[shells];

         for (int k = 0; k < shells; k++) {
            float t = shells == 1 ? 0.0F : (float)k / (float)(shells - 1);
            int c = VoxelFlameDraw.mix(core, color, t);
            rgb[k] = fl > 0.0F ? VoxelFlameDraw.mix(c, 16777215, fl) : c;
            inflate[k] = INFLATE_PX[k] * (1.0F + 1.6F * fl) / 16.0F;
            alpha[k] = 150.0F * bright * (1.0F - 0.78F * t);
         }

         VfxDraw.bodyThenGlow(
            ps,
            sink,
            (pose, vc) -> shells(pose, vc, boxes, shells, rgb, inflate, alpha, 0.55F),
            (pose, vc) -> shells(pose, vc, boxes, shells, rgb, inflate, alpha, 0.2F)
         );
         if (in >= 0.65F) {
            crackle(ps, sink, boxes, inflate[shells - 1], in, age, seed, core);
         }
      }
   }

   private static void shells(Pose pose, VertexConsumer vc, float[][] boxes, int shells, int[] rgb, float[] inflate, float[] alpha, float share) {
      for (int k = 0; k < shells; k++) {
         int a = Math.round(alpha[k] * share);
         if (a > 1) {
            for (float[] b : boxes) {
               box(pose, vc, b, inflate[k], a, rgb[k]);
            }
         }
      }
   }

   private static void box(Pose pose, VertexConsumer vc, float[] b, float d, int alpha, int rgb) {
      float x0 = b[0] / 16.0F - d;
      float y0 = b[1] / 16.0F - d;
      float z0 = b[2] / 16.0F - d;
      float x1 = b[3] / 16.0F + d;
      float y1 = b[4] / 16.0F + d;
      float z1 = b[5] / 16.0F + d;
      int side = VfxColor.argb(alpha, rgb);
      int top = VfxColor.argb(alpha, VfxColor.lighten(rgb, 0.12F));
      int bottom = VfxColor.argb(alpha, VfxColor.shade(rgb, 0.86F));
      quad(pose, vc, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, top);
      quad(pose, vc, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, bottom);
      quad(pose, vc, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, side);
      quad(pose, vc, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, side);
      quad(pose, vc, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, side);
      quad(pose, vc, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, side);
   }

   private static void quad(
      Pose pose,
      VertexConsumer vc,
      float ax,
      float ay,
      float az,
      float bx,
      float by,
      float bz,
      float cx,
      float cy,
      float cz,
      float dx,
      float dy,
      float dz,
      int col
   ) {
      tri(pose, vc, ax, ay, az, bx, by, bz, cx, cy, cz, col);
      tri(pose, vc, ax, ay, az, cx, cy, cz, dx, dy, dz, col);
   }

   private static void tri(Pose pose, VertexConsumer vc, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, int col) {
      vc.addVertex(pose, ax, ay, az).setColor(col);
      vc.addVertex(pose, bx, by, bz).setColor(col);
      vc.addVertex(pose, cx, cy, cz).setColor(col);
      vc.addVertex(pose, ax, ay, az).setColor(col);
      vc.addVertex(pose, cx, cy, cz).setColor(col);
      vc.addVertex(pose, bx, by, bz).setColor(col);
   }

   private static void crackle(PoseStack ps, VfxDraw.Sink sink, float[][] boxes, float outer, float in, float age, int seed, int core) {
      int count = Math.round((in - 0.65F) / 0.35000002F * 7.0F);
      if (count > 0) {
         int window = seed * 31 + (int)(age / 2.0F);
         float lift = outer + 0.0375F;
         int white = VfxColor.lighten(core, 0.5F);
         VfxDraw.flame(ps, sink, (pose, vc) -> {
            for (int i = 0; i < count; i++) {
               float[] b = boxes[i % boxes.length];
               int face = (int)(VoxelFlameDraw.noise01(window, i, 1) * 6.0F) % 6;
               float u = VoxelFlameDraw.noise01(window, i, 2);
               float v = VoxelFlameDraw.noise01(window, i, 3);
               float x0 = b[0] / 16.0F - lift;
               float y0 = b[1] / 16.0F - lift;
               float z0 = b[2] / 16.0F - lift;
               float x1 = b[3] / 16.0F + lift;
               float y1 = b[4] / 16.0F + lift;
               float z1 = b[5] / 16.0F + lift;
               float x = Mth.lerp(u, x0, x1);
               float y = Mth.lerp(v, y0, y1);
               float z = Mth.lerp(u, z0, z1);
               switch (face) {
                  case 0:
                     x = x0;
                     break;
                  case 1:
                     x = x1;
                     break;
                  case 2:
                     y = y0;
                     z = Mth.lerp(v, z0, z1);
                     break;
                  case 3:
                     y = y1;
                     z = Mth.lerp(v, z0, z1);
                     break;
                  case 4:
                     z = z0;
                     break;
                  default:
                     z = z1;
               }

               float half = (0.35F + 0.4F * VoxelFlameDraw.noise01(window, i, 4)) / 16.0F;
               int axis = (int)(VoxelFlameDraw.noise01(window, i, 5) * 3.0F) % 3;
               float ang = VoxelFlameDraw.noise01(window, i, 6) * 6.2832F + age * 0.4F;
               int rgb = VoxelFlameDraw.noise01(window, i, 7) < 0.5F ? core : white;
               VoxelFlameDraw.cube(pose, vc, x, y, z, half, axis, ang, 170, rgb);
            }
         });
      }
   }

   static float[][] boxes(ModelPart part) {
      float[][] cached = BOXES.get(part);
      if (cached != null) {
         return cached;
      } else {
         List<float[]> out = new ArrayList<>();
         part.visit(new PoseStack(), (pose, path, index, cube) -> {
            if (path.isEmpty()) {
               out.add(new float[]{cube.minX, cube.minY, cube.minZ, cube.maxX, cube.maxY, cube.maxZ});
            }
         });
         float[][] boxes = out.toArray(new float[0][]);
         if (BOXES.size() >= 64) {
            BOXES.clear();
         }

         BOXES.put(part, boxes);
         return boxes;
      }
   }
}
