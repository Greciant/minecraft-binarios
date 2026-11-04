package com.devilfruits.fruits.kilo_kilo;

import com.devilfruits.fruits.kilo_kilo.weight.HeavyBody;
import com.devilfruits.fruits.kilo_kilo.weight.WeightCurves;
import com.devilfruits.fruits.kilo_kilo.weight.WeightManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

final class KiloEvents {
   static void register(IEventBus bus) {
      bus.addListener(KiloEvents::onPlayerTick);
      bus.addListener(KiloEvents::onJump);
      bus.addListener(KiloEvents::onKnockBack);
      bus.addListener(KiloEvents::onExplosionKnockBack);
      bus.addListener(EventPriority.LOW, KiloEvents::onLogin);
      bus.addListener(EventPriority.LOW, KiloEvents::onRespawn);
      bus.addListener(EventPriority.LOW, KiloEvents::onDimension);
      bus.addListener(KiloEvents::onLogout);
   }

   private static void onPlayerTick(Post event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         WeightManager.tick(player);
      }
   }

   private static void onJump(LivingJumpEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && KiloKilo.isUser(player)) {
         HeavyBody.onJump(player);
      }
   }

   private static void onKnockBack(LivingKnockBackEvent event) {
      if (event.getEntity() instanceof Player player && KiloKilo.isUser(player)) {
         double mult = WeightCurves.knockbackMultiplier(KiloKilo.weight(player).kg(), WeightManager.tuning());
         if (mult > 1.0) {
            event.setStrength((float)((double)event.getStrength() * mult));
         }
      }
   }

   private static void onExplosionKnockBack(ExplosionKnockbackEvent event) {
      if (event.getAffectedEntity() instanceof Player player && KiloKilo.isUser(player)) {
         double mult = WeightCurves.knockbackMultiplier(KiloKilo.weight(player).kg(), WeightManager.tuning());
         if (mult > 1.0) {
            event.setKnockbackVelocity(event.getKnockbackVelocity().scale(mult));
         }
      }
   }

   private static void onLogin(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         WeightManager.settle(player);
      }
   }

   private static void onRespawn(PlayerRespawnEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         WeightManager.settle(player);
      }
   }

   private static void onDimension(PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         WeightManager.settle(player);
      }
   }

   private static void onLogout(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         HeavyBody.forget(player);
      }
   }

   private KiloEvents() {
   }
}
