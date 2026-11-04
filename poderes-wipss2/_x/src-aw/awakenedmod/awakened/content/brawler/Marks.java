package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.TickFreeze;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.mixin.CreeperFuseAccessor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class Marks {
   private static final Map<Integer, Marks.Held> HELD = new ConcurrentHashMap<>();
   private static final Map<Integer, Marks.Ledger> LEDGERS = new ConcurrentHashMap<>();
   private static final Map<UUID, Marks.Transitions> TRANSITIONS = new ConcurrentHashMap<>();
   private static final Map<UUID, Deque<Integer>> RECENT = new ConcurrentHashMap<>();
   private static int lastTick = -1;

   private Marks() {
   }

   public static int apply(ServerPlayer player, LivingEntity target, Mark mark, int baseTicks, Mark.Flavour flavour) {
      return apply(player, target, mark, baseTicks, flavour, null);
   }

   public static int apply(ServerPlayer player, LivingEntity target, Mark mark, int baseTicks, Mark.Flavour flavour, String abilityId) {
      if (target != null && target != player && target.isAlive() && !target.isRemoved() && baseTicks > 0) {
         payLandingBeat(HELD.get(target.getId()));
         if (target.isAlive() && !target.isRemoved()) {
            long now = target.level().getGameTime();
            Marks.Ledger ledger = LEDGERS.computeIfAbsent(target.getId(), k -> new Marks.Ledger());
            int tier = currentTier(ledger, mark, now);
            int ticks = ladderTicks(ledger, mark, tier, baseTicks, now);
            ledger.lastTouch = now;
            Marks.Held held = HELD.computeIfAbsent(target.getId(), k -> new Marks.Held(target));
            unfreeze(held, false);
            BrawlerHitStop.clear(target);
            held.owner = player.getUUID();
            held.flavour = flavour == null ? Mark.Flavour.NONE : flavour;
            held.createdBy = abilityId;
            held.expiry = Math.max(held.expiry, now + (long)ticks);
            held.age = 0;
            Mark landed = displace(player, held, target, mark, tier);
            held.mark = landed;
            if (target instanceof ServerPlayer victim) {
               held.lockLeft = 0;
               Harm.effect(player, victim, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1, false, false, true));
            } else {
               held.lockLeft = ticks;
               if (landed != Mark.AIRBORNE && !held.settling) {
                  engageLock(held);
               }

               BrawlerHitStop.moving(target, held.frozen ? 0 : 6);
               defuse(target);
               pose(held, landed == Mark.GROUNDED);
            }

            publish(held);
            remember(player, target);
            if (!held.settling) {
               BrawlerFx.markLanded(player.serverLevel(), player, target, landed, held.flavour);
            }

            return ticks;
         } else {
            return 0;
         }
      } else {
         return 0;
      }
   }

   private static Mark displace(ServerPlayer player, Marks.Held held, LivingEntity target, Mark mark, int tier) {
      double scale = displacementScale(target);
      boolean mob = !(target instanceof Player);
      held.launchHeight = 0.0;
      held.settling = false;
      held.settleTicks = 0;
      held.driveSpeed = 0.0;
      held.landingBeat = null;
      switch (mark) {
         case AIRBORNE:
            double height = BrawlerTuning.HANG_HEIGHT[Math.min(tier, BrawlerTuning.HANG_HEIGHT.length - 1)] * scale;
            if (MarkHang.ceilingWithin(target, height + 0.5)) {
               if (target.level() instanceof ServerLevel level) {
                  BrawlerFx.ceilingSlam(level, target);
               }

               return displace(player, held, target, Mark.GROUNDED, tier);
            }

            held.launchHeight = height;
            if (mob) {
               MarkHang.launch(player, target, height);
            } else {
               Vec3 look = player.getLookAngle();
               Harm.setMotion(player, target, look.x * 0.15, BrawlerTuning.launchSpeedFor(height), look.z * 0.15);
               target.resetFallDistance();
            }

            return Mark.AIRBORNE;
         case GROUNDED:
            MarkHang.end(target);
            boolean down = target.onGround();
            double speed = (down ? 0.2 : 1.2) * scale;
            Harm.setMotion(player, target, 0.0, -speed, 0.0);
            target.resetFallDistance();
            held.settling = mob && !down;
            held.driveSpeed = speed;
            return Mark.GROUNDED;
         default:
            MarkHang.end(target);
            Harm.setMotion(player, target, 0.0, 0.0, 0.0);
            held.settling = mob && !target.onGround();
            return Mark.PINNED;
      }
   }

   public static double displacementScale(LivingEntity target) {
      return BrawlerTargets.unliftable(target) ? 0.35 : 1.0;
   }

   private static boolean settle(Marks.Held held) {
      LivingEntity target = held.target;
      if (!target.onGround() && !target.isPassenger()) {
         held.settleTicks++;
         double speed = Math.min(1.6, held.driveSpeed + 0.14);
         held.driveSpeed = speed;
         Vec3 v = target.getDeltaMovement();
         target.setDeltaMovement(v.x, -speed, v.z);
         target.hurtMarked = true;
         target.resetFallDistance();
         if (TickFreeze.isFrozen(target)) {
            target.move(MoverType.SELF, new Vec3(0.0, -speed, 0.0));
            target.resetFallDistance();
         }

         BrawlerHitStop.moving(target, 2);
         return target.onGround() || held.settleTicks >= 40;
      } else {
         return true;
      }
   }

   private static void touchdown(Marks.Held held, ServerPlayer owner) {
      held.settling = false;
      BrawlerHitStop.moving(held.target, 0);
      engageLock(held);
      pose(held, held.mark == Mark.GROUNDED);
      if (held.target.level() instanceof ServerLevel level) {
         BrawlerFx.markLanded(level, owner, held.target, held.mark, held.flavour);
      }

      payLandingBeat(held);
   }

   private static void payLandingBeat(Marks.Held held) {
      if (held != null && held.landingBeat != null) {
         Consumer<ServerLevel> beat = held.landingBeat;
         held.landingBeat = null;
         if (held.target.level() instanceof ServerLevel level) {
            beat.accept(level);
         }
      }
   }

   private static void engageLock(Marks.Held held) {
      if (!held.frozen && held.lockLeft > 0 && !(held.target instanceof Player)) {
         TickFreeze.freeze(held.target);
         held.frozen = true;
         held.target.setDeltaMovement(Vec3.ZERO);
         held.target.hurtMarked = true;
      }
   }

   private static void releaseLock(Marks.Held held) {
      unfreeze(held, true);
      held.lockLeft = 0;
   }

   private static void unfreeze(Marks.Held held, boolean settle) {
      if (held.frozen) {
         TickFreeze.release(held.target);
         held.frozen = false;
         if (settle && held.target.isAlive()) {
            held.target.setDeltaMovement(Vec3.ZERO);
            held.target.hurtMarked = true;
         }
      }
   }

   private static void defuse(LivingEntity target) {
      if (target instanceof Creeper creeper) {
         creeper.setSwellDir(-1);
         if (creeper instanceof CreeperFuseAccessor fuse) {
            fuse.awakened$setSwell(0);
            fuse.awakened$setOldSwell(0);
         }
      }
   }

   private static void pose(Marks.Held held, boolean prone) {
      if (held.target instanceof Mob mob) {
         if (prone) {
            mob.setPose(Pose.SWIMMING);
            held.prone = true;
         } else if (held.prone) {
            mob.setPose(Pose.STANDING);
            held.prone = false;
         }
      }
   }

   private static int currentTier(Marks.Ledger ledger, Mark mark, long now) {
      int idx = mark.ordinal();
      long since = now - ledger.tierTick[idx];
      int recovered = (int)Math.max(0L, since / 60L);
      return Math.max(0, ledger.tier[idx] - recovered);
   }

   private static int ladderTicks(Marks.Ledger ledger, Mark mark, int tier, int baseTicks, long now) {
      int idx = mark.ordinal();
      float mult = BrawlerTuning.MARK_LADDER[Math.min(tier, BrawlerTuning.MARK_LADDER.length - 1)];
      ledger.tier[idx] = Math.min(BrawlerTuning.MARK_LADDER.length - 1, tier + 1);
      ledger.tierTick[idx] = now;
      return Math.max(5, Math.round((float)baseTicks * mult));
   }

   private static void touchLedger(LivingEntity target) {
      Marks.Ledger ledger = LEDGERS.get(target.getId());
      if (ledger != null) {
         ledger.lastTouch = target.level().getGameTime();
      }
   }

   public static int lockLeft(LivingEntity target) {
      Marks.Held held = target == null ? null : HELD.get(target.getId());
      return held == null ? 0 : held.lockLeft;
   }

   public static int ladderTier(LivingEntity target, Mark mark) {
      Marks.Ledger ledger = target == null ? null : LEDGERS.get(target.getId());
      return ledger == null ? 0 : currentTier(ledger, mark, target.level().getGameTime());
   }

   public static Mark current(LivingEntity target) {
      Marks.Held held = target == null ? null : HELD.get(target.getId());
      return held == null ? null : held.mark;
   }

   public static Mark.Flavour flavour(LivingEntity target) {
      Marks.Held held = target == null ? null : HELD.get(target.getId());
      return held != null && held.flavour != null ? held.flavour : Mark.Flavour.NONE;
   }

   public static double launchHeight(LivingEntity target) {
      Marks.Held held = target == null ? null : HELD.get(target.getId());
      return held != null && held.mark == Mark.AIRBORNE ? held.launchHeight : 0.0;
   }

   public static String createdBy(LivingEntity target) {
      Marks.Held held = target == null ? null : HELD.get(target.getId());
      return held == null ? null : held.createdBy;
   }

   public static boolean locked(LivingEntity target) {
      Marks.Held held = target == null ? null : HELD.get(target.getId());
      return held != null && held.frozen;
   }

   public static boolean settling(LivingEntity target) {
      Marks.Held held = target == null ? null : HELD.get(target.getId());
      return held != null && held.settling;
   }

   public static void atLanding(LivingEntity target, Consumer<ServerLevel> beat) {
      if (target != null && beat != null) {
         Marks.Held held = HELD.get(target.getId());
         if (held != null && held.settling) {
            held.landingBeat = beat;
         } else if (target.level() instanceof ServerLevel level) {
            beat.accept(level);
         }
      }
   }

   public static boolean marked(LivingEntity target) {
      return target != null && HELD.containsKey(target.getId());
   }

   public static boolean airborne(LivingEntity target) {
      return target != null && (current(target) == Mark.AIRBORNE || !target.onGround() && !target.isPassenger());
   }

   public static Mark shapeFor(LivingEntity target) {
      Mark mark = current(target);
      return mark != null ? mark : (airborne(target) ? Mark.AIRBORNE : null);
   }

   public static boolean airborneNear(ServerPlayer player, double radius) {
      double radiusSqr = radius * radius;

      for (Marks.Held held : HELD.values()) {
         if (held.mark == Mark.AIRBORNE && held.target.level() == player.serverLevel() && held.target.distanceToSqr(player) <= radiusSqr) {
            return true;
         }
      }

      return false;
   }

   public static List<LivingEntity> recent(ServerPlayer player) {
      Deque<Integer> ids = RECENT.get(player.getUUID());
      if (ids != null && !ids.isEmpty()) {
         List<LivingEntity> bodies = new ArrayList<>(ids.size());

         for (Integer id : ids) {
            Entity var6 = player.serverLevel().getEntity(id);
            if (var6 instanceof LivingEntity) {
               LivingEntity body = (LivingEntity)var6;
               if (body.isAlive() && !body.isRemoved()) {
                  bodies.add(body);
               }
            }
         }

         return bodies;
      } else {
         return List.of();
      }
   }

   public static boolean consumeTransition(ServerPlayer player, LivingEntity target, String abilityId) {
      Marks.Held held = target == null ? null : HELD.get(target.getId());
      if (held != null
         && held.mark != null
         && held.createdBy != null
         && abilityId != null
         && !abilityId.equals(held.createdBy)
         && BrawlerTargets.countsForCombo(player, target)) {
         Marks.Transitions transitions = TRANSITIONS.computeIfAbsent(player.getUUID(), k -> new Marks.Transitions());
         transitions.keys.add(held.createdBy + ">" + abilityId);
         return true;
      } else {
         return false;
      }
   }

   public static int transitions(ServerPlayer player) {
      Marks.Transitions transitions = TRANSITIONS.get(player.getUUID());
      return transitions == null ? 0 : Math.min(5, transitions.keys.size());
   }

   public static int transitionsConverted(ServerPlayer player) {
      Marks.Transitions transitions = TRANSITIONS.get(player.getUUID());
      return transitions == null ? 0 : transitions.keys.size();
   }

   public static void clearTransitions(ServerPlayer player) {
      TRANSITIONS.remove(player.getUUID());
   }

   public static void release(LivingEntity target) {
      BrawlerHitStop.clear(target);
      Marks.Held held = target == null ? null : HELD.remove(target.getId());
      if (held != null) {
         payLandingBeat(held);
         letGo(held, held.target.isAlive());
      }
   }

   public static void tickAll(MinecraftServer server) {
      int now = server.getTickCount();
      if (now != lastTick) {
         lastTick = now;
         boolean tellTick = now % 2 == 0;
         Map<UUID, Set<Integer>> tellSets = tellTick ? new HashMap<>() : null;
         Iterator<Entry<Integer, Marks.Held>> it = HELD.entrySet().iterator();

         while (it.hasNext()) {
            Marks.Held held = it.next().getValue();
            LivingEntity target = held.target;
            if (target.isAlive() && !target.isRemoved()) {
               ServerPlayer owner = server.getPlayerList().getPlayer(held.owner);
               if (owner != null && owner.level() == target.level()) {
                  held.age++;
                  if (held.mark == Mark.AIRBORNE && !(target instanceof Player) && !flight(held, owner)) {
                     it.remove();
                     letGo(held, true);
                  } else {
                     if (held.settling && settle(held)) {
                        touchdown(held, owner);
                     }

                     if (held.frozen) {
                        touchLedger(target);
                        if (--held.lockLeft <= 0) {
                           releaseLock(held);
                        }
                     }

                     if (target.level().getGameTime() >= held.expiry && !held.settling) {
                        it.remove();
                        letGo(held, true);
                     } else if (tellTick) {
                        Level telling = target.level();
                        if (telling instanceof ServerLevel) {
                           ServerLevel level = (ServerLevel)telling;
                           Set<Integer> tellingx = tellSets.computeIfAbsent(held.owner, k -> tellSet(owner));
                           if (tellingx.contains(target.getId())) {
                              if (held.prone) {
                                 pose(held, true);
                              }

                              BrawlerFx.markTell(level, target, held.mark, held.flavour, held.age);
                           }
                        }
                     }
                  }
               } else {
                  it.remove();
                  letGo(held, true);
               }
            } else {
               it.remove();
               letGo(held, false);
            }
         }

         MarkHang.sweep(server);
         if (now % 40 == 0) {
            pruneLedgers(server.overworld().getGameTime());
         }
      }
   }

   private static boolean flight(Marks.Held held, ServerPlayer owner) {
      switch (MarkHang.step(held.target)) {
         case APEX:
            engageLock(held);
            break;
         case CEILING:
            if (held.target.level() instanceof ServerLevel level) {
               BrawlerFx.ceilingSlam(level, held.target);
            }

            held.mark = displace(owner, held, held.target, Mark.GROUNDED, 0);
            pose(held, true);
            BrawlerHitStop.moving(held.target, 6);
            if (!held.settling) {
               engageLock(held);
            }

            publish(held);
            break;
         case BOUNCE:
            if (held.target.level() instanceof ServerLevel level) {
               BrawlerFx.markBounce(level, held.target);
            }
            break;
         case LANDED:
            return false;
      }

      return true;
   }

   private static Set<Integer> tellSet(ServerPlayer owner) {
      Set<Integer> set = new HashSet<>();
      LivingEntity aimed = AimUtil.aimedLivingSoft(owner, 16.0, 30.0, e -> e != owner && marked(e));
      if (aimed != null) {
         set.add(aimed.getId());
      }

      Deque<Integer> recent = RECENT.get(owner.getUUID());
      if (recent != null) {
         for (Integer id : recent) {
            if (set.size() >= 3) {
               break;
            }

            set.add(id);
         }
      }

      return set;
   }

   private static void remember(ServerPlayer player, LivingEntity target) {
      Deque<Integer> recent = RECENT.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
      recent.remove(target.getId());
      recent.addFirst(target.getId());

      while (recent.size() > 2) {
         recent.removeLast();
      }
   }

   private static void letGo(Marks.Held held, boolean alive) {
      LivingEntity target = held.target;
      unfreeze(held, alive);
      held.settling = false;
      held.landingBeat = null;
      MarkHang.end(target);
      if (held.prone && target instanceof Mob mob) {
         mob.setPose(Pose.STANDING);
         held.prone = false;
      }

      held.mark = null;
      publish(held);
   }

   private static void publish(Marks.Held held) {
      MarkState next = held.mark == null ? MarkState.NONE : MarkState.of(held.mark, held.flavour == null ? Mark.Flavour.NONE : held.flavour, held.expiry);
      if (!next.equals(held.target.getData(AwakenedAttachments.BRAWLER_MARK))) {
         held.target.setData(AwakenedAttachments.BRAWLER_MARK, next);
      }
   }

   private static void pruneLedgers(long gameTime) {
      LEDGERS.entrySet().removeIf(e -> gameTime - e.getValue().lastTouch > 240L && !HELD.containsKey(e.getKey()));
   }

   public static void clearPlayer(ServerPlayer player) {
      UUID id = player.getUUID();
      List<Integer> mine = new ArrayList<>();

      for (Entry<Integer, Marks.Held> entry : HELD.entrySet()) {
         Marks.Held held = entry.getValue();
         if (id.equals(held.owner) || held.target == player) {
            mine.add(entry.getKey());
         }
      }

      for (Integer key : mine) {
         Marks.Held held = HELD.remove(key);
         if (held != null) {
            letGo(held, held.target.isAlive());
         }
      }

      TRANSITIONS.remove(id);
      RECENT.remove(id);
   }

   public static void clearAll() {
      for (Marks.Held held : HELD.values()) {
         letGo(held, held.target.isAlive());
      }

      HELD.clear();
      LEDGERS.clear();
      TRANSITIONS.clear();
      RECENT.clear();
      MarkHang.clearAll();
      lastTick = -1;
   }

   private static final class Held {
      final LivingEntity target;
      UUID owner;
      Mark mark;
      Mark.Flavour flavour;
      long expiry;
      int lockLeft;
      boolean frozen;
      boolean prone;
      String createdBy;
      double launchHeight;
      boolean settling;
      double driveSpeed;
      int settleTicks;
      Consumer<ServerLevel> landingBeat;
      int age;

      Held(LivingEntity target) {
         this.target = target;
      }
   }

   private static final class Ledger {
      final int[] tier = new int[Mark.values().length];
      final long[] tierTick = new long[Mark.values().length];
      long lastTouch;
   }

   private static final class Transitions {
      final Set<String> keys = new LinkedHashSet<>();
   }
}
