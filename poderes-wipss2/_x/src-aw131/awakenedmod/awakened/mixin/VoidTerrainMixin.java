package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.voidview.VoidView;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.neoforged.neoforge.client.ClientHooks;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelRenderer.class})
public abstract class VoidTerrainMixin {
   @Shadow
   private int ticks;
   @Shadow
   @Final
   private Minecraft minecraft;

   @Shadow
   public abstract Frustum getFrustum();

   @Inject(
      method = {"renderSectionLayer"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void awakened$voidTerrain(RenderType type, double x, double y, double z, Matrix4f modelView, Matrix4f projection, CallbackInfo ci) {
      if (VoidView.hidesWorld()) {
         Camera camera = this.minecraft.gameRenderer.getMainCamera();
         ClientHooks.dispatchRenderStage(type, (LevelRenderer)this, modelView, projection, this.ticks, camera, this.getFrustum());
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderSnowAndRain"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void awakened$voidWeather(LightTexture light, float partialTick, double x, double y, double z, CallbackInfo ci) {
      if (VoidView.hidesWorld()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"tickRain"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void awakened$voidRainTick(Camera camera, CallbackInfo ci) {
      if (VoidView.hidesWorld()) {
         ci.cancel();
      }
   }
}
