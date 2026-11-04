package com.awakenedmod.awakened.core;

import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

public final class RegistryNames {
   private RegistryNames() {
   }

   @Nullable
   public static EntityType<?> entityType(String entityId) {
      ResourceLocation id = ResourceLocation.tryParse(entityId);
      return id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id) ? (EntityType)BuiltInRegistries.ENTITY_TYPE.get(id) : null;
   }

   public static Component entityName(String entityId) {
      EntityType<?> type = entityType(entityId);
      return (Component)(type != null ? type.getDescription() : Component.literal(entityId));
   }

   @Nullable
   public static Item item(String itemId) {
      ResourceLocation id = ResourceLocation.tryParse(itemId);
      return id != null && BuiltInRegistries.ITEM.containsKey(id) ? (Item)BuiltInRegistries.ITEM.get(id) : null;
   }

   public static Component itemName(String itemId) {
      Item item = item(itemId);
      return item != null ? Component.translatable(item.getDescriptionId()) : Component.literal(itemId);
   }
}
