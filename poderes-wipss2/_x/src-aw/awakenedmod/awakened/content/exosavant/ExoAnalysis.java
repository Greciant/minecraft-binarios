package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.data.PowerData;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class ExoAnalysis {
   public static final int UNLOCK = 10;
   private static final int GAZE_TICKS = 20;
   private static final double GAZE_RANGE = 32.0;
   private static final Map<UUID, UUID> GAZE_TARGET = new ConcurrentHashMap<>();
   private static final Map<UUID, Integer> GAZE_HELD = new ConcurrentHashMap<>();

   private ExoAnalysis() {
   }

   public static boolean unlocked(PowerData data) {
      return data.masteryLevel() >= 10;
   }

   public static boolean analyzing(ServerPlayer player) {
      int held = GAZE_HELD.getOrDefault(player.getUUID(), 0);
      return held > 0 && held < 20;
   }

   public static void tick(ServerPlayer player, PowerData data) {
      if (player.isAlive() && ExoSuit.isSuited(player) && unlocked(data)) {
         LivingEntity looked = AimUtil.aimedLivingEntity(player, 32.0, e -> ExoTags.taggable(e, player));
         if (looked == null) {
            reset(player);
         } else {
            UUID id = player.getUUID();
            if (!looked.getUUID().equals(GAZE_TARGET.get(id))) {
               GAZE_TARGET.put(id, looked.getUUID());
               GAZE_HELD.put(id, 1);
            } else {
               int held = Math.min(20, GAZE_HELD.getOrDefault(id, 0) + 1);
               GAZE_HELD.put(id, held);
               if (held >= 20) {
                  ExoTags.tag(player, looked);
               }
            }
         }
      } else {
         reset(player);
      }
   }

   private static void reset(ServerPlayer player) {
      GAZE_TARGET.remove(player.getUUID());
      GAZE_HELD.remove(player.getUUID());
   }

   public static void clear(ServerPlayer player) {
      reset(player);
   }
}
