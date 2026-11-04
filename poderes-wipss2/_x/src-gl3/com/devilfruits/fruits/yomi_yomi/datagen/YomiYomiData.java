package com.devilfruits.fruits.yomi_yomi.datagen;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.item.AbilityIconItem;
import com.devilfruits.core.trigger.DevilFruitTrigger;
import com.devilfruits.datagen.DFItemModelProvider;
import com.devilfruits.datagen.DFRecipeProvider;
import com.devilfruits.datagen.DFTagProviders;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.yomi_yomi.YomiAbilities;
import com.devilfruits.fruits.yomi_yomi.YomiItems;
import com.devilfruits.fruits.yomi_yomi.YomiTriggers;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.frost.YomiFrost;
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
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class YomiYomiData implements FruitDataContributor {
   private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/block/soul_soil.png");

   @Override
   public void itemModels(DFItemModelProvider models) {
      models.fruitItem(YomiYomi.fruit(), (Item)YomiItems.FRUIT.get());

      for (Item item : new Item[]{(Item)YomiItems.SOUL_CANE.get(), (Item)YomiItems.SCABBARD.get()}) {
         String name = path(item);
         ((ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)models.getBuilder(name))
                     .parent(new UncheckedModelFile(models.mcLoc("builtin/entity"))))
                  .guiLight(GuiLight.FRONT))
               .texture("particle", models.modLoc("item/yomi_yomi/" + name)))
            .transforms()
            .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
            .rotation(0.0F, -90.0F, 55.0F)
            .translation(0.0F, 4.0F, 0.5F)
            .scale(0.85F)
            .end()
            .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
            .rotation(0.0F, 90.0F, -55.0F)
            .translation(0.0F, 4.0F, 0.5F)
            .scale(0.85F)
            .end()
            .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            .rotation(0.0F, -90.0F, 25.0F)
            .translation(1.13F, 3.2F, 1.13F)
            .scale(0.68F)
            .end()
            .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
            .rotation(0.0F, 90.0F, -25.0F)
            .translation(1.13F, 3.2F, 1.13F)
            .scale(0.68F)
            .end()
            .transform(ItemDisplayContext.GROUND)
            .translation(0.0F, 2.0F, 0.0F)
            .scale(0.5F)
            .end()
            .transform(ItemDisplayContext.HEAD)
            .rotation(0.0F, 180.0F, 0.0F)
            .translation(0.0F, 13.0F, 7.0F)
            .end()
            .transform(ItemDisplayContext.FIXED)
            .rotation(0.0F, 180.0F, 0.0F)
            .end()
            .end();
      }

      String guitar = path((Item)YomiItems.SOUL_GUITAR.get());
      ((ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)models.getBuilder(guitar)).parent(new UncheckedModelFile(models.mcLoc("builtin/entity"))))
            .guiLight(GuiLight.FRONT))
         .texture("particle", models.modLoc("item/yomi_yomi/" + guitar));
   }

   @Override
   public void damageTypes(BootstrapContext<DamageType> context) {
      context.register(YomiYomi.SOUL_DAMAGE, new DamageType("devilfruits.yomi_yomi.soul", 0.1F));
      context.register(YomiFrost.HELLFIRE_COLD, new DamageType("devilfruits.yomi_yomi.hellfire_cold", 0.1F));
      context.register(YomiYomi.ROAR_DAMAGE, new DamageType("devilfruits.yomi_yomi.soul_king_roar", 0.1F));
   }

   @Override
   public void damageTypeTags(DFTagProviders.DamageTypeTagGen tags) {
      tags.damageType(DFTags.DEVIL_FRUIT_ATTACKS).add(new ResourceKey[]{YomiYomi.SOUL_DAMAGE, YomiFrost.HELLFIRE_COLD, YomiYomi.ROAR_DAMAGE});
      tags.damageType(DamageTypeTags.BYPASSES_ARMOR).add(YomiYomi.ROAR_DAMAGE);
      tags.damageType(DamageTypeTags.BYPASSES_ARMOR).add(YomiFrost.HELLFIRE_COLD);
      tags.damageType(DamageTypeTags.BYPASSES_ENCHANTMENTS).add(YomiFrost.HELLFIRE_COLD);
      tags.damageType(DamageTypeTags.BYPASSES_SHIELD).add(YomiFrost.HELLFIRE_COLD);
   }

   @Override
   public void recipes(RecipeOutput output) {
      ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, (ItemLike)YomiItems.SOUL_CANE.get())
         .pattern(" SI")
         .pattern(" PS")
         .pattern("P  ")
         .define('S', Items.STICK)
         .define('I', Items.IRON_INGOT)
         .define('P', Items.PURPLE_DYE)
         .unlockedBy("has_iron_ingot", DFRecipeProvider.hasItem(Items.IRON_INGOT))
         .save(output, YomiYomi.id("soul_solid"));
      ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, (ItemLike)YomiItems.SCABBARD.get())
         .requires(Items.STICK)
         .requires(Items.STICK)
         .requires(Items.PURPLE_DYE)
         .requires(Items.LEATHER)
         .unlockedBy("has_leather", DFRecipeProvider.hasItem(Items.LEATHER))
         .save(output, YomiYomi.id("soul_solid_scabbard"));
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)YomiItems.SOUL_GUITAR.get())
         .pattern(" SI")
         .pattern("PJP")
         .pattern("BP ")
         .define('S', Items.STRING)
         .define('I', Items.IRON_INGOT)
         .define('P', Items.PRISMARINE_SHARD)
         .define('J', Items.JUKEBOX)
         .define('B', Items.BONE_BLOCK)
         .unlockedBy("has_jukebox", DFRecipeProvider.hasItem(Items.JUKEBOX))
         .save(output, YomiYomi.id("soul_guitar"));
   }

   @Override
   public void advancements(Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper efh) {
      AdvancementHolder root = Builder.advancement()
         .display(
            new ItemStack((ItemLike)YomiItems.FRUIT.get()), title("revive_revive"), desc("revive_revive"), BACKGROUND, AdvancementType.TASK, true, true, false
         )
         .addCriterion("ate_fruit", DevilFruitTrigger.TriggerInstance.of(DFTriggers.ATE_FRUIT.get(), YomiYomi.ID))
         .save(saver, YomiYomi.id("revive_revive").toString());
      AdvancementHolder departed = Builder.advancement()
         .parent(root)
         .display(new ItemStack(Items.SOUL_LANTERN), title("soul_departed"), desc("soul_departed"), null, AdvancementType.TASK, true, true, false)
         .addCriterion("soul_departed", player(YomiTriggers.SOUL_DEPARTED.get()))
         .save(saver, YomiYomi.id("soul_departed").toString());
      AdvancementHolder revived = Builder.advancement()
         .parent(departed)
         .display(new ItemStack((ItemLike)YomiItems.SOUL_CANE.get()), title("revived"), desc("revived"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("revived", player(YomiTriggers.REVIVED.get()))
         .save(saver, YomiYomi.id("revived").toString());
      Builder.advancement()
         .parent(revived)
         .display(new ItemStack(Items.SKELETON_SKULL), title("just_bones"), desc("just_bones"), null, AdvancementType.TASK, true, true, false)
         .addCriterion("skeleton", player(YomiTriggers.SKELETON.get()))
         .save(saver, YomiYomi.id("just_bones").toString());
      Builder.advancement()
         .parent(revived)
         .display(new ItemStack((ItemLike)YomiItems.SOUL_GUITAR.get()), title("guitar"), desc("guitar"), null, AdvancementType.TASK, true, true, false)
         .addCriterion("has_guitar", TriggerInstance.hasItems(new ItemLike[]{(ItemLike)YomiItems.SOUL_GUITAR.get()}))
         .save(saver, YomiYomi.id("guitar").toString());
      Builder.advancement()
         .parent(root)
         .display(icon(YomiAbilities.SOUL_KING), title("soul_king"), desc("soul_king"), null, AdvancementType.CHALLENGE, true, true, false)
         .rewards(net.minecraft.advancements.AdvancementRewards.Builder.experience(300))
         .addCriterion("max_mastery", DevilFruitTrigger.TriggerInstance.of(DFTriggers.MAX_MASTERY.get(), YomiYomi.ID))
         .save(saver, YomiYomi.id("soul_king").toString());
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
      return Component.translatable("advancements.devilfruits.yomi_yomi." + name + ".title");
   }

   private static Component desc(String name) {
      return Component.translatable("advancements.devilfruits.yomi_yomi." + name + ".description");
   }
}
