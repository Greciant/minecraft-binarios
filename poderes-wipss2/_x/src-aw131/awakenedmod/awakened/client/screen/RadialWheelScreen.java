package com.awakenedmod.awakened.client.screen;

import com.awakenedmod.awakened.client.screen.wheel.SolidWheelSkin;
import com.awakenedmod.awakened.client.screen.wheel.WheelSkin;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class RadialWheelScreen extends AwakenedScreen {
   private final List<RadialWheelScreen.RadialOption> options;
   private final IntConsumer onSelect;
   private final KeyMapping openKey;
   private final WheelSkin skin;
   private final int fallback;
   private int hovered = -1;
   private boolean committed;

   public RadialWheelScreen(Component title, List<RadialWheelScreen.RadialOption> options, IntConsumer onSelect, KeyMapping openKey) {
      this(title, options, onSelect, openKey, -1);
   }

   public RadialWheelScreen(
      Component title, List<RadialWheelScreen.RadialOption> options, IntConsumer onSelect, KeyMapping openKey, int fallback, WheelSkin skin
   ) {
      super(title);
      this.options = options;
      this.onSelect = onSelect;
      this.openKey = openKey;
      this.fallback = fallback;
      this.skin = skin;
   }

   public RadialWheelScreen(Component title, List<RadialWheelScreen.RadialOption> options, IntConsumer onSelect, KeyMapping openKey, int fallback) {
      this(title, options, onSelect, openKey, fallback, new SolidWheelSkin());
   }

   public static boolean keyHeld(KeyMapping mapping) {
      Key key = mapping.getKey();
      int keyCode = key.getValue();
      if (keyCode == InputConstants.UNKNOWN.getValue()) {
         return false;
      } else {
         Minecraft mc = Minecraft.getInstance();
         long window = mc.getWindow().getWindow();
         if (key.getType() == Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, keyCode) == 1;
         } else {
            return key.getType() != Type.KEYSYM ? mapping.isDown() : InputConstants.isKeyDown(window, keyCode);
         }
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void tick() {
      super.tick();
      if (!this.openKeyHeld()) {
         this.commit();
      }
   }

   private boolean openKeyHeld() {
      return keyHeld(this.openKey);
   }

   protected void init() {
      super.init();
      this.skin.layout(this.width, this.height, this.options.size());
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.render(g, mouseX, mouseY, partialTick);
      this.hovered = this.skin.hover((double)mouseX - (double)this.width / 2.0, (double)mouseY - (double)this.height / 2.0);
      this.skin.draw(g, this.minecraft.font, this.options, this.hovered);
   }

   private void commit() {
      if (!this.committed) {
         this.committed = true;
         int picked = this.hovered >= 0 ? this.hovered : this.fallback;
         if (picked >= 0 && picked < this.options.size()) {
            RadialWheelScreen.RadialOption opt = this.options.get(picked);
            if (opt.locked()) {
               this.softFail(opt);
            } else {
               this.onSelect.accept(opt.payloadIndex());
            }
         }

         this.onClose();
      }
   }

   private void softFail(RadialWheelScreen.RadialOption opt) {
      if (this.minecraft != null && this.minecraft.player != null) {
         this.minecraft.player.playSound((SoundEvent)SoundEvents.NOTE_BLOCK_BASS.value(), 0.4F, 0.7F);
         Component reason = opt.lockHint() != null
            ? Component.translatable("awakened.wheel.locked_hint", new Object[]{opt.label(), opt.lockHint()})
            : Component.translatable("awakened.wheel.locked", new Object[]{opt.label()});
         this.minecraft.player.displayClientMessage(reason, true);
      }
   }

   public void onClose() {
      while (this.openKey.consumeClick()) {
      }

      super.onClose();
   }

   public static record RadialOption(
      ResourceLocation icon,
      int color,
      Component label,
      boolean locked,
      Component lockHint,
      boolean selected,
      int payloadIndex,
      Component cost,
      boolean affordable,
      ItemStack stack
   ) {
      public RadialOption(
         ResourceLocation icon,
         int color,
         Component label,
         boolean locked,
         Component lockHint,
         boolean selected,
         int payloadIndex,
         Component cost,
         boolean affordable
      ) {
         this(icon, color, label, locked, lockHint, selected, payloadIndex, cost, affordable, ItemStack.EMPTY);
      }
   }
}
