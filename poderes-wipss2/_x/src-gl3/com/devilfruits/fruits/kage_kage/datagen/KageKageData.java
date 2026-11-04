package com.devilfruits.fruits.kage_kage.datagen;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.item.AbilityIconItem;
import com.devilfruits.core.trigger.DevilFruitTrigger;
import com.devilfruits.datagen.DFItemModelProvider;
import com.devilfruits.datagen.DFRecipeProvider;
import com.devilfruits.datagen.DFTagProviders;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.kage_kage.KageAbilities;
import com.devilfruits.fruits.kage_kage.KageItems;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kage_kage.KageTriggers;
import com.devilfruits.registry.DFTags;
import com.devilfruits.registry.DFTriggers;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.Advancement.Builder;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance;
import net.minecraft.client.renderer.block.model.BlockModel.GuiLight;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.SeparateTransformsModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class KageKageData implements FruitDataContributor {
   private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/block/soul_soil.png");

   @Override
   public void itemModels(DFItemModelProvider models) {
      models.fruitItem(KageKage.fruit(), (Item)KageItems.FRUIT.get());

      for (Item item : new Item[]{
         (Item)KageItems.CADAVER_FLESH.get(),
         (Item)KageItems.CADAVER_ARM.get(),
         (Item)KageItems.CADAVER_LEG.get(),
         (Item)KageItems.CADAVER_HEAD.get(),
         (Item)KageItems.CADAVER_TORSO.get(),
         (Item)KageItems.HOLLOW_CORPSE.get()
      }) {
         String name = path(item);
         ((ItemModelBuilder)models.withExistingParent(name, models.mcLoc("item/generated"))).texture("layer0", models.modLoc("item/kage_kage/" + name));
      }

      ItemModelBuilder split = scissors(models, "moria_scissors_split", "moria_scissors_held_a", "moria_scissors", true);
      scissors(models, "moria_scissors", "moria_scissors_held", "moria_scissors", false)
         .override()
         .predicate(DevilFruits.id("kage_split"), 1.0F)
         .model(split)
         .end();
      scissors(models, path((Item)KageItems.MORIA_BLADE.get()), "moria_scissors_held_b", "moria_scissors_blade", false);
   }

   private static ItemModelBuilder scissors(DFItemModelProvider models, String name, String held, String icon, boolean flip) {
      ItemModelBuilder hand = (ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)models.nested())
            .parent(models.getExistingFile(models.mcLoc("item/handheld"))))
         .texture("layer0", models.modLoc("item/kage_kage/" + held));
      int f = flip ? -1 : 1;
      hand.transforms()
         .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
         .rotation(0.0F, (float)(-90 * f), (float)(10 * f))
         .translation(0.0F, 9.0F, 1.5F)
         .scale(2.0F)
         .end()
         .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
         .rotation(0.0F, (float)(90 * f), (float)(-10 * f))
         .translation(0.0F, 9.0F, 1.5F)
         .scale(2.0F)
         .end()
         .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
         .rotation(0.0F, (float)(-90 * f), (float)(-20 * f))
         .translation(1.13F, 4.2F, 1.13F)
         .scale(1.1F)
         .end()
         .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
         .rotation(0.0F, (float)(90 * f), (float)(20 * f))
         .translation(1.13F, 4.2F, 1.13F)
         .scale(1.1F)
         .end()
         .transform(ItemDisplayContext.GROUND)
         .translation(0.0F, 2.0F, 0.0F)
         .scale(0.75F)
         .end();
      return (ItemModelBuilder)((ItemModelBuilder)((SeparateTransformsModelBuilder)((ItemModelBuilder)((ItemModelBuilder)models.getBuilder(name))
                  .guiLight(GuiLight.FRONT))
               .customLoader(SeparateTransformsModelBuilder::begin))
            .base(hand)
            .perspective(
               ItemDisplayContext.GUI,
               (ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)models.nested()).parent(new UncheckedModelFile(models.mcLoc("builtin/entity"))))
                  .texture("particle", models.modLoc("item/kage_kage/" + icon))
            )
            .end())
         .texture("particle", models.modLoc("item/kage_kage/" + icon));
   }

   @Override
   public void damageTypes(BootstrapContext<DamageType> context) {
      context.register(KageKage.SHADOW_DAMAGE, new DamageType("devilfruits.kage_kage.shadow", 0.1F));
   }

   @Override
   public void damageTypeTags(DFTagProviders.DamageTypeTagGen tags) {
      tags.damageType(DFTags.DEVIL_FRUIT_ATTACKS).add(KageKage.SHADOW_DAMAGE);
   }

   @Override
   public void recipes(RecipeOutput output) {
      ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, (ItemLike)KageItems.MORIA_SCISSORS.get())
         .pattern("D D")
         .pattern(" N ")
         .pattern("B B")
         .define('D', Items.DIAMOND)
         .define('N', Items.NETHERITE_INGOT)
         .define('B', Items.BONE_BLOCK)
         .unlockedBy("has_netherite_ingot", DFRecipeProvider.hasItem(Items.NETHERITE_INGOT))
         .save(output, KageKage.id("moria_scissors"));
      part(output, (Item)KageItems.CADAVER_ARM.get(), 3, 1);
      part(output, (Item)KageItems.CADAVER_LEG.get(), 3, 2);
      part(output, (Item)KageItems.CADAVER_HEAD.get(), 4, 1);
      part(output, (Item)KageItems.CADAVER_TORSO.get(), 6, 3);
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)KageItems.HOLLOW_CORPSE.get())
         .pattern(" H ")
         .pattern("ATA")
         .pattern("L L")
         .define('H', (ItemLike)KageItems.CADAVER_HEAD.get())
         .define('A', (ItemLike)KageItems.CADAVER_ARM.get())
         .define('T', (ItemLike)KageItems.CADAVER_TORSO.get())
         .define('L', (ItemLike)KageItems.CADAVER_LEG.get())
         .unlockedBy("has_cadaver_torso", DFRecipeProvider.hasItem((ItemLike)KageItems.CADAVER_TORSO.get()))
         .save(output, KageKage.id("hollow_corpse"));
   }

   private static void part(RecipeOutput output, Item part, int flesh, int bones) {
      ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, part)
         .requires((ItemLike)KageItems.CADAVER_FLESH.get(), flesh)
         .requires(Items.BONE, bones)
         .unlockedBy("has_cadaver_flesh", DFRecipeProvider.hasItem((ItemLike)KageItems.CADAVER_FLESH.get()))
         .save(output, KageKage.id(path(part)));
   }

   @Override
   public void advancements(Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper efh) {
      AdvancementHolder root = Builder.advancement()
         .display(
            new ItemStack((ItemLike)KageItems.FRUIT.get()), title("shadow_shadow"), desc("shadow_shadow"), BACKGROUND, AdvancementType.TASK, true, true, false
         )
         .addCriterion("ate_fruit", DevilFruitTrigger.TriggerInstance.of(DFTriggers.ATE_FRUIT.get(), KageKage.ID))
         .save(saver, KageKage.id("shadow_shadow").toString());
      AdvancementHolder scissors = Builder.advancement()
         .parent(root)
         .display(new ItemStack((ItemLike)KageItems.MORIA_SCISSORS.get()), title("scissors"), desc("scissors"), null, AdvancementType.TASK, true, true, false)
         .addCriterion("has_scissors", TriggerInstance.hasItems(new ItemLike[]{(ItemLike)KageItems.MORIA_SCISSORS.get()}))
         .save(saver, KageKage.id("scissors").toString());
      AdvancementHolder snip = Builder.advancement()
         .parent(scissors)
         .display(icon(KageAbilities.COLLECTION), title("snip"), desc("snip"), null, AdvancementType.TASK, true, true, false)
         .addCriterion("shadow_taken", player(KageTriggers.SHADOW_TAKEN.get()))
         .save(saver, KageKage.id("snip").toString());
      AdvancementHolder zombie = Builder.advancement()
         .parent(snip)
         .display(
            new ItemStack((ItemLike)KageItems.HOLLOW_CORPSE.get()),
            title("thriller_bark"),
            desc("thriller_bark"),
            null,
            AdvancementType.GOAL,
            true,
            true,
            false
         )
         .addCriterion("zombie_raised", player(KageTriggers.ZOMBIE_RAISED.get()))
         .save(saver, KageKage.id("thriller_bark").toString());
      Builder.advancement()
         .parent(zombie)
         .display(new ItemStack(Items.ZOMBIE_HEAD), title("full_crew"), desc("full_crew"), null, AdvancementType.CHALLENGE, true, true, false)
         .addCriterion("full_crew", player(KageTriggers.FULL_CREW.get()))
         .save(saver, KageKage.id("full_crew").toString());
      Builder.advancement()
         .parent(root)
         .display(icon(KageAbilities.DOPPELMAN), title("warlord"), desc("warlord"), null, AdvancementType.CHALLENGE, true, true, false)
         .rewards(net.minecraft.advancements.AdvancementRewards.Builder.experience(300))
         .addCriterion("max_mastery", DevilFruitTrigger.TriggerInstance.of(DFTriggers.MAX_MASTERY.get(), KageKage.ID))
         .save(saver, KageKage.id("warlord").toString());
   }

   private static String path(Item item) {
      return BuiltInRegistries.ITEM.getKey(item).getPath();
   }

   private static ItemStack icon(Ability ability) {
      return AbilityIconItem.of(ability);
   }

   private static Criterion<net.minecraft.advancements.critereon.PlayerTrigger.TriggerInstance> player(PlayerTrigger trigger) {
      return trigger.createCriterion(new net.minecraft.advancements.critereon.PlayerTrigger.TriggerInstance(Optional.empty()));
   }

   private static Component title(String name) {
      return Component.translatable("advancements.devilfruits.kage_kage." + name + ".title");
   }

   private static Component desc(String name) {
      return Component.translatable("advancements.devilfruits.kage_kage." + name + ".description");
   }
}
