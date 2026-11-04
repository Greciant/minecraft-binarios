package com.devilfruits.core.ability;

import com.devilfruits.core.fruit.DevilFruit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public final class AbilityRegistry {
   private static final Map<ResourceLocation, Ability> BY_ID = new HashMap<>();
   private static final Map<ResourceLocation, DevilFruit> OWNERS = new HashMap<>();
   private static final List<Ability> ORDERED = new ArrayList<>();
   private static final List<Ability> VIEW = Collections.unmodifiableList(ORDERED);

   public static void registerAll(DevilFruit fruit) {
      for (Ability ability : fruit.abilities()) {
         DevilFruit owner = OWNERS.get(ability.id());
         if (owner != null) {
            throw new IllegalStateException("Ability " + ability.id() + " already belongs to " + owner.id());
         }

         BY_ID.put(ability.id(), ability);
         OWNERS.put(ability.id(), fruit);
         ORDERED.add(ability);
      }
   }

   @Nullable
   public static Ability get(@Nullable ResourceLocation id) {
      return id == null ? null : BY_ID.get(id);
   }

   @Nullable
   public static DevilFruit owner(Ability ability) {
      return OWNERS.get(ability.id());
   }

   public static List<Ability> all() {
      return VIEW;
   }

   public static int iconIndex(@Nullable ResourceLocation id) {
      Ability ability = get(id);
      return ability == null ? 0 : ORDERED.indexOf(ability) + 1;
   }

   private AbilityRegistry() {
   }
}
