package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.screen.RadialWheelScreen;
import com.awakenedmod.awakened.core.power.PassiveToggle;
import com.awakenedmod.awakened.core.power.Power;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class PassiveToggleWheel {
   private PassiveToggleWheel() {
   }

   public static RadialWheelScreen create(Power power) {
      List<PassiveToggle.Wedge> wedges = power.secondaryKeyWheel();
      List<RadialWheelScreen.RadialOption> options = new ArrayList<>();

      for (int i = 0; i < wedges.size(); i++) {
         PassiveToggle.Wedge wedge = wedges.get(i);
         boolean on = PassiveToggleClient.enabled(wedge.toggle());
         options.add(
            new RadialWheelScreen.RadialOption(
               wedge.texture(),
               power.themeColor(),
               Component.translatable(wedge.toggle().nameKey()),
               false,
               null,
               on,
               i,
               Component.translatable(on ? "awakened.wheel.toggle_on" : "awakened.wheel.toggle_off"),
               true,
               wedge.item() != null ? new ItemStack(wedge.item()) : ItemStack.EMPTY
            )
         );
      }

      return new RadialWheelScreen(
         Component.translatable("awakened.wheel.toggles"),
         options,
         index -> PassiveToggleClient.toggle(wedges.get(index).toggle()),
         AwakenedKeybinds.SECONDARY_ABILITY,
         -1
      );
   }
}
