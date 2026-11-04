package com.awakenedmod.awakened.content.shadow;

import com.awakenedmod.awakened.content.AwakenedItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

@EventBusSubscriber(
   modid = "awakened"
)
public final class ShadowBladeBinding {
   private ShadowBladeBinding() {
   }

   private static boolean isBlade(ItemStack stack) {
      return stack.is((Item)AwakenedItems.SHADOW_BLADE.get());
   }

   @SubscribeEvent
   public static void onToss(ItemTossEvent event) {
      if (!event.getPlayer().level().isClientSide() && isBlade(event.getEntity().getItem())) {
         Player player = event.getPlayer();
         ItemStack blade = event.getEntity().getItem();
         event.setCanceled(true);
         if (!player.getInventory().add(blade)) {
            player.setItemInHand(InteractionHand.MAIN_HAND, blade);
         }
      }
   }

   @SubscribeEvent
   public static void onItemEntityJoin(EntityJoinLevelEvent event) {
      if (!event.getLevel().isClientSide() && event.getEntity() instanceof ItemEntity item && isBlade(item.getItem())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onTick(Post event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         AbstractContainerMenu menu = player.containerMenu;
         if (menu != null) {
            boolean changed = false;

            for (Slot slot : menu.slots) {
               if (slot.container != player.getInventory() && slot.hasItem() && isBlade(slot.getItem())) {
                  ItemStack blade = slot.getItem();
                  slot.set(ItemStack.EMPTY);
                  slot.setChanged();
                  player.getInventory().add(blade);
                  changed = true;
               }
            }

            if (changed) {
               menu.broadcastChanges();
            }
         }
      }
   }
}
