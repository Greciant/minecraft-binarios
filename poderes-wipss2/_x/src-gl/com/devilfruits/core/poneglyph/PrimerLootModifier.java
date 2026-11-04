package com.devilfruits.core.poneglyph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class PrimerLootModifier extends LootModifier {
   public static final MapCodec<PrimerLootModifier> CODEC = RecordCodecBuilder.mapCodec(
      inst -> codecStart(inst).and(Codec.STRING.fieldOf("source").forGetter(m -> m.source)).apply(inst, PrimerLootModifier::new)
   );
   private final String source;

   public PrimerLootModifier(LootItemCondition[] conditions, String source) {
      super(conditions);
      this.source = source;
   }

   private double chance() {
      String var1 = this.source;

      return switch (var1) {
         case "shipwreck" -> PoneglyphConfig.PRIMER_SHIPWRECK.get();
         case "buried_treasure" -> PoneglyphConfig.PRIMER_TREASURE.get();
         case "underwater_ruin" -> PoneglyphConfig.PRIMER_UNDERWATER_RUIN.get();
         case "poneglyph_ruins" -> PoneglyphConfig.PRIMER_PONEGLYPH_RUINS.get();
         default -> 0.0;
      };
   }

   protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
      if (context.getRandom().nextDouble() < this.chance()) {
         generatedLoot.add(new ItemStack((ItemLike)Poneglyphs.PRIMER.get()));
      }

      return generatedLoot;
   }

   public MapCodec<? extends IGlobalLootModifier> codec() {
      return CODEC;
   }
}
