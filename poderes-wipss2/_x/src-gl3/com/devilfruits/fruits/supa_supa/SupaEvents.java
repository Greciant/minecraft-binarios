package com.devilfruits.fruits.supa_supa;

import com.devilfruits.fruits.supa_supa.ability.BladeSkatesAbility;
import com.devilfruits.fruits.supa_supa.ability.SpiralHollowAbility;
import com.devilfruits.fruits.supa_supa.ability.SteelBodyAbility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

public final class SupaEvents {
   static void register(IEventBus bus) {
      bus.addListener(SupaEvents::onPlayerTick);
      bus.addListener(SupaEvents::onLogout);
      bus.addListener(EventPriority.LOWEST, SupaEvents::onIncomingDamage);
      bus.addListener(SupaEvents::onKnockBack);
      bus.addListener(SupaEvents::onExplosionKnockBack);
      bus.addListener(SupaEvents::onProjectileImpact);
   }

   private static void onPlayerTick(Post event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         if (SupaSupa.isUser(player)) {
            if (!SupaSupa.active(player) && BladeLimbs.of(player).any()) {
               BladeLimbs.clear(player);
            }

            BladeLimbs.tick(player);
         }
      }
   }

   private static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && SteelBodyAbility.active(player)) {
         event.setAmount(SteelBodyAbility.absorb(player, event.getSource(), event.getAmount()));
      }
   }

   private static void onKnockBack(LivingKnockBackEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && SteelBodyAbility.active(player)) {
         event.setCanceled(true);
      }
   }

   private static void onExplosionKnockBack(ExplosionKnockbackEvent event) {
      if (event.getAffectedEntity() instanceof ServerPlayer player && SteelBodyAbility.active(player)) {
         event.setKnockbackVelocity(Vec3.ZERO);
      }
   }

   private static void onProjectileImpact(ProjectileImpactEvent event) {
      Projectile projectile = event.getProjectile();
      if (!projectile.level().isClientSide() && event.getRayTraceResult() instanceof EntityHitResult hit) {
         if (hit.getEntity() instanceof ServerPlayer player && SteelBodyAbility.active(player) && SteelBodyAbility.bounce(player, projectile)) {
            event.setCanceled(true);
         }
      }
   }

   private static void onLogout(PlayerLoggedOutEvent event) {
      BladeLimbs.forget(event.getEntity().getUUID());
      BladeSkatesAbility.forget(event.getEntity().getUUID());
      SpiralHollowAbility.forget(event.getEntity().getUUID());
      if (event.getEntity() instanceof ServerPlayer player) {
         SteelBodyAbility.clear(player);
      }
   }

   private SupaEvents() {
   }
}
