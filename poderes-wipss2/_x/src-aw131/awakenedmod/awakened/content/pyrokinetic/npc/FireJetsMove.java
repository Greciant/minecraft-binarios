package com.awakenedmod.awakened.content.pyrokinetic.npc;

import com.awakenedmod.awakened.content.pyrokinetic.FireJetsEngine;
import com.awakenedmod.awakened.content.pyrokinetic.PyroHeatState;
import com.awakenedmod.awakened.core.npc.NpcCaster;
import com.awakenedmod.awakened.core.npc.NpcContext;
import com.awakenedmod.awakened.core.npc.NpcMove;
import com.awakenedmod.awakened.core.npc.NpcNav;
import com.awakenedmod.awakened.core.npc.NpcTravel;
import com.awakenedmod.awakened.core.npc.PoweredNpc;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

final class FireJetsMove extends NpcMove implements NpcTravel {
   private static final int RETAP_TICKS = 10;
   private static final int TAP_CLIP_TICKS = 14;
   private static final int LAND_CLIP_TICKS = 9;
   private static final int TAP_THRUST_TICKS = 4;
   private static final int SOFT_FLIGHT_TICKS = 70;
   private static final int HARD_FLIGHT_TICKS = 120;
   private static final int MAX_FALL_TICKS = 40;
   private static final double CLEARANCE = 2.5;
   private static final double CLEARANCE_RETRY = 3.0;
   private static final double MIN_TRIP = 4.0;
   private static final double MAX_TRIP = 32.0;
   private static final double GLIDE_TICKS = 3.0;
   private static final double CUT_MIN = 1.5;
   private static final double CUT_MAX = 6.0;
   private static final double FLARE = 0.45;
   private static final double FLARE_MAX_RISE = 0.2;
   private static final double STALL_TRAVEL = 0.08;
   private static final int STALL_TICKS = 8;
   private static final double TRIP_SPEED = 1.0;
   private static final int TRIP_SPARE_TICKS = 10;
   private static final float RESERVE = 10.0F;
   private static final int ENGAGE_FLIGHT_REST = 80;
   private static final int SAFE_DROP = 24;
   private static final float HUGGED_SCORE = 2.2F;
   private static final float ESCAPE_SCORE = 2.0F;
   private static final float LEDGE_SCORE = 1.8F;
   private static final float GAP_SCORE = 1.5F;
   private static final float POP_UP_SCORE = 1.3F;
   private static final float CHASE_SCORE = 1.2F;
   private static final float IN_SIGHT = 0.6F;
   private static final float HOT_STRAIN = 0.9F;
   private static final float ESCAPE_HEALTH = 0.35F;
   private static final double ESCAPE_RANGE = 8.0;
   private static final double CLOSE = 3.0;
   private static final float CLOSE_HEALTH = 0.5F;
   private static final double LEDGE_RISE = 3.0;
   private static final double GAP_FLAT = 10.0;
   private static final double GAP_MAX = 20.0;
   private static final double CHASE_FLAT = 24.0;
   private static final int REACH_RECHECK = 40;
   private static final float REACH_SEARCH = 0.5F;
   private static final double DETOUR = 2.0;
   private static final int DETOUR_SLACK = 8;
   private static final double[] APPROACH = new double[]{9.0, 7.0, 11.0, 5.0};
   private static final double[] APPROACH_SIDE = new double[]{0.0, 1.5, -1.5};
   private static final double[] ESCAPE_TURNS = new double[]{0.0, 40.0, -40.0, 80.0, -80.0};
   private static final double ESCAPE_REACH = 5.0;
   private static final double HOP_MIN = 2.0;
   private static final double HOP_MAX = 9.0;
   private static final double HOP_LIFT = 0.45;
   private static final int TAP_REACH = 12;
   private static final double POP_UP_ROOM = 5.0;
   private static final float FLY_TURN = 20.0F;
   private static final double EYE = 1.5;
   @Nullable
   private FireJetsMove.Trip planned;
   private int plannedTick = Integer.MIN_VALUE;
   @Nullable
   private FireJetsMove.Trip trip;
   @Nullable
   private FireJetsEngine.Jet jet;
   private int age;
   private int cutAge = -1;
   private boolean engaged;
   private int stalled;
   private boolean turnedBack;
   private int nextEngageFlight;
   private int reachCheckedAt = -40;
   private boolean reachable = true;

   FireJetsMove() {
      super("fire_jets", 5, 0.0, 32.0, 10);
   }

   @Override
   public float staminaCost() {
      return 10.0F;
   }

   @Override
   public String balanceKey() {
      return FireJetsEngine.BALANCE_KEY;
   }

   @Override
   public boolean needsLineOfSight() {
      return false;
   }

   @Override
   public boolean inRange(NpcContext ctx) {
      return true;
   }

   @Override
   public boolean canStart(NpcContext ctx) {
      return this.planned != null && this.plannedTick == ctx.npc().tickCount;
   }

   @Override
   public boolean committed() {
      return true;
   }

   @Override
   public double footworkWhileRunning() {
      return 0.0;
   }

   @Override
   public float score(NpcContext ctx) {
      this.planned = null;
      PoweredNpc npc = ctx.npc();
      LivingEntity target = ctx.target();
      if (target != null && canFly(npc)) {
         float health = ctx.healthFraction();
         if (ctx.hugging() || ctx.distance() < 3.0 && health < 0.5F) {
            Vec3 dir = escapeDir(npc, target);
            if (dir != null) {
               return this.plan(npc, tap(npc, dir, NpcTravel.Purpose.FLEE), 2.2F);
            }
         }

         if (health < 0.35F && ctx.distance() < 8.0) {
            Vec3 away = NpcNav.fleePoint(npc, target);
            FireJetsMove.Trip out = away == null ? null : this.flight(npc, away, NpcTravel.Purpose.FLEE);
            if (out != null) {
               return this.plan(npc, out, 2.0F);
            }
         }

         if (npc.tickCount >= this.nextEngageFlight && !(ctx.strain() >= 0.9F)) {
            double rise = target.getY() - npc.getY();
            double flat = NpcNav.flat(npc.position(), target.position());
            if (flat > 32.0 + APPROACH[0]) {
               return 0.0F;
            } else {
               boolean ledge = rise > 3.0;
               if (ledge || flat > 10.0 && flat <= 20.0) {
                  Vec3 spot = approachSpot(npc, target, ledge);
                  FireJetsMove.Trip over = spot == null ? null : this.flight(npc, spot, NpcTravel.Purpose.ENGAGE);
                  if (over != null && !this.reachable(npc, target)) {
                     return this.plan(npc, over, (ledge ? 1.8F : 1.5F) * (ctx.lineOfSight() ? 0.6F : 1.0F));
                  }

                  Vec3 up = over == null && ledge && !ctx.lineOfSight() ? popUpDir(npc, target) : null;
                  if (up != null && !this.reachable(npc, target)) {
                     return this.plan(npc, tap(npc, up, NpcTravel.Purpose.ENGAGE), 1.3F);
                  }
               }

               if (flat > 24.0 && ctx.fleeing()) {
                  Vec3 spotx = approachSpot(npc, target, false);
                  FireJetsMove.Trip chase = spotx == null ? null : this.flight(npc, spotx, NpcTravel.Purpose.ENGAGE);
                  if (chase != null) {
                     return this.plan(npc, chase, 1.2F);
                  }
               }

               return 0.0F;
            }
         } else {
            return 0.0F;
         }
      } else {
         return 0.0F;
      }
   }

   @Override
   public boolean plan(NpcContext ctx, Vec3 destination, NpcTravel.Purpose purpose) {
      PoweredNpc npc = ctx.npc();
      if (!canFly(npc)) {
         return false;
      } else {
         FireJetsMove.Trip t = purpose != NpcTravel.Purpose.REPOSITION && purpose != NpcTravel.Purpose.DODGE ? null : hop(npc, destination, purpose);
         if (t == null && purpose != NpcTravel.Purpose.DODGE) {
            t = this.flight(npc, destination, purpose);
         }

         if (t == null) {
            return false;
         } else {
            this.plan(npc, t, 0.0F);
            return true;
         }
      }
   }

   @Nullable
   private static FireJetsMove.Trip hop(PoweredNpc npc, Vec3 dest, NpcTravel.Purpose purpose) {
      Vec3 flat = new Vec3(dest.x - npc.getX(), 0.0, dest.z - npc.getZ());
      double len = flat.length();
      if (!(len < 2.0) && !(len > 9.0) && !(PyroHeatState.heat(npc.caster()) + 0.02F > PyroHeatState.cap(npc.mastery()))) {
         Vec3 d = flat.scale(1.0 / len);
         Level level = npc.level();
         Vec3 low = npc.position().add(0.0, 0.5, 0.0);
         Vec3 high = npc.position().add(0.0, 1.7, 0.0);
         Vec3 reach = d.scale(len).add(0.0, 1.0, 0.0);
         return NpcNav.clearLine(level, npc, low, low.add(reach))
               && NpcNav.clearLine(level, npc, high, high.add(reach))
               && NpcNav.safeBelow(level, low.add(reach), 6)
               && !NpcNav.crossesFluid(level, low, low.add(reach))
               && safeRunOut(npc, d)
            ? tap(npc, new Vec3(d.x, 0.45, d.z).normalize(), purpose)
            : null;
      } else {
         return null;
      }
   }

   @Override
   public void start(NpcContext ctx) {
      PoweredNpc npc = ctx.npc();
      FireJetsMove.Trip t = this.planned != null && this.plannedTick == npc.tickCount ? this.planned : this.fallbackTrip(ctx);
      this.planned = null;
      this.trip = t;
      this.age = 0;
      this.cutAge = -1;
      this.engaged = false;
      this.stalled = 0;
      this.turnedBack = false;
      this.jet = FireJetsEngine.burst(ctx.caster(), t.kind() == FireJetsMove.Kind.TAP ? t.dir() : steer(npc, t));
      npc.showClip(PyroNpcKit.FIRE_JETS_TAP);
      npc.getNavigation().stop();
      if (t.kind() == FireJetsMove.Kind.FLIGHT && t.purpose() == NpcTravel.Purpose.ENGAGE) {
         this.nextEngageFlight = npc.tickCount + 80;
      }
   }

   @Override
   public boolean tick(NpcContext ctx) {
      FireJetsMove.Trip t = this.trip;
      if (t == null) {
         return false;
      } else {
         PoweredNpc npc = ctx.npc();
         this.age++;
         if (this.jet != null) {
            this.burn(ctx, this.jet);
         }

         if (this.jet != null) {
            return true;
         } else {
            npc.resetFallDistance();
            boolean down = npc.onGround() || npc.isInWater() || this.age - this.cutAge >= 40;
            int clipEnd = t.kind() == FireJetsMove.Kind.TAP ? 14 : this.cutAge + (this.engaged ? 9 : 0);
            return !down || this.age < clipEnd;
         }
      }
   }

   @Override
   public void stop(NpcContext ctx, boolean interrupted) {
      this.planned = null;
      this.trip = null;
      this.jet = null;
      super.stop(ctx, interrupted);
   }

   private void burn(NpcContext ctx, FireJetsEngine.Jet burning) {
      PoweredNpc npc = ctx.npc();
      NpcCaster caster = ctx.caster();
      FireJetsMove.Trip t = this.trip;
      int tick = FireJetsEngine.advance(burning);
      if (t.kind() == FireJetsMove.Kind.TAP) {
         if (tick >= 4) {
            this.letGo(ctx, false);
         } else {
            FireJetsEngine.thrust(caster, burning, tick, new FireJetsMove.Steer(t.dir(), 0.0, 0.0));
         }
      } else {
         if (tick == 6) {
            this.engaged = true;
            npc.showClip(PyroNpcKit.FIRE_JETS);
         }

         if (tick >= 6 && !npc.stamina().drain(0.6F, this.balanceKey(), npc.level().getGameTime())) {
            this.letGo(ctx, true);
         } else if (!npc.isUnderWater() && !waterOverhead(npc)) {
            Level level = npc.level();
            Vec3 pos = npc.position();
            Vec3 v = npc.getDeltaMovement();
            double cut = Math.max(1.5, Math.min(6.0, Math.sqrt(v.x * v.x + v.z * v.z) * 3.0));
            boolean safe = NpcNav.safeBelow(level, pos, 24);
            boolean arrived = NpcNav.flat(pos, t.dest()) <= cut && pos.y >= t.dest().y - 0.5;
            if ((!safe || !arrived && tick < 70) && tick < 120) {
               if (tick >= 70 && !this.turnedBack) {
                  this.turnedBack = true;
                  this.trip = new FireJetsMove.Trip(FireJetsMove.Kind.FLIGHT, t.dir(), t.from(), Math.max(pos.y, t.from().y + 2.5), t.purpose(), t.from());
               }

               Vec3 dir = steer(npc, this.trip);
               FireJetsEngine.thrust(caster, burning, tick, new FireJetsMove.Steer(dir, 0.0, 0.0));
               npc.aimAt(npc.getEyePosition().add(dir.scale(8.0)), 20.0F);
               if (tick <= 10 || !(burning.moved() < 0.08)) {
                  this.stalled = 0;
               } else if (++this.stalled >= 8) {
                  this.letGo(ctx, this.engaged);
               }
            } else {
               this.letGo(ctx, this.engaged);
            }
         } else {
            this.letGo(ctx, this.engaged);
         }
      }
   }

   private void letGo(NpcContext ctx, boolean hold) {
      PoweredNpc npc = ctx.npc();
      this.jet = null;
      this.cutAge = this.age;
      if (hold) {
         Vec3 v = npc.getDeltaMovement();
         npc.setDeltaMovement(v.x * 0.45, Math.min(v.y, 0.2), v.z * 0.45);
         npc.hurtMarked = true;
         npc.showClip(PyroNpcKit.FIRE_JETS_LAND);
         FireJetsEngine.land(ctx.caster());
      }
   }

   private static Vec3 steer(PoweredNpc npc, FireJetsMove.Trip t) {
      Vec3 d = new Vec3(t.dest().x, t.cruiseY(), t.dest().z).subtract(npc.position());
      return d.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 1.0, 0.0) : d.normalize();
   }

   private float plan(PoweredNpc npc, FireJetsMove.Trip t, float score) {
      this.planned = t;
      this.plannedTick = npc.tickCount;
      return score;
   }

   private static FireJetsMove.Trip tap(PoweredNpc npc, Vec3 dir, NpcTravel.Purpose purpose) {
      return new FireJetsMove.Trip(FireJetsMove.Kind.TAP, dir, npc.position(), 0.0, purpose, npc.position());
   }

   @Nullable
   private FireJetsMove.Trip flight(PoweredNpc npc, Vec3 dest, NpcTravel.Purpose purpose) {
      Vec3 from = npc.position();
      double flat = NpcNav.flat(from, dest);
      if (!(flat < 4.0) && !(flat > 32.0)) {
         int ticks = 6 + (int)Math.ceil(flat / 1.0) + 10;
         if (!npc.stamina().canAfford(10.0F + 0.6F * (float)ticks + 10.0F, this.balanceKey())) {
            return null;
         } else {
            float heatAfter = PyroHeatState.heat(npc.caster()) + 0.02F + 0.004F * (float)ticks;
            if (heatAfter > PyroHeatState.cap(npc.mastery())) {
               return null;
            } else {
               double base = Math.max(from.y, dest.y) + 2.5;

               for (double cruise : new double[]{base, base + 3.0}) {
                  if (routeClear(npc, from, dest, cruise)) {
                     Vec3 dir = new Vec3(dest.x - from.x, cruise - from.y, dest.z - from.z).normalize();
                     return new FireJetsMove.Trip(FireJetsMove.Kind.FLIGHT, dir, dest, cruise, purpose, from);
                  }
               }

               return null;
            }
         }
      } else {
         return null;
      }
   }

   private static boolean routeClear(PoweredNpc npc, Vec3 from, Vec3 dest, double cruise) {
      Level level = npc.level();
      if (level.isOutsideBuildHeight(BlockPos.containing(dest.x, cruise + 2.0, dest.z))) {
         return false;
      } else {
         Vec3 over = new Vec3(dest.x, cruise, dest.z);
         return leg(npc, from, over) && leg(npc, over, dest);
      }
   }

   private static boolean leg(PoweredNpc npc, Vec3 a, Vec3 b) {
      Level level = npc.level();
      Vec3 feet = new Vec3(0.0, 0.3, 0.0);
      Vec3 head = new Vec3(0.0, 1.6, 0.0);
      return NpcNav.clearLine(level, npc, a.add(feet), b.add(feet))
         && NpcNav.clearLine(level, npc, a.add(head), b.add(head))
         && !NpcNav.crossesFluid(level, a.add(0.0, 0.9, 0.0), b.add(0.0, 0.9, 0.0));
   }

   @Nullable
   private static Vec3 approachSpot(PoweredNpc npc, LivingEntity target, boolean ledge) {
      Level level = npc.level();
      Vec3 toward = new Vec3(npc.getX() - target.getX(), 0.0, npc.getZ() - target.getZ());
      toward = toward.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : toward.normalize();
      Vec3 side = new Vec3(-toward.z, 0.0, toward.x);
      Vec3 eye = target.getEyePosition();

      for (double d : APPROACH) {
         for (double s : APPROACH_SIDE) {
            Vec3 p = target.position().add(toward.scale(d)).add(side.scale(s));
            Vec3 stand = NpcNav.standInColumn(level, p, 2, ledge ? 2 : 6);
            if (stand != null && (!ledge || !(stand.y < target.getY() - 1.5)) && NpcNav.clearLine(level, npc, stand.add(0.0, 1.5, 0.0), eye)) {
               return stand;
            }
         }
      }

      return null;
   }

   @Nullable
   private static Vec3 escapeDir(PoweredNpc npc, LivingEntity target) {
      Level level = npc.level();
      Vec3 away = new Vec3(npc.getX() - target.getX(), 0.0, npc.getZ() - target.getZ());
      away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, npc.getYRot() + 180.0F) : away.normalize();
      Vec3 low = npc.position().add(0.0, 0.5, 0.0);
      Vec3 high = npc.position().add(0.0, 1.7, 0.0);

      for (double turn : ESCAPE_TURNS) {
         Vec3 d = away.yRot((float)Math.toRadians(turn));
         Vec3 reach = d.scale(5.0).add(0.0, 1.0, 0.0);
         if (NpcNav.clearLine(level, npc, low, low.add(reach))
            && NpcNav.clearLine(level, npc, high, high.add(reach))
            && NpcNav.safeBelow(level, low.add(reach), 6)
            && !NpcNav.crossesFluid(level, low, low.add(reach))
            && safeRunOut(npc, d)) {
            return new Vec3(d.x * 0.85, 0.5, d.z * 0.85).normalize();
         }
      }

      return null;
   }

   private static boolean safeRunOut(PoweredNpc npc, Vec3 d) {
      Level level = npc.level();
      Vec3 low = npc.position().add(0.0, 0.5, 0.0);

      for (int i = 1; i <= 12; i++) {
         Vec3 p = low.add(d.scale((double)i));
         if (!NpcNav.clearLine(level, npc, low, p)) {
            return true;
         }

         if (!NpcNav.safeBelow(level, p, 6)) {
            return false;
         }
      }

      return true;
   }

   @Nullable
   private static Vec3 popUpDir(PoweredNpc npc, LivingEntity target) {
      Vec3 toward = new Vec3(target.getX() - npc.getX(), 0.0, target.getZ() - npc.getZ());
      toward = toward.lengthSqr() < 1.0E-4 ? Vec3.ZERO : toward.normalize();
      Vec3 dir = toward.scale(0.35).add(0.0, 0.94, 0.0).normalize();
      Vec3 eye = npc.getEyePosition();
      return NpcNav.clearLine(npc.level(), npc, eye, eye.add(dir.scale(5.0))) ? dir : null;
   }

   private FireJetsMove.Trip fallbackTrip(NpcContext ctx) {
      PoweredNpc npc = ctx.npc();
      LivingEntity target = ctx.target();
      if (target != null) {
         Vec3 spot = approachSpot(npc, target, target.getY() - npc.getY() > 3.0);
         FireJetsMove.Trip t = spot == null ? null : this.flight(npc, spot, NpcTravel.Purpose.ENGAGE);
         if (t != null) {
            return t;
         }
      }

      return tap(npc, new Vec3(0.0, 1.0, 0.0), NpcTravel.Purpose.ENGAGE);
   }

   private boolean reachable(PoweredNpc npc, LivingEntity target) {
      if (npc.tickCount - this.reachCheckedAt >= 40) {
         this.reachCheckedAt = npc.tickCount;
         npc.getNavigation().setMaxVisitedNodesMultiplier(0.5F);
         Path path = npc.getNavigation().createPath(target, 1);
         npc.getNavigation().resetMaxVisitedNodesMultiplier();
         double straight = (double)npc.distanceTo(target);
         this.reachable = path != null && path.canReach() && (double)path.getNodeCount() <= straight * 2.0 + 8.0;
      }

      return this.reachable;
   }

   private static boolean canFly(PoweredNpc npc) {
      return !npc.isUnderWater() && !waterOverhead(npc);
   }

   private static boolean waterOverhead(PoweredNpc npc) {
      Level level = npc.level();
      Vec3 eye = npc.getEyePosition();

      for (int dy = 1; dy <= 3; dy++) {
         if (!level.getFluidState(BlockPos.containing(eye.x, eye.y + (double)dy - 0.5, eye.z)).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private static enum Kind {
      TAP,
      FLIGHT;
   }

   private static record Steer(Vec3 direction, double strafe, double lift) implements FireJetsEngine.Steering {
   }

   private static record Trip(FireJetsMove.Kind kind, Vec3 dir, Vec3 dest, double cruiseY, NpcTravel.Purpose purpose, Vec3 from) {
   }
}
