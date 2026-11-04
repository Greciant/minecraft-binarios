package com.devilfruits.core.logia;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class LogiaInteractionRegistry {
   private static final Map<ResourceLocation, Map<ResourceLocation, LogiaInteractionRegistry.Interaction>> RULES = new HashMap<>();

   public static synchronized void register(ResourceLocation attacker, ResourceLocation defender, LogiaInteractionRegistry.Interaction interaction) {
      RULES.computeIfAbsent(attacker, k -> new HashMap<>()).put(defender, interaction);
   }

   public static synchronized void unregister(ResourceLocation attacker, ResourceLocation defender) {
      Map<ResourceLocation, LogiaInteractionRegistry.Interaction> map = RULES.get(attacker);
      if (map != null) {
         map.remove(defender);
      }
   }

   public static synchronized LogiaInteractionRegistry.Interaction get(ResourceLocation attacker, ResourceLocation defender) {
      Map<ResourceLocation, LogiaInteractionRegistry.Interaction> map = RULES.get(attacker);
      LogiaInteractionRegistry.Interaction interaction = map == null ? null : map.get(defender);
      return interaction == null ? LogiaInteractionRegistry.Interaction.NEUTRAL : interaction;
   }

   private LogiaInteractionRegistry() {
   }

   public static enum Interaction {
      BYPASS,
      NEUTRAL,
      RESISTED;
   }
}
