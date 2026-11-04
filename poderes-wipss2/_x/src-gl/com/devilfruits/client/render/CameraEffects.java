package com.devilfruits.client.render;

import com.devilfruits.core.config.CoreClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeFov;

@EventBusSubscriber(
   modid = "devilfruits",
   value = {Dist.CLIENT}
)
public final class CameraEffects {
   private static float trauma;
   private static float sustainedTrauma;
   private static int sustainTicks;
   private static int rollTicks;
   private static int rollDuration;
   private static float rollStrength;
   private static float fovPunch;
   private static float prevFovPunch;
   private static float flash;
   private static float prevFlash;
   private static int flashColor = 16777215;

   public static void shake(float amount) {
      trauma = Math.min(1.0F, trauma + amount);
   }

   public static void shakeAt(Vec3 pos, float strength, double radius) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         double dist = mc.player.position().distanceTo(pos);
         if (dist < radius) {
            shake(strength * (float)(1.0 - dist / radius));
         }
      }
   }

   public static void sustain(float level, int ticks) {
      sustainedTrauma = Math.max(sustainedTrauma, level);
      sustainTicks = Math.max(sustainTicks, ticks);
   }

   public static void roll(int duration, float strength) {
      if ((Boolean)CoreClientConfig.CAMERA_ROLL.get()) {
         rollDuration = Math.max(1, duration);
         rollTicks = 0;
         rollStrength = Mth.clamp(strength, 0.0F, 1.0F);
      }
   }

   public static void fovPunch(float amount) {
      if ((Boolean)CoreClientConfig.FOV_PUNCH.get()) {
         fovPunch = Math.min(0.25F, fovPunch + amount);
      }
   }

   public static void flash(float amount) {
      flash(amount, 16777215);
   }

   public static void flash(float amount, int rgb) {
      if ((Boolean)CoreClientConfig.SCREEN_FLASH.get()) {
         flash = Math.min(1.0F, flash + amount);
         flashColor = rgb;
      }
   }

   public static int flashColor() {
      return flashColor;
   }

   public static float flashAlpha(float partialTick) {
      return Mth.lerp(partialTick, prevFlash, flash);
   }

   public static void tick() {
      trauma = Math.max(0.0F, trauma - 0.045F);
      if (sustainTicks > 0) {
         sustainTicks--;
         trauma = Math.max(trauma, sustainedTrauma);
         if (sustainTicks == 0) {
            sustainedTrauma = 0.0F;
         }
      }

      if (rollDuration > 0 && ++rollTicks > rollDuration) {
         rollDuration = 0;
      }

      prevFovPunch = fovPunch;
      fovPunch *= 0.72F;
      if (fovPunch < 0.001F) {
         fovPunch = 0.0F;
      }

      prevFlash = flash;
      flash = Math.max(0.0F, flash * 0.8F - 0.01F);
   }

   public static void reset() {
      sustainedTrauma = 0.0F;
      trauma = 0.0F;
      sustainTicks = 0;
      rollDuration = 0;
      prevFovPunch = 0.0F;
      fovPunch = 0.0F;
      prevFlash = 0.0F;
      flash = 0.0F;
   }

   @SubscribeEvent
   public static void onCameraAngles(ComputeCameraAngles event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && !mc.isPaused()) {
         float scale = ((Double)CoreClientConfig.CAMERA_SHAKE.get()).floatValue();
         float t = (float)mc.level.getGameTime() + (float)event.getPartialTick();
         if (trauma > 0.0F && scale > 0.0F) {
            float s = trauma * trauma * scale;
            event.setYaw(event.getYaw() + s * 4.0F * noise(t * 1.9F, 0.3F));
            event.setPitch(event.getPitch() + s * 3.2F * noise(t * 2.3F, 7.1F));
            event.setRoll(event.getRoll() + s * 3.0F * noise(t * 1.6F, 13.7F));
         }

         if (rollDuration > 0) {
            float p = ((float)rollTicks + (float)event.getPartialTick()) / (float)rollDuration;
            float envelope = Mth.sin(Mth.clamp(p, 0.0F, 1.0F) * (float) Math.PI);
            float sway = Mth.sin(p * (float) Math.PI * 3.0F) * 0.65F + 0.35F;
            event.setRoll(event.getRoll() + 14.0F * rollStrength * envelope * sway);
         }
      }
   }

   @SubscribeEvent
   public static void onFov(ComputeFov event) {
      if (fovPunch > 0.0F || prevFovPunch > 0.0F) {
         float punch = Mth.lerp((float)event.getPartialTick(), prevFovPunch, fovPunch);
         event.setFOV(event.getFOV() * (1.0 + (double)punch));
      }
   }

   private static float noise(float t, float seed) {
      return Mth.sin(t * 2.17F + seed) * 0.5F + Mth.sin(t * 3.71F + seed * 1.9F) * 0.3F + Mth.sin(t * 7.13F + seed * 0.7F) * 0.2F;
   }

   private CameraEffects() {
   }
}
