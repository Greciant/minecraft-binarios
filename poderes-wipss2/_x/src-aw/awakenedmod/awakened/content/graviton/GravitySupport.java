package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.core.Dmg;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GravitySupport {
   private GravitySupport() {
   }

   public static void pull(LivingEntity target, Vec3 point, double strength, double lift) {
      pullEntity(target, point, strength, lift);
   }

   public static void pullEntity(Entity target, Vec3 point, double strength, double lift) {
      Vec3 dir = point.subtract(target.position());
      if (!(dir.lengthSqr() < 1.0E-4)) {
         dir = dir.normalize();
         Vec3 v = target.getDeltaMovement().scale(0.2).add(dir.x * strength, dir.y * strength + lift, dir.z * strength);
         target.setDeltaMovement(v);
         target.hurtMarked = true;
      }
   }

   public static void shove(LivingEntity target, Vec3 center, double strength, double lift) {
      Vec3 away = new Vec3(target.getX() - center.x, 0.0, target.getZ() - center.z);
      if (away.lengthSqr() < 1.0E-4) {
         away = new Vec3(0.0, 0.0, 1.0);
      }

      away = away.normalize();
      target.setDeltaMovement(away.x * strength, lift, away.z * strength);
      target.hurtMarked = true;
   }

   public static boolean crush(ServerLevel level, ServerPlayer player, LivingEntity target, float damage) {
      boolean hit = Dmg.hurt(target, level, player.damageSources().playerAttack(player), damage);
      if (hit) {
         target.hurtMarked = true;
      }

      return hit;
   }

   public static List<Entity> looseAround(ServerLevel level, Vec3 center, double radius) {
      return level.getEntitiesOfClass(
         Entity.class, new AABB(center, center).inflate(radius), e -> !(e instanceof LivingEntity) && e.isAlive() && e.distanceToSqr(center) <= radius * radius
      );
   }
}
