package com.devilfruits.datagen;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.poneglyph.Poneglyphs;
import com.devilfruits.core.seastone.SeastoneGear;
import com.devilfruits.core.trigger.DevilFruitTrigger;
import com.devilfruits.registry.DFItems;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Advancement.Builder;
import net.minecraft.advancements.critereon.PlayerTrigger.TriggerInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class DFPoneglyphAdvancements {
   private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/block/polished_blackstone.png");

   static void generate(Consumer<AdvancementHolder> saver) {
      AdvancementHolder root = Builder.advancement()
         .display(
            new ItemStack((ItemLike)Poneglyphs.PONEGLYPH_FACE_ITEM.get()), title("root"), desc("root"), BACKGROUND, AdvancementType.TASK, true, true, false
         )
         .addCriterion("rubbing", Poneglyphs.FIRST_RUBBING.get().createCriterion(new TriggerInstance(Optional.empty())))
         .save(saver, DevilFruits.id("poneglyph/root").toString());
      AdvancementHolder deciphered = Builder.advancement()
         .parent(root)
         .display(
            new ItemStack((ItemLike)Poneglyphs.DECIPHERED_BLUEPRINT.get()),
            title("deciphered"),
            desc("deciphered"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion("deciphered", Poneglyphs.DECIPHERED_TRIGGER.get().createCriterion(new TriggerInstance(Optional.empty())))
         .save(saver, DevilFruits.id("poneglyph/deciphered").toString());
      Builder.advancement()
         .parent(deciphered)
         .display(new ItemStack((ItemLike)Poneglyphs.PRIMER.get()), title("literate"), desc("literate"), null, AdvancementType.CHALLENGE, true, true, false)
         .addCriterion("literate", Poneglyphs.LITERACY_MAX.get().createCriterion(new TriggerInstance(Optional.empty())))
         .save(saver, DevilFruits.id("poneglyph/literate").toString());
      AdvancementHolder refining = Builder.advancement()
         .parent(deciphered)
         .display(new ItemStack((ItemLike)DFItems.SEASTONE_INGOT.get()), title("refining"), desc("refining"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion("refining", DevilFruitTrigger.TriggerInstance.of(Poneglyphs.KNOWLEDGE_LEARNED.get(), DevilFruits.id("seastone_refining")))
         .save(saver, DevilFruits.id("poneglyph/refining").toString());
      Builder.advancement()
         .parent(refining)
         .display(new ItemStack((ItemLike)SeastoneGear.CHESTPLATE.get()), title("full_set"), desc("full_set"), null, AdvancementType.GOAL, true, true, false)
         .addCriterion(
            "full_set",
            net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(
               new ItemLike[]{
                  (ItemLike)SeastoneGear.HELMET.get(),
                  (ItemLike)SeastoneGear.CHESTPLATE.get(),
                  (ItemLike)SeastoneGear.LEGGINGS.get(),
                  (ItemLike)SeastoneGear.BOOTS.get()
               }
            )
         )
         .save(saver, DevilFruits.id("poneglyph/full_set").toString());
   }

   private static Component title(String name) {
      return Component.translatable("advancements.devilfruits.poneglyph." + name + ".title");
   }

   private static Component desc(String name) {
      return Component.translatable("advancements.devilfruits.poneglyph." + name + ".description");
   }

   private DFPoneglyphAdvancements() {
   }
}
