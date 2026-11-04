package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.compat.CosmeticArmorCompat;
import com.awakenedmod.awakened.core.curios.CuriosSlots;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

public record AccessoryVisibility(LivingEntity entity, int skinnedMask) {
   public static AccessoryVisibility of(LivingEntity entity) {
      return new AccessoryVisibility(entity, CosmeticArmorCompat.skinnedMask(entity));
   }

   public boolean shows(String slot, Item item) {
      return CuriosSlots.rendersWorn(this.entity, slot, item) && !this.hidesAccessories();
   }

   public boolean hidesArmor(EquipmentSlot slot) {
      return CosmeticArmorCompat.skinned(this.skinnedMask, slot);
   }

   public boolean hidesAccessories() {
      return CosmeticArmorCompat.allSkinned(this.skinnedMask);
   }
}
