package com.impactframe.client;

import com.impactframe.ImpactConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ImpactPresetScreen extends Screen {
   private final Screen parent;
   private Button presetButton;
   private Button previewButton;
   private static int previewIndex;

   public ImpactPresetScreen(Screen parent) {
      super(Component.translatable("impactframe.gui.title"));
      this.parent = parent;
   }

   protected void init() {
      int centerX = this.width / 2;
      int top = this.menuTop();
      int step = this.menuStep();
      this.presetButton = (Button)this.addRenderableWidget(Button.builder(this.presetButtonText(), button -> {
         ImpactConfig.setPreset(ImpactConfig.preset().next());
         this.updatePresetButton();
      }).bounds(centerX - 105, top, 210, 24).build());
      this.addRenderableWidget(Button.builder(Component.translatable("impactframe.gui.advanced.open"), button -> {
         ImpactConfig.setPreset(ImpactPreset.ADVANCED);
         if (this.minecraft != null) {
            this.minecraft.setScreen(new AdvancedImpactScreen(this));
         }
      }).bounds(centerX - 105, top + 2 * step, 210, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("impactframe.gui.options.open"), button -> {
         if (this.minecraft != null) {
            this.minecraft.setScreen(new ImpactOptionsScreen(this));
         }
      }).bounds(centerX - 105, top + 3 * step, 210, 20).build());
      this.previewButton = (Button)this.addRenderableWidget(Button.builder(this.previewText(), button -> {
         ImpactFrameClient.preview(ImpactFrameClient.PREVIEW_TYPES[previewIndex]);
         previewIndex = (previewIndex + 1) % ImpactFrameClient.PREVIEW_TYPES.length;
         button.setMessage(this.previewText());
      }).bounds(centerX - 105, top + 4 * step, 210, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("impactframe.gui.server.open"), button -> {
         if (this.minecraft != null) {
            this.minecraft.setScreen(new ServerFeaturesScreen(this));
         }
      }).bounds(centerX - 105, top + 5 * step, 210, 20).build());
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.done"), button -> this.onClose()).bounds(centerX - 105, top + 6 * step, 210, 20).build()
      );
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics, mouseX, mouseY, partialTick);
      super.render(graphics, mouseX, mouseY, partialTick);
      int top = this.menuTop();
      int step = this.menuStep();
      graphics.drawCenteredString(this.font, this.title, this.width / 2, top - 18, 16777215);
      graphics.drawCenteredString(this.font, Component.translatable(ImpactConfig.preset().descriptionKey()), this.width / 2, top + 27, 12632256);
      graphics.drawCenteredString(this.font, Component.translatable("impactframe.gui.keybindHint"), this.width / 2, top + 6 * step + 23, 8421504);
   }

   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parent);
      }
   }

   private Component previewText() {
      return Component.translatable(
         "impactframe.gui.preview", new Object[]{Component.translatable("impactframe.gui.trigger." + ImpactFrameClient.PREVIEW_TYPES[previewIndex].key)}
      );
   }

   private Component presetButtonText() {
      return Component.translatable("impactframe.gui.preset.button", new Object[]{Component.translatable(ImpactConfig.preset().labelKey())});
   }

   private void updatePresetButton() {
      if (this.presetButton != null) {
         this.presetButton.setMessage(this.presetButtonText());
      }
   }

   private int menuStep() {
      return Math.max(21, Math.min(25, (this.height - 85) / 7));
   }

   private int menuTop() {
      return this.height / 2 - 3 * this.menuStep();
   }

   void refreshPresetButton() {
      this.updatePresetButton();
   }
}
