package com.awakenedmod.awakened.core;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.Enemy;

public final class Taunt {
   private static final double BLIND_RADIUS = 4.0;

   private Taunt() {
   }

   public static int pull(ServerPlayer player, double radius) {
      return turn(player, radius).size();
   }

   public static List<Mob> turn(ServerPlayer player, double radius) {
      ServerLevel r2 = player.serverLevel();
      if (r2 instanceof ServerLevel && !player.isSpectator() && player.isAlive()) {
         double r2x = radius * radius;
         List<Mob> mobs = r2.getEntitiesOfClass(
            Mob.class, player.getBoundingBox().inflate(radius), m -> m instanceof Enemy && m.isAlive() && m.distanceToSqr(player) <= r2
         );
         List<Mob> turned = new ArrayList<>();

         for (Mob mob : mobs) {
            if (mob.getTarget() != player && eligible(player, mob)) {
               mob.setTarget(player);
               if (mob.getBrain().checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED)) {
                  mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, player);
               }

               turned.add(mob);
            }
         }

         return turned;
      } else {
         return List.of();
      }
   }

   private static boolean eligible(ServerPlayer player, Mob mob) {
      return Harm.allowed(player, mob) && !Foes.fightsFor(player, mob) && mob.canAttack(player)
         ? mob.distanceToSqr(player) <= 16.0 || mob.hasLineOfSight(player)
         : false;
   }
}
