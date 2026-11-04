package com.devilfruits.fruits.supa_supa.datagen;

import com.devilfruits.core.item.AbilityIconItem;
import com.devilfruits.core.trigger.DevilFruitTrigger;
import com.devilfruits.datagen.DFItemModelProvider;
import com.devilfruits.datagen.DFTagProviders;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.supa_supa.SupaAbilities;
import com.devilfruits.fruits.supa_supa.SupaItems;
import com.devilfruits.fruits.supa_supa.SupaSupa;
import com.devilfruits.fruits.supa_supa.SupaTriggers;
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
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class SupaSupaData implements FruitDataContributor {
   private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");

   @Override
   public void advancements(Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper efh) {
      AdvancementHolder root = Builder.advancement()
         .display(
            new ItemStack((ItemLike)SupaItems.FRUIT.get()), title("blade_blade"), desc("blade_blade"), BACKGROUND, AdvancementType.TASK, true, true, false
         )
         .addCriterion("ate_fruit", DevilFruitTrigger.TriggerInstance.of(DFTriggers.ATE_FRUIT.get(), SupaSupa.ID))
         .save(saver, SupaSupa.id("blade_blade").toString());
      Builder.advancement()
         .parent(root)
         .display(AbilityIconItem.of(SupaAbilities.STEEL_BODY), title("steel_wall"), desc("steel_wall"), null, AdvancementType.TASK, true, true, false)
         .addCriterion("steel_wall", player(SupaTriggers.STEEL_WALL.get()))
         .save(saver, SupaSupa.id("steel_wall").toString());
      AdvancementHolder ship = Builder.advancement()
         .parent(root)
         .display(AbilityIconItem.of(SupaAbilities.SUPER_SLASH), title("ship_in_two"), desc("ship_in_two"), null, AdvancementType.CHALLENGE, true, true, false)
         .rewards(net.minecraft.advancements.AdvancementRewards.Builder.experience(200))
         .addCriterion("ship_in_two", player(SupaTriggers.SHIP_IN_TWO.get()))
         .save(saver, SupaSupa.id("ship_in_two").toString());
      Builder.advancement()
         .parent(ship)
         .display(new ItemStack(Items.NETHERITE_SWORD), title("mr_one"), desc("mr_one"), null, AdvancementType.CHALLENGE, true, true, false)
         .rewards(net.minecraft.advancements.AdvancementRewards.Builder.experience(300))
         .addCriterion("max_mastery", DevilFruitTrigger.TriggerInstance.of(DFTriggers.MAX_MASTERY.get(), SupaSupa.ID))
         .save(saver, SupaSupa.id("mr_one").toString());
   }

   private static Criterion<TriggerInstance> player(PlayerTrigger trigger) {
      return trigger.createCriterion(new TriggerInstance(Optional.empty()));
   }

   private static Component title(String name) {
      return Component.translatable("advancements.devilfruits.supa_supa." + name + ".title");
   }

   private static Component desc(String name) {
      return Component.translatable("advancements.devilfruits.supa_supa." + name + ".description");
   }

   @Override
   public void itemModels(DFItemModelProvider models) {
      models.fruitItem(SupaSupa.fruit(), (Item)SupaItems.FRUIT.get());
   }

   @Override
   public void damageTypes(BootstrapContext<DamageType> context) {
      context.register(SupaSupa.BLADE_DAMAGE, new DamageType("devilfruits.supa_supa.blade", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
   }

   @Override
   public void damageTypeTags(DFTagProviders.DamageTypeTagGen tags) {
      tags.damageType(DFTags.DEVIL_FRUIT_ATTACKS).add(SupaSupa.BLADE_DAMAGE);
   }
}
