package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.RuleEngine;
import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class BleedsEffect implements Effect {
   private static final ResourceKey<DamageType> BLEED = ResourceKey.create(Registries.DAMAGE_TYPE, AwId.of("bleed"));
   private static final int INTERVAL_TICKS = 20;
   private static final int DRIP_TICKS = 5;
   private static final DustParticleOptions BLOOD = Fx.dust(11541294, 1.1F);

   @Override
   public String id() {
      return "bleeds";
   }

   @Override
   public int unlockLevel() {
      return 3;
   }

   @Override
   public double defaultWeight() {
      return 15.0;
   }

   @Override
   public boolean hasLevel() {
      return true;
   }

   @Override
   public String numberUnit() {
      return "awakened.command.unit.health_per_second";
   }

   @Override
   public boolean supports(TargetKind kind) {
      return kind != TargetKind.SELF;
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      if (target != caster && target.level() instanceof ServerLevel world) {
         Vec3 var8 = target.position().add(0.0, (double)target.getBbHeight() * 0.6, 0.0);
         if ((age + 1) % 20 != 0) {
            if (age % 5 == 0) {
               Fx.burst(world, BLOOD, var8, 1, (double)target.getBbWidth() * 0.3, 0.0);
            }

            return false;
         } else {
            boolean dealt = Dmg.hurtRapid(target, world, Dmg.by(caster, BLEED), RuleEngine.ruleDamage((float)Math.max(1, level)));
            if (dealt) {
               Fx.burst(world, BLOOD, var8, 4 + 2 * Math.max(1, level), (double)target.getBbWidth() * 0.35, 0.02);
            }

            return dealt;
         }
      } else {
         return false;
      }
   }
}
