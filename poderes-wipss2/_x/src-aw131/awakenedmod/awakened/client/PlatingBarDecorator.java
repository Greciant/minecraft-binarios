package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.adamantium.AdamantiumPlating;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

public final class PlatingBarDecorator implements IItemDecorator {
   private static final int TRACK_X = 2;
   private static final int TRACK_Y = 11;
   private static final int TRACK_W = 13;
   private static final int SEGMENT_GAP = 2;
   private static final int SEGMENT_W = Math.max(1, 3);
   private static final int BAR_W = Math.min(13, 3 * SEGMENT_W + 4);
   private static final int COLOR_BACKDROP = -16777216;
   private static final int COLOR_PLATED = -3417882;
   private static final int COLOR_EMPTY = -11114634;
   private static final PlatingBarDecorator INSTANCE = new PlatingBarDecorator();

   private PlatingBarDecorator() {
   }

   public static void register(RegisterItemDecorationsEvent event) {
      for (Item item : BuiltInRegistries.ITEM) {
         event.register(item, INSTANCE);
      }
   }

   public boolean render(GuiGraphics g, Font font, ItemStack stack, int xOffset, int yOffset) {
      int rounds = AdamantiumPlating.rounds(stack);
      if (rounds <= 0) {
         return false;
      } else {
         int left = xOffset + 2;
         int top = yOffset + 11;
         fill(g, left, top, left + BAR_W, top + 2, -16777216);

         for (int i = 0; i < 3; i++) {
            int x = left + i * (SEGMENT_W + 2);
            if (x + SEGMENT_W > left + 13) {
               break;
            }

            fill(g, x, top, x + SEGMENT_W, top + 1, i < rounds ? -3417882 : -11114634);
         }

         return false;
      }
   }

   private static void fill(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
      g.fill(RenderType.guiOverlay(), x0, y0, x1, y1, color);
   }
}
