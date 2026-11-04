package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.content.vfx.VfxBolt;
import com.awakenedmod.awakened.core.PowerSuppression;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class BrawlerEvents {
   private BrawlerEvents() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(EventPriority.HIGH, BrawlerEvents::onGuardIncoming);
      gameBus.addListener(EventPriority.HIGH, BrawlerEvents::onGuardProjectile);
      gameBus.addListener(BrawlerEvents::onMeleeLanded);
      gameBus.addListener(BrawlerEvents::onIncomingDamage);
      gameBus.addListener(BrawlerEvents::onKnockback);
      gameBus.addListener(BrawlerEvents::onServerTick);
      gameBus.addListener(BrawlerEvents::onServerStopped);
      gameBus.addListener(BrawlerEvents::onLogout);
      gameBus.addListener(BrawlerEvents::onChangedDimension);
   }

   private static ServerPlayer vanillaMelee(DamageSource source) {
      if (!source.is(DamageTypes.PLAYER_ATTACK)) {
         return null;
      } else {
         if (source.getDirectEntity() instanceof ServerPlayer player && source.getEntity() == player) {
            return isBrawler(player) ? player : null;
         }

         return null;
      }
   }

   private static boolean isBrawler(ServerPlayer player) {
      PowerData data = AwakenedAttachments.get(player);
      return data.hasPower() && "brawler".equals(data.powerId()) && !PowerSuppression.isSuppressed(player);
   }

   private static ServerPlayer striker(DamageSource source) {
      if (source.getDirectEntity() != null) {
         return vanillaMelee(source);
      } else if (source.is(DamageTypes.PLAYER_ATTACK) && source.getEntity() instanceof ServerPlayer player) {
         return isBrawler(player) ? player : null;
      } else {
         return null;
      }
   }

   private static void onGuardIncoming(LivingIncomingDamageEvent event) {
      if (event.getEntity() instanceof ServerPlayer victim && isBrawler(victim) && BrawlerGuard.onIncomingDamage(victim, event.getSource(), event.getAmount())) {
         event.setCanceled(true);
      }
   }

   private static void onGuardProjectile(ProjectileImpactEvent event) {
      if (event.getRayTraceResult() instanceof EntityHitResult hit
         && hit.getEntity() instanceof ServerPlayer victim
         && isBrawler(victim)
         && BrawlerGuard.onProjectileImpact(victim, event.getProjectile())) {
         event.setCanceled(true);
      }
   }

   private static void onMeleeLanded(Post event) {
      if (!(event.getNewDamage() <= 0.0F)) {
         ServerPlayer player = vanillaMelee(event.getSource());
         if (player != null) {
            LivingEntity victim = event.getEntity();
            if (victim != player) {
               BrawlerCombo.hit(player, victim);
            }
         }
      }
   }

   private static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (event.getEntity() instanceof ServerPlayer victim
         && isBrawler(victim)
         && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         && BrawlerIframes.active(victim)) {
         event.setCanceled(true);
         return;
      }

      ServerPlayer striker = striker(event.getSource());
      if (striker != null) {
         BrawlerCombo.markStrike(striker, event.getEntity());
      }

      ServerPlayer player = vanillaMelee(event.getSource());
      if (player != null) {
         float bonus = MarkBonus.of(MarkBonus.PUNCH, event.getEntity());
         if (player.getMainHandItem().isEmpty()) {
            bonus += BrawlerCombo.bonusFraction(player);
         }

         if (bonus > 0.0F) {
            event.setAmount(event.getAmount() * (1.0F + bonus));
         }
      }
   }

   private static void onKnockback(LivingKnockBackEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && isBrawler(player)) {
         if (BrawlerFlow.holdsGround(player) || BrawlerGuard.parriedThisTick(player)) {
            event.setCanceled(true);
         }

         return;
      }
   }

   private static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
      Marks.tickAll(event.getServer());
      BrawlerFollowUp.tickAll(event.getServer());
      BrawlerHitStop.tickAll(event.getServer());
      BrawlerLift.tickAll(event.getServer());
      VfxBolt.tickAll(event.getServer());
      BrawlerGuard.tickPost(event.getServer());
   }

   private static void onServerStopped(ServerStoppedEvent event) {
      BrawlerGuard.clearAll();
      Marks.clearAll();
      BrawlerFollowUp.clearAll();
      BrawlerHitStop.clearAll();
      BrawlerLift.clearAll();
      BrawlerCombo.clearAll();
      BrawlerMelee.clearAll();
      BrawlerFlow.clearAll();
      BrawlerIframes.clearAll();
   }

   private static void onLogout(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         Marks.clearPlayer(player);
         BrawlerFollowUp.clearPlayer(player);
         BrawlerLift.clearPlayer(player);
         BrawlerCombo.clearPlayer(player);
         BrawlerMelee.clearPlayer(player);
         BrawlerFlow.clearPlayer(player);
         BrawlerIframes.clearPlayer(player);
         BrawlerGuard.clearPlayer(player);
      }
   }

   private static void onChangedDimension(PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         if (isBrawler(player)) {
            Marks.clearPlayer(player);
            BrawlerFollowUp.clearPlayer(player);
            BrawlerLift.clearPlayer(player);
            BrawlerCombo.reset(player);
            BrawlerMelee.clearPlayer(player);
            BrawlerFlow.clearPlayer(player);
            BrawlerIframes.clearPlayer(player);
            BrawlerGuard.clearPlayer(player);
         }
      }
   }
}
