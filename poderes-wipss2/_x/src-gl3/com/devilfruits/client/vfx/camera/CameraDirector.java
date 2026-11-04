package com.devilfruits.client.vfx.camera;

import com.devilfruits.core.config.CoreClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
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
public final class CameraDirector {
   private static final int SLOTS = 8;
   private static final float[] TRAUMA = new float[8];
   private static final float[] DECAY = new float[8];
   private static final float[] AMP = new float[8];
   private static final float[] FREQ = new float[8];
   private static final float[] SEED = new float[8];
   private static final int[] TAG = new int[8];
   private static float punch;
   private static float prevPunch;
   private static float dolly = 1.0F;
   private static float prevDolly = 1.0F;
   private static float dollyWanted = 1.0F;
   private static float dollyRate = 0.15F;
   private static float clock;

   public static void shake(float trauma, float decayPerTick, float maxDegrees, float frequency) {
      shake(0, trauma, decayPerTick, maxDegrees, frequency);
   }

   public static void shake(int tag, float trauma, float decayPerTick, float maxDegrees, float frequency) {
      float scale = ((Double)CoreClientConfig.CAMERA_SHAKE.get()).floatValue();
      if (!(scale <= 0.0F)) {
         int slot = -1;
         float weakest = Float.MAX_VALUE;

         for (int i = 0; i < 8; i++) {
            if (tag != 0 && TAG[i] == tag && TRAUMA[i] > 0.0F) {
               slot = i;
               break;
            }

            float w = TRAUMA[i] * AMP[i];
            if (w < weakest) {
               weakest = w;
               slot = i;
            }
         }

         TRAUMA[slot] = Math.min(1.0F, tag != 0 ? Math.max(TRAUMA[slot], trauma) : trauma);
         DECAY[slot] = decayPerTick;
         AMP[slot] = maxDegrees * scale;
         FREQ[slot] = frequency;
         SEED[slot] = (float)slot * 7.31F + clock * 0.37F;
         TAG[slot] = tag;
      }
   }

   public static float exposure(Entity caster, Vec3 at, double near, double far) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return 0.0F;
      } else if (caster == mc.player) {
         return 1.0F;
      } else {
         double d = mc.gameRenderer.getMainCamera().getPosition().distanceTo(at);
         return d <= near ? 1.0F : (d >= far ? 0.0F : (float)(1.0 - (d - near) / (far - near)));
      }
   }

   public static void fovPunch(float amount) {
      if ((Boolean)CoreClientConfig.FOV_PUNCH.get()) {
         punch = Mth.clamp(punch + amount, -0.2F, 0.25F);
      }
   }

   public static void dolly(float factor, float rate) {
      if (Math.abs(factor - 1.0F) > Math.abs(dollyWanted - 1.0F)) {
         dollyWanted = factor;
         dollyRate = rate;
      }
   }

   public static void snapDolly() {
      prevDolly = 1.0F;
      dolly = 1.0F;
      dollyWanted = 1.0F;
   }

   public static void tick() {
      clock++;

      for (int i = 0; i < 8; i++) {
         if (TRAUMA[i] > 0.0F) {
            TRAUMA[i] = Math.max(0.0F, TRAUMA[i] - DECAY[i]);
            if (TRAUMA[i] == 0.0F) {
               TAG[i] = 0;
            }
         }
      }

      prevPunch = punch;
      punch *= 0.7F;
      if (Math.abs(punch) < 0.001F) {
         punch = 0.0F;
      }

      prevDolly = dolly;
      dolly = dolly + (dollyWanted - dolly) * dollyRate;
      if (Math.abs(dolly - 1.0F) < 0.001F && dollyWanted == 1.0F) {
         dolly = 1.0F;
      }

      dollyWanted = 1.0F;
      dollyRate = 0.2F;
   }

   public static void reset() {
      for (int i = 0; i < 8; i++) {
         TRAUMA[i] = 0.0F;
         TAG[i] = 0;
      }

      prevPunch = 0.0F;
      punch = 0.0F;
      dollyWanted = 1.0F;
      prevDolly = 1.0F;
      dolly = 1.0F;
   }

   @SubscribeEvent
   public static void onAngles(ComputeCameraAngles event) {
      float t = clock + (float)event.getPartialTick();
      float yaw = 0.0F;
      float pitch = 0.0F;
      float roll = 0.0F;

      for (int i = 0; i < 8; i++) {
         if (!(TRAUMA[i] <= 0.0F)) {
            float s = TRAUMA[i] * TRAUMA[i] * AMP[i];
            float f = FREQ[i];
            yaw += s * noise(t * f * 0.9F, SEED[i]);
            pitch += s * 0.8F * noise(t * f * 1.1F, SEED[i] + 5.3F);
            roll += s * 0.6F * noise(t * f * 0.8F, SEED[i] + 11.7F);
         }
      }

      if (yaw != 0.0F || pitch != 0.0F || roll != 0.0F) {
         event.setYaw(event.getYaw() + Mth.clamp(yaw, -6.0F, 6.0F));
         event.setPitch(event.getPitch() + Mth.clamp(pitch, -5.0F, 5.0F));
         event.setRoll(event.getRoll() + Mth.clamp(roll, -4.0F, 4.0F));
      }
   }

   @SubscribeEvent
   public static void onFov(ComputeFov event) {
      float p = (float)event.getPartialTick();
      float k = Mth.lerp(p, prevDolly, dolly) * (1.0F + Mth.lerp(p, prevPunch, punch));
      if (k != 1.0F) {
         event.setFOV(event.getFOV() * (double)k);
      }
   }

   private static float noise(float t, float seed) {
      return Mth.sin(t * 2.17F + seed) * 0.5F + Mth.sin(t * 3.71F + seed * 1.9F) * 0.3F + Mth.sin(t * 7.13F + seed * 0.7F) * 0.2F;
   }

   private CameraDirector() {
   }
}
