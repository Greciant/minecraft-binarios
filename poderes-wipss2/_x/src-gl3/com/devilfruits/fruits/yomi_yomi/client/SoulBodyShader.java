package com.devilfruits.fruits.yomi_yomi.client;

import com.devilfruits.DevilFruits;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

public final class SoulBodyShader {
   @Nullable
   private static ShaderInstance shader;
   private static final Map<ResourceLocation, RenderType> TYPES = new HashMap<>();

   static void onRegister(RegisterShadersEvent event) {
      try {
         event.registerShader(
            new ShaderInstance(event.getResourceProvider(), DevilFruits.id("yomi_soul_body"), DefaultVertexFormat.NEW_ENTITY), s -> shader = s
         );
      } catch (IOException var2) {
         DevilFruits.LOGGER.error("The Soul King's fire shader failed to load; it is drawn from its texture", var2);
      }
   }

   @Nullable
   static RenderType type(ResourceLocation texture) {
      return shader == null ? null : TYPES.computeIfAbsent(texture, SoulBodyShader.Shards::create);
   }

   static void frame(float ox, float oy, float oz, float turnDegrees, float time, float burn) {
      if (shader != null) {
         shader.safeGetUniform("Origin").set(ox, oy, oz);
         shader.safeGetUniform("Yaw").set(-turnDegrees * (float) (Math.PI / 180.0));
         shader.safeGetUniform("Time").set(time);
         shader.safeGetUniform("Burn").set(burn);
      }
   }

   private SoulBodyShader() {
   }

   private abstract static class Shards extends RenderType {
      private Shards() {
         super("", DefaultVertexFormat.POSITION, Mode.QUADS, 0, false, false, () -> {
         }, () -> {
         });
      }

      static RenderType create(ResourceLocation texture) {
         return RenderType.create(
            "devilfruits_yomi_soul_body",
            DefaultVertexFormat.NEW_ENTITY,
            Mode.QUADS,
            32768,
            false,
            false,
            CompositeState.builder()
               .setShaderState(new ShaderStateShard(() -> SoulBodyShader.shader))
               .setTextureState(new TextureStateShard(texture, false, false))
               .setTransparencyState(NO_TRANSPARENCY)
               .setCullState(CULL)
               .setLightmapState(LIGHTMAP)
               .setOverlayState(OVERLAY)
               .createCompositeState(false)
         );
      }
   }
}
