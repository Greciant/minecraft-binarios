package com.devilfruits.core.config;

import com.devilfruits.core.form.FormAttribute;
import com.devilfruits.core.form.FormStats;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class FormConfig {
   @Nullable
   private final IntValue requiredMastery;
   @Nullable
   private final IntValue enterEnergyCost;
   @Nullable
   private final DoubleValue energyDrainPerSecond;
   @Nullable
   private final IntValue switchCooldownTicks;
   @Nullable
   private final IntValue castTicks;
   @Nullable
   private final DoubleValue regenMultiplier;
   @Nullable
   private final DoubleValue boostDrainPerSecond;
   @Nullable
   private final IntValue timeLimitSeconds;
   @Nullable
   private final BooleanValue freeAbilities;
   private final Map<Holder<Attribute>, DoubleValue> attributes = new HashMap<>();

   FormConfig(Builder b, FormStats d, boolean base, boolean boostable, List<FormAttribute> formAttributes, int timeLimit, boolean free) {
      if (!formAttributes.isEmpty()) {
         b.comment("Attribute modifiers of the form (amounts; the operation is fixed by the fruit).").push("attributes");

         for (FormAttribute attribute : formAttributes) {
            this.attributes.put(attribute.attribute(), b.defineInRange(attributeKey(attribute.attribute()), attribute.amount(), -1000.0, 1000.0));
         }

         b.pop();
      }

      this.regenMultiplier = b.comment("Factor on energy regeneration while in this form.").defineInRange("regenMultiplier", d.regenMultiplier(), 0.0, 10.0);
      if (base) {
         this.requiredMastery = null;
         this.enterEnergyCost = null;
         this.energyDrainPerSecond = null;
         this.castTicks = null;
         this.boostDrainPerSecond = null;
         this.timeLimitSeconds = null;
         this.freeAbilities = null;
         this.switchCooldownTicks = b.comment("Cooldown in ticks after returning to the base form before transforming again.")
            .defineInRange("switchCooldownTicks", d.switchCooldownTicks(), 0, 72000);
      } else {
         this.requiredMastery = b.comment("Mastery level (1-10) required to enter this form.").defineInRange("requiredMastery", d.requiredMastery(), 1, 10);
         this.enterEnergyCost = b.comment("Energy paid to enter this form.").defineInRange("enterEnergyCost", d.enterEnergyCost(), 0, 10000);
         this.energyDrainPerSecond = b.comment("Energy drained per second while in this form.")
            .defineInRange("energyDrainPerSecond", d.energyDrainPerSecond(), 0.0, 1000.0);
         this.switchCooldownTicks = b.comment("Cooldown in ticks after entering this form before the next voluntary switch.")
            .defineInRange("switchCooldownTicks", d.switchCooldownTicks(), 0, 72000);
         this.castTicks = b.comment("Wind-up in ticks before a voluntary switch into this form completes (0 = instant).")
            .defineInRange("castTicks", d.castTicks(), 0, 600);
         this.boostDrainPerSecond = boostable
            ? b.comment("Extra energy drained per second while the boost key is held.")
               .defineInRange("boostDrainPerSecond", d.boostDrainPerSecond(), 0.0, 1000.0)
            : null;
         this.timeLimitSeconds = b.comment(
               "Seconds a full energy bar lasts in this form: it then drains max energy / this each second, whatever the mastery, instead of energyDrainPerSecond and boostDrainPerSecond (0 = those drains)."
            )
            .defineInRange("timeLimitSeconds", timeLimit, 0, 3600);
         this.freeAbilities = b.comment("Techniques cost no energy in this form (the form itself spends it).").define("freeAbilities", free);
      }
   }

   private FormConfig() {
      this.requiredMastery = null;
      this.enterEnergyCost = null;
      this.energyDrainPerSecond = null;
      this.switchCooldownTicks = null;
      this.castTicks = null;
      this.regenMultiplier = null;
      this.boostDrainPerSecond = null;
      this.timeLimitSeconds = null;
      this.freeAbilities = null;
   }

   static FormConfig base() {
      return new FormConfig();
   }

   public int requiredMastery() {
      return this.requiredMastery == null ? 1 : (Integer)this.requiredMastery.get();
   }

   public int enterEnergyCost() {
      return this.enterEnergyCost == null ? 0 : (Integer)this.enterEnergyCost.get();
   }

   public double energyDrainPerSecond() {
      return this.energyDrainPerSecond == null ? 0.0 : (Double)this.energyDrainPerSecond.get();
   }

   public int switchCooldownTicks() {
      return this.switchCooldownTicks == null ? 0 : (Integer)this.switchCooldownTicks.get();
   }

   public int castTicks() {
      return this.castTicks == null ? 0 : (Integer)this.castTicks.get();
   }

   public double regenMultiplier() {
      return this.regenMultiplier == null ? 1.0 : (Double)this.regenMultiplier.get();
   }

   public double boostDrainPerSecond() {
      return this.boostDrainPerSecond == null ? 0.0 : (Double)this.boostDrainPerSecond.get();
   }

   public int timeLimitSeconds() {
      return this.timeLimitSeconds == null ? 0 : (Integer)this.timeLimitSeconds.get();
   }

   public boolean freeAbilities() {
      return this.freeAbilities != null && (Boolean)this.freeAbilities.get();
   }

   public double attribute(Holder<Attribute> attribute, double fallback) {
      DoubleValue value = this.attributes.get(attribute);
      return value == null ? fallback : (Double)value.get();
   }

   public static String attributeKey(Holder<Attribute> attribute) {
      ResourceLocation id = ((ResourceKey)attribute.unwrapKey().orElseThrow()).location();
      String path = id.getPath().replace('.', '_').replace('/', '_');
      return id.getNamespace().equals("minecraft") ? path : id.getNamespace() + "_" + path;
   }
}
