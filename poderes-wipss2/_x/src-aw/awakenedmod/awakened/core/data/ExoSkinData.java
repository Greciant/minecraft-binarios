package com.awakenedmod.awakened.core.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class ExoSkinData {
   public static final String DEFAULT_ID = "default";
   public static final MapCodec<ExoSkinData> MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               Codec.STRING.listOf().optionalFieldOf("unlocked", List.of()).forGetter(ExoSkinData::unlockedList),
               Codec.STRING.optionalFieldOf("selected", "default").forGetter(ExoSkinData::selected)
            )
            .apply(instance, ExoSkinData::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ExoSkinData> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ExoSkinData::unlockedList, ByteBufCodecs.STRING_UTF8, ExoSkinData::selected, ExoSkinData::new
   );
   private final Set<String> unlocked;
   private String selected;

   public ExoSkinData() {
      this(List.of(), "default");
   }

   private ExoSkinData(List<String> unlocked, String selected) {
      this.unlocked = new LinkedHashSet<>(unlocked);
      this.selected = selected;
   }

   private List<String> unlockedList() {
      return List.copyOf(this.unlocked);
   }

   public boolean isUnlocked(String id) {
      return "default".equals(id) || this.unlocked.contains(id);
   }

   public boolean unlock(String id) {
      return this.unlocked.add(id);
   }

   public String selected() {
      return this.selected;
   }

   public void select(String id) {
      this.selected = id;
   }
}
