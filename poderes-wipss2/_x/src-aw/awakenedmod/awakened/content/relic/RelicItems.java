package com.awakenedmod.awakened.content.relic;

import com.awakenedmod.awakened.core.relic.Relic;
import com.awakenedmod.awakened.core.relic.RelicRegistry;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister.Items;

public final class RelicItems {
   public static final String PREFIX = "relic_";
   private static Map<String, DeferredItem<RelicItem>> items = Map.of();

   private RelicItems() {
   }

   public static void registerAll(Items registry) {
      Map<String, DeferredItem<RelicItem>> registered = new LinkedHashMap<>();

      for (Relic relic : RelicRegistry.all()) {
         String relicId = relic.id();
         registered.put(
            relicId,
            registry.registerItem(
               "relic_" + relicId,
               properties -> new RelicItem(
                     properties.rarity(relic.rarity())
                        .stacksTo(16)
                        .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                        .component(DataComponents.DYED_COLOR, dyed(relic.themeColor())),
                     relicId
                  )
            )
         );
      }

      items = Collections.unmodifiableMap(registered);
   }

   private static DyedItemColor dyed(int themeColor) {
      return new DyedItemColor(themeColor & 16777215, false);
   }

   @Nullable
   public static RelicItem item(String relicId) {
      DeferredItem<RelicItem> holder = items.get(relicId);
      return holder == null ? null : (RelicItem)holder.get();
   }

   public static ItemStack stack(String relicId) {
      RelicItem item = item(relicId);
      return item == null ? ItemStack.EMPTY : new ItemStack(item);
   }
}
