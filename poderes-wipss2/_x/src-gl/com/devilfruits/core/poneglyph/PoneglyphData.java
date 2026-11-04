package com.devilfruits.core.poneglyph;

import com.devilfruits.DevilFruits;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class PoneglyphData {
   public static final Codec<String> WORD = Codec.STRING
      .validate(s -> s.matches("[a-z]{2,10}") ? DataResult.success(s) : DataResult.error(() -> "Poneglyph words are 2-10 lowercase letters a-z: " + s));
   private static volatile Map<ResourceLocation, PoneglyphData.Poneglyph> poneglyphs = Map.of();
   private static volatile Map<ResourceLocation, PoneglyphData.Knowledge> knowledge = Map.of();

   @Nullable
   public static PoneglyphData.Poneglyph poneglyph(ResourceLocation id) {
      return poneglyphs.get(id);
   }

   @Nullable
   public static PoneglyphData.Knowledge knowledge(ResourceLocation id) {
      return knowledge.get(id);
   }

   public static Map<ResourceLocation, PoneglyphData.Poneglyph> poneglyphs() {
      return poneglyphs;
   }

   public static Map<ResourceLocation, PoneglyphData.Knowledge> knowledgeBranches() {
      return knowledge;
   }

   public static Collection<PoneglyphData.Poneglyph> allPoneglyphs() {
      return Collections.unmodifiableCollection(poneglyphs.values());
   }

   public static void set(Map<ResourceLocation, PoneglyphData.Poneglyph> p, Map<ResourceLocation, PoneglyphData.Knowledge> k) {
      poneglyphs = Collections.unmodifiableMap(new TreeMap<>(p));
      knowledge = Collections.unmodifiableMap(new TreeMap<>(k));
   }

   private PoneglyphData() {
   }

   public static record Knowledge(List<ResourceLocation> recipes, List<ResourceLocation> requires, ResourceLocation icon, String nameKey) {
      public static final Codec<PoneglyphData.Knowledge> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  ResourceLocation.CODEC.listOf().optionalFieldOf("recipes", List.of()).forGetter(PoneglyphData.Knowledge::recipes),
                  ResourceLocation.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(PoneglyphData.Knowledge::requires),
                  ResourceLocation.CODEC.optionalFieldOf("icon", ResourceLocation.withDefaultNamespace("paper")).forGetter(PoneglyphData.Knowledge::icon),
                  Codec.STRING.optionalFieldOf("name_key", "").forGetter(PoneglyphData.Knowledge::nameKey)
               )
               .apply(i, PoneglyphData.Knowledge::new)
      );

      public String nameKey(ResourceLocation id) {
         return this.nameKey.isEmpty() ? nameKey(id.getNamespace(), id.getPath()) : this.nameKey;
      }

      public static String nameKey(String namespace, String path) {
         return "knowledge." + namespace + "." + path;
      }
   }

   public static final class Loader<T> extends SimpleJsonResourceReloadListener {
      private final Codec<T> codec;
      private final boolean poneglyphFolder;

      private Loader(String folder, Codec<T> codec, boolean poneglyphFolder) {
         super(new Gson(), folder);
         this.codec = codec;
         this.poneglyphFolder = poneglyphFolder;
      }

      public static PoneglyphData.Loader<PoneglyphData.Poneglyph> poneglyphs() {
         return new PoneglyphData.Loader<>("poneglyph", PoneglyphData.Poneglyph.CODEC, true);
      }

      public static PoneglyphData.Loader<PoneglyphData.Knowledge> knowledge() {
         return new PoneglyphData.Loader<>("knowledge", PoneglyphData.Knowledge.CODEC, false);
      }

      protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
         Map<ResourceLocation, T> out = new TreeMap<>();
         files.forEach(
            (id, json) -> this.codec
                  .parse(JsonOps.INSTANCE, json)
                  .resultOrPartial(
                     err -> DevilFruits.LOGGER.error("Bad {} file {}: {}", new Object[]{this.poneglyphFolder ? "poneglyph" : "knowledge", id, err})
                  )
                  .ifPresent(v -> out.put(id, (T)v))
         );
         if (this.poneglyphFolder) {
            PoneglyphData.poneglyphs = Collections.unmodifiableMap(out);
         } else {
            PoneglyphData.knowledge = Collections.unmodifiableMap(out);
         }

         DevilFruits.LOGGER.info("Loaded {} {} definitions", out.size(), this.poneglyphFolder ? "poneglyph" : "knowledge");
      }
   }

   public static record Poneglyph(ResourceLocation knowledge, List<String> words, int carvingVariant, String loreKey, int minLiteracy, int weight) {
      public static final Codec<PoneglyphData.Poneglyph> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  ResourceLocation.CODEC.fieldOf("knowledge").forGetter(PoneglyphData.Poneglyph::knowledge),
                  PoneglyphData.WORD
                     .listOf()
                     .validate(
                        l -> l.size() >= 6 && l.stream().distinct().count() == (long)l.size()
                              ? DataResult.success(l)
                              : DataResult.error(() -> "A poneglyph needs at least 6 different words")
                     )
                     .fieldOf("words")
                     .forGetter(PoneglyphData.Poneglyph::words),
                  Codec.intRange(0, 3).optionalFieldOf("carving_variant", 0).forGetter(PoneglyphData.Poneglyph::carvingVariant),
                  Codec.STRING.optionalFieldOf("lore_key", "").forGetter(PoneglyphData.Poneglyph::loreKey),
                  Codec.intRange(0, 5).optionalFieldOf("min_literacy", 0).forGetter(PoneglyphData.Poneglyph::minLiteracy),
                  Codec.intRange(0, 1000).optionalFieldOf("weight", 10).forGetter(PoneglyphData.Poneglyph::weight)
               )
               .apply(i, PoneglyphData.Poneglyph::new)
      );

      public String loreKey(ResourceLocation id) {
         return this.loreKey.isEmpty() ? "poneglyph." + id.getNamespace() + "." + id.getPath() + ".lore" : this.loreKey;
      }
   }
}
