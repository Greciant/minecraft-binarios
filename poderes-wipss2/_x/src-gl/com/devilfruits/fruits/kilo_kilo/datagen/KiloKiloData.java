package com.devilfruits.fruits.kilo_kilo.datagen;

import com.devilfruits.core.item.AbilityIconItem;
import com.devilfruits.core.trigger.DevilFruitTrigger;
import com.devilfruits.datagen.DFItemModelProvider;
import com.devilfruits.datagen.DFTagProviders;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.kilo_kilo.KiloAbilities;
import com.devilfruits.fruits.kilo_kilo.KiloItems;
import com.devilfruits.fruits.kilo_kilo.KiloKilo;
import com.devilfruits.fruits.kilo_kilo.KiloTriggers;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class KiloKiloData implements FruitDataContributor {
   private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/block/polished_andesite.png");

   @Override
   public void advancements(Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper efh) {
      AdvancementHolder root = Builder.advancement()
         .display(
            new ItemStack((ItemLike)KiloItems.FRUIT.get()), title("weight_weight"), desc("weight_weight"), BACKGROUND, AdvancementType.TASK, true, true, false
         )
         .addCriterion("ate_fruit", DevilFruitTrigger.TriggerInstance.of(DFTriggers.ATE_FRUIT.get(), KiloKilo.ID))
         .save(saver, KiloKilo.id("weight_weight").toString());
      AdvancementHolder feather = Builder.advancement()
         .parent(root)
         .display(AbilityIconItem.of(KiloAbilities.ONE_KILO), title("feather_jump"), desc("feather_jump"), null, AdvancementType.TASK, true, true, false)
         .addCriterion("feather_jump", player(KiloTriggers.FEATHER_JUMP.get()))
         .save(saver, KiloKilo.id("feather_jump").toString());
      AdvancementHolder wrecking = Builder.advancement()
         .parent(root)
         .display(AbilityIconItem.of(KiloAbilities.PRESS), title("wrecking_ball"), desc("wrecking_ball"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("wrecking_ball", player(KiloTriggers.WRECKING_BALL.get()))
         .save(saver, KiloKilo.id("wrecking_ball").toString());
      Builder.advancement()
         .parent(wrecking)
         .display(new ItemStack(Items.OAK_BOAT), title("sinking_feeling"), desc("sinking_feeling"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("sank_raft", player(KiloTriggers.SANK_RAFT.get()))
         .save(saver, KiloKilo.id("sinking_feeling").toString());
      Builder.advancement()
         .parent(feather)
         .display(new ItemStack(Items.ANVIL), title("miss_valentine"), desc("miss_valentine"), null, AdvancementType.CHALLENGE, true, true, false)
         .rewards(net.minecraft.advancements.AdvancementRewards.Builder.experience(300))
         .addCriterion("max_mastery", DevilFruitTrigger.TriggerInstance.of(DFTriggers.MAX_MASTERY.get(), KiloKilo.ID))
         .save(saver, KiloKilo.id("miss_valentine").toString());
   }

   private static Criterion<TriggerInstance> player(PlayerTrigger trigger) {
      return trigger.createCriterion(new TriggerInstance(Optional.empty()));
   }

   private static Component title(String name) {
      return Component.translatable("advancements.devilfruits.kilo_kilo." + name + ".title");
   }

   private static Component desc(String name) {
      return Component.translatable("advancements.devilfruits.kilo_kilo." + name + ".description");
   }

   @Override
   public void itemModels(DFItemModelProvider models) {
      models.fruitItem(KiloKilo.fruit(), (Item)KiloItems.FRUIT.get());
   }

   @Override
   public void damageTypes(BootstrapContext<DamageType> context) {
      context.register(KiloKilo.PRESS_DAMAGE, new DamageType("devilfruits.kilo_kilo.press", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
   }

   @Override
   public void damageTypeTags(DFTagProviders.DamageTypeTagGen tags) {
      tags.damageType(DFTags.DEVIL_FRUIT_ATTACKS).add(KiloKilo.PRESS_DAMAGE);
      tags.damageType(DamageTypeTags.NO_KNOCKBACK).add(KiloKilo.PRESS_DAMAGE);
   }
}
