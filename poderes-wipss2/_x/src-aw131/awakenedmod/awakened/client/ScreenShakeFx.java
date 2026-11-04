package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.net.ScreenShakePayload;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles;

public final class ScreenShakeFx {
   private static final List<ScreenShakeFx.Shake> SHAKES = new ArrayList<>();

   private ScreenShakeFx() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ScreenShakeFx::onClientTick);
      gameBus.addListener(ScreenShakeFx::onCameraAngles);
   }

   public static void add(ScreenShakePayload payload) {
      SHAKES.add(
         new ScreenShakeFx.Shake(new Vec3(payload.x(), payload.y(), payload.z()), payload.radius(), payload.magnitude(), payload.duration(), payload.fade())
      );
   }

   public static void local(float magnitude, int duration, int fade) {
      if (!(magnitude <= 0.0F)) {
         SHAKES.add(new ScreenShakeFx.Shake(null, 0.0F, magnitude, duration, fade));
      }
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         SHAKES.clear();
      } else if (!mc.isPaused() && !SHAKES.isEmpty()) {
         Iterator<ScreenShakeFx.Shake> it = SHAKES.iterator();

         while (it.hasNext()) {
            ScreenShakeFx.Shake s = it.next();
            if (++s.age > s.duration + s.fade) {
               it.remove();
            }
         }
      }
   }

   private static void onCameraAngles(ComputeCameraAngles event) {
      if (!SHAKES.isEmpty() && (Boolean)ClientConfig.SCREEN_SHAKE.get()) {
         Player player = Minecraft.getInstance().player;
         if (player != null) {
            float partial = (float)event.getPartialTick();
            float t = (float)player.tickCount + partial;
            float amp = 0.0F;
            float rollAmp = 0.0F;

            for (ScreenShakeFx.Shake s : SHAKES) {
               float a = (float)s.age + partial;
               float base = a < (float)s.duration ? s.magnitude : sq(1.0F - (a - (float)s.duration) / ((float)s.fade + 1.0F)) * s.magnitude;
               if (s.pos == null) {
                  amp += base;
               } else {
                  float dist = (float)s.pos.distanceTo(player.getEyePosition());
                  float here = base * sq(1.0F - Mth.clamp(dist / s.radius, 0.0F, 1.0F));
                  amp += here;
                  rollAmp += here;
               }
            }

            float intensity = ((Double)ClientConfig.SCREEN_SHAKE_INTENSITY.get()).floatValue()
               * ((Double)Minecraft.getInstance().options.screenEffectScale().get()).floatValue();
            rollAmp = Math.min(rollAmp, 1.0F) * intensity;
            amp = Math.min(amp, 1.0F) * intensity;
            if (!(amp <= 1.0E-4F)) {
               event.setPitch(event.getPitch() + amp * Mth.cos(t * 3.0F + 2.0F) * 25.0F);
               event.setYaw(event.getYaw() + amp * Mth.cos(t * 5.0F + 1.0F) * 25.0F);
               event.setRoll(event.getRoll() + rollAmp * Mth.cos(t * 4.0F) * 25.0F);
            }
         }
      }
   }

   private static float sq(float f) {
      return f * f;
   }

   private static final class Shake {
      final Vec3 pos;
      final float radius;
      final float magnitude;
      final int duration;
      final int fade;
      int age;

      Shake(Vec3 pos, float radius, float magnitude, int duration, int fade) {
         this.pos = pos;
         this.radius = radius;
         this.magnitude = magnitude;
         this.duration = duration;
         this.fade = fade;
      }
   }
}
