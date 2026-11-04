package com.awakenedmod.awakened.content.spider;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.SpiderTypePayload;
import com.awakenedmod.awakened.core.power.AbilityFeedback;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

public final class SpiderTypeSync {
   public static final String LOADED_TYPE = "spider.web_type";

   private SpiderTypeSync() {
   }

   public static WebType loaded(PowerData data) {
      return WebType.byKey(data.strVar("spider.web_type", WebType.SPLAT.key()));
   }

   public static boolean hasGauntlets(ServerPlayer player) {
      ICuriosItemHandler inv = (ICuriosItemHandler)CuriosApi.getCuriosInventory(player).orElse(null);
      return inv != null && inv.findFirstCurio(s -> s.getItem() instanceof WebGauntletsItem).isPresent();
   }

   public static void handleSelect(ServerPlayer player, String typeKey) {
      if (player != null) {
         PowerData data = PowerService.data(player);
         WebType type = WebType.byKey(typeKey);
         if (!type.isUnlocked(data, hasGauntlets(player))) {
            AbilityFeedback.fail(player, "awakened.spider.type_locked");
         } else {
            data.setStrVar("spider.web_type", type.key());
            push(player);
         }
      }
   }

   public static void push(ServerPlayer player) {
      PowerData data = PowerService.data(player);
      boolean gauntlets = hasGauntlets(player);
      PacketDistributor.sendToPlayer(player, new SpiderTypePayload(loaded(data).key(), gauntlets), new CustomPacketPayload[0]);
      SpiderVisualState.setGauntlets(player, gauntlets);
   }
}
