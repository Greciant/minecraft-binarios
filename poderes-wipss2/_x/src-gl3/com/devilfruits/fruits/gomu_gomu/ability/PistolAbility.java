package com.devilfruits.fruits.gomu_gomu.ability;

import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.fruits.gomu_gomu.GomuSounds;
import com.devilfruits.fruits.gomu_gomu.strike.StrikeKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class PistolAbility extends StrikeAbility {
   public PistolAbility() {
      super("pistol", AbilityStats.builder().energy(8).cooldown(100).mastery(1).damage(6.0).range(10.0).knockback(1.6).build());
   }

   @Override
   protected StrikeKind kind() {
      return StrikeKind.PISTOL;
   }

   @Override
   protected float ratio() {
      return 1.0F;
   }

   @Override
   protected ResourceLocation clip(int[] arms) {
      return sided("pistol", arms);
   }

   @Override
   protected SoundEvent hitSound() {
      return GomuSounds.PUNCH_HIT.get();
   }
}
