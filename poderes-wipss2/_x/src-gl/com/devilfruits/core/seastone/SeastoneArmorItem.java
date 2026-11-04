package com.devilfruits.core.seastone;

import com.devilfruits.DevilFruits;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial.Layer;
import net.minecraft.world.item.Item.Properties;

public class SeastoneArmorItem extends ArmorItem {
   private final ResourceLocation texture;

   public SeastoneArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
      super(material, type, properties);

      String piece = switch (type) {
         case HELMET -> "helmet";
         case CHESTPLATE, BODY -> "chest";
         case LEGGINGS -> "legs";
         case BOOTS -> "boots";
         default -> throw new MatchException(null, null);
      };
      this.texture = DevilFruits.id("textures/models/armor/seastone_" + piece + ".png");
   }

   public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, Layer layer, boolean innerModel) {
      return this.texture;
   }
}
