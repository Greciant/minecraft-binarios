package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.brawler.BrawlerTuning;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public final class BrawlerComboSounds {
   private static final List<BrawlerComboSounds.Scheduled> QUEUE = new ArrayList<>();
   private static int tick;

   private BrawlerComboSounds() {
   }

   private static boolean enabled() {
      return (Boolean)ClientConfig.BRAWLER_COMBO_SOUNDS.get();
   }

   private static void play(SoundEvent sound, float pitch, float volume) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
      }
   }

   private static void later(int ticks, SoundEvent sound, float pitch, float volume) {
      QUEUE.add(new BrawlerComboSounds.Scheduled(tick + ticks, sound, pitch, volume));
   }

   static float plingPitch(int count) {
      int[] ladder = BrawlerTuning.PLING_SEMITONES;
      int step = count - 2;
      int last = ladder.length - 1;
      int index = step <= last ? Math.max(0, step) : (step % 2 == 0 ? last : last - 1);
      return 0.5F * (float)Math.pow(2.0, (double)ladder[index] / 12.0);
   }

   public static void hit(int count, int flavor) {
      if (enabled() && count >= 2) {
         float vol = flavor == 0 ? 0.35F : 0.6F;
         play((SoundEvent)SoundEvents.NOTE_BLOCK_PLING.value(), plingPitch(count), vol);
         if (count % 5 == 0) {
            play(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.3F, 0.5F);
         }
      }
   }

   public static void tierUp(int tier) {
      float f = enabled() ? 1.0F : 0.5F;
      switch (tier) {
         case 2:
            play(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.4F, 1.0F * f);
            play(SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 1.0F * f);
            break;
         case 3:
            play(SoundEvents.PLAYER_LEVELUP, 1.6F, 0.6F * f);
            play(SoundEvents.ANVIL_LAND, 1.5F, 0.4F * f);
            break;
         case 4:
            play(SoundEvents.ANVIL_LAND, 1.1F, 0.7F * f);
            play((SoundEvent)SoundEvents.TRIDENT_THUNDER.value(), 1.5F, 0.4F * f);
            break;
         case 5:
            play(SoundEvents.TOTEM_USE, 1.3F, 0.7F * f);
            play(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.4F, 0.5F * f);
      }
   }

   public static void hurry() {
      if (enabled()) {
         play((SoundEvent)SoundEvents.NOTE_BLOCK_HAT.value(), 1.6F, 0.35F);
      }
   }

   public static void drop(int ended) {
      if (enabled() && ended >= 5) {
         float[] pitches = BrawlerTuning.DROP_BASS_PITCH;

         for (int i = 0; i < pitches.length; i++) {
            later(i * 3, (SoundEvent)SoundEvents.NOTE_BLOCK_BASS.value(), pitches[i], 1.0F);
         }

         play(SoundEvents.ITEM_BREAK, 0.7F, 1.0F);
      }
   }

   public static void cashIn() {
      if (enabled()) {
         play((SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), 1.25F, 0.7F);

         for (float pitch : BrawlerTuning.CASH_IN_CHORD) {
            play((SoundEvent)SoundEvents.NOTE_BLOCK_PLING.value(), pitch, 0.6F);
         }
      }
   }

   static void tick() {
      tick++;
      if (!QUEUE.isEmpty()) {
         Iterator<BrawlerComboSounds.Scheduled> it = QUEUE.iterator();

         while (it.hasNext()) {
            BrawlerComboSounds.Scheduled s = it.next();
            if (s.dueTick <= tick) {
               play(s.sound, s.pitch, s.volume);
               it.remove();
            }
         }
      }
   }

   static void clear() {
      QUEUE.clear();
   }

   private static record Scheduled(int dueTick, SoundEvent sound, float pitch, float volume) {
   }
}
