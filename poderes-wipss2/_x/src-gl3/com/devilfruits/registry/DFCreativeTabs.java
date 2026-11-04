package com.devilfruits.registry;

import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DFCreativeTabs {
   public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "devilfruits");
   private static final List<Supplier<? extends ItemLike>> EXTRAS = new ArrayList<>();
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(
      "main",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.devilfruits"))
            .withTabsBefore(new ResourceKey[]{CreativeModeTabs.SPAWN_EGGS})
            .icon(
               () -> FruitRegistry.playable()
                     .stream()
                     .flatMap(f -> f.item().stream())
                     .findFirst()
                     .<ItemStack>map(ItemStack::new)
                     .orElseGet(() -> new ItemStack((ItemLike)DFItems.SEASTONE_INGOT.get()))
            )
            .displayItems((params, output) -> {
               for (DevilFruit fruit : FruitRegistry.playable()) {
                  fruit.item().ifPresent(output::accept);
               }

               for (Supplier<? extends ItemLike> extra : EXTRAS) {
                  output.accept(extra.get());
               }

               output.accept((ItemLike)DFItems.CASKET.get());
               output.accept((ItemLike)DFItems.GOLD_KEY.get());
               output.accept((ItemLike)DFItems.SEASTONE_INGOT.get());
               output.accept((ItemLike)DFItems.SEASTONE_BLOCK.get());
               output.accept((ItemLike)DFItems.HAKI_SCROLL.get());
               output.accept((ItemLike)DFItems.HAKI_SCROLL_MASTER.get());
            })
            .build()
   );

   public static void addExtra(Supplier<? extends ItemLike> item) {
      EXTRAS.add(item);
   }

   private DFCreativeTabs() {
   }
}
