package com.awakenedmod.awakened.core.trade;

import com.awakenedmod.awakened.core.ChatFx;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class TradeChat {
   private TradeChat() {
   }

   static void sendInvite(ServerPlayer from, ServerPlayer to, Component fromPower, Component toPower, int seconds) {
      String fromName = from.getName().getString();
      to.sendSystemMessage(
         ChatFx.prefix()
            .append(ChatFx.note("awakened.trade.invited_you", fromName, fromPower, toPower))
            .append(ChatFx.button("awakened.trade.button_review", ChatFormatting.GREEN, "awakened trade review", "awakened.trade.button_review_tip", fromName))
            .append(Component.literal(" "))
            .append(ChatFx.button("awakened.trade.button_decline", ChatFormatting.RED, "awakened trade decline", "awakened.trade.button_decline_tip", fromName))
      );
      tell(from, "awakened.trade.invite_sent", to.getName().getString(), seconds);
   }

   static void tell(ServerPlayer target, String key, Object... args) {
      target.sendSystemMessage(ChatFx.prefix().append(ChatFx.note(key, args)));
   }

   static void refuse(ServerPlayer target, Component reason) {
      target.displayClientMessage(reason.copy().withStyle(ChatFormatting.RED), true);
   }
}
