package com.awakenedmod.awakened.client.screen;

import com.awakenedmod.awakened.client.ClientPowerState;
import com.awakenedmod.awakened.content.summoning.SoulData;
import com.awakenedmod.awakened.content.summoning.SummonAttachments;
import com.awakenedmod.awakened.content.summoning.SummonSouls;
import com.awakenedmod.awakened.core.net.SoulMenuPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BlacklistScreen extends AwakenedScreen {
   private static final int PANEL_W = 300;
   private static final int PAD = 12;
   private static final int TITLE_SCALE_PCT = 150;
   private static final int HEADER_H = 30;
   private static final int ROW_H = 24;
   private static final int ROW_GAP = 3;
   private static final int SCREEN_MARGIN = 16;
   private static final int LINE = 10;
   private static final int BG = -435023338;
   private static final int TEXT = -1;
   private static final int DIM = -5723984;
   private static final int RED = -38037;
   private static final int RED_IDLE = -7714230;
   private static final int VIOLET = -5005861;
   private static final int CELL_BG = -1728053248;
   private static final int CELL_BORDER = -14671832;
   private static final int BTN_BG = -870704606;
   private static final int DORMANT = -6657188;
   private final List<BlacklistScreen.Row> rows = new ArrayList<>();
   private int scrollOffset;
   private boolean canScroll;
   private int maxScrollOffset;
   private int backX0;
   private int backY0;
   private int backX1;
   private int backY1;
   private Component pendingTooltip;
   private int pendingTooltipX;
   private int pendingTooltipY;

   public BlacklistScreen() {
      super(Component.translatable("awakened.souls.blacklist_title"));
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(new SoulMenuScreen());
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.render(g, mouseX, mouseY, partialTick);
      if (this.minecraft != null && this.minecraft.player != null) {
         int theme = ClientPowerState.get().awakened() ? -13049400 : -13056257;
         SoulData souls = (SoulData)this.minecraft.player.getData(SummonAttachments.SOULS);
         Set<String> blacklist = souls.blacklist();
         int textW = 276;
         int rowH = 27;
         int helpLines = this.font.split(Component.translatable("awakened.souls.blacklist_help"), textW).size();
         int footerH = 12 + helpLines * 10 + 8 + 10;
         int availList = this.height - 32 - 30 - footerH - 12;
         int visibleRows = Math.max(1, availList / rowH);
         int totalRows = Math.max(1, blacklist.size());
         this.maxScrollOffset = Math.max(0, totalRows - visibleRows);
         this.canScroll = this.maxScrollOffset > 0;
         this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScrollOffset);
         int listH = blacklist.isEmpty() ? 26 : Math.min(totalRows, visibleRows) * rowH;
         int panelH = 30 + listH + footerH + 12;
         int x = (this.width - 300) / 2;
         int y = Math.max(16, (this.height - panelH) / 2);
         g.fill(x - 1, y - 1, x + 300 + 1, y + panelH + 1, theme);
         g.fill(x, y, x + 300, y + panelH, -435023338);
         g.fill(x, y, x + 300, y + 30 - 4, theme & 16777215 | 637534208);
         g.fill(x, y, x + 300, y + 1, 1073741824);
         g.fill(x, y, x + 1, y + panelH, 1073741824);
         g.fill(x, y + panelH - 1, x + 300, y + panelH, 587202559);
         g.fill(x + 300 - 1, y, x + 300, y + panelH, 587202559);
         int cx = x + 12;
         int cy = y + 12;
         this.drawScaledTitle(g, cx, cy, theme);
         String count = String.valueOf(blacklist.size());
         g.drawString(this.font, count, x + 300 - 12 - this.font.width(count), cy + 3, -5723984);
         cy = y + 30;
         this.rows.clear();
         if (blacklist.isEmpty()) {
            int ey = cy + (listH - this.font.split(Component.translatable("awakened.souls.blacklist_empty"), textW).size() * 10) / 2;

            for (FormattedCharSequence line : this.font.split(Component.translatable("awakened.souls.blacklist_empty"), textW)) {
               g.drawString(this.font, line, cx, ey, -5723984);
               ey += 10;
            }

            cy += listH;
         } else {
            g.enableScissor(x, cy, x + 300, cy + listH);
            int i = 0;

            for (String type : blacklist) {
               if (i >= this.scrollOffset && i < this.scrollOffset + visibleRows) {
                  int rowY = cy + (i - this.scrollOffset) * rowH;
                  this.rows.add(this.drawRow(g, type, cx, rowY, x + 300 - 12, theme, mouseX, mouseY));
               }

               i++;
            }

            g.disableScissor();
            cy += listH;
         }

         cy += 6;
         g.fill(cx, cy, x + 300 - 12, cy + 1, 872415231);
         cy += 5;

         for (FormattedCharSequence line : this.font.split(Component.translatable("awakened.souls.blacklist_help"), textW)) {
            g.drawString(this.font, line, cx, cy, -5723984);
            cy += 10;
         }

         cy += 8;
         String backText = Component.translatable("awakened.souls.blacklist_back").getString();
         this.backX0 = cx - 2;
         this.backY0 = cy - 2;
         this.backX1 = x + 300 - 12;
         this.backY1 = cy + 10;
         boolean hoverBack = mouseX >= this.backX0 && mouseX <= this.backX1 && mouseY >= this.backY0 && mouseY <= this.backY1;
         g.fill(this.backX0 - 1, this.backY0 - 1, this.backX1 + 1, this.backY1 + 1, hoverBack ? theme : -14671832);
         g.fill(this.backX0, this.backY0, this.backX1, this.backY1, hoverBack ? -869651912 : -870704606);
         g.drawString(this.font, backText, cx, cy, hoverBack ? -1 : -5723984);
         this.flushTooltip(g);
      }
   }

   private void drawScaledTitle(GuiGraphics g, int x, int y, int color) {
      float scale = 1.5F;
      Component title = Component.translatable("awakened.souls.blacklist_title");
      g.pose().pushPose();
      g.pose().translate((float)x, (float)y, 0.0F);
      g.pose().scale(scale, scale, 1.0F);
      g.drawString(this.font, title, 0, 0, color);
      g.pose().popPose();
   }

   private BlacklistScreen.Row drawRow(GuiGraphics g, String type, int x0, int y0, int rightEdge, int theme, int mouseX, int mouseY) {
      int y1 = y0 + 24;
      boolean dormant = SummonSouls.isDormant(type);
      boolean hoverRow = mouseX >= x0 && mouseX <= rightEdge && mouseY >= y0 && mouseY <= y1;
      g.fill(x0 - 1, y0 - 1, rightEdge + 1, y1 + 1, hoverRow ? -5005861 : -14671832);
      g.fill(x0, y0, rightEdge, y1, hoverRow ? -870049229 : -1728053248);
      if (hoverRow) {
         g.fill(x0, y0, rightEdge, y0 + 1, 1090519039);
         g.fill(x0, y0, x0 + 1, y1, 1090519039);
      }

      g.fill(x0, y0, x0 + 2, y1, -5005861);
      g.renderItem(this.iconFor(type), x0 + 6, y0 + 4);
      String name = SummonSouls.displayName(type).getString();
      String removeText = Component.translatable("awakened.souls.blacklist_remove").getString();
      int removeX1 = rightEdge - 3;
      int removeX0 = removeX1 - this.font.width(removeText) - 8;
      int removeY0 = y0 + 6;
      int removeY1 = removeY0 + 11;
      String shortName = this.font.plainSubstrByWidth(name, removeX0 - (x0 + 26) - 4);
      g.drawString(this.font, shortName, x0 + 26, y0 + 7, dormant ? -6657188 : -1);
      boolean rowNameHover = hoverRow && mouseX < removeX0;
      this.tooltip(
         g, dormant ? Component.translatable("awakened.souls.dormant") : Component.literal(name), mouseX, mouseY, rowNameHover && !shortName.equals(name)
      );
      boolean hoverRemove = mouseX >= removeX0 && mouseX <= removeX1 && mouseY >= removeY0 && mouseY <= removeY1;
      g.fill(removeX0 - 1, removeY0 - 1, removeX1 + 1, removeY1 + 1, hoverRemove ? -38037 : -14671832);
      g.fill(removeX0, removeY0, removeX1, removeY1, hoverRemove ? -869656038 : -870704606);
      g.drawString(this.font, removeText, removeX0 + 4, removeY0 + 2, hoverRemove ? -38037 : -7714230);
      this.tooltip(g, Component.translatable("awakened.souls.tip_unban", new Object[]{SummonSouls.displayName(type)}), mouseX, mouseY, hoverRemove);
      return new BlacklistScreen.Row(type, y0, y1, removeX0, removeY0, removeX1, removeY1);
   }

   private void tooltip(GuiGraphics g, Component text, int mouseX, int mouseY, boolean hovering) {
      if (hovering) {
         this.pendingTooltip = text;
         this.pendingTooltipX = mouseX;
         this.pendingTooltipY = mouseY;
      }
   }

   private void flushTooltip(GuiGraphics g) {
      if (this.pendingTooltip != null) {
         g.renderTooltip(this.font, this.pendingTooltip, this.pendingTooltipX, this.pendingTooltipY);
         this.pendingTooltip = null;
      }
   }

   private ItemStack iconFor(String type) {
      EntityType<?> et = SummonSouls.type(type);
      SpawnEggItem egg = et != null ? SpawnEggItem.byId(et) : null;
      return egg != null ? new ItemStack(egg) : new ItemStack(Items.SKELETON_SKULL);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return this.handleClick((int)mouseX, (int)mouseY, button) ? true : super.mouseClicked(mouseX, mouseY, button);
   }

   private boolean handleClick(int mx, int my, int button) {
      if (button != 0) {
         return false;
      } else if (mx >= this.backX0 && mx <= this.backX1 && my >= this.backY0 && my <= this.backY1) {
         this.onClose();
         return true;
      } else {
         for (BlacklistScreen.Row row : this.rows) {
            if (mx >= row.removeX0() && mx <= row.removeX1() && my >= row.removeY0() && my <= row.removeY1()) {
               this.send(row.type());
               return true;
            }
         }

         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (scrollY != 0.0 && this.canScroll) {
         this.scrollOffset = Mth.clamp(this.scrollOffset - (int)Math.signum(scrollY), 0, this.maxScrollOffset);
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
   }

   private void send(String type) {
      SoulMenuPayload payload = new SoulMenuPayload(5, type, 0);
      PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
   }

   private static record Row(String type, int y0, int y1, int removeX0, int removeY0, int removeX1, int removeY1) {
   }
}
