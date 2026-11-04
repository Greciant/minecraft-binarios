package com.devilfruits.fruits.doru_doru.datagen;

import com.devilfruits.core.item.AbilityIconItem;
import com.devilfruits.core.trigger.DevilFruitTrigger;
import com.devilfruits.datagen.DFBlockStateProvider;
import com.devilfruits.datagen.DFItemModelProvider;
import com.devilfruits.datagen.DFTagProviders;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.doru_doru.DoruAbilities;
import com.devilfruits.fruits.doru_doru.DoruBlocks;
import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.DoruItems;
import com.devilfruits.fruits.doru_doru.DoruTags;
import com.devilfruits.fruits.doru_doru.DoruTriggers;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class DoruDoruData implements FruitDataContributor {
   private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/block/white_concrete_powder.png");

   @Override
   public void advancements(Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper efh) {
      AdvancementHolder root = Builder.advancement()
         .display(new ItemStack((ItemLike)DoruItems.FRUIT.get()), title("wax_wax"), desc("wax_wax"), BACKGROUND, AdvancementType.TASK, true, true, false)
         .addCriterion("ate_fruit", DevilFruitTrigger.TriggerInstance.of(DFTriggers.ATE_FRUIT.get(), DoruDoru.ID))
         .save(saver, DoruDoru.id("wax_wax").toString());
      AdvancementHolder arch = Builder.advancement()
         .parent(root)
         .display(AbilityIconItem.of(DoruAbilities.ATELIER), title("first_arch"), desc("first_arch"), null, AdvancementType.TASK, true, true, false)
         .addCriterion("first_arch", player(DoruTriggers.FIRST_ARCH.get()))
         .save(saver, DoruDoru.id("first_arch").toString());
      Builder.advancement()
         .parent(arch)
         .display(new ItemStack(Items.GLASS_PANE), title("symmetry"), desc("symmetry"), null, AdvancementType.TASK, true, true, false)
         .addCriterion("symmetry", player(DoruTriggers.SYMMETRY.get()))
         .save(saver, DoruDoru.id("symmetry").toString());
      Builder.advancement()
         .parent(arch)
         .display(new ItemStack(Items.ARMOR_STAND), title("cast_mold"), desc("cast_mold"), null, AdvancementType.TASK, true, true, false)
         .addCriterion("cast_mold", player(DoruTriggers.CAST_MOLD.get()))
         .save(saver, DoruDoru.id("cast_mold").toString());
      Builder.advancement()
         .parent(root)
         .display(new ItemStack(Items.LAVA_BUCKET), title("melt_wall"), desc("melt_wall"), null, AdvancementType.TASK, true, true, true)
         .addCriterion("melt_wall", player(DoruTriggers.MELT_WALL.get()))
         .save(saver, DoruDoru.id("melt_wall").toString());
      AdvancementHolder encase = Builder.advancement()
         .parent(root)
         .display(AbilityIconItem.of(DoruAbilities.SERVICE), title("encase"), desc("encase"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("encase", player(DoruTriggers.ENCASE.get()))
         .save(saver, DoruDoru.id("encase").toString());
      AdvancementHolder champion = Builder.advancement()
         .parent(encase)
         .display(AbilityIconItem.of(DoruAbilities.CHAMPION), title("champion"), desc("champion"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("champion", player(DoruTriggers.CHAMPION.get()))
         .save(saver, DoruDoru.id("champion").toString());
      Builder.advancement()
         .parent(champion)
         .display(new ItemStack(Items.CANDLE), title("mr_3"), desc("mr_3"), null, AdvancementType.CHALLENGE, true, true, false)
         .rewards(net.minecraft.advancements.AdvancementRewards.Builder.experience(300))
         .addCriterion("max_mastery", DevilFruitTrigger.TriggerInstance.of(DFTriggers.MAX_MASTERY.get(), DoruDoru.ID))
         .save(saver, DoruDoru.id("mr_3").toString());
   }

   private static Criterion<TriggerInstance> player(PlayerTrigger trigger) {
      return trigger.createCriterion(new TriggerInstance(Optional.empty()));
   }

   private static Component title(String name) {
      return Component.translatable("advancements.devilfruits.doru_doru." + name + ".title");
   }

   private static Component desc(String name) {
      return Component.translatable("advancements.devilfruits.doru_doru." + name + ".description");
   }

   @Override
   public void blockStates(DFBlockStateProvider states) {
      ModelFile wax = states.models().cubeAll("block/doru_doru/wax", states.modLoc("block/doru_doru/wax"));
      states.getVariantBuilder((Block)DoruBlocks.WAX.get()).forAllStates(s -> ConfiguredModel.builder().modelFile(wax).build());
   }

   @Override
   public void itemModels(DFItemModelProvider models) {
      models.fruitItem(DoruDoru.fruit(), (Item)DoruItems.FRUIT.get());
      ((ItemModelBuilder)models.withExistingParent("wax_lump", models.mcLoc("item/generated"))).texture("layer0", models.modLoc("item/doru_doru/wax_lump"));

      for (String tool : new String[]{"pickaxe", "shovel", "axe", "hoe", "sword"}) {
         ((ItemModelBuilder)models.withExistingParent("wax_" + tool, models.mcLoc("item/handheld")))
            .texture("layer0", models.modLoc("item/doru_doru/wax_" + tool));
      }
   }

   @Override
   public void itemTags(DFTagProviders.ItemTagGen tags) {
      tags.item(DFTags.CASKET_KEYS).add((Item)DoruItems.WAX_KEY.get());
   }

   @Override
   public void blockTags(DFTagProviders.BlockTagGen tags) {
      tags.block(BlockTags.MINEABLE_WITH_PICKAXE).add((Block)DoruBlocks.WAX.get());
      tags.block(DoruTags.WAX_HEAT)
         .add(
            new Block[]{
               Blocks.LAVA,
               Blocks.FIRE,
               Blocks.SOUL_FIRE,
               Blocks.MAGMA_BLOCK,
               Blocks.CAMPFIRE,
               Blocks.SOUL_CAMPFIRE,
               Blocks.FURNACE,
               Blocks.SMOKER,
               Blocks.BLAST_FURNACE,
               Blocks.LAVA_CAULDRON
            }
         )
         .addTag(BlockTags.CANDLES)
         .addTag(BlockTags.CANDLE_CAKES);
      tags.block(DFTags.RESISTS_TECHNIQUES).add((Block)DoruBlocks.WAX.get());
   }

   @Override
   public void damageTypes(BootstrapContext<DamageType> context) {
      context.register(DoruDoru.WAX_DAMAGE, new DamageType("devilfruits.doru_doru.wax", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
   }

   @Override
   public void damageTypeTags(DFTagProviders.DamageTypeTagGen tags) {
      tags.damageType(DFTags.DEVIL_FRUIT_ATTACKS).add(DoruDoru.WAX_DAMAGE);
      tags.damageType(DoruTags.HEAT_DAMAGE).addTag(DamageTypeTags.IS_FIRE).add(DamageTypes.LIGHTNING_BOLT);
   }
}
