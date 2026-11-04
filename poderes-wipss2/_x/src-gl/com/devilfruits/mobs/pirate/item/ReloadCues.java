package com.devilfruits.mobs.pirate.item;

import com.devilfruits.mobs.pirate.PirateClips;
import com.devilfruits.mobs.pirate.Pirates;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public final class ReloadCues {
   public static void sound(Entity at, int action, int tick, SoundSource source, boolean cock) {
      if (action >= 0 && action < PirateClips.NAMES.length && PirateClips.HALF_COCK[action] >= 0) {
         SoundEvent sound = null;
         float volume = 0.6F;
         float pitch = 1.0F;
         if (tick == PirateClips.HALF_COCK[action] || tick == PirateClips.PAN_SHUT[action]) {
            sound = Pirates.GUN_CLICK.get();
            volume = 0.5F;
            pitch = 1.3F;
         } else if (tick == PirateClips.PRIME[action]) {
            sound = Pirates.GUN_POUR.get();
            volume = 0.3F;
            pitch = 1.5F;
         } else if (tick == PirateClips.POUR[action]) {
            sound = Pirates.GUN_POUR.get();
         } else if (tick == PirateClips.ROD_OUT[action] || tick == PirateClips.ROD_IN[action]) {
            sound = Pirates.GUN_RAM.get();
            volume = 0.3F;
            pitch = 1.7F;
         } else if (tick == PirateClips.RAM_1[action] || tick == PirateClips.RAM_2[action]) {
            sound = Pirates.GUN_RAM.get();
            volume = 0.7F;
            pitch = action == 6 ? 1.3F : 1.0F;
         } else if (cock && tick == PirateClips.HIT[action]) {
            sound = Pirates.GUN_CLICK.get();
            volume = 0.9F;
         }

         if (sound != null) {
            at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, source, volume, pitch);
         }
      }
   }

   public static int action(boolean musket) {
      return musket ? 8 : 6;
   }

   public static float drillTick(boolean musket, float used, int total) {
      int action = action(musket);
      return Math.max(0.0F, Math.min(1.0F, used / (float)Math.max(1, total))) * (float)PirateClips.LENGTH[action];
   }

   private ReloadCues() {
   }
}
