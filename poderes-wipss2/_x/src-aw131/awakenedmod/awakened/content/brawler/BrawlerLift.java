package com.awakenedmod.awakened.content.brawler;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

public final class BrawlerLift {
   private static final Map<UUID, BrawlerLift.Lift> LIFTS = new ConcurrentHashMap<>();
   private static final Map<UUID, Integer> AIR_GRACE = new ConcurrentHashMap<>();
   private static int lastTick = -1;

   private BrawlerLift() {
   }

   public static boolean liftable(ServerPlayer player) {
      return !player.isPassenger() && !player.getAbilities().flying && !player.isFallFlying() && !player.isSwimming() && !player.isInWater();
   }

   public static void noteFooting(ServerPlayer player) {
      if (!player.onGround()) {
         AIR_GRACE.put(player.getUUID(), 6);
      } else {
         Integer left = AIR_GRACE.get(player.getUUID());
         if (left != null) {
            if (left <= 1) {
               AIR_GRACE.remove(player.getUUID());
            } else {
               AIR_GRACE.put(player.getUUID(), left - 1);
            }
         }
      }
   }

   public static boolean wantsLift(ServerPlayer player) {
      return !liftable(player)
         ? false
         : !player.onGround() || AIR_GRACE.containsKey(player.getUUID()) || !player.isShiftKeyDown() && player.getXRot() <= -25.0F;
   }

   public static boolean rideUp(ServerPlayer player, double heightBlocks, int hoverTicks) {
      return rideUp(player, heightBlocks, hoverTicks, null);
   }

   public static boolean rideUp(ServerPlayer player, double heightBlocks, int hoverTicks, @Nullable Vec3 drift) {
      if (!(heightBlocks < 0.4) && liftable(player)) {
         Vec3 v = drift == null ? player.getDeltaMovement() : drift;
         player.setDeltaMovement(v.x, BrawlerTuning.launchSpeedFor(heightBlocks), v.z);
         player.hurtMarked = true;
         player.resetFallDistance();
         BrawlerLift.Lift lift = LIFTS.computeIfAbsent(player.getUUID(), k -> new BrawlerLift.Lift());
         lift.rising = true;
         lift.ticks = 0;
         lift.hoverTicks = Math.min(hoverTicks, 30);
         lift.timeout = 100;
         return true;
      } else {
         return false;
      }
   }

   public static void coverFall(ServerPlayer player, int ticks) {
      player.resetFallDistance();
      BrawlerLift.Lift lift = LIFTS.get(player.getUUID());
      if (lift == null) {
         lift = new BrawlerLift.Lift();
         lift.rising = false;
         lift.timeout = ticks;
         LIFTS.put(player.getUUID(), lift);
      } else {
         lift.timeout = Math.max(lift.timeout, lift.ticks + ticks);
      }
   }

   public static void endHover(ServerPlayer player) {
      BrawlerLift.Lift lift = LIFTS.get(player.getUUID());
      if (lift != null) {
         lift.rising = false;
         lift.hoverTicks = 0;
         if (lift.floating) {
            lift.floating = false;
            player.removeEffect(MobEffects.SLOW_FALLING);
            restoreParked(player, lift);
         }
      }
   }

   public static void tickAll(MinecraftServer server) {
      int now = server.getTickCount();
      if (now != lastTick) {
         lastTick = now;
         Iterator<Entry<UUID, BrawlerLift.Lift>> it = LIFTS.entrySet().iterator();

         while (it.hasNext()) {
            Entry<UUID, BrawlerLift.Lift> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            BrawlerLift.Lift lift = entry.getValue();
            if (player != null && ++lift.ticks <= lift.timeout) {
               player.resetFallDistance();
               if (lift.rising) {
                  rise(player, lift);
               }

               if (lift.ticks >= 5 && (player.onGround() || player.isInWater())) {
                  it.remove();
               }
            } else {
               it.remove();
            }
         }
      }
   }

   private static void rise(ServerPlayer player, BrawlerLift.Lift lift) {
      boolean apex = lift.ticks >= 2 && player.getDeltaMovement().y <= 0.02 || lift.ticks >= 16 || player.verticalCollision;
      if (apex) {
         lift.rising = false;
         if (!player.onGround() && lift.hoverTicks > 0) {
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x * 0.2, 0.0, v.z * 0.2);
            player.hurtMarked = true;
            park(player, lift);
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, lift.hoverTicks + 2, 0, false, false));
            lift.floating = true;
         }
      }
   }

   private static void park(ServerPlayer player, BrawlerLift.Lift lift) {
      MobEffectInstance own = player.getEffect(MobEffects.SLOW_FALLING);
      if (own != null) {
         lift.parked = new MobEffectInstance(own);
         lift.parkedAt = lift.ticks;
      }
   }

   private static void restoreParked(ServerPlayer player, BrawlerLift.Lift lift) {
      MobEffectInstance parked = lift.parked;
      lift.parked = null;
      if (parked != null) {
         int left = parked.getDuration() - (lift.ticks - lift.parkedAt);
         if (left > 0) {
            player.addEffect(
               new MobEffectInstance(MobEffects.SLOW_FALLING, left, parked.getAmplifier(), parked.isAmbient(), parked.isVisible(), parked.showIcon())
            );
         }
      }
   }

   public static void clearPlayer(ServerPlayer player) {
      LIFTS.remove(player.getUUID());
      AIR_GRACE.remove(player.getUUID());
   }

   public static void clearAll() {
      LIFTS.clear();
      AIR_GRACE.clear();
      lastTick = -1;
   }

   private static final class Lift {
      boolean rising = true;
      int ticks;
      int hoverTicks;
      boolean floating;
      @Nullable
      MobEffectInstance parked;
      int parkedAt;
      int timeout = 100;
   }
}
