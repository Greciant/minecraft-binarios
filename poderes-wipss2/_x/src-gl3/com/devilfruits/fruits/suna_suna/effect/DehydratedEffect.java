package com.devilfruits.fruits.suna_suna.effect;

import com.devilfruits.fruits.suna_suna.SunaParticles;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import com.devilfruits.registry.DFDamageTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;

public class DehydratedEffect extends MobEffect {
   public DehydratedEffect() {
      super(MobEffectCategory.HARMFUL, 13215850);
      this.addAttributeModifier(Attributes.MOVEMENT_SPEED, SunaSuna.id("dehydrated_speed"), -0.06, Operation.ADD_MULTIPLIED_TOTAL);
      this.addAttributeModifier(Attributes.ATTACK_DAMAGE, SunaSuna.id("dehydrated_attack"), -1.0, Operation.ADD_VALUE);
   }

   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return duration % 40 == 0;
   }

   public boolean applyEffectTick(LivingEntity entity, int amplifier) {
      if (entity instanceof Player player) {
         player.causeFoodExhaustion(1.0F * (float)(amplifier + 1));
      }

      if (entity.getType().is(EntityTypeTags.AQUATIC)) {
         entity.hurt(DFDamageTypes.source(entity.level(), SunaSuna.DEHYDRATION_DAMAGE, null), 1.0F + (float)amplifier);
      }

      return true;
   }

   public ParticleOptions createParticleOptions(MobEffectInstance effect) {
      return (ParticleOptions)SunaParticles.DEHYDRATED.get();
   }
}
