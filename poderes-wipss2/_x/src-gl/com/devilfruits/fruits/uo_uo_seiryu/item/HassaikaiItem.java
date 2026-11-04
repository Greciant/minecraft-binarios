package com.devilfruits.fruits.uo_uo_seiryu.item;

import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class HassaikaiItem extends SwordItem {
   public HassaikaiItem(Tier tier, Properties properties) {
      super(tier, properties);
   }

   public static ItemAttributeModifiers createAttributes() {
      return ItemAttributeModifiers.builder()
         .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 12.0, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
         .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.2, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
         .add(
            Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(UoUoSeiryu.id("hassaikai_reach"), 1.0, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND
         )
         .build();
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.devilfruits.hassaikai.lore").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));
      tooltip.add(Component.translatable("item.devilfruits.hassaikai.hint").withStyle(ChatFormatting.BLUE));
   }
}
