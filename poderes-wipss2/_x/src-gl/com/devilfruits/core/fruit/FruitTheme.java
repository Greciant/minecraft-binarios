package com.devilfruits.core.fruit;

import com.devilfruits.DevilFruits;
import net.minecraft.resources.ResourceLocation;

public record FruitTheme(
   ResourceLocation energyBackground,
   ResourceLocation energyProgress,
   ResourceLocation energyBackgroundVertical,
   ResourceLocation energyProgressVertical,
   ResourceLocation chargeBackground,
   ResourceLocation chargeProgress,
   ResourceLocation selection,
   ResourceLocation activeFrame,
   int nameColor,
   int energyTextColor,
   int masteryColor,
   int maxMasteryColor,
   int costColor,
   int chargeGlowColor
) {
   public static final FruitTheme DEFAULT = sprites("").colors(16777215, 15918003, 15909955, 16766282, 15918003, 16773570);

   public static FruitTheme sprites(String folder) {
      String prefix = folder.isEmpty() ? "hud/" : "hud/" + folder + "/";
      return new FruitTheme(
         DevilFruits.id(prefix + "energy_background"),
         DevilFruits.id(prefix + "energy_progress"),
         DevilFruits.id(prefix + "energy_background_vertical"),
         DevilFruits.id(prefix + "energy_progress_vertical"),
         DevilFruits.id(prefix + "charge_background"),
         DevilFruits.id(prefix + "charge_progress"),
         DevilFruits.id(prefix + "ability_selection"),
         DevilFruits.id(prefix + "ability_active"),
         16777215,
         15918003,
         15909955,
         16766282,
         15918003,
         16773570
      );
   }

   public FruitTheme colors(int name, int energyText, int mastery, int maxMastery, int cost, int chargeGlow) {
      return new FruitTheme(
         this.energyBackground,
         this.energyProgress,
         this.energyBackgroundVertical,
         this.energyProgressVertical,
         this.chargeBackground,
         this.chargeProgress,
         this.selection,
         this.activeFrame,
         name,
         energyText,
         mastery,
         maxMastery,
         cost,
         chargeGlow
      );
   }
}
