package com.awakenedmod.awakened.content.adamantium;

import com.awakenedmod.awakened.core.AwId;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;

public final class AdamantiumPlating {
   public static final int MAX_ROUNDS = 3;
   private static final double[] ARMOR_BY_ROUNDS = new double[]{0.0, 1.0, 1.5, 2.0};
   public static final double TOUGHNESS_PER_ROUND = 0.25;
   public static final double SPEED_PENALTY_PER_ROUND = -0.005;
   public static final TagKey<Item> PLATING_DENIED = ItemTags.create(AwId.of("plating_denied"));

   private AdamantiumPlating() {
   }

   public static int rounds(ItemStack stack) {
      return stack.isEmpty() ? 0 : Mth.clamp((Integer)stack.getOrDefault((DataComponentType)AdamantiumComponents.PLATING.get(), 0), 0, 3);
   }

   public static boolean isMaxed(ItemStack stack) {
      return rounds(stack) >= 3;
   }

   public static double armorFor(int rounds) {
      return ARMOR_BY_ROUNDS[Mth.clamp(rounds, 0, 3)];
   }

   public static ItemStack withRounds(ItemStack stack, int rounds) {
      ItemStack copy = stack.copyWithCount(1);
      int clamped = Mth.clamp(rounds, 0, 3);
      if (clamped <= 0) {
         copy.remove((DataComponentType)AdamantiumComponents.PLATING.get());
      } else {
         copy.set((DataComponentType)AdamantiumComponents.PLATING.get(), clamped);
      }

      return copy;
   }

   public static boolean isPlatable(ItemStack stack) {
      EquipmentSlot slot = armorSlot(stack);
      return slot != null && !stack.is(PLATING_DENIED) && grantsArmor(stack, slot);
   }

   @Nullable
   public static EquipmentSlot armorSlot(ItemStack stack) {
      if (stack.isEmpty()) {
         return null;
      } else {
         EquipmentSlot slot = stack.getEquipmentSlot();
         if (slot == null) {
            Equipable equipable = Equipable.get(stack);
            slot = equipable == null ? null : equipable.getEquipmentSlot();
         }

         return slot != null && slot.getType() == Type.HUMANOID_ARMOR ? slot : null;
      }
   }

   private static boolean grantsArmor(ItemStack stack, EquipmentSlot slot) {
      ItemAttributeModifiers modifiers = stack.getAttributeModifiers();

      for (Entry entry : modifiers.modifiers()) {
         if (entry.slot().test(slot) && entry.attribute().value() == Attributes.ARMOR.value() && entry.modifier().amount() > 0.0) {
            return true;
         }
      }

      return false;
   }
}
