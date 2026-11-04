package com.devilfruits.core.seastone;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.item.SeastoneItem;
import com.devilfruits.core.logia.LogiaBypass;
import com.devilfruits.core.logia.LogiaBypassSource;
import com.devilfruits.registry.DFBlocks;
import com.devilfruits.registry.DFCreativeTabs;
import com.devilfruits.registry.DFItems;
import com.devilfruits.registry.DFMobEffects;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial.Layer;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SeastoneGear {
   public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, "devilfruits");
   public static final TagKey<Item> GEAR = TagKey.create(Registries.ITEM, DevilFruits.id("seastone_gear"));
   public static final Tier TIER = new SimpleTier(
      BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2031, 9.0F, 4.0F, 15, () -> Ingredient.of(new ItemLike[]{(ItemLike)DFItems.SEASTONE_INGOT.get()})
   );
   public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMOR = ARMOR_MATERIALS.register(
      "seastone",
      () -> new ArmorMaterial(
            (Map)Util.make(new EnumMap(Type.class), m -> {
               m.put(Type.BOOTS, 3);
               m.put(Type.LEGGINGS, 6);
               m.put(Type.CHESTPLATE, 8);
               m.put(Type.HELMET, 3);
               m.put(Type.BODY, 11);
            }),
            15,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            () -> Ingredient.of(new ItemLike[]{(ItemLike)DFItems.SEASTONE_INGOT.get()}),
            List.of(new Layer(DevilFruits.id("seastone"))),
            3.0F,
            0.1F
         )
   );
   public static final DeferredHolder<MobEffect, MobEffect> SUPPRESSED = DFMobEffects.EFFECTS
      .register("seastone_suppressed", () -> new DFMobEffects.CoreEffect(MobEffectCategory.HARMFUL, 3968147));
   public static final DeferredBlock<DropExperienceBlock> SEASTONE_ORE = DFBlocks.BLOCKS
      .register(
         "seastone_ore",
         () -> new DropExperienceBlock(
               UniformInt.of(2, 5), Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)
            )
      );
   public static final DeferredItem<BlockItem> SEASTONE_ORE_ITEM = DFItems.ITEMS
      .register("seastone_ore", () -> new SeastoneItem.Block((Block)SEASTONE_ORE.get(), new net.minecraft.world.item.Item.Properties()));
   public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_SEASTONE_ORE = DFBlocks.BLOCKS
      .register(
         "deepslate_seastone_ore",
         () -> new DropExperienceBlock(
               UniformInt.of(2, 5), Properties.of().mapColor(MapColor.DEEPSLATE).strength(6.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)
            )
      );
   public static final DeferredItem<BlockItem> DEEPSLATE_SEASTONE_ORE_ITEM = DFItems.ITEMS
      .register("deepslate_seastone_ore", () -> new SeastoneItem.Block((Block)DEEPSLATE_SEASTONE_ORE.get(), new net.minecraft.world.item.Item.Properties()));
   public static final DeferredItem<SeastoneItem> RAW_SEASTONE = DFItems.ITEMS
      .register("raw_seastone", () -> new SeastoneItem(new net.minecraft.world.item.Item.Properties()));
   public static final DeferredItem<SeastoneItem> SEASTONE_NUGGET = DFItems.ITEMS
      .register("seastone_nugget", () -> new SeastoneItem(new net.minecraft.world.item.Item.Properties()));
   public static final DeferredItem<PickaxeItem> PICKAXE = DFItems.ITEMS
      .register(
         "seastone_pickaxe",
         () -> new PickaxeItem(TIER, new net.minecraft.world.item.Item.Properties().attributes(PickaxeItem.createAttributes(TIER, 1.0F, -2.8F)))
      );
   public static final DeferredItem<AxeItem> AXE = DFItems.ITEMS
      .register("seastone_axe", () -> new AxeItem(TIER, new net.minecraft.world.item.Item.Properties().attributes(AxeItem.createAttributes(TIER, 5.0F, -3.0F))));
   public static final DeferredItem<ShovelItem> SHOVEL = DFItems.ITEMS
      .register(
         "seastone_shovel",
         () -> new ShovelItem(TIER, new net.minecraft.world.item.Item.Properties().attributes(ShovelItem.createAttributes(TIER, 1.5F, -3.0F)))
      );
   public static final DeferredItem<HoeItem> HOE = DFItems.ITEMS
      .register("seastone_hoe", () -> new HoeItem(TIER, new net.minecraft.world.item.Item.Properties().attributes(HoeItem.createAttributes(TIER, -4.0F, 0.0F))));
   public static final DeferredItem<SwordItem> SWORD = DFItems.ITEMS
      .register(
         "seastone_sword", () -> new SwordItem(TIER, new net.minecraft.world.item.Item.Properties().attributes(SwordItem.createAttributes(TIER, 3, -2.4F)))
      );
   public static final DeferredItem<SwordItem> JITTE = DFItems.ITEMS
      .register(
         "seastone_jitte", () -> new SwordItem(TIER, new net.minecraft.world.item.Item.Properties().attributes(SwordItem.createAttributes(TIER, 4, -2.8F)))
      );
   public static final DeferredItem<ArrowItem> ARROW = DFItems.ITEMS
      .register("seastone_arrow", () -> new ArrowItem(new net.minecraft.world.item.Item.Properties()));
   public static final DeferredItem<SeastoneCuffsItem> CUFFS = DFItems.ITEMS
      .register("seastone_cuffs", () -> new SeastoneCuffsItem(new net.minecraft.world.item.Item.Properties().durability(24)));
   public static final DeferredItem<SeastoneArmorItem> HELMET = armor("seastone_helmet", Type.HELMET);
   public static final DeferredItem<SeastoneArmorItem> CHESTPLATE = armor("seastone_chestplate", Type.CHESTPLATE);
   public static final DeferredItem<SeastoneArmorItem> LEGGINGS = armor("seastone_leggings", Type.LEGGINGS);
   public static final DeferredItem<SeastoneArmorItem> BOOTS = armor("seastone_boots", Type.BOOTS);

   private static DeferredItem<SeastoneArmorItem> armor(String name, Type type) {
      return DFItems.ITEMS
         .register(name, () -> new SeastoneArmorItem(ARMOR, type, new net.minecraft.world.item.Item.Properties().durability(type.getDurability(37))));
   }

   public static void init(IEventBus modBus) {
      ARMOR_MATERIALS.register(modBus);

      for (Supplier<? extends Item> item : List.of(
         RAW_SEASTONE,
         SEASTONE_NUGGET,
         SEASTONE_ORE_ITEM,
         DEEPSLATE_SEASTONE_ORE_ITEM,
         PICKAXE,
         AXE,
         SHOVEL,
         HOE,
         SWORD,
         JITTE,
         ARROW,
         CUFFS,
         HELMET,
         CHESTPLATE,
         LEGGINGS,
         BOOTS
      )) {
         DFCreativeTabs.addExtra(item);
      }

      LogiaBypass.register((victim, source) -> {
         if (source.getDirectEntity() instanceof AbstractArrow arrow && isSeastoneArrow(arrow)) {
            return new LogiaBypass.Result(LogiaBypassSource.SEASTONE_WEAPON, 1.0F);
         }

         return null;
      });
   }

   public static boolean isSeastoneArrow(AbstractArrow arrow) {
      ItemStack pickup = arrow.getPickupItemStackOrigin();
      return pickup.is((Item)ARROW.get());
   }

   private SeastoneGear() {
   }
}
