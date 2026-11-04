package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.earth.MechMotion;
import com.awakenedmod.awakened.content.earth.StoneMechEntity;
import com.awakenedmod.awakened.core.net.StoneMechActionPayload;
import com.awakenedmod.awakened.core.net.StoneMechInputPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Pre;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.neoforged.neoforge.network.PacketDistributor;

public final class StoneMechPilot {
   private static boolean active;
   private static int targetId = -1;
   private static boolean sneakHeld;
   private static int localPoundCd;

   private StoneMechPilot() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(StoneMechPilot::onComputeCameraAngles);
      ClickClaims.add(StoneMechPilot::isActive, () -> send(new StoneMechActionPayload(0)));
      gameBus.addListener(StoneMechPilot::onUse);
      gameBus.addListener(StoneMechPilot::onPreClientTick);
      gameBus.addListener(StoneMechPilot::onRenderHand);
   }

   public static boolean isActive() {
      return active;
   }

   public static void start(int entityId) {
      Minecraft mc = Minecraft.getInstance();
      if (entityId < 0) {
         stop();
      } else if (mc.level != null && mc.player != null && mc.level.getEntity(entityId) instanceof StoneMechEntity mech) {
         if (!active) {
            sneakHeld = mc.options.keyShift.isDown();
            active = true;
            MeldHidden.set(mc.player.getId(), true);
         }

         targetId = entityId;
         mc.setCameraEntity(mech);
      }
   }

   public static void stop() {
      if (active) {
         Minecraft mc = Minecraft.getInstance();
         active = false;
         targetId = -1;
         localPoundCd = 0;
         if (mc.player != null) {
            MeldHidden.set(mc.player.getId(), false);
            mc.setCameraEntity(mc.player);
         }
      }
   }

   public static boolean handleInput() {
      if (!active) {
         return false;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (localPoundCd > 0) {
            localPoundCd--;
         }

         if (mc.player != null && mc.screen == null) {
            boolean sneak = mc.options.keyShift.isDown();
            if (sneak && !sneakHeld) {
               send(new StoneMechActionPayload(2));
               mc.player.stopRiding();
               stop();
               return true;
            } else {
               sneakHeld = sneak;
               float yaw = mc.player.getYRot();
               float pitch = mc.player.getXRot();
               float forward = sign(mc.options.keyUp.isDown()) - sign(mc.options.keyDown.isDown());
               float strafe = sign(mc.options.keyLeft.isDown()) - sign(mc.options.keyRight.isDown());
               boolean jump = mc.options.keyJump.isDown();
               boolean sprint = mc.options.keySprint.isDown();
               send(new StoneMechInputPayload(yaw, pitch, forward, strafe, jump, sprint));
               if (mc.level != null && mc.level.getEntity(targetId) instanceof StoneMechEntity mech && !mech.isAssembling()) {
                  boolean leap = jump && mech.onGround() && localPoundCd <= 0;
                  if (leap) {
                     localPoundCd = 50;
                  }

                  MechMotion.step(mech, yaw, pitch, forward, strafe, jump, sprint, leap);
               }

               return true;
            }
         } else {
            return true;
         }
      }
   }

   private static float sign(boolean down) {
      return down ? 1.0F : 0.0F;
   }

   private static void send(CustomPacketPayload payload) {
      PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
   }

   public static void clientTick() {
      if (active) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null || !mc.player.isAlive() || mc.level == null || !(mc.level.getEntity(targetId) instanceof StoneMechEntity m) || !m.isAlive()) {
            stop();
         }
      }
   }

   private static void onPreClientTick(Pre event) {
      if (active) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null && mc.level.getEntity(targetId) instanceof StoneMechEntity mech) {
            mech.lerpTo(mech.getX(), mech.getY(), mech.getZ(), mech.getYRot(), mech.getXRot(), 0);
         }
      }
   }

   private static void onRenderHand(RenderHandEvent event) {
      if (active) {
         event.setCanceled(true);
      }
   }

   private static void onUse(InteractionKeyMappingTriggered event) {
      if (active) {
         if (event.isUseItem()) {
            event.setCanceled(true);
            send(new StoneMechActionPayload(1));
         }
      }
   }

   private static void onComputeCameraAngles(ComputeCameraAngles event) {
      if (active) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            event.setYaw(player.getViewYRot((float)event.getPartialTick()));
            event.setPitch(player.getViewXRot((float)event.getPartialTick()));
         }
      }
   }
}
