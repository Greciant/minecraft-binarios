package com.impactframe.client;

public final class FreezeLockBridge {
   private FreezeLockBridge() {
   }

   public static void onFreezeState(boolean active) {
      FreezeLock.setServerLock(active);
   }
}
