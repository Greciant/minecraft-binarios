package com.devilfruits.fruits.suna_suna.datagen;

import com.devilfruits.core.item.AbilityIconItem;
import com.devilfruits.core.trigger.DevilFruitTrigger;
import com.devilfruits.datagen.DFBlockStateProvider;
import com.devilfruits.datagen.DFItemModelProvider;
import com.devilfruits.datagen.DFTagProviders;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.suna_suna.SunaAbilities;
import com.devilfruits.fruits.suna_suna.SunaBlocks;
import com.devilfruits.fruits.suna_suna.SunaItems;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import com.devilfruits.fruits.suna_suna.SunaTags;
import com.devilfruits.fruits.suna_suna.SunaTriggers;
import com.devilfruits.fruits.suna_suna.block.QuicksandBlock;
import com.devilfruits.registry.DFTags;
import com.devilfruits.registry.DFTriggers;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.Advancement.Builder;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger.TriggerInstance;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile;
import net.neoforged.neoforge.common.Tags.Blocks;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class SunaData implements FruitDataContributor {
   private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/block/sand.png");

   @Override
   public void advancements(Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper efh) {
      AdvancementHolder root = Builder.advancement()
         .display(
            new ItemStack((ItemLike)SunaItems.FRUIT.get()), title("desert_king"), desc("desert_king"), BACKGROUND, AdvancementType.TASK, true, true, false
         )
         .addCriterion("ate_fruit", DevilFruitTrigger.TriggerInstance.of(DFTriggers.ATE_FRUIT.get(), SunaSuna.ID))
         .save(saver, SunaSuna.id("desert_king").toString());
      AdvancementHolder grains = Builder.advancement()
         .parent(root)
         .display(new ItemStack(Items.SAND), title("grains_of_sand"), desc("grains_of_sand"), null, AdvancementType.TASK, true, true, false)
         .addCriterion("passed", DevilFruitTrigger.TriggerInstance.of(DFTriggers.LOGIA_PASSED.get(), SunaSuna.ID))
         .save(saver, SunaSuna.id("grains_of_sand").toString());
      AdvancementHolder spada = Builder.advancement()
         .parent(root)
         .display(AbilityIconItem.of(SunaAbilities.DESERT_SPADA), title("desert_spada"), desc("desert_spada"), null, AdvancementType.TASK, true, true, false)
         .addCriterion("used_spada", player(SunaTriggers.USED_SPADA.get()))
         .save(saver, SunaSuna.id("desert_spada").toString());
      AdvancementHolder girasole = Builder.advancement()
         .parent(spada)
         .display(AbilityIconItem.of(SunaAbilities.DESERT_GIRASOLE), title("ant_lion"), desc("ant_lion"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("opened_girasole", player(SunaTriggers.OPENED_GIRASOLE.get()))
         .save(saver, SunaSuna.id("ant_lion").toString());
      AdvancementHolder sables = Builder.advancement()
         .parent(girasole)
         .display(AbilityIconItem.of(SunaAbilities.SABLES), title("sables"), desc("sables"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("used_sables", player(SunaTriggers.USED_SABLES.get()))
         .save(saver, SunaSuna.id("sables").toString());
      Builder.advancement()
         .parent(sables)
         .display(new ItemStack(Items.GOLDEN_HOE), title("warlord"), desc("warlord"), null, AdvancementType.CHALLENGE, true, true, false)
         .rewards(net.minecraft.advancements.AdvancementRewards.Builder.experience(500))
         .addCriterion("max_mastery", DevilFruitTrigger.TriggerInstance.of(DFTriggers.MAX_MASTERY.get(), SunaSuna.ID))
         .save(saver, SunaSuna.id("warlord").toString());
   }

   private static Criterion<TriggerInstance> player(PlayerTrigger trigger) {
      return trigger.createCriterion(new TriggerInstance(Optional.empty()));
   }

   private static Component title(String name) {
      return Component.translatable("advancements.devilfruits.suna_suna." + name + ".title");
   }

   private static Component desc(String name) {
      return Component.translatable("advancements.devilfruits.suna_suna." + name + ".description");
   }

   @Override
   public void itemModels(DFItemModelProvider models) {
      models.fruitItem(SunaSuna.fruit(), (Item)SunaItems.FRUIT.get());
   }

   @Override
   public void blockStates(DFBlockStateProvider states) {
      ((VariantBlockStateBuilder)states.getVariantBuilder((Block)SunaBlocks.QUICKSAND.get())
            .partialState()
            .with(QuicksandBlock.RED, false)
            .modelForState()
            .modelFile(new UncheckedModelFile(ResourceLocation.withDefaultNamespace("block/sand")))
            .addModel())
         .partialState()
         .with(QuicksandBlock.RED, true)
         .modelForState()
         .modelFile(new UncheckedModelFile(ResourceLocation.withDefaultNamespace("block/red_sand")))
         .addModel();
   }

   @Override
   public void blockTags(DFTagProviders.BlockTagGen tags) {
      tags.block(SunaTags.SAND_SOURCE).addTag(Blocks.SANDS).addTag(Blocks.SANDSTONE_BLOCKS).add((Block)SunaBlocks.QUICKSAND.get());
      tags.block(SunaTags.DRIES_TO_SAND)
         .add(
            new Block[]{
               net.minecraft.world.level.block.Blocks.GRASS_BLOCK,
               net.minecraft.world.level.block.Blocks.DIRT,
               net.minecraft.world.level.block.Blocks.COARSE_DIRT,
               net.minecraft.world.level.block.Blocks.ROOTED_DIRT,
               net.minecraft.world.level.block.Blocks.PODZOL,
               net.minecraft.world.level.block.Blocks.MYCELIUM,
               net.minecraft.world.level.block.Blocks.FARMLAND,
               net.minecraft.world.level.block.Blocks.DIRT_PATH,
               net.minecraft.world.level.block.Blocks.MUD,
               net.minecraft.world.level.block.Blocks.MUDDY_MANGROVE_ROOTS,
               net.minecraft.world.level.block.Blocks.CLAY,
               net.minecraft.world.level.block.Blocks.MOSS_BLOCK
            }
         );
      tags.block(SunaTags.WITHERS)
         .addTag(BlockTags.SMALL_FLOWERS)
         .addTag(BlockTags.TALL_FLOWERS)
         .addTag(BlockTags.SAPLINGS)
         .addTag(BlockTags.CROPS)
         .add(
            new Block[]{
               net.minecraft.world.level.block.Blocks.SHORT_GRASS,
               net.minecraft.world.level.block.Blocks.TALL_GRASS,
               net.minecraft.world.level.block.Blocks.FERN,
               net.minecraft.world.level.block.Blocks.LARGE_FERN,
               net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH,
               net.minecraft.world.level.block.Blocks.PINK_PETALS,
               net.minecraft.world.level.block.Blocks.SUGAR_CANE
            }
         );
      tags.block(SunaTags.SPADA_RESISTS).addTag(DFTags.RESISTS_TECHNIQUES);
      tags.block(SunaTags.DRIES_AWAY)
         .add(
            new Block[]{
               net.minecraft.world.level.block.Blocks.SNOW,
               net.minecraft.world.level.block.Blocks.KELP,
               net.minecraft.world.level.block.Blocks.KELP_PLANT,
               net.minecraft.world.level.block.Blocks.SEAGRASS,
               net.minecraft.world.level.block.Blocks.TALL_SEAGRASS,
               net.minecraft.world.level.block.Blocks.LILY_PAD,
               net.minecraft.world.level.block.Blocks.MOSS_CARPET
            }
         );
   }

   @Override
   public void damageTypes(BootstrapContext<DamageType> context) {
      context.register(SunaSuna.SAND_DAMAGE, new DamageType("devilfruits.suna_suna.sand", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
      context.register(SunaSuna.DEHYDRATION_DAMAGE, new DamageType("devilfruits.suna_suna.dehydration", DamageScaling.NEVER, 0.0F));
      context.register(SunaSuna.QUICKSAND_DAMAGE, new DamageType("devilfruits.suna_suna.quicksand", DamageScaling.NEVER, 0.0F));
   }

   @Override
   public void damageTypeTags(DFTagProviders.DamageTypeTagGen tags) {
      tags.damageType(DFTags.DEVIL_FRUIT_ATTACKS).add(SunaSuna.DEHYDRATION_DAMAGE);
      tags.damageType(DamageTypeTags.BYPASSES_ARMOR).add(new ResourceKey[]{SunaSuna.DEHYDRATION_DAMAGE, SunaSuna.QUICKSAND_DAMAGE});
      tags.damageType(DamageTypeTags.NO_KNOCKBACK).add(new ResourceKey[]{SunaSuna.SAND_DAMAGE, SunaSuna.DEHYDRATION_DAMAGE, SunaSuna.QUICKSAND_DAMAGE});
   }
}
