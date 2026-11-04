package com.awakenedmod.awakened.content.summoning;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;

public final class SummonMounts {
   private static final double SPEED_BONUS_AT_CAP = 0.5;
   private static final double JUMP_BONUS_AT_CAP = 0.43;
   private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath("awakened", "mount_mastery_speed");
   private static final ResourceLocation JUMP_ID = ResourceLocation.fromNamespaceAndPath("awakened", "mount_mastery_jump");

   private SummonMounts() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(SummonMounts::onEntityInteract);
      gameBus.addListener(SummonMounts::onDrops);
      gameBus.addListener(SummonMounts::onBreed);
   }

   public static boolean isMount(Entity entity) {
      return entity instanceof AbstractHorse && SummonAttachments.minion(entity).isMinion();
   }

   static void prepare(Mob mob, ServerPlayer owner) {
      if (mob instanceof AbstractHorse horse) {
         horse.setTamed(true);
         horse.setOwnerUUID(owner.getUUID());
         if (horse.isSaddleable()) {
            horse.equipSaddle(new ItemStack(Items.SADDLE), null);
         }

         int mastery = AwakenedAttachments.get(owner).masteryLevel();
         double t = (double)(MasteryScaling.massBonus(mastery) / MasteryScaling.massBonus(Integer.MAX_VALUE));
         MinionSupport.scaleAttribute(horse, Attributes.MOVEMENT_SPEED, SPEED_ID, 0.5 * t);
         MinionSupport.scaleAttribute(horse, Attributes.JUMP_STRENGTH, JUMP_ID, 0.43 * t);
      }
   }

   private static void onEntityInteract(EntityInteract event) {
      if (event.getTarget() instanceof AbstractHorse horse && isMount(horse)) {
         ItemStack held = event.getItemStack();
         if (!SummonAttachments.minion(horse).isOwnedBy(event.getEntity().getUUID()) || held.is(Items.CHEST) || fitsBody(horse, held) || stripsGear(held)) {
            event.setCanceled(true);
         }

         return;
      }
   }

   private static boolean fitsBody(AbstractHorse horse, ItemStack stack) {
      return !stack.isEmpty() && horse.isBodyArmorItem(stack);
   }

   private static boolean stripsGear(ItemStack stack) {
      return false;
   }

   private static void onBreed(BabyEntitySpawnEvent event) {
      if (isMount(event.getParentA()) || isMount(event.getParentB())) {
         event.setCanceled(true);
      }
   }

   private static void onDrops(LivingDropsEvent event) {
      if (isMount(event.getEntity())) {
         event.getDrops().removeIf(drop -> drop.getItem().is(Items.SADDLE));
      }
   }
}
