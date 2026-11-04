package com.awakenedmod.awakened.client.nominate;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.bus.api.IEventBus;

public final class NominationPauseButton {
   private static final String MODS_KEY = "fml.menu.mods";
   private static final int ROW = 24;
   private static final int WIDTH = 204;
   private static final int HEIGHT = 20;
   private static final int FROM_BOTTOM = 30;

   private NominationPauseButton() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(
         e -> {
            if (e.getScreen() instanceof PauseScreen pause && pause.showsPauseMenu() && NominationCampaign.linkVisible()) {
               AbstractWidget mods = modsButton(pause);
               int x = (pause.width - 204) / 2;
               int y = pause.height - 30;
               int w = 204;
               if (mods != null) {
                  x = mods.getX();
                  y = mods.getY();
                  w = mods.getWidth();

                  for (GuiEventListener child : pause.children()) {
                     if (child instanceof AbstractWidget) {
                        AbstractWidget widget = (AbstractWidget)child;
                        if (widget.getY() >= y) {
                           widget.setY(widget.getY() + 24);
                        }
                     }
                  }
               }

               e.addListener(
                  Button.builder(
                        Component.translatable("awakened.nominate.pause").withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}),
                        b -> NominationCommand.open()
                     )
                     .bounds(x, y, w, 20)
                     .tooltip(Tooltip.create(Component.translatable("awakened.nominate.pause_tip")))
                     .build()
               );
               return;
            }
         }
      );
   }

   private static AbstractWidget modsButton(PauseScreen pause) {
      for (GuiEventListener child : pause.children()) {
         if (child instanceof AbstractWidget widget
            && widget.getMessage().getContents() instanceof TranslatableContents t
            && "fml.menu.mods".equals(t.getKey())) {
            return widget;
         }
      }

      return null;
   }
}
