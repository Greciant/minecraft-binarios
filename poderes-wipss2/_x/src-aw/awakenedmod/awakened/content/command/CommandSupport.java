package com.awakenedmod.awakened.content.command;

import com.awakenedmod.awakened.core.MoveKeys;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class CommandSupport {
   private static final long NIGHT_START = 13000L;
   private static final long NIGHT_END = 23000L;

   private CommandSupport() {
   }

   public static ServerLevel level(ServerPlayer player) {
      return player.serverLevel();
   }

   public static MinecraftServer server(ServerPlayer player) {
      return level(player).getServer();
   }

   public static boolean forwardHeld(ServerPlayer player) {
      return MoveKeys.of(player).forward();
   }

   public static boolean movementInput(ServerPlayer player) {
      return MoveKeys.of(player).moving();
   }

   public static boolean night(Level level) {
      if (level.dimensionType().hasFixedTime()) {
         return false;
      } else {
         long time = Math.floorMod(level.getDayTime(), 24000L);
         return time >= 13000L && time < 23000L;
      }
   }

   public static Vec3 motion(LivingEntity body) {
      return body.getKnownMovement();
   }

   @Nullable
   public static Entity byUuid(@Nullable MinecraftServer server, @Nullable UUID id) {
      if (server != null && id != null) {
         ServerPlayer online = server.getPlayerList().getPlayer(id);
         if (online != null) {
            return online;
         } else {
            for (ServerLevel candidate : server.getAllLevels()) {
               Entity found = candidate.getEntity(id);
               if (found != null) {
                  return found;
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }
}
