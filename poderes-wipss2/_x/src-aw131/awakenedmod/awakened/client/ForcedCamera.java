package com.awakenedmod.awakened.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles;

public final class ForcedCamera {
   private static boolean active;
   private static int ticksLeft;
   private static CameraType saved;
   private static boolean cameraSwapped;
   private static int orbitEntityId = -1;

   private ForcedCamera() {
   }

   public static boolean active() {
      return active;
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ForcedCamera::onClientTick);
      gameBus.addListener(ForcedCamera::onCameraAngles);
   }

   public static void force(int ticks) {
      force(ticks, -1);
   }

   public static void force(int ticks, int orbitId) {
      Minecraft mc = Minecraft.getInstance();
      if (ticks <= 0) {
         restore(mc);
      } else {
         if (!active) {
            saved = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            active = true;
         }

         if (orbitId >= 0 && !cameraSwapped && mc.level != null && mc.player != null) {
            Entity target = mc.level.getEntity(orbitId);
            if (target != null) {
               mc.setCameraEntity(target);
               cameraSwapped = true;
               orbitEntityId = orbitId;
            }
         }

         ticksLeft = ticks;
      }
   }

   private static void onClientTick(Post event) {
      if (active) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null) {
            restore(mc);
         } else {
            if (cameraSwapped) {
               Entity target = mc.level == null ? null : mc.level.getEntity(orbitEntityId);
               if (target == null || target.isRemoved()) {
                  restore(mc);
                  return;
               }
            }

            if (!mc.isPaused()) {
               if (--ticksLeft <= 0) {
                  restore(mc);
               }
            }
         }
      }
   }

   private static void onCameraAngles(ComputeCameraAngles event) {
      if (active && cameraSwapped) {
         LocalPlayer localPlayer = Minecraft.getInstance().player;
         if (localPlayer != null) {
            event.setYaw(localPlayer.getViewYRot((float)event.getPartialTick()));
            event.setPitch(localPlayer.getViewXRot((float)event.getPartialTick()));
            event.setRoll(0.0F);
         }
      }
   }

   private static void restore(Minecraft mc) {
      if (active) {
         active = false;
         ticksLeft = 0;
         if (cameraSwapped) {
            cameraSwapped = false;
            orbitEntityId = -1;
            if (mc.player != null) {
               mc.setCameraEntity(mc.player);
            }
         }

         if (saved != null) {
            mc.options.setCameraType(saved);
         }
      }
   }
}
