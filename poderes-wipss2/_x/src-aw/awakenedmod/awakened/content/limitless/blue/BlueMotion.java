package com.awakenedmod.awakened.content.limitless.blue;

import com.awakenedmod.awakened.core.AimUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class BlueMotion {
   private BlueMotion() {
   }

   public static Vec3 target(Player body) {
      return AimUtil.standPoint(body, 20.0, 1.0).point();
   }

   public static boolean arrived(Vec3 feet, Vec3 target, double slack) {
      return feet.distanceTo(target) < 0.6 + slack;
   }

   public static Vec3 step(Vec3 feet, Vec3 v, Vec3 target, int ticks) {
      Vec3 d = target.subtract(feet);
      double dist = d.length();
      if (dist < 1.0E-4) {
         return Vec3.ZERO;
      } else {
         double ramp = Math.min(1.0, (double)(ticks + 1) / 8.0);
         double carried = Math.min(v.length(), 1.6);
         double allowed = Math.sqrt(0.5 * Math.max(0.0, dist - 0.3)) + 0.05;
         double cap = Math.min(Math.max(1.6 * ramp, carried), allowed);
         Vec3 want = d.scale(cap / dist);
         Vec3 next = v.scale(0.25).add(want.scale(0.75));
         double speed = next.length();
         return speed > cap ? next.scale(cap / speed) : next;
      }
   }
}
