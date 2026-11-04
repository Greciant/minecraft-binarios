package com.devilfruits.fruits.doru_doru.client;

import com.devilfruits.client.input.DFKeys;
import com.devilfruits.fruits.doru_doru.DoruItems;
import com.devilfruits.fruits.doru_doru.atelier.DoruNetwork;
import com.devilfruits.fruits.doru_doru.item.WaxTools;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.network.PacketDistributor;

public class WaxToolWheelScreen extends Screen {
   private int hovered = -1;

   public WaxToolWheelScreen() {
      super(Component.translatable("screen.devilfruits.doru_doru.tools"));
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
      gg.fillGradient(0, 0, this.width, this.height, 805306368, 1342177280);
   }

   public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
      super.render(gg, mouseX, mouseY, partialTick);
      int cx = this.width / 2;
      int cy = this.height / 2;
      int n = this.entries();
      double dx = (double)(mouseX - cx);
      double dy = (double)(mouseY - cy);
      this.hovered = -1;
      if (dx * dx + dy * dy > 324.0) {
         double angle = Math.atan2(dy, dx) + (Math.PI / 2);
         this.hovered = Math.floorMod((int)Math.round(angle / ((Math.PI * 2) / (double)n)), n);
      }

      int r = 44;

      for (int i = 0; i < n; i++) {
         double a = (-Math.PI / 2) + (double)i * Math.PI * 2.0 / (double)n;
         int x = cx + (int)Math.round(Math.cos(a) * (double)r) - 12;
         int y = cy + (int)Math.round(Math.sin(a) * (double)r) - 12;
         gg.fill(x - 1, y - 1, x + 25, y + 25, i == this.hovered ? -1058487686 : -1876944366);
         gg.renderItem(icon(i), x + 4, y + 4);
      }

      if (this.hovered >= 0) {
         gg.drawCenteredString(
            this.font, Component.translatable(icon(this.hovered).getItem().getDescriptionId()).withStyle(ChatFormatting.BOLD), cx, cy - 4, 16777215
         );
      }
   }

   private int entries() {
      return WaxTools.Kind.values().length + (this.minecraft.player != null && WaxTools.hasMold(this.minecraft.player) ? 1 : 0);
   }

   private static ItemStack icon(int i) {
      return i == WaxTools.KEY_CHOICE ? new ItemStack((ItemLike)DoruItems.WAX_KEY.get()) : new ItemStack(WaxTools.item(WaxTools.Kind.values()[i]));
   }

   public void tick() {
      super.tick();
      Key key = DFKeys.USE.getKey();
      if (key.getType() == Type.KEYSYM && !InputConstants.isKeyDown(this.minecraft.getWindow().getWindow(), key.getValue())) {
         this.confirm();
      }
   }

   public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
      if (DFKeys.USE.matches(keyCode, scanCode)) {
         this.confirm();
         return true;
      } else {
         return super.keyReleased(keyCode, scanCode, modifiers);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.confirm();
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   private void confirm() {
      if (this.hovered >= 0) {
         PacketDistributor.sendToServer(new DoruNetwork.ToolChoicePayload(this.hovered), new CustomPacketPayload[0]);
      }

      this.onClose();
   }
}
