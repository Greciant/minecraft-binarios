package com.awakenedmod.awakened.content.exosavant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ExpiringTargetSet {
   private final Map<UUID, Map<UUID, Long>> sets = new ConcurrentHashMap<>();
   private final double maxDistanceSqr;

   public ExpiringTargetSet(double maxDistance) {
      this.maxDistanceSqr = maxDistance * maxDistance;
   }

   public void add(ServerPlayer player, LivingEntity target, int lifetimeTicks) {
      long expiry = player.serverLevel().getGameTime() + (long)lifetimeTicks;
      this.sets.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>()).put(target.getUUID(), expiry);
   }

   public boolean contains(ServerPlayer player, Entity target) {
      if (player != null && target != null) {
         Map<UUID, Long> mine = this.sets.get(player.getUUID());
         if (mine == null) {
            return false;
         } else {
            Long expiry = mine.get(target.getUUID());
            return expiry != null && player.serverLevel().getGameTime() < expiry;
         }
      } else {
         return false;
      }
   }

   public List<LivingEntity> live(ServerPlayer player) {
      this.prune(player);
      List<LivingEntity> out = new ArrayList<>();
      Map<UUID, Long> mine = this.sets.get(player.getUUID());
      if (mine == null) {
         return out;
      } else {
         ServerLevel level = player.serverLevel();

         for (UUID id : mine.keySet()) {
            Entity var8 = level.getEntity(id);
            if (var8 instanceof LivingEntity) {
               LivingEntity living = (LivingEntity)var8;
               if (living.isAlive()) {
                  out.add(living);
               }
            }
         }

         return out;
      }
   }

   public void prune(ServerPlayer player) {
      Map<UUID, Long> mine = this.sets.get(player.getUUID());
      if (mine != null) {
         long now = player.serverLevel().getGameTime();
         ServerLevel level = player.serverLevel();
         Vec3 center = player.position();
         Iterator<Entry<UUID, Long>> it = mine.entrySet().iterator();

         while (it.hasNext()) {
            Entry<UUID, Long> e = it.next();
            if (now >= e.getValue()) {
               it.remove();
            } else {
               Entity var10 = level.getEntity(e.getKey());
               if (var10 instanceof LivingEntity) {
                  LivingEntity living = (LivingEntity)var10;
                  if (living.isAlive() && !(living.distanceToSqr(center) > this.maxDistanceSqr)) {
                     continue;
                  }
               }

               it.remove();
            }
         }

         if (mine.isEmpty()) {
            this.sets.remove(player.getUUID());
         }
      }
   }

   public void clear(ServerPlayer player) {
      this.sets.remove(player.getUUID());
   }
}
