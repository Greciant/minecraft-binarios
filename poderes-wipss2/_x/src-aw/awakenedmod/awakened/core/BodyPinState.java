package com.awakenedmod.awakened.core;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BodyPinState {
   private static final Set<Integer> CLIENT = ConcurrentHashMap.newKeySet();
   private static final Set<UUID> SERVER = ConcurrentHashMap.newKeySet();

   private BodyPinState() {
   }

   public static void setClient(int entityId, boolean pinned) {
      if (pinned) {
         CLIENT.add(entityId);
      } else {
         CLIENT.remove(entityId);
      }
   }

   public static boolean clientPinned(int entityId) {
      return CLIENT.contains(entityId);
   }

   public static void clearClient() {
      CLIENT.clear();
   }

   public static void setServer(UUID id, boolean pinned) {
      if (pinned) {
         SERVER.add(id);
      } else {
         SERVER.remove(id);
      }
   }

   public static boolean serverPinned(UUID id) {
      return SERVER.contains(id);
   }
}
