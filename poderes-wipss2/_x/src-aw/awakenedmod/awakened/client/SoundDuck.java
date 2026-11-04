package com.awakenedmod.awakened.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class SoundDuck {
   private static final float FLOOR = 0.02F;
   private static final List<SoundDuck.Source> SOURCES = new CopyOnWriteArrayList<>();

   private SoundDuck() {
   }

   public static void add(SoundDuck.Source source) {
      SOURCES.add(source);
   }

   public static float gain(SoundInstance sound) {
      if (!SOURCES.isEmpty() && !spared(sound)) {
         float gain = 1.0F;

         for (SoundDuck.Source s : SOURCES) {
            gain *= s.gain(sound);
         }

         return gain >= 1.0F ? 1.0F : Math.max(0.02F, gain);
      } else {
         return 1.0F;
      }
   }

   private static boolean spared(SoundInstance sound) {
      if (sound.getClass().getName().startsWith("com.awakenedmod.")) {
         return true;
      } else {
         SoundSource source = sound.getSource();
         return source == SoundSource.MASTER;
      }
   }

   @SubscribeEvent
   public static void onTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      boolean changed = false;

      for (SoundDuck.Source s : SOURCES) {
         changed |= s.tick(mc);
      }

      if (changed) {
         refresh(mc);
      }
   }

   @SubscribeEvent
   public static void onMusic(SelectMusicEvent event) {
      for (SoundDuck.Source s : SOURCES) {
         if (s.holdsMusic()) {
            event.overrideMusic(null);
            return;
         }
      }
   }

   private static void refresh(Minecraft mc) {
      mc.getSoundManager().updateSourceVolume(SoundSource.AMBIENT, mc.options.getSoundSourceVolume(SoundSource.AMBIENT));
   }

   public interface Source {
      boolean tick(Minecraft var1);

      float gain(SoundInstance var1);

      default boolean holdsMusic() {
         return false;
      }
   }
}
