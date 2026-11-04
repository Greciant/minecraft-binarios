package com.awakenedmod.awakened.core;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;

public final class SoundMixer {
   static final double MERGE_RADIUS = 6.0;
   static final float MERGE_PITCH = 0.05F;
   static final float MERGE_VOLUME_STEP = 0.15F;
   static final float MERGE_VOLUME_CAP = 1.5F;
   static final float PITCH_JITTER = 0.04F;
   private static final List<SoundMixer.Voice> PENDING = new ArrayList<>();
   private static final Map<SoundEvent, Boolean> TONAL = new IdentityHashMap<>();
   private static boolean registered;
   private static boolean inTick;
   private static boolean flushQueued;

   private SoundMixer() {
   }

   public static void play(ServerLevel level, Vec3 pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
      ensureRegistered();
      queue(level, pos, sound, source, volume, pitch);
      if (!inTick && !flushQueued) {
         flushQueued = true;
         level.getServer().execute(() -> {
            flushQueued = false;
            flush();
         });
      }
   }

   private static void queue(ServerLevel level, Vec3 pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
      for (SoundMixer.Voice voice : PENDING) {
         if (voice.absorbs(level, pos, sound, source, pitch)) {
            voice.add(pos, volume, pitch);
            return;
         }
      }

      PENDING.add(new SoundMixer.Voice(level, sound, source, pos, volume, pitch));
   }

   private static synchronized void ensureRegistered() {
      if (!registered) {
         registered = true;
         NeoForge.EVENT_BUS.addListener(e -> inTick = true);
         NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, e -> {
            inTick = false;
            flush();
         });
         NeoForge.EVENT_BUS.addListener(e -> {
            inTick = false;
            flushQueued = false;
            PENDING.clear();
         });
      }
   }

   private static void flush() {
      if (!PENDING.isEmpty()) {
         List<SoundMixer.Voice> batch = new ArrayList<>(PENDING);
         PENDING.clear();

         for (SoundMixer.Voice voice : batch) {
            voice.send();
         }
      }
   }

   private static boolean tonal(SoundEvent sound) {
      return TONAL.computeIfAbsent(
         sound,
         s -> {
            ResourceLocation key = BuiltInRegistries.SOUND_EVENT.getKey(s);
            if (key == null) {
               return false;
            } else {
               String path = key.getPath();
               return path.startsWith("block.note_block.")
                  || path.startsWith("block.bell.")
                  || path.equals("entity.player.levelup")
                  || path.equals("entity.experience_orb.pickup");
            }
         }
      );
   }

   private static final class Voice {
      private final ServerLevel level;
      private final SoundEvent sound;
      private final SoundSource source;
      private final Vec3 anchor;
      private double sumX;
      private double sumY;
      private double sumZ;
      private int copies;
      private float loudest;
      private float pitch;

      Voice(ServerLevel level, SoundEvent sound, SoundSource source, Vec3 pos, float volume, float pitch) {
         this.level = level;
         this.sound = sound;
         this.source = source;
         this.anchor = pos;
         this.pitch = pitch;
         this.add(pos, volume, pitch);
      }

      boolean absorbs(ServerLevel level, Vec3 pos, SoundEvent sound, SoundSource source, float pitch) {
         return this.level == level
            && this.sound == sound
            && this.source == source
            && Math.abs(this.pitch - pitch) <= 0.05F
            && this.anchor.distanceToSqr(pos) <= 36.0;
      }

      void add(Vec3 pos, float volume, float pitch) {
         this.sumX = this.sumX + pos.x;
         this.sumY = this.sumY + pos.y;
         this.sumZ = this.sumZ + pos.z;
         this.copies++;
         if (volume > this.loudest || this.copies == 1) {
            this.loudest = volume;
            this.pitch = pitch;
         }
      }

      void send() {
         float volume = this.loudest * Math.min(1.5F, 1.0F + 0.15F * (float)(this.copies - 1));
         float played = SoundMixer.tonal(this.sound) ? this.pitch : this.pitch * (1.0F + (this.level.getRandom().nextFloat() * 2.0F - 1.0F) * 0.04F);
         this.level
            .playSound(
               null, this.sumX / (double)this.copies, this.sumY / (double)this.copies, this.sumZ / (double)this.copies, this.sound, this.source, volume, played
            );
      }
   }
}
