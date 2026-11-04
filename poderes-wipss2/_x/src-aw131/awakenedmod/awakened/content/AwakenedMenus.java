package com.awakenedmod.awakened.content;

import com.awakenedmod.awakened.content.relic.RelicBeltMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AwakenedMenus {
   private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "awakened");
   public static final DeferredHolder<MenuType<?>, MenuType<RelicBeltMenu>> RELIC_BELT = MENUS.register(
      "relic_belt", () -> IMenuTypeExtension.create((windowId, inv, data) -> new RelicBeltMenu(windowId, inv))
   );

   private AwakenedMenus() {
   }

   public static void register(IEventBus modEventBus) {
      MENUS.register(modEventBus);
   }
}
