package com.devilfruits.fruits.supa_supa.slash;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public record SlashPlane(Axis along, int sign, Axis across, int boundary, Axis span, int spanSign) {
   public static final int EXIT_RAMP = 5;

   public static SlashPlane of(Vec3 localDir, Vec3 localUp, Vec3 localPos) {
      Axis across = largest(localDir.cross(localUp), null);
      Axis along = largest(localDir, across);
      Axis span = third(across, along);
      int sign = localDir.get(along) >= 0.0 ? 1 : -1;
      int spanSign = localUp.get(span) >= 0.0 ? 1 : -1;
      return new SlashPlane(along, sign, across, (int)Math.round(localPos.get(across)), span, spanSign);
   }

   private static Axis largest(Vec3 v, Axis not) {
      Axis best = null;
      double most = -1.0;

      for (Axis a : Axis.values()) {
         if (a != not && Math.abs(v.get(a)) > most) {
            most = Math.abs(v.get(a));
            best = a;
         }
      }

      return best;
   }

   private static Axis third(Axis a, Axis b) {
      for (Axis c : Axis.values()) {
         if (c != a && c != b) {
            return c;
         }
      }

      throw new IllegalArgumentException("two axes the same: " + a);
   }

   private static Vec3 unit(Axis axis, int sign) {
      return switch (axis) {
         case X -> new Vec3((double)sign, 0.0, 0.0);
         case Y -> new Vec3(0.0, (double)sign, 0.0);
         case Z -> new Vec3(0.0, 0.0, (double)sign);
         default -> throw new MatchException(null, null);
      };
   }

   public Vec3 travel() {
      return unit(this.along, this.sign);
   }

   public Vec3 normal() {
      return unit(this.across, 1);
   }

   public Vec3 spanDir() {
      return unit(this.span, this.spanSign);
   }

   public Vec3 onPlane(Vec3 p) {
      return switch (this.across) {
         case X -> new Vec3((double)this.boundary, p.y, p.z);
         case Y -> new Vec3(p.x, (double)this.boundary, p.z);
         case Z -> new Vec3(p.x, p.y, (double)this.boundary);
         default -> throw new MatchException(null, null);
      };
   }

   public int pack(int index) {
      return index & 0xFF | this.across.ordinal() << 8 | this.span.ordinal() << 10 | (this.spanSign > 0 ? 1 : 0) << 12;
   }

   public static SlashPlane unpack(Vec3 start, Vec3 velocity, int bits) {
      Axis across = Axis.values()[bits >> 8 & 3];
      Axis span = Axis.values()[bits >> 10 & 3];
      Axis along = third(across, span);
      int sign = velocity.get(along) >= 0.0 ? 1 : -1;
      return new SlashPlane(along, sign, across, (int)Math.round(start.get(across)), span, (bits >> 12 & 1) == 1 ? 1 : -1);
   }

   public static int index(int bits) {
      return bits & 0xFF;
   }

   public int side(BlockPos pos) {
      return pos.get(this.across) < this.boundary ? -1 : 1;
   }

   public boolean against(BlockPos pos) {
      int c = pos.get(this.across);
      return c == this.boundary - 1 || c == this.boundary;
   }

   public SlashPlane.Partition partition(Iterable<BlockPos> blocks) {
      List<BlockPos> minus = new ArrayList<>();
      List<BlockPos> plus = new ArrayList<>();

      for (BlockPos p : blocks) {
         (this.side(p) < 0 ? minus : plus).add(p.immutable());
      }

      return new SlashPlane.Partition(minus, plus);
   }

   public double glideLength(double start, Iterable<BlockPos> against) {
      double end = start;

      for (BlockPos p : against) {
         int a = p.get(this.along);
         end = this.sign > 0 ? Math.max(end, (double)(a + 1)) : Math.min(end, (double)a);
      }

      return Math.abs(end - start);
   }

   public static double glideSpeed(double length, double speed, int maxTicks) {
      return Math.max(speed, length / (double)Math.max(1, maxTicks));
   }

   public static double glideSpeed(double length, double speed, int maxTicks, double tempo) {
      return glideSpeed(length, speed * tempo, (int)Math.max(1L, Math.round((double)maxTicks / tempo)));
   }

   public static int pauseTicks(int delay, double tempo) {
      return (int)Math.max(0L, Math.round((double)delay / Math.max(0.01, tempo)));
   }

   public static int glideTicks(double length, double speed) {
      return Math.max(1, Mth.ceil(length / speed - 1.0E-6));
   }

   public static double exitSpeed(int k, double glide, double full) {
      double t = Math.min(1.0, (double)(k + 1) / 5.0);
      return glide + (full - glide) * t * t;
   }

   public static int exitTicks(double glide, double full, double distance) {
      double done = 0.0;
      int k = 0;

      while (done < distance - 1.0E-6 && k < 1000) {
         done += exitSpeed(k++, glide, full);
      }

      return k;
   }

   public static SlashPlane.Refusal check(boolean enabled, boolean allowed, int blocks, int maxBlocks, boolean solidMinus, boolean solidPlus) {
      if (!enabled) {
         return SlashPlane.Refusal.DISABLED;
      } else if (!allowed) {
         return SlashPlane.Refusal.PROTECTED;
      } else if (blocks > maxBlocks) {
         return SlashPlane.Refusal.TOO_BIG;
      } else {
         return solidMinus && solidPlus ? SlashPlane.Refusal.NONE : SlashPlane.Refusal.ONE_SIDED;
      }
   }

   public static record Partition(List<BlockPos> minus, List<BlockPos> plus) {
      public List<BlockPos> smaller() {
         return this.minus.size() <= this.plus.size() ? this.minus : this.plus;
      }

      public int smallerSide() {
         return this.minus.size() <= this.plus.size() ? -1 : 1;
      }
   }

   public static enum Refusal {
      NONE,
      DISABLED,
      PROTECTED,
      TOO_BIG,
      ONE_SIDED;
   }
}
