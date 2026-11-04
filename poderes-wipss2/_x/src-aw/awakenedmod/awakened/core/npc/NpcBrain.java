package com.awakenedmod.awakened.core.npc;

import com.awakenedmod.awakened.core.AbilityGate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class NpcBrain {
   public static final int THINK_INTERVAL = 4;
   private static final float SCORE_FLOOR = 0.15F;
   private static final double TRAVEL_DISCOUNT_RANGE = 14.0;
   private static final double TRAVEL_DISCOUNT_FLOOR = 0.1;
   private static final float REPEAT_ONCE = 0.7F;
   private static final float REPEAT_TWICE = 0.35F;
   private static final float SAME_MOVE = 0.8F;
   private static final float SAME_MOVE_TWICE = 0.6F;
   private static final int VELOCITY_WINDOW = 5;
   private static final int FLEE_WINDOW = 20;
   private static final double FLEE_GAIN = 2.5;
   private static final double HUG_RADIUS = 4.0;
   private static final int HUG_TRIGGER_TICKS = 40;
   static final int FORGET_TICKS = 600;
   private static final int HUNT_DELAY = 20;
   private static final int HUNT_REPLAN = 60;
   private static final double HUNT_ARRIVE = 2.5;
   private static final double HUNT_SEARCH_MIN = 4.0;
   private static final int HUNT_SEARCH_RADIUS = 10;
   private static final int HUNT_STALL = 10;
   private static final double HUNT_PACE = 0.9;
   private static final int CORNERED_TICKS = 40;
   private static final double CORNERED_PROGRESS = 1.0;
   private static final int RETARGET_INTERVAL = 20;
   private static final int SWITCH_WINDOW = 60;
   private static final float URGENT_SCORE = 2.0F;
   private static final float VENT_RECOVER = 0.72F;
   private static final double COOL_OUT = 2.0;
   private static final double PRESS_IN = 3.0;
   private static final double PRESS_BAND = 2.0;
   private static final double PRESS_FLOOR = 4.0;
   private static final double KITE_BAND = 3.0;
   private static final double KITE_OUT = 3.0;
   private static final float DISENGAGE_HEALTH = 0.3F;
   private static final float DISENGAGE_AGAIN_DROP = 0.1F;
   private static final float DRY_STAMINA = 0.12F;
   private static final float DRY_RECOVER = 0.3F;
   private static final float FINISH_HEALTH = 0.3F;
   private static final float RECOVER_STAMINA = 0.5F;
   private static final float RECOVER_STRAIN = 0.6F;
   private static final float RECOVER_HEALTH = 0.45F;
   private static final int DISENGAGE_MAX_TICKS = 120;
   private static final double AIR_FLEE_RANGE = 14.0;
   private static final int FLEE_REPLAN_TICKS = 60;
   private static final double REST_DISTANCE = 10.0;
   private static final int REST_UNSEEN_TICKS = 40;
   private static final int REST_HEAL_INTERVAL = 40;
   private static final float REST_HEAL = 1.0F;
   private static final int WATER_REPATH = 20;
   private static final int WATER_REACH = 12;
   private static final double WATER_PACE = 1.25;
   private static final int HAZARD_STEP_TICKS = 15;
   private static final double HAZARD_PACE = 1.2;
   private static final double FIGHT_PACE = 1.0;
   private static final double RETREAT_PACE = 1.15;
   private static final double SCARED_PACE = 1.3;
   @Nullable
   private static volatile NpcObserver observer;
   private final PoweredNpc npc;
   private final List<NpcMove> moves;
   private final List<NpcTactic> tactics;
   private final NpcFootwork footwork;
   private final ProjectileDodge dodge;
   private final NpcRead read = new NpcRead();
   private final Map<NpcMove, Integer> readyAt = new HashMap<>();
   private final Map<NpcTactic, Integer> tacticReadyAt = new HashMap<>();
   @Nullable
   private NpcTactic tactic;
   @Nullable
   private NpcTactic lastTactic;
   @Nullable
   private NpcTactic tacticBeforeLast;
   private NpcFooting footing = NpcFooting.JUKE;
   @Nullable
   private Vec3 walkDest;
   private boolean cooling;
   private boolean tacticUrgent;
   private NpcBrain.State state = NpcBrain.State.ROAM;
   @Nullable
   private LivingEntity trackedTarget;
   private int noticeUntil;
   private int noticeStart;
   private boolean noticeQuiet;
   private int thinkAt;
   @Nullable
   private NpcMove running;
   @Nullable
   private NpcMove intent;
   @Nullable
   private NpcMove lastMove;
   @Nullable
   private NpcMove moveBeforeLast;
   private int startedAt;
   private boolean venting;
   private int disengagedAt;
   private float retreatHealthMark = 1.1F;
   private boolean airFleeTried;
   @Nullable
   private Vec3 fleeDest;
   private int fleeReplanAt;
   private int waterRepathAt;
   @Nullable
   private Vec3 waterDest;
   private int hazardStepUntil;
   private final Vec3[] history = new Vec3[6];
   private int historyCount;
   @Nullable
   private Vec3 lastSeenPos;
   private int lastSeenTick;
   private int fleeSampleTick = Integer.MIN_VALUE;
   private Vec3 fleeSamplePos = Vec3.ZERO;
   @Nullable
   private Vec3 huntDest;
   private int huntReplanAt;
   private boolean huntReachedSpot;
   private int lostTargetTick = -61;
   private int bandSince;
   private double bandSinceDistance;
   private boolean fleeing;
   private int hugSince = Integer.MIN_VALUE;
   private boolean hugging;

   public static void observe(@Nullable NpcObserver watching) {
      observer = watching;
   }

   @Nullable
   static NpcObserver observer() {
      return observer;
   }

   NpcBrain(PoweredNpc npc, List<NpcMove> moves, List<NpcTactic> tactics) {
      this.npc = npc;
      this.moves = List.copyOf(moves);
      this.tactics = List.copyOf(tactics);
      this.footwork = new NpcFootwork(npc);
      this.dodge = new ProjectileDodge(npc, owner -> owner != npc && npc.caster().canHarm(owner));
   }

   public NpcBrain.State state() {
      return this.state;
   }

   @Nullable
   public NpcMove running() {
      return this.running;
   }

   public NpcFootwork footwork() {
      return this.footwork;
   }

   public boolean venting() {
      return this.venting;
   }

   public boolean cooling() {
      return this.cooling;
   }

   public String lastMoveId() {
      return this.lastMove == null ? "" : this.lastMove.id();
   }

   @Nullable
   public NpcTactic tactic() {
      return this.tactic;
   }

   public NpcFooting footing() {
      return this.footing;
   }

   public NpcRead read() {
      return this.read;
   }

   public boolean ready(String moveId, NpcContext ctx) {
      NpcMove move = this.move(moveId);
      return move != null && this.available(move, ctx);
   }

   public boolean usable(String moveId, NpcContext ctx) {
      NpcMove move = this.move(moveId);
      return move != null && this.available(move, ctx) && move.usable(ctx);
   }

   public boolean offCooldown(String moveId) {
      NpcMove move = this.move(moveId);
      return move != null
         && !AbilityGate.isSealed(this.npc)
         && this.npc.mastery() >= move.unlockLevel()
         && this.npc.tickCount >= this.readyAt.getOrDefault(move, 0);
   }

   public float moveScore(String moveId, NpcContext ctx) {
      NpcMove move = this.move(moveId);
      return move != null && this.available(move, ctx) ? move.score(ctx) : 0.0F;
   }

   @Nullable
   private NpcMove move(String id) {
      for (NpcMove m : this.moves) {
         if (m.id().equals(id)) {
            return m;
         }
      }

      return null;
   }

   public boolean forceStart(String moveId) {
      if (this.running == null && this.trackedTarget != null) {
         for (NpcMove move : this.moves) {
            if (move.id().equals(moveId)) {
               if (this.npc.mastery() >= move.unlockLevel() && this.npc.stamina().canAfford(move.staminaCost(), move.balanceKey())) {
                  this.start(move, this.context(this.trackedTarget));
                  return this.running == move;
               }

               return false;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public void interrupt() {
      if (this.running != null) {
         this.finish(this.context(null), true);
      }

      this.endTactic(this.context(null), "cut");
   }

   private void emit(String kind, String detail) {
      NpcObserver o = observer;
      if (o != null) {
         o.event(this.npc, kind, detail);
      }
   }

   void tick(ServerLevel level) {
      LivingEntity target = this.npc.getTarget();
      if (target != null
         && (!target.isAlive() || target.isRemoved() || target.level() != this.npc.level() || target instanceof Player p && (p.isCreative() || p.isSpectator()))
         )
       {
         this.npc.setTarget(null);
         target = null;
      }

      if (target != this.trackedTarget) {
         this.onTargetChanged(target);
      }

      if (target != null) {
         this.track(target);
         if (this.npc.tickCount - this.lastSeenTick > 600) {
            this.emit("forget", "unseen " + (this.npc.tickCount - this.lastSeenTick));
            this.npc.setTarget(null);
            target = null;
            this.onTargetChanged(null);
         }
      }

      if (this.running != null && AbilityGate.isSealed(this.npc)) {
         this.emit("sealed", this.running.id());
         this.finish(this.context(target), true);
      }

      if (target != null && AbilityGate.isSealed(this.npc)) {
         if (this.state != NpcBrain.State.REPOSITION) {
            this.emit("scared", "sealed");
         }

         this.state = NpcBrain.State.REPOSITION;
         this.intent = null;
         this.dodge.cancel();
         this.footwork.retreatFrom(target.position(), 1.3);
      } else if (this.running != null && this.running.committed()) {
         this.tickRunning(this.context(target));
      } else if (target == null) {
         this.state = NpcBrain.State.ROAM;
         this.venting = false;
         if (this.npc.tickCount % 20 == 0) {
            LivingEntity attacker = this.attackerInSight(level);
            if (attacker != null) {
               this.npc.setTarget(attacker);
            }
         }
      } else {
         NpcContext ctx = this.context(target);
         if (this.state == NpcBrain.State.NOTICE) {
            this.footwork.face(target);
            this.footwork.still();
            if (!this.noticeQuiet) {
               this.npc.kit().noticeTick(ctx, this.npc.tickCount - this.noticeStart);
            }

            if (this.npc.tickCount >= this.noticeUntil) {
               this.state = NpcBrain.State.ENGAGE;
            }
         } else {
            if (this.running != null && !this.running.allowsSidestep()) {
               this.dodge.cancel();
            } else {
               NpcObserver o = observer;
               long began = o == null ? 0L : System.nanoTime();
               this.dodge.scan(level, ctx.skill().sidestepChance(), ctx.skill().reactionTicks());
               if (o != null) {
                  o.section(this.npc, "dodge-scan", System.nanoTime() - began);
               }
            }

            boolean stepping = this.dodge.stepping();
            if (this.dodge.tick(level, target)) {
               if (!stepping) {
                  this.emit("sidestep", this.running == null ? "" : "under " + this.running.id());
               }

               if (this.running != null && !this.running.tick(ctx)) {
                  this.finish(ctx, false);
               }
            } else if (this.running != null) {
               this.tickRunning(ctx);
            } else if (!this.avoidHazards(ctx)) {
               if (this.updateDisengage(ctx)) {
                  this.disengage(ctx);
               } else {
                  this.updateVent(ctx);
                  if (this.npc.tickCount >= this.thinkAt) {
                     this.thinkAt = this.npc.tickCount + 4;
                     if (this.tactics.isEmpty()) {
                        this.intent = this.pick(ctx, this.cooling ? 2.0F : 0.0F);
                        NpcObserver ox = observer;
                        long beganx = ox == null ? 0L : System.nanoTime();
                        boolean go = this.intent != null && this.startable(this.intent, ctx);
                        if (ox != null && this.intent != null) {
                           ox.section(this.npc, "startable:" + this.intent.id(), System.nanoTime() - beganx);
                        }

                        if (go) {
                           this.start(this.intent, ctx);
                           return;
                        }
                     } else if (this.plan(ctx)) {
                        return;
                     }
                  }

                  if (!ctx.lineOfSight() && ctx.unseenTicks() >= 20 && this.lastSeenPos != null) {
                     this.state = NpcBrain.State.REPOSITION;
                     NpcObserver oxx = observer;
                     long beganxx = oxx == null ? 0L : System.nanoTime();
                     this.hunt();
                     if (oxx != null) {
                        oxx.section(this.npc, "hunt", System.nanoTime() - beganxx);
                     }
                  } else {
                     this.huntDest = null;
                     this.huntReachedSpot = false;
                     NpcBrain.Band band = this.band();
                     double min = band.min();
                     double max = band.max();
                     if (this.intent != null && !this.intent.inRange(ctx)) {
                        min = this.intent.minRange();
                        max = this.intent.maxRange();
                     }

                     if (this.walkDest != null) {
                        this.state = NpcBrain.State.REPOSITION;
                        this.footwork.walkTo(this.walkDest, 1.0);
                        this.footwork.face(target);
                     } else if (band.hold()) {
                        this.state = NpcBrain.State.ENGAGE;
                        this.footwork.face(target);
                        this.footwork.still();
                     } else {
                        NpcBrain.State next = !(ctx.distance() < min) && !(ctx.distance() > max) ? NpcBrain.State.ENGAGE : NpcBrain.State.REPOSITION;
                        if (next != this.state || next == NpcBrain.State.REPOSITION && Math.abs(ctx.distance() - this.bandSinceDistance) >= 1.0) {
                           this.bandSince = this.npc.tickCount;
                           this.bandSinceDistance = ctx.distance();
                        }

                        this.state = next;
                        NpcObserver oxx = observer;
                        long beganxx = oxx == null ? 0L : System.nanoTime();
                        this.footwork.keepBand(target, min, max, band.pace(), band.juke());
                        if (oxx != null) {
                           oxx.section(this.npc, "footwork", System.nanoTime() - beganxx);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private NpcBrain.Band band() {
      double min = this.npc.kit().preferredMin();
      double max = this.npc.kit().preferredMax();
      if (this.cooling) {
         return new NpcBrain.Band(min + 2.0, max + 2.0, true, 1.0, false);
      } else {
         return switch (this.footing) {
            case BAND -> new NpcBrain.Band(min, max, false, 1.0, false);
            case JUKE -> new NpcBrain.Band(min, max, true, 1.0, false);
            case PRESS -> new NpcBrain.Band(Math.max(4.0, min - 3.0), min + 2.0, true, 1.0, false);
            case KITE -> new NpcBrain.Band(max - 3.0, max + 3.0, false, 1.15, false);
            case HOLD -> new NpcBrain.Band(min, max, false, 1.0, true);
         };
      }
   }

   private boolean plan(NpcContext ctx) {
      NpcMove urgent = this.pick(ctx, 2.0F);
      if (urgent != null && this.startable(urgent, ctx)) {
         this.emit("reflex", urgent.id());
         this.start(urgent, ctx);
         return this.running == urgent;
      } else {
         this.intent = null;
         this.walkDest = null;
         NpcTactic t = this.tactic;
         if (t != null) {
            String why = t.age(ctx) > t.maxTicks()
               ? "timeout"
               : (t.broken(ctx) ? "broken" : (this.cooling && !t.whileCooling() && !this.tacticUrgent ? "cooling" : null));
            if (why != null) {
               this.endTactic(ctx, why);
            }
         }

         if (this.tactic == null) {
            this.chooseTactic(ctx);
         }

         t = this.tactic;
         if (t == null) {
            this.footing = NpcFooting.JUKE;
            return false;
         } else {
            NpcStep step = t.step(ctx);
            this.footing = step.footing();
            if (!this.cooling || this.tacticUrgent || step.kind() != NpcStep.Kind.CAST && step.kind() != NpcStep.Kind.TRAVEL) {
               switch (step.kind()) {
                  case DONE:
                     this.endTactic(ctx, "done");
                     this.footing = NpcFooting.JUKE;
                     break;
                  case CAST:
                     NpcMove move = step.move() == null ? null : this.move(step.move());
                     if (move != null && this.available(move, ctx) && move.usable(ctx)) {
                        if (this.startable(move, ctx)) {
                           this.start(move, ctx);
                           if (this.running == move) {
                              t.started(ctx, move);
                              return true;
                           }
                        }

                        if (!move.inRange(ctx)) {
                           this.intent = move;
                        }
                     }
                     break;
                  case TRAVEL:
                     if (step.dest() != null && step.purpose() != null) {
                        if (this.travel(ctx, step.dest(), step.purpose())) {
                           t.started(ctx, this.running);
                           return true;
                        }

                        this.walkDest = step.dest();
                     }
                  case MOVE:
               }

               return false;
            } else {
               return false;
            }
         }
      }
   }

   private void chooseTactic(NpcContext ctx) {
      NpcTactic best = null;
      float bestScore = 0.15F;
      StringBuilder weighed = observer == null ? null : new StringBuilder();

      for (NpcTactic t : this.tactics) {
         if (this.npc.mastery() >= t.unlockLevel() && this.npc.tickCount >= this.tacticReadyAt.getOrDefault(t, 0)) {
            t.clearReason();
            float w = t.score(ctx);
            if (!(w <= 0.0F) && (!this.cooling || t.whileCooling() || !(w < 2.0F))) {
               if (t == this.lastTactic) {
                  w *= this.lastTactic == this.tacticBeforeLast ? 0.35F : 0.7F;
               }

               if (this.lastMove != null && this.lastMove.id().equals(t.opens())) {
                  w *= this.lastMove == this.moveBeforeLast ? 0.6F : 0.8F;
               }

               w *= 1.0F + ctx.skill().scoreNoise() * (this.npc.getRandom().nextFloat() * 2.0F - 1.0F);
               if (weighed != null) {
                  weighed.append(String.format(Locale.ROOT, " %s:%.2f", t.id(), w));
               }

               if (w > bestScore) {
                  bestScore = w;
                  best = t;
               }
            }
         }
      }

      if (best != null) {
         best.clearReason();
         this.tacticUrgent = best.score(ctx) >= 2.0F;
         this.tactic = best;
         this.tacticBeforeLast = this.lastTactic;
         this.lastTactic = best;
         this.tacticReadyAt.put(best, this.npc.tickCount + best.cooldownTicks());
         best.startRun(ctx);
         if (observer != null) {
            this.emit("tactic", best.id() + " " + (best.reason().isEmpty() ? "-" : best.reason()) + " |" + weighed);
            if (!best.reason().isEmpty()) {
               this.emit("react", best.reason() + " by " + best.id());
            }
         }
      }
   }

   private void endTactic(NpcContext ctx, String why) {
      NpcTactic t = this.tactic;
      if (t != null) {
         this.tactic = null;
         this.walkDest = null;
         t.end(ctx);
         if (observer != null) {
            this.emit("tactic_end", t.id() + " " + why + " after " + t.age(ctx));
         }
      }
   }

   private void tickRunning(NpcContext ctx) {
      NpcMove move = this.running;
      if (move != null) {
         NpcObserver o = observer;
         long began = o == null ? 0L : System.nanoTime();
         boolean going = move.tick(ctx);
         if (o != null) {
            o.section(this.npc, "run:" + move.id(), System.nanoTime() - began);
         }

         if (!going) {
            this.finish(ctx, false);
         } else if (this.running == move) {
            LivingEntity target = ctx.target();
            if (target != null && !ctx.lineOfSight() && ctx.unseenTicks() >= 20) {
               this.footwork.still();
            } else if (target != null) {
               NpcBrain.Band band = this.band();
               this.footwork.keepBand(target, band.min(), band.max(), band.pace() * move.footworkWhileRunning(), band.juke());
            } else {
               this.footwork.still();
            }
         }
      }
   }

   @Nullable
   private NpcMove pick(NpcContext ctx, float minScore) {
      NpcObserver o = observer;
      List<NpcObserver.Option> options = o == null ? null : new ArrayList<>(this.moves.size());
      NpcMove best = null;
      float bestScore = 0.15F;

      for (NpcMove move : this.moves) {
         if (!this.available(move, ctx)) {
            if (options != null && this.npc.mastery() >= move.unlockLevel()) {
               options.add(new NpcObserver.Option(move.id(), 0.0F, this.npc.tickCount < this.readyAt.getOrDefault(move, 0) ? "cd" : "cost"));
            }
         } else {
            long began = o == null ? 0L : System.nanoTime();
            float w = move.score(ctx);
            if (o != null) {
               o.section(this.npc, "score:" + move.id(), System.nanoTime() - began);
            }

            if (!(w <= 0.0F) && !(w < minScore)) {
               if (!move.inRange(ctx)) {
                  double toBand = Math.max(0.0, Math.max(move.minRange() - ctx.distance(), ctx.distance() - move.maxRange()));
                  w *= (float)Mth.clamp(1.0 - toBand / 14.0, 0.1, 1.0);
               }

               if (move == this.lastMove) {
                  w *= this.lastMove == this.moveBeforeLast ? 0.35F : 0.7F;
               }

               w *= 1.0F + ctx.skill().scoreNoise() * (this.npc.getRandom().nextFloat() * 2.0F - 1.0F);
               if (options != null) {
                  options.add(
                     new NpcObserver.Option(
                        move.id(), w, !move.inRange(ctx) ? "out of band" : (move.needsLineOfSight() && !ctx.lineOfSight() ? "no sight" : "")
                     )
                  );
               }

               if (w > bestScore) {
                  bestScore = w;
                  best = move;
               }
            } else if (options != null) {
               options.add(new NpcObserver.Option(move.id(), w, w <= 0.0F ? "zero" : "not urgent"));
            }
         }
      }

      if (o != null) {
         options.sort((a, b) -> Float.compare(b.score(), a.score()));
         o.think(this.npc, ctx, this.state, options, best);
      }

      return best;
   }

   private boolean available(NpcMove move, NpcContext ctx) {
      return !AbilityGate.isSealed(this.npc)
         && this.npc.mastery() >= move.unlockLevel()
         && this.npc.tickCount >= this.readyAt.getOrDefault(move, 0)
         && this.npc.stamina().canAfford(move.staminaCost(), move.balanceKey());
   }

   private boolean startable(NpcMove move, NpcContext ctx) {
      return (move.inRange(ctx) || this.cornered(ctx)) && (!move.needsLineOfSight() || ctx.lineOfSight()) && move.canStart(ctx);
   }

   private boolean cornered(NpcContext ctx) {
      return this.state == NpcBrain.State.REPOSITION && this.npc.tickCount - this.bandSince >= 40 && Math.abs(ctx.distance() - this.bandSinceDistance) < 1.0;
   }

   private void start(NpcMove move, NpcContext ctx) {
      if (!AbilityGate.isSealed(this.npc) && this.npc.stamina().spend(move.staminaCost(), move.balanceKey(), this.npc.level().getGameTime())) {
         this.readyAt.put(move, this.npc.tickCount + move.cooldownTicks());
         this.moveBeforeLast = this.lastMove;
         this.lastMove = move;
         this.running = move;
         this.intent = null;
         if (this.state != NpcBrain.State.DISENGAGE) {
            this.state = NpcBrain.State.ENGAGE;
         }

         if (!move.allowsSidestep()) {
            this.dodge.cancel();
         }

         this.startedAt = this.npc.tickCount;
         if (observer != null) {
            this.emit(
               "move_start",
               String.format(
                  Locale.ROOT,
                  "%s d=%.1f los=%d sta=%.2f str=%.2f hp=%.2f%s",
                  move.id(),
                  ctx.distance(),
                  ctx.lineOfSight() ? 1 : 0,
                  ctx.stamina(),
                  ctx.strain(),
                  ctx.healthFraction(),
                  this.state == NpcBrain.State.DISENGAGE ? " (disengaged)" : (this.venting ? " (venting)" : "")
               )
            );
         }

         move.start(ctx);
      }
   }

   private void finish(NpcContext ctx, boolean interrupted) {
      NpcMove move = this.running;
      this.running = null;
      if (move != null) {
         move.stop(ctx, interrupted);
         if (observer != null) {
            this.emit(
               "move_stop",
               String.format(
                  Locale.ROOT,
                  "%s after %d%s sta=%.2f str=%.2f",
                  move.id(),
                  this.npc.tickCount - this.startedAt,
                  interrupted ? " interrupted" : "",
                  this.npc.stamina().fraction(),
                  this.npc.kit().strain(this.npc.caster())
               )
            );
         }
      }
   }

   private boolean travel(NpcContext ctx, Vec3 destination, NpcTravel.Purpose purpose) {
      if (this.running != null) {
         return false;
      } else {
         for (NpcMove move : this.moves) {
            if (move instanceof NpcTravel mobility && this.available(move, ctx) && mobility.plan(ctx, destination, purpose)) {
               this.emit("travel", purpose + " by " + move.id());
               this.start(move, ctx);
               return this.running == move;
            }
         }

         return false;
      }
   }

   private void hunt() {
      Vec3 seen = this.lastSeenPos;
      boolean stalled = this.huntDest != null && this.npc.getNavigation().isDone() && this.npc.tickCount >= this.huntReplanAt - 60 + 10;
      if (this.huntDest == null || this.npc.tickCount >= this.huntReplanAt || stalled || NpcNav.flat(this.npc.position(), this.huntDest) < 2.5) {
         if (this.huntDest != null && NpcNav.flat(this.huntDest, seen) < 2.5) {
            this.huntReachedSpot = true;
         }

         this.huntReplanAt = this.npc.tickCount + 60;
         Vec3 next = seen;
         if (this.huntReachedSpot) {
            double a = this.npc.getRandom().nextDouble() * Math.PI * 2.0;
            double r = 4.0 + this.npc.getRandom().nextDouble() * 6.0;
            Vec3 around = NpcNav.standInColumn(this.npc.level(), seen.add(Math.cos(a) * r, 0.0, Math.sin(a) * r), 2, 3);
            next = around != null ? around : seen;
         }

         if (this.huntDest == null) {
            this.emit("hunt", String.format(Locale.ROOT, "to last seen %.1f %.1f %.1f", seen.x, seen.y, seen.z));
         }

         this.huntDest = next;
         this.footwork.repathNow();
      }

      this.footwork.walkTo(this.huntDest, 0.9);
      this.npc.getLookControl().setLookAt(this.huntDest.x, this.huntDest.y + 1.5, this.huntDest.z);
   }

   @Nullable
   private LivingEntity attackerInSight(ServerLevel level) {
      double r = 24.0;
      LivingEntity best = null;
      double bestD = Double.MAX_VALUE;

      for (Mob m : level.getEntitiesOfClass(Mob.class, this.npc.getBoundingBox().inflate(r), mx -> mx.getTarget() == this.npc && mx.isAlive())) {
         double d = m.distanceToSqr(this.npc);
         if (d < bestD && d <= r * r && this.npc.caster().canHarm(m) && this.npc.getSensing().hasLineOfSight(m)) {
            bestD = d;
            best = m;
         }
      }

      return best;
   }

   private void updateVent(NpcContext ctx) {
      if (!this.venting && ctx.strain() >= 1.0F) {
         this.venting = true;
         this.emit("vent_on", String.format(Locale.ROOT, "str=%.2f", ctx.strain()));
         this.npc.kit().onVent(ctx);
      } else if (this.venting && ctx.strain() < 0.72F) {
         this.venting = false;
         this.emit("vent_off", "");
      }

      boolean dry = this.cooling && !this.venting ? ctx.stamina() < 0.3F : ctx.stamina() < 0.12F;
      this.cooling = this.venting || dry;
   }

   private boolean updateDisengage(NpcContext ctx) {
      float health = ctx.healthFraction();
      if (this.state == NpcBrain.State.DISENGAGE) {
         boolean rested = ctx.stamina() >= 0.5F && ctx.strain() < 0.6F;
         if (!rested || !(health >= 0.45F) && this.npc.tickCount - this.disengagedAt < 120) {
            return true;
         } else {
            this.state = NpcBrain.State.ENGAGE;
            this.fleeDest = null;
            this.emit("disengage_off", String.format(Locale.ROOT, "after %d hp=%.2f", this.npc.tickCount - this.disengagedAt, health));
            return false;
         }
      } else {
         boolean hurt = health < 0.3F && health < this.retreatHealthMark - 0.1F && ctx.read().healthFraction() >= 0.3F;
         if (!hurt) {
            return false;
         } else {
            LivingEntity target = ctx.target();
            if (target != null && NpcNav.fleePoint(this.npc, target, true) == null) {
               this.retreatHealthMark = health;
               this.emit("stand", String.format(Locale.ROOT, "no cover hp=%.2f", health));
               return false;
            } else {
               this.state = NpcBrain.State.DISENGAGE;
               this.disengagedAt = this.npc.tickCount;
               this.airFleeTried = false;
               this.fleeDest = null;
               this.retreatHealthMark = health;
               this.endTactic(ctx, "disengage");
               this.emit("disengage_on", String.format(Locale.ROOT, "hurt hp=%.2f sta=%.2f str=%.2f", health, ctx.stamina(), ctx.strain()));
               return true;
            }
         }
      }
   }

   private void disengage(NpcContext ctx) {
      LivingEntity target = ctx.target();
      if (target != null) {
         if (!this.airFleeTried && ctx.distance() < 14.0) {
            this.airFleeTried = true;
            Vec3 away = NpcNav.fleePoint(this.npc, target);
            if (away != null && this.travel(ctx, away, NpcTravel.Purpose.FLEE)) {
               return;
            }
         }

         if (this.npc.tickCount >= this.thinkAt) {
            this.thinkAt = this.npc.tickCount + 4;
            NpcMove answer = this.pick(ctx, 2.0F);
            if (answer != null && this.startable(answer, ctx)) {
               this.start(answer, ctx);
               return;
            }
         }

         if (!ctx.lineOfSight() && ctx.distance() >= 10.0) {
            this.footwork.still();
            if (this.lastSeenPos != null) {
               this.npc.getLookControl().setLookAt(this.lastSeenPos.x, this.lastSeenPos.y + 1.5, this.lastSeenPos.z);
            }

            if (ctx.unseenTicks() >= 40 && this.npc.tickCount % 40 == 0 && this.npc.getHealth() < this.npc.getMaxHealth()) {
               this.npc.heal(1.0F);
            }
         } else {
            if (this.fleeDest == null || this.npc.tickCount >= this.fleeReplanAt || this.npc.position().distanceToSqr(this.fleeDest) < 4.0) {
               this.fleeReplanAt = this.npc.tickCount + 60;
               this.fleeDest = NpcNav.fleePoint(this.npc, target);
               this.footwork.repathNow();
            }

            if (this.fleeDest != null) {
               this.footwork.walkTo(this.fleeDest, 1.15);
            } else {
               this.footwork.retreat(target, 1.15);
            }

            if (ctx.distance() < this.npc.kit().preferredMin()) {
               this.footwork.face(target);
            }
         }
      }
   }

   private boolean avoidHazards(NpcContext ctx) {
      if (this.npc.kit().avoidsWater() && NpcNav.wet(this.npc)) {
         if (this.npc.tickCount >= this.waterRepathAt) {
            this.waterRepathAt = this.npc.tickCount + 20;
            this.waterDest = NpcNav.dryLand(this.npc, 12);
            this.emit(
               "water", this.waterDest == null ? "no land" : String.format(Locale.ROOT, "to land %.1f away", NpcNav.flat(this.npc.position(), this.waterDest))
            );
            if (this.waterDest != null && this.travel(ctx, this.waterDest, NpcTravel.Purpose.LEAVE_HAZARD)) {
               return true;
            }
         }

         if (this.waterDest == null) {
            return false;
         } else {
            NpcFootwork.wadeTo(this.npc, this.waterDest, 1.25);
            return true;
         }
      } else {
         this.waterDest = null;
         LivingEntity target = ctx.target();
         if (this.npc.tickCount < this.hazardStepUntil) {
            if (target != null) {
               this.footwork.face(target);
            }

            return true;
         } else {
            Vec3 hazard = this.npc.kit().hazardNear(this.npc.caster());
            if (hazard == null) {
               return false;
            } else {
               this.hazardStepUntil = this.npc.tickCount + 15;
               this.emit("hazard", String.format(Locale.ROOT, "%.1f %.1f %.1f", hazard.x, hazard.y, hazard.z));
               this.footwork.repathNow();
               this.footwork.retreatFrom(hazard, 1.2);
               if (target != null) {
                  this.footwork.face(target);
               }

               return true;
            }
         }
      }
   }

   private void onTargetChanged(@Nullable LivingEntity target) {
      if (this.running != null && !this.running.committed()) {
         this.finish(this.context(this.trackedTarget), true);
      }

      this.endTactic(this.context(this.trackedTarget), "target");
      this.read.reset(this.npc.tickCount);
      boolean switching = this.trackedTarget != null || this.npc.tickCount - this.lostTargetTick <= 60;
      if (this.trackedTarget != null && target == null) {
         this.lostTargetTick = this.npc.tickCount;
      }

      this.huntDest = null;
      this.huntReachedSpot = false;
      this.trackedTarget = target;
      this.historyCount = 0;
      this.fleeSampleTick = Integer.MIN_VALUE;
      this.fleeing = false;
      this.hugSince = Integer.MIN_VALUE;
      this.hugging = false;
      this.intent = null;
      if (target != null) {
         this.lastSeenTick = this.npc.tickCount;
         this.lastSeenPos = target.position();
         this.state = NpcBrain.State.NOTICE;
         int beat = NpcSkill.of(this.npc.mastery()).reactionTicks();
         this.noticeUntil = this.npc.tickCount + (switching ? beat / 2 : beat);
         this.noticeStart = this.npc.tickCount;
         this.noticeQuiet = switching || this.running != null;
         this.emit("notice", target.getType().toShortString() + " d=" + Math.round(this.npc.distanceTo(target)) + (switching ? " (switch)" : ""));
         if (!this.noticeQuiet) {
            this.npc.kit().onNotice(this.context(target));
         }
      } else {
         this.lastSeenPos = null;
      }
   }

   private void track(LivingEntity target) {
      System.arraycopy(this.history, 0, this.history, 1, this.history.length - 1);
      this.history[0] = target.position();
      this.historyCount = Math.min(this.historyCount + 1, this.history.length);
      boolean inSight = this.npc.getSensing().hasLineOfSight(target);
      if (inSight) {
         this.lastSeenTick = this.npc.tickCount;
         this.lastSeenPos = target.position();
      }

      this.read.update(this.npc.tickCount, this.npc, target, this.targetVelocity(), inSight);
      double dist = (double)this.npc.distanceTo(target);
      if (dist <= 4.0) {
         if (this.hugSince == Integer.MIN_VALUE) {
            this.hugSince = this.npc.tickCount;
         }

         this.hugging = this.npc.tickCount - this.hugSince >= 40;
      } else {
         this.hugSince = Integer.MIN_VALUE;
         this.hugging = false;
      }

      if (this.fleeSampleTick == Integer.MIN_VALUE) {
         this.fleeSampleTick = this.npc.tickCount;
         this.fleeSamplePos = target.position();
      } else if (this.npc.tickCount - this.fleeSampleTick >= 20) {
         Vec3 out = new Vec3(target.getX() - this.npc.getX(), 0.0, target.getZ() - this.npc.getZ());
         double len = out.length();
         Vec3 moved = target.position().subtract(this.fleeSamplePos);
         this.fleeing = len > 0.001 && (moved.x * out.x + moved.z * out.z) / len >= 2.5;
         this.fleeSampleTick = this.npc.tickCount;
         this.fleeSamplePos = target.position();
      }
   }

   private Vec3 targetVelocity() {
      if (this.historyCount < 2) {
         return Vec3.ZERO;
      } else {
         int oldest = this.historyCount - 1;
         return this.history[0].subtract(this.history[oldest]).scale(1.0 / (double)oldest);
      }
   }

   NpcContext context(@Nullable LivingEntity target) {
      boolean los = target != null && this.npc.getSensing().hasLineOfSight(target);
      return new NpcContext(
         this.npc,
         this.npc.caster(),
         target,
         target == null ? Double.MAX_VALUE : (double)this.npc.distanceTo(target),
         los,
         target == null ? Vec3.ZERO : this.targetVelocity(),
         this.lastSeenPos,
         this.lastSeenTick,
         this.fleeing,
         this.hugging,
         this.npc.kit().strain(this.npc.caster()),
         this.npc.stamina().fraction(),
         NpcSkill.of(this.npc.mastery()),
         this.read
      );
   }

   private static record Band(double min, double max, boolean juke, double pace, boolean hold) {
   }

   public static enum State {
      ROAM,
      NOTICE,
      ENGAGE,
      REPOSITION,
      DISENGAGE;
   }
}
