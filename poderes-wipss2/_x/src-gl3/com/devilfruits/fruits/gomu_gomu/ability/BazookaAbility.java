package com.devilfruits.fruits.gomu_gomu.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.fruits.gomu_gomu.GomuGomu;
import com.devilfruits.fruits.gomu_gomu.GomuSounds;
import com.devilfruits.fruits.gomu_gomu.strike.Breaker;
import com.devilfruits.fruits.gomu_gomu.strike.StrikeKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class BazookaAbility extends StrikeAbility {
   public BazookaAbility() {
      super(
         "bazooka",
         AbilityStats.builder()
            .energy(18)
            .cooldown(200)
            .mastery(3)
            .damage(18.0)
            .range(8.0)
            .knockback(3.0)
            .blockDamage(true)
            .extra("bazookaMaxHardness", 2.0, 0.0, 50.0, "The hardest block the palms smash through (2: wood, logs; stone is 1.5 but see the resistance).")
            .extra("bazookaMaxResistance", 3.0, 0.0, 1200.0, "The toughest block against blasts they smash through (3: wood; stone and cobblestone are 6).")
            .extra("bazookaMaxBlocks", 16.0, 0.0, 256.0, "Blocks one Bazooka breaks at most.")
            .build()
      );
   }

   @Override
   protected Breaker breaker(AbilityContext ctx) {
      return new Breaker(ctx.extra("bazookaMaxHardness"), ctx.extra("bazookaMaxResistance"), 0.9, (int)ctx.extra("bazookaMaxBlocks"));
   }

   @Override
   protected StrikeKind kind() {
      return StrikeKind.BAZOOKA;
   }

   @Override
   protected float ratio() {
      return 3.0F;
   }

   @Override
   protected ResourceLocation clip(int[] arms) {
      return GomuGomu.id("bazooka");
   }

   @Override
   protected SoundEvent hitSound() {
      return GomuSounds.BAZOOKA_HIT.get();
   }
}
