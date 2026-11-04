package com.devilfruits.fruits.horo_horo;

import com.devilfruits.fruits.horo_horo.ability.MiniHollowAbility;
import com.devilfruits.fruits.horo_horo.ability.SpecialHollowAbility;
import com.devilfruits.fruits.horo_horo.entity.GhostBody;
import com.devilfruits.fruits.horo_horo.ghost.GhostForm;
import net.minecraft.tags.DamageTypeTags;
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

public final class HoroEvents {
   private static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (event.getEntity() instanceof Player player
         && HoroHoro.isGhost(player)
         && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         && !GhostForm.forwarding()) {
         event.setCanceled(true);
         return;
      }

      if (event.getSource().getEntity() != null && SpecialHollowAbility.swallowed(event.getSource().getEntity())) {
         event.setCanceled(true);
      }
   }

   private static void onAttack(AttackEntityEvent event) {
      if (HoroHoro.isGhost(event.getEntity()) || SpecialHollowAbility.swallowed(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   private static void onInteract(PlayerInteractEvent event) {
      if (!(event instanceof ICancellableEvent cancellable) || !HoroHoro.isGhost(event.getEntity())) {
         return;
      }

      if (event instanceof EntityInteract interact && interact.getTarget() instanceof GhostBody body && event.getEntity().getUUID().equals(body.ownerId())) {
         return;
      }

      cancellable.setCanceled(true);
   }

   private static void onBreak(BreakEvent event) {
      if (HoroHoro.isGhost(event.getPlayer())) {
         event.setCanceled(true);
      }
   }

   private static void onPlace(EntityPlaceEvent event) {
      if (event.getEntity() instanceof Player player && HoroHoro.isGhost(player)) {
         event.setCanceled(true);
      }
   }

   private static void onPickup(Pre event) {
      if (HoroHoro.isGhost(event.getPlayer())) {
         event.setCanPickup(TriState.FALSE);
      }
   }

   private static void onChangeTarget(LivingChangeTargetEvent event) {
      if (event.getNewAboutToBeSetTarget() instanceof Player player && HoroHoro.isGhost(player)) {
         event.setCanceled(true);
      }
   }

   public static void register(IEventBus bus) {
      bus.addListener(HoroEvents::onIncomingDamage);
      bus.addListener(HoroEvents::onAttack);
      bus.addListener(RightClickBlock.class, HoroEvents::onInteract);
      bus.addListener(RightClickItem.class, HoroEvents::onInteract);
      bus.addListener(LeftClickBlock.class, HoroEvents::onInteract);
      bus.addListener(EntityInteract.class, HoroEvents::onInteract);
      bus.addListener(EntityInteractSpecific.class, HoroEvents::onInteract);
      bus.addListener(HoroEvents::onBreak);
      bus.addListener(HoroEvents::onPlace);
      bus.addListener(HoroEvents::onPickup);
      bus.addListener(HoroEvents::onChangeTarget);
      bus.addListener(e -> MiniHollowAbility.forget(e.getEntity().getUUID()));
      bus.addListener(e -> SpecialHollowAbility.clear());
   }

   private HoroEvents() {
   }
}
