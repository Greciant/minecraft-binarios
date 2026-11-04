package com.awakenedmod.awakened.content.reaper;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class ReaperSoulData {
   public static final MapCodec<ReaperSoulData> MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(Codec.LONG.optionalFieldOf("souls", 0L).forGetter(d -> d.souls)).apply(instance, ReaperSoulData::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ReaperSoulData> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_LONG, d -> d.souls, ReaperSoulData::new
   );
   private long souls;

   public ReaperSoulData() {
      this(0L);
   }

   private ReaperSoulData(long souls) {
      this.souls = Math.max(0L, souls);
   }

   public long souls() {
      return this.souls;
   }

   public void add(long n) {
      if (n > 0L) {
         this.souls += n;
      }
   }

   public void withdraw(long n) {
      if (n > 0L) {
         this.souls = Math.max(0L, this.souls - n);
      }
   }

   public void set(long n) {
      this.souls = Math.max(0L, n);
   }
}
