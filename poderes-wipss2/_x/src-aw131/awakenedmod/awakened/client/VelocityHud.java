package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.velocity.Gearbox;
import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public final class VelocityHud {
   private static final float CHIME_PITCH_BASE = 0.8F;
   private static final float CHIME_PITCH_PER_GEAR = 0.1F;
   private static final float CHIME_VOLUME = 0.7F;
   private static final float READY_PITCH_BASE = 1.2F;
   private static final float READY_PITCH_PER_GEAR = 0.1F;
   private static final float READY_VOLUME = 0.25F;
   private static final int UNKNOWN = -1;
   private static int lastGear = -1;
   private static int lastEarned = -1;
   private static int lastPlayerId = -1;

   private VelocityHud() {
   }

   public static void tickChime(LocalPlayer local) {
      if (local != null && "velocity".equals(ClientPowerState.get().powerId())) {
         if (local.getId() != lastPlayerId) {
            lastPlayerId = local.getId();
            lastGear = -1;
            lastEarned = -1;
         }

         VelocityVisual vis = VelocityVisualState.of(local);
         int gear = vis.gear();
         if (lastGear != -1 && gear != lastGear && vis.overdrive() && !idleDown(vis, gear)) {
            Minecraft.getInstance()
               .getSoundManager()
               .play(SimpleSoundInstance.forUI((SoundEvent)SoundEvents.NOTE_BLOCK_BELL.value(), 0.8F + 0.1F * (float)gear, 0.7F));
         }

         if (readyPing(vis)) {
            Minecraft.getInstance()
               .getSoundManager()
               .play(SimpleSoundInstance.forUI((SoundEvent)SoundEvents.NOTE_BLOCK_PLING.value(), 1.2F + 0.1F * (float)vis.earnedGear(), 0.25F));
         }

         lastGear = gear;
         lastEarned = vis.earnedGear();
      } else {
         lastGear = -1;
         lastEarned = -1;
         lastPlayerId = -1;
      }
   }

   private static boolean readyPing(VelocityVisual vis) {
      return lastEarned != -1
         && vis.overdrive()
         && vis.earnedGear() > lastEarned
         && vis.earnedGear() > vis.chosen()
         && !PassiveToggleClient.enabled(Gearbox.AUTO);
   }

   private static boolean idleDown(VelocityVisual vis, int gear) {
      return gear < lastGear && !vis.sprinting() && gear == vis.earnedGear();
   }
}
