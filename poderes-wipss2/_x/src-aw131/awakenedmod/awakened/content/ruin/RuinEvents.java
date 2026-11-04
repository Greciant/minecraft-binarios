package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.core.Dmg;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class RuinEvents {
   private RuinEvents() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(RuinEvents::onDamageLanded);
      gameBus.addListener(EventPriority.LOWEST, RuinEvents::onDeath);
      gameBus.addListener(RuinEvents::onServerTick);
      gameBus.addListener(RuinEvents::onServerStopped);
      RuinFallGuard.register(gameBus);
   }

   private static void onDamageLanded(Post event) {
      if (!(event.getNewDamage() <= 0.0F) && !Dmg.inAbilityHit()) {
         ServerPlayer player = melee(event.getSource());
         LivingEntity victim = event.getEntity();
         if (player != null && victim != player && RuinStacks.isRuin(player)) {
            Vec3 at = RuinStacks.touchPoint(player, victim);
            if (RuinStacks.add(victim, 1, player, at)) {
               RuinCues.stackLanded(player.serverLevel(), victim, at);
            }
         }
      }
   }

   private static ServerPlayer melee(DamageSource source) {
      if (!source.is(DamageTypes.PLAYER_ATTACK)) {
         return null;
      } else {
         if (source.getDirectEntity() instanceof ServerPlayer player && source.getEntity() == player) {
            return player;
         }

         return null;
      }
   }

   private static void onDeath(LivingDeathEvent event) {
      if (!event.isCanceled()) {
         RuinFeats.died(event.getEntity(), event.getSource());
      }
   }

   private static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
      RuinStacks.tickAll(event.getServer());
   }

   private static void onServerStopped(ServerStoppedEvent event) {
      RuinStacks.clearAll();
   }
}
