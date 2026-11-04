package com.devilfruits.fruits.gura_gura.datagen;

import com.devilfruits.core.item.AbilityIconItem;
import com.devilfruits.core.trigger.DevilFruitTrigger;
import com.devilfruits.datagen.DFItemModelProvider;
import com.devilfruits.datagen.DFRecipeProvider;
import com.devilfruits.datagen.DFTagProviders;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.gura_gura.GuraAbilities;
import com.devilfruits.fruits.gura_gura.GuraGura;
import com.devilfruits.fruits.gura_gura.GuraItems;
import com.devilfruits.fruits.gura_gura.GuraTriggers;
import com.devilfruits.registry.DFTriggers;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Advancement.Builder;
import net.minecraft.advancements.critereon.PlayerTrigger.TriggerInstance;
import net.minecraft.client.renderer.block.model.BlockModel.GuiLight;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.loaders.SeparateTransformsModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class GuraGuraData implements FruitDataContributor {
   private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/block/cracked_deepslate_tiles.png");

   @Override
   public void itemModels(DFItemModelProvider models) {
      models.fruitItem(GuraGura.fruit(), (Item)GuraItems.FRUIT.get());
      ((ItemModelBuilder)((SeparateTransformsModelBuilder)((ItemModelBuilder)((ItemModelBuilder)models.getBuilder(GuraItems.BISENTO.getId().getPath()))
                  .guiLight(GuiLight.SIDE))
               .customLoader(SeparateTransformsModelBuilder::begin))
            .base((ItemModelBuilder)((ItemModelBuilder)models.nested()).parent(models.getExistingFile(models.modLoc("item/gura_gura/bisento_in_hand"))))
            .perspective(
               ItemDisplayContext.GUI,
               (ItemModelBuilder)((ItemModelBuilder)models.nested()).parent(models.getExistingFile(models.modLoc("item/gura_gura/gui/bisento")))
            )
            .perspective(ItemDisplayContext.FIXED, flatBisento(models))
            .end())
         .texture("particle", models.modLoc("item/fruit/gura_gura/bisento"));
   }

   private static ItemModelBuilder flatBisento(DFItemModelProvider models) {
      return (ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)models.nested()).parent(models.getExistingFile(models.mcLoc("item/generated"))))
         .texture("layer0", models.modLoc("item/fruit/gura_gura/bisento"));
   }

   @Override
   public void itemTags(DFTagProviders.ItemTagGen tags) {
      tags.item(GuraGura.QUAKE_WEAPONS).addTag(ItemTags.SWORDS).addTag(ItemTags.AXES).add(Items.TRIDENT).add((Item)GuraItems.BISENTO.get());
      tags.item(ItemTags.SWORDS).add((Item)GuraItems.BISENTO.get());
   }

   @Override
   public void damageTypes(BootstrapContext<DamageType> context) {
      context.register(GuraGura.QUAKE_DAMAGE, new DamageType("devilfruits.gura_gura.quake", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
   }

   @Override
   public void damageTypeTags(DFTagProviders.DamageTypeTagGen tags) {
      tags.damageType(DamageTypeTags.BYPASSES_SHIELD).add(GuraGura.QUAKE_DAMAGE);
      tags.damageType(DamageTypeTags.NO_KNOCKBACK).add(GuraGura.QUAKE_DAMAGE);
   }

   @Override
   public void advancements(Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper efh) {
      AdvancementHolder root = Builder.advancement()
         .display(
            new ItemStack((ItemLike)GuraItems.FRUIT.get()), title("tremor_tremor"), desc("tremor_tremor"), BACKGROUND, AdvancementType.TASK, true, true, false
         )
         .addCriterion("ate_fruit", DevilFruitTrigger.TriggerInstance.of(DFTriggers.ATE_FRUIT.get(), GuraGura.ID))
         .save(saver, GuraGura.id("tremor_tremor").toString());
      AdvancementHolder islandShaker = Builder.advancement()
         .parent(root)
         .display(AbilityIconItem.of(GuraAbilities.SHIMA_YURASHI), title("island_shaker"), desc("island_shaker"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("used_shima_yurashi", GuraTriggers.USED_SHIMA_YURASHI.get().createCriterion(new TriggerInstance(Optional.empty())))
         .save(saver, GuraGura.id("island_shaker").toString());
      Builder.advancement()
         .parent(root)
         .display(AbilityIconItem.of(GuraAbilities.KAISHO), title("seaquake"), desc("seaquake"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("ocean_seaquake", GuraTriggers.OCEAN_SEAQUAKE.get().createCriterion(new TriggerInstance(Optional.empty())))
         .save(saver, GuraGura.id("seaquake").toString());
      Builder.advancement()
         .parent(islandShaker)
         .display(
            new ItemStack((ItemLike)GuraItems.BISENTO.get()), title("strongest_man"), desc("strongest_man"), null, AdvancementType.CHALLENGE, true, true, false
         )
         .rewards(net.minecraft.advancements.AdvancementRewards.Builder.experience(500))
         .addCriterion("max_mastery", DevilFruitTrigger.TriggerInstance.of(DFTriggers.MAX_MASTERY.get(), GuraGura.ID))
         .save(saver, GuraGura.id("strongest_man").toString());
   }

   @Override
   public void recipes(RecipeOutput output) {
      ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, (ItemLike)GuraItems.BISENTO.get())
         .pattern(" NN")
         .pattern(" SN")
         .pattern("S  ")
         .define('N', Items.NETHERITE_INGOT)
         .define('S', Items.STICK)
         .unlockedBy("has_netherite_ingot", DFRecipeProvider.hasItem(Items.NETHERITE_INGOT))
         .save(output, GuraGura.id("bisento"));
   }

   private static Component title(String name) {
      return Component.translatable("advancements.devilfruits.gura_gura." + name + ".title");
   }

   private static Component desc(String name) {
      return Component.translatable("advancements.devilfruits.gura_gura." + name + ".description");
   }
}
