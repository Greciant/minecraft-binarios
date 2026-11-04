package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ExoTags {
   public static final int TAG_LIFETIME_TICKS = 180;
   static final double MAX_TAG_DISTANCE = 56.0;
   public static final double IN_VIEW_COS = Math.cos(Math.toRadians(25.0));
   private static final double AIM_TIE_EPS = 1.0E-4;
   private static final float TAGGED_BONUS_FRACTION = 0.35F;
   private static final ExpiringTargetSet TAGS = new ExpiringTargetSet(56.0);

   private ExoTags() {
   }

   public static boolean taggable(Entity target, ServerPlayer owner) {
      if (target instanceof LivingEntity living && living.isAlive() && living.attackable() && living != owner && !living.isAlliedTo(owner)) {
         return true;
      }

      return false;
   }

   public static boolean isHostileThreat(Entity candidate, ServerPlayer self) {
      return Foes.threat(candidate, self);
   }

   public static void tag(ServerPlayer player, LivingEntity target) {
      if (player != null && target != null && target != player && taggable(target, player)) {
         TAGS.add(player, target, 180);
      }
   }

   public static boolean isTagged(ServerPlayer player, Entity target) {
      return TAGS.contains(player, target);
   }

   public static List<LivingEntity> tagged(ServerPlayer player) {
      return TAGS.live(player);
   }

   public static LivingEntity primaryTarget(ServerPlayer player) {
      return selectPrimary(player.getEyePosition(), player.getLookAngle(), tagged(player));
   }

   public static LivingEntity selectPrimary(Vec3 eye, Vec3 look, Iterable<? extends LivingEntity> candidates) {
      LivingEntity best = null;
      double bestDot = -Double.MAX_VALUE;
      double bestDist = Double.MAX_VALUE;

      for (LivingEntity foe : candidates) {
         Vec3 to = foe.getBoundingBox().getCenter().subtract(eye);
         double dist = to.length();
         if (dist < 0.001) {
            return foe;
         }

         double dot = to.scale(1.0 / dist).dot(look);
         if (!(dot < IN_VIEW_COS) && (dot > bestDot + 1.0E-4 || dot > bestDot - 1.0E-4 && dist < bestDist)) {
            bestDot = dot;
            bestDist = dist;
            best = foe;
         }
      }

      return best;
   }

   public static void applyTaggedBonus(ServerPlayer owner, LivingEntity victim, ServerLevel level, float baseDamage) {
      if (isTagged(owner, victim)) {
         float bonus = baseDamage * 0.35F;
         if (!(bonus <= 0.0F)) {
            Dmg.hurtRapid(victim, level, Dmg.by(owner, DamageTypes.MAGIC), bonus);
         }
      }
   }

   public static void tick(ServerPlayer player) {
      TAGS.prune(player);
   }

   public static void clear(ServerPlayer player) {
      TAGS.clear(player);
   }
}
