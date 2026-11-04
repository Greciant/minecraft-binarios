package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.summoning.PossessMotion;
import com.awakenedmod.awakened.core.BodyPinState;
import com.awakenedmod.awakened.core.net.PossessActionPayload;
import com.awakenedmod.awakened.core.net.PossessInputPayload;
import com.awakenedmod.awakened.core.net.PossessTeleportPayload;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Pre;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PossessCamera {
   private static boolean active;
   private static int targetId = -1;
   private static CameraType prevType;
   private static boolean sneakHeld;
   private static boolean screenWasOpen;
   private static int attackRepeat;
   private static final int ATTACK_REPEAT_TICKS = 2;
   private static float aimYaw;
   private static float aimPitch;

   private PossessCamera() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(PossessCamera::onComputeCameraAngles);
      ClickClaims.add(() -> active, PossessCamera::requestAttack);
      gameBus.addListener(PossessCamera::onInteract);
      gameBus.addListener(PossessCamera::onPreClientTick);
      gameBus.addListener(PossessCamera::onRenderHand);
      gameBus.addListener(PossessCamera::onLoggingOut);
   }

   private static void onLoggingOut(LoggingOut event) {
      stop();
   }

   public static boolean isActive() {
      return active;
   }

   public static Mob piloted() {
      Minecraft mc = Minecraft.getInstance();
      return active && mc.level != null && mc.level.getEntity(targetId) instanceof Mob mob ? mob : null;
   }

   public static void start(int entityId) {
      Minecraft mc = Minecraft.getInstance();
      if (entityId < 0) {
         stop();
      } else if (mc.level != null && mc.player != null && mc.level.getEntity(entityId) instanceof Mob mob) {
         if (!active) {
            prevType = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.player.input = new Input();
            sneakHeld = mc.options.keyShift.isDown();
            attackRepeat = 2;
            screenWasOpen = false;
            if (mc.gameMode != null && mc.gameMode.isDestroying()) {
               mc.gameMode.stopDestroyBlock();
            }

            aimYaw = mc.player.getYRot();
            aimPitch = mc.player.getXRot();
            mc.player.setDeltaMovement(Vec3.ZERO);
            active = true;
            BodyPinState.setClient(mc.player.getId(), true);
            mc.player.displayClientMessage(Component.translatable("awakened.possess.controls"), true);
         }

         if (targetId != entityId && targetId >= 0) {
            PossessMotion.clearVerticalClient(targetId);
         }

         targetId = entityId;
         mc.setCameraEntity(mob);
      }
   }

   public static void applyLook(double yRotDelta, double xRotDelta) {
      aimYaw += (float)yRotDelta * 0.15F;
      aimPitch = Mth.clamp(aimPitch + (float)xRotDelta * 0.15F, -90.0F, 90.0F);
   }

   public static void stop() {
      if (active) {
         Minecraft mc = Minecraft.getInstance();
         active = false;
         attackRepeat = 0;
         screenWasOpen = false;
         PossessMotion.clearVerticalClient(targetId);
         targetId = -1;
         BodyPinState.clearClient();
         if (mc.player != null) {
            mc.setCameraEntity(mc.player);
            mc.player.input = new KeyboardInput(mc.options);
         }

         if (prevType != null) {
            mc.options.setCameraType(prevType);
         }
      }
   }

   public static boolean handleInput() {
      if (!active) {
         return false;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null) {
            return true;
         } else if (mc.screen != null) {
            send(new PossessInputPayload(aimYaw, aimPitch, 0.0F, 0.0F, false, false));
            screenWasOpen = true;
            return true;
         } else {
            boolean sneak = mc.options.keyShift.isDown();
            if (screenWasOpen) {
               screenWasOpen = false;
               sneakHeld = sneak;
            }

            if (sneak && !sneakHeld) {
               send(new PossessActionPayload(1));
               stop();
               return true;
            } else {
               sneakHeld = sneak;
               pollAttack(mc);
               boolean teleport = false;

               while (AwakenedKeybinds.ACTIVATE.consumeClick()) {
                  teleport = true;
               }

               boolean cycle = false;

               while (AwakenedKeybinds.CYCLE.consumeClick()) {
                  cycle = true;
               }

               while (AwakenedKeybinds.CYCLE_BACK.consumeClick()) {
                  cycle = true;
               }

               if (teleport) {
                  send(new PossessTeleportPayload(targetId));
               } else if (cycle) {
                  send(new PossessActionPayload(2));
               }

               float yaw = aimYaw;
               float pitch = aimPitch;
               float forward = sign(mc.options.keyUp.isDown()) - sign(mc.options.keyDown.isDown());
               float strafe = sign(mc.options.keyLeft.isDown()) - sign(mc.options.keyRight.isDown());
               boolean jump = mc.options.keyJump.isDown();
               boolean sprint = mc.options.keySprint.isDown();
               send(new PossessInputPayload(yaw, pitch, forward, strafe, jump, sprint));
               if (mc.level.getEntity(targetId) instanceof Mob mob) {
                  PossessMotion.step(mob, yaw, pitch, forward, strafe, jump, sprint);
               }

               return true;
            }
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
         if (mc.player == null || !mc.player.isAlive() || mc.level == null || !(mc.level.getEntity(targetId) instanceof Mob m) || !m.isAlive()) {
            if (mc.player != null && mc.level != null) {
               send(new PossessActionPayload(1));
            }

            stop();
         }
      }
   }

   private static void onPreClientTick(Pre event) {
      if (active) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null && mc.level.getEntity(targetId) instanceof Mob mob) {
            mob.lerpTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), mob.getXRot(), 0);
         }
      }
   }

   private static void onRenderHand(RenderHandEvent event) {
      if (active) {
         event.setCanceled(true);
      }
   }

   private static void onInteract(InteractionKeyMappingTriggered event) {
      if (active) {
         if (event.isPickBlock()) {
            event.setCanceled(true);
         } else if (event.isUseItem()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            requestAttack();
         }
      }
   }

   private static void pollAttack(Minecraft mc) {
      if (attackRepeat > 0) {
         attackRepeat--;
      }

      if (mc.options.keyAttack.isDown() || mc.options.keyUse.isDown()) {
         requestAttack();
      }
   }

   private static void requestAttack() {
      if (attackRepeat <= 0) {
         send(new PossessActionPayload(0));
         attackRepeat = 2;
      }
   }

   private static void onComputeCameraAngles(ComputeCameraAngles event) {
      if (active) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            event.setYaw(aimYaw);
            event.setPitch(aimPitch);
         }
      }
   }
}
