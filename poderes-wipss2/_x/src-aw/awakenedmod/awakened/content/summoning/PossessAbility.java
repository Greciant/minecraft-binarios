package com.awakenedmod.awakened.content.summoning;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.power.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;

public final class PossessAbility extends Ability {
   private static final double AIM_RANGE = 48.0;

   public PossessAbility() {
      super("possess", 25.0F, 10);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (PossessControl.isPiloting(player)) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         Mob minion = MinionSupport.aimedMinion(player, 48.0);
         if (minion == null) {
            minion = MinionSupport.nearestMinion(player);
         }

         if (minion == null) {
            return this.fail(player, "awakened.summon.no_minions", new Object[0]);
         } else {
            ServerLevel var4 = player.serverLevel();
            if (var4 instanceof ServerLevel) {
               Fx.burst(var4, ParticleTypes.SCULK_SOUL, player.position().add(0.0, 1.0, 0.0), 14, 0.3, 0.02);
               Fx.sound(var4, player.position(), (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.7F, 0.9F);
            }

            PossessControl.begin(player, minion);
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      PossessControl.tick(player);
   }

   @Override
   public boolean isActive(ServerPlayer player) {
      return PossessControl.isPiloting(player);
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      PossessControl.end(player, true);
      super.clearPlayer(player);
   }
}
