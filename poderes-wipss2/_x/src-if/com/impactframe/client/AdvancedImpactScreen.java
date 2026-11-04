package com.impactframe.client;

import com.impactframe.ImpactConfig;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.ContainerObjectSelectionList.Entry;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AdvancedImpactScreen extends Screen {
   private static final ImpactType[] DISPLAY_ORDER = new ImpactType[]{
      ImpactType.ATTACK,
      ImpactType.DAMAGE,
      ImpactType.CRIT,
      ImpactType.KILL,
      ImpactType.BLOCK_BREAK,
      ImpactType.BLOCK_PLACE,
      ImpactType.LAND,
      ImpactType.FOOTSTEP,
      ImpactType.EXPLOSION,
      ImpactType.MACE
   };
   private final Screen parent;
   private AdvancedImpactScreen.TriggerList triggerList;

   public AdvancedImpactScreen(Screen parent) {
      super(Component.translatable("impactframe.gui.advanced.title"));
      this.parent = parent;
   }

   protected void init() {
      int listHeight = this.height - 160;
      this.triggerList = new AdvancedImpactScreen.TriggerList(Minecraft.getInstance(), this.width, listHeight, 50, 32);

      for (ImpactType type : DISPLAY_ORDER) {
         this.triggerList.addTrigger(type);
      }

      this.addRenderableWidget(this.triggerList);
      int thresholdWidth = Math.max(128, Math.min(250, (this.width - 48) / 2 - 8));
      int thresholdGap = 12;
      int thresholdLeft = (this.width - thresholdWidth * 2 - thresholdGap) / 2;
      this.addRenderableWidget(
         new AdvancedImpactScreen.ThresholdSlider(
            thresholdLeft,
            this.height - 82,
            thresholdWidth,
            20,
            0.0,
            10.0,
            0.5,
            ImpactConfig.minimumDamage(),
            ImpactConfig::setMinimumDamage,
            value -> Component.translatable("impactframe.gui.advanced.minimumDamage", new Object[]{format(value)})
         )
      );
      this.addRenderableWidget(
         new AdvancedImpactScreen.ThresholdSlider(
            thresholdLeft + thresholdWidth + thresholdGap,
            this.height - 82,
            thresholdWidth,
            20,
            0.0,
            12.0,
            0.5,
            ImpactConfig.minimumFallDistance(),
            ImpactConfig::setMinimumFallDistance,
            value -> Component.translatable("impactframe.gui.advanced.minimumFall", new Object[]{format(value)})
         )
      );
      this.addRenderableWidget(
         new AdvancedImpactScreen.ThresholdSlider(
            thresholdLeft,
            this.height - 58,
            thresholdWidth,
            20,
            0.35,
            1.0,
            0.05,
            ImpactConfig.minimumExplosionStrength(),
            ImpactConfig::setMinimumExplosionStrength,
            value -> Component.translatable("impactframe.gui.advanced.minimumExplosion", new Object[]{percent(value)})
         )
      );
      this.addRenderableWidget(
         new AdvancedImpactScreen.ThresholdSlider(
            thresholdLeft + thresholdWidth + thresholdGap,
            this.height - 58,
            thresholdWidth,
            20,
            0.0,
            20.0,
            0.5,
            ImpactConfig.minimumBlockHardness(),
            ImpactConfig::setMinimumBlockHardness,
            value -> Component.translatable("impactframe.gui.advanced.minimumBlock", new Object[]{format(value)})
         )
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.done"), button -> this.onClose()).bounds(this.width / 2 - 100, this.height - 29, 200, 20).build()
      );
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics, mouseX, mouseY, partialTick);
      super.render(graphics, mouseX, mouseY, partialTick);
      graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 16777215);
      graphics.drawCenteredString(this.font, Component.translatable("impactframe.gui.advanced.hint"), this.width / 2, 27, 12632256);
      graphics.drawCenteredString(this.font, Component.translatable("impactframe.gui.advanced.thresholds"), this.width / 2, this.height - 104, 16777120);
      int left = this.triggerList.getRowLeft();
      graphics.drawString(this.font, Component.translatable("impactframe.gui.advanced.column.trigger"), left + 4, 39, 16777215);
      graphics.drawString(this.font, Component.translatable("impactframe.gui.advanced.column.enabled"), left + 107, 39, 16777215);
      graphics.drawString(this.font, Component.translatable("impactframe.gui.advanced.column.intensity"), left + 168, 39, 16777215);
   }

   public void onClose() {
      ImpactConfig.save();
      if (this.parent instanceof ImpactPresetScreen presetScreen) {
         presetScreen.refreshPresetButton();
      }

      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parent);
      }
   }

   private static String format(double value) {
      return String.format(Locale.ROOT, "%.1f", value);
   }

   private static String percent(double value) {
      return String.format(Locale.ROOT, "%.0f", value * 100.0);
   }

   private static final class ThresholdSlider extends AbstractSliderButton {
      private final double min;
      private final double max;
      private final double step;
      private final DoubleConsumer setter;
      private final Function<Double, Component> formatter;

      private ThresholdSlider(
         int x, int y, int width, int height, double min, double max, double step, double initial, DoubleConsumer setter, Function<Double, Component> formatter
      ) {
         super(x, y, width, height, Component.empty(), (initial - min) / (max - min));
         this.min = min;
         this.max = max;
         this.step = step;
         this.setter = setter;
         this.formatter = formatter;
         this.updateMessage();
      }

      protected void updateMessage() {
         this.setMessage(this.formatter.apply(this.actualValue()));
      }

      protected void applyValue() {
         double actual = this.actualValue();
         this.setter.accept(actual);
         this.value = (actual - this.min) / (this.max - this.min);
         this.updateMessage();
      }

      private double actualValue() {
         double actual = this.min + this.value * (this.max - this.min);
         return Math.max(this.min, Math.min(this.max, this.min + (double)Math.round((actual - this.min) / this.step) * this.step));
      }
   }

   private static final class TriggerIntensitySlider extends AbstractSliderButton {
      private final ImpactType type;

      private TriggerIntensitySlider(int x, int y, int width, int height, ImpactType type) {
         super(x, y, width, height, Component.empty(), ImpactConfig.triggerIntensity(type));
         this.type = type;
         this.updateMessage();
      }

      protected void updateMessage() {
         this.setMessage(Component.translatable("impactframe.gui.advanced.intensity", new Object[]{(int)Math.floor(this.value * 100.0)}));
      }

      protected void applyValue() {
         ImpactConfig.setTriggerIntensity(this.type, this.value);
      }
   }

   private final class TriggerList extends ContainerObjectSelectionList<AdvancedImpactScreen.TriggerRow> {
      private TriggerList(Minecraft minecraft, int width, int height, int top, int rowHeight) {
         super(minecraft, width, height, top, rowHeight);
      }

      public int getRowWidth() {
         return Math.max(260, Math.min(620, this.width - 32));
      }

      private void addTrigger(ImpactType type) {
         int sliderWidth = Math.max(80, this.getRowWidth() - 176);
         this.addEntry(AdvancedImpactScreen.this.new TriggerRow(type, sliderWidth));
      }
   }

   private final class TriggerRow extends Entry<AdvancedImpactScreen.TriggerRow> {
      private final ImpactType type;
      private final Button enabledButton;
      private final AdvancedImpactScreen.TriggerIntensitySlider intensitySlider;

      private TriggerRow(ImpactType type, int sliderWidth) {
         this.type = type;
         this.enabledButton = Button.builder(this.enabledText(), button -> {
            ImpactConfig.setTriggerEnabled(type, !ImpactConfig.triggerConfigured(type));
            button.setMessage(this.enabledText());
         }).bounds(0, 0, 54, 20).build();
         this.intensitySlider = new AdvancedImpactScreen.TriggerIntensitySlider(0, 0, sliderWidth, 20, type);
      }

      public List<? extends GuiEventListener> children() {
         return List.of(this.enabledButton, this.intensitySlider);
      }

      public List<? extends NarratableEntry> narratables() {
         return List.of(this.enabledButton, this.intensitySlider);
      }

      public void render(
         GuiGraphics graphics, int index, int top, int left, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float partialTick
      ) {
         this.enabledButton.setX(left + 107);
         this.enabledButton.setY(top + 6);
         this.intensitySlider.setX(left + 168);
         this.intensitySlider.setY(top + 6);
         graphics.drawString(AdvancedImpactScreen.this.font, Component.translatable("impactframe.gui.trigger." + this.type.key), left + 4, top + 12, 16777215);
         this.enabledButton.render(graphics, mouseX, mouseY, partialTick);
         this.intensitySlider.render(graphics, mouseX, mouseY, partialTick);
      }

      private Component enabledText() {
         return Component.translatable(ImpactConfig.triggerConfigured(this.type) ? "impactframe.gui.advanced.on" : "impactframe.gui.advanced.off");
      }
   }
}
