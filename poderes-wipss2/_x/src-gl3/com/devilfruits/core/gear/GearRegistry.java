package com.devilfruits.core.gear;

import com.devilfruits.core.fruit.DevilFruit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public final class GearRegistry {
   private static final Map<ResourceLocation, List<GearDefinition>> SETS = new ConcurrentHashMap<>();

   public static void register(ResourceLocation fruit, GearDefinition... gears) {
      List<GearDefinition> list = new ArrayList<>(List.of(gears));
      list.sort(Comparator.comparingInt(GearDefinition::ordinal));
      Set<String> names = new HashSet<>();

      for (GearDefinition gear : list) {
         if (!names.add(gear.name())) {
            throw new IllegalStateException(fruit + " declares gear " + gear.name() + " twice");
         }

         for (GearDefinition sub : gear.subGears()) {
            if (!names.add(sub.name())) {
               throw new IllegalStateException(fruit + " declares gear " + sub.name() + " twice");
            }
         }
      }

      SETS.put(fruit, List.copyOf(list));
   }

   public static List<GearDefinition> of(@Nullable DevilFruit fruit) {
      return fruit == null ? List.of() : SETS.getOrDefault(fruit.id(), List.of());
   }

   public static boolean hasGears(@Nullable DevilFruit fruit) {
      return !of(fruit).isEmpty();
   }

   @Nullable
   public static GearDefinition find(@Nullable DevilFruit fruit, String name) {
      for (GearDefinition gear : of(fruit)) {
         if (gear.name().equals(name)) {
            return gear;
         }
      }

      return null;
   }

   private GearRegistry() {
   }
}
