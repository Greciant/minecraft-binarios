package com.devilfruits.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;

public final class EntityShadows {
   private static final List<Predicate<Entity>> HIDDEN = new ArrayList<>();

   public static void hideWhen(Predicate<Entity> rule) {
      HIDDEN.add(rule);
   }

   public static boolean hidden(Entity entity) {
      for (Predicate<Entity> rule : HIDDEN) {
         if (rule.test(entity)) {
            return true;
         }
      }

      return false;
   }

   private EntityShadows() {
   }
}
