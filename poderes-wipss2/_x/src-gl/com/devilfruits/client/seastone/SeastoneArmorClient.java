package com.devilfruits.client.seastone;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.seastone.SeastoneGear;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class SeastoneArmorClient {
   private static final Map<EquipmentSlot, ModelLayerLocation> LAYERS = new EnumMap<>(
      Map.of(
         EquipmentSlot.HEAD,
         new ModelLayerLocation(DevilFruits.id("seastone_armor"), "helmet"),
         EquipmentSlot.CHEST,
         new ModelLayerLocation(DevilFruits.id("seastone_armor"), "chest"),
         EquipmentSlot.LEGS,
         new ModelLayerLocation(DevilFruits.id("seastone_armor"), "legs"),
         EquipmentSlot.FEET,
         new ModelLayerLocation(DevilFruits.id("seastone_armor"), "boots")
      )
   );
   private static final Map<EquipmentSlot, HumanoidModel<LivingEntity>> MODELS = new EnumMap<>(EquipmentSlot.class);

   public static void init(IEventBus modBus) {
      modBus.addListener(e -> {
         e.registerLayerDefinition(LAYERS.get(EquipmentSlot.HEAD), SeastoneArmorGeometry::helmet);
         e.registerLayerDefinition(LAYERS.get(EquipmentSlot.CHEST), SeastoneArmorGeometry::chest);
         e.registerLayerDefinition(LAYERS.get(EquipmentSlot.LEGS), SeastoneArmorGeometry::legs);
         e.registerLayerDefinition(LAYERS.get(EquipmentSlot.FEET), SeastoneArmorGeometry::boots);
      });
      modBus.addListener(e -> MODELS.clear());
      modBus.addListener(
         e -> e.registerItem(
               new IClientItemExtensions() {
                  public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                     ModelLayerLocation layer = SeastoneArmorClient.LAYERS.get(slot);
                     return layer == null
                        ? original
                        : SeastoneArmorClient.MODELS.computeIfAbsent(slot, s -> new HumanoidModel(Minecraft.getInstance().getEntityModels().bakeLayer(layer)));
                  }
               },
               new Item[]{
                  (Item)SeastoneGear.HELMET.get(), (Item)SeastoneGear.CHESTPLATE.get(), (Item)SeastoneGear.LEGGINGS.get(), (Item)SeastoneGear.BOOTS.get()
               }
            )
      );
   }

   private SeastoneArmorClient() {
   }
}
