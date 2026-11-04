package com.devilfruits.client.vfx.render;

import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.post.VfxTargets;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

public final class VfxRenderer {
   private static final GpuMesh PARTICLES = new GpuMesh(1048576);
   private static final GpuMesh[] MESHES = new GpuMesh[MeshTex.values().length];
   private static final GpuMesh DISTORT = new GpuMesh(65536);
   private static int meshes;

   public static BufferBuilder mesh(MeshTex tex) {
      return MESHES[tex.ordinal()].begin();
   }

   public static boolean allowMesh() {
      return ++meshes <= VfxSettings.maxMeshes();
   }

   @Nullable
   public static BufferBuilder distortion() {
      return VfxSettings.distortion() && VfxShaders.distort != null ? DISTORT.begin() : null;
   }

   public static void render(VfxFrame frame, Matrix4f modelView, Matrix4f projection, Consumer<VfxFrame> cinematics) {
      meshes = 0;
      if (VfxShaders.ready()) {
         boolean post = VfxSettings.post();
         VfxParticles.write(PARTICLES.begin(), frame);
         cinematics.accept(frame);
         boolean any = PARTICLES.upload();

         for (GpuMesh m : MESHES) {
            any |= m.upload();
         }

         boolean distort = DISTORT.upload() && VfxSettings.distortion();
         if (!any && !distort) {
            VertexBuffer.unbind();
         } else {
            Minecraft mc = Minecraft.getInstance();
            RenderTarget main = mc.getMainRenderTarget();
            RenderTarget fabulous = mc.levelRenderer.getParticlesTarget();
            RenderTarget out = fabulous != null ? fabulous : main;
            VfxTargets.ensure(main);
            VfxTargets.depth().copyDepthFrom(main);
            int depthTex = VfxTargets.depth().getDepthTextureId();
            int atlas = VfxSprites.id(VfxSprites.ATLAS);
            int lut = VfxSprites.id(VfxSprites.LUT);
            int noise = VfxSprites.id(VfxSprites.NOISE);
            out.bindWrite(true);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(515);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            float minAlpha = fabulous != null ? 0.02F : 0.0F;
            draw(modelView, projection, out.width, out.height, 0.0F, post ? 1.0F : 0.0F, minAlpha, atlas, lut, noise, depthTex);
            boolean emissive = false;
            if (post && VfxSettings.bloomStrength() > 0.0F) {
               RenderTarget e = VfxTargets.emissive();
               e.clear(Minecraft.ON_OSX);
               e.bindWrite(true);
               RenderSystem.disableDepthTest();
               draw(modelView, projection, e.width, e.height, 1.0F, 1.0F, 0.0F, atlas, lut, noise, depthTex);
               emissive = true;
            }

            if (distort) {
               RenderTarget d = VfxTargets.distort();
               d.clear(Minecraft.ON_OSX);
               d.bindWrite(true);
               RenderSystem.disableDepthTest();
               RenderSystem.blendFunc(SourceFactor.ONE, DestFactor.ONE);
               ShaderInstance s = VfxShaders.distort;
               s.setSampler("AtlasSampler", atlas);
               s.setSampler("HeatSampler", VfxSprites.id(VfxSprites.HEAT_NORMAL));
               s.setSampler("DepthSampler", depthTex);
               s.safeGetUniform("TargetSize").set((float)d.width, (float)d.height);
               s.safeGetUniform("Time").set(frame.time);
               DISTORT.draw(s, modelView, projection);
            }

            VfxPost.drawn(emissive, distort);
            VertexBuffer.unbind();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            out.bindWrite(true);
         }
      }
   }

   private static void draw(
      Matrix4f modelView, Matrix4f projection, int width, int height, float pass, float soft, float minAlpha, int atlas, int lut, int noise, int depthTex
   ) {
      float emissiveScale = 1.0F;
      ShaderInstance m = VfxShaders.mesh;
      if (pass < 0.5F) {
         RenderSystem.depthMask(true);
      }

      meshes(m, modelView, projection, true, width, height, pass, soft, minAlpha, emissiveScale, lut, noise, depthTex);
      RenderSystem.depthMask(false);
      ShaderInstance p = VfxShaders.particle;
      if (PARTICLES.ready()) {
         p.setSampler("AtlasSampler", atlas);
         p.setSampler("PackSampler", VfxSprites.packId(atlas));
         p.setSampler("LutSampler", lut);
         p.setSampler("DepthSampler", depthTex);
         uniforms(p, width, height, pass, soft, minAlpha, emissiveScale);
         PARTICLES.draw(p, modelView, projection);
      }

      meshes(m, modelView, projection, false, width, height, pass, soft, minAlpha, emissiveScale, lut, noise, depthTex);
   }

   private static void meshes(
      ShaderInstance m,
      Matrix4f modelView,
      Matrix4f projection,
      boolean solid,
      int width,
      int height,
      float pass,
      float soft,
      float minAlpha,
      float emissiveScale,
      int lut,
      int noise,
      int depthTex
   ) {
      for (MeshTex tex : MeshTex.values()) {
         GpuMesh mesh = MESHES[tex.ordinal()];
         if (tex.solid == solid && mesh.ready()) {
            m.setSampler("MainSampler", VfxSprites.id(tex.texture));
            m.setSampler("NoiseSampler", noise);
            m.setSampler("LutSampler", lut);
            m.setSampler("DepthSampler", depthTex);
            uniforms(m, width, height, pass, soft, minAlpha, emissiveScale);
            m.safeGetUniform("AlphaCut").set(solid ? 0.18F : 0.002F);
            mesh.draw(m, modelView, projection);
         }
      }
   }

   private static void uniforms(ShaderInstance s, int width, int height, float pass, float soft, float minAlpha, float emissiveScale) {
      s.safeGetUniform("TargetSize").set((float)width, (float)height);
      s.safeGetUniform("Pass").set(pass);
      s.safeGetUniform("SoftScale").set(soft);
      s.safeGetUniform("MinAlpha").set(minAlpha);
      s.safeGetUniform("EmissiveScale").set(emissiveScale);
   }

   public static void discard() {
      PARTICLES.discard();

      for (GpuMesh m : MESHES) {
         m.discard();
      }

      DISTORT.discard();
   }

   private VfxRenderer() {
   }

   static {
      for (int i = 0; i < MESHES.length; i++) {
         MESHES[i] = new GpuMesh(262144);
      }
   }
}
