package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.render.VfxDraw;
import com.devilfruits.client.render.vfx.VfxContext;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class SandArt {
   public static final ResourceLocation PACKED = texture("packed_sand");
   public static final ResourceLocation STREAM = texture("stream");
   public static final ResourceLocation STORM = texture("storm");
   public static final ResourceLocation WAVE = texture("wave");
   public static final ResourceLocation FIN = texture("spada_fin");
   public static final ResourceLocation BARJAN = texture("barjan");
   public static final int BARJAN_FRAMES = 4;
   public static final ResourceLocation BLADE = texture("blade");
   private static final Matrix4f POSE = new Matrix4f();
   private static final Matrix3f ROT = new Matrix3f();

   private static ResourceLocation texture(String name) {
      return DevilFruits.id("textures/entity/suna_suna/" + name + ".png");
   }

   public static VertexConsumer solid(VfxContext ctx, ResourceLocation texture) {
      return ctx.buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
   }

   public static VertexConsumer wispy(VfxContext ctx, ResourceLocation texture) {
      return ctx.buffers.getBuffer(RenderType.entityTranslucent(texture));
   }

   public static int light(double x, double y, double z) {
      ClientLevel level = Minecraft.getInstance().level;
      return level == null ? 15728880 : LevelRenderer.getLightColor(level, BlockPos.containing(x, y, z));
   }

   public static void chunk(
      VfxContext ctx, VertexConsumer vc, double x, double y, double z, float hx, float hy, float hz, Quaternionf rot, int variant, int light
   ) {
      POSE.set(ctx.matrix).translate(ctx.rx(x), ctx.ry(y), ctx.rz(z)).rotate(rot);
      ROT.rotation(rot);
      VfxDraw.tiledBox(vc, POSE, ROT, hx, hy, hz, (float)(variant * 5 & 15) / 16.0F, (float)(variant * 9 + 3 & 15) / 16.0F, -1, light);
   }

   public static void vertex(VfxContext ctx, VertexConsumer vc, Vec3 p, float u, float v, int argb, int light) {
      vertex(ctx, vc, p, u, v, argb, light, 0.0F, 1.0F, 0.0F);
   }

   public static void vertex(VfxContext ctx, VertexConsumer vc, Vec3 p, float u, float v, int argb, int light, float nx, float ny, float nz) {
      vc.addVertex(ctx.matrix, ctx.rx(p.x), ctx.ry(p.y), ctx.rz(p.z))
         .setColor(argb)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(nx, ny, nz);
   }

   public static void quad(VfxContext ctx, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float u0, float v0, float u1, float v1, int argb, int light) {
      vertex(ctx, vc, a, u0, v0, argb, light);
      vertex(ctx, vc, b, u1, v0, argb, light);
      vertex(ctx, vc, c, u1, v1, argb, light);
      vertex(ctx, vc, d, u0, v1, argb, light);
   }

   public static void ribbon(
      VfxContext ctx,
      VertexConsumer vc,
      Vec3[] path,
      Vec3[] across,
      float[] halfWidth,
      float[] alpha,
      int count,
      float uScale,
      float scroll,
      int rgb,
      int light
   ) {
      float u = scroll;

      for (int i = 0; i + 1 < count; i++) {
         Vec3 a = path[i];
         Vec3 b = path[i + 1];
         float du = (float)a.distanceTo(b) * uScale;
         Vec3 wa = across[i].scale((double)halfWidth[i]);
         Vec3 wb = across[i + 1].scale((double)halfWidth[i + 1]);
         int ca = argb(rgb, alpha[i]);
         int cb = argb(rgb, alpha[i + 1]);
         vertex(ctx, vc, a.subtract(wa), u, 1.0F, ca, light);
         vertex(ctx, vc, a.add(wa), u, 0.0F, ca, light);
         vertex(ctx, vc, b.add(wb), u + du, 0.0F, cb, light);
         vertex(ctx, vc, b.subtract(wb), u + du, 1.0F, cb, light);
         u += du;
      }
   }

   public static int argb(int rgb, float alpha) {
      int a = Math.max(0, Math.min(255, (int)(alpha * 255.0F)));
      return a << 24 | rgb & 16777215;
   }

   private SandArt() {
   }
}
