package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.ExoVisual;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class ExoFlightHint {
   private static final int MAX_HINTS = 3;
   private static boolean wasFlying;
   private static boolean initialJet;
   private static boolean countedThisFlight;

   private ExoFlightHint() {
   }

   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player == null) {
         wasFlying = false;
         countedThisFlight = false;
      } else {
         ExoVisual vis = (ExoVisual)player.getData(AwakenedAttachments.EXO_VISUAL);
         boolean flying = vis.flying();
         if (flying && !wasFlying) {
            initialJet = vis.jet();
            countedThisFlight = false;
            maybeShowHint(player);
         } else if (flying && !countedThisFlight && !vis.sputtering() && vis.jet() != initialJet) {
            countedThisFlight = true;
            ClientConfig.EXO_FLIGHT_HINTS_SHOWN.set(3);
         } else if (!flying && wasFlying) {
            countedThisFlight = false;
         }

         wasFlying = flying;
      }
   }

   private static void maybeShowHint(LocalPlayer player) {
      int shown = (Integer)ClientConfig.EXO_FLIGHT_HINTS_SHOWN.get();
      if (shown < 3) {
         ClientConfig.EXO_FLIGHT_HINTS_SHOWN.set(shown + 1);
         player.displayClientMessage(
            Component.translatable(
                  "awakened.exosavant.flight_hint",
                  new Object[]{AwakenedKeybinds.ACTIVATE.getTranslatedKeyMessage(), AwakenedKeybinds.SECONDARY_ABILITY.getTranslatedKeyMessage()}
               )
               .withStyle(ChatFormatting.AQUA),
            false
         );
      }
   }
}
