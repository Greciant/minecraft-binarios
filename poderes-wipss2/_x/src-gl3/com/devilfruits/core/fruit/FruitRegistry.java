package com.devilfruits.core.fruit;

import com.devilfruits.core.ability.AbilityRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class FruitRegistry {
   private static final Map<ResourceLocation, DevilFruit> FRUITS = new LinkedHashMap<>();
   private static final Collection<DevilFruit> VIEW = Collections.unmodifiableCollection(FRUITS.values());
   private static boolean frozen;

   public static synchronized DevilFruit register(DevilFruit fruit) {
      if (frozen) {
         throw new IllegalStateException("Devil Fruits must be registered during mod construction: " + fruit.id());
      } else if (FRUITS.containsKey(fruit.id())) {
         throw new IllegalStateException("Duplicate Devil Fruit " + fruit.id());
      } else {
         AbilityRegistry.registerAll(fruit);
         FRUITS.put(fruit.id(), fruit);
         return fruit;
      }
   }

   @Nullable
   public static DevilFruit get(@Nullable ResourceLocation id) {
      return id == null ? null : FRUITS.get(id);
   }

   public static Optional<DevilFruit> find(@Nullable ResourceLocation id) {
      return Optional.ofNullable(get(id));
   }

   public static Collection<DevilFruit> all() {
      return VIEW;
   }

   public static List<DevilFruit> playable() {
      return FRUITS.values().stream().filter(f -> !f.isDevOnly()).toList();
   }

   public static Collection<ResourceLocation> ids() {
      return Collections.unmodifiableSet(FRUITS.keySet());
   }

   @Nullable
   public static DevilFruit byItem(Item item) {
      for (DevilFruit fruit : FRUITS.values()) {
         if (fruit.hasItem() && fruit.item().orElse(null) == item) {
            return fruit;
         }
      }

      return null;
   }

   public static void freeze() {
      frozen = true;
   }

   public static boolean isFrozen() {
      return frozen;
   }

   private FruitRegistry() {
   }
}
