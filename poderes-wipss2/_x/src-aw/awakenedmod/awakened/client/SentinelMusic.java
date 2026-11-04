package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.content.entity.sentinel.SentinelEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.Music;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

public final class SentinelMusic {
   private static final double RANGE_SQ = 2304.0;
   private static Music battleMusic;

   private SentinelMusic() {
   }

   private static Music battleMusic() {
      if (battleMusic == null) {
         battleMusic = new Music(AwakenedSounds.SENTINEL_BATTLE, 0, 0, true);
      }

      return battleMusic;
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(SentinelMusic::onSelectMusic);
   }

   private static void onSelectMusic(SelectMusicEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof SentinelEntity sentinel && sentinel.isAlive() && sentinel.isInCombat() && sentinel.distanceToSqr(mc.player) <= 2304.0) {
               event.overrideMusic(battleMusic());
               return;
            }
         }
      }
   }
}
