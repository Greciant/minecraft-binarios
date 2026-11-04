package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.core.net.LoopSoundPayload;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public final class LoopSound {
   static final int RENEW_TICKS = 5;
   static final int CHANGE_TICKS = 2;
   static final int FORGET_TICKS = 40;
   private static final Map<String, Integer> LENGTHS = Map.ofEntries(
      Map.entry("awakened:chrono.watch_loop", 0),
      Map.entry("awakened:stormcaller.tesla.buzz", 0),
      Map.entry("awakened:stormcaller.tesla.crackle", 0),
      Map.entry("awakened:ruin.rumble", 0),
      Map.entry("awakened:graviton.horizon.rumble", 0),
      Map.entry("awakened:graviton.horizon.whine", 0),
      Map.entry("awakened:graviton.dense.load", 0),
      Map.entry("awakened:graviton.well.hum", 0),
      Map.entry("awakened:graviton.well.rush", 0),
      Map.entry("awakened:graviton.hand.bend", 0),
      Map.entry("awakened:graviton.weightless.zone", 0),
      Map.entry("awakened:limitless.purple.charge", 0),
      Map.entry("awakened:limitless.purple.roar", 0),
      Map.entry("awakened:limitless.purple.erase", 0),
      Map.entry("minecraft:block.beacon.ambient", 116),
      Map.entry("minecraft:block.conduit.ambient", 91),
      Map.entry("minecraft:block.conduit.ambient.short", 70),
      Map.entry("minecraft:block.portal.ambient", 99),
      Map.entry("minecraft:entity.breeze.idle_air", 23),
      Map.entry("minecraft:weather.rain", 40),
      Map.entry("minecraft:item.brush.brushing.gravel", 12),
      Map.entry("minecraft:block.suspicious_gravel.step", 26)
   );
   static final int DEFAULT_LENGTH = 60;
   private static final Map<LoopSound.Id, LoopSound.Lease> LEASES = new HashMap<>();
   private static boolean registered;

   private LoopSound() {
   }

   private static int lengthOf(SoundEvent sound) {
      ResourceLocation key = BuiltInRegistries.SOUND_EVENT.getKey(sound);
      return key == null ? 60 : LENGTHS.getOrDefault(key.toString(), 60);
   }

   public static void keep(Entity anchor, String key, SoundEvent sound, SoundSource source, float volume, float pitch) {
      if (anchor.level() instanceof ServerLevel level) {
         keep(level, anchor.getId(), anchor.position(), key, sound, source, volume, pitch, 0.0F);
      }
   }

   public static void keepOnBeam(Entity caster, String key, double range, SoundEvent sound, SoundSource source, float volume, float pitch) {
      if (caster.level() instanceof ServerLevel level) {
         keep(level, caster.getId(), caster.position(), key, sound, source, volume, pitch, (float)range);
      }
   }

   public static void keepAt(ServerLevel level, String key, Vec3 pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
      keep(level, -1, pos, key, sound, source, volume, pitch, 0.0F);
   }

   public static void stop(Entity anchor, String key) {
      if (anchor.level() instanceof ServerLevel level) {
         stop(level, anchor.getId(), key);
      }
   }

   public static void stopAt(ServerLevel level, String key) {
      stop(level, -1, key);
   }

   private static void keep(
      ServerLevel level, int entityId, Vec3 pos, String key, SoundEvent sound, SoundSource source, float volume, float pitch, float beamRange
   ) {
      ensureRegistered();
      long now = level.getGameTime();
      LoopSound.Lease lease = LEASES.computeIfAbsent(new LoopSound.Id(level.dimension(), entityId, key), id -> new LoopSound.Lease());
      lease.level = level;
      lease.pos = pos;
      lease.lastKept = now;
      long since = now - lease.lastSent;
      boolean changed = lease.sound != sound || Math.abs(lease.volume - volume) > 0.03F || Math.abs(lease.pitch - pitch) > 0.02F;
      if (since >= 5L || changed && since >= 2L) {
         lease.sound = sound;
         lease.source = source;
         lease.volume = volume;
         lease.pitch = pitch;
         lease.beamRange = beamRange;
         lease.lastSent = now;
         send(lease, entityId, key, false);
      }
   }

   private static void stop(ServerLevel level, int entityId, String key) {
      LoopSound.Lease lease = LEASES.remove(new LoopSound.Id(level.dimension(), entityId, key));
      if (lease != null && lease.sound != null) {
         send(lease, entityId, key, true);
      }
   }

   private static void send(LoopSound.Lease lease, int entityId, String key, boolean stop) {
      LoopSoundPayload payload = new LoopSoundPayload(
         entityId,
         lease.pos.x,
         lease.pos.y,
         lease.pos.z,
         key,
         BuiltInRegistries.SOUND_EVENT.wrapAsHolder(lease.sound),
         lease.source,
         lease.volume,
         lease.pitch,
         lease.beamRange,
         lengthOf(lease.sound),
         stop
      );
      double range = (double)lease.sound.getRange(lease.volume) + 16.0 + (double)lease.beamRange;

      for (ServerPlayer player : lease.level.players()) {
         if (player.position().distanceToSqr(lease.pos) <= range * range) {
            PacketDistributor.sendToPlayer(player, payload, new CustomPacketPayload[0]);
         }
      }
   }

   private static synchronized void ensureRegistered() {
      if (!registered) {
         registered = true;
         NeoForge.EVENT_BUS.addListener(e -> {
            if (!LEASES.isEmpty()) {
               LEASES.values().removeIf(l -> l.level.getGameTime() - l.lastKept > 40L);
            }
         });
         NeoForge.EVENT_BUS.addListener(e -> LEASES.clear());
      }
   }

   private static record Id(ResourceKey<Level> dimension, int entityId, String key) {
   }

   private static final class Lease {
      ServerLevel level;
      Vec3 pos;
      SoundEvent sound;
      SoundSource source;
      float volume;
      float pitch;
      float beamRange;
      long lastSent = -1000L;
      long lastKept;
   }
}
