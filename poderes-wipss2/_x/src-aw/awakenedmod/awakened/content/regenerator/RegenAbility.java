package com.awakenedmod.awakened.content.regenerator;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.power.Ability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public abstract class RegenAbility extends Ability {
   protected RegenAbility(String id, float staminaCost, int unlockLevel) {
      super(id, staminaCost, unlockLevel);
   }

   protected static void clawHit(ServerPlayer player, LivingEntity target, float damage) {
      Dmg.hurt(target, player.serverLevel(), player.damageSources().playerAttack(player), damage);
      target.hurtMarked = true;
   }
}
