package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.core.Harm;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class BrawlerFollowUp {
   private static final Map<UUID, List<BrawlerFollowUp.Pending>> QUEUES = new ConcurrentHashMap<>();
   private static int lastTick = -1;

   private BrawlerFollowUp() {
   }

   static void shove(ServerPlayer owner, LivingEntity target, Vec3 velocity, int delayTicks, Mark mark, int baseTicks, Mark.Flavour flavour, String abilityId) {
      Marks.release(target);
      BrawlerHitStop.moving(target, delayTicks);
      Harm.setMotion(owner, target, velocity);
      schedule(owner, target, delayTicks, mark, baseTicks, flavour, abilityId);
   }

   static void schedule(ServerPlayer owner, LivingEntity target, int delayTicks, Mark mark, int baseTicks, Mark.Flavour flavour, String abilityId) {
      if (target != null && target.isAlive() && !target.isRemoved()) {
         if (delayTicks <= 0) {
            Marks.apply(owner, target, mark, baseTicks, flavour, abilityId);
         } else {
            QUEUES.computeIfAbsent(owner.getUUID(), k -> new ArrayList<>())
               .add(new BrawlerFollowUp.Pending(target, owner.level().getGameTime() + (long)delayTicks, delayTicks, mark, baseTicks, flavour, abilityId));
         }
      }
   }

   static void tickAll(MinecraftServer server) {
      int now = server.getTickCount();
      if (now != lastTick && !QUEUES.isEmpty()) {
         lastTick = now;
         Iterator<Entry<UUID, List<BrawlerFollowUp.Pending>>> queues = QUEUES.entrySet().iterator();

         while (queues.hasNext()) {
            Entry<UUID, List<BrawlerFollowUp.Pending>> entry = queues.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (owner == null) {
               queues.remove();
            } else {
               List<BrawlerFollowUp.Pending> queue = entry.getValue();
               Iterator<BrawlerFollowUp.Pending> it = queue.iterator();

               while (it.hasNext()) {
                  BrawlerFollowUp.Pending pending = it.next();
                  LivingEntity target = pending.target;
                  if (target.isAlive() && !target.isRemoved() && target.level() == owner.level()) {
                     long gameTime = owner.level().getGameTime();
                     if (Marks.settling(target)) {
                        pending.at = gameTime + (long)pending.delayTicks;
                     } else if (gameTime >= pending.at) {
                        it.remove();
                        Marks.apply(owner, target, pending.mark, pending.baseTicks, pending.flavour, pending.abilityId);
                     }
                  } else {
                     it.remove();
                  }
               }

               if (queue.isEmpty()) {
                  queues.remove();
               }
            }
         }
      }
   }

   public static void clearPlayer(ServerPlayer player) {
      QUEUES.remove(player.getUUID());
   }

   static void clearAll() {
      QUEUES.clear();
      lastTick = -1;
   }

   private static final class Pending {
      final LivingEntity target;
      final int delayTicks;
      long at;
      final Mark mark;
      final int baseTicks;
      final Mark.Flavour flavour;
      final String abilityId;

      Pending(LivingEntity target, long at, int delayTicks, Mark mark, int baseTicks, Mark.Flavour flavour, String abilityId) {
         this.target = target;
         this.at = at;
         this.delayTicks = delayTicks;
         this.mark = mark;
         this.baseTicks = baseTicks;
         this.flavour = flavour;
         this.abilityId = abilityId;
      }
   }
}
