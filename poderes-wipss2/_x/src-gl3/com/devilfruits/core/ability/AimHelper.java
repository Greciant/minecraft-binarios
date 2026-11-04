package com.devilfruits.core.ability;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.network.AimPayload;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class AimHelper {
   public static final double MAX_RANGE = 160.0;
   public static final double MAX_ANGLE = 35.0;
   public static final int STALE_TICKS = 25;
   private static final double MIN_DISTANCE = 1.0;
   private static final double COS_MAX = Math.cos(Math.toRadians(35.0));
   public static volatile boolean debug = Boolean.getBoolean("devilfruits.aimdebug");
   private static final Map<UUID, AimHelper.Aim> AIMS = new ConcurrentHashMap<>();

   public static void receive(ServerPlayer player, AimPayload payload) {
      Vec3 p = payload.point();
      if (payload.valid() && Double.isFinite(p.x) && Double.isFinite(p.y) && Double.isFinite(p.z)) {
         AIMS.put(player.getUUID(), new AimHelper.Aim(p, player.server.getTickCount()));
      } else {
         AIMS.remove(player.getUUID());
      }
   }

   public static void forget(UUID id) {
      AIMS.remove(id);
   }

   @Nullable
   public static Vec3 point(Player player) {
      if (player instanceof ServerPlayer sp) {
         AimHelper.Aim aim = AIMS.get(sp.getUUID());
         if (aim == null) {
            return null;
         } else {
            int age = sp.server.getTickCount() - aim.tick;
            if (age >= 0 && age <= 25) {
               Vec3 to = aim.point.subtract(sp.getEyePosition());
               double d = to.length();
               return !(d < 1.0) && !(d > 160.0) && !(to.scale(1.0 / d).dot(sp.getLookAngle()) < COS_MAX) ? aim.point : null;
            } else {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   public static Vec3 look(Player player) {
      return lookFrom(player, player.getEyePosition());
   }

   public static Vec3 lookFrom(Player player, Vec3 origin) {
      Vec3 look = player.getLookAngle();
      Vec3 point = point(player);
      if (point == null) {
         return look;
      } else {
         Vec3 to = point.subtract(origin);
         if (to.lengthSqr() < 1.0) {
            return look;
         } else {
            Vec3 dir = to.normalize();
            if (debug) {
               DevilFruits.LOGGER
                  .info(
                     "[devilfruits aim] {}: look {} -> crosshair {} ({} deg apart), point {}",
                     new Object[]{
                        player.getGameProfile().getName(),
                        fmt(look),
                        fmt(dir),
                        String.format("%.2f", Math.toDegrees(Math.acos(Math.min(1.0, look.dot(dir))))),
                        fmt(point)
                     }
                  );
            }

            return dir;
         }
      }
   }

   private static String fmt(Vec3 v) {
      return String.format("(%.3f, %.3f, %.3f)", v.x, v.y, v.z);
   }

   private AimHelper() {
   }

   private static record Aim(Vec3 point, int tick) {
   }
}
