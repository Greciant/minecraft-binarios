package com.devilfruits.fruits.horo_horo.datagen;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.item.AbilityIconItem;
import com.devilfruits.core.trigger.DevilFruitTrigger;
import com.devilfruits.datagen.DFItemModelProvider;
import com.devilfruits.datagen.DFTagProviders;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.horo_horo.HoroAbilities;
import com.devilfruits.fruits.horo_horo.HoroHoro;
import com.devilfruits.fruits.horo_horo.HoroItems;
import com.devilfruits.fruits.horo_horo.HoroTriggers;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class HoroHoroData implements FruitDataContributor {
   private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/block/pink_concrete_powder.png");

   @Override
   public void itemModels(DFItemModelProvider models) {
      models.fruitItem(HoroHoro.fruit(), (Item)HoroItems.FRUIT.get());
   }

   @Override
   public void damageTypes(BootstrapContext<DamageType> context) {
      context.register(HoroHoro.HOLLOW_DAMAGE, new DamageType("devilfruits.horo_horo.hollow", 0.1F));
      context.register(HoroHoro.BLAST_DAMAGE, new DamageType("devilfruits.horo_horo.blast", 0.1F));
   }

   @Override
   public void damageTypeTags(DFTagProviders.DamageTypeTagGen tags) {
      tags.damageType(DFTags.DEVIL_FRUIT_ATTACKS).add(new ResourceKey[]{HoroHoro.HOLLOW_DAMAGE, HoroHoro.BLAST_DAMAGE});
      tags.damageType(DamageTypeTags.IS_EXPLOSION).add(HoroHoro.BLAST_DAMAGE);
      tags.damageType(DamageTypeTags.NO_KNOCKBACK).add(HoroHoro.BLAST_DAMAGE);
   }

   @Override
   public void advancements(Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper efh) {
      AdvancementHolder root = Builder.advancement()
         .display(
            new ItemStack((ItemLike)HoroItems.FRUIT.get()), title("hollow_hollow"), desc("hollow_hollow"), BACKGROUND, AdvancementType.TASK, true, true, false
         )
         .addCriterion("ate_fruit", DevilFruitTrigger.TriggerInstance.of(DFTriggers.ATE_FRUIT.get(), HoroHoro.ID))
         .save(saver, HoroHoro.id("hollow_hollow").toString());
      Builder.advancement()
         .parent(root)
         .display(icon(HoroAbilities.NEGATIVE_HOLLOW), title("negative"), desc("negative"), null, AdvancementType.TASK, true, true, false)
         .addCriterion("negative", player(HoroTriggers.NEGATIVE.get()))
         .save(saver, HoroHoro.id("negative").toString());
      Builder.advancement()
         .parent(root)
         .display(icon(HoroAbilities.MINI_HOLLOW), title("ghost_rap"), desc("ghost_rap"), null, AdvancementType.TASK, true, true, false)
         .addCriterion("ghost_rap", player(HoroTriggers.GHOST_RAP.get()))
         .save(saver, HoroHoro.id("ghost_rap").toString());
      Builder.advancement()
         .parent(root)
         .display(icon(HoroAbilities.GHOST_FORM), title("ghost_form"), desc("ghost_form"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("ghost_form", player(HoroTriggers.GHOST_FORM.get()))
         .save(saver, HoroHoro.id("ghost_form").toString());
      Builder.advancement()
         .parent(root)
         .display(icon(HoroAbilities.SPECIAL_HOLLOW), title("kamikaze"), desc("kamikaze"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("kamikaze", player(HoroTriggers.KAMIKAZE.get()))
         .save(saver, HoroHoro.id("kamikaze").toString());
      Builder.advancement()
         .parent(root)
         .display(icon(HoroAbilities.SPECIAL_HOLLOW), title("ghost_princess"), desc("ghost_princess"), null, AdvancementType.CHALLENGE, true, true, false)
         .rewards(net.minecraft.advancements.AdvancementRewards.Builder.experience(300))
         .addCriterion("max_mastery", DevilFruitTrigger.TriggerInstance.of(DFTriggers.MAX_MASTERY.get(), HoroHoro.ID))
         .save(saver, HoroHoro.id("ghost_princess").toString());
   }

   private static ItemStack icon(Ability ability) {
      return AbilityIconItem.of(ability);
   }

   private static Criterion<TriggerInstance> player(PlayerTrigger trigger) {
      return trigger.createCriterion(new TriggerInstance(Optional.empty()));
   }

   private static Component title(String name) {
      return Component.translatable("advancements.devilfruits.horo_horo." + name + ".title");
   }

   private static Component desc(String name) {
      return Component.translatable("advancements.devilfruits.horo_horo." + name + ".description");
   }
}
