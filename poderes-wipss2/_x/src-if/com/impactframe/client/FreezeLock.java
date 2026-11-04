package com.impactframe.client;

import com.impactframe.ImpactConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class FreezeLock {
   private static final long MAX_LOCK_NANOS = 2000000000L;
   private static boolean serverLock;
   private static long serverLockSince;
   private static boolean locked;
   private static Vec3 savedMotion = Vec3.ZERO;
   private static boolean savedNoGravity;

   private FreezeLock() {
   }

   public static void setServerLock(boolean active) {
      serverLock = active;
      serverLockSince = System.nanoTime();
   }

   static boolean isLocked() {
      return locked;
   }

   private static boolean shouldLock() {
      if (serverLock && ImpactConfig.serverFeatures() && ImpactConfig.lockMovement()) {
         if (System.nanoTime() - serverLockSince > 2000000000L) {
            serverLock = false;
            return false;
         } else {
            Level level = Minecraft.getInstance().level;
            return level != null && level.tickRateManager().isFrozen();
         }
      } else {
         return false;
      }
   }

   static void tick(LocalPlayer player) {
      boolean lock = player != null && shouldLock();
      if (lock && !locked) {
         locked = true;
         savedMotion = player.getDeltaMovement();
         savedNoGravity = player.isNoGravity();
         player.setNoGravity(true);
      }

      if (lock) {
         player.setDeltaMovement(Vec3.ZERO);
      } else if (locked) {
         release(player);
      }
   }

   static void applyToInput(Input input) {
      if (locked) {
         input.leftImpulse = 0.0F;
         input.forwardImpulse = 0.0F;
         input.up = false;
         input.down = false;
         input.left = false;
         input.right = false;
         input.jumping = false;
         input.shiftKeyDown = false;
      }
   }

   static void reset() {
      serverLock = false;
      locked = false;
      savedMotion = Vec3.ZERO;
   }

   private static void release(LocalPlayer player) {
      locked = false;
      if (player != null) {
         player.setNoGravity(savedNoGravity);
         player.setDeltaMovement(savedMotion);
      }

      savedMotion = Vec3.ZERO;
   }
}
