package com.awakenedmod.awakened.content.limitless;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(
   modid = "awakened"
)
public final class LimitlessEvents {
   private LimitlessEvents() {
   }

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && LimitlessInfinity.onIncoming(player, event.getSource(), event.getAmount())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public static void onProjectileImpact(ProjectileImpactEvent event) {
      if (event.getRayTraceResult() instanceof EntityHitResult hit
         && hit.getEntity() instanceof ServerPlayer player
         && LimitlessInfinity.onImpact(player, event.getProjectile())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public static void onAttack(AttackEntityEvent event) {
      if (LimitlessCast.of(event.getEntity(), LimitlessCast.RCT).live()) {
         event.setCanceled(true);
         if (event.getEntity().level() instanceof ServerLevel level) {
            LimitlessFx.rctSwingRefused(level, event.getEntity());
         }
      }
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      LimitlessInfinity.forgetAll();
      ShotHold.forgetAll();
   }
}
