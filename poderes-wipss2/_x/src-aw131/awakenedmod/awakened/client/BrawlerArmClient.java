package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.render.ArmStretch;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.BrawlerVisual;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class BrawlerArmClient {
   private static final Map<Integer, BrawlerArmClient.Track> TRACKS = new HashMap<>();
   private static final double NANOS_PER_TICK = 5.0E7;

   private BrawlerArmClient() {
   }

   public static void register() {
      ArmStretch.addSource(BrawlerArmClient::reach);
      AwakenedAnimations.holdLoop(AwId.of("brawler_reel_in"), BrawlerArmClient::attached);
   }

   public static boolean attached(Player player) {
      return ((BrawlerVisual)player.getData(AwakenedAttachments.BRAWLER_VISUAL)).attached();
   }

   public static ArmStretch.Reach reach(LivingEntity entity, float partialTick) {
      BrawlerVisual visual = (BrawlerVisual)entity.getData(AwakenedAttachments.BRAWLER_VISUAL);
      int id = entity.getId();
      long now = System.nanoTime();
      BrawlerArmClient.Track track = TRACKS.get(id);
      boolean left = false;
      if (!visual.active()) {
         if (track == null) {
            return null;
         } else {
            if (!track.local) {
               if (track.tip == null || track.reach <= 0.0F) {
                  TRACKS.remove(id);
                  return null;
               }

               track.begin(3, true, now);
            }

            return retract(id, track, track.tip, elapsed(track, now), left);
         }
      } else {
         if (track == null) {
            track = new BrawlerArmClient.Track();
            TRACKS.put(id, track);
         }

         if (track.local || track.phase != visual.phase()) {
            track.begin(visual.phase(), false, now);
         }

         double elapsedTicks = elapsed(track, now);
         switch (visual.phase()) {
            case 1:
            case 2:
               Vec3 target = chestOf(visual.target(), partialTick);
               if (target == null) {
                  if (track.tip == null) {
                     return null;
                  }

                  track.begin(3, true, now);
                  return retract(id, track, track.tip, 0.0, left);
               }

               float reach = (float)Math.min(1.0, (double)track.fromReach + elapsedTicks / 3.0);
               track.tip = target;
               track.reach = reach;
               return new ArmStretch.Reach(target, reach, left);
            case 3:
               return retract(id, track, new Vec3(visual.x(), visual.y(), visual.z()), elapsedTicks, left);
            default:
               TRACKS.remove(id);
               return null;
         }
      }
   }

   private static double elapsed(BrawlerArmClient.Track track, long now) {
      return Math.max(0.0, (double)(now - track.startNanos) / 5.0E7);
   }

   private static ArmStretch.Reach retract(int id, BrawlerArmClient.Track track, Vec3 tip, double elapsedTicks, boolean left) {
      float from = track.fromReach <= 0.0F ? 1.0F : track.fromReach;
      float reach = (float)Math.max(0.0, (double)from - elapsedTicks / 3.0);
      if (reach <= 0.0F) {
         TRACKS.remove(id);
         return null;
      } else {
         track.tip = tip;
         track.reach = reach;
         return new ArmStretch.Reach(tip, reach, left);
      }
   }

   private static Vec3 chestOf(int entityId, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return null;
      } else {
         Entity target = mc.level.getEntity(entityId);
         return target != null && target.isAlive() ? target.getPosition(partialTick).add(0.0, (double)target.getBbHeight() * 0.55, 0.0) : null;
      }
   }

   private static final class Track {
      int phase = 0;
      boolean local;
      long startNanos;
      float fromReach;
      Vec3 tip;
      float reach;

      void begin(int phase, boolean local, long now) {
         this.phase = phase;
         this.local = local;
         this.startNanos = now;
         this.fromReach = this.reach;
      }
   }
}
