package com.awakenedmod.awakened.client;

import net.minecraft.client.Minecraft;

public final class GammaRageState {
   private static final ExpiringEntityValues<Float> ENTRIES = new ExpiringEntityValues<>(30);

   private GammaRageState() {
   }

   public static void put(int entityId, float rage) {
      ENTRIES.put(entityId, rage);
   }

   public static void clear() {
      ENTRIES.clear();
   }

   public static void tick() {
      ENTRIES.tick();
   }

   public static float trackedRage(int entityId) {
      Float rage = ENTRIES.get(entityId);
      return rage == null ? 0.0F : rage;
   }

   public static float localRage() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return 0.0F;
      } else {
         ClientPowerState ps = ClientPowerState.get();
         if ("gamma".equals(ps.powerId())) {
            float synced = ps.resourceFraction();
            if (synced > 0.0F) {
               return synced;
            }
         }

         return trackedRage(mc.player.getId());
      }
   }
}
