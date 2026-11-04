package com.devilfruits.core.gacha;

import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.registry.DFItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class CasketLootModifier extends LootModifier {
   public static final MapCodec<CasketLootModifier> CODEC = RecordCodecBuilder.mapCodec(
      inst -> codecStart(inst)
            .and(Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(m -> m.chance))
            .and(Codec.BOOL.optionalFieldOf("replace", false).forGetter(m -> m.replace))
            .apply(inst, CasketLootModifier::new)
   );
   public static final Map<String, Float> DEFAULT_CHANCES = defaults();
   public static final List<String> REPLACING = List.of("minecraft:archaeology/desert_well", "minecraft:archaeology/desert_pyramid");
   private final float chance;
   private final boolean replace;
   @Nullable
   private static List<? extends String> parsedFrom;
   private static Map<ResourceLocation, Float> parsed = Map.of();

   private static Map<String, Float> defaults() {
      Map<String, Float> m = new LinkedHashMap<>();
      m.put("minecraft:chests/shipwreck_treasure", 0.04F);
      m.put("minecraft:chests/buried_treasure", 0.06F);
      m.put("minecraft:chests/desert_pyramid", 0.04F);
      m.put("minecraft:chests/jungle_temple", 0.05F);
      m.put("minecraft:chests/simple_dungeon", 0.04F);
      m.put("minecraft:chests/pillager_outpost", 0.04F);
      m.put("minecraft:chests/abandoned_mineshaft", 0.02F);
      m.put("minecraft:chests/stronghold_library", 0.12F);
      m.put("minecraft:chests/woodland_mansion", 0.15F);
      m.put("minecraft:chests/trial_chambers/reward", 0.06F);
      m.put("minecraft:chests/trial_chambers/reward_ominous", 0.12F);
      m.put("minecraft:chests/ancient_city", 0.1F);
      m.put("minecraft:chests/bastion_treasure", 0.15F);
      m.put("minecraft:chests/end_city_treasure", 0.1F);
      m.put("minecraft:chests/nether_bridge", 0.06F);
      m.put("minecraft:archaeology/desert_well", 0.03F);
      m.put("minecraft:archaeology/desert_pyramid", 0.02F);
      return Collections.unmodifiableMap(m);
   }

   public CasketLootModifier(LootItemCondition[] conditions, float chance, boolean replace) {
      super(conditions);
      this.chance = chance;
      this.replace = replace;
   }

   protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
      if (!(Boolean)CoreConfig.CASKET_LOOT.get()) {
         return generatedLoot;
      } else {
         ObjectListIterator configured = generatedLoot.iterator();

         while (configured.hasNext()) {
            ItemStack stack = (ItemStack)configured.next();
            if (stack.is((Item)DFItems.CASKET.get())) {
               return generatedLoot;
            }
         }

         Float configuredx = tableChances().get(context.getQueriedLootTableId());
         float effective = (float)((double)(configuredx != null ? configuredx : this.chance) * (Double)CoreConfig.LOOT_CHANCE_MULTIPLIER.get());
         if (context.getRandom().nextFloat() >= effective) {
            return generatedLoot;
         } else {
            if (this.replace) {
               generatedLoot.clear();
            }

            generatedLoot.add(new ItemStack((ItemLike)DFItems.CASKET.get()));
            return generatedLoot;
         }
      }
   }

   public MapCodec<? extends IGlobalLootModifier> codec() {
      return CODEC;
   }

   public static synchronized Map<ResourceLocation, Float> tableChances() {
      List<? extends String> entries = (List<? extends String>)CoreConfig.CASKET_TABLE_CHANCES.get();
      if (entries != parsedFrom) {
         Map<ResourceLocation, Float> map = new HashMap<>();

         for (String entry : entries) {
            Entry<ResourceLocation, Float> e = parseEntry(entry);
            if (e != null) {
               map.put(e.getKey(), e.getValue());
            }
         }

         parsed = map;
         parsedFrom = entries;
      }

      return parsed;
   }

   @Nullable
   public static Entry<ResourceLocation, Float> parseEntry(String entry) {
      int eq = entry.lastIndexOf(61);
      if (eq <= 0) {
         return null;
      } else {
         ResourceLocation table = ResourceLocation.tryParse(entry.substring(0, eq).trim());

         try {
            float value = Float.parseFloat(entry.substring(eq + 1).trim());
            return table != null && !(value < 0.0F) && !(value > 1.0F) ? Map.entry(table, value) : null;
         } catch (NumberFormatException var4) {
            return null;
         }
      }
   }

   public static List<String> defaultChanceEntries() {
      List<String> out = new ArrayList<>();
      DEFAULT_CHANCES.forEach((table, value) -> out.add(table + "=" + value));
      return out;
   }
}
