package com.devilfruits.fruits.bomu_bomu.datagen;

import com.devilfruits.core.item.AbilityIconItem;
import com.devilfruits.core.trigger.DevilFruitTrigger;
import com.devilfruits.datagen.DFItemModelProvider;
import com.devilfruits.datagen.DFTagProviders;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.bomu_bomu.BomuAbilities;
import com.devilfruits.fruits.bomu_bomu.BomuBomu;
import com.devilfruits.fruits.bomu_bomu.BomuItems;
import com.devilfruits.fruits.bomu_bomu.BomuTriggers;
import com.devilfruits.registry.DFTags;
import com.devilfruits.registry.DFTriggers;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.Advancement.Builder;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.advancements.critereon.TradeTrigger.TriggerInstance;
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

public final class BomuBomuData implements FruitDataContributor {
   private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/block/tnt_side.png");

   @Override
   public void advancements(Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper efh) {
      AdvancementHolder root = Builder.advancement()
         .display(new ItemStack((ItemLike)BomuItems.FRUIT.get()), title("bomb_bomb"), desc("bomb_bomb"), BACKGROUND, AdvancementType.TASK, true, true, false)
         .addCriterion("ate_fruit", DevilFruitTrigger.TriggerInstance.of(DFTriggers.ATE_FRUIT.get(), BomuBomu.ID))
         .save(saver, BomuBomu.id("bomb_bomb").toString());
      Builder.advancement()
         .parent(root)
         .display(new ItemStack(Items.TNT), title("untouched"), desc("untouched"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("tnt_untouched", player(BomuTriggers.TNT_UNTOUCHED.get()))
         .save(saver, BomuBomu.id("untouched").toString());
      AdvancementHolder pistol = Builder.advancement()
         .parent(root)
         .display(
            new ItemStack((ItemLike)BomuItems.BOMB_PISTOL.get()), title("bomb_pistol"), desc("bomb_pistol"), null, AdvancementType.TASK, true, true, false
         )
         .addCriterion(
            "bought_pistol",
            CriteriaTriggers.TRADE
               .createCriterion(
                  new TriggerInstance(
                     Optional.empty(),
                     Optional.empty(),
                     Optional.of(
                        net.minecraft.advancements.critereon.ItemPredicate.Builder.item().of(new ItemLike[]{(ItemLike)BomuItems.BOMB_PISTOL.get()}).build()
                     )
                  )
               )
         )
         .save(saver, BomuBomu.id("bomb_pistol").toString());
      AdvancementHolder destruction = Builder.advancement()
         .parent(root)
         .display(
            AbilityIconItem.of(BomuAbilities.SELF_DESTRUCT), title("self_destruction"), desc("self_destruction"), null, AdvancementType.GOAL, true, true, false
         )
         .addCriterion("self_destruction", player(BomuTriggers.SELF_DESTRUCTION.get()))
         .save(saver, BomuBomu.id("self_destruction").toString());
      Builder.advancement()
         .parent(destruction)
         .display(new ItemStack(Items.OAK_BOAT), title("ship_hole"), desc("ship_hole"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("ship_hole", player(BomuTriggers.SHIP_HOLE.get()))
         .save(saver, BomuBomu.id("ship_hole").toString());
      Builder.advancement()
         .parent(pistol)
         .display(new ItemStack(Items.FIRE_CHARGE), title("mr_5"), desc("mr_5"), null, AdvancementType.CHALLENGE, true, true, false)
         .rewards(net.minecraft.advancements.AdvancementRewards.Builder.experience(300))
         .addCriterion("max_mastery", DevilFruitTrigger.TriggerInstance.of(DFTriggers.MAX_MASTERY.get(), BomuBomu.ID))
         .save(saver, BomuBomu.id("mr_5").toString());
   }

   private static Criterion<net.minecraft.advancements.critereon.PlayerTrigger.TriggerInstance> player(PlayerTrigger trigger) {
      return trigger.createCriterion(new net.minecraft.advancements.critereon.PlayerTrigger.TriggerInstance(Optional.empty()));
   }

   private static Component title(String name) {
      return Component.translatable("advancements.devilfruits.bomu_bomu." + name + ".title");
   }

   private static Component desc(String name) {
      return Component.translatable("advancements.devilfruits.bomu_bomu." + name + ".description");
   }

   @Override
   public void itemModels(DFItemModelProvider models) {
      models.fruitItem(BomuBomu.fruit(), (Item)BomuItems.FRUIT.get());
   }

   @Override
   public void damageTypes(BootstrapContext<DamageType> context) {
      context.register(BomuBomu.EXPLOSION_DAMAGE, new DamageType("devilfruits.bomu_bomu.explosion", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
   }

   @Override
   public void damageTypeTags(DFTagProviders.DamageTypeTagGen tags) {
      tags.damageType(DFTags.DEVIL_FRUIT_ATTACKS).add(BomuBomu.EXPLOSION_DAMAGE);
      tags.damageType(DamageTypeTags.IS_EXPLOSION).add(BomuBomu.EXPLOSION_DAMAGE);
      tags.damageType(DamageTypeTags.NO_KNOCKBACK).add(BomuBomu.EXPLOSION_DAMAGE);
   }
}
