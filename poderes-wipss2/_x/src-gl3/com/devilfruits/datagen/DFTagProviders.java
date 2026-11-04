package com.devilfruits.datagen;

import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModules;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.poneglyph.Poneglyphs;
import com.devilfruits.core.seastone.SeastoneGear;
import com.devilfruits.mobs.pirate.Pirates;
import com.devilfruits.registry.DFBlocks;
import com.devilfruits.registry.DFDamageTypes;
import com.devilfruits.registry.DFItems;
import com.devilfruits.registry.DFTags;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider.IntrinsicTagAppender;
import net.minecraft.data.tags.TagsProvider.TagAppender;
import net.minecraft.data.tags.TagsProvider.TagLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class DFTagProviders {
   static boolean isPlayable(FruitModule module) {
      DevilFruit fruit = FruitRegistry.get(module.id());
      return fruit != null && !fruit.isDevOnly();
   }

   private DFTagProviders() {
   }

   public static final class BlockTagGen extends BlockTagsProvider {
      public BlockTagGen(PackOutput output, CompletableFuture<Provider> lookup, ExistingFileHelper efh) {
         super(output, lookup, "devilfruits", efh);
      }

      protected void addTags(Provider provider) {
         this.tag(DFTags.DESTRUCTION_IMMUNE)
            .addTag(BlockTags.WITHER_IMMUNE)
            .addTag(BlockTags.DRAGON_IMMUNE)
            .add(Blocks.BUDDING_AMETHYST)
            .add((Block)DFBlocks.SEASTONE_BLOCK.get());
         this.tag(DFTags.RESISTS_TECHNIQUES)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.ORES_COAL)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.ORES_DIAMOND)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.ORES_EMERALD)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.ORES_GOLD)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.ORES_IRON)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.ORES_LAPIS)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.ORES_NETHERITE_SCRAP)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.ORES_QUARTZ)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.ORES_REDSTONE)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS_IRON)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS_GOLD)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS_NETHERITE)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS_DIAMOND)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS_EMERALD)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS_LAPIS)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS_REDSTONE)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS_COAL)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS_RAW_IRON)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS_RAW_GOLD)
            .addTag(net.neoforged.neoforge.common.Tags.Blocks.OBSIDIANS)
            .addTag(BlockTags.ANVIL)
            .addTag(BlockTags.CAULDRONS)
            .add(
               new Block[]{
                  Blocks.BEDROCK,
                  Blocks.IRON_BARS,
                  Blocks.IRON_DOOR,
                  Blocks.IRON_TRAPDOOR,
                  Blocks.CHAIN,
                  Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
                  Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE,
                  Blocks.LODESTONE,
                  Blocks.REINFORCED_DEEPSLATE
               }
            );
         this.tag(DFTags.SEASTONE_BLOCKS)
            .add(new Block[]{(Block)DFBlocks.SEASTONE_BLOCK.get(), (Block)SeastoneGear.SEASTONE_ORE.get(), (Block)SeastoneGear.DEEPSLATE_SEASTONE_ORE.get()});
         this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .add(new Block[]{(Block)DFBlocks.SEASTONE_BLOCK.get(), (Block)SeastoneGear.SEASTONE_ORE.get(), (Block)SeastoneGear.DEEPSLATE_SEASTONE_ORE.get()});
         this.tag(BlockTags.NEEDS_IRON_TOOL)
            .add(new Block[]{(Block)DFBlocks.SEASTONE_BLOCK.get(), (Block)SeastoneGear.SEASTONE_ORE.get(), (Block)SeastoneGear.DEEPSLATE_SEASTONE_ORE.get()});
         this.tag(net.neoforged.neoforge.common.Tags.Blocks.ORES)
            .add(new Block[]{(Block)SeastoneGear.SEASTONE_ORE.get(), (Block)SeastoneGear.DEEPSLATE_SEASTONE_ORE.get()});
         this.tag(Poneglyphs.UNBREAKABLE).add(new Block[]{(Block)Poneglyphs.PONEGLYPH_STONE.get(), (Block)Poneglyphs.PONEGLYPH_FACE.get()});
         this.tag(DFTags.DESTRUCTION_IMMUNE).addTag(Poneglyphs.UNBREAKABLE);
         this.tag(BlockTags.WITHER_IMMUNE).addTag(Poneglyphs.UNBREAKABLE);
         this.tag(BlockTags.DRAGON_IMMUNE).addTag(Poneglyphs.UNBREAKABLE);
         this.tag(BlockTags.MINEABLE_WITH_AXE).add((Block)Poneglyphs.DECIPHER_DESK.get());

         for (FruitModule module : FruitModules.all()) {
            if (DFTagProviders.isPlayable(module)) {
               module.data().blockTags(this);
            }
         }
      }

      public IntrinsicTagAppender<Block> block(TagKey<Block> key) {
         return this.tag(key);
      }
   }

   public static final class DamageTypeTagGen extends TagsProvider<DamageType> {
      public DamageTypeTagGen(PackOutput output, CompletableFuture<Provider> lookup, ExistingFileHelper efh) {
         super(output, Registries.DAMAGE_TYPE, lookup, "devilfruits", efh);
      }

      protected void addTags(Provider provider) {
         this.tag(DamageTypeTags.NO_KNOCKBACK).add(new ResourceKey[]{DFDamageTypes.DEVIL_FRUIT, DFDamageTypes.SECOND_FRUIT});
         this.tag(DamageTypeTags.BYPASSES_SHIELD).add(DFDamageTypes.SECOND_FRUIT);
         this.tag(DamageTypeTags.BYPASSES_ARMOR).add(new ResourceKey[]{DFDamageTypes.SECOND_FRUIT, DFDamageTypes.HAKI_RYUO});
         this.tag(DamageTypeTags.BYPASSES_INVULNERABILITY).add(DFDamageTypes.SECOND_FRUIT);
         this.tag(DamageTypeTags.BYPASSES_EFFECTS).add(DFDamageTypes.SECOND_FRUIT);
         this.tag(DamageTypeTags.BYPASSES_RESISTANCE).add(DFDamageTypes.SECOND_FRUIT);
         this.tag(DamageTypeTags.BYPASSES_ENCHANTMENTS).add(DFDamageTypes.SECOND_FRUIT);
         TagAppender<DamageType> attacks = this.tag(DFTags.DEVIL_FRUIT_ATTACKS).add(DFDamageTypes.DEVIL_FRUIT);

         for (DevilFruit fruit : FruitRegistry.playable()) {
            if (fruit.damageType() != DFDamageTypes.DEVIL_FRUIT) {
               attacks.add(fruit.damageType());
            }
         }

         this.tag(DFTags.LOGIA_BYPASS_DAMAGE).add(DFDamageTypes.HAKI_RYUO);
         this.tag(DFTags.LOGIA_WATER_DAMAGE);

         for (FruitModule module : FruitModules.all()) {
            if (DFTagProviders.isPlayable(module)) {
               module.data().damageTypeTags(this);
            }
         }
      }

      public TagAppender<DamageType> damageType(TagKey<DamageType> key) {
         return this.tag(key);
      }
   }

   public static final class EntityTypeTagGen extends EntityTypeTagsProvider {
      public EntityTypeTagGen(PackOutput output, CompletableFuture<Provider> lookup, ExistingFileHelper efh) {
         super(output, lookup, "devilfruits", efh);
      }

      protected void addTags(Provider provider) {
         for (FruitModule module : FruitModules.all()) {
            if (DFTagProviders.isPlayable(module)) {
               module.data().entityTypeTags(this);
            }
         }
      }

      public IntrinsicTagAppender<EntityType<?>> entityType(TagKey<EntityType<?>> key) {
         return this.tag(key);
      }
   }

   public static final class ItemTagGen extends ItemTagsProvider {
      public ItemTagGen(PackOutput output, CompletableFuture<Provider> lookup, CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper efh) {
         super(output, lookup, blockTags, "devilfruits", efh);
      }

      protected void addTags(Provider provider) {
         IntrinsicTagAppender<Item> fruits = this.tag(DFTags.DEVIL_FRUITS);

         for (DevilFruit fruit : FruitRegistry.playable()) {
            fruit.item().ifPresent(fruits::add);
         }

         this.tag(DFTags.CASKET_KEYS).add((Item)DFItems.GOLD_KEY.get());
         this.tag(DFTags.SEASTONE_ITEMS)
            .add(
               new Item[]{
                  (Item)DFItems.SEASTONE_INGOT.get(),
                  (Item)DFItems.SEASTONE_BLOCK.get(),
                  (Item)SeastoneGear.RAW_SEASTONE.get(),
                  (Item)SeastoneGear.SEASTONE_NUGGET.get(),
                  (Item)SeastoneGear.SEASTONE_ORE_ITEM.get(),
                  (Item)SeastoneGear.DEEPSLATE_SEASTONE_ORE_ITEM.get()
               }
            )
            .addTag(SeastoneGear.GEAR);
         this.tag(SeastoneGear.GEAR)
            .add(
               new Item[]{
                  (Item)SeastoneGear.PICKAXE.get(),
                  (Item)SeastoneGear.AXE.get(),
                  (Item)SeastoneGear.SHOVEL.get(),
                  (Item)SeastoneGear.HOE.get(),
                  (Item)SeastoneGear.SWORD.get(),
                  (Item)SeastoneGear.JITTE.get(),
                  (Item)SeastoneGear.HELMET.get(),
                  (Item)SeastoneGear.CHESTPLATE.get(),
                  (Item)SeastoneGear.LEGGINGS.get(),
                  (Item)SeastoneGear.BOOTS.get(),
                  (Item)SeastoneGear.CUFFS.get()
               }
            );
         this.tag(ItemTags.SWORDS).add(new Item[]{(Item)SeastoneGear.SWORD.get(), (Item)SeastoneGear.JITTE.get()});
         this.tag(ItemTags.PICKAXES).add((Item)SeastoneGear.PICKAXE.get());
         this.tag(ItemTags.AXES).add((Item)SeastoneGear.AXE.get());
         this.tag(ItemTags.SHOVELS).add((Item)SeastoneGear.SHOVEL.get());
         this.tag(ItemTags.HOES).add((Item)SeastoneGear.HOE.get());
         this.tag(ItemTags.HEAD_ARMOR).add((Item)SeastoneGear.HELMET.get());
         this.tag(ItemTags.CHEST_ARMOR).add((Item)SeastoneGear.CHESTPLATE.get());
         this.tag(ItemTags.LEG_ARMOR).add((Item)SeastoneGear.LEGGINGS.get());
         this.tag(ItemTags.FOOT_ARMOR).add((Item)SeastoneGear.BOOTS.get());
         this.tag(ItemTags.ARROWS).add((Item)SeastoneGear.ARROW.get());
         this.tag(ItemTags.BOOKSHELF_BOOKS).add((Item)Poneglyphs.PRIMER.get());
         this.tag(ItemTags.DURABILITY_ENCHANTABLE).add((Item)SeastoneGear.CUFFS.get());
         this.tag(DFTags.SEASTONE_WEAPONS).addTag(DFTags.SEASTONE_ITEMS);
         this.tag(DFTags.LOGIA_BYPASS_ITEMS);
         this.tag(ItemTags.SWORDS).add(new Item[]{(Item)Pirates.CUTLASS.get(), (Item)Pirates.CAPTAIN_CUTLASS.get()});
         this.tag(ItemTags.DURABILITY_ENCHANTABLE).add(new Item[]{(Item)Pirates.FLINTLOCK_PISTOL.get(), (Item)Pirates.MUSKET.get()});

         for (FruitModule module : FruitModules.all()) {
            if (DFTagProviders.isPlayable(module)) {
               module.data().itemTags(this);
            }
         }
      }

      public IntrinsicTagAppender<Item> item(TagKey<Item> key) {
         return this.tag(key);
      }
   }
}
