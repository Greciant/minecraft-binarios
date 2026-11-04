package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.screen.RadialWheelScreen;
import com.awakenedmod.awakened.content.spider.WebShotAbility;
import com.awakenedmod.awakened.content.spider.WebType;
import com.awakenedmod.awakened.core.net.SpiderSelectTypePayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SpiderWheel {
   private SpiderWheel() {
   }

   public static RadialWheelScreen create() {
      boolean hasGauntlets = SpiderClientState.hasGauntlets();
      ClientPowerState power = ClientPowerState.get();
      int mastery = power.masteryLevel();
      boolean infinite = power.infiniteStamina();
      float stamina = power.stamina();
      String loadedKey = SpiderClientState.loadedType();
      List<RadialWheelScreen.RadialOption> options = new ArrayList<>();

      for (WebType type : WebType.values()) {
         boolean unlocked = isUnlockedClient(type, hasGauntlets, mastery);
         Component lockHint = unlocked ? null : Component.literal("Lv " + type.unlockLevel());
         float shotCost = WebShotAbility.perShotCost(type);
         boolean affordable = infinite || stamina >= shotCost;
         Component cost = Component.literal(formatCost(shotCost));
         options.add(
            new RadialWheelScreen.RadialOption(
               null,
               type.themeColor(),
               type.displayName(),
               !unlocked,
               lockHint,
               type.key().equals(loadedKey),
               type.ordinal(),
               cost,
               affordable,
               new ItemStack(type.icon())
            )
         );
      }

      return new RadialWheelScreen(
         Component.translatable("awakened.spider.hud_loaded"),
         options,
         ordinal -> send(new SpiderSelectTypePayload(WebType.byOrdinal(ordinal).key())),
         AwakenedKeybinds.SECONDARY_ABILITY
      );
   }

   private static boolean isUnlockedClient(WebType type, boolean hasGauntlets, int mastery) {
      return (!type.gauntletGated() || hasGauntlets) && mastery >= type.unlockLevel();
   }

   static String formatCost(float cost) {
      return (double)cost == Math.rint((double)cost) ? Integer.toString((int)cost) : String.format(Locale.ROOT, "%.1f", cost);
   }

   private static void send(CustomPacketPayload payload) {
      PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
   }
}
