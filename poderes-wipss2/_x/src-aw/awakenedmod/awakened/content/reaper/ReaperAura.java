package com.awakenedmod.awakened.content.reaper;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.ReaperAuraPayload;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ReaperAura {
   private static final String K_SHOWN = "reaper_aura_shown";
   private static final String K_PREF = "reaper_aura_pref";
   private static final float MIN_SHOWN = 0.0F;
   private static final float MAX_SHOWN = 1.0F;
   private static final int BROADCAST_INTERVAL = 8;
   private static final float DRAIN_PER_SEC = 0.2F;
   private static final int DRAIN_SYNC_TICKS = 3;
   private static final Set<UUID> SELF_HIDDEN = ConcurrentHashMap.newKeySet();

   private ReaperAura() {
   }

   public static void setSelfHidden(ServerPlayer player, boolean hide) {
      if (hide) {
         SELF_HIDDEN.add(player.getUUID());
      } else {
         SELF_HIDDEN.remove(player.getUUID());
      }
   }

   public static float shown(PowerData data) {
      return Mth.clamp(data.var("reaper_aura_shown", 1.0F), 0.0F, 1.0F);
   }

   private static float pref(PowerData data) {
      return Mth.clamp(data.var("reaper_aura_pref", 1.0F), 0.0F, 1.0F);
   }

   public static void toggle(ServerPlayer player) {
      if (player != null && "reaper".equals(PowerService.data(player).powerId())) {
         PowerData data = PowerService.data(player);
         float next = shown(data) > 0.001F ? 0.0F : pref(data);
         data.setVar("reaper_aura_shown", next);
         PowerService.sync(player);
         broadcast(player);
      }
   }

   public static void setShown(ServerPlayer player, float pct) {
      if (player != null && "reaper".equals(PowerService.data(player).powerId())) {
         PowerData data = PowerService.data(player);
         float clamped = Mth.clamp(pct, 0.0F, 1.0F);
         data.setVar("reaper_aura_shown", clamped);
         if (clamped > 0.001F) {
            data.setVar("reaper_aura_pref", clamped);
         }

         PowerService.sync(player);
         broadcast(player);
      }
   }

   private static void broadcast(ServerPlayer player) {
      long souls = ReaperAttachments.souls(player).souls();
      int tier = ReaperScaling.milestoneIndex(souls);
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(
         player, new ReaperAuraPayload(player.getId(), tier, shown(PowerService.data(player))), new CustomPacketPayload[0]
      );
   }

   public static void tick(ServerPlayer player, PowerData data) {
      long souls = ReaperAttachments.souls(player).souls();
      float shown = shown(data);
      if (shown < 0.999F) {
         long gameTime = player.serverLevel().getGameTime();
         float perTick = StaminaSystem.effectiveCost(0.2F * (1.0F - shown) * ReaperScaling.staminaMult(souls) / 20.0F, ReaperAura.class.getSimpleName());
         float remaining = StaminaSystem.drainSustained(data, gameTime, perTick);
         if (remaining <= 0.0F && perTick > 0.0F) {
            data.setVar("reaper_aura_shown", 1.0F);
            shown = 1.0F;
            PowerService.sync(player);
            broadcast(player);
         } else if (player.tickCount % 3 == 0) {
            PowerService.sync(player);
         }
      }

      int tier = ReaperScaling.milestoneIndex(souls);
      ReaperFx.aura(player.serverLevel(), player, tier, shown, SELF_HIDDEN.contains(player.getUUID()));
      if (player.tickCount % 8 == 0) {
         PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new ReaperAuraPayload(player.getId(), tier, shown), new CustomPacketPayload[0]);
      }
   }
}
