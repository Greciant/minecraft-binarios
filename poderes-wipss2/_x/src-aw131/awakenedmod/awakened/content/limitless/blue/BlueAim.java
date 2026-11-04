package com.awakenedmod.awakened.content.limitless.blue;

import com.awakenedmod.awakened.core.AimUtil;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

final class BlueAim {
   private BlueAim() {
   }

   static BlueAim.Tap tap(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();
      BlockHitResult hit = AimUtil.clipBlocks(level, player, eye, eye.add(look.scale(20.0)));
      boolean block = hit.getType() != Type.MISS;
      double reach = block ? hit.getLocation().distanceTo(eye) : 20.0;
      LivingEntity body = AimUtil.aimedLivingSoft(player, reach, 4.0, e -> BlueField.catches(player, e));
      if (body != null) {
         return new BlueAim.Tap(body, over(level, body));
      } else if (!block) {
         return new BlueAim.Tap(null, lift(level, eye.add(look.scale(8.0))));
      } else {
         Direction face = hit.getDirection();
         return new BlueAim.Tap(
            null, lift(level, hit.getLocation().add((double)face.getStepX() * 0.8, (double)face.getStepY() * 0.8, (double)face.getStepZ() * 0.8))
         );
      }
   }

   static Vec3 over(ServerLevel level, LivingEntity body) {
      Vec3 feet = body.position();
      double up = Math.max(3.0, (double)body.getBbHeight() + 0.5 + 0.4);
      Vec3 want = feet.add(0.0, up, 0.0);
      Vec3 mid = feet.add(0.0, (double)body.getBbHeight() * 0.5, 0.0);
      BlockHitResult ceiling = AimUtil.clipBlocks(level, null, mid, want.add(0.0, 0.6, 0.0));
      return ceiling.getType() == Type.MISS ? want : new Vec3(want.x, Math.max(mid.y, ceiling.getLocation().y - 0.6), want.z);
   }

   private static Vec3 lift(ServerLevel level, Vec3 p) {
      BlockHitResult floor = AimUtil.clipBlocks(level, null, p, p.subtract(0.0, 3.0, 0.0));
      if (floor.getType() == Type.MISS) {
         return p;
      } else {
         Vec3 want = new Vec3(p.x, floor.getLocation().y + 3.0, p.z);
         BlockHitResult ceiling = AimUtil.clipBlocks(level, null, p, want.add(0.0, 0.6, 0.0));
         return ceiling.getType() == Type.MISS ? want : new Vec3(p.x, Math.max(p.y, ceiling.getLocation().y - 0.6), p.z);
      }
   }

   static Vec3 hold(ServerPlayer player) {
      Vec3 eye = player.getEyePosition();
      Vec3 end = eye.add(player.getLookAngle().scale(20.0));
      BlockHitResult hit = AimUtil.clipBlocks(player.serverLevel(), player, eye, end);
      if (hit.getType() == Type.MISS) {
         return end;
      } else {
         Direction face = hit.getDirection();
         return hit.getLocation().add((double)face.getStepX() * 0.5, (double)face.getStepY() * 0.5, (double)face.getStepZ() * 0.5);
      }
   }

   static record Tap(@Nullable LivingEntity body, Vec3 point) {
      Vec3 resolve(ServerPlayer player) {
         return this.body != null
               && this.body.isAlive()
               && !this.body.isRemoved()
               && this.body.level() == player.serverLevel()
               && (double)this.body.distanceTo(player) <= 22.0
            ? BlueAim.over(player.serverLevel(), this.body)
            : this.point;
      }
   }
}
