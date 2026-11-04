package com.awakenedmod.awakened.content;

import com.awakenedmod.awakened.core.relic.Condition;
import com.awakenedmod.awakened.core.relic.Relic;
import com.awakenedmod.awakened.core.relic.RelicRegistry;
import com.awakenedmod.awakened.core.skill.Stat;
import net.minecraft.world.item.Rarity;

public final class AwakenedRelics {
   public static final String WARDENS_EYE = "wardens_eye";
   public static final String SCALES_OF_JUDGMENT = "scales_of_judgment";
   public static final String FANGBINDER_SIGIL = "fangbinder_sigil";
   public static final String TIDECALLER_PEARL = "tidecaller_pearl";
   public static final String DUELISTS_EDGE = "duelists_edge";
   public static final float HEALTHY_FRACTION = 0.75F;
   private static final Condition WHILE_HEALTHY = Condition.gate("while_healthy", ctx -> ctx.healthFraction() >= 0.75F);
   private static final Condition WHEN_WOUNDED = Condition.scale("when_wounded", ctx -> (double)ctx.missingHealthFraction());
   private static final Condition WHILE_IN_WATER = Condition.gate("while_in_water", ctx -> ctx.inWater());
   private static final Condition WHILE_SPRINTING = Condition.gate("while_sprinting", ctx -> ctx.sprinting());

   private AwakenedRelics() {
   }

   public static void registerAll() {
      RelicRegistry.register(Relic.builder("iron_charm").rarity(Rarity.COMMON).color(-5197640).stat(Stat.MAX_HEALTH, 2.0F).build());
      RelicRegistry.register(Relic.builder("swift_cells").rarity(Rarity.COMMON).color(-7350529).stat(Stat.MAX_STAMINA, 20.0F).build());
      RelicRegistry.register(Relic.builder("warding_plate").rarity(Rarity.UNCOMMON).color(-2065798).stat(Stat.MAX_HEALTH, 4.0F).build());
      RelicRegistry.register(Relic.builder("kinetic_band").rarity(Rarity.UNCOMMON).color(-8395824).stat(Stat.STAMINA_REGEN, 3.0F).build());
      RelicRegistry.register(Relic.builder("focus_lens").rarity(Rarity.UNCOMMON).color(-4944897).stat(Stat.ABILITY_POWER, 0.08F).build());
      RelicRegistry.register(Relic.builder("vital_core").rarity(Rarity.EPIC).color(-1750693).stat(Stat.MAX_HEALTH, 6.0F).build());
      RelicRegistry.register(Relic.builder("stamina_matrix").rarity(Rarity.RARE).color(-13056257).stat(Stat.MAX_STAMINA, 40.0F).build());
      RelicRegistry.register(Relic.builder("flux_conduit").rarity(Rarity.RARE).color(-10034945).stat(Stat.STAMINA_REGEN, 6.0F).build());
      RelicRegistry.register(Relic.builder("power_prism").rarity(Rarity.EPIC).color(-3703297).stat(Stat.ABILITY_POWER, 0.15F).build());
      RelicRegistry.register(Relic.builder("titans_heart").rarity(Rarity.EPIC).color(-21965).stat(Stat.MAX_HEALTH, 4.0F).stat(Stat.ABILITY_POWER, 0.1F).build());
      RelicRegistry.register(
         Relic.builder("wardens_eye").signature().rarity(Rarity.EPIC).color(-10237976).stat(Stat.ABILITY_POWER, 0.22F, WHILE_HEALTHY).build()
      );
      RelicRegistry.register(
         Relic.builder("scales_of_judgment")
            .signature()
            .rarity(Rarity.EPIC)
            .color(-1522617)
            .stat(Stat.ABILITY_POWER, 0.3F)
            .stat(Stat.MAX_HEALTH, -4.0F)
            .build()
      );
      RelicRegistry.register(
         Relic.builder("fangbinder_sigil").signature().rarity(Rarity.EPIC).color(-7346512).stat(Stat.ABILITY_POWER, 0.4F, WHEN_WOUNDED).build()
      );
      RelicRegistry.register(
         Relic.builder("tidecaller_pearl")
            .signature()
            .rarity(Rarity.EPIC)
            .color(-13726762)
            .stat(Stat.MAX_STAMINA, 30.0F)
            .stat(Stat.STAMINA_REGEN, 2.0F)
            .stat(Stat.STAMINA_REGEN, 8.0F, WHILE_IN_WATER)
            .build()
      );
      RelicRegistry.register(
         Relic.builder("duelists_edge")
            .signature()
            .rarity(Rarity.EPIC)
            .color(-2602945)
            .stat(Stat.STAMINA_REGEN, 2.0F)
            .stat(Stat.ABILITY_POWER, 0.25F, WHILE_SPRINTING)
            .build()
      );
   }
}
