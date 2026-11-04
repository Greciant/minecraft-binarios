package com.devilfruits.fruits.uo_uo_seiryu;

import com.devilfruits.core.ability.AimHelper;
import com.devilfruits.core.physics.PhysicsWorld;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class DragonHead {
   public static final float SCALE = 0.09375F;
   public static final float MAX_PITCH = 70.0F;
   private static final float MOUTH_AHEAD = 30.0F;
   private static final float MOUTH_DOWN = 7.0F;

   public static double pivotHeight() {
      return 1.1049999594688416;
   }

   public static Vec3 mouth(Entity e, float partial) {
      float yaw = e.getViewYRot(partial);
      float pitch = Mth.clamp(e.getViewXRot(partial), -70.0F, 70.0F);
      Vec3 forward = Vec3.directionFromRotation(pitch, yaw);
      Vec3 up = Vec3.directionFromRotation(pitch - 90.0F, yaw);
      return e.getPosition(partial).add(0.0, pivotHeight(), 0.0).add(forward.scale(2.8125)).add(up.scale(-0.65625));
   }

   public static Vec3 aim(Level level, Entity e, Vec3 from, double reach, float partial) {
      if (e instanceof Player player && AimHelper.point(player) != null) {
         return AimHelper.lookFrom(player, from);
      }

      Vec3 eye = e.getEyePosition(partial);
      Vec3 look = e.getViewVector(partial);
      Vec3 far = eye.add(look.scale(reach));
      BlockHitResult hit = PhysicsWorld.clip(level, new ClipContext(eye, far, Block.COLLIDER, Fluid.ANY, e));
      Vec3 target = hit.getType() == Type.MISS ? far : hit.getLocation();
      Vec3 d = target.subtract(from);
      return !(d.lengthSqr() < 1.0) && !(d.dot(look) <= 0.0) ? d.normalize() : look;
   }

   private DragonHead() {
   }
}
