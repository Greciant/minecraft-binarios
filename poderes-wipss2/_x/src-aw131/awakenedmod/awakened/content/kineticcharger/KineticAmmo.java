package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.power.PassiveToggle;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class KineticAmmo {
   public static final TagKey<Item> PROTECTED = ItemTags.create(AwId.of("kinetic_protected"));
   public static final PassiveToggle MAIN_HAND_ONLY = new PassiveToggle(
      "kinetic_main_hand", "awakened.screen.setting_kinetic_main_hand", "awakened.screen.setting_kinetic_main_hand_desc", true
   );
   private static final float HEAVIEST_MULTIPLIER = 3.0F;
   private static final int FULL_STACK = 64;
   private static final int HOTBAR_SLOTS = 9;
   private static final int MAIN_SLOTS = 36;

   private KineticAmmo() {
   }

   public static boolean isThrowable(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         if (stack.getMaxStackSize() <= 1) {
            return false;
         } else if (stack.isEnchanted()) {
            return false;
         } else if (stack.has(DataComponents.CUSTOM_NAME)) {
            return false;
         } else {
            return "awakened".equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace()) ? false : !stack.is(PROTECTED);
         }
      } else {
         return false;
      }
   }

   public static ItemStack pick(Player player) {
      ItemStack main = player.getMainHandItem();
      if (isThrowable(main)) {
         return main;
      } else if (MAIN_HAND_ONLY.enabled(player)) {
         return ItemStack.EMPTY;
      } else {
         ItemStack off = player.getOffhandItem();
         if (isThrowable(off)) {
            return off;
         } else {
            for (int slot = 0; slot < 9; slot++) {
               ItemStack stack = player.getInventory().getItem(slot);
               if (isThrowable(stack)) {
                  return stack;
               }
            }

            for (int slotx = 9; slotx < 36; slotx++) {
               ItemStack stack = player.getInventory().getItem(slotx);
               if (isThrowable(stack)) {
                  return stack;
               }
            }

            return ItemStack.EMPTY;
         }
      }
   }

   public static float heaviness(ItemStack stack) {
      int maxStack = Math.max(1, Math.min(64, stack.getMaxStackSize()));
      return 1.0F + (1.0F - (float)maxStack / 64.0F) * 2.0F;
   }
}
