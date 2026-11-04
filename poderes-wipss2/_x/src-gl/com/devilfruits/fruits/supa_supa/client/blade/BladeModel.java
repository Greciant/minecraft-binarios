package com.devilfruits.fruits.supa_supa.client.blade;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.model.geo.GeoModel;
import com.devilfruits.client.model.geo.GeoPose;
import com.devilfruits.client.model.geo.GeoRenderer;
import com.devilfruits.client.model.geo.GeoResources;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.fruits.supa_supa.BladeLimbs;
import com.devilfruits.fruits.supa_supa.SupaSupa;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BladeModel {
   public static final ResourceLocation MODEL = SupaSupa.id("blades");
   public static final ResourceLocation TEXTURE = DevilFruits.id("textures/entity/supa_supa/blades.png");
   public static final ResourceLocation EMISSIVE = DevilFruits.id("textures/entity/supa_supa/blades_emissive.png");
   public static final ResourceLocation STEEL = DevilFruits.id("textures/entity/supa_supa/blade_steel.png");
   private static final float WHITE_U = 0.984375F;
   private static final float WHITE_V = 0.984375F;
   private static GeoModel model;
   private static GeoRenderer geo;
   private static GeoPose pose;
   private static int generation = -1;
   private static final int[][] BONES = new int[2][];
   private static final int[] SKATE = new int[2];
   private static final int BLADE = 0;
   private static final int FLAP_F = 1;
   private static final int FLAP_B = 2;
   private static final int CLAWS = 3;
   private static final int INNER = 4;
   private static final int XBLADE = 5;
   private static final int XFLAP_F = 6;
   private static final int XFLAP_B = 7;
   private static final int SHOULDER = 8;
   private static final int SAW1 = 9;
   private static final int SAW2 = 10;
   private static final int SAW3 = 11;
   private static final String[] NAMES = new String[]{
      "blade", "flap_f", "flap_b", "claws", "inner", "xblade", "xflap_f", "xflap_b", "shoulder", "saw_1", "saw_2", "saw_3"
   };
   private static final Vector3f P = new Vector3f();
   private static final Vector3f N = new Vector3f();

   private static boolean ready() {
      GeoModel m = GeoResources.model(MODEL);
      if (m == null) {
         return false;
      } else {
         if (m != model || generation != GeoResources.MODELS.generation()) {
            model = m;
            generation = GeoResources.MODELS.generation();
            geo = new GeoRenderer(m);
            pose = new GeoPose(m);

            for (int side = 0; side < 2; side++) {
               String prefix = side == 0 ? "r_" : "l_";
               BONES[side] = new int[NAMES.length];

               for (int i = 0; i < NAMES.length; i++) {
                  BONES[side][i] = m.bone(prefix + NAMES[i]);
               }

               SKATE[side] = m.bone(prefix + "skate");
            }
         }

         return true;
      }
   }

   static float outBack(float x) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float t = x - 1.0F;
      return 1.0F + c3 * t * t * t + c1 * t * t;
   }

   static float smooth(float x) {
      x = Mth.clamp(x, 0.0F, 1.0F);
      return x * x * (3.0F - 2.0F * x);
   }

   static float extension(BladeLimbsClient.Limb limb, float partial, float delay) {
      float g = limb.growth(partial);
      if (!limb.growing()) {
         return smooth(g);
      } else {
         float x = Mth.clamp((g - delay) / (1.0F - delay), 0.0F, 1.0F);
         return x <= 0.0F ? 0.0F : outBack(x);
      }
   }

   public static void arm(
      PoseStack ps, MultiBufferSource buffers, int light, boolean right, boolean slim, BladeLimbsClient.Limb limb, float partial, float time, int skin
   ) {
      BladeLimbs.ArmMode mode = limb.shown();
      if (mode != BladeLimbs.ArmMode.NONE && ready()) {
         float g = limb.growth(partial);
         if (!(g <= 0.0F)) {
            int side = right ? 0 : 1;
            int[] b = BONES[side];
            float sx = right ? 1.0F : -1.0F;
            pose.reset();

            for (int i = 0; i < pose.hidden.length; i++) {
               pose.hidden[i] = true;
            }

            float age = limb.age(partial);
            float e = extension(limb, partial, 0.3F);
            float cut = BladeSkinMask.cut(limb, partial);
            float out = limb.growing() ? extension(limb, partial, 0.62F) : smooth(g * 2.0F - 1.0F);
            int lipA = -1;
            int lipB = -1;
            if (mode == BladeLimbs.ArmMode.FOREARM || mode == BladeLimbs.ArmMode.X_BLADES) {
               lipA = mode == BladeLimbs.ArmMode.FOREARM ? b[1] : b[6];
               lipB = mode == BladeLimbs.ArmMode.FOREARM ? b[2] : b[7];
               lips(lipA, lipB, cut, slim, sx);
            }

            CoreClientConfig.CinematicQuality tier = VfxSettings.tier();
            boolean fancy = tier != CoreClientConfig.CinematicQuality.LOW;
            ps.pushPose();
            if (slim && mode == BladeLimbs.ArmMode.CLAWS) {
               ps.translate(sx * 0.5F / 16.0F, 0.0F, 0.0F);
            }

            VertexConsumer steel = buffers.getBuffer(RenderType.entityCutoutNoCull(STEEL));
            switch (mode) {
               case FOREARM:
               case X_BLADES:
                  boolean x = mode == BladeLimbs.ArmMode.X_BLADES;
                  half(ps, steel, x ? (slim ? BladeShapes.XIN_SLIM : BladeShapes.XIN_WIDE) : (slim ? BladeShapes.IN_SLIM : BladeShapes.IN_WIDE), cut, sx, light);
                  blade(
                     ps,
                     steel,
                     x ? (slim ? BladeShapes.XOUT_SLIM : BladeShapes.XOUT_WIDE) : (slim ? BladeShapes.OUT_SLIM : BladeShapes.OUT_WIDE),
                     out,
                     sx,
                     light
                  );
                  break;
               case CLAWS:
                  float c = extension(limb, partial, 0.25F);

                  for (BladeMesh.Shape claw : BladeShapes.CLAWS) {
                     blade(ps, steel, claw, c, sx, light);
                  }

                  blade(ps, steel, BladeShapes.INNER, extension(limb, partial, 0.4F), sx, light);
                  break;
               case SAW:
                  saws(ps, buffers, limb, partial, time, sx, slim, light);
            }

            ps.pushPose();
            ps.scale(0.0625F, 0.0625F, 0.0625F);
            ps.mulPose(Axis.ZP.rotationDegrees(180.0F));
            geo.pose(ps.last(), pose);
            VertexConsumer geoSteel = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

            for (GeoModel.Bone bone : model.bones) {
               if (!pose.hidden[bone.index]) {
                  boolean lip = bone.index == lipA || bone.index == lipB;
                  geo.renderBone(geoSteel, bone, light, OverlayTexture.NO_OVERLAY, lip ? 0xFF000000 | skin : -1);
               }
            }

            if (fancy) {
               beats(ps, buffers, mode, limb, g, age, sx, slim);
            }

            ps.popPose();
            if (fancy) {
               float glow = Mth.clamp(e, 0.0F, 1.0F) * 0.55F;
               VertexConsumer em = buffers.getBuffer(RenderType.eyes(EMISSIVE));
               int edge = argb(glow, 0.8F, 0.9F, 1.0F);
               forEachShape(mode, slim, sh -> edge(ps, em, sh, BladeShapes.isInArm(sh) ? cut : out, sx, edge));
               if (tier != CoreClientConfig.CinematicQuality.MEDIUM && g >= 1.0F) {
                  VertexConsumer sheen = buffers.getBuffer(BladeRenderTypes.sheen());
                  forEachShape(mode, slim, sh -> BladeMesh.sheen(sheen, ps.last(), sh, sx));
               }
            }

            ps.popPose();
         }
      }
   }

   private static void saws(PoseStack ps, MultiBufferSource buffers, BladeLimbsClient.Limb limb, float partial, float time, float sx, boolean slim, int light) {
      float cx = slim ? -0.5F : -1.0F;
      float rIn = slim ? 2.6F : 2.95F;
      float spinUp = smooth(limb.growth(partial));
      VertexConsumer steel = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
      float[] ys = new float[]{3.4F, 5.0F, 6.6F, 8.2F};

      for (int k = 0; k < ys.length; k++) {
         float e = extension(limb, partial, 0.2F + 0.08F * (float)k);
         if (!(e <= 0.001F)) {
            float rOut = rIn + 1.55F * e;
            float r0 = rIn * Math.min(1.0F, 0.6F + 0.4F * e);
            float tilt = k % 2 == 0 ? 0.24F : -0.12F;
            float angle = time * 0.95F * spinUp + (float)k * 0.6F;
            BladeMesh.ring(steel, ps.last(), cx, ys[k], r0, rOut, 0.75F, 9, tilt, angle, sx, light, -722689, -3153665, 0.984375F, 0.984375F);
         }
      }

      if (VfxSettings.tier() != CoreClientConfig.CinematicQuality.LOW && spinUp > 0.3F) {
         VertexConsumer em = buffers.getBuffer(RenderType.eyes(EMISSIVE));

         for (int kx = 0; kx < ys.length; kx++) {
            float tilt = kx % 2 == 0 ? 0.24F : -0.12F;
            BladeMesh.ringBlur(
               em,
               ps.last(),
               cx,
               ys[kx],
               rIn + 0.4F,
               rIn + 1.9F,
               tilt,
               sx,
               argb(0.05F * spinUp, 0.8F, 0.9F, 1.0F),
               argb(0.22F * spinUp, 0.9F, 0.95F, 1.0F),
               0.984375F,
               0.984375F
            );
         }
      }
   }

   private static void forEachShape(BladeLimbs.ArmMode mode, boolean slim, Consumer<BladeMesh.Shape> action) {
      switch (mode) {
         case FOREARM:
            action.accept(slim ? BladeShapes.IN_SLIM : BladeShapes.IN_WIDE);
            action.accept(slim ? BladeShapes.OUT_SLIM : BladeShapes.OUT_WIDE);
            break;
         case X_BLADES:
            action.accept(slim ? BladeShapes.XIN_SLIM : BladeShapes.XIN_WIDE);
            action.accept(slim ? BladeShapes.XOUT_SLIM : BladeShapes.XOUT_WIDE);
            break;
         case CLAWS:
            for (BladeMesh.Shape claw : BladeShapes.CLAWS) {
               action.accept(claw);
            }

            action.accept(BladeShapes.INNER);
      }
   }

   private static void edge(PoseStack ps, VertexConsumer em, BladeMesh.Shape sh, float e, float sx, int argb) {
      if (!(e <= 0.0F)) {
         ps.pushPose();
         place(ps, sh, e, sx);
         BladeMesh.edgeLine(em, ps.last(), sh, sx, BladeShapes.isInArm(sh) ? Math.min(1.0F, e * 1.08F) : Math.min(1.0F, e), argb, 0.984375F, 0.984375F);
         ps.popPose();
      }
   }

   private static void half(PoseStack ps, VertexConsumer vc, BladeMesh.Shape sh, float cut, float sx, int light) {
      if (!(cut <= 0.001F)) {
         BladeMesh.emit(vc, ps.last(), sh, sx, Math.min(1.0F, cut * 1.08F + 0.04F), light, -1, 1.0F);
      }
   }

   private static void blade(PoseStack ps, VertexConsumer vc, BladeMesh.Shape sh, float e, float sx, int light) {
      if (!(e <= 0.001F)) {
         ps.pushPose();
         place(ps, sh, e, sx);
         BladeMesh.emit(vc, ps.last(), sh, sx, BladeShapes.isPastFist(sh) ? 1.0F : Math.min(1.0F, e), light, -1, 1.0F);
         ps.popPose();
      }
   }

   private static void place(PoseStack ps, BladeMesh.Shape sh, float e, float sx) {
      if (!BladeShapes.isInArm(sh)) {
         if (BladeShapes.isPastFist(sh)) {
            float back = (1.0F - e) * (sh.length - 0.4F) / 16.0F;
            ps.translate(-sh.a[0] * sx * back, -sh.a[1] * back, -sh.a[2] * back);
         } else {
            float inward = 2.2F * (1.0F - Mth.clamp(e, 0.0F, 1.0F));
            ps.translate(sx * inward / 16.0F, 0.0F, 0.0F);
            if (e > 1.0F) {
               float k = (e - 1.0F) * sh.length * 0.35F / 16.0F;
               ps.translate(sh.a[0] * sx * k, sh.a[1] * k, sh.a[2] * k);
            }
         }
      }
   }

   public static void leg(PoseStack ps, MultiBufferSource buffers, int light, boolean right, BladeLimbsClient.Limb limb, float partial) {
      float g = limb.growth(partial);
      if (!(g <= 0.0F)) {
         float e = extension(limb, partial, 0.2F);
         float sx = right ? 1.0F : -1.0F;
         ps.pushPose();
         ps.translate(0.0F, 0.75F, 0.0F);
         ps.scale(1.0F, Math.max(0.02F, e), 1.0F);
         ps.translate(0.0F, -0.75F, 0.0F);
         BladeMesh.emit(buffers.getBuffer(RenderType.entityCutoutNoCull(STEEL)), ps.last(), BladeShapes.SKATE, sx, 1.0F, light, -1, 1.0F);
         if (VfxSettings.tier() != CoreClientConfig.CinematicQuality.LOW) {
            BladeMesh.edgeLine(
               buffers.getBuffer(RenderType.eyes(EMISSIVE)),
               ps.last(),
               BladeShapes.SKATE,
               sx,
               1.0F,
               argb(Mth.clamp(e, 0.0F, 1.0F) * 0.5F, 0.8F, 0.9F, 1.0F),
               0.984375F,
               0.984375F
            );
         }

         ps.popPose();
      }
   }

   private static void show(int bone, float s) {
      if (bone >= 0) {
         pose.hide(bone, s <= 0.001F);
         pose.setScale(bone, Math.max(0.001F, s));
      }
   }

   private static void lips(int outer, int inner, float cut, boolean slim, float sx) {
      for (int bone : new int[]{outer, inner}) {
         if (bone >= 0) {
            pose.hide(bone, cut <= 0.001F);
            pose.scale[bone * 3 + 1] = Math.max(0.001F, Math.min(1.0F, cut * 1.05F));
            if (slim && bone == outer) {
               pose.addPosition(bone, -sx, 0.0F, 0.0F);
            }
         }
      }
   }

   private static void beats(
      PoseStack ps, MultiBufferSource buffers, BladeLimbs.ArmMode mode, BladeLimbsClient.Limb limb, float g, float age, float sx, boolean slim
   ) {
      if (limb.growing()) {
         VertexConsumer vc = null;
         Matrix4f m = ps.last().pose();
         Matrix3f nm = ps.last().normal();
         float outer = slim ? -2.04F : -3.04F;
         if (mode != BladeLimbs.ArmMode.FOREARM && mode != BladeLimbs.ArmMode.X_BLADES) {
            if (mode == BladeLimbs.ArmMode.CLAWS && g < 0.45F) {
               vc = buffers.getBuffer(RenderType.eyes(EMISSIVE));
               float head = 4.0F + 6.5F * smooth(g / 0.4F);
               front(vc, m, nm, head, 1.0F - smooth((g - 0.32F) / 0.13F), sx);
            }
         } else {
            if (g < 0.42F) {
               vc = buffers.getBuffer(RenderType.eyes(EMISSIVE));
               float head = -2.0F + 12.5F * smooth(g / 0.36F);
               float tail = Math.max(-2.0F, head - 6.0F);
               float fade = 1.0F - smooth((g - 0.3F) / 0.12F);
               line(vc, m, nm, outer * sx, tail, head, 0.22F, fade, sx);
            }

            if (g > 0.2F && g < 0.62F) {
               vc = vc != null ? vc : buffers.getBuffer(RenderType.eyes(EMISSIVE));
               float a = Mth.sin((g - 0.2F) / 0.42F * (float) Math.PI);
               line(vc, m, nm, (outer - 0.02F) * sx, mode == BladeLimbs.ArmMode.X_BLADES ? 3.0F : 3.5F, 10.0F, 0.42F, a * 0.9F, sx);
            }
         }
      }
   }

   private static void line(VertexConsumer vc, Matrix4f m, Matrix3f nm, float x, float y0, float y1, float halfWidth, float alpha, float sx) {
      if (!(alpha <= 0.01F) && !(y1 <= y0)) {
         int head = argb(alpha, 1.0F, 1.0F, 1.0F);
         int tail = argb(alpha * 0.15F, 0.6F, 0.78F, 1.0F);
         quadV(vc, m, nm, x, y0, y1, -halfWidth, halfWidth, tail, head, -sx);
      }
   }

   private static void front(VertexConsumer vc, Matrix4f m, Matrix3f nm, float head, float alpha, float sx) {
      if (!(alpha <= 0.01F)) {
         float y0 = Math.max(4.0F, head - 4.0F);
         int c0 = argb(alpha * 0.15F, 0.6F, 0.78F, 1.0F);
         int c1 = argb(alpha, 1.0F, 1.0F, 1.0F);
         float z = -2.04F;
         float xa = -1.2F * sx;
         float xb = -0.8F * sx;
         vertex(vc, m, nm, -xa, -y0, z, c0, 0.0F, 0.0F, -1.0F);
         vertex(vc, m, nm, -xb, -y0, z, c0, 0.0F, 0.0F, -1.0F);
         vertex(vc, m, nm, -xb, -head, z, c1, 0.0F, 0.0F, -1.0F);
         vertex(vc, m, nm, -xa, -head, z, c1, 0.0F, 0.0F, -1.0F);
      }
   }

   private static void quadV(VertexConsumer vc, Matrix4f m, Matrix3f nm, float x, float y0, float y1, float z0, float z1, int c0, int c1, float nx) {
      vertex(vc, m, nm, -x, -y0, z0, c0, nx, 0.0F, 0.0F);
      vertex(vc, m, nm, -x, -y0, z1, c0, nx, 0.0F, 0.0F);
      vertex(vc, m, nm, -x, -y1, z1, c1, nx, 0.0F, 0.0F);
      vertex(vc, m, nm, -x, -y1, z0, c1, nx, 0.0F, 0.0F);
   }

   private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f nm, float x, float y, float z, int argb, float nx, float ny, float nz) {
      m.transformPosition(x, y, z, P);
      nm.transform(nx, ny, nz, N);
      vc.addVertex(P.x, P.y, P.z, argb, 0.984375F, 0.984375F, OverlayTexture.NO_OVERLAY, 15728880, N.x, N.y, N.z);
   }

   private static void sheenBone(VertexConsumer vc, GeoModel.Bone bone) {
      float[] mesh = bone.mesh;
      Matrix4f m = geo.boneMatrix(bone.index);

      for (int v = 0; v < mesh.length; v += 8) {
         m.transformPosition(mesh[v], mesh[v + 1], mesh[v + 2], P);
         vc.addVertex(P.x, P.y, P.z).setUv(mesh[v] * 0.03F + mesh[v + 1] * 0.05F, mesh[v + 2] * 0.03F - mesh[v + 1] * 0.02F);
      }
   }

   static int argb(float a, float r, float g, float b) {
      return Mth.clamp((int)(a * 255.0F), 0, 255) << 24
         | Mth.clamp((int)(r * 255.0F), 0, 255) << 16
         | Mth.clamp((int)(g * 255.0F), 0, 255) << 8
         | Mth.clamp((int)(b * 255.0F), 0, 255);
   }

   @Nullable
   static GeoModel model() {
      return ready() ? model : null;
   }

   private BladeModel() {
   }
}
