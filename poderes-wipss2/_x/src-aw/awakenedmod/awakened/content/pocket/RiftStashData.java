package com.awakenedmod.awakened.content.pocket;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class RiftStashData implements Container {
   public static final int SIZE = 27;
   public static final MapCodec<RiftStashData> MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(RiftStashData::toList))
            .apply(instance, RiftStashData::fromList)
   );
   private final NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

   private static RiftStashData fromList(List<ItemStack> list) {
      RiftStashData data = new RiftStashData();

      for (int i = 0; i < 27 && i < list.size(); i++) {
         data.items.set(i, list.get(i));
      }

      return data;
   }

   private List<ItemStack> toList() {
      return List.copyOf(this.items);
   }

   public int getContainerSize() {
      return 27;
   }

   public boolean isEmpty() {
      for (ItemStack stack : this.items) {
         if (!stack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public ItemStack getItem(int slot) {
      return slot >= 0 && slot < 27 ? (ItemStack)this.items.get(slot) : ItemStack.EMPTY;
   }

   public ItemStack removeItem(int slot, int amount) {
      return ContainerHelper.removeItem(this.items, slot, amount);
   }

   public ItemStack removeItemNoUpdate(int slot) {
      return ContainerHelper.takeItem(this.items, slot);
   }

   public void setItem(int slot, ItemStack stack) {
      if (slot >= 0 && slot < 27) {
         this.items.set(slot, stack);
      }
   }

   public void setChanged() {
   }

   public boolean stillValid(Player player) {
      return true;
   }

   public void clearContent() {
      this.items.clear();
   }
}
