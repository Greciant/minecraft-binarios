package com.devilfruits.client.vfx.render;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.logia.LogiaBodyShader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

public final class VfxShaders {
   @Nullable
   static ShaderInstance particle;
   @Nullable
   static ShaderInstance mesh;
   @Nullable
   static ShaderInstance distort;
   @Nullable
   public static ShaderInstance bloomDown;
   @Nullable
   public static ShaderInstance bloomUp;
   @Nullable
   public static ShaderInstance composite;
   @Nullable
   static ShaderInstance dissolve;

   public static void onRegister(RegisterShadersEvent event) {
      try {
         event.registerShader(new ShaderInstance(event.getResourceProvider(), DevilFruits.id("vfx_particle"), VfxFormat.FORMAT), s -> particle = s);
         event.registerShader(new ShaderInstance(event.getResourceProvider(), DevilFruits.id("vfx_mesh"), VfxFormat.FORMAT), s -> mesh = s);
         event.registerShader(new ShaderInstance(event.getResourceProvider(), DevilFruits.id("vfx_distort"), VfxFormat.FORMAT), s -> distort = s);
         event.registerShader(
            new ShaderInstance(event.getResourceProvider(), DevilFruits.id("vfx_bloom_down"), DefaultVertexFormat.POSITION), s -> bloomDown = s
         );
         event.registerShader(new ShaderInstance(event.getResourceProvider(), DevilFruits.id("vfx_bloom_up"), DefaultVertexFormat.POSITION), s -> bloomUp = s);
         event.registerShader(
            new ShaderInstance(event.getResourceProvider(), DevilFruits.id("vfx_composite"), DefaultVertexFormat.POSITION), s -> composite = s
         );
         event.registerShader(
            new ShaderInstance(event.getResourceProvider(), DevilFruits.id("vfx_dissolve"), DefaultVertexFormat.NEW_ENTITY), s -> dissolve = s
         );
         event.registerShader(
            new ShaderInstance(event.getResourceProvider(), DevilFruits.id("logia_body"), DefaultVertexFormat.NEW_ENTITY), s -> LogiaBodyShader.shader = s
         );
      } catch (IOException var2) {
         DevilFruits.LOGGER.error("Cinematic VFX shaders failed to load; the layer falls back to its plain tier", var2);
      }
   }

   public static boolean ready() {
      return particle != null && mesh != null;
   }

   private VfxShaders() {
   }
}
