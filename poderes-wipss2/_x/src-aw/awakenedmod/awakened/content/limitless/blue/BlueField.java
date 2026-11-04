package com.awakenedmod.awakened.content.limitless.blue;

import com.awakenedmod.awakened.content.limitless.LimitlessFx;
import com.awakenedmod.awakened.content.limitless.ShotHold;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.HitStops;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.TickFreeze;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class BlueField {
   static final int LENGTH = 43;
   private final BlueOrbEntity orb;
   private final Vec3 center;
   private final Vec3 stash;
   private final double radius;
   private final float crush;
   private final float slam;
   private final String balanceKey;
   private final Map<Integer, LivingEntity> bodies = new LinkedHashMap<>();
   private final Map<Integer, Entity> loose = new LinkedHashMap<>();
   private final Map<Integer, ShotHold> arrows = new LinkedHashMap<>();
   private final Set<Integer> clumped = new HashSet<>();
   private final HitStops stops = new HitStops();
   private boolean rewarded;
   private int ticks;
   private int closing;

   BlueField(ServerPlayer player, BlueOrbEntity orb, String balanceKey) {
      this.orb = orb;
      this.center = orb.position();
      this.stash = this.center.subtract(0.0, 0.85, 0.0);
      this.radius = pullRadius(player);
      this.crush = MasteryScaling.scaledDamage(1.0F, player, balanceKey);
      this.slam = MasteryScaling.scaledDamage(3.0F, player, balanceKey);
      this.balanceKey = balanceKey;
   }

   static double pullRadius(ServerPlayer player) {
      return BlueAbility.PULL_RADIUS.after(PowerService.data(player).masteryLevel(), MasteryScaling.abilityPower(player));
   }

   static boolean catches(ServerPlayer player, LivingEntity e) {
      if (e == player || e.isSpectator()) {
         return false;
      } else {
         if (e instanceof Player p && p.isCreative()) {
            return false;
         }

         return Harm.allowed(player, e) && !Foes.fightsFor(player, e);
      }
   }

   int ticksLeft() {
      return this.closing > 0 ? this.closing : 43 - this.ticks;
   }

   boolean tick(ServerPlayer player, ServerLevel level) {
      this.ticks++;
      this.stops.tick();
      if (!this.orb.isAlive() || this.orb.level() != level) {
         this.end(level);
         return false;
      } else if (this.closing > 0) {
         if (--this.closing <= 0) {
            this.end(level);
            return false;
         } else {
            return true;
         }
      } else if (this.ticks >= 40) {
         this.implode(player, level);
         return true;
      } else {
         if (this.ticks >= 2) {
            this.catchBodies(player);
            this.steerBodies(player, level);
            this.pullLoose(player, level);
            if (this.ticks % 10 == 0) {
               this.crushBodies(player, level);
            }

            this.reward(player);
         }

         return true;
      }
   }

   private void catchBodies(ServerPlayer player) {
      for (LivingEntity e : AimUtil.livingAround(player, this.center, this.radius + 2.0)) {
         if (!this.bodies.containsKey(e.getId()) && catches(player, e) && this.reach(e) <= this.radius) {
            this.bodies.put(e.getId(), e);
         }
      }

      this.bodies.values().removeIf(ex -> !ex.isAlive() || ex.isRemoved() || this.reach(ex) > this.radius + 3.0);
   }

   private static int snapAt() {
      return 7;
   }

   private void steerBodies(ServerPlayer player, ServerLevel level) {
      if (this.ticks == snapAt()) {
         LimitlessFx.blueSnap(level, this.center);
      }

      int n = this.bodies.size();
      int i = 0;

      for (LivingEntity e : this.bodies.values()) {
         Vec3 slot = this.slot(i++, n, e);
         if (!TickFreeze.isFrozen(e)) {
            if (this.ticks < snapAt()) {
               this.lean(e);
            } else {
               Vec3 gap = slot.subtract(e.position());
               double lift = Math.min(2.0, gap.horizontalDistance() * 0.3);
               Vec3 want = gap.add(0.0, lift, 0.0).scale(0.5);
               if (want.length() > 2.6) {
                  want = want.normalize().scale(2.6);
               }

               if (gap.length() < 1.6 && this.clumped.add(e.getId())) {
                  this.arrived(player, level, e);
               }

               if (e instanceof Player) {
                  Player p = (Player)e;
                  if (this.ticks % 3 == 0) {
                     Harm.setMotion(player, p, want.scale(0.55));
                  }
               } else {
                  Vec3 v = e.getDeltaMovement().scale(0.2).add(want.scale(0.8)).add(0.0, e.getGravity(), 0.0);
                  e.setDeltaMovement(v);
                  e.setOnGround(false);
                  e.hurtMarked = true;
                  e.resetFallDistance();
                  if (e instanceof Mob) {
                     Mob mob = (Mob)e;
                     if (mob.isNoAi()) {
                        mob.move(MoverType.SELF, v);
                     }
                  }
               }
            }
         }
      }
   }

   private void lean(LivingEntity e) {
      if (!(e instanceof Player)) {
         Vec3 to = this.center.subtract(e.position());
         Vec3 flat = new Vec3(to.x, 0.0, to.z);
         if (!(flat.lengthSqr() < 1.0E-4)) {
            e.setDeltaMovement(e.getDeltaMovement().add(flat.normalize().scale(0.07)));
            e.hurtMarked = true;
         }
      }
   }

   private void arrived(ServerPlayer player, ServerLevel level, LivingEntity e) {
      if (!(e instanceof Player) && !(e.getDeltaMovement().length() < 0.45)) {
         Dmg.hurtThrough(e, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), this.slam);
         this.stops.hold(e, 2, null);
         LimitlessFx.blueSlam(level, e.position().add(0.0, (double)e.getBbHeight() * 0.5, 0.0));
      }
   }

   private Vec3 slot(int i, int n, LivingEntity e) {
      double spin = (double)i * Math.PI * 2.0 / (double)Math.max(1, n) + (double)this.ticks * 0.04;
      double r = n <= 1 ? 0.0 : 0.7;
      return this.center.add(Math.cos(spin) * r, -(0.5 + (double)e.getBbHeight()), Math.sin(spin) * r);
   }

   private void crushBodies(ServerPlayer player, ServerLevel level) {
      for (LivingEntity e : this.bodies.values()) {
         if (this.reach(e) <= 2.0) {
            Dmg.hurtTick(e, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), this.crush);
         }
      }
   }

   private void reward(ServerPlayer player) {
      if (!this.rewarded) {
         List<LivingEntity> crowd = new ArrayList<>();

         for (int id : this.clumped) {
            LivingEntity e = this.bodies.get(id);
            if (e != null && !(e instanceof Player) && Foes.counts(player, e)) {
               crowd.add(e);
            }
         }

         if (crowd.size() >= 3) {
            this.rewarded = true;
            MasterySystem.award(player, MasteryReason.HIT, crowd);
         }
      }
   }

   private double reach(Entity e) {
      return AimUtil.reachToBox(e, this.center);
   }

   private void pullLoose(ServerPlayer player, ServerLevel level) {
      AABB box = new AABB(this.center, this.center).inflate(this.radius);

      for (Entity e : level.getEntities((Entity)null, box, ex -> looseThing(player, ex))) {
         int id = e.getId();
         if (!this.loose.containsKey(id) && !this.arrows.containsKey(id) && !(e.distanceToSqr(this.center) > this.radius * this.radius)) {
            if (e instanceof AbstractArrow) {
               AbstractArrow arrow = (AbstractArrow)e;
               if (arrow.getDeltaMovement().lengthSqr() > 0.001) {
                  this.arrows.put(id, new ShotHold(arrow, arrow.getDeltaMovement()));
               }
            } else {
               this.loose.put(id, e);
            }
         }
      }

      this.loose.values().removeIf(ex -> !ex.isAlive());

      for (Entity ex : this.loose.values()) {
         Vec3 at = this.pin(ex, (double)ex.getBbHeight() * 0.5);
         if (at != null) {
            ex.setPos(at.x, at.y, at.z);
            ex.setDeltaMovement(Vec3.ZERO);
            if (ex instanceof ItemEntity item) {
               item.setPickUpDelay(10);
            }
         }

         ex.hurtMarked = true;
      }

      this.arrows.values().removeIf(hx -> !hx.shot().isAlive());

      for (ShotHold h : this.arrows.values()) {
         Vec3 at = this.pin(h.shot(), 0.0);
         if (at != null) {
            h.hold(at);
         } else {
            ShotHold.push(h.shot());
         }
      }
   }

   private static boolean looseThing(ServerPlayer player, Entity e) {
      if (!e.isAlive()) {
         return false;
      } else if (e instanceof AbstractArrow arrow) {
         Entity owner = arrow.getOwner();
         return owner == null || owner != player && !Foes.fightsFor(player, owner) && Harm.allowed(player, owner);
      } else {
         return e instanceof ItemEntity || e instanceof ExperienceOrb;
      }
   }

   private Vec3 pin(Entity e, double halfHeight) {
      Vec3 spot = this.stash.add(scatter(e.getId(), 0), scatter(e.getId(), 1) * 0.5, scatter(e.getId(), 2)).subtract(0.0, halfHeight, 0.0);
      Vec3 gap = spot.subtract(e.position());
      double d = gap.length();
      if (d <= 0.75) {
         return spot;
      } else {
         e.setDeltaMovement(gap.scale(Math.min(0.6, d * 0.5) / d).add(0.0, e.getGravity(), 0.0));
         return null;
      }
   }

   private static double scatter(int id, int axis) {
      int h = id * -1640531535 ^ axis * -2048144789;
      h ^= h >>> 15;
      return ((double)(h & 65535) / 65535.0 - 0.5) * 0.7;
   }

   private void implode(ServerPlayer player, ServerLevel level) {
      List<LivingEntity> held = new ArrayList<>();

      for (LivingEntity e : this.bodies.values()) {
         if (e.isAlive() && !e.isRemoved() && this.reach(e) <= 3.0) {
            held.add(e);
         }
      }

      float bonus = Math.min(3.0F, 1.0F * (float)Math.max(0, held.size() - 1));
      float damage = MasteryScaling.scaledDamage(3.0F + bonus, player, this.balanceKey);

      for (LivingEntity ex : held) {
         if (!(ex instanceof Player)) {
            ex.setDeltaMovement(Vec3.ZERO);
            ex.hurtMarked = true;
         }

         Dmg.hurtThrough(ex, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), damage);
         this.stops.hold(ex, 3, null);
      }

      LimitlessFx.blueImploded(level, this.center);
      this.closing = 3;
   }

   boolean touches(ServerLevel level, Vec3 at) {
      if (this.closing <= 0 && this.orb.level() == level) {
         if (at.distanceTo(this.center) <= 2.5) {
            return true;
         } else {
            for (LivingEntity e : this.bodies.values()) {
               if (AimUtil.reachToBox(e, at) <= 1.5) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   double orbAlong(ServerLevel level, Vec3 from, Vec3 dir, double max) {
      if (this.closing <= 0 && this.orb.level() == level) {
         Vec3 to = this.center.subtract(from);
         double along = to.dot(dir);
         double miss = to.lengthSqr() - along * along;
         double r2 = 1.44;
         return !(along < 0.0) && !(miss > r2) ? Math.min(max, Math.max(0.0, along - Math.sqrt(r2 - miss))) : max;
      } else {
         return max;
      }
   }

   BluePop pop(ServerLevel level) {
      List<LivingEntity> held = new ArrayList<>();

      for (LivingEntity e : this.bodies.values()) {
         if (e.isAlive() && !e.isRemoved() && e.level() == level) {
            held.add(e);
         }
      }

      this.orb.pop();
      this.end(level);
      LimitlessFx.bluePopped(level, this.center);
      return new BluePop(this.center, List.copyOf(held));
   }

   void end(ServerLevel level) {
      for (Entity e : this.loose.values()) {
         if (e.isAlive()) {
            e.setDeltaMovement(Vec3.ZERO);
            e.hurtMarked = true;
         }
      }

      for (ShotHold h : this.arrows.values()) {
         if (h.shot().isAlive()) {
            h.drop(level);
         }
      }

      this.loose.clear();
      this.arrows.clear();
      this.bodies.clear();
      this.stops.releaseAll();
      this.orb.letGo();
   }
}
