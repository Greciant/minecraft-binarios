package com.devilfruits.core.config;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.EnergyRules;
import com.devilfruits.core.gacha.GachaTier;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class FruitConfig {
   private final ModConfigSpec spec;
   private final BooleanValue enabled;
   private final IntValue lootWeight;
   private final EnumValue<GachaTier> gachaTier;
   private final IntValue configRevision;
   private final DevilFruit fruit;
   private final IntValue baseEnergy;
   private final IntValue energyPerLevel;
   private final DoubleValue regenInCombat;
   private final DoubleValue regenOutOfCombat;
   private final DoubleValue xpMultiplier;
   final Map<String, FormConfig> forms = new LinkedHashMap<>();
   private final Map<String, DoubleValue> settings = new LinkedHashMap<>();
   private final Map<String, BooleanValue> flags = new LinkedHashMap<>();
   final Map<ResourceLocation, AbilityConfig> abilities = new LinkedHashMap<>();
   @Nullable
   private final LogiaFruitConfig logia;

   FruitConfig(DevilFruit fruit) {
      this.fruit = fruit;
      Builder b = new Builder();
      EnergyRules energy = fruit.energyDefaults();
      b.comment("Availability.").push("fruit");
      this.enabled = b.comment("Whether a casket can roll this fruit and it can be eaten. Existing users keep their power.").define("enabled", true);
      this.gachaTier = b.comment("Rarity when a Devil Fruit Casket opens: LEGENDARY, RARE, UNCOMMON or COMMON (tier weights: devilfruits-common.toml [gacha]).")
         .defineEnum("gachaTier", fruit.defaultGachaTier());
      this.lootWeight = b.comment("Relative weight among the fruits of the same gacha tier when a casket opens (0 = never in a casket).")
         .defineInRange("lootWeight", fruit.defaultLootWeight(), 0, 10000);
      this.configRevision = b.comment(
            new String[]{
               "Balance updates this file has received (the mod raises it itself when it resets values whose defaults",
               "changed; lower it to take an update's new defaults again)."
            }
         )
         .defineInRange("configRevision", 0, 0, 10000);
      b.pop();
      b.comment("Energy.").push("energy");
      this.baseEnergy = b.comment("Maximum energy at mastery 1.").defineInRange("baseEnergy", energy.baseEnergy(), 10, 10000);
      this.energyPerLevel = b.comment("Additional maximum energy per mastery level above 1.").defineInRange("energyPerLevel", energy.energyPerLevel(), 0, 1000);
      this.regenInCombat = b.comment("Energy regenerated per second while in combat.").defineInRange("regenInCombat", energy.regenInCombat(), 0.0, 1000.0);
      this.regenOutOfCombat = b.comment("Energy regenerated per second out of combat.")
         .defineInRange("regenOutOfCombat", energy.regenOutOfCombat(), 0.0, 1000.0);
      this.xpMultiplier = b.comment("Multiplier for this fruit's mastery XP (on top of the global one).").defineInRange("xpMultiplier", 1.0, 0.0, 100.0);
      b.pop();
      if (!fruit.settings().isEmpty()) {
         b.comment("Fruit-wide settings.").push("settings");

         for (DevilFruit.Setting setting : fruit.settings()) {
            if (setting.flag()) {
               this.flags.put(setting.name(), b.comment(setting.comment()).define(setting.name(), setting.defaultValue() != 0.0));
            } else {
               this.settings
                  .put(setting.name(), b.comment(setting.comment()).defineInRange(setting.name(), setting.defaultValue(), setting.min(), setting.max()));
            }
         }

         b.pop();
      }

      this.logia = fruit.logia() == null ? null : new LogiaFruitConfig(b, fruit.logia());
      if (fruit.hasForms()) {
         b.comment("Forms.").push("forms");

         for (FruitForm form : fruit.forms()) {
            b.push(form.name());
            this.forms
               .put(
                  form.name(),
                  new FormConfig(b, form.defaults(), form.isBase(), form.boostable(), form.attributes(), form.timeLimitSeconds(), form.freeAbilities())
               );
            b.pop();
         }

         b.pop();
      }

      b.comment("Per-ability balance.").push("abilities");

      for (Ability ability : fruit.abilities()) {
         b.push(ability.name());
         this.abilities.put(ability.id(), new AbilityConfig(b, ability.defaults()));
         b.pop();
      }

      b.pop();
      this.spec = b.build();
      if (!fruit.hasForms()) {
         this.forms.put(fruit.baseForm().name(), FormConfig.base());
      }
   }

   public ModConfigSpec spec() {
      return this.spec;
   }

   void upgrade() {
      int have = (Integer)this.configRevision.get();
      int want = this.fruit.configRevision();
      if (have < want) {
         for (DevilFruit.ConfigUpdate update : this.fruit.configUpdates()) {
            if (update.revision() > have) {
               for (String path : update.paths()) {
                  if (this.spec.getValues().get(path) instanceof ConfigValue<?> value) {
                     reset(value);
                  } else {
                     DevilFruits.LOGGER.warn("{}: no config key {} for update {}", new Object[]{this.fruit.id(), path, update.revision()});
                  }
               }
            }
         }

         this.configRevision.set(want);
         this.spec.save();
         DevilFruits.LOGGER.info("{}: config updated from revision {} to {}", new Object[]{this.fruit.id(), have, want});
      }
   }

   private static <T> void reset(ConfigValue<T> value) {
      value.set(value.getDefault());
   }

   public boolean enabled() {
      return (Boolean)this.enabled.get();
   }

   public int lootWeight() {
      return (Integer)this.lootWeight.get();
   }

   public GachaTier gachaTier() {
      return (GachaTier)this.gachaTier.get();
   }

   public int baseEnergy() {
      return (Integer)this.baseEnergy.get();
   }

   public int energyPerLevel() {
      return (Integer)this.energyPerLevel.get();
   }

   public double regenInCombat() {
      return (Double)this.regenInCombat.get();
   }

   public double regenOutOfCombat() {
      return (Double)this.regenOutOfCombat.get();
   }

   public double xpMultiplier() {
      return (Double)this.xpMultiplier.get();
   }

   @Nullable
   public LogiaFruitConfig logia() {
      return this.logia;
   }

   public double setting(String name) {
      DoubleValue value = this.settings.get(name);
      if (value == null) {
         throw new IllegalArgumentException("Unknown fruit setting: " + name);
      } else {
         return (Double)value.get();
      }
   }

   public boolean flag(String name) {
      BooleanValue value = this.flags.get(name);
      if (value == null) {
         throw new IllegalArgumentException("Unknown fruit flag: " + name);
      } else {
         return (Boolean)value.get();
      }
   }
}
