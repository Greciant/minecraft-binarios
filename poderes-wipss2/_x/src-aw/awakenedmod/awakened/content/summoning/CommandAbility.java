package com.awakenedmod.awakened.content.summoning;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.power.Ability;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class CommandAbility extends Ability {
   private static final double AIM_RANGE = 40.0;
   private static final double FOE_CONE_DEG = 12.0;

   public CommandAbility() {
      super("command", 6.0F, 3);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      List<Mob> minions = MinionSupport.minionsOf(player);
      if (minions.isEmpty()) {
         return this.fail(player, "awakened.summon.no_minions", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         LivingEntity foe = AimUtil.aimedLivingCone(
            player,
            40.0,
            12.0,
            e -> e != player && Harm.allowed(player, e) && (!(e instanceof Mob mx) || !SummonAttachments.minion(mx).isOwnedBy(player.getUUID()))
         );
         if (foe != null) {
            for (Mob m : minions) {
               MinionSupport.setHolding(m, false);
               if (MinionSupport.canPressAttack(m)) {
                  m.setTarget(foe);
               }
            }

            SummonOrders.attack(player, foe);
            this.marker(level, foe.position().add(0.0, (double)foe.getBbHeight() * 0.5, 0.0), true);
            Fx.sound(level, player.position(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.4F, 1.6F);
         } else {
            Vec3 spot = MinionSupport.groundSpot(player, 40.0);

            for (Mob mx : minions) {
               mx.setTarget(null);
               MinionSupport.setHolding(mx, spot);
            }

            SummonOrders.hold(player, spot);
            this.marker(level, spot.add(0.0, 0.1, 0.0), false);
            Fx.sound(level, player.position(), (SoundEvent)SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.PLAYERS, 0.6F, 0.6F);
         }

         return true;
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      SummonOrders.clear(player);
      super.clearPlayer(player);
   }

   private void marker(ServerLevel level, Vec3 at, boolean attack) {
      if (attack) {
         Fx.burst(level, ParticleTypes.SCULK_SOUL, at, 16, 0.3, 0.02);
         Fx.burst(level, ParticleTypes.SOUL_FIRE_FLAME, at, 8, 0.25, 0.02);
      } else {
         Fx.disk(level, ParticleTypes.SCULK_SOUL, at, 1.4);
      }
   }
}
