package com.awakenedmod.awakened.content.relic;

import com.awakenedmod.awakened.content.AwakenedMenus;
import com.awakenedmod.awakened.core.data.RelicData;
import com.awakenedmod.awakened.core.relic.RelicSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class RelicBeltMenu extends AbstractContainerMenu {
   private static final int BELT_SLOTS = 3;
   private final Container belt;

   public RelicBeltMenu(int containerId, Inventory playerInventory) {
      this(containerId, playerInventory, new SimpleContainer(3));
   }

   public RelicBeltMenu(int containerId, Inventory playerInventory, Container belt) {
      super((MenuType)AwakenedMenus.RELIC_BELT.get(), containerId);
      checkContainerSize(belt, 3);
      this.belt = belt;
      belt.startOpen(playerInventory.player);
      int beltStartX = 62;

      for (int i = 0; i < 3; i++) {
         this.addSlot(new Slot(belt, i, beltStartX + i * 18, 20) {
            public boolean mayPlace(ItemStack stack) {
               return stack.getItem() instanceof RelicItem;
            }

            public int getMaxStackSize() {
               return 1;
            }
         });
      }

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
      }
   }

   public static void open(ServerPlayer player) {
      RelicData data = RelicSystem.data(player);
      SimpleContainer belt = new SimpleContainer(3);
      List<String> ids = data.attuned();

      for (int i = 0; i < Math.min(ids.size(), 3); i++) {
         belt.setItem(i, RelicItem.of(ids.get(i)));
      }

      player.openMenu(new SimpleMenuProvider((id, inv, p) -> new RelicBeltMenu(id, inv, belt), Component.translatable("container.awakened.relic_belt")));
   }

   public void removed(Player player) {
      super.removed(player);
      if (player instanceof ServerPlayer serverPlayer) {
         List<String> ids = new ArrayList<>();

         for (int i = 0; i < 3; i++) {
            ItemStack stack = this.belt.getItem(i);
            if (!stack.isEmpty()) {
               String id = RelicItem.relicId(stack);
               if (id != null) {
                  ids.add(id);
               }
            }
         }

         RelicSystem.writeBeltSlots(serverPlayer, ids);
      }

      this.belt.stopOpen(player);
   }

   public boolean stillValid(Player player) {
      return RelicSystem.data(player).hasBelt();
   }

   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack moved = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(index);
      if (slot != null && slot.hasItem()) {
         ItemStack inSlot = slot.getItem();
         moved = inSlot.copy();
         if (index < 3) {
            if (!this.moveItemStackTo(inSlot, 3, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else {
            if (!(inSlot.getItem() instanceof RelicItem)) {
               return ItemStack.EMPTY;
            }

            if (!this.moveItemStackTo(inSlot, 0, 3, false)) {
               return ItemStack.EMPTY;
            }
         }

         if (inSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }
      }

      return moved;
   }
}
