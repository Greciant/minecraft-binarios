package com.devilfruits.client.animation;

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

public final class AnimationLoader extends SimpleJsonResourceReloadListener {
   public static final AnimationLoader INSTANCE = new AnimationLoader();
   private Map<ResourceLocation, PlayerAnimation> animations = Map.of();

   private AnimationLoader() {
      super(new Gson(), "player_animations");
   }

   protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler) {
      Map<ResourceLocation, PlayerAnimation> loaded = new HashMap<>();
      jsons.forEach((id, json) -> {
         try {
            loaded.put(id, PlayerAnimation.parse(json.getAsJsonObject()));
         } catch (RuntimeException var4x) {
            DevilFruits.LOGGER.error("Invalid player animation {}: {}", id, var4x.getMessage());
         }
      });
      this.animations = loaded;
      DevilFruits.LOGGER.debug("Loaded {} player animations", loaded.size());
   }

   @Nullable
   public PlayerAnimation get(ResourceLocation id) {
      return this.animations.get(id);
   }
}
