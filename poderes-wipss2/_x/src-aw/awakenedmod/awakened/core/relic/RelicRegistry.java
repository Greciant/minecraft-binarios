package com.awakenedmod.awakened.core.relic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RelicRegistry {
   private static final Map<String, Relic> RELICS = new LinkedHashMap<>();

   private RelicRegistry() {
   }

   public static void register(Relic relic) {
      if (RELICS.putIfAbsent(relic.id(), relic) != null) {
         throw new IllegalStateException("Duplicate relic id: " + relic.id());
      }
   }

   public static Relic get(String id) {
      return id == null ? null : RELICS.get(id);
   }

   public static Collection<Relic> all() {
      return RELICS.values();
   }

   public static List<String> ids() {
      return new ArrayList<>(RELICS.keySet());
   }

   public static boolean isEmpty() {
      return RELICS.isEmpty();
   }

   public static int count() {
      return RELICS.size();
   }
}
