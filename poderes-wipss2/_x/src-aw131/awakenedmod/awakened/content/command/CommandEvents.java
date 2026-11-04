package com.awakenedmod.awakened.content.command;

import com.awakenedmod.awakened.content.command.vocab.effect.ReturnsHereEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class CommandEvents {
   private CommandEvents() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(CommandEvents::onTargetLeave);
      gameBus.addListener(EventPriority.HIGH, CommandEvents::onIncomingDamage);
      gameBus.addListener(CommandEvents::onHeldBodyDamage);
      gameBus.addListener(CommandEvents::onLogin);
      gameBus.addListener(ReturnsHereEffect::onFall);
      gameBus.addListener(CommandEvents::onServerStopped);
   }

   private static void onLogin(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && CommandPower.owns(player)) {
         RuleEngine.restoreAfterCrash(player);
      }
   }

   private static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && CommandPower.owns(player) && RuleEngine.shieldsFrom(player, event.getSource())) {
         event.setCanceled(true);
      }
   }

   private static void onHeldBodyDamage(LivingIncomingDamageEvent event) {
      if (!event.getEntity().level().isClientSide()) {
         float factor = RuleEngine.incomingDamageMultiplier(event.getEntity());
         if (factor != 1.0F) {
            event.setAmount(event.getAmount() * factor);
         }
      }
   }

   private static void onTargetLeave(EntityLeaveLevelEvent event) {
      Entity body = event.getEntity();
      if (Landings.isBoundTarget(body.getUUID())) {
         if (event.getLevel() instanceof ServerLevel level) {
            RemovalReason reason = body.getRemovalReason();
            if (reason != null && reason.shouldDestroy()) {
               RuleEngine.onTargetDestroyed(level.getServer(), body.getUUID());
            } else {
               RuleEngine.onTargetGone(level.getServer(), body.getUUID());
            }
         }
      }
   }

   private static void onServerStopped(ServerStoppedEvent event) {
      RuleEngine.clearAll(event.getServer());
   }
}
