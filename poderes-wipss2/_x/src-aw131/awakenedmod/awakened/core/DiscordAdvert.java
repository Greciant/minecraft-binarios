package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.Config;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public final class DiscordAdvert {
   private DiscordAdvert() {
   }

   public static void onLogin(ServerPlayer player) {
      if ((Boolean)Config.DISCORD_ADVERT_ENABLED.get()) {
         long now = System.currentTimeMillis();
         long last = (Long)player.getData(AwakenedAttachments.DISCORD_LAST_SHOWN);
         long cooldownMs = (long)((Double)Config.DISCORD_ADVERT_COOLDOWN_HOURS.get() * 3600000.0);
         if (last == 0L || now - last >= cooldownMs) {
            player.setData(AwakenedAttachments.DISCORD_LAST_SHOWN, now);
            player.sendSystemMessage(buildMessage());
         }
      }
   }

   private static Component buildMessage() {
      String url = (String)Config.DISCORD_INVITE_URL.get();
      MutableComponent prefix = ChatFx.prefix();
      MutableComponent body = Component.literal("Enjoying your powers? Join the community on ").withStyle(ChatFormatting.GRAY);
      HoverEvent hover = ChatFx.showText(Component.literal("Click to open\n" + url).withStyle(ChatFormatting.GRAY));
      ClickEvent click = ChatFx.openUrl(url);
      MutableComponent link = Component.literal("Discord")
         .withStyle(s -> s.withColor(5793266).withBold(true).withUnderlined(true).withClickEvent(click).withHoverEvent(hover));
      MutableComponent tail = Component.literal(". Say hi, share your feedback, and get the latest news!").withStyle(ChatFormatting.GRAY);
      return prefix.append(body).append(link).append(tail);
   }
}
