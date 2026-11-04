package com.awakenedmod.awakened.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles;

public final class SpiderCamera {
   private static final float MAX_BANK = 11.0F;
   private static final float BANK_PER_SPEED = 16.0F;
   private static final float BANK_EASE = 0.3F;
   private static final float FLOAT_RISE = 0.7F;
   private static final float FLOAT_DECAY = 0.85F;
   private static final int ZIP_DIP_TICKS = 4;
   private static final float ZIP_DIP_PITCH = 6.0F;
   private static final int FLIP_TICKS = 8;
   private static final int HITSTOP_TICKS = 2;
   private static final float FLIP_MAX_PITCH = 65.0F;
   private static float bank;
   private static float prevBank;
   private static float floatLift;
   private static float prevFloatLift;
   private static boolean wasSwinging;
   private static int flipTicks;
   private static int hitstopTicks;
   private static float flipIntensity;
   private static float flipPitch;
   private static float prevFlipPitch;
   private static int zipDipTicks;
   private static float zipDipPitch;
   private static float prevZipDipPitch;

   private SpiderCamera() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(SpiderCamera::onClientTick);
      gameBus.addListener(SpiderCamera::onCameraAngles);
   }

   public static void triggerStrikeFlip(float intensity) {
      flipIntensity = Mth.clamp(intensity, 0.0F, 1.0F);
      flipTicks = 8;
      hitstopTicks = 2;
      prevFlipPitch = 0.0F;
      flipPitch = 0.0F;
   }

   public static boolean inHitstop() {
      return hitstopTicks > 0;
   }

   public static void triggerZipDip() {
      zipDipTicks = 4;
      prevZipDipPitch = 0.0F;
      zipDipPitch = 0.0F;
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      prevBank = bank;
      prevFloatLift = floatLift;
      prevFlipPitch = flipPitch;
      prevZipDipPitch = zipDipPitch;
      boolean spider = mc.player != null && "spider".equals(ClientPowerState.get().powerId());
      boolean swinging = spider && SpiderClientState.swinging();
      boolean hanging = swinging && SpiderClientState.hanging();
      if (mc.player == null) {
         floatLift = 0.0F;
         bank = 0.0F;
         zipDipTicks = 0;
         zipDipPitch = 0.0F;
         wasSwinging = false;
      } else if (!mc.isPaused()) {
         if (wasSwinging && !swinging) {
            floatLift = 0.7F;
         }

         wasSwinging = swinging;
         float bankTarget = 0.0F;
         if (swinging && !hanging) {
            LocalPlayer p = mc.player;
            Vec3 v = p.getDeltaMovement();
            Vec3 look = p.getLookAngle();
            Vec3 right = new Vec3(look.x, 0.0, look.z).normalize().cross(new Vec3(0.0, 1.0, 0.0));
            double lateral = v.x * right.x + v.z * right.z;
            bankTarget = (float)Mth.clamp(lateral * 16.0, -11.0, 11.0);
         }

         bank = bank + (bankTarget - bank) * 0.3F;
         floatLift *= 0.85F;
         if (floatLift < 0.02F) {
            floatLift = 0.0F;
         }

         advanceStrikeFlip();
         advanceZipDip();
      }
   }

   private static void advanceZipDip() {
      if (zipDipTicks > 0) {
         zipDipTicks--;
         float p = 1.0F - (float)zipDipTicks / 4.0F;
         zipDipPitch = 6.0F * (float)Math.sin(Math.PI * (double)p);
      } else {
         zipDipPitch = 0.0F;
      }
   }

   private static void advanceStrikeFlip() {
      if (hitstopTicks > 0) {
         hitstopTicks--;
         flipPitch = 0.0F;
      } else {
         if (flipTicks > 0) {
            flipTicks--;
            float p = 1.0F - (float)flipTicks / 8.0F;
            float bump = (float)Math.sin(Math.PI * (double)p);
            flipPitch = 65.0F * (0.45F + 0.55F * flipIntensity) * bump;
         } else {
            flipPitch = 0.0F;
         }
      }
   }

   private static void onCameraAngles(ComputeCameraAngles event) {
      float partial = (float)event.getPartialTick();
      float roll = Mth.lerp(partial, prevBank, bank) + SpiderSwingController.glideCameraRollDeg(partial);
      float lift = Mth.lerp(partial, prevFloatLift, floatLift);
      float flip = hitstopTicks <= 0 && !ForcedCamera.active() ? Mth.lerp(partial, prevFlipPitch, flipPitch) : 0.0F;
      float dip = Mth.lerp(partial, prevZipDipPitch, zipDipPitch);
      if (!(Math.abs(roll) < 0.02F) || !(lift < 0.02F) || !(Math.abs(flip) < 0.02F) || !(Math.abs(dip) < 0.02F)) {
         event.setRoll(event.getRoll() + roll);
         event.setPitch(event.getPitch() - lift + flip + dip);
      }
   }
}
