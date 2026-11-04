package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.pyrokinetic.FlameStyle;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public final class VoxelFlameDraw {
   private static final float BRIGHT = 0.65F;
   private static final float BODY_W = 0.6F;
   private static final float GLOW_W = 0.65F;
   private static final float DISC_BODY_W = 0.2F;
   private static VoxelFlameDraw.Pass pass = VoxelFlameDraw.Pass.SOLO;
   private static final int DISC_SEGS = 10;
   private static final float INNER = 0.45F;
   private static final float MID_ALPHA = 0.55F;
   private static final float[] XS = new float[8];
   private static final float[] YS = new float[8];
   private static final float[] ZS = new float[8];
   private static final float BASE_SHADE = 0.5F;
   private static final int VARIANTS = 3;
   private static final VoxelFlameDraw.Ladders[] LADDERS = new VoxelFlameDraw.Ladders[FlameStyle.values().length];
   private static final int BLEND_STEPS = 32;
   private static final VoxelFlameDraw.Blend[] BLENDS = new VoxelFlameDraw.Blend[33];
   private static final float BREATHE_RATE = 0.22F;
   private static final float BREATHE_AMOUNT = 0.18F;

   private VoxelFlameDraw() {
   }

   private static int[] tiers(int bodyRgb, int midRgb, int tipRgb) {
      return new int[]{VfxColor.shade(bodyRgb, 0.5F), bodyRgb, midRgb, tipRgb};
   }

   public static VoxelFlameDraw.Ladders ladders(FlameStyle style) {
      VoxelFlameDraw.Ladders l = LADDERS[style.ordinal()];
      if (l == null) {
         l = ladders(style.body(), style.mid(), style.tip(), style.hueSpread());
         LADDERS[style.ordinal()] = l;
      }

      return l;
   }

   public static VoxelFlameDraw.Ladders ladders(int body, int mid, int tip, float hueSpread) {
      return new VoxelFlameDraw.Ladders(
         tiers(body, mid, tip),
         tiers(VfxColor.hueShift(body, -hueSpread), VfxColor.hueShift(mid, -hueSpread), VfxColor.hueShift(tip, -hueSpread)),
         tiers(VfxColor.hueShift(body, hueSpread), VfxColor.hueShift(mid, hueSpread), VfxColor.hueShift(tip, hueSpread))
      );
   }

   public static VoxelFlameDraw.Palette blend(float blueFraction) {
      int bucket = Mth.clamp(Math.round(blueFraction * 32.0F), 0, 32);
      VoxelFlameDraw.Ladders orange = ladders(FlameStyle.ORANGE);
      VoxelFlameDraw.Blend b = BLENDS[bucket];
      if (b == null) {
         b = VoxelFlameDraw.Blend.of(orange, ladders(FlameStyle.BLUE), (float)bucket / 32.0F);
         BLENDS[bucket] = b;
      }

      return b;
   }

   public static VoxelFlameDraw.Palette laddersFor(float heat01) {
      return blend(FlameStyle.blueFraction(heat01));
   }

   public static int gradient(int[] tiers, float t) {
      return t < 0.5F ? mix(tiers[1], tiers[2], t * 2.0F) : mix(tiers[2], tiers[3], (t - 0.5F) * 2.0F);
   }

   public static float noise01(int a, int b, int c) {
      return VfxGeometry.noise01(a, b, c);
   }

   public static float breathe(float age, int phaseSeed, float rateScale) {
      return 1.0F + 0.18F * Mth.sin(age * 0.22F * rateScale + (float)phaseSeed * 0.19F);
   }

   public static float breathe(float age, int phaseSeed) {
      return breathe(age, phaseSeed, 1.0F);
   }

   public static int mix(int c1, int c2, float t) {
      float f = Mth.clamp(t, 0.0F, 1.0F);
      int r = (int)Mth.lerp(f, (float)(c1 >> 16 & 0xFF), (float)(c2 >> 16 & 0xFF));
      int g = (int)Mth.lerp(f, (float)(c1 >> 8 & 0xFF), (float)(c2 >> 8 & 0xFF));
      int b = (int)Mth.lerp(f, (float)(c1 & 0xFF), (float)(c2 & 0xFF));
      return r << 16 | g << 8 | b;
   }

   public static Vector3f[] cameraAxes() {
      Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
      Vector3f up = new Vector3f(camera.getUpVector());
      Vector3f right = new Vector3f(camera.getLeftVector()).mul(-1.0F);
      return new Vector3f[]{right, up};
   }

   static void pass(VoxelFlameDraw.Pass p, Pose pose, VertexConsumer vc, VfxDraw.Geometry g) {
      VoxelFlameDraw.Pass prev = pass;
      pass = p;

      try {
         g.build(pose, vc);
      } finally {
         pass = prev;
      }
   }

   public static void twoPass(MultiBufferSource buffers, Consumer<VertexConsumer> draw) {
      VoxelFlameDraw.Pass prev = pass;

      try {
         pass = VoxelFlameDraw.Pass.BODY;
         draw.accept(FxLayers.begin(buffers, FxLayer.PAINT));
         pass = VoxelFlameDraw.Pass.GLOW;
         draw.accept(FxLayers.begin(buffers, FxLayer.LIGHT));
      } finally {
         pass = prev;
      }
   }

   public static void cube(Pose pose, VertexConsumer vc, float cx, float cy, float cz, float half, int axis, float ang, int alpha, int rgb) {
      float a = (float)alpha * 0.65F;

      alpha = Math.min(255, Math.round(switch (pass) {
         case BODY -> 0.6F;
         case GLOW -> 0.65F;
      }));
      if (alpha > 0) {
         int top = VfxColor.argb(alpha, VfxColor.lighten(rgb, 0.3F));
         int side = VfxColor.argb(alpha, rgb);
         int bottom = VfxColor.argb(alpha, VfxColor.shade(rgb, 0.7F));
         float cosA = Mth.cos(ang);
         float sinA = Mth.sin(ang);
         float[] xs = XS;
         float[] ys = YS;
         float[] zs = ZS;

         for (int i = 0; i < 8; i++) {
            float x = (i & 1) == 0 ? -half : half;
            float y = (i & 2) == 0 ? -half : half;
            float z = (i & 4) == 0 ? -half : half;
            float rx = x;
            float ry = y;
            float rz = z;
            switch (axis) {
               case 0:
                  ry = y * cosA - z * sinA;
                  rz = y * sinA + z * cosA;
                  break;
               case 1:
                  rx = x * cosA + z * sinA;
                  rz = -x * sinA + z * cosA;
                  break;
               default:
                  rx = x * cosA - y * sinA;
                  ry = x * sinA + y * cosA;
            }

            xs[i] = cx + rx;
            ys[i] = cy + ry;
            zs[i] = cz + rz;
         }

         quad(pose, vc, xs, ys, zs, 2, 3, 7, 6, top);
         quad(pose, vc, xs, ys, zs, 0, 4, 5, 1, bottom);
         quad(pose, vc, xs, ys, zs, 0, 2, 6, 4, side);
         quad(pose, vc, xs, ys, zs, 1, 5, 7, 3, side);
         quad(pose, vc, xs, ys, zs, 0, 1, 3, 2, side);
         quad(pose, vc, xs, ys, zs, 4, 6, 7, 5, side);
      }
   }

   public static void disc(Pose pose, VertexConsumer vc, float cx, float cy, float cz, Vector3f right, Vector3f up, float r, int alpha, int rgb) {
      float aF = (float)alpha * 0.65F;
      boolean coreOnly = pass == VoxelFlameDraw.Pass.BODY;
      if (coreOnly) {
         aF *= 0.2F;
      }

      alpha = Math.min(255, Math.round(aF));
      if (alpha > 0) {
         int centerCol = VfxColor.argb(alpha, VfxColor.lighten(rgb, 0.12F));
         int midCol = VfxColor.argb(coreOnly ? 0 : (int)((float)alpha * 0.55F), rgb);
         int rimCol = VfxColor.argb(0, rgb);
         float ri = r * 0.45F;

         for (int j = 0; j < 10; j++) {
            float a0 = (float)((Math.PI * 2) * (double)j / 10.0);
            float a1 = (float)((Math.PI * 2) * (double)(j + 1) / 10.0);
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            float dx0 = right.x * c0 + up.x * s0;
            float dy0 = right.y * c0 + up.y * s0;
            float dz0 = right.z * c0 + up.z * s0;
            float dx1 = right.x * c1 + up.x * s1;
            float dy1 = right.y * c1 + up.y * s1;
            float dz1 = right.z * c1 + up.z * s1;
            tri(pose, vc, cx, cy, cz, centerCol, cx + dx0 * ri, cy + dy0 * ri, cz + dz0 * ri, midCol, cx + dx1 * ri, cy + dy1 * ri, cz + dz1 * ri, midCol);
            if (!coreOnly) {
               tri(
                  pose,
                  vc,
                  cx + dx0 * ri,
                  cy + dy0 * ri,
                  cz + dz0 * ri,
                  midCol,
                  cx + dx0 * r,
                  cy + dy0 * r,
                  cz + dz0 * r,
                  rimCol,
                  cx + dx1 * r,
                  cy + dy1 * r,
                  cz + dz1 * r,
                  rimCol
               );
               tri(
                  pose,
                  vc,
                  cx + dx0 * ri,
                  cy + dy0 * ri,
                  cz + dz0 * ri,
                  midCol,
                  cx + dx1 * r,
                  cy + dy1 * r,
                  cz + dz1 * r,
                  rimCol,
                  cx + dx1 * ri,
                  cy + dy1 * ri,
                  cz + dz1 * ri,
                  midCol
               );
            }
         }
      }
   }

   private static void quad(Pose pose, VertexConsumer vc, float[] xs, float[] ys, float[] zs, int a, int b, int c, int d, int col) {
      cubeTri(pose, vc, xs, ys, zs, a, b, c, col);
      cubeTri(pose, vc, xs, ys, zs, a, c, d, col);
   }

   private static void cubeTri(Pose pose, VertexConsumer vc, float[] xs, float[] ys, float[] zs, int a, int b, int c, int col) {
      vc.addVertex(pose, xs[a], ys[a], zs[a]).setColor(col);
      vc.addVertex(pose, xs[b], ys[b], zs[b]).setColor(col);
      vc.addVertex(pose, xs[c], ys[c], zs[c]).setColor(col);
      vc.addVertex(pose, xs[a], ys[a], zs[a]).setColor(col);
      vc.addVertex(pose, xs[c], ys[c], zs[c]).setColor(col);
      vc.addVertex(pose, xs[b], ys[b], zs[b]).setColor(col);
   }

   private static void tri(
      Pose pose, VertexConsumer vc, float x0, float y0, float z0, int col0, float x1, float y1, float z1, int col1, float x2, float y2, float z2, int col2
   ) {
      vc.addVertex(pose, x0, y0, z0).setColor(col0);
      vc.addVertex(pose, x1, y1, z1).setColor(col1);
      vc.addVertex(pose, x2, y2, z2).setColor(col2);
      vc.addVertex(pose, x0, y0, z0).setColor(col0);
      vc.addVertex(pose, x2, y2, z2).setColor(col2);
      vc.addVertex(pose, x1, y1, z1).setColor(col1);
   }

   public static record Blend(VoxelFlameDraw.Ladders orange, VoxelFlameDraw.Ladders blue, float blueFraction, int[] tiers) implements VoxelFlameDraw.Palette {
      private static final int SALT_A = 81;
      private static final int SALT_B = 177;

      static VoxelFlameDraw.Blend of(VoxelFlameDraw.Ladders orange, VoxelFlameDraw.Ladders blue, float blueFraction) {
         int[] o = orange.base();
         int[] b = blue.base();
         int[] mixed = new int[o.length];

         for (int i = 0; i < mixed.length; i++) {
            mixed[i] = VoxelFlameDraw.mix(o[i], b[i], blueFraction);
         }

         return new VoxelFlameDraw.Blend(orange, blue, blueFraction, mixed);
      }

      @Override
      public int[] base() {
         return this.tiers;
      }

      @Override
      public int[] pick(int seed) {
         boolean isBlue = this.blueFraction >= 1.0F || this.blueFraction > 0.0F && VoxelFlameDraw.noise01(seed, 81, 177) < this.blueFraction;
         return isBlue ? this.blue.pick(seed) : this.orange.pick(seed);
      }
   }

   public static record Ladders(int[] base, int[] warm, int[] cool) implements VoxelFlameDraw.Palette {
      @Override
      public int[] pick(int seed) {
         int h = seed * -1640531535;
         h ^= h >>> 15;

         return switch (Math.floorMod(h, 3)) {
            case 1 -> this.warm;
            case 2 -> this.cool;
            default -> this.base;
         };
      }
   }

   public interface Palette {
      int[] base();

      int[] pick(int var1);
   }

   static enum Pass {
      SOLO,
      BODY,
      GLOW;
   }
}
