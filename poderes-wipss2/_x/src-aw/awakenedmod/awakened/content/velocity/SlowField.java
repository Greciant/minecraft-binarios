package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.core.TeamTimeLock;
import com.awakenedmod.awakened.core.TickFreeze;
import com.awakenedmod.awakened.core.rules.PvpRules;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class SlowField {
   private static final double HYSTERESIS = 1.0;
   private static final double LANDED_SPEED_SQR = 1.0E-4;
   private final Map<Integer, Entity> caught = new LinkedHashMap<>();
   private final Map<Integer, Entity> allies = new LinkedHashMap<>();
   private int divisor;

   void sweep(ServerPlayer player, double radius, int divisor) {
      if (divisor != this.divisor) {
         this.restrength(divisor);
      }

      ServerLevel level = player.serverLevel();
      Vec3 centre = player.position();
      double release = (radius + 1.0) * (radius + 1.0);
      Iterator<Entry<Integer, Entity>> held = this.caught.entrySet().iterator();

      while (held.hasNext()) {
         Entity entity = held.next().getValue();
         if (entity.level() != level || entity.distanceToSqr(centre) > release || !catchable(player, entity)) {
            TickFreeze.unslow(entity, this.divisor);
            held.remove();
         }
      }

      double reach = radius * radius;

      for (Entity entity : level.getEntities(player, new AABB(centre, centre).inflate(radius), e -> e.distanceToSqr(centre) <= reach && catchable(player, e))) {
         if (!this.caught.containsKey(entity.getId())) {
            TickFreeze.slow(entity, this.divisor);
            this.caught.put(entity.getId(), entity);
         }
      }

      this.sweepAllies(player, level, centre, radius, release);
   }

   private void sweepAllies(ServerPlayer player, ServerLevel level, Vec3 centre, double radius, double release) {
      boolean engaged = TeamTimeLock.engaged(player, this.caught.values());
      Iterator<Entity> held = this.allies.values().iterator();

      while (held.hasNext()) {
         Entity ally = held.next();
         if (!engaged || ally.level() != level || ally.distanceToSqr(centre) > release || !lockable(player, ally)) {
            TickFreeze.unslow(ally, this.divisor);
            held.remove();
         }
      }

      if (engaged) {
         double reach = radius * radius;

         for (Entity ally : level.getEntities(player, new AABB(centre, centre).inflate(radius), e -> e.distanceToSqr(centre) <= reach && lockable(player, e))) {
            if (this.allies.putIfAbsent(ally.getId(), ally) == null) {
               TickFreeze.slow(ally, this.divisor);
               TeamTimeLock.tellHeld(player, ally, "awakened.team.time_held_slow");
            }
         }
      }
   }

   private static boolean lockable(ServerPlayer player, Entity entity) {
      if (allyBody(player, entity)) {
         return true;
      } else if (entity.isAlive() && entity.isVehicle() && slowable(entity) && !entity.hasIndirectPassenger(player)) {
         for (Entity rider : entity.getIndirectPassengers()) {
            if (allyBody(player, rider)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static boolean allyBody(ServerPlayer player, Entity entity) {
      return TeamTimeLock.allied(player, entity) && !runsOwnField(entity);
   }

   private static boolean runsOwnField(Entity entity) {
      if (entity instanceof Player target && VelocityVisualState.of(target).timeless()) {
         return true;
      }

      return false;
   }

   private void restrength(int next) {
      if (this.divisor > 1) {
         for (Entity entity : this.everyHold()) {
            TickFreeze.unslow(entity, this.divisor);
         }
      }

      this.divisor = next;
      if (next > 1) {
         for (Entity entity : this.everyHold()) {
            TickFreeze.slow(entity, next);
         }
      }
   }

   void releaseAll() {
      for (Entity entity : this.everyHold()) {
         TickFreeze.unslow(entity, this.divisor);
      }

      this.caught.clear();
      this.allies.clear();
   }

   private Iterable<Entity> everyHold() {
      return Iterables.concat(this.caught.values(), this.allies.values());
   }

   Collection<Entity> caught() {
      return this.caught.values();
   }

   private static boolean catchable(ServerPlayer player, Entity entity) {
      if (entity.isAlive() && slowable(entity) && !landed(entity) && !spared(player, entity)) {
         if (!entity.isVehicle()) {
            return true;
         } else {
            for (Entity rider : entity.getIndirectPassengers()) {
               if (spared(player, rider)) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean spared(ServerPlayer player, Entity entity) {
      if (entity instanceof Projectile) {
         return false;
      } else {
         if (entity instanceof Player target && (target.isSpectator() || target.isCreative() || runsOwnField(target))) {
            return true;
         }

         return PvpRules.blocksControl(player, entity) ? true : TeamTimeLock.sameSide(player, entity);
      }
   }

   private static boolean slowable(Entity entity) {
      return entity instanceof LivingEntity
         || entity instanceof Projectile
         || entity instanceof FallingBlockEntity
         || entity instanceof PrimedTnt
         || entity instanceof ItemEntity
         || entity instanceof ExperienceOrb
         || entity instanceof VehicleEntity;
   }

   private static boolean landed(Entity entity) {
      if (entity instanceof Projectile shot && shot.getDeltaMovement().lengthSqr() <= 1.0E-4) {
         return true;
      }

      return false;
   }
}
