package com.awakenedmod.awakened.content.summoning;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.phys.Vec3;

public final class PossessMotion {
   private static final double SPEED_MULT = 1.0;
   private static final double GRAVITY = 0.08;
   private static final double DRAG = 0.98;
   private static final double FLIGHT_FEEL = 1.0;
   private static final double SPRINT_FLY_MULT = 1.6;
   private static final double FLY_ASCEND = 0.3;
   private static final double FLUTTER_FALL = 0.6;
   private static final double STRIDER_LAVA_SPEED = 0.1;
   private static final double FLOP_HOP = 0.42;
   private static final double BEACHED_CRAWL = 0.35;
   private static final double HORSE_SPEED = 0.34;
   private static final double HORSE_JUMP_MIN = 0.42;
   private static final double HORSE_JUMP_MAX = 0.9;
   private static final double JUMP_CHARGE_RATE = 0.06;
   private static final Map<Integer, Double> VY_CLIENT = new ConcurrentHashMap<>();
   private static final Map<Integer, Double> VY_SERVER = new ConcurrentHashMap<>();
   private static final Map<Integer, Double> JUMP_CLIENT = new ConcurrentHashMap<>();
   private static final Map<Integer, Double> JUMP_SERVER = new ConcurrentHashMap<>();

   private PossessMotion() {
   }

   public static void clearVertical(Mob mob) {
      boolean client = mob.level().isClientSide();
      (client ? VY_CLIENT : VY_SERVER).remove(mob.getId());
      (client ? JUMP_CLIENT : JUMP_SERVER).remove(mob.getId());
   }

   public static void clearVerticalClient(int entityId) {
      VY_CLIENT.remove(entityId);
      JUMP_CLIENT.remove(entityId);
   }

   public static boolean isChargeMount(Mob mob) {
      EntityType<?> t = mob.getType();
      return t == EntityType.HORSE || t == EntityType.DONKEY || t == EntityType.MULE || t == EntityType.SKELETON_HORSE || t == EntityType.ZOMBIE_HORSE;
   }

   public static double jumpCharge(Mob mob) {
      return JUMP_CLIENT.getOrDefault(mob.getId(), 0.0);
   }

   public static boolean canSprint(Mob mob) {
      return !(mob instanceof Creeper);
   }

   public static boolean isFlyer(Mob mob) {
      return mob instanceof FlyingAnimal
         || mob instanceof Bat
         || mob instanceof Ghast
         || mob instanceof Phantom
         || mob instanceof Vex
         || mob instanceof Blaze
         || mob.getNavigation() instanceof FlyingPathNavigation
         || mob.getMoveControl() instanceof FlyingMoveControl;
   }

   public static boolean isSwimmer(Mob mob) {
      return mob.getNavigation() instanceof WaterBoundPathNavigation
         || mob.getNavigation() instanceof AmphibiousPathNavigation
         || mob.getType() == EntityType.SQUID
         || mob.getType() == EntityType.GLOW_SQUID;
   }

   private static boolean flutters(Mob mob) {
      return mob.getType() == EntityType.CHICKEN;
   }

   private static boolean flopsWhenBeached(Mob mob) {
      EntityType<?> t = mob.getType();
      return t == EntityType.COD
         || t == EntityType.SALMON
         || t == EntityType.PUFFERFISH
         || t == EntityType.TROPICAL_FISH
         || t == EntityType.TADPOLE
         || t == EntityType.SQUID
         || t == EntityType.GLOW_SQUID;
   }

   private static double flightSpeed(Mob mob) {
      EntityType<?> type = mob.getType();
      if (type == EntityType.PHANTOM) {
         return 0.8;
      } else if (type == EntityType.VEX) {
         return 0.45;
      } else if (type == EntityType.BAT) {
         return 0.35;
      } else if (type == EntityType.GHAST) {
         return 0.1;
      } else if (type == EntityType.BLAZE) {
         return 0.06;
      } else {
         AttributeInstance flying = mob.getAttribute(Attributes.FLYING_SPEED);
         if (flying != null) {
            return Mth.clamp(flying.getValue() * 0.2 * 1.0, 0.06, 1.0);
         } else {
            AttributeInstance move = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            double walk = move != null ? move.getValue() : 0.25;
            return Mth.clamp(walk * 0.35 * 1.0, 0.06, 0.6);
         }
      }
   }

   private static double swimSpeed(Mob mob) {
      EntityType<?> type = mob.getType();
      if (type == EntityType.DOLPHIN) {
         return 0.22;
      } else if (type == EntityType.GUARDIAN) {
         return 0.13;
      } else if (type == EntityType.ELDER_GUARDIAN) {
         return 0.1;
      } else if (type == EntityType.SQUID || type == EntityType.GLOW_SQUID) {
         return 0.12;
      } else if (type == EntityType.AXOLOTL) {
         return 0.13;
      } else if (type == EntityType.TURTLE) {
         return 0.07;
      } else if (type == EntityType.TADPOLE) {
         return 0.08;
      } else if (type == EntityType.COD || type == EntityType.TROPICAL_FISH) {
         return 0.07;
      } else if (type == EntityType.SALMON) {
         return 0.08;
      } else if (type == EntityType.PUFFERFISH) {
         return 0.06;
      } else {
         AttributeInstance move = mob.getAttribute(Attributes.MOVEMENT_SPEED);
         double walk = move != null ? move.getValue() : 0.7;
         return Mth.clamp(walk * 0.18, 0.05, 0.25);
      }
   }

   public static double jumpVelocity(Mob mob) {
      AttributeInstance jump = mob.getAttribute(Attributes.JUMP_STRENGTH);
      return jump != null ? jump.getValue() : 0.42;
   }

   public static void step(Mob mob, float yaw, float pitch, float forward, float strafe, boolean jump, boolean sprint) {
      mob.setYRot(yaw);
      mob.setYBodyRot(yaw);
      mob.setYHeadRot(yaw);
      mob.setXRot(mob instanceof Phantom ? -pitch : pitch);
      if (isFlyer(mob)) {
         freeMove(mob, yaw, pitch, forward, strafe, jump, flightSpeed(mob), sprint);
      } else if (mob.getType() == EntityType.STRIDER && mob.isInLava()) {
         freeMove(mob, yaw, pitch, forward, strafe, jump, 0.1, sprint);
      } else if (!mob.isUnderWater() && (!isSwimmer(mob) || !mob.isInWater())) {
         groundStep(mob, yaw, forward, strafe, jump, sprint);
      } else {
         freeMove(mob, yaw, pitch, forward, strafe, jump, swimSpeed(mob), sprint);
      }

      if (mob.level().isClientSide()) {
         mob.calculateEntityAnimation(false);
      }
   }

   private static void groundStep(Mob mob, float yaw, float forward, float strafe, boolean jump, boolean sprint) {
      float yawRad = yaw * (float) (Math.PI / 180.0);
      double sin = (double)Mth.sin(yawRad);
      double cos = (double)Mth.cos(yawRad);
      Vec3 wish = new Vec3((double)strafe * cos - (double)forward * sin, 0.0, (double)forward * cos + (double)strafe * sin);
      if (wish.lengthSqr() > 1.0E-4) {
         wish = wish.normalize();
      }

      mob.setSprinting(sprint && forward > 0.0F && canSprint(mob));
      double speed = isChargeMount(mob) ? 0.34 : (isSwimmer(mob) ? swimSpeed(mob) * 0.35 : mob.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.0);
      Vec3 horizontal = wish.scale(speed);
      Map<Integer, Double> vyMap = mob.level().isClientSide() ? VY_CLIENT : VY_SERVER;
      boolean grounded = mob.onGround() || mob.verticalCollisionBelow;
      double vy = (vyMap.getOrDefault(mob.getId(), mob.getDeltaMovement().y) - 0.08) * 0.98;
      if (isChargeMount(mob)) {
         vy = chargeJump(mob, grounded, jump, vy);
      } else if (grounded && flopsWhenBeached(mob)) {
         vy = 0.42;
      } else if (grounded && jump) {
         vy = jumpVelocity(mob);
      } else if (vy < 0.0 && flutters(mob)) {
         vy *= 0.6;
      }

      mob.setDeltaMovement(horizontal.x, vy, horizontal.z);
      mob.move(MoverType.SELF, mob.getDeltaMovement());
      if (mob.verticalCollision) {
         vy = 0.0;
      }

      vyMap.put(mob.getId(), vy);
   }

   private static double chargeJump(Mob mob, boolean grounded, boolean jump, double gravityVy) {
      Map<Integer, Double> jumpMap = mob.level().isClientSide() ? JUMP_CLIENT : JUMP_SERVER;
      double charge = jumpMap.getOrDefault(mob.getId(), 0.0);
      double vy = gravityVy;
      if (!grounded) {
         charge = 0.0;
      } else if (jump) {
         charge = Math.min(1.0, charge + 0.06);
      } else if (charge > 0.0) {
         vy = 0.42 + 0.48000000000000004 * charge;
         charge = 0.0;
      }

      jumpMap.put(mob.getId(), charge);
      return vy;
   }

   private static void freeMove(Mob mob, float yaw, float pitch, float forward, float strafe, boolean jump, double baseSpeed, boolean sprint) {
      float yawRad = yaw * (float) (Math.PI / 180.0);
      float pitchRad = pitch * (float) (Math.PI / 180.0);
      double sinY = (double)Mth.sin(yawRad);
      double cosY = (double)Mth.cos(yawRad);
      double cosP = (double)Mth.cos(pitchRad);
      double sinP = (double)Mth.sin(pitchRad);
      Vec3 wish = new Vec3(-sinY * cosP, -sinP, cosY * cosP).scale((double)forward).add(new Vec3(cosY, 0.0, sinY).scale((double)strafe));
      if (wish.lengthSqr() > 1.0E-4) {
         wish = wish.normalize();
      }

      double speed = baseSpeed * (sprint && canSprint(mob) ? 1.6 : 1.0);
      Vec3 vel = wish.scale(speed);
      if (jump) {
         vel = vel.add(0.0, 0.3, 0.0);
      }

      mob.setDeltaMovement(vel);
      mob.move(MoverType.SELF, vel);
      mob.resetFallDistance();
      clearVertical(mob);
   }
}
