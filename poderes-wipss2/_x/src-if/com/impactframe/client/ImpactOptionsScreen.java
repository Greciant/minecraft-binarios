package com.impactframe.client;

import com.impactframe.ImpactConfig;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.ContainerObjectSelectionList.Entry;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ImpactOptionsScreen extends Screen {
   private final Screen parent;
   private ImpactOptionsScreen.OptionList optionList;

   public ImpactOptionsScreen(Screen parent) {
      super(Component.translatable("impactframe.gui.options.title"));
      this.parent = parent;
   }

   protected void init() {
      this.optionList = new ImpactOptionsScreen.OptionList(Minecraft.getInstance(), this.width, this.height - 82, 46, 30);
      this.optionList.addSection("impactframe.gui.options.section.general");
      this.optionList.addToggle("impactframe.configuration.enabled", ImpactConfig::enabledSetting, ImpactConfig::setEnabled);
      this.optionList
         .addSlider(
            "impactframe.configuration.intensity", 0.0, 1.0, 0.01, ImpactConfig::intensitySetting, ImpactConfig::setIntensity, ImpactOptionsScreen::percent
         );
      this.optionList
         .addSlider(
            "impactframe.configuration.cooldownTicks",
            0.0,
            100.0,
            1.0,
            () -> (double)ImpactConfig.cooldownTicksSetting(),
            value -> ImpactConfig.setCooldownTicks((int)Math.round(value)),
            value -> Component.literal(String.format(Locale.ROOT, "%.0f ticks", value))
         );
      this.optionList.addToggle("impactframe.configuration.safeMode", ImpactConfig::safeMode, ImpactConfig::setSafeMode);
      this.optionList.addSection("impactframe.gui.options.section.sound");
      this.optionList.addToggle("impactframe.configuration.soundEnabled", ImpactConfig::soundEnabled, ImpactConfig::setSoundEnabled);
      this.optionList
         .addSlider(
            "impactframe.configuration.soundVolume", 0.0, 1.0, 0.01, ImpactConfig::soundVolume, ImpactConfig::setSoundVolume, ImpactOptionsScreen::percent
         );
      this.optionList.addSection("impactframe.gui.options.section.effects");
      this.optionList.addToggle("impactframe.configuration.directionalStreaks", ImpactConfig::directionalStreaks, ImpactConfig::setDirectionalStreaks);
      this.optionList.addToggle("impactframe.configuration.zoomEnabled", ImpactConfig::zoomEnabled, ImpactConfig::setZoomEnabled);
      this.optionList
         .addSlider(
            "impactframe.configuration.zoomStrength",
            0.0,
            3.0,
            0.05,
            ImpactConfig::zoomStrength,
            ImpactConfig::setZoomStrength,
            value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
         );
      this.optionList
         .addSlider(
            "impactframe.configuration.halftone",
            0.0,
            1.0,
            0.01,
            () -> (double)ImpactConfig.halftone(),
            ImpactConfig::setHalftone,
            ImpactOptionsScreen::percent
         );
      this.optionList.addToggle("impactframe.configuration.onomatopoeia", ImpactConfig::onomatopoeia, ImpactConfig::setOnomatopoeia);
      this.optionList
         .addSlider(
            "impactframe.configuration.onomatopoeiaScale",
            0.5,
            2.0,
            0.05,
            () -> (double)ImpactConfig.onomatopoeiaScale(),
            ImpactConfig::setOnomatopoeiaScale,
            value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
         );
      this.optionList.addToggle("impactframe.configuration.colorKeyedFrames", ImpactConfig::colorKeyed, ImpactConfig::setColorKeyed);
      this.optionList.addToggle("impactframe.configuration.letterbox", ImpactConfig::letterbox, ImpactConfig::setLetterbox);
      this.optionList
         .addSlider(
            "impactframe.configuration.letterboxSize",
            0.02,
            0.25,
            0.01,
            () -> (double)ImpactConfig.letterboxSize(),
            ImpactConfig::setLetterboxSize,
            value -> Component.literal(String.format(Locale.ROOT, "%.0f%%", value * 100.0))
         );
      this.optionList
         .addSlider(
            "impactframe.configuration.flashBudget",
            0.0,
            60.0,
            1.0,
            () -> (double)ImpactConfig.flashBudget(),
            value -> ImpactConfig.setFlashBudget((int)Math.round(value)),
            value -> Component.literal(String.format(Locale.ROOT, "%.0f / sec", value))
         );
      this.optionList.addSection("impactframe.gui.options.section.compatibility");
      this.optionList.addToggle("impactframe.configuration.depthOutlines", ImpactConfig::depthOutlines, ImpactConfig::setDepthOutlines);
      this.optionList.addToggle("impactframe.configuration.liteWithShaderPacks", ImpactConfig::liteWithShaderPacks, ImpactConfig::setLiteWithShaderPacks);
      this.addRenderableWidget(this.optionList);
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.done"), button -> this.onClose()).bounds(this.width / 2 - 100, this.height - 29, 200, 20).build()
      );
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics, mouseX, mouseY, partialTick);
      super.render(graphics, mouseX, mouseY, partialTick);
      graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 16777215);
      graphics.drawCenteredString(this.font, Component.translatable("impactframe.gui.options.hint"), this.width / 2, 27, 12632256);
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

   private static Component percent(double value) {
      return Component.literal(String.format(Locale.ROOT, "%.0f%%", value * 100.0));
   }

   private abstract class OptionEntry extends Entry<ImpactOptionsScreen.OptionEntry> {
      public List<? extends GuiEventListener> children() {
         return List.of();
      }

      public List<? extends NarratableEntry> narratables() {
         return List.of();
      }
   }

   private final class OptionList extends ContainerObjectSelectionList<ImpactOptionsScreen.OptionEntry> {
      private OptionList(Minecraft minecraft, int width, int height, int top, int rowHeight) {
         super(minecraft, width, height, top, rowHeight);
      }

      public int getRowWidth() {
         return Math.max(280, Math.min(640, this.width - 32));
      }

      private void addSection(String labelKey) {
         this.addEntry(ImpactOptionsScreen.this.new SectionEntry(labelKey));
      }

      private void addToggle(String labelKey, BooleanSupplier getter, Consumer<Boolean> setter) {
         this.addEntry(ImpactOptionsScreen.this.new ToggleEntry(labelKey, getter, setter));
      }

      private void addSlider(
         String labelKey, double min, double max, double step, DoubleSupplier getter, DoubleConsumer setter, Function<Double, Component> formatter
      ) {
         this.addEntry(
            ImpactOptionsScreen.this.new SliderEntry(labelKey, min, max, step, getter, setter, formatter, Math.max(88, Math.min(170, this.getRowWidth() - 150)))
         );
      }
   }

   private final class SectionEntry extends ImpactOptionsScreen.OptionEntry {
      private final String labelKey;

      private SectionEntry(String labelKey) {
         this.labelKey = labelKey;
      }

      public void render(
         GuiGraphics graphics, int index, int top, int left, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float partialTick
      ) {
         graphics.drawString(ImpactOptionsScreen.this.font, Component.translatable(this.labelKey), left + 4, top + 11, 16777120);
      }
   }

   private final class SliderEntry extends ImpactOptionsScreen.OptionEntry {
      private final String labelKey;
      private final ImpactOptionsScreen.ValueSlider slider;

      private SliderEntry(
         String labelKey,
         double min,
         double max,
         double step,
         DoubleSupplier getter,
         DoubleConsumer setter,
         Function<Double, Component> formatter,
         int sliderWidth
      ) {
         this.labelKey = labelKey;
         this.slider = new ImpactOptionsScreen.ValueSlider(min, max, step, getter.getAsDouble(), setter, formatter, sliderWidth);
         this.slider.setTooltip(Tooltip.create(Component.translatable(labelKey + ".tooltip")));
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return List.of(this.slider);
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return List.of(this.slider);
      }

      public void render(
         GuiGraphics graphics, int index, int top, int left, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float partialTick
      ) {
         graphics.drawString(ImpactOptionsScreen.this.font, Component.translatable(this.labelKey), left + 4, top + 11, 16777215);
         this.slider.setX(left + rowWidth - this.slider.getWidth());
         this.slider.setY(top + 5);
         this.slider.render(graphics, mouseX, mouseY, partialTick);
      }
   }

   private final class ToggleEntry extends ImpactOptionsScreen.OptionEntry {
      private final String labelKey;
      private final BooleanSupplier getter;
      private final Button button;

      private ToggleEntry(String labelKey, BooleanSupplier getter, Consumer<Boolean> setter) {
         this.labelKey = labelKey;
         this.getter = getter;
         this.button = Button.builder(this.toggleText(), widget -> {
            setter.accept(!getter.getAsBoolean());
            widget.setMessage(this.toggleText());
         }).tooltip(Tooltip.create(Component.translatable(labelKey + ".tooltip"))).bounds(0, 0, 54, 20).build();
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return List.of(this.button);
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return List.of(this.button);
      }

      public void render(
         GuiGraphics graphics, int index, int top, int left, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float partialTick
      ) {
         graphics.drawString(ImpactOptionsScreen.this.font, Component.translatable(this.labelKey), left + 4, top + 11, 16777215);
         this.button.setX(left + rowWidth - 58);
         this.button.setY(top + 5);
         this.button.render(graphics, mouseX, mouseY, partialTick);
      }

      private Component toggleText() {
         return Component.translatable(this.getter.getAsBoolean() ? "impactframe.gui.advanced.on" : "impactframe.gui.advanced.off");
      }
   }

   private static final class ValueSlider extends AbstractSliderButton {
      private final double min;
      private final double max;
      private final double step;
      private final DoubleConsumer setter;
      private final Function<Double, Component> formatter;

      private ValueSlider(double min, double max, double step, double initial, DoubleConsumer setter, Function<Double, Component> formatter, int width) {
         super(0, 0, width, 20, Component.empty(), (initial - min) / (max - min));
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
         if (this.step > 0.0) {
            actual = this.min + (double)Math.round((actual - this.min) / this.step) * this.step;
         }

         return Math.max(this.min, Math.min(this.max, actual));
      }
   }
}
