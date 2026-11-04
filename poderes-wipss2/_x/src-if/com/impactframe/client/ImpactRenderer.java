package com.impactframe.client;

import com.impactframe.ImpactConfig;
import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public final class ImpactRenderer {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation CHAIN_ID = ResourceLocation.fromNamespaceAndPath("impactframe", "shaders/post/impact_frame.json");
   private static final ResourceLocation LITE_CHAIN_ID = ResourceLocation.fromNamespaceAndPath("impactframe", "shaders/post/impact_frame_lite.json");
   private static PostChain chain;
   private static PostPass impactPass;
   private static Field passesField;
   private static boolean loadFailed;
   private static boolean builtForDepth = true;
   private static int width = -1;
   private static int height = -1;

   private ImpactRenderer() {
   }

   public static void render(
      float strength,
      float time,
      float focusU,
      float focusV,
      float focusRadius,
      int tier,
      boolean invert,
      float near,
      float far,
      float partialTick,
      float dirX,
      float dirY,
      float[] tint,
      float letterbox
   ) {
      Minecraft mc = Minecraft.getInstance();
      if (chain != null && builtForDepth != ImpactConfig.useDepth()) {
         invalidate();
      }

      if (ensureChain(mc)) {
         int w = mc.getWindow().getWidth();
         int h = mc.getWindow().getHeight();
         if (w > 0 && h > 0) {
            if (w != width || h != height) {
               chain.resize(w, h);
               width = w;
               height = h;
            }

            EffectInstance fx = impactPass.getEffect();
            setFloat(fx, "Strength", strength);
            setFloat(fx, "Time", time);
            setVec2(fx, "Center", focusU, focusV);
            setFloat(fx, "FocusRadius", focusRadius);
            setFloat(fx, "SafeMode", ImpactConfig.safeMode() ? 1.0F : 0.0F);
            setFloat(fx, "LinearIntensity", ImpactConfig.preset() == ImpactPreset.ADVANCED ? 1.0F : 0.0F);
            setFloat(fx, "Invert", invert ? 1.0F : 0.0F);
            setFloat(fx, "Tier", (float)tier);
            setVec2(fx, "DepthRange", near, far);
            boolean directional = ImpactConfig.directionalStreaks() && (dirX != 0.0F || dirY != 0.0F);
            setVec2(fx, "Direction", dirX, dirY);
            setFloat(fx, "DirectionAmount", directional ? 1.0F : 0.0F);
            setFloat(fx, "Halftone", ImpactConfig.halftone());
            AbstractUniform tintUniform = fx.safeGetUniform("Tint");
            if (tintUniform != null) {
               tintUniform.set(tint != null ? tint[0] : 1.0F, tint != null ? tint[1] : 1.0F, tint != null ? tint[2] : 1.0F);
            }

            setFloat(fx, "TintAmount", tint != null ? 1.0F : 0.0F);
            setFloat(fx, "Letterbox", letterbox);

            try {
               chain.process(partialTick);
            } catch (RuntimeException var21) {
               LOGGER.error("Impact Frame: the post chain failed, turning it off until the next resource reload", var21);
               invalidate();
               loadFailed = true;
            }

            mc.getMainRenderTarget().bindWrite(true);
         }
      }
   }

   public static void invalidate() {
      if (chain != null) {
         chain.close();
      }

      chain = null;
      impactPass = null;
      loadFailed = false;
      width = -1;
      height = -1;
   }

   private static boolean ensureChain(Minecraft mc) {
      if (chain != null) {
         return true;
      } else if (loadFailed) {
         return false;
      } else {
         builtForDepth = ImpactConfig.useDepth();
         if (builtForDepth && build(mc, CHAIN_ID)) {
            return true;
         } else {
            LOGGER.warn("Impact Frame: falling back to the lite chain without depth outlines");
            if (build(mc, LITE_CHAIN_ID)) {
               return true;
            } else {
               loadFailed = true;
               return false;
            }
         }
      }
   }

   private static boolean build(Minecraft mc, ResourceLocation id) {
      PostChain created = null;

      try {
         created = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), id);
         PostPass pass = firstPass(created);
         if (pass == null) {
            created.close();
            LOGGER.error("Impact Frame: the post chain {} has no passes", id);
            return false;
         } else {
            chain = created;
            impactPass = pass;
            width = -1;
            height = -1;
            return true;
         }
      } catch (Exception var4) {
         if (created != null) {
            created.close();
         }

         LOGGER.error("Impact Frame: could not load {}", id, var4);
         return false;
      }
   }

   private static PostPass firstPass(PostChain postChain) throws ReflectiveOperationException {
      if (passesField == null) {
         passesField = PostChain.class.getDeclaredField("passes");
         passesField.setAccessible(true);
      }

      List<PostPass> passes = (List<PostPass>)passesField.get(postChain);
      return passes.isEmpty() ? null : passes.get(0);
   }

   private static void setFloat(EffectInstance fx, String name, float value) {
      AbstractUniform uniform = fx.safeGetUniform(name);
      if (uniform != null) {
         uniform.set(value);
      }
   }

   private static void setVec2(EffectInstance fx, String name, float x, float y) {
      AbstractUniform uniform = fx.safeGetUniform(name);
      if (uniform != null) {
         uniform.set(x, y);
      }
   }
}
