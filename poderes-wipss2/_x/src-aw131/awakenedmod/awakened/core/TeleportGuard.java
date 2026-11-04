package com.awakenedmod.awakened.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class TeleportGuard {
   private static final List<TeleportGuard.Boundary> BOUNDARIES = new CopyOnWriteArrayList<>();

   private TeleportGuard() {
   }

   public static void register(TeleportGuard.Boundary boundary) {
      BOUNDARIES.add(boundary);
   }

   public static boolean crosses(ServerPlayer player, Vec3 from, Vec3 to) {
      if (BOUNDARIES.isEmpty()) {
         return false;
      } else {
         for (TeleportGuard.Boundary boundary : BOUNDARIES) {
            if (boundary.crosses(player, from, to)) {
               return true;
            }
         }

         return false;
      }
   }

   @FunctionalInterface
   public interface Boundary {
      boolean crosses(ServerPlayer var1, Vec3 var2, Vec3 var3);
   }
}
