package com.devilfruits.fruits.yomi_yomi;

import com.devilfruits.fruits.yomi_yomi.entity.PlayerCorpse;
import com.devilfruits.fruits.yomi_yomi.frost.YomiFrost;
import com.devilfruits.fruits.yomi_yomi.soul.SoulProjection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

public final class YomiEvents {
   private static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (event.getEntity() instanceof Player player
         && YomiYomi.isSoul(player)
         && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         && !SoulProjection.forwarding()) {
         event.setCanceled(true);
         return;
      }

      if (event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker == event.getSource().getEntity() && YomiFrost.frozen(attacker)) {
         event.setCanceled(true);
      }
   }

   private static void onAttack(AttackEntityEvent event) {
      if (YomiYomi.isSoul(event.getEntity()) || YomiFrost.frozen(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   private static void onInteract(PlayerInteractEvent event) {
      if (!(event instanceof ICancellableEvent cancellable) || !YomiYomi.isSoul(event.getEntity())) {
         return;
      }

      if (event instanceof EntityInteract interact
         && interact.getTarget() instanceof PlayerCorpse corpse
         && event.getEntity().getUUID().equals(corpse.ownerId())) {
         return;
      }

      cancellable.setCanceled(true);
   }

   private static void onBreak(BreakEvent event) {
      if (YomiYomi.isSoul(event.getPlayer())) {
         event.setCanceled(true);
      }
   }

   private static void onPlace(EntityPlaceEvent event) {
      if (event.getEntity() instanceof Player player && YomiYomi.isSoul(player)) {
         event.setCanceled(true);
      }
   }

   private static void onPickup(Pre event) {
      if (YomiYomi.isSoul(event.getPlayer())) {
         event.setCanPickup(TriState.FALSE);
      }
   }

   private static void onChangeTarget(LivingChangeTargetEvent event) {
      if (event.getNewAboutToBeSetTarget() instanceof Player player) {
         if (YomiYomi.isSoul(player)) {
            event.setCanceled(true);
         } else {
            LivingEntity mob = event.getEntity();
            if (YomiYomi.vessel(player) == Vessel.CORPSE && mob.getType().is(EntityTypeTags.UNDEAD) && mob.getLastHurtByMob() != player) {
               event.setCanceled(true);
            }
         }
      }
   }

   private static void onTick(Post event) {
      if (event.getEntity() instanceof ServerPlayer player && YomiYomi.isUser(player)) {
         WaterRun.tick(player);
         if (player.tickCount % 20 == 0
            && YomiYomi.vessel(player) == Vessel.CORPSE
            && !player.isCreative()
            && !player.isSpectator()
            && !YomiYomi.isSoul(player)) {
            scareTheLiving(player);
         }

         return;
      }
   }

   public static void scareTheLiving(ServerPlayer player) {
      double fear = YomiYomi.setting("villagerFearRadius");

      for (Villager villager : player.serverLevel().getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(fear))) {
         villager.getBrain().setMemoryWithExpiry(MemoryModuleType.HURT_BY_ENTITY, player, 60L);
         villager.getBrain().setMemoryWithExpiry(MemoryModuleType.HURT_BY, player.damageSources().playerAttack(player), 60L);
      }

      double aggro = YomiYomi.setting("golemAggroRadius");

      for (IronGolem golem : player.serverLevel().getEntitiesOfClass(IronGolem.class, player.getBoundingBox().inflate(aggro))) {
         if (golem.getTarget() == null || golem.getTarget() == player) {
            angerAt(golem, player);
         }
      }
   }

   private static void angerAt(Mob golem, ServerPlayer player) {
      if (golem instanceof IronGolem g) {
         g.setPersistentAngerTarget(player.getUUID());
         g.startPersistentAngerTimer();
      }

      golem.setTarget(player);
   }

   public static void register(IEventBus bus) {
      bus.addListener(YomiEvents::onIncomingDamage);
      bus.addListener(YomiEvents::onAttack);
      bus.addListener(RightClickBlock.class, YomiEvents::onInteract);
      bus.addListener(RightClickItem.class, YomiEvents::onInteract);
      bus.addListener(LeftClickBlock.class, YomiEvents::onInteract);
      bus.addListener(EntityInteract.class, YomiEvents::onInteract);
      bus.addListener(EntityInteractSpecific.class, YomiEvents::onInteract);
      bus.addListener(YomiEvents::onBreak);
      bus.addListener(YomiEvents::onPlace);
      bus.addListener(YomiEvents::onPickup);
      bus.addListener(YomiEvents::onChangeTarget);
      bus.addListener(YomiEvents::onTick);
   }

   private YomiEvents() {
   }
}
