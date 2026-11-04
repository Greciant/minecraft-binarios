package com.devilfruits.datagen;

import com.devilfruits.DevilFruits;
import com.devilfruits.mobs.pirate.Pirates;
import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.Advancement.Builder;
import net.minecraft.advancements.AdvancementRequirements.Strategy;
import net.minecraft.advancements.critereon.KilledTrigger.TriggerInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class DFPirateAdvancements {
   private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/block/dark_oak_planks.png");

   static void generate(Consumer<AdvancementHolder> saver) {
      AdvancementHolder root = Builder.advancement()
         .display(new ItemStack((ItemLike)Pirates.CUTLASS.get()), title("root"), desc("root"), BACKGROUND, AdvancementType.TASK, true, true, false)
         .addCriterion("deckhand", killed(Pirates.DECKHAND.get()))
         .addCriterion("gunner", killed(Pirates.GUNNER.get()))
         .addCriterion("captain", killed(Pirates.CAPTAIN.get()))
         .requirements(Strategy.OR)
         .save(saver, DevilFruits.id("pirates/root").toString());
      Builder.advancement()
         .parent(root)
         .display(
            new ItemStack((ItemLike)Pirates.FLINTLOCK_PISTOL.get()), title("shot_first"), desc("shot_first"), null, AdvancementType.TASK, true, true, false
         )
         .addCriterion(
            "shot_first",
            TriggerInstance.playerKilledEntity(
               net.minecraft.advancements.critereon.EntityPredicate.Builder.entity().of(Pirates.GUNNER.get()),
               net.minecraft.advancements.critereon.DamageSourcePredicate.Builder.damageType()
                  .direct(net.minecraft.advancements.critereon.EntityPredicate.Builder.entity().of(Pirates.MUSKET_BALL_ENTITY.get()))
            )
         )
         .save(saver, DevilFruits.id("pirates/shot_first").toString());
      AdvancementHolder captain = Builder.advancement()
         .parent(root)
         .display(new ItemStack((ItemLike)Pirates.CAPTAIN_CUTLASS.get()), title("captain"), desc("captain"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("captain", killed(Pirates.CAPTAIN.get()))
         .save(saver, DevilFruits.id("pirates/captain").toString());
      Builder.advancement()
         .parent(captain)
         .display(
            new ItemStack((ItemLike)Pirates.JOLLY_ROGER.get()), title("jolly_roger"), desc("jolly_roger"), null, AdvancementType.CHALLENGE, true, true, false
         )
         .addCriterion(
            "jolly_roger",
            net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(new ItemLike[]{(ItemLike)Pirates.JOLLY_ROGER.get()})
         )
         .save(saver, DevilFruits.id("pirates/jolly_roger").toString());
   }

   private static Criterion<TriggerInstance> killed(EntityType<?> type) {
      return TriggerInstance.playerKilledEntity(net.minecraft.advancements.critereon.EntityPredicate.Builder.entity().of(type));
   }

   private static Component title(String name) {
      return Component.translatable("advancements.devilfruits.pirates." + name + ".title");
   }

   private static Component desc(String name) {
      return Component.translatable("advancements.devilfruits.pirates." + name + ".description");
   }

   private DFPirateAdvancements() {
   }
}
