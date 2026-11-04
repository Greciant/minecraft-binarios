package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Bosses;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BrawlerTargets {
   public static final TagKey<EntityType<?>> UNLIFTABLE = TagKey.create(Registries.ENTITY_TYPE, AwId.of("brawler_unliftable"));

   private BrawlerTargets() {
   }

   public static boolean countsForCombo(ServerPlayer player, Entity target) {
      if (target == null || target == player || !target.isAlive() || !Harm.allowed(player, target)) {
         return false;
      } else if (target instanceof Player) {
         return true;
      } else {
         if (target instanceof Mob mob) {
            if (mob.isNoAi()) {
               return false;
            }

            if (mob.getTarget() == player) {
               return true;
            }
         }

         return Foes.counts(player, target);
      }
   }

   public static boolean inCone(ServerPlayer player, Entity target, double reach, double halfDeg) {
      Vec3 eye = player.getEyePosition();
      Vec3 to = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0).subtract(eye);
      double dist = to.length();
      if (dist > reach + (double)target.getBbWidth() * 0.5) {
         return false;
      } else if (dist < 1.0E-4) {
         return true;
      } else if (to.scale(1.0 / dist).dot(player.getLookAngle()) >= Math.cos(Math.toRadians(halfDeg))) {
         return true;
      } else {
         AABB box = target.getBoundingBox().inflate(0.3);
         return box.contains(eye) || box.clip(eye, eye.add(player.getLookAngle().scale(reach))).isPresent();
      }
   }

   public static boolean lenientSight(Level level, Vec3 eye, LivingEntity target, double freeDist) {
      Vec3 chest = target.position().add(0.0, (double)target.getBbHeight() * 0.55, 0.0);
      if (eye.distanceToSqr(chest) <= freeDist * freeDist) {
         return true;
      } else if (clearOfFullBlocks(level, eye, chest)) {
         return true;
      } else {
         Vec3 flat = new Vec3(chest.x - eye.x, 0.0, chest.z - eye.z);
         if (flat.lengthSqr() < 1.0E-6) {
            return false;
         } else {
            Vec3 side = new Vec3(-flat.z, 0.0, flat.x).normalize().scale((double)target.getBbWidth() * 0.5);
            return clearOfFullBlocks(level, eye, chest.add(side)) || clearOfFullBlocks(level, eye, chest.subtract(side));
         }
      }
   }

   private static boolean clearOfFullBlocks(Level level, Vec3 from, Vec3 to) {
      double length = from.distanceTo(to);
      int steps = Math.max(1, (int)Math.ceil(length / 0.25));
      Vec3 step = to.subtract(from).scale(1.0 / (double)steps);
      BlockPos last = null;

      for (int i = 1; i < steps; i++) {
         BlockPos pos = BlockPos.containing(from.add(step.scale((double)i)));
         if (!pos.equals(last)) {
            last = pos;
            if (level.getBlockState(pos).isCollisionShapeFullBlock(level, pos)) {
               return false;
            }
         }
      }

      return true;
   }

   public static boolean unliftable(LivingEntity target) {
      if (Bosses.is(target)) {
         return true;
      } else {
         return target.getType().is(UNLIFTABLE) ? true : target.getBbHeight() > 2.5F || target.getBbWidth() > 1.5F;
      }
   }
}
