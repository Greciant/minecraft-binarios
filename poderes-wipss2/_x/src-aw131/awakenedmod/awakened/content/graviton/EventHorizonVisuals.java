package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Lab;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class EventHorizonVisuals {
   public static final int OPEN_TICKS = 20;
   public static final int COLLAPSE_TICKS = 16;
   public static final int FLASH_TICKS = 18;
   public static final int WHINE_TICKS = 30;
   public static final float DEFAULT_PULL_RADIUS = 16.0F;
   public static final double DEFAULT_GROUND_DROP = 4.0;
   private static final double GROUND_SCAN = 12.0;
   public static final Lab.Value DISK_TILT = Lab.SERVER.def("graviton.hole.disk.tilt", 20.0, 0.0, 60.0, 0.5);
   public static final Lab.Value DISK_ROLL = Lab.SERVER.def("graviton.hole.disk.roll", 7.0, -45.0, 45.0, 0.5);

   private EventHorizonVisuals() {
   }

   public static EventHorizonEntity spawn(ServerLevel level, Vec3 center, int sustainTicks, float yawDeg) {
      EventHorizonEntity hole = new EventHorizonEntity(AwakenedEntities.EVENT_HORIZON.get(), level);
      hole.setPos(center.x, center.y, center.z);
      double drop = AimUtil.groundedPositionIfFound(level, center).map(g -> center.y - g.y).filter(d -> d >= 0.0 && d <= 12.0).orElse(4.0);
      hole.configure((int)level.getGameTime(), Math.max(0, sustainTicks), 16.0F, (float)drop, yawDeg, DISK_TILT.getF(), DISK_ROLL.getF());
      level.addFreshEntity(hole);
      return hole;
   }

   public static EventHorizonEntity spawn(ServerLevel level, Vec3 center, int sustainTicks) {
      return spawn(level, center, sustainTicks, 0.0F);
   }

   public static Vec3[] diskFrame(float yawDeg, float tiltDeg, float rollDeg) {
      double yaw = Math.toRadians((double)yawDeg);
      Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
      Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
      double tilt = Math.toRadians((double)tiltDeg);
      Vec3 n = new Vec3(0.0, Math.cos(tilt), 0.0).subtract(forward.scale(Math.sin(tilt)));
      double roll = Math.toRadians((double)rollDeg);
      n = rotate(n, forward, roll).normalize();
      Vec3 u = rotate(right, forward, roll).normalize();
      Vec3 v = n.cross(u).normalize();
      return new Vec3[]{u, v, n};
   }

   private static Vec3 rotate(Vec3 v, Vec3 axis, double angle) {
      double c = Math.cos(angle);
      double s = Math.sin(angle);
      return v.scale(c).add(axis.cross(v).scale(s)).add(axis.scale(axis.dot(v) * (1.0 - c)));
   }

   public static int totalTicks(int sustainTicks) {
      return 20 + sustainTicks + 16 + 18;
   }

   public static EventHorizonVisuals.Phase phase(float age, int sustainTicks, int collapseAt) {
      int scheduled = 20 + sustainTicks;
      int collapseStart = collapseAt >= 0 ? Math.min(collapseAt, scheduled) : scheduled;
      float open = Mth.clamp(age / 20.0F, 0.0F, 1.0F);
      float sustain = collapseStart > 20 ? Mth.clamp((age - 20.0F) / (float)(collapseStart - 20), 0.0F, 1.0F) : 1.0F;
      float collapse = Mth.clamp((age - (float)collapseStart) / 16.0F, 0.0F, 1.0F);
      float flash = Mth.clamp((age - (float)collapseStart - 16.0F) / 18.0F, 0.0F, 1.0F);
      float whine = Mth.clamp((age - (float)(collapseStart - 30)) / 30.0F, 0.0F, 1.0F);
      boolean done = age >= (float)(collapseStart + 16 + 18);
      return new EventHorizonVisuals.Phase(age, open, sustain, collapse, flash, whine, collapseStart, done);
   }

   public static record Phase(float age, float open, float sustain, float collapse, float flash, float whine, int collapseStart, boolean done) {
      public boolean holding() {
         return this.collapse <= 0.0F;
      }

      public boolean flashing() {
         return this.collapse >= 1.0F;
      }

      public float presence() {
         return this.open * (1.0F - this.collapse);
      }
   }
}
