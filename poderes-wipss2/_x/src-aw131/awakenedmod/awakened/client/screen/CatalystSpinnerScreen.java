package com.awakenedmod.awakened.client.screen;

import com.awakenedmod.awakened.client.AbilityIcons;
import com.awakenedmod.awakened.client.ClientConfig;
import com.awakenedmod.awakened.core.net.CatalystChoicePayload;
import com.awakenedmod.awakened.core.net.CatalystSpinPayload;
import com.awakenedmod.awakened.core.net.SkipCatalystSpinPayload;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CatalystSpinnerScreen extends AwakenedScreen {
   private static final int CELL_W = 40;
   private static final int CELL_H = 48;
   private static final int ICON = 32;
   private static final int MAX_VISIBLE_CELLS = 9;
   private static final int PANEL_PAD = 12;
   private static final long SPIN_MS = 4600L;
   private static final long RESULT_MS = 2000L;
   private static final long FLASH_MS = 450L;
   private static final float EASE_EXP = 3.0F;
   private static final int START_CELL = 2;
   private static final int LANDING_CELL = 42;
   private static final int TAIL_CELLS = 6;
   private static final long MIN_TICK_GAP_MS = 45L;
   private static final long CHOICE_ARM_MS = 1000L;
   private static final int CHOICE_BUTTON_H = 20;
   private static final int COLOR_PANEL = -267710701;
   private static final int COLOR_PANEL_EDGE = -4667692;
   private static final int COLOR_BORDER = -14011072;
   private static final int COLOR_CELL_BG = -16447477;
   private static final int COLOR_TITLE = -1511692;
   private static final int COLOR_DIM = -8483943;
   private static final int COLOR_MARKER = -9864;
   private final Power result;
   private final Power current;
   private final List<Power> strip;
   private final float landingJitter;
   private long start;
   private boolean landed;
   private long landedAt;
   private int lastCell = Integer.MIN_VALUE;
   private long lastTickAt;
   private boolean skipSent;
   private boolean answered;
   private Button take;
   private Button keep;
   private int visibleCells = 9;
   private static CatalystSpinPayload unanswered;

   public static void register(IEventBus gameBus) {
      gameBus.addListener(CatalystSpinnerScreen::onClientTick);
      gameBus.addListener(event -> unanswered = null);
   }

   public static void open(CatalystSpinPayload payload) {
      Power result = PowerRegistry.get(payload.powerId());
      Power current = payload.offersChoice() ? PowerRegistry.get(payload.currentId()) : null;
      if (!payload.offersChoice() || result != null && current != null) {
         if (result != null && (current != null || (Boolean)ClientConfig.CATALYST_SPINNER.get())) {
            CatalystSpinnerScreen screen = new CatalystSpinnerScreen(payload, result, current);
            if (!(Boolean)ClientConfig.CATALYST_SPINNER.get()) {
               screen.fastForward();
            }

            unanswered = current != null ? payload : null;
            Minecraft.getInstance().setScreen(screen);
         } else {
            sendSkip();
         }
      } else {
         sendChoice(false);
      }
   }

   public static void offerClosed() {
      unanswered = null;
      if (Minecraft.getInstance().screen instanceof CatalystSpinnerScreen screen && screen.current != null) {
         screen.answered = true;
         screen.onClose();
      }
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (unanswered != null && mc.screen == null && mc.player != null && mc.player.isAlive()) {
         Power result = PowerRegistry.get(unanswered.powerId());
         Power current = PowerRegistry.get(unanswered.currentId());
         if (result != null && current != null) {
            CatalystSpinnerScreen screen = new CatalystSpinnerScreen(unanswered, result, current);
            screen.fastForward();
            mc.setScreen(screen);
         } else {
            unanswered = null;
         }
      }
   }

   private static List<Power> resolvePool(List<String> poolIds) {
      List<Power> pool = poolIds.stream().map(PowerRegistry::get).filter(Objects::nonNull).toList();
      return pool.size() >= 2 ? pool : List.copyOf(PowerRegistry.rollable());
   }

   private CatalystSpinnerScreen(CatalystSpinPayload payload, Power result, Power current) {
      super(Component.translatable("awakened.spinner.title"));
      this.result = result;
      this.current = current;
      Random random = new Random(payload.seed());
      this.strip = buildStrip(result, random, resolvePool(payload.poolIds()));
      this.landingJitter = (random.nextFloat() - 0.5F) * 0.7F;
      this.start = Util.getMillis();
   }

   private static List<Power> buildStrip(Power result, Random random, List<Power> pool) {
      List<Power> cells = new ArrayList<>();

      for (int i = 0; i <= 48; i++) {
         if (i == 42) {
            cells.add(result);
         } else {
            Power pick = pool.get(random.nextInt(pool.size()));
            boolean nearLanding = Math.abs(i - 42) <= 2;

            for (int tries = 0; tries < 8 && (!cells.isEmpty() && pick == cells.get(cells.size() - 1) || nearLanding && pick == result); tries++) {
               pick = pool.get(random.nextInt(pool.size()));
            }

            cells.add(pick);
         }
      }

      return cells;
   }

   protected void init() {
      int fit = (this.width - 48) / 40;
      if (fit % 2 == 0) {
         fit--;
      }

      this.visibleCells = Mth.clamp(fit, 3, 9);
      if (this.current != null) {
         int cx = this.width / 2;
         int buttonW = Math.max(90, (this.visibleCells * 40 - 8) / 2);
         int y = this.choiceButtonsY();
         this.take = (Button)this.addRenderableWidget(
            Button.builder(Component.translatable("awakened.spinner.take", new Object[]{this.result.displayName()}), b -> this.answer(true))
               .bounds(cx - buttonW - 4, y, buttonW, 20)
               .build()
         );
         this.keep = (Button)this.addRenderableWidget(
            Button.builder(Component.translatable("awakened.spinner.keep", new Object[]{this.current.displayName()}), b -> this.answer(false))
               .bounds(cx + 4, y, buttonW, 20)
               .build()
         );
         this.updateChoiceButtons(Util.getMillis());
      }
   }

   private int stripTop() {
      return this.height / 2 - 24;
   }

   private int choiceButtonsY() {
      return this.stripTop() + 48 + 46;
   }

   private void updateChoiceButtons(long now) {
      if (this.take != null) {
         boolean armed = this.landed && now - this.landedAt >= 1000L && !this.answered;
         this.take.visible = this.landed;
         this.keep.visible = this.landed;
         this.take.active = armed;
         this.keep.active = armed;
      }
   }

   private void answer(boolean takeIt) {
      if (!this.answered) {
         this.answered = true;
         unanswered = null;
         sendChoice(takeIt);
         this.onClose();
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.render(g, mouseX, mouseY, partialTick);
      long now = Util.getMillis();
      if (this.current == null && this.landed && now - this.landedAt >= 2000L) {
         this.onClose();
      } else {
         float progress = Math.min(1.0F, (float)(now - this.start) / 4600.0F);
         float eased = 1.0F - (float)Math.pow((double)(1.0F - progress), 3.0);
         float cellPos = 2.0F + (42.0F + this.landingJitter - 2.0F) * eased;
         if (!this.landed) {
            if (progress >= 1.0F) {
               this.land(now);
            } else {
               this.tickPassingIcons(now, cellPos, eased);
            }
         }

         this.updateChoiceButtons(now);
         int cx = this.width / 2;
         int stripW = this.visibleCells * 40;
         int stripX = cx - stripW / 2;
         int stripY = this.stripTop();
         int panelX0 = stripX - 12;
         int panelY0 = stripY - 34;
         int panelX1 = stripX + stripW + 12;
         int panelY1 = this.current != null ? this.choiceButtonsY() + 20 + 10 : stripY + 48 + 44;
         if (this.take != null) {
            panelX0 = Math.min(panelX0, this.take.getX() - 12);
            panelX1 = Math.max(panelX1, this.keep.getX() + this.keep.getWidth() + 12);
         }

         g.fill(panelX0 - 1, panelY0 - 1, panelX1 + 1, panelY1 + 1, -4667692);
         g.fill(panelX0, panelY0, panelX1, panelY1, -267710701);
         g.drawCenteredString(this.font, this.title, cx, panelY0 + 9, -1511692);
         g.fill(stripX - 2, stripY - 2, stripX + stripW + 2, stripY + 48 + 2, -14011072);
         g.enableScissor(stripX, stripY, stripX + stripW, stripY + 48);
         g.fill(stripX, stripY, stripX + stripW, stripY + 48, -16447477);
         int first = Math.max(0, (int)cellPos - this.visibleCells / 2 - 1);
         int last = Math.min(this.strip.size() - 1, (int)cellPos + this.visibleCells / 2 + 2);

         for (int i = first; i <= last; i++) {
            this.drawCell(g, this.strip.get(i), Math.round((float)cx + ((float)i - cellPos) * 40.0F), stripY, this.landed && i == 42);
         }

         for (int s = 0; s < 4; s++) {
            int alpha = 160 - s * 40;
            g.fill(stripX + s * 7, stripY, stripX + (s + 1) * 7, stripY + 48, alpha << 24);
            g.fill(stripX + stripW - (s + 1) * 7, stripY, stripX + stripW - s * 7, stripY + 48, alpha << 24);
         }

         g.disableScissor();
         g.fill(cx - 1, stripY - 5, cx + 1, stripY + 48 + 5, -9864);

         for (int s = 0; s < 3; s++) {
            int half = 3 - s;
            g.fill(cx - half, stripY - 5 + s, cx + half, stripY - 4 + s, -9864);
            g.fill(cx - half, stripY + 48 + 4 - s, cx + half, stripY + 48 + 5 - s, -9864);
         }

         if (this.landed) {
            this.renderResult(g, cx, stripY, now);
         } else {
            g.drawCenteredString(this.font, Component.translatable("awakened.spinner.skip"), cx, stripY + 48 + 18, -8483943);
         }

         for (Renderable widget : this.renderables) {
            widget.render(g, mouseX, mouseY, partialTick);
         }
      }
   }

   private void drawCell(GuiGraphics g, Power power, int centerX, int stripY, boolean highlight) {
      int x0 = centerX - 20;
      int rgb = power.themeColor() & 16777215;
      g.fill(x0 + 2, stripY + 2, x0 + 40 - 2, stripY + 48 - 2, 1073741824 | rgb);
      g.fill(x0 + 2, stripY + 2, x0 + 40 - 2, stripY + 4, -872415232 | rgb);
      int ix = centerX - 16;
      int iy = stripY + 8;
      AbilityIcons.draw(g, power, ix, iy, 32);
      if (highlight) {
         int c = 0xFF000000 | rgb;
         g.fill(x0 + 1, stripY + 1, x0 + 40 - 1, stripY + 2, c);
         g.fill(x0 + 1, stripY + 48 - 2, x0 + 40 - 1, stripY + 48 - 1, c);
         g.fill(x0 + 1, stripY + 1, x0 + 2, stripY + 48 - 1, c);
         g.fill(x0 + 40 - 2, stripY + 1, x0 + 40 - 1, stripY + 48 - 1, c);
      }

      g.fill(x0 + 40 - 1, stripY, x0 + 40, stripY + 48, -14011072);
   }

   private void renderResult(GuiGraphics g, int cx, int stripY, long now) {
      long since = now - this.landedAt;
      int rgb = this.result.themeColor() & 16777215;
      float burst = Math.min(1.0F, (float)since / 600.0F);
      int midY = stripY + 24;

      for (int ringIdx = 0; ringIdx < 3; ringIdx++) {
         float fr = Math.min(1.0F, burst * (1.2F - (float)ringIdx * 0.2F));
         int r = (int)(fr * (float)(24 + ringIdx * 12));
         int alpha = (int)((1.0F - fr) * (float)(88 - ringIdx * 20));
         if (alpha > 0 && r > 0) {
            g.fill(cx - r, midY - r, cx + r, midY + r, alpha << 24 | rgb);
         }
      }

      Component name = this.result.displayName().copy().withStyle(ChatFormatting.BOLD);
      int nameY = stripY + 48 + 14;
      float s = 1.5F;
      g.pose().pushPose();
      g.pose().translate((float)cx, (float)nameY, 0.0F);
      g.pose().scale(s, s, 1.0F);
      g.drawCenteredString(this.font, name, 0, 0, 0xFF000000 | rgb);
      g.pose().popPose();
      g.drawCenteredString(
         this.font, Component.translatable(this.current != null ? "awakened.spinner.choice_hint" : "awakened.spinner.continue"), cx, nameY + 18, -8483943
      );
      if (since < 450L) {
         int alpha = (int)(180.0F * (1.0F - (float)since / 450.0F));
         if (alpha > 0) {
            g.fill(0, 0, this.width, this.height, alpha << 24 | 16777215);
         }
      }
   }

   private void tickPassingIcons(long now, float cellPos, float eased) {
      int cell = Math.round(cellPos);
      if (cell != this.lastCell) {
         this.lastCell = cell;
         if (now - this.lastTickAt >= 45L) {
            this.lastTickAt = now;
            play((SoundEvent)SoundEvents.NOTE_BLOCK_HAT.value(), 0.7F + 0.7F * eased, 0.5F);
         }
      }
   }

   private void land(long now) {
      this.landed = true;
      this.landedAt = now;
      this.lastCell = 42;
      play(SoundEvents.PLAYER_LEVELUP, 1.0F, 0.9F);
      play((SoundEvent)SoundEvents.NOTE_BLOCK_BELL.value(), 1.4F, 0.8F);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return this.current != null && this.landed
         ? super.mouseClicked(mouseX, mouseY, button)
         : this.handleClick() || super.mouseClicked(mouseX, mouseY, button);
   }

   private boolean handleClick() {
      if (this.landed) {
         this.onClose();
      } else {
         if (this.current == null) {
            this.sendSkipOnce();
         }

         this.fastForward();
      }

      return true;
   }

   private void fastForward() {
      long now = Util.getMillis();
      this.start = now - 4600L;
      this.land(now);
   }

   public void onClose() {
      if (this.current != null && !this.answered) {
         if (!this.landed) {
            this.fastForward();
         }
      } else {
         super.onClose();
      }
   }

   public void removed() {
      if (!this.landed && this.current == null) {
         this.sendSkipOnce();
      }

      super.removed();
   }

   private void sendSkipOnce() {
      if (!this.skipSent) {
         this.skipSent = true;
         sendSkip();
      }
   }

   private static void sendSkip() {
      if (Minecraft.getInstance().getConnection() != null) {
         PacketDistributor.sendToServer(SkipCatalystSpinPayload.INSTANCE, new CustomPacketPayload[0]);
      }
   }

   private static void sendChoice(boolean take) {
      if (Minecraft.getInstance().getConnection() != null) {
         PacketDistributor.sendToServer(new CatalystChoicePayload(take), new CustomPacketPayload[0]);
      }
   }

   private static void play(SoundEvent sound, float pitch, float volume) {
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
   }

   public boolean isPauseScreen() {
      return false;
   }
}
