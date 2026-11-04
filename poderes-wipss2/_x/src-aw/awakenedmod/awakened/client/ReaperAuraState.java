package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.screen.ReaperAuraSliderScreen;
import com.awakenedmod.awakened.content.reaper.ReaperAttachments;
import com.awakenedmod.awakened.content.reaper.ReaperScaling;
import net.minecraft.client.Minecraft;

public final class ReaperAuraState {
   private static final ExpiringEntityValues<ReaperAuraState.Entry> ENTRIES = new ExpiringEntityValues<>(30);

   private ReaperAuraState() {
   }

   public static void put(int entityId, int tier, float shownPct) {
      ENTRIES.put(entityId, new ReaperAuraState.Entry(tier, shownPct));
   }

   public static void applyLocalAware(int entityId, int tier, float shownPct) {
      put(entityId, tier, shownPct);
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && entityId == mc.player.getId() && !(mc.screen instanceof ReaperAuraSliderScreen)) {
         ReaperAuraSettings.seedFromServer(shownPct);
      }
   }

   public static void clear() {
      ENTRIES.clear();
   }

   public static void tick() {
      ENTRIES.tick();
   }

   public static int trackedTier(int entityId) {
      ReaperAuraState.Entry e = ENTRIES.get(entityId);
      return e == null ? -1 : e.tier();
   }

   public static float trackedShown(int entityId) {
      ReaperAuraState.Entry e = ENTRIES.get(entityId);
      return e == null ? 0.0F : e.shownPct();
   }

   public static float localShown() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return -1.0F;
      } else {
         ReaperAuraState.Entry e = ENTRIES.get(mc.player.getId());
         return e == null ? -1.0F : e.shownPct();
      }
   }

   public static int localTier() {
      ClientPowerState ps = ClientPowerState.get();
      return !"reaper".equals(ps.powerId()) ? -1 : localMilestone();
   }

   private static int localMilestone() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return 0;
      } else {
         long souls = ReaperAttachments.souls(mc.player).souls();
         return ReaperScaling.milestoneIndex(souls);
      }
   }

   private static record Entry(int tier, float shownPct) {
   }
}
