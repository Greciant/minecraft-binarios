package com.awakenedmod.awakened.client.screen;

import com.awakenedmod.awakened.client.AbilityIcons;
import com.awakenedmod.awakened.core.net.TradeActionPayload;
import com.awakenedmod.awakened.core.net.TradeScreenPayload;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import java.util.List;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PowerTradeScreen extends AwakenedScreen {
   private static final int PANEL_W = 300;
   private static final int PANEL_H = 186;
   private static final int ICON = 32;
   private static final int COLUMN_OFFSET = 72;
   private static final int BUTTON_W = 120;
   private static final int BUTTON_H = 20;
   private static final int COLOR_PANEL = -267710701;
   private static final int COLOR_PANEL_EDGE = -4667692;
   private static final int COLOR_TITLE = -1511692;
   private static final int COLOR_DIM = -8483943;
   private static final int COLOR_WARN = -38053;
   private static final int COLOR_OK = -8595844;
   private TradeScreenPayload state;
   private Power give;
   private Power get;
   private long armedAt;
   private boolean pressed;
   private boolean closing;
   private Button primary;

   public static void apply(TradeScreenPayload payload) {
      Minecraft mc = Minecraft.getInstance();
      PowerTradeScreen open = mc.screen instanceof PowerTradeScreen screen ? screen : null;
      if (payload.stage() == 3) {
         if (open != null) {
            open.closing = true;
            open.onClose();
         }
      } else if (PowerRegistry.get(payload.givePower()) != null && PowerRegistry.get(payload.getPower()) != null) {
         if (open != null && open.state.stage() != 0 && open.state.partner().equals(payload.partner())) {
            open.update(payload);
         } else {
            mc.setScreen(new PowerTradeScreen(payload));
         }
      } else {
         if (payload.stage() != 0) {
            send(3, payload.partner());
         }
      }
   }

   private PowerTradeScreen(TradeScreenPayload payload) {
      super(Component.translatable("awakened.trade.title"));
      this.accept(payload);
   }

   private void accept(TradeScreenPayload payload) {
      this.state = payload;
      this.give = PowerRegistry.get(payload.givePower());
      this.get = PowerRegistry.get(payload.getPower());
      this.pressed = false;
   }

   private void update(TradeScreenPayload payload) {
      boolean newStep = payload.stage() != this.state.stage();
      this.accept(payload);
      if (newStep) {
         this.armedAt = Util.getMillis() + (long)payload.armTicks() * 50L;
      }
   }

   protected void init() {
      if (this.armedAt == 0L) {
         this.armedAt = Util.getMillis() + (long)this.state.armTicks() * 50L;
      }

      int by = this.top() + 186 - 20 - 10;
      this.primary = (Button)this.addRenderableWidget(
         Button.builder(Component.empty(), b -> this.pressPrimary()).bounds(this.width / 2 - 120 - 4, by, 120, 20).build()
      );
      this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> this.onClose()).bounds(this.width / 2 + 4, by, 120, 20).build());
      this.refreshPrimary();
   }

   private int top() {
      return (this.height - 186) / 2;
   }

   private void refreshPrimary() {
      Component label = Component.translatable(switch (this.state.stage()) {
         case 0 -> "awakened.trade.send";
         case 1 -> "awakened.trade.accept";
         default -> "awakened.trade.confirm";
      });
      long left = this.armedAt - Util.getMillis();
      boolean done = this.pressed || this.state.youReady();
      if (done) {
         label = Component.translatable("awakened.trade.waiting_button");
      } else if (left > 0L) {
         label = Component.translatable("awakened.trade.countdown", new Object[]{label, (left + 999L) / 1000L});
      }

      this.primary.setMessage(label);
      this.primary.active = !done && left <= 0L;
   }

   private void pressPrimary() {
      if (!this.pressed && Util.getMillis() >= this.armedAt) {
         this.pressed = true;
         switch (this.state.stage()) {
            case 0:
               send(0, this.state.partner());
               this.closing = true;
               this.onClose();
               break;
            case 1:
               send(1, this.state.partner());
               break;
            default:
               send(2, this.state.partner());
         }
      }
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(g, mouseX, mouseY, partialTick);
      int x0 = (this.width - 300) / 2;
      int y0 = this.top();
      int edge = this.state.stage() == 2 ? -38053 : -4667692;
      g.fill(x0 - 1, y0 - 1, x0 + 300 + 1, y0 + 186 + 1, edge);
      g.fill(x0, y0, x0 + 300, y0 + 186, -267710701);
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.refreshPrimary();
      super.render(g, mouseX, mouseY, partialTick);
      int cx = this.width / 2;
      int y0 = this.top();
      boolean last = this.state.stage() == 2;

      Component title = switch (this.state.stage()) {
         case 0 -> Component.translatable("awakened.trade.title.propose", new Object[]{this.state.partnerName()});
         case 1 -> Component.translatable("awakened.trade.title.review", new Object[]{this.state.partnerName()});
         default -> Component.translatable("awakened.trade.title.final");
      };
      g.drawCenteredString(this.font, title, cx, y0 + 10, last ? -38053 : -1511692);
      this.drawSide(g, cx - 72, y0 + 28, "awakened.trade.you_give", this.give, this.state.giveMastery());
      this.drawSide(g, cx + 72, y0 + 28, "awakened.trade.you_get", this.get, this.state.getMastery());

      Component info = switch (this.state.stage()) {
         case 0 -> Component.translatable("awakened.trade.info.propose");
         case 1 -> Component.translatable("awakened.trade.info.review");
         default -> Component.translatable("awakened.trade.info.final", new Object[]{this.give.displayName()});
      };
      List<FormattedCharSequence> lines = GuiPanels.clipLines(this.font, info, 276, 2);

      for (int i = 0; i < lines.size(); i++) {
         g.drawCenteredString(this.font, lines.get(i), cx, y0 + 111 + i * 10, last ? -38053 : -8483943);
      }

      if (this.state.stage() != 0) {
         boolean ready = this.state.partnerReady();
         g.drawCenteredString(
            this.font,
            Component.translatable(ready ? "awakened.trade.partner_ready" : "awakened.trade.partner_waiting", new Object[]{this.state.partnerName()}),
            cx,
            y0 + 137,
            ready ? -8595844 : -8483943
         );
      }
   }

   private void drawSide(GuiGraphics g, int cx, int y, String headingKey, Power power, int mastery) {
      g.drawCenteredString(this.font, Component.translatable(headingKey), cx, y, -8483943);
      int rgb = power.themeColor() & 16777215;
      int ix = cx - 16;
      int iy = y + 13;
      g.fill(ix - 4, iy - 4, ix + 32 + 4, iy + 32 + 4, 1073741824 | rgb);
      g.fill(ix - 4, iy - 4, ix + 32 + 4, iy - 2, -872415232 | rgb);
      AbilityIcons.draw(g, power, ix, iy, 32);
      g.drawCenteredString(this.font, power.displayName(), cx, iy + 32 + 8, 0xFF000000 | rgb);
      g.drawCenteredString(this.font, Component.translatable("awakened.trade.mastery", new Object[]{mastery}), cx, iy + 32 + 20, -8483943);
   }

   public void removed() {
      if (!this.closing && this.state.stage() != 0) {
         send(3, this.state.partner());
      }

      this.closing = true;
      super.removed();
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static void send(int action, UUID partner) {
      if (Minecraft.getInstance().getConnection() != null) {
         PacketDistributor.sendToServer(new TradeActionPayload(action, partner), new CustomPacketPayload[0]);
      }
   }
}
