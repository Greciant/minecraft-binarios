package com.awakenedmod.awakened.core.rules;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.team.EntityOwners;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class PvpRules {
   private PvpRules() {
   }

   static void register(IEventBus gameBus) {
      gameBus.addListener(PvpRules::onIncomingDamage);
   }

   private static void onIncomingDamage(LivingIncomingDamageEvent event) {
      event.setAmount(scaled(event.getEntity(), event.getSource(), event.getAmount()));
   }

   public static float scaled(Entity victim, DamageSource source, float amount) {
      double multiplier = ServerRules.pvpDamage();
      return multiplier != 1.0 && victim instanceof Player && Dmg.inAbilityHit() && source.getEntity() instanceof Player attacker && attacker != victim
         ? (float)((double)amount * multiplier)
         : amount;
   }

   public static boolean blocksControl(@Nullable Entity source, Entity target) {
      if (ServerRules.pvpHostileEffects() || !(target instanceof Player)) {
         return false;
      } else if (source == null) {
         return true;
      } else {
         UUID owner = EntityOwners.ownerOf(source);
         return owner != null && !owner.equals(target.getUUID());
      }
   }

   public static boolean blocksEffect(@Nullable Entity source, Entity target, MobEffectInstance effect) {
      return ((MobEffect)effect.getEffect().value()).getCategory() == MobEffectCategory.HARMFUL && blocksControl(source, target);
   }

   public static boolean byAnotherPlayer(@Nullable Entity attacker, Player victim) {
      UUID owner = attacker == null ? null : EntityOwners.ownerOf(attacker);
      return owner != null && !owner.equals(victim.getUUID());
   }
}
