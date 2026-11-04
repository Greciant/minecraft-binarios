package com.devilfruits.core.haki;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;

public final class HakiFormParts {
   private static final Map<String, Map<Integer, List<String>>> FORMS = new HashMap<>();

   public static void register(ResourceLocation fruit, String form, Map<Integer, List<String>> parts) {
      FORMS.put(key(fruit, form), Map.copyOf(parts));
   }

   public static boolean has(ResourceLocation fruit, String form) {
      return FORMS.containsKey(key(fruit, form));
   }

   public static List<String> parts(ResourceLocation fruit, String form, int mask) {
      Map<Integer, List<String>> parts = FORMS.get(key(fruit, form));
      if (parts == null) {
         return List.of();
      } else {
         Set<String> out = new LinkedHashSet<>();

         for (int i = 0; i < 9; i++) {
            int bit = 1 << i;
            if ((mask & bit) != 0) {
               out.addAll(parts.getOrDefault(bit, List.of()));
            }
         }

         return new ArrayList<>(out);
      }
   }

   public static int support(ResourceLocation fruit, String form) {
      Map<Integer, List<String>> parts = FORMS.get(key(fruit, form));
      int bits = 0;
      if (parts != null) {
         for (Entry<Integer, List<String>> e : parts.entrySet()) {
            if (!e.getValue().isEmpty()) {
               bits |= e.getKey();
            }
         }
      }

      return bits;
   }

   private static String key(ResourceLocation fruit, String form) {
      return fruit + "#" + form;
   }

   private HakiFormParts() {
   }
}
