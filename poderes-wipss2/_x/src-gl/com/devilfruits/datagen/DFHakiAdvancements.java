package com.devilfruits.datagen;

import com.devilfruits.DevilFruits;
import com.devilfruits.registry.DFItems;
import com.devilfruits.registry.DFTriggers;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Advancement.Builder;
import net.minecraft.advancements.critereon.PlayerTrigger.TriggerInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

final class DFHakiAdvancements {
   private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/block/blackstone.png");

   static void generate(Consumer<AdvancementHolder> saver) {
      AdvancementHolder root = Builder.advancement()
         .display(new ItemStack((ItemLike)DFItems.HAKI_SCROLL.get()), title("armament"), desc("armament"), BACKGROUND, AdvancementType.TASK, true, true, false)
         .addCriterion("unlocked", DFTriggers.HAKI_UNLOCKED.get().createCriterion(new TriggerInstance(Optional.empty())))
         .save(saver, DevilFruits.id("haki/armament").toString());
      AdvancementHolder logia = Builder.advancement()
         .parent(root)
         .display(new ItemStack(Items.NETHERITE_SWORD), title("logia"), desc("logia"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("logia_hit", DFTriggers.HAKI_LOGIA_HIT.get().createCriterion(new TriggerInstance(Optional.empty())))
         .save(saver, DevilFruits.id("haki/logia").toString());
      Builder.advancement()
         .parent(logia)
         .display(
            new ItemStack((ItemLike)DFItems.HAKI_SCROLL_MASTER.get()), title("master"), desc("master"), null, AdvancementType.CHALLENGE, true, true, false
         )
         .rewards(net.minecraft.advancements.AdvancementRewards.Builder.experience(500))
         .addCriterion("master", DFTriggers.HAKI_MASTER.get().createCriterion(new TriggerInstance(Optional.empty())))
         .save(saver, DevilFruits.id("haki/master").toString());
   }

   private static Component title(String name) {
      return Component.translatable("advancements.devilfruits.haki." + name + ".title");
   }

   private static Component desc(String name) {
      return Component.translatable("advancements.devilfruits.haki." + name + ".description");
   }

   private DFHakiAdvancements() {
   }
}
