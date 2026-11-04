package com.devilfruits.client.logia;

import com.devilfruits.client.vfx.VfxSettings;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderStateShard.TexturingStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class LogiaBodyShader {
   @Nullable
   public static ShaderInstance shader;
   private static final float[][] AXES = new float[][]{
      {0.0F, 0.0F, 0.0F, 0.0F, -8.0F, 0.0F},
      {0.0F, 0.0F, 0.0F, 0.0F, 12.0F, 0.0F},
      {-1.0F, -1.0F, 0.0F, -1.0F, 10.0F, 0.0F},
      {1.0F, -1.0F, 0.0F, 1.0F, 10.0F, 0.0F},
      {0.0F, 0.0F, 0.0F, 0.0F, 12.0F, 0.0F},
      {0.0F, 0.0F, 0.0F, 0.0F, 12.0F, 0.0F}
   };
   private static final Int2ObjectMap<LogiaBodyShader.Uniforms> DATA = new Int2ObjectOpenHashMap();
   @Nullable
   private static DynamicTexture wideMap;
   @Nullable
   private static DynamicTexture slimMap;
   private static final Matrix4f[] LIMB = new Matrix4f[6];
   private static final Matrix4f INVERSE = new Matrix4f();
   private static final Vector3f V = new Vector3f();
   private static boolean worldPass;

   public static boolean active() {
      return shader != null && !VfxSettings.legacy();
   }

   @Nullable
   public static RenderType type(AbstractClientPlayer player) {
      if (!active()) {
         return null;
      } else {
         LogiaRenderState state = LogiaRenderState.get(player);
         if (state != null && state.visiblyChanged() && !state.hidden()) {
            PlayerSkin skin = player.getSkin();
            LogiaBodyShader.Uniforms u = (LogiaBodyShader.Uniforms)DATA.computeIfAbsent(player.getId(), k -> new LogiaBodyShader.Uniforms());
            if (u.type == null || !skin.texture().equals(u.skin)) {
               u.skin = skin.texture();
               u.type = LogiaBodyShader.Types.create(skin.texture(), player.getId());
            }

            u.slim = skin.model() == Model.SLIM;
            u.edge = LogiaThemes.current(player).edgeColor();
            return u.type;
         } else {
            return null;
         }
      }
   }

   public static void begin(AbstractClientPlayer player, PoseStack pose, float partial) {
      Minecraft mc = Minecraft.getInstance();
      Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
      Vec3 at = player.getPosition(partial).subtract(cam);
      pose.last().pose().getTranslation(V);
      worldPass = Math.abs((double)V.x - at.x) + Math.abs((double)V.y - at.y) + Math.abs((double)V.z - at.z) < 0.05;
   }

   public static void pose(AbstractClientPlayer player, PlayerModel<?> model, PoseStack pose) {
      LogiaBodyShader.Uniforms u = (LogiaBodyShader.Uniforms)DATA.get(player.getId());
      LogiaRenderState state = LogiaRenderState.get(player);
      if (u != null && state != null && active()) {
         Matrix4f base = pose.last().pose();
         limb(base, model.head, 0);
         limb(base, model.body, 1);
         limb(base, model.rightArm, 2);
         limb(base, model.leftArm, 3);
         limb(base, model.rightLeg, 4);
         limb(base, model.leftLeg, 5);

         for (int i = 0; i < 6; i++) {
            axis(u, i, LIMB[i], state.bodyLevel(i));
         }

         if (worldPass) {
            Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            state.bindTunnels(cam, LIMB, u);
         }

         holes(u, state, -1);
      }
   }

   @Nullable
   public static RenderType arm(AbstractClientPlayer player, ModelPart arm, int limb, PoseStack pose) {
      RenderType type = type(player);
      LogiaBodyShader.Uniforms u = (LogiaBodyShader.Uniforms)DATA.get(player.getId());
      LogiaRenderState state = LogiaRenderState.get(player);
      if (type != null && u != null && state != null) {
         limb(pose.last().pose(), arm, limb);

         for (int i = 0; i < 6; i++) {
            axis(u, i, LIMB[limb], i == limb ? state.bodyLevel(i) : 0.0F);
         }

         holes(u, state, limb);
         return type;
      } else {
         return null;
      }
   }

   private static void limb(Matrix4f base, ModelPart part, int index) {
      Matrix4f m = LIMB[index].set(base).translate(part.x / 16.0F, part.y / 16.0F, part.z / 16.0F);
      if (part.xRot != 0.0F || part.yRot != 0.0F || part.zRot != 0.0F) {
         m.rotate(new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot));
      }

      if (part.xScale != 1.0F || part.yScale != 1.0F || part.zScale != 1.0F) {
         m.scale(part.xScale, part.yScale, part.zScale);
      }
   }

   private static void axis(LogiaBodyShader.Uniforms u, int i, Matrix4f m, float level) {
      float[] a = AXES[i];
      float slim = u.slim && (i == 2 || i == 3) ? (i == 2 ? 0.5F : -0.5F) : 0.0F;
      m.transformPosition((a[0] + slim) / 16.0F, a[1] / 16.0F, a[2] / 16.0F, V);
      u.root[i * 4] = V.x;
      u.root[i * 4 + 1] = V.y;
      u.root[i * 4 + 2] = V.z;
      u.root[i * 4 + 3] = level;
      m.transformPosition((a[3] + slim) / 16.0F, a[4] / 16.0F, a[5] / 16.0F, V);
      u.tip[i * 4] = V.x;
      u.tip[i * 4 + 1] = V.y;
      u.tip[i * 4 + 2] = V.z;
   }

   private static void holes(LogiaBodyShader.Uniforms u, LogiaRenderState state, int only) {
      u.holes = 0;

      for (LogiaRenderState.Tunnel t : state.tunnels()) {
         if (u.holes >= 8) {
            break;
         }

         if (t.limb >= 0 && (only < 0 || t.limb == only)) {
            float r = t.radius();
            if (!(r <= 0.0F)) {
               Matrix4f m = LIMB[t.limb];
               int k = u.holes * 4;
               m.transformPosition(t.localA, V);
               u.holeA[k] = V.x;
               u.holeA[k + 1] = V.y;
               u.holeA[k + 2] = V.z;
               u.holeA[k + 3] = r;
               m.transformPosition(t.localB, V);
               u.holeB[k] = V.x;
               u.holeB[k + 1] = V.y;
               u.holeB[k + 2] = V.z;
               u.holes++;
            }
         }
      }
   }

   static void upload(int id) {
      ShaderInstance s = shader;
      LogiaBodyShader.Uniforms u = (LogiaBodyShader.Uniforms)DATA.get(id);
      if (s != null && u != null) {
         for (int i = 0; i < 8; i++) {
            boolean on = i < u.holes;
            s.safeGetUniform("H" + i + "A")
               .set(on ? u.holeA[i * 4] : 0.0F, on ? u.holeA[i * 4 + 1] : 0.0F, on ? u.holeA[i * 4 + 2] : 0.0F, on ? u.holeA[i * 4 + 3] : 0.0F);
            s.safeGetUniform("H" + i + "B").set(on ? u.holeB[i * 4] : 0.0F, on ? u.holeB[i * 4 + 1] : 0.0F, on ? u.holeB[i * 4 + 2] : 0.0F, 0.0F);
         }

         s.safeGetUniform("HoleCount").set((float)u.holes);

         for (int i = 0; i < 6; i++) {
            s.safeGetUniform("L" + i + "R").set(u.root[i * 4], u.root[i * 4 + 1], u.root[i * 4 + 2], u.root[i * 4 + 3]);
            s.safeGetUniform("L" + i + "T").set(u.tip[i * 4], u.tip[i * 4 + 1], u.tip[i * 4 + 2], 0.0F);
         }

         s.safeGetUniform("EdgeColor").set((float)(u.edge >> 16 & 0xFF) / 255.0F, (float)(u.edge >> 8 & 0xFF) / 255.0F, (float)(u.edge & 0xFF) / 255.0F);
         s.setSampler("LimbSampler", map(u.slim).getId());
      }
   }

   private static DynamicTexture map(boolean slim) {
      DynamicTexture t = slim ? slimMap : wideMap;
      if (t == null) {
         byte[] limbOf = new byte[4096];
         LogiaSkinLayout.classify(limbOf, new byte[4096], slim);
         NativeImage img = new NativeImage(64, 64, false);

         for (int i = 0; i < 4096; i++) {
            img.setPixelRGBA(i % 64, i / 64, 0xFF000000 | limbOf[i] + 1 & 0xFF);
         }

         t = new DynamicTexture(img);
         Minecraft.getInstance().getTextureManager().register("devilfruits_logia_limbs_" + (slim ? "slim" : "wide"), t);
         if (slim) {
            slimMap = t;
         } else {
            wideMap = t;
         }
      }

      return t;
   }

   static void forget(int id) {
      DATA.remove(id);
   }

   static void forgetAll() {
      DATA.clear();
   }

   private LogiaBodyShader() {
   }

   static {
      for (int i = 0; i < 6; i++) {
         LIMB[i] = new Matrix4f();
      }
   }

   private abstract static class Types extends RenderType {
      private Types() {
         super("", DefaultVertexFormat.POSITION, Mode.QUADS, 0, false, false, () -> {
         }, () -> {
         });
      }

      static RenderType create(ResourceLocation skin, int id) {
         return RenderType.create(
            "devilfruits_logia_body",
            DefaultVertexFormat.NEW_ENTITY,
            Mode.QUADS,
            1536,
            true,
            true,
            CompositeState.builder()
               .setShaderState(new ShaderStateShard(() -> LogiaBodyShader.shader))
               .setTextureState(new TextureStateShard(skin, false, false))
               .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
               .setCullState(NO_CULL)
               .setLightmapState(LIGHTMAP)
               .setOverlayState(OVERLAY)
               .setTexturingState(new TexturingStateShard("devilfruits_logia_body_" + id, () -> LogiaBodyShader.upload(id), () -> {
               }))
               .createCompositeState(true)
         );
      }
   }

   static final class Uniforms {
      final float[] holeA = new float[32];
      final float[] holeB = new float[32];
      int holes;
      final float[] root = new float[24];
      final float[] tip = new float[24];
      int edge;
      boolean slim;
      ResourceLocation skin;
      RenderType type;
   }
}
