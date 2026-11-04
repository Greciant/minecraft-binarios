package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import com.awakenedmod.awakened.core.net.LaneTransitPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class VelocityLaneTransitClient {
   private static final int FLAG_GRACE_TICKS = 3;
   private static final int OVERRUN_TICKS = 40;
   public static final float CADENCE_TICKS = 6.0F;
   private static final float RISE_TICKS = 10.0F;
   private static final float RISE_MAX_FRACTION = 0.4F;
   private static final float EXIT_FRACTION = 0.25F;
   private static final float FLARE_TICKS = 4.0F;
   private static final float FLARE_MAX_FRACTION = 0.25F;
   private static Vec3 from = Vec3.ZERO;
   private static Vec3 to = Vec3.ZERO;
   private static int duration;
   private static int ticks;

   private VelocityLaneTransitClient() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(VelocityLaneTransitClient::onClientTick);
   }

   public static void set(LaneTransitPayload payload) {
      if (!payload.running()) {
         clear();
      } else {
         from = payload.from();
         to = payload.to();
         duration = payload.duration();
         ticks = 0;
      }
   }

   public static void clear() {
      duration = 0;
      ticks = 0;
      from = Vec3.ZERO;
      to = Vec3.ZERO;
   }

   public static boolean active() {
      return duration > 0;
   }

   public static Vec3 from() {
      return from;
   }

   public static Vec3 to() {
      return to;
   }

   public static int duration() {
      return duration;
   }

   public static float age(float partialTick) {
      return duration > 0 ? (float)ticks + partialTick : 0.0F;
   }

   public static float progress(float partialTick) {
      return duration <= 0 ? 0.0F : Mth.clamp(age(partialTick) / (float)duration, 0.0F, 1.0F);
   }

   public static double distance() {
      return duration > 0 ? from.distanceTo(to) : 0.0;
   }

   public static double remaining(float partialTick) {
      return distance() * (double)(1.0F - progress(partialTick));
   }

   public static Vec3 position(float partialTick) {
      return duration > 0 ? from.lerp(to, (double)progress(partialTick)) : Vec3.ZERO;
   }

   public static double speed() {
      return 5.0;
   }

   public static float ramp(float partialTick) {
      if (duration <= 0) {
         return 0.0F;
      } else {
         float rise = Math.min(10.0F, (float)duration * 0.4F);
         float u = 1.0F - Mth.clamp(age(partialTick) / rise, 0.0F, 1.0F);
         return 1.0F - u * u;
      }
   }

   public static float exit(float partialTick) {
      return duration <= 0 ? 0.0F : Mth.clamp((progress(partialTick) - 0.75F) / 0.25F, 0.0F, 1.0F);
   }

   public static float flare(float partialTick) {
      if (duration <= 0) {
         return 0.0F;
      } else {
         float span = Math.min(4.0F, (float)duration * 0.25F);
         return Mth.clamp((age(partialTick) - ((float)duration - span)) / span, 0.0F, 1.0F);
      }
   }

   public static float cadence(float partialTick) {
      return age(partialTick) * (float) (Math.PI * 2) / 6.0F;
   }

   private static void onClientTick(Post event) {
      if (duration > 0) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer local = mc.player;
         if (local == null || mc.level == null) {
            clear();
         } else if (!mc.isPaused()) {
            ticks++;
            boolean flagGone = ticks > 3 && !VelocityVisualState.of(local).tunnel();
            if (flagGone || ticks > duration + 40) {
               clear();
            }
         }
      }
   }
}
