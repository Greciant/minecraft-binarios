package com.devilfruits.core.poneglyph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public final class PoneglyphPlayerData {
   private PoneglyphPlayerData() {
   }

   public static record PlayerKnowledge(Set<ResourceLocation> learned) {
      public static final PoneglyphPlayerData.PlayerKnowledge EMPTY = new PoneglyphPlayerData.PlayerKnowledge(Set.of());
      public static final Codec<PoneglyphPlayerData.PlayerKnowledge> CODEC = ResourceLocation.CODEC
         .listOf()
         .xmap(l -> new PoneglyphPlayerData.PlayerKnowledge(Set.copyOf(l)), k -> List.copyOf(k.learned));
      public static final StreamCodec<ByteBuf, PoneglyphPlayerData.PlayerKnowledge> STREAM_CODEC = ResourceLocation.STREAM_CODEC
         .apply(ByteBufCodecs.list())
         .map(l -> new PoneglyphPlayerData.PlayerKnowledge(Set.copyOf(l)), k -> new ArrayList<>(k.learned));

      public PlayerKnowledge(Set<ResourceLocation> learned) {
         learned = Set.copyOf(learned);
         this.learned = learned;
      }

      public boolean knows(ResourceLocation id) {
         return this.learned.contains(id);
      }

      public PoneglyphPlayerData.PlayerKnowledge plus(ResourceLocation id) {
         Set<ResourceLocation> s = new HashSet<>(this.learned);
         s.add(id);
         return new PoneglyphPlayerData.PlayerKnowledge(s);
      }

      public PoneglyphPlayerData.PlayerKnowledge minus(ResourceLocation id) {
         Set<ResourceLocation> s = new HashSet<>(this.learned);
         s.remove(id);
         return new PoneglyphPlayerData.PlayerKnowledge(s);
      }
   }

   public static record PoneglyphLiteracy(int level, int xp) {
      public static final PoneglyphPlayerData.PoneglyphLiteracy EMPTY = new PoneglyphPlayerData.PoneglyphLiteracy(0, 0);
      public static final Codec<PoneglyphPlayerData.PoneglyphLiteracy> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  Codec.intRange(0, 5).optionalFieldOf("level", 0).forGetter(PoneglyphPlayerData.PoneglyphLiteracy::level),
                  Codec.INT.optionalFieldOf("xp", 0).forGetter(PoneglyphPlayerData.PoneglyphLiteracy::xp)
               )
               .apply(i, PoneglyphPlayerData.PoneglyphLiteracy::new)
      );
      public static final StreamCodec<ByteBuf, PoneglyphPlayerData.PoneglyphLiteracy> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT,
         PoneglyphPlayerData.PoneglyphLiteracy::level,
         ByteBufCodecs.VAR_INT,
         PoneglyphPlayerData.PoneglyphLiteracy::xp,
         PoneglyphPlayerData.PoneglyphLiteracy::new
      );
   }

   public static record RubbingProgress(Map<ResourceLocation, List<Long>> readyAt) {
      public static final PoneglyphPlayerData.RubbingProgress EMPTY = new PoneglyphPlayerData.RubbingProgress(Map.of());
      public static final Codec<PoneglyphPlayerData.RubbingProgress> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG.listOf())
         .xmap(PoneglyphPlayerData.RubbingProgress::new, PoneglyphPlayerData.RubbingProgress::readyAt);
      public static final StreamCodec<ByteBuf, PoneglyphPlayerData.RubbingProgress> STREAM_CODEC = ByteBufCodecs.map(
            HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list())
         )
         .map(PoneglyphPlayerData.RubbingProgress::new, PoneglyphPlayerData.RubbingProgress::readyAt);

      public RubbingProgress(Map<ResourceLocation, List<Long>> readyAt) {
         readyAt = Map.copyOf(readyAt);
         this.readyAt = readyAt;
      }

      public long readyAt(ResourceLocation poneglyph, int cell) {
         List<Long> cells = this.readyAt.get(poneglyph);
         return cells != null && cell < cells.size() ? cells.get(cell) : 0L;
      }

      public PoneglyphPlayerData.RubbingProgress with(ResourceLocation poneglyph, int cell, long time) {
         Long[] cells = new Long[9];
         Arrays.fill(cells, Long.valueOf(0L));
         List<Long> old = this.readyAt.get(poneglyph);
         if (old != null) {
            for (int i = 0; i < Math.min(9, old.size()); i++) {
               cells[i] = old.get(i);
            }
         }

         cells[cell] = time;
         Map<ResourceLocation, List<Long>> m = new HashMap<>(this.readyAt);
         m.put(poneglyph, List.of(cells));
         return new PoneglyphPlayerData.RubbingProgress(m);
      }
   }
}
