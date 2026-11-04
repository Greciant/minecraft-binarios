package com.awakenedmod.awakened.client.music;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class BossNoteSequencer {
   private static final List<BossTrack> TRACKS = new ArrayList<>();

   private BossNoteSequencer() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(BossNoteSequencer::onClientTick);
      gameBus.addListener(BossNoteSequencer::onSelectMusic);
      TRACKS.add(new VarkosTheme());
      TRACKS.add(new ArbiterTheme());
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (!mc.isPaused()) {
         for (BossTrack track : TRACKS) {
            track.clientTick(mc);
         }
      }
   }

   private static void onSelectMusic(SelectMusicEvent event) {
      for (BossTrack track : TRACKS) {
         if (track.playing()) {
            event.overrideMusic(null);
            return;
         }
      }
   }
}
