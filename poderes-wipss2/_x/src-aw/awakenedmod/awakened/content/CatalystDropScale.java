package com.awakenedmod.awakened.content;

import com.awakenedmod.awakened.core.CatalystConfig;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;

public final class CatalystDropScale extends LootModifier {
   private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS = DeferredRegister.create(
      Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "awakened"
   );
   public static final MapCodec<CatalystDropScale> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, CatalystDropScale::new));

   public CatalystDropScale(LootItemCondition[] conditions) {
      super(conditions);
   }

   public static void register(IEventBus modBus) {
      SERIALIZERS.register(modBus);
   }

   protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
      double multiplier = CatalystConfig.dropMultiplier();
      if (multiplier == 1.0) {
         return loot;
      } else {
         RandomSource random = context.getRandom();
         ObjectArrayList<ItemStack> out = new ObjectArrayList(loot.size());
         ObjectListIterator var7 = loot.iterator();

         while (var7.hasNext()) {
            ItemStack stack = (ItemStack)var7.next();
            if (!(stack.getItem() instanceof CatalystItem) && !(stack.getItem() instanceof FocusedCatalystItem)) {
               out.add(stack);
            } else {
               int left = scaled(stack.getCount(), multiplier, random);

               while (left > 0) {
                  int take = Math.min(left, stack.getMaxStackSize());
                  out.add(stack.copyWithCount(take));
                  left -= take;
               }
            }
         }

         return out;
      }
   }

   private static int scaled(int count, double multiplier, RandomSource random) {
      double exact = (double)count * multiplier;
      int whole = (int)exact;
      return whole + (random.nextDouble() < exact - (double)whole ? 1 : 0);
   }

   public MapCodec<? extends IGlobalLootModifier> codec() {
      return CODEC;
   }

   static {
      SERIALIZERS.register("catalyst_drop_scale", () -> CODEC);
   }
}
