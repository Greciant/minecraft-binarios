package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.core.data.VelocityVisual;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Downstep {
   private static final double[] REACH_BY_GEAR = new double[]{0.0, 1.0, 1.5, 2.0, 2.5, 3.0};
   private static final double MIN_DROP = 1.0E-4;
   private static final double REACH_EPSILON = 1.0E-6;
   private static final Set<UUID> GROUNDED_CLIENT = ConcurrentHashMap.newKeySet();
   private static final Set<UUID> GROUNDED_SERVER = ConcurrentHashMap.newKeySet();

   private Downstep() {
   }

   public static boolean stepDown(Player runner) {
      Set<UUID> grounded = memory(runner);
      UUID id = runner.getUUID();
      boolean wasGrounded = grounded.contains(id);
      boolean snapped = !runner.onGround() && wasGrounded && qualifies(runner) && snapDown(runner);
      if (runner.onGround()) {
         grounded.add(id);
      } else {
         grounded.remove(id);
      }

      return snapped;
   }

   public static void forget(UUID id) {
      GROUNDED_CLIENT.remove(id);
      GROUNDED_SERVER.remove(id);
   }

   public static void clearAll() {
      GROUNDED_CLIENT.clear();
      GROUNDED_SERVER.clear();
   }

   private static Set<UUID> memory(Player runner) {
      return runner.level().isClientSide() ? GROUNDED_CLIENT : GROUNDED_SERVER;
   }

   private static boolean qualifies(Player runner) {
      if (runner.isPassenger() || runner.getAbilities().flying || runner.isInWater() || runner.isInLava() || runner.onClimbable() || runner.isFallFlying()) {
         return false;
      } else if (runner.isShiftKeyDown()) {
         return false;
      } else {
         VelocityVisual visual = VelocityVisualState.of(runner);
         return visual.overdrive() && runner.isSprinting() && visual.gear() >= 1 ? !visual.phasing() && !visual.tunnelling() : false;
      }
   }

   private static boolean snapDown(Player runner) {
      Vec3 velocity = runner.getDeltaMovement();
      if (velocity.y > 0.0) {
         return false;
      } else {
         double reach = reachOf(runner);
         Level level = runner.level();
         AABB box = runner.getBoundingBox();
         double drop = -Entity.collideBoundingBox(runner, new Vec3(0.0, -reach, 0.0), box, level, List.of()).y;
         if (drop >= reach - 1.0E-6) {
            return false;
         } else if (drop <= 1.0E-4) {
            return false;
         } else {
            runner.setPos(runner.getX(), runner.getY() - drop, runner.getZ());
            runner.setDeltaMovement(velocity.x, 0.0, velocity.z);
            runner.setOnGround(true);
            runner.resetFallDistance();
            return true;
         }
      }
   }

   private static double reachOf(Player runner) {
      int gear = Math.max(0, Math.min(REACH_BY_GEAR.length - 1, VelocityVisualState.of(runner).gear()));
      return REACH_BY_GEAR[gear];
   }
}
