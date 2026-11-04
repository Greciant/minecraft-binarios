package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.summoning.SummonSouls;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

public final class SoulIcons {
   private SoulIcons() {
   }

   public static ItemStack of(String soulType) {
      return of(SummonSouls.type(soulType));
   }

   public static ItemStack of(EntityType<?> type) {
      SpawnEggItem egg = type != null ? SpawnEggItem.byId(type) : null;
      return egg != null ? new ItemStack(egg) : new ItemStack(Items.SKELETON_SKULL);
   }

   public static ItemStack of(String soulType, int count) {
      ItemStack stack = of(soulType);
      stack.setCount(Math.max(1, count));
      return stack;
   }
}
