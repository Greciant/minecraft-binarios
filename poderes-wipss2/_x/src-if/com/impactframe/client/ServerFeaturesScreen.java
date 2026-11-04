package com.impactframe.client;

import com.impactframe.ImpactConfig;
import com.impactframe.network.FreezeRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

public final class ServerFeaturesScreen extends Screen {
   private static final int MIN_TICKS = 1;
   private static final int MAX_TICKS = 10;
   private final Screen parent;
   private Button masterButton;
   private Button freezeButton;
   private Button lockButton;
   private ServerFeaturesScreen.FreezeTicksSlider freezeSlider;

   public ServerFeaturesScreen(Screen parent) {
      super(Component.translatable("impactframe.gui.server.title"));
      this.parent = parent;
   }

   protected void init() {
      int cx = this.width / 2;
      int cy = this.height / 2;
      this.masterButton = (Button)this.addRenderableWidget(Button.builder(masterText(), button -> {
         ImpactConfig.setServerFeatures(!ImpactConfig.serverFeatures());
         this.refresh();
      }).bounds(cx - 105, cy - 55, 210, 20).build());
      this.freezeButton = (Button)this.addRenderableWidget(Button.builder(freezeText(), button -> {
         ImpactConfig.setFreezeOnImpact(!ImpactConfig.freezeOnImpact());
         this.refresh();
      }).bounds(cx - 105, cy - 30, 210, 20).build());
      this.lockButton = (Button)this.addRenderableWidget(Button.builder(lockText(), button -> {
         ImpactConfig.setLockMovement(!ImpactConfig.lockMovement());
         this.refresh();
      }).bounds(cx - 105, cy - 5, 210, 20).build());
      this.freezeSlider = (ServerFeaturesScreen.FreezeTicksSlider)this.addRenderableWidget(
         new ServerFeaturesScreen.FreezeTicksSlider(cx - 105, cy + 20, 210, 20)
      );
      this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose()).bounds(cx - 105, cy + 62, 210, 20).build());
      this.refresh();
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics, mouseX, mouseY, partialTick);
      super.render(graphics, mouseX, mouseY, partialTick);
      graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 85, 16777215);
      graphics.drawCenteredString(this.font, Component.translatable("impactframe.gui.server.hint"), this.width / 2, this.height / 2 - 72, 12632256);
      graphics.drawCenteredString(this.font, statusText(), this.width / 2, this.height / 2 + 44, 12632256);
      graphics.drawCenteredString(this.font, Component.translatable("impactframe.gui.server.rules"), this.width / 2, this.height / 2 + 90, 8421504);
   }

   public void onClose() {
      ImpactConfig.save();
      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parent);
      }
   }

   private void refresh() {
      this.masterButton.setMessage(masterText());
      this.freezeButton.setMessage(freezeText());
      this.lockButton.setMessage(lockText());
      boolean on = ImpactConfig.serverFeatures();
      this.freezeButton.active = on;
      this.lockButton.active = on;
      this.freezeSlider.active = on;
   }

   private static Component onOff(boolean value) {
      return Component.translatable(value ? "impactframe.gui.advanced.on" : "impactframe.gui.advanced.off");
   }

   private static Component masterText() {
      return Component.translatable("impactframe.gui.server.master", new Object[]{onOff(ImpactConfig.serverFeatures())});
   }

   private static Component freezeText() {
      return Component.translatable("impactframe.gui.server.freeze", new Object[]{onOff(ImpactConfig.freezeOnImpact())});
   }

   private static Component lockText() {
      return Component.translatable("impactframe.gui.server.lock", new Object[]{onOff(ImpactConfig.lockMovement())});
   }

   private static Component statusText() {
      ClientPacketListener connection = Minecraft.getInstance().getConnection();
      return connection == null
         ? Component.translatable("impactframe.gui.server.status.none")
         : Component.translatable(connection.hasChannel(FreezeRequestPayload.TYPE) ? "impactframe.gui.server.status.yes" : "impactframe.gui.server.status.no");
   }

   private static final class FreezeTicksSlider extends AbstractSliderButton {
      private FreezeTicksSlider(int x, int y, int width, int height) {
         super(x, y, width, height, Component.empty(), (double)(ImpactConfig.freezeTicks() - 1) / 9.0);
         this.updateMessage();
      }

      private int ticks() {
         return 1 + (int)Math.round(this.value * 9.0);
      }

      protected void updateMessage() {
         int ticks = this.ticks();
         this.setMessage(Component.translatable("impactframe.gui.server.duration", new Object[]{ticks, ticks * 50}));
      }

      protected void applyValue() {
         ImpactConfig.setFreezeTicks(this.ticks());
      }
   }
}
