package com.awakenedmod.awakened.content.shadow;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ShadowBladeAbility extends Ability {
   private static final float COST = 4.0F;

   public ShadowBladeAbility() {
      super("shadow_blade", 4.0F, 0);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (player.getMainHandItem().is((Item)AwakenedItems.SHADOW_BLADE.get())) {
         ShadowSlash.shoot(player);
         return true;
      } else {
         return manifest(player);
      }
   }

   private static boolean manifest(ServerPlayer player) {
      Inventory inv = player.getInventory();
      ItemStack blade = ItemStack.EMPTY;

      for (int i = 0; i < inv.getContainerSize(); i++) {
         if (inv.getItem(i).is((Item)AwakenedItems.SHADOW_BLADE.get())) {
            blade = inv.removeItemNoUpdate(i);
            break;
         }
      }

      if (blade.isEmpty()) {
         blade = ShadowBladeItem.stack();
      }

      ItemStack displaced = player.getMainHandItem();
      player.setItemInHand(InteractionHand.MAIN_HAND, blade);
      if (!displaced.isEmpty() && !displaced.is((Item)AwakenedItems.SHADOW_BLADE.get()) && !inv.add(displaced)) {
         player.drop(displaced, false);
      }

      return true;
   }

   @Override
   public boolean usesSneakActivate() {
      return true;
   }

   @Override
   public void onSecondaryAction(ServerPlayer player, int action) {
      if (action == 3) {
         sheathe(player);
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      Inventory inv = player.getInventory();

      for (int i = 0; i < inv.getContainerSize(); i++) {
         if (inv.getItem(i).is((Item)AwakenedItems.SHADOW_BLADE.get())) {
            inv.setItem(i, ItemStack.EMPTY);
         }
      }

      super.clearPlayer(player);
   }

   private static void sheathe(ServerPlayer player) {
      InteractionHand hand = null;
      if (player.getMainHandItem().is((Item)AwakenedItems.SHADOW_BLADE.get())) {
         hand = InteractionHand.MAIN_HAND;
      } else if (player.getOffhandItem().is((Item)AwakenedItems.SHADOW_BLADE.get())) {
         hand = InteractionHand.OFF_HAND;
      }

      if (hand != null) {
         player.setItemInHand(hand, ItemStack.EMPTY);
         player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 0.6F);
      }
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }
}
