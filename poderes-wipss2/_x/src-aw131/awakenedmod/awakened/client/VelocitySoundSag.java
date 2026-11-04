package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.velocity.TimelessAbility;
import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

public final class VelocitySoundSag {
   private static final float MIN_PITCH = 0.5F;
   private static final double SELF_BLOCKS = 2.0;

   private VelocitySoundSag() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(VelocitySoundSag::onPlaySound);
   }

   private static void onPlaySound(PlaySoundEvent event) {
      SoundInstance sound = event.getSound();
      if (sound != null && !sound.isRelative() && !leftAlone(sound.getSource()) && !ours(sound)) {
         if (!(divisorAt(sound.getX(), sound.getY(), sound.getZ()) <= 1.0F)) {
            event.setSound(
               (SoundInstance)(sound instanceof TickableSoundInstance tickable ? new VelocitySoundSag.TickableSag(tickable) : new VelocitySoundSag.Sag(sound))
            );
         }
      }
   }

   private static boolean leftAlone(SoundSource source) {
      return source == SoundSource.MUSIC || source == SoundSource.MASTER || source == SoundSource.RECORDS;
   }

   private static boolean ours(SoundInstance sound) {
      return sound.getClass().getName().startsWith("com.awakenedmod.");
   }

   private static float divisorAt(double x, double y, double z) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return 1.0F;
      } else {
         VelocityVisual visual = VelocityVisualState.of(player);
         if (!visual.timeless()) {
            return 1.0F;
         } else {
            double radius = visual.fieldRadius();
            double distanceSq = player.distanceToSqr(x, y, z);
            return !(radius <= 0.0) && !(distanceSq > radius * radius) && !(distanceSq <= 4.0) ? (float)TimelessAbility.divisor(visual.gear()) : 1.0F;
         }
      }
   }

   private static float sagged(float pitch, double x, double y, double z) {
      return Math.max(0.5F, pitch / divisorAt(x, y, z));
   }

   private static class Sag implements SoundInstance {
      private final SoundInstance inner;

      Sag(SoundInstance inner) {
         this.inner = inner;
      }

      public float getPitch() {
         return VelocitySoundSag.sagged(this.inner.getPitch(), this.inner.getX(), this.inner.getY(), this.inner.getZ());
      }

      public ResourceLocation getLocation() {
         return this.inner.getLocation();
      }

      public WeighedSoundEvents resolve(SoundManager manager) {
         return this.inner.resolve(manager);
      }

      public Sound getSound() {
         return this.inner.getSound();
      }

      public SoundSource getSource() {
         return this.inner.getSource();
      }

      public boolean isLooping() {
         return this.inner.isLooping();
      }

      public boolean isRelative() {
         return this.inner.isRelative();
      }

      public int getDelay() {
         return this.inner.getDelay();
      }

      public float getVolume() {
         return this.inner.getVolume();
      }

      public double getX() {
         return this.inner.getX();
      }

      public double getY() {
         return this.inner.getY();
      }

      public double getZ() {
         return this.inner.getZ();
      }

      public Attenuation getAttenuation() {
         return this.inner.getAttenuation();
      }

      public boolean canStartSilent() {
         return this.inner.canStartSilent();
      }

      public boolean canPlaySound() {
         return this.inner.canPlaySound();
      }

      public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean looping) {
         return this.inner.getStream(buffers, sound, looping);
      }
   }

   private static final class TickableSag extends VelocitySoundSag.Sag implements TickableSoundInstance {
      private final TickableSoundInstance inner;

      TickableSag(TickableSoundInstance inner) {
         super(inner);
         this.inner = inner;
      }

      public boolean isStopped() {
         return this.inner.isStopped();
      }

      public void tick() {
         this.inner.tick();
      }
   }
}
