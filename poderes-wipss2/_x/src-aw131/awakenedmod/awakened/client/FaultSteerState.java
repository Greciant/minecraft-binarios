package com.awakenedmod.awakened.client;

import net.minecraft.client.Minecraft;

public final class FaultSteerState {
   private static final long SAFETY_TICKS = 40L;
   private static boolean live;
   private static long expiresAt;

   private FaultSteerState() {
   }

   public static void set(boolean isLive) {
      live = isLive;
      expiresAt = isLive ? gameTime() + 40L : 0L;
   }

   public static boolean live() {
      if (live && gameTime() > expiresAt) {
         live = false;
      }

      return live;
   }

   private static long gameTime() {
      Minecraft mc = Minecraft.getInstance();
      return mc.level != null ? mc.level.getGameTime() : 0L;
   }
}
