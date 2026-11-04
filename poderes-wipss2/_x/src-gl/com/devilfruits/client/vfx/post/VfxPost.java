package com.devilfruits.client.vfx.post;

import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.render.VfxShaders;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class VfxPost {
   private static final Matrix4f IDENTITY = new Matrix4f();
   private static final Vector4f TMP = new Vector4f();
   private static final int FLASH_INTERVAL = 7;
   private static VertexBuffer quad;
   private static float chroma;
   private static float prevChroma;
   private static float radial;
   private static float prevRadial;
   private static double radialX;
   private static double radialY;
   private static double radialZ;
   private static float flash;
   private static float prevFlash;
   private static int flashColor = 16777215;
   private static long lastFlashTick = -100L;
   private static float grade;
   private static float prevGrade;
   private static float gradeWanted;
   private static int gradeColor = 16777215;
   private static float vignette;
   private static float prevVignette;
   private static float vignetteWanted;
   private static float desaturate;
   private static float prevDesaturate;
   private static float desaturateWanted;
   private static float bloomBoost;
   private static float prevBloomBoost;
   private static float bloomBoostWanted;
   private static boolean emissiveDrawn;
   private static boolean distortDrawn;

   public static void chromatic(float amount) {
      float a = VfxSettings.reduceFlashing() ? amount * 0.5F : amount;
      chroma = Math.min(1.0F, Math.max(chroma, a));
   }

   public static void radialBlur(Vec3 at, float amount) {
      float a = VfxSettings.reduceFlashing() ? amount * 0.5F : amount;
      if (a > radial) {
         radialX = at.x;
         radialY = at.y;
         radialZ = at.z;
      }

      radial = Math.min(0.5F, Math.max(radial, a));
   }

   public static void flash(int rgb, float amount) {
      long now = VfxClock.ticks();
      float a = Math.min(amount, VfxSettings.flashCap());
      if (now - lastFlashTick < 7L) {
         flash = Math.max(flash, Math.min(a, flash + a * 0.25F));
      } else {
         lastFlashTick = now;
         flash = Math.max(flash, a);
         flashColor = rgb;
      }
   }

   public static void grade(int rgb, float amount) {
      if (amount > gradeWanted) {
         gradeWanted = amount;
         gradeColor = rgb;
      }
   }

   public static void vignette(float amount) {
      vignetteWanted = Math.max(vignetteWanted, amount);
   }

   public static void desaturate(float amount) {
      desaturateWanted = Math.max(desaturateWanted, amount);
   }

   public static void bloomBoost(float amount) {
      float a = VfxSettings.reduceFlashing() ? amount * 0.5F : amount;
      bloomBoostWanted = Math.max(bloomBoostWanted, a);
   }

   public static void tick() {
      prevChroma = chroma;
      chroma = chroma < 0.005F ? 0.0F : chroma * 0.72F;
      prevRadial = radial;
      radial = radial < 0.004F ? 0.0F : radial * 0.68F;
      prevFlash = flash;
      flash = flash < 0.005F ? 0.0F : flash * 0.7F - 0.004F;
      flash = Math.max(0.0F, flash);
      prevGrade = grade;
      grade = grade + (gradeWanted - grade) * (gradeWanted > grade ? 0.18F : 0.06F);
      if (gradeWanted == 0.0F && grade < 0.003F) {
         grade = 0.0F;
      }

      gradeWanted = 0.0F;
      prevVignette = vignette;
      vignette = vignette + (vignetteWanted - vignette) * 0.12F;
      if (vignetteWanted == 0.0F && vignette < 0.003F) {
         vignette = 0.0F;
      }

      vignetteWanted = 0.0F;
      prevDesaturate = desaturate;
      desaturate = desaturate + (desaturateWanted - desaturate) * 0.12F;
      if (desaturateWanted == 0.0F && desaturate < 0.003F) {
         desaturate = 0.0F;
      }

      desaturateWanted = 0.0F;
      prevBloomBoost = bloomBoost;
      bloomBoost = bloomBoost + (bloomBoostWanted - bloomBoost) * 0.12F;
      if (bloomBoostWanted == 0.0F && bloomBoost < 0.003F) {
         bloomBoost = 0.0F;
      }

      bloomBoostWanted = 0.0F;
   }

   public static void reset() {
      prevFlash = 0.0F;
      flash = 0.0F;
      prevRadial = 0.0F;
      radial = 0.0F;
      prevChroma = 0.0F;
      chroma = 0.0F;
      vignetteWanted = 0.0F;
      prevVignette = 0.0F;
      vignette = 0.0F;
      gradeWanted = 0.0F;
      prevGrade = 0.0F;
      grade = 0.0F;
      bloomBoostWanted = 0.0F;
      prevBloomBoost = 0.0F;
      bloomBoost = 0.0F;
      desaturateWanted = 0.0F;
      prevDesaturate = 0.0F;
      desaturate = 0.0F;
      distortDrawn = false;
      emissiveDrawn = false;
   }

   public static void drawn(boolean emissive, boolean distort) {
      emissiveDrawn = emissive;
      distortDrawn = distort;
   }

   private static boolean pulsesActive() {
      return chroma > 0.0F
         || prevChroma > 0.0F
         || radial > 0.0F
         || prevRadial > 0.0F
         || flash > 0.0F
         || prevFlash > 0.0F
         || grade > 0.0F
         || prevGrade > 0.0F
         || vignette > 0.0F
         || prevVignette > 0.0F
         || desaturate > 0.0F
         || prevDesaturate > 0.0F;
   }

   public static void apply(Matrix4f modelView, Matrix4f projection) {
      boolean emissive = emissiveDrawn;
      boolean distort = distortDrawn;
      distortDrawn = false;
      emissiveDrawn = false;
      if (VfxSettings.post() && VfxShaders.composite != null && VfxShaders.bloomDown != null && VfxShaders.bloomUp != null) {
         if (emissive || distort || pulsesActive()) {
            Minecraft mc = Minecraft.getInstance();
            RenderTarget main = mc.getMainRenderTarget();
            VfxTargets.ensure(main);
            if (quad == null) {
               BufferBuilder b = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION);
               b.addVertex(-1.0F, -1.0F, 0.0F);
               b.addVertex(1.0F, -1.0F, 0.0F);
               b.addVertex(1.0F, 1.0F, 0.0F);
               b.addVertex(-1.0F, 1.0F, 0.0F);
               quad = new VertexBuffer(Usage.STATIC);
               quad.bind();
               quad.upload(b.buildOrThrow());
            }

            float p = VfxClock.partial();
            float strength = VfxSettings.bloomStrength();
            int mips = VfxSettings.bloomMips();
            boolean bloom = emissive && strength > 0.0F && mips > 0;
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            if (bloom) {
               ShaderInstance down = VfxShaders.bloomDown;
               RenderSystem.disableBlend();
               RenderTarget src = VfxTargets.emissive();

               for (int i = 0; i < mips; i++) {
                  RenderTarget dst = VfxTargets.bloom(i);
                  dst.bindWrite(true);
                  down.setSampler("Source", src.getColorTextureId());
                  down.safeGetUniform("SrcTexel").set(1.0F / (float)src.width, 1.0F / (float)src.height);
                  down.safeGetUniform("Threshold").set(0.22F);
                  down.safeGetUniform("FirstPass").set(i == 0 ? 1.0F : 0.0F);
                  fullscreen(down);
                  src = dst;
               }

               ShaderInstance up = VfxShaders.bloomUp;
               RenderSystem.enableBlend();
               RenderSystem.blendFunc(SourceFactor.ONE, DestFactor.ONE);

               for (int i = mips - 2; i >= 0; i--) {
                  RenderTarget s = VfxTargets.bloom(i + 1);
                  VfxTargets.bloom(i).bindWrite(true);
                  up.setSampler("Source", s.getColorTextureId());
                  up.safeGetUniform("SrcTexel").set(1.0F / (float)s.width, 1.0F / (float)s.height);
                  up.safeGetUniform("Radius").set(1.0F);
                  up.safeGetUniform("Intensity").set(0.9F);
                  fullscreen(up);
               }

               RenderSystem.disableBlend();
               RenderSystem.defaultBlendFunc();
            }

            VfxTargets.copyScene(main);
            main.bindWrite(true);
            ShaderInstance c = VfxShaders.composite;
            c.setSampler("SceneSampler", VfxTargets.scene().getColorTextureId());
            c.setSampler("DistortSampler", VfxTargets.distort().getColorTextureId());
            c.setSampler("EmissiveSampler", VfxTargets.emissive().getColorTextureId());
            c.setSampler("BloomSampler", VfxTargets.bloom(0).getColorTextureId());
            boolean lens = VfxSettings.lensEffects();
            float rad = lens ? Mth.lerp(p, prevRadial, radial) : 0.0F;
            float cx = 0.5F;
            float cy = 0.5F;
            if (rad > 0.0F) {
               Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
               TMP.set((float)(radialX - cam.x), (float)(radialY - cam.y), (float)(radialZ - cam.z), 1.0F);
               modelView.transform(TMP);
               projection.transform(TMP);
               if (TMP.w > 0.05F) {
                  cx = Mth.clamp(TMP.x / TMP.w * 0.5F + 0.5F, -0.2F, 1.2F);
                  cy = Mth.clamp(TMP.y / TMP.w * 0.5F + 0.5F, -0.2F, 1.2F);
               }
            }

            c.safeGetUniform("RadialCenter").set(cx, cy);
            c.safeGetUniform("RadialStrength").set(rad);
            c.safeGetUniform("Chromatic").set(lens ? Mth.lerp(p, prevChroma, chroma) * 0.012F : 0.0F);
            c.safeGetUniform("BloomIntensity").set(bloom ? strength * 0.85F * (1.0F + Mth.lerp(p, prevBloomBoost, bloomBoost)) : 0.0F);
            c.safeGetUniform("DistortOn").set(distort && VfxSettings.distortion() ? 1.0F : 0.0F);
            float fl = Mth.lerp(p, prevFlash, flash);
            c.safeGetUniform("Flash").set(r(flashColor), g(flashColor), b(flashColor), fl);
            float gr = Mth.lerp(p, prevGrade, grade);
            c.safeGetUniform("Grade").set(r(gradeColor), g(gradeColor), b(gradeColor), gr);
            c.safeGetUniform("Vignette").set(Mth.lerp(p, prevVignette, vignette));
            c.safeGetUniform("Desaturate").set(Mth.lerp(p, prevDesaturate, desaturate));
            c.safeGetUniform("InvSize").set(1.0F / (float)main.width, 1.0F / (float)main.height);
            fullscreen(c);
            VertexBuffer.unbind();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            main.bindWrite(true);
         }
      }
   }

   private static void fullscreen(ShaderInstance shader) {
      quad.bind();
      quad.drawWithShader(IDENTITY, IDENTITY, shader);
   }

   private static float r(int rgb) {
      return (float)(rgb >> 16 & 0xFF) / 255.0F;
   }

   private static float g(int rgb) {
      return (float)(rgb >> 8 & 0xFF) / 255.0F;
   }

   private static float b(int rgb) {
      return (float)(rgb & 0xFF) / 255.0F;
   }

   private VfxPost() {
   }
}
