package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.net.ReaperHideAuraPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ReaperSelfAura {
   private static boolean lastSent;
   private static boolean inited;

   private ReaperSelfAura() {
   }

   public static boolean visible() {
      boolean firstPerson = Minecraft.getInstance().options.getCameraType().isFirstPerson();
      return firstPerson ? (Boolean)ClientConfig.REAPER_AURA_SELF_FIRST.get() : (Boolean)ClientConfig.REAPER_AURA_SELF_THIRD.get();
   }

   public static boolean hidden() {
      return "reaper".equals(ClientPowerState.get().powerId()) && !visible();
   }

   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.getConnection() != null) {
         boolean h = hidden();
         if (!inited || h != lastSent) {
            inited = true;
            lastSent = h;
            ReaperHideAuraPayload payload = new ReaperHideAuraPayload(h);
            PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
         }
      } else {
         inited = false;
      }
   }
}
