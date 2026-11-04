package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Power;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;

public final class PowerSuppression {
   private static final List<PowerSuppression.Source> SOURCES = new CopyOnWriteArrayList<>();
   private static final Map<UUID, String> TOLD = new ConcurrentHashMap<>();

   private PowerSuppression() {
   }

   public static void register(Predicate<LivingEntity> source) {
      register(source, null);
   }

   public static void register(Predicate<LivingEntity> source, @Nullable String reasonKey) {
      SOURCES.add(new PowerSuppression.Source(source, reasonKey));
   }

   public static boolean test(LivingEntity body) {
      return cause(body) != null;
   }

   @Nullable
   private static PowerSuppression.Source cause(LivingEntity body) {
      for (PowerSuppression.Source source : SOURCES) {
         if (source.test().test(body)) {
            return source;
         }
      }

      return null;
   }

   public static boolean isSuppressed(Entity body) {
      return body != null && Boolean.TRUE.equals(body.getExistingDataOrNull(AwakenedAttachments.POWER_SUPPRESSED.get()));
   }

   public static boolean update(ServerPlayer player, Power power, PowerData data) {
      PowerSuppression.Source cause = player.isAlive() ? cause(player) : null;
      boolean now = cause != null;
      boolean was = isSuppressed(player);
      if (now) {
         tell(player, cause);
      } else {
         TOLD.remove(player.getUUID());
      }

      if (now == was) {
         return now;
      } else {
         if (now) {
            player.setData(AwakenedAttachments.POWER_SUPPRESSED.get(), Boolean.TRUE);
            if (power != null) {
               power.clearAbilityState(player);
               power.onSuppressed(player, data);
            }

            TeleportUtil.unwedge(player);
         } else {
            player.removeData(AwakenedAttachments.POWER_SUPPRESSED.get());
            if (power != null) {
               power.onUnsuppressed(player, data);
            }
         }

         PowerService.sync(player);
         return now;
      }
   }

   private static void tell(ServerPlayer player, PowerSuppression.Source cause) {
      String key = cause.reasonKey();
      if (key != null && !key.equals(TOLD.put(player.getUUID(), key))) {
         player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY));
      } else if (key == null) {
         TOLD.remove(player.getUUID());
      }
   }

   public static void release(ServerPlayer player, Power power) {
      if (isSuppressed(player)) {
         player.removeData(AwakenedAttachments.POWER_SUPPRESSED.get());
         TOLD.remove(player.getUUID());
         if (power != null) {
            power.onUnsuppressed(player, PowerService.data(player));
         }
      }
   }

   static {
      NeoForge.EVENT_BUS.addListener(e -> TOLD.remove(e.getEntity().getUUID()));
   }

   private static record Source(Predicate<LivingEntity> test, @Nullable String reasonKey) {
   }
}
