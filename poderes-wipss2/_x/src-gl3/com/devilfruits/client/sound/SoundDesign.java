package com.devilfruits.client.sound;

import com.devilfruits.core.config.CoreClientConfig;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class SoundDesign {
   public static SoundDesign.Layer layer(Supplier<SoundEvent> sound, float volume, float pitch) {
      return new SoundDesign.Layer(sound, volume, pitch);
   }

   public static void play(Vec3 pos, Supplier<SoundEvent> signature, float volume, float pitch, boolean distanceDelay, SoundDesign.Layer... layers) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         level.playLocalSound(pos.x, pos.y, pos.z, signature.get(), SoundSource.PLAYERS, volume, pitch, distanceDelay);
         if ((Boolean)CoreClientConfig.LAYERED_SOUNDS.get()) {
            for (SoundDesign.Layer layer : layers) {
               level.playLocalSound(pos.x, pos.y, pos.z, layer.sound.get(), SoundSource.PLAYERS, layer.volume, layer.pitch, distanceDelay);
            }
         }
      }
   }

   public static void play(Vec3 pos, Supplier<SoundEvent> signature, float volume, float pitch, SoundDesign.Layer... layers) {
      play(pos, signature, volume, pitch, false, layers);
   }

   public static void layerOnly(Vec3 pos, Supplier<SoundEvent> layer, float volume, float pitch) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && (Boolean)CoreClientConfig.LAYERED_SOUNDS.get()) {
         level.playLocalSound(pos.x, pos.y, pos.z, layer.get(), SoundSource.PLAYERS, volume, pitch, false);
      }
   }

   private SoundDesign() {
   }

   public static record Layer(Supplier<SoundEvent> sound, float volume, float pitch) {
   }
}
