package com.devilfruits.client.model.geo;

import com.devilfruits.DevilFruits;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class GeoResources {
   public static final GeoResources.Models MODELS = new GeoResources.Models();
   public static final GeoResources.Animations ANIMATIONS = new GeoResources.Animations();

   @Nullable
   public static GeoModel model(ResourceLocation id) {
      return MODELS.models.get(id);
   }

   @Nullable
   public static GeoAnimation animation(ResourceLocation id) {
      return ANIMATIONS.clips.get(id);
   }

   private GeoResources() {
   }

   public static final class Animations extends SimpleJsonResourceReloadListener {
      private Map<ResourceLocation, GeoAnimation> clips = Map.of();

      private Animations() {
         super(new Gson(), "geo_animations");
      }

      protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager manager, ProfilerFiller profiler) {
         Map<ResourceLocation, GeoAnimation> loaded = new HashMap<>();
         jsons.forEach((file, json) -> {
            if (file.getPath().endsWith(".animation")) {
               String prefix = file.getPath().substring(0, file.getPath().length() - ".animation".length()) + "/";

               try {
                  for (GeoAnimation clip : GeoAnimation.parseFile(file.getNamespace(), prefix, json.getAsJsonObject())) {
                     loaded.put(clip.id, clip);
                  }
               } catch (RuntimeException var6) {
                  DevilFruits.LOGGER.error("Invalid geo animation file {}: {}", file, var6.toString());
               }
            }
         });
         this.clips = loaded;
         DevilFruits.LOGGER.debug("Loaded {} geo animation clips", loaded.size());
      }
   }

   public static final class Models extends SimpleJsonResourceReloadListener {
      private Map<ResourceLocation, GeoModel> models = Map.of();
      private int generation;

      private Models() {
         super(new Gson(), "geo");
      }

      public int generation() {
         return this.generation;
      }

      protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager manager, ProfilerFiller profiler) {
         Map<ResourceLocation, GeoModel> loaded = new HashMap<>();
         jsons.forEach((file, json) -> {
            if (file.getPath().endsWith(".geo")) {
               ResourceLocation id = file.withPath(file.getPath().substring(0, file.getPath().length() - 4));

               try {
                  loaded.put(id, GeoParser.parse(id, json.getAsJsonObject()));
               } catch (RuntimeException var5) {
                  DevilFruits.LOGGER.error("Invalid geo model {}: {}", id, var5.toString());
               }
            }
         });
         this.models = loaded;
         this.generation++;
         DevilFruits.LOGGER.debug("Loaded {} geo models", loaded.size());
      }
   }
}
