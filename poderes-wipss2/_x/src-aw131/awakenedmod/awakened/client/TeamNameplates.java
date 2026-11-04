package com.awakenedmod.awakened.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

public final class TeamNameplates {
   private TeamNameplates() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(TeamNameplates::onRenderNameTag);
   }

   private static void onRenderNameTag(RenderNameTagEvent event) {
      if (event.getEntity() instanceof Player player) {
         Component tinted = tint(player, event.getContent());
         if (tinted != null) {
            event.setContent(tinted);
         }
      }
   }

   private static Component tint(Player player, Component content) {
      if (content == null) {
         return null;
      } else {
         Integer colour = ClientTeamState.nameplateTint(player.getUUID());
         return colour == null ? null : content.copy().withStyle(s -> s.withColor(colour));
      }
   }
}
