package com.devilfruits.fruits.gura_gura.item;

import com.devilfruits.fruits.gura_gura.GuraGura;
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

public class BisentoItem extends SwordItem {
   public BisentoItem(Tier tier, Properties properties) {
      super(tier, properties);
   }

   public static ItemAttributeModifiers createBisentoAttributes() {
      return ItemAttributeModifiers.builder()
         .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 11.0, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
         .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.0, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
         .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(GuraGura.id("bisento_reach"), 1.5, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
         .build();
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.devilfruits.bisento.lore").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));
      tooltip.add(Component.translatable("item.devilfruits.bisento.hint").withStyle(ChatFormatting.DARK_AQUA));
   }
}
