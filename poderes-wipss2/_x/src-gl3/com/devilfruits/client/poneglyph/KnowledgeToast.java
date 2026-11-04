package com.devilfruits.client.poneglyph;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.components.toasts.Toast.Visibility;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class KnowledgeToast implements Toast {
   private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("toast/advancement");
   private final Component name;
   private final ItemStack icon;

   public KnowledgeToast(String nameKey, ResourceLocation icon) {
      this.name = Component.translatable(nameKey);
      this.icon = new ItemStack((ItemLike)BuiltInRegistries.ITEM.get(icon));
   }

   public Visibility render(GuiGraphics gg, ToastComponent toasts, long time) {
      gg.blitSprite(BACKGROUND, 0, 0, this.width(), this.height());
      gg.drawString(toasts.getMinecraft().font, Component.translatable("poneglyph.devilfruits.toast.title"), 30, 7, -7347224, false);
      gg.drawString(toasts.getMinecraft().font, this.name, 30, 18, -1, false);
      gg.renderFakeItem(this.icon, 8, 8);
      return time >= 5000L ? Visibility.HIDE : Visibility.SHOW;
   }
}
