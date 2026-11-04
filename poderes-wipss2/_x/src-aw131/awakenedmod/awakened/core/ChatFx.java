package com.awakenedmod.awakened.core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.Action;

public final class ChatFx {
   private ChatFx() {
   }

   public static MutableComponent prefix() {
      return Component.literal("[Awakened] ").withStyle(s -> s.withColor(ChatFormatting.GOLD).withBold(true));
   }

   public static Component note(String key, Object... args) {
      return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
   }

   public static MutableComponent button(String labelKey, ChatFormatting color, String command, String tipKey, Object... tipArgs) {
      HoverEvent hover = showText(Component.translatable(tipKey, tipArgs).withStyle(ChatFormatting.GRAY));
      ClickEvent click = runCommand(command);
      return Component.translatable(labelKey).withStyle(s -> s.withColor(color).withBold(true).withClickEvent(click).withHoverEvent(hover));
   }

   public static ClickEvent runCommand(String command) {
      return new ClickEvent(Action.RUN_COMMAND, "/" + command);
   }

   public static ClickEvent suggestCommand(String text) {
      return new ClickEvent(Action.SUGGEST_COMMAND, text);
   }

   public static ClickEvent openUrl(String url) {
      return new ClickEvent(Action.OPEN_URL, url);
   }

   public static HoverEvent showText(Component text) {
      return new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, text);
   }
}
