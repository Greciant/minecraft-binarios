package com.awakenedmod.awakened.content.adamantium;

import com.awakenedmod.awakened.core.AwId;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

@EventBusSubscriber(
   modid = "awakened"
)
public final class PlatingAttributes {
   private static final Map<EquipmentSlot, PlatingAttributes.SlotIds> IDS = buildIds();

   private PlatingAttributes() {
   }

   @SubscribeEvent
   public static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
      ItemStack stack = event.getItemStack();
      int rounds = AdamantiumPlating.rounds(stack);
      if (rounds > 0) {
         EquipmentSlot slot = AdamantiumPlating.armorSlot(stack);
         PlatingAttributes.SlotIds ids = slot == null ? null : IDS.get(slot);
         if (ids != null) {
            EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(slot);
            event.addModifier(Attributes.ARMOR, new AttributeModifier(ids.armor(), AdamantiumPlating.armorFor(rounds), Operation.ADD_VALUE), group);
            event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(ids.toughness(), 0.25 * (double)rounds, Operation.ADD_VALUE), group);
            event.addModifier(Attributes.MOVEMENT_SPEED, new AttributeModifier(ids.speed(), -0.005 * (double)rounds, Operation.ADD_MULTIPLIED_BASE), group);
         }
      }
   }

   private static Map<EquipmentSlot, PlatingAttributes.SlotIds> buildIds() {
      Map<EquipmentSlot, PlatingAttributes.SlotIds> ids = new EnumMap<>(EquipmentSlot.class);

      for (EquipmentSlot slot : EquipmentSlot.values()) {
         if (slot.getType() == Type.HUMANOID_ARMOR) {
            ids.put(
               slot,
               new PlatingAttributes.SlotIds(
                  AwId.of("adamantium_plating.armor." + slot.getName()),
                  AwId.of("adamantium_plating.toughness." + slot.getName()),
                  AwId.of("adamantium_plating.speed." + slot.getName())
               )
            );
         }
      }

      return ids;
   }

   private static record SlotIds(ResourceLocation armor, ResourceLocation toughness, ResourceLocation speed) {
   }
}
