package com.devilfruits.core.item;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityRegistry;
import com.devilfruits.registry.DFDataComponents;
import com.devilfruits.registry.DFItems;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ItemLike;

public class AbilityIconItem extends Item {
   public AbilityIconItem(Properties properties) {
      super(properties);
   }

   public static ItemStack of(Ability ability) {
      ItemStack stack = new ItemStack((ItemLike)DFItems.ABILITY_ICON.get());
      stack.set((DataComponentType)DFDataComponents.ABILITY.get(), ability.id());
      return stack;
   }

   public Component getName(ItemStack stack) {
      ResourceLocation id = (ResourceLocation)stack.get((DataComponentType)DFDataComponents.ABILITY.get());
      Ability ability = AbilityRegistry.get(id);
      return ability != null ? ability.displayName() : super.getName(stack);
   }
}
