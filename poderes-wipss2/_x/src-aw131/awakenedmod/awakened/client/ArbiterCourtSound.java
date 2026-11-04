package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.content.entity.ArbiterEntity;
import com.awakenedmod.awakened.content.entity.ArbiterWhipEntity;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class ArbiterCourtSound {
   private static final float BASE = 0.15F;
   private static final double FULL_RANGE = 12.0;
   private static final double RANGE = 48.0;
   private static final float EASE = 0.12F;
   private static final float SILENT = 0.004F;
   private static final double OFF = Double.NaN;
   private static final ArbiterCourtSound.Voice KNEEL = new ArbiterCourtSound.Voice(AwakenedSounds.ARBITER_KNEEL_DRONE, 1.0F);
   private static final ArbiterCourtSound.Voice CHAIN = new ArbiterCourtSound.Voice(AwakenedSounds.ARBITER_CHAIN_RATTLE, 1.0F);

   private ArbiterCourtSound() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ArbiterCourtSound::onClientTick);
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      double kneel = Double.NaN;
      double chain = Double.NaN;
      if (mc.level != null && mc.player != null && !mc.player.isRemoved()) {
         for (Entity entity : mc.level.entitiesForRendering()) {
            if (!entity.isRemoved()) {
               if (entity instanceof ArbiterEntity) {
                  ArbiterEntity judge = (ArbiterEntity)entity;
                  if (judge.isKneeling() && !judge.isDeadOrDying() && judge.deathTime == 0) {
                     kneel = nearer(kneel, judge.distanceToSqr(mc.player));
                  }
               } else if (entity instanceof ArbiterWhipEntity) {
                  ArbiterWhipEntity head = (ArbiterWhipEntity)entity;
                  if (rattles(head)) {
                     chain = nearer(chain, head.distanceToSqr(mc.player));
                  }
               }
            }
         }
      }

      KNEEL.tick(mc, kneel);
      CHAIN.tick(mc, chain);
   }

   private static boolean rattles(ArbiterWhipEntity head) {
      byte mode = head.chainMode();
      return mode == 2 || mode == 1 && head.tickCount >= 4;
   }

   private static double nearer(double best, double distanceSq) {
      if (distanceSq > 2304.0) {
         return best;
      } else {
         double d = Math.sqrt(distanceSq);
         return !Double.isNaN(best) && !(d < best) ? best : d;
      }
   }

   private static float duck(double distance) {
      if (Double.isNaN(distance)) {
         return 0.0F;
      } else {
         return distance <= 12.0 ? 0.15F : 0.15F * (float)Math.max(0.0, 1.0 - (distance - 12.0) / 36.0);
      }
   }

   private static final class Instance extends AbstractTickableSoundInstance {
      private final ArbiterCourtSound.Voice voice;

      private Instance(ArbiterCourtSound.Voice voice) {
         super(voice.sound.get(), SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
         this.voice = voice;
         this.looping = true;
         this.delay = 0;
         this.relative = true;
         this.x = 0.0;
         this.y = 0.0;
         this.z = 0.0;
         this.volume = 0.001F;
         this.pitch = voice.pitch;
      }

      public void tick() {
         if (Minecraft.getInstance().level == null) {
            this.stop();
         } else {
            float target = ArbiterCourtSound.duck(this.voice.distance);
            this.volume = this.volume + (target - this.volume) * 0.12F;
            if (target <= 0.0F && this.volume <= 0.004F) {
               this.stop();
            }
         }
      }
   }

   private static final class Voice {
      private final Supplier<SoundEvent> sound;
      private final float pitch;
      private double distance = Double.NaN;
      private ArbiterCourtSound.Instance playing;
      private ClientLevel startedIn;

      private Voice(Supplier<SoundEvent> sound, float pitch) {
         this.sound = sound;
         this.pitch = pitch;
      }

      private void tick(Minecraft mc, double distance) {
         this.distance = distance;
         if (this.playing != null && (this.playing.isStopped() || mc.level != this.startedIn)) {
            this.playing = null;
            this.startedIn = null;
         }

         if (!Double.isNaN(distance) && this.playing == null && mc.level != null) {
            this.playing = new ArbiterCourtSound.Instance(this);
            this.startedIn = mc.level;
            mc.getSoundManager().play(this.playing);
         }
      }
   }
}
