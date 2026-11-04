package com.awakenedmod.awakened.client.screen;

import com.awakenedmod.awakened.client.ClientPowerState;
import com.awakenedmod.awakened.core.admin.AdminAction;
import com.awakenedmod.awakened.core.net.AdminActionPayload;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AdminScreen extends AwakenedScreen {
   private static final int BUTTON_WIDTH = 200;
   private static final int BUTTON_HEIGHT = 20;
   private static final int GAP = 3;
   private static final int COL_GAP = 8;
   private static final int TOP = 44;
   private static final int MARGIN = 4;
   private static final int MIN_COL_WIDTH = 90;

   public AdminScreen() {
      super(Component.translatable("awakened.admin.title"));
   }

   protected void init() {
      List<List<AdminScreen.Cell>> rows = this.buildRows();
      this.layout(rows);
   }

   private List<List<AdminScreen.Cell>> buildRows() {
      List<List<AdminScreen.Cell>> rows = new ArrayList<>();

      for (Power power : PowerRegistry.all()) {
         String id = power.id();
         rows.add(
            List.of(
               new AdminScreen.Cell(
                  Component.translatable("awakened.admin.set_power", new Object[]{power.displayName()}), b -> send(AdminAction.SET_POWER, id, 0)
               )
            )
         );
      }

      rows.add(List.of(new AdminScreen.Cell(Component.translatable("awakened.admin.reroll"), b -> send(AdminAction.REROLL, "", 0))));
      rows.add(List.of(new AdminScreen.Cell(Component.translatable("awakened.admin.clear"), b -> send(AdminAction.CLEAR, "", 0))));
      rows.add(List.of(new AdminScreen.Cell(Component.translatable("awakened.admin.refill"), b -> send(AdminAction.REFILL_STAMINA, "", 0))));
      rows.add(List.of(new AdminScreen.Cell(Component.translatable("awakened.admin.toggle_infinite"), b -> send(AdminAction.TOGGLE_INFINITE_STAMINA, "", 0))));
      rows.add(
         List.of(
            new AdminScreen.Cell(
               Component.translatable("awakened.admin.mastery_down"),
               b -> send(AdminAction.SET_MASTERY, "", Math.max(0, ClientPowerState.get().masteryLevel() - 1))
            ),
            new AdminScreen.Cell(
               Component.translatable("awakened.admin.mastery_up"), b -> send(AdminAction.SET_MASTERY, "", ClientPowerState.get().masteryLevel() + 1)
            ),
            new AdminScreen.Cell(
               Component.translatable("awakened.admin.mastery_up5"), b -> send(AdminAction.SET_MASTERY, "", ClientPowerState.get().masteryLevel() + 5)
            )
         )
      );
      rows.add(List.of(new AdminScreen.Cell(Component.translatable("awakened.admin.grant_xp"), b -> send(AdminAction.GRANT_XP, "", 100))));
      rows.add(List.of(new AdminScreen.Cell(Component.translatable("awakened.admin.bleed_self"), b -> send(AdminAction.BLEED_SELF, "", 2))));
      rows.add(
         List.of(
            new AdminScreen.Cell(Component.translatable("awakened.admin.grant_souls_10"), b -> send(AdminAction.GRANT_SOULS, "", 10)),
            new AdminScreen.Cell(Component.translatable("awakened.admin.grant_souls_50"), b -> send(AdminAction.GRANT_SOULS, "", 50)),
            new AdminScreen.Cell(Component.translatable("awakened.admin.grant_souls_100"), b -> send(AdminAction.GRANT_SOULS, "", 100))
         )
      );
      rows.add(
         List.of(
            new AdminScreen.Cell(Component.translatable("awakened.admin.grant_souls"), b -> send(AdminAction.GRANT_SOULS, "", 500)),
            new AdminScreen.Cell(Component.translatable("awakened.admin.grant_souls_max"), b -> send(AdminAction.GRANT_SOULS, "", 5000)),
            new AdminScreen.Cell(Component.translatable("awakened.admin.reset_souls"), b -> send(AdminAction.RESET_SOULS, "", 0))
         )
      );
      rows.add(List.of(new AdminScreen.Cell(Component.translatable("awakened.admin.haunt_self"), b -> send(AdminAction.HAUNT_SELF, "", 0))));
      rows.add(
         List.of(
            new AdminScreen.Cell(Component.translatable("awakened.admin.grant_relic"), b -> send(AdminAction.GRANT_RELIC, "", -1)),
            new AdminScreen.Cell(Component.translatable("awakened.admin.grant_belt"), b -> send(AdminAction.GRANT_BELT, "", 0))
         )
      );
      rows.add(
         List.of(
            new AdminScreen.Cell(Component.translatable("awakened.admin.spawn_suppressor"), b -> send(AdminAction.SPAWN_SUPPRESSOR, "", 0)),
            new AdminScreen.Cell(Component.translatable("awakened.admin.spawn_stalker"), b -> send(AdminAction.SPAWN_STALKER, "", 0))
         )
      );
      rows.add(
         List.of(
            new AdminScreen.Cell(Component.translatable("awakened.admin.spawn_enforcer"), b -> send(AdminAction.SPAWN_ENFORCER, "", 0)),
            new AdminScreen.Cell(Component.translatable("awakened.admin.spawn_overseer"), b -> send(AdminAction.SPAWN_OVERSEER, "", 0))
         )
      );
      rows.add(
         List.of(
            new AdminScreen.Cell(Component.translatable("awakened.admin.spawn_arbiter"), b -> send(AdminAction.SPAWN_ARBITER, "", 0)),
            new AdminScreen.Cell(Component.translatable("awakened.admin.spawn_sentinel"), b -> send(AdminAction.SPAWN_SENTINEL, "", 0))
         )
      );
      return rows;
   }

   private void layout(List<List<AdminScreen.Cell>> rows) {
      int rowH = 23;
      int avail = Math.max(rowH, this.height - 4 - 44);
      int rowsThatFit = Math.max(1, avail / rowH);
      int cols = Math.max(1, ceilDiv(rows.size(), rowsThatFit));
      int maxColsByWidth = Math.max(1, (this.width - 8 + 8) / 98);
      cols = Math.min(cols, maxColsByWidth);
      int rowsPerCol = ceilDiv(rows.size(), cols);
      int colWidth = Math.min(200, (this.width - 8 - (cols - 1) * 8) / cols);
      int totalWidth = cols * colWidth + (cols - 1) * 8;
      int startX = this.width / 2 - totalWidth / 2;

      for (int i = 0; i < rows.size(); i++) {
         int col = i / rowsPerCol;
         int rowInCol = i % rowsPerCol;
         int x = startX + col * (colWidth + 8);
         int y = 44 + rowInCol * rowH;
         this.placeRow(rows.get(i), x, y, colWidth);
      }
   }

   private void placeRow(List<AdminScreen.Cell> cells, int x, int y, int rowWidth) {
      int n = cells.size();
      int cellWidth = (rowWidth - (n - 1) * 3) / n;

      for (int i = 0; i < n; i++) {
         AdminScreen.Cell cell = cells.get(i);
         int cx = x + i * (cellWidth + 3);
         this.addRenderableWidget(Button.builder(cell.label(), cell.onPress()).bounds(cx, y, cellWidth, 20).build());
      }
   }

   private static int ceilDiv(int a, int b) {
      return (a + b - 1) / b;
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      ClientPowerState state = ClientPowerState.get();
      graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, -8054);
      String power = state.awakened() ? state.powerId() : "none";
      Component status = Component.translatable("awakened.admin.status", new Object[]{power, state.masteryLevel(), state.infiniteStamina() ? "ON" : "off"});
      graphics.drawCenteredString(this.font, status, this.width / 2, 30, -5592406);
   }

   private static void send(AdminAction action, String powerId, int intArg) {
      PacketDistributor.sendToServer(new AdminActionPayload(action.ordinal(), powerId, intArg), new CustomPacketPayload[0]);
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static record Cell(Component label, OnPress onPress) {
   }
}
