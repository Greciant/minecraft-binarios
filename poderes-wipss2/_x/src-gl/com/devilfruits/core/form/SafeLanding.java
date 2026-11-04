package com.devilfruits.core.form;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class SafeLanding {
   private static final int MAX_TICKS = 1200;
   private static final Map<UUID, Long> ACTIVE = new HashMap<>();

   public static void grant(ServerPlayer player) {
      ACTIVE.put(player.getUUID(), player.level().getGameTime());
      player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true));
      player.fallDistance = 0.0F;
   }

   public static boolean isActive(ServerPlayer player) {
      return ACTIVE.containsKey(player.getUUID());
   }

   public static boolean consume(ServerPlayer player) {
      return ACTIVE.remove(player.getUUID()) != null;
   }

   public static void tick(ServerPlayer player) {
      Long since = ACTIVE.get(player.getUUID());
      if (since != null) {
         long age = player.level().getGameTime() - since;
         if (age > 1200L || player.isInWater() || age > 5L && player.onGround() && player.fallDistance == 0.0F) {
            ACTIVE.remove(player.getUUID());
         }
      }
   }

   public static void forget(UUID player) {
      ACTIVE.remove(player);
   }

   private SafeLanding() {
   }
}
