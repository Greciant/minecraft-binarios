package com.awakenedmod.awakened.core.power;

import com.awakenedmod.awakened.Awakened;
import com.awakenedmod.awakened.core.rules.ServerRules;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;

public final class PowerRegistry {
   private static final Map<String, Power> POWERS = new LinkedHashMap<>();

   private PowerRegistry() {
   }

   public static void register(Power power) {
      if (POWERS.putIfAbsent(power.id(), power) != null) {
         throw new IllegalStateException("Duplicate power id: " + power.id());
      } else {
         Awakened.LOGGER.debug("Registered power '{}'", power.id());
      }
   }

   public static Power get(String id) {
      return POWERS.get(id);
   }

   public static Collection<Power> all() {
      return Collections.unmodifiableCollection(POWERS.values());
   }

   public static boolean isEmpty() {
      return POWERS.isEmpty();
   }

   public static List<Power> rollable() {
      return POWERS.values().stream().filter(PowerRegistry::obtainable).toList();
   }

   public static boolean obtainable(Power power) {
      return power.rollable() && !ServerRules.powerDisabled(power.id());
   }

   public static Power roll(RandomSource random) {
      List<Power> values = rollable();
      if (values.isEmpty()) {
         throw new IllegalStateException("No obtainable powers to roll from");
      } else {
         return values.get(random.nextInt(values.size()));
      }
   }
}
