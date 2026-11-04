package com.awakenedmod.awakened.content.relic;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;

public final class RelicLegacyMigration {
   private RelicLegacyMigration() {
   }

   public static void register(IEventBus eventBus) {
      eventBus.addListener(RelicLegacyMigration::onLogin);
   }

   private static void onLogin(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         convertInventory(player);
      }
   }

   private static void convertInventory(ServerPlayer player) {
      Inventory inventory = player.getInventory();

      for (int i = 0; i < inventory.getContainerSize(); i++) {
         ItemStack stack = inventory.getItem(i);
         Item relicId = stack.getItem();
         if (relicId instanceof RelicItem) {
            RelicItem relicItem = (RelicItem)relicId;
            if (relicItem.boundRelicId() == null) {
               String relicIdx = RelicItem.relicId(stack);
               if (relicIdx != null) {
                  RelicItem replacement = RelicItems.item(relicIdx);
                  if (replacement != null) {
                     ItemStack converted = new ItemStack(replacement, stack.getCount());
                     copyComponent(stack, converted, DataComponents.CUSTOM_NAME);
                     copyComponent(stack, converted, DataComponents.LORE);
                     inventory.setItem(i, converted);
                  }
               }
            }
         }
      }
   }

   private static <T> void copyComponent(ItemStack from, ItemStack to, DataComponentType<T> type) {
      T value = (T)from.get(type);
      if (value != null) {
         to.set(type, value);
      }
   }
}
