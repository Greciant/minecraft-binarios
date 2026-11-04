package com.devilfruits.fruits.uo_uo_seiryu.datagen;

import com.devilfruits.core.item.AbilityIconItem;
import com.devilfruits.core.trigger.DevilFruitTrigger;
import com.devilfruits.datagen.DFItemModelProvider;
import com.devilfruits.datagen.DFRecipeProvider;
import com.devilfruits.datagen.DFTagProviders;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuAbilities;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuItems;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuTriggers;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.registry.DFTags;
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
import net.minecraft.resources.ResourceKey;
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

public final class SeiryuData implements FruitDataContributor {
   private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/block/prismarine_bricks.png");

   @Override
   public void itemModels(DFItemModelProvider models) {
      models.fruitItem(UoUoSeiryu.fruit(), (Item)SeiryuItems.FRUIT.get());
      ((ItemModelBuilder)((SeparateTransformsModelBuilder)((ItemModelBuilder)((ItemModelBuilder)models.getBuilder(SeiryuItems.HASSAIKAI.getId().getPath()))
                  .guiLight(GuiLight.SIDE))
               .customLoader(SeparateTransformsModelBuilder::begin))
            .base((ItemModelBuilder)((ItemModelBuilder)models.nested()).parent(models.getExistingFile(models.modLoc("item/uo_uo_seiryu/hassaikai_in_hand"))))
            .perspective(
               ItemDisplayContext.GUI,
               (ItemModelBuilder)((ItemModelBuilder)models.nested()).parent(models.getExistingFile(models.modLoc("item/uo_uo_seiryu/gui/hassaikai")))
            )
            .perspective(ItemDisplayContext.FIXED, flat(models))
            .end())
         .texture("particle", models.modLoc("item/fruit/uo_uo_seiryu/hassaikai"));
   }

   private static ItemModelBuilder flat(DFItemModelProvider models) {
      return (ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)models.nested()).parent(models.getExistingFile(models.mcLoc("item/generated"))))
         .texture("layer0", models.modLoc("item/fruit/uo_uo_seiryu/hassaikai"));
   }

   @Override
   public void itemTags(DFTagProviders.ItemTagGen tags) {
      tags.item(UoUoSeiryu.CLUBS).add((Item)SeiryuItems.HASSAIKAI.get());
      tags.item(ItemTags.SWORDS).add((Item)SeiryuItems.HASSAIKAI.get());
   }

   @Override
   public void damageTypes(BootstrapContext<DamageType> context) {
      context.register(UoUoSeiryu.THUNDER_DAMAGE, new DamageType("devilfruits.uo_uo_seiryu.thunder", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
      context.register(UoUoSeiryu.FLAME_DAMAGE, new DamageType("devilfruits.uo_uo_seiryu.flame", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
      context.register(UoUoSeiryu.DRAGON_DAMAGE, new DamageType("devilfruits.uo_uo_seiryu.dragon", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
   }

   @Override
   public void damageTypeTags(DFTagProviders.DamageTypeTagGen tags) {
      tags.damageType(DamageTypeTags.IS_LIGHTNING).add(UoUoSeiryu.THUNDER_DAMAGE);
      tags.damageType(DamageTypeTags.IS_FIRE).add(UoUoSeiryu.FLAME_DAMAGE);
      tags.damageType(DFTags.DEVIL_FRUIT_ATTACKS).add(new ResourceKey[]{UoUoSeiryu.THUNDER_DAMAGE, UoUoSeiryu.FLAME_DAMAGE});
      tags.damageType(DamageTypeTags.NO_KNOCKBACK).add(new ResourceKey[]{UoUoSeiryu.THUNDER_DAMAGE, UoUoSeiryu.FLAME_DAMAGE, UoUoSeiryu.DRAGON_DAMAGE});
   }

   @Override
   public void advancements(Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper efh) {
      AdvancementHolder root = Builder.advancement()
         .display(
            new ItemStack((ItemLike)SeiryuItems.FRUIT.get()), title("azure_dragon"), desc("azure_dragon"), BACKGROUND, AdvancementType.TASK, true, true, false
         )
         .addCriterion("ate_fruit", DevilFruitTrigger.TriggerInstance.of(DFTriggers.ATE_FRUIT.get(), UoUoSeiryu.ID))
         .save(saver, UoUoSeiryu.id("azure_dragon").toString());
      AdvancementHolder hybrid = Builder.advancement()
         .parent(root)
         .display(AbilityIconItem.of(SeiryuAbilities.TRANSFORM_HYBRID), title("man_beast"), desc("man_beast"), null, AdvancementType.TASK, true, true, false)
         .addCriterion("hybrid", DevilFruitTrigger.TriggerInstance.of(DFTriggers.CHANGED_FORM.get(), UoUoSeiryu.ID, "hybrid"))
         .save(saver, UoUoSeiryu.id("man_beast").toString());
      AdvancementHolder dragon = Builder.advancement()
         .parent(hybrid)
         .display(AbilityIconItem.of(SeiryuAbilities.TRANSFORM_DRAGON), title("seiryu"), desc("seiryu"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("dragon", DevilFruitTrigger.TriggerInstance.of(DFTriggers.CHANGED_FORM.get(), UoUoSeiryu.ID, "dragon"))
         .save(saver, UoUoSeiryu.id("seiryu").toString());
      Builder.advancement()
         .parent(hybrid)
         .display(AbilityIconItem.of(SeiryuAbilities.DRACONIC_CATACLYSM), title("ragnaraku"), desc("ragnaraku"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("used_ragnaraku", SeiryuTriggers.USED_RAGNARAKU.get().createCriterion(new TriggerInstance(Optional.empty())))
         .save(saver, UoUoSeiryu.id("ragnaraku").toString());
      AdvancementHolder storm = Builder.advancement()
         .parent(dragon)
         .display(
            AbilityIconItem.of(SeiryuAbilities.THUNDER_STORM),
            title("thunder_cloud_storm"),
            desc("thunder_cloud_storm"),
            null,
            AdvancementType.GOAL,
            true,
            true,
            false
         )
         .addCriterion("used_thunder_storm", SeiryuTriggers.USED_THUNDER_STORM.get().createCriterion(new TriggerInstance(Optional.empty())))
         .save(saver, UoUoSeiryu.id("thunder_cloud_storm").toString());
      Builder.advancement()
         .parent(storm)
         .display(
            new ItemStack((ItemLike)SeiryuItems.HASSAIKAI.get()),
            title("strongest_creature"),
            desc("strongest_creature"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            false
         )
         .rewards(net.minecraft.advancements.AdvancementRewards.Builder.experience(500))
         .addCriterion("max_mastery", DevilFruitTrigger.TriggerInstance.of(DFTriggers.MAX_MASTERY.get(), UoUoSeiryu.ID))
         .save(saver, UoUoSeiryu.id("strongest_creature").toString());
   }

   @Override
   public void recipes(RecipeOutput output) {
      ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, (ItemLike)SeiryuItems.HASSAIKAI.get())
         .pattern(" ON")
         .pattern(" NO")
         .pattern("S  ")
         .define('N', Items.NETHERITE_INGOT)
         .define('O', Items.OBSIDIAN)
         .define('S', Items.STICK)
         .unlockedBy("has_netherite_ingot", DFRecipeProvider.hasItem(Items.NETHERITE_INGOT))
         .save(output, UoUoSeiryu.id("hassaikai"));
   }

   private static Component title(String name) {
      return Component.translatable("advancements.devilfruits.uo_uo_seiryu." + name + ".title");
   }

   private static Component desc(String name) {
      return Component.translatable("advancements.devilfruits.uo_uo_seiryu." + name + ".description");
   }
}
