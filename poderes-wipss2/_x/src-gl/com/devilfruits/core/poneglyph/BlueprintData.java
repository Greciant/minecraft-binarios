package com.devilfruits.core.poneglyph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public final class BlueprintData {
   private BlueprintData() {
   }

   public static record Deciphered(ResourceLocation knowledge, ResourceLocation poneglyph) {
      public static final Codec<BlueprintData.Deciphered> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  ResourceLocation.CODEC.fieldOf("knowledge").forGetter(BlueprintData.Deciphered::knowledge),
                  ResourceLocation.CODEC.fieldOf("poneglyph").forGetter(BlueprintData.Deciphered::poneglyph)
               )
               .apply(i, BlueprintData.Deciphered::new)
      );
      public static final StreamCodec<ByteBuf, BlueprintData.Deciphered> STREAM_CODEC = StreamCodec.composite(
         ResourceLocation.STREAM_CODEC,
         BlueprintData.Deciphered::knowledge,
         ResourceLocation.STREAM_CODEC,
         BlueprintData.Deciphered::poneglyph,
         BlueprintData.Deciphered::new
      );
   }

   public static record Encrypted(ResourceLocation poneglyph, long seed, BlueprintData.Progress progress) {
      public static final Codec<BlueprintData.Encrypted> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  ResourceLocation.CODEC.fieldOf("poneglyph").forGetter(BlueprintData.Encrypted::poneglyph),
                  Codec.LONG.fieldOf("seed").forGetter(BlueprintData.Encrypted::seed),
                  BlueprintData.Progress.CODEC.optionalFieldOf("progress", BlueprintData.Progress.EMPTY).forGetter(BlueprintData.Encrypted::progress)
               )
               .apply(i, BlueprintData.Encrypted::new)
      );
      public static final StreamCodec<ByteBuf, BlueprintData.Encrypted> STREAM_CODEC = StreamCodec.composite(
         ResourceLocation.STREAM_CODEC,
         BlueprintData.Encrypted::poneglyph,
         ByteBufCodecs.VAR_LONG,
         BlueprintData.Encrypted::seed,
         BlueprintData.Progress.STREAM_CODEC,
         BlueprintData.Encrypted::progress,
         BlueprintData.Encrypted::new
      );

      public BlueprintData.Encrypted with(BlueprintData.Progress p) {
         return new BlueprintData.Encrypted(this.poneglyph, this.seed, p);
      }
   }

   public static record Progress(int owner, int draw, int round, int mistakes, long lockUntil, int flags, int shuffle) {
      public static final int HINT_USED = 1;
      public static final int REVEAL_USED = 2;
      public static final int FORGIVE_USED = 4;
      public static final BlueprintData.Progress EMPTY = new BlueprintData.Progress(0, 0, 0, 0, 0L, 0, 0);
      public static final Codec<BlueprintData.Progress> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  Codec.INT.optionalFieldOf("owner", 0).forGetter(BlueprintData.Progress::owner),
                  Codec.INT.optionalFieldOf("draw", 0).forGetter(BlueprintData.Progress::draw),
                  Codec.INT.optionalFieldOf("round", 0).forGetter(BlueprintData.Progress::round),
                  Codec.INT.optionalFieldOf("mistakes", 0).forGetter(BlueprintData.Progress::mistakes),
                  Codec.LONG.optionalFieldOf("lock_until", 0L).forGetter(BlueprintData.Progress::lockUntil),
                  Codec.INT.optionalFieldOf("flags", 0).forGetter(BlueprintData.Progress::flags),
                  Codec.INT.optionalFieldOf("shuffle", 0).forGetter(BlueprintData.Progress::shuffle)
               )
               .apply(i, BlueprintData.Progress::new)
      );
      public static final StreamCodec<ByteBuf, BlueprintData.Progress> STREAM_CODEC = new StreamCodec<ByteBuf, BlueprintData.Progress>() {
         public BlueprintData.Progress decode(ByteBuf buf) {
            return new BlueprintData.Progress(
               buf.readInt(),
               (Integer)ByteBufCodecs.VAR_INT.decode(buf),
               (Integer)ByteBufCodecs.VAR_INT.decode(buf),
               (Integer)ByteBufCodecs.VAR_INT.decode(buf),
               buf.readLong(),
               (Integer)ByteBufCodecs.VAR_INT.decode(buf),
               (Integer)ByteBufCodecs.VAR_INT.decode(buf)
            );
         }

         public void encode(ByteBuf buf, BlueprintData.Progress p) {
            buf.writeInt(p.owner);
            ByteBufCodecs.VAR_INT.encode(buf, p.draw);
            ByteBufCodecs.VAR_INT.encode(buf, p.round);
            ByteBufCodecs.VAR_INT.encode(buf, p.mistakes);
            buf.writeLong(p.lockUntil);
            ByteBufCodecs.VAR_INT.encode(buf, p.flags);
            ByteBufCodecs.VAR_INT.encode(buf, p.shuffle);
         }
      };

      public boolean has(int flag) {
         return (this.flags & flag) != 0;
      }

      public BlueprintData.Progress with(int flag) {
         return new BlueprintData.Progress(this.owner, this.draw, this.round, this.mistakes, this.lockUntil, this.flags | flag, this.shuffle);
      }
   }

   public static record Rubbing(ResourceLocation poneglyph, int cell) {
      public static final Codec<BlueprintData.Rubbing> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  ResourceLocation.CODEC.fieldOf("poneglyph").forGetter(BlueprintData.Rubbing::poneglyph),
                  Codec.intRange(0, 8).fieldOf("cell").forGetter(BlueprintData.Rubbing::cell)
               )
               .apply(i, BlueprintData.Rubbing::new)
      );
      public static final StreamCodec<ByteBuf, BlueprintData.Rubbing> STREAM_CODEC = StreamCodec.composite(
         ResourceLocation.STREAM_CODEC, BlueprintData.Rubbing::poneglyph, ByteBufCodecs.VAR_INT, BlueprintData.Rubbing::cell, BlueprintData.Rubbing::new
      );
   }
}
