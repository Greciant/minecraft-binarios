package com.awakenedmod.awakened.content.hydro;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public record HydroVisual(boolean surfing, boolean wet, long lanceStart, boolean lanceWet, long pourStart) {
   public static final HydroVisual NONE = new HydroVisual(false, false, -1L, false, -1L);
   public static final StreamCodec<ByteBuf, HydroVisual> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      HydroVisual::surfing,
      ByteBufCodecs.BOOL,
      HydroVisual::wet,
      ByteBufCodecs.VAR_LONG,
      HydroVisual::lanceStart,
      ByteBufCodecs.BOOL,
      HydroVisual::lanceWet,
      ByteBufCodecs.VAR_LONG,
      HydroVisual::pourStart,
      HydroVisual::new
   );

   public static HydroVisual of(Entity entity) {
      return (HydroVisual)entity.getData(AwakenedAttachments.HYDRO_VISUAL);
   }

   public long lanceAge(long now) {
      return age(this.lanceStart, now);
   }

   public long pourAge(long now) {
      return age(this.pourStart, now);
   }

   private static long age(long start, long now) {
      return start < 0L ? -1L : Math.max(0L, now - start);
   }

   static void setSurfing(ServerPlayer player, boolean surfing) {
      HydroVisual now = of(player);
      if (now.surfing != surfing) {
         set(player, new HydroVisual(surfing, now.wet, now.lanceStart, now.lanceWet, now.pourStart));
      }
   }

   static void setWet(ServerPlayer player, boolean wet) {
      HydroVisual now = of(player);
      if (now.wet != wet) {
         set(player, new HydroVisual(now.surfing, wet, now.lanceStart, now.lanceWet, now.pourStart));
      }
   }

   static void startLance(ServerPlayer player, boolean wet) {
      HydroVisual now = of(player);
      set(player, new HydroVisual(now.surfing, now.wet, player.serverLevel().getGameTime(), wet, now.pourStart));
   }

   static void endLance(ServerPlayer player) {
      HydroVisual now = of(player);
      if (now.lanceStart >= 0L) {
         set(player, new HydroVisual(now.surfing, now.wet, -1L, false, now.pourStart));
      }
   }

   static void startPour(ServerPlayer player) {
      HydroVisual now = of(player);
      set(player, new HydroVisual(now.surfing, now.wet, now.lanceStart, now.lanceWet, player.serverLevel().getGameTime()));
   }

   static void endPour(ServerPlayer player) {
      HydroVisual now = of(player);
      if (now.pourStart >= 0L) {
         set(player, new HydroVisual(now.surfing, now.wet, now.lanceStart, now.lanceWet, -1L));
      }
   }

   static void clear(ServerPlayer player) {
      if (!of(player).equals(NONE)) {
         set(player, NONE);
      }
   }

   private static void set(ServerPlayer player, HydroVisual visual) {
      player.setData(AwakenedAttachments.HYDRO_VISUAL, visual);
   }
}
