package com.devilfruits.core.config;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class FruitConfigs {
   private static final Map<ResourceLocation, FruitConfig> FRUITS = new HashMap<>();
   private static final Map<ResourceLocation, AbilityConfig> ABILITIES = new HashMap<>();

   public static void register(DevilFruit fruit, ModContainer container) {
      FruitConfig config = new FruitConfig(fruit);
      FRUITS.put(fruit.id(), config);
      ABILITIES.putAll(config.abilities);
      String file = container.getModId() + "-" + fruit.id().getPath().replace('/', '-') + "-common.toml";
      container.registerConfig(Type.COMMON, config.spec(), file);
   }

   public static FruitConfig fruit(DevilFruit fruit) {
      FruitConfig config = FRUITS.get(fruit.id());
      if (config == null) {
         throw new IllegalStateException("No config registered for " + fruit.id() + " (use FruitModuleContext#registerFruit)");
      } else {
         return config;
      }
   }

   public static FormConfig form(FruitForm form) {
      return fruit(form.fruit()).forms.get(form.name());
   }

   public static AbilityConfig ability(Ability ability) {
      AbilityConfig config = ABILITIES.get(ability.id());
      if (config == null) {
         throw new IllegalStateException("No config for ability " + ability.id());
      } else {
         return config;
      }
   }

   public static void upgrade(Object spec) {
      for (FruitConfig config : FRUITS.values()) {
         if (config.spec() == spec) {
            config.upgrade();
         }
      }
   }

   public static boolean isFruitSpec(Object spec) {
      for (FruitConfig config : FRUITS.values()) {
         if (config.spec() == spec) {
            return true;
         }
      }

      return spec instanceof ModConfigSpec && spec == CoreConfig.SPEC;
   }

   private FruitConfigs() {
   }
}
