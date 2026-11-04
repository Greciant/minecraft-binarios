package com.awakenedmod.awakened.core.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class RelicData {
   public static final int MAX_SLOTS = 3;
   public static final MapCodec<RelicData> MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               Codec.STRING.listOf().optionalFieldOf("attuned", List.of()).forGetter(d -> d.attuned),
               Codec.BOOL.optionalFieldOf("belt", false).forGetter(d -> d.belt),
               Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("levels", Map.of()).forGetter(d -> d.levels)
            )
            .apply(instance, RelicData::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, RelicData> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      RelicData::hasBelt,
      ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
      RelicData::attuned,
      ByteBufCodecs.map(i -> new HashMap(), ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT),
      RelicData::levels,
      RelicData::create
   );
   private final List<String> attuned;
   private boolean belt;
   private final Map<String, Integer> levels;

   public RelicData() {
      this(List.of(), false, Map.of());
   }

   private RelicData(List<String> attuned, boolean belt, Map<String, Integer> levels) {
      this.attuned = new ArrayList<>(attuned);
      this.belt = belt;
      this.levels = new HashMap<>(levels);
   }

   private static RelicData create(boolean belt, List<String> attuned, Map<String, Integer> levels) {
      return new RelicData(attuned, belt, levels);
   }

   public Map<String, Integer> levels() {
      return this.levels;
   }

   public boolean hasBelt() {
      return this.belt;
   }

   public void setBelt(boolean belt) {
      this.belt = belt;
   }

   public List<String> attuned() {
      return this.attuned;
   }

   public boolean isAttuned(String id) {
      return this.attuned.contains(id);
   }

   public boolean hasFreeSlot() {
      return this.attuned.size() < 3;
   }

   public void setAttuned(List<String> ids) {
      this.attuned.clear();

      for (String id : ids) {
         if (id != null && this.attuned.size() < 3) {
            this.attuned.add(id);
         }
      }
   }

   public void clearAttuned() {
      this.attuned.clear();
   }
}
