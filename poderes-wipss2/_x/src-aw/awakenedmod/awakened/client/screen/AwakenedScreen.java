package com.awakenedmod.awakened.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class AwakenedScreen extends Screen {
   protected AwakenedScreen(Component title) {
      super(title);
   }

   protected void renderBlurredBackground(float partialTick) {
   }
}
