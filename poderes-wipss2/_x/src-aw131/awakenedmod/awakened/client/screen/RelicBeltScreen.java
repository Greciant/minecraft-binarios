package com.awakenedmod.awakened.client.screen;

import com.awakenedmod.awakened.content.relic.RelicBeltMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class RelicBeltScreen extends AbstractContainerScreen<RelicBeltMenu> {
   public static boolean returnToCharacterScreen = false;

   public RelicBeltScreen(RelicBeltMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.imageWidth = 176;
      this.imageHeight = 166;
      this.inventoryLabelY = this.imageHeight - 94;
   }

   public void onClose() {
      boolean returnToCharacter = returnToCharacterScreen;
      returnToCharacterScreen = false;
      super.onClose();
      if (returnToCharacter && this.minecraft != null) {
         this.minecraft.setScreen(new CharacterScreen(4));
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.render(g, mouseX, mouseY, partialTick);
      this.renderTooltip(g, mouseX, mouseY);
   }

   protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
      int x = this.leftPos;
      int y = this.topPos;
      g.fill(x, y, x + this.imageWidth, y + this.imageHeight, -266726874);
      g.fill(x + 3, y + 3, x + this.imageWidth - 3, y + this.imageHeight - 3, -14017731);
      g.fill(x + 6, y + 16, x + this.imageWidth - 6, y + 42, -14805968);

      for (Slot slot : ((RelicBeltMenu)this.menu).slots) {
         int sx = x + slot.x;
         int sy = y + slot.y;
         g.fill(sx - 1, sy - 1, sx + 17, sy + 17, -15988716);
         g.fill(sx, sy, sx + 16, sy + 16, -15396833);
      }
   }
}
