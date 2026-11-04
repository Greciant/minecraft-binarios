package com.devilfruits.fruits.suke_suke.datagen;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.item.AbilityIconItem;
import com.devilfruits.core.trigger.DevilFruitTrigger;
import com.devilfruits.datagen.DFItemModelProvider;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.suke_suke.SukeAbilities;
import com.devilfruits.fruits.suke_suke.SukeItems;
import com.devilfruits.fruits.suke_suke.SukeSuke;
import com.devilfruits.fruits.suke_suke.SukeTriggers;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class SukeSukeData implements FruitDataContributor {
   private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/block/light_blue_stained_glass.png");

   @Override
   public void itemModels(DFItemModelProvider models) {
      models.fruitItem(SukeSuke.fruit(), (Item)SukeItems.FRUIT.get()).renderType("translucent");
   }

   @Override
   public void advancements(Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper efh) {
      AdvancementHolder root = Builder.advancement()
         .display(
            new ItemStack((ItemLike)SukeItems.FRUIT.get()), title("clear_clear"), desc("clear_clear"), BACKGROUND, AdvancementType.TASK, true, true, false
         )
         .addCriterion("ate_fruit", DevilFruitTrigger.TriggerInstance.of(DFTriggers.ATE_FRUIT.get(), SukeSuke.ID))
         .save(saver, SukeSuke.id("clear_clear").toString());
      Builder.advancement()
         .parent(root)
         .display(icon(SukeAbilities.INVISIBILITY), title("vanish"), desc("vanish"), null, AdvancementType.TASK, true, true, false)
         .addCriterion("vanish", player(SukeTriggers.VANISH.get()))
         .save(saver, SukeSuke.id("vanish").toString());
      Builder.advancement()
         .parent(root)
         .display(icon(SukeAbilities.GROUP_INVISIBILITY), title("shared"), desc("shared"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("shared", player(SukeTriggers.SHARED.get()))
         .save(saver, SukeSuke.id("shared").toString());
      Builder.advancement()
         .parent(root)
         .display(icon(SukeAbilities.SHIP_INVISIBILITY), title("ghost_ship"), desc("ghost_ship"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("ghost_ship", player(SukeTriggers.GHOST_SHIP.get()))
         .save(saver, SukeSuke.id("ghost_ship").toString());
      Builder.advancement()
         .parent(root)
         .display(icon(SukeAbilities.INVISIBILITY), title("invisible_man"), desc("invisible_man"), null, AdvancementType.CHALLENGE, true, true, false)
         .rewards(net.minecraft.advancements.AdvancementRewards.Builder.experience(300))
         .addCriterion("max_mastery", DevilFruitTrigger.TriggerInstance.of(DFTriggers.MAX_MASTERY.get(), SukeSuke.ID))
         .save(saver, SukeSuke.id("invisible_man").toString());
   }

   private static ItemStack icon(Ability ability) {
      return AbilityIconItem.of(ability);
   }

   private static Criterion<TriggerInstance> player(PlayerTrigger trigger) {
      return trigger.createCriterion(new TriggerInstance(Optional.empty()));
   }

   private static Component title(String name) {
      return Component.translatable("advancements.devilfruits.suke_suke." + name + ".title");
   }

   private static Component desc(String name) {
      return Component.translatable("advancements.devilfruits.suke_suke." + name + ".description");
   }
}
