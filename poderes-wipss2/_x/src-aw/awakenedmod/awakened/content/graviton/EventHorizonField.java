package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Bosses;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.TickFreeze;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class EventHorizonField {
   static final double PULL_RADIUS = 16.0;
   private static final double INWARD_RIM = 0.1;
   private static final double INWARD_CORE = 0.42;
   private static final double SWIRL_RIM = 0.2;
   private static final double SWIRL_CORE = 0.5;
   private static final double PLANE_EASE = 0.14;
   private static final double PLANE_MAX = 0.38;
   private static final double KEEP = 0.35;
   private static final double COLLAPSE_BOOST = 1.6;
   private static final double PLAYER_SCALE = 0.55;
   private static final int PLAYER_PULSE = 3;
   private static final double PLAYER_LIFT = 0.02;
   private static final double LOOSE_SPEED_RIM = 0.25;
   private static final double LOOSE_SPEED_CORE = 0.6;
   private static final double STASH_RADIUS = 1.5;
   static final double INNER_RADIUS = 4.0;
   static final int CRUSH_INTERVAL = 10;
   static final float CRUSH = 1.5F;
   static final float BOSS_CRUSH = 2.5F;
   private static final double ERASE_RADIUS = 1.6;
   static final float ERASE_MAX_HEALTH = 30.0F;
   static final double COLLAPSE_RADIUS = 8.0;
   static final float COLLAPSE = 12.0F;
   static final float BOSS_COLLAPSE = 40.0F;
   private static final double THROW = 1.7;
   private static final double THROW_LIFT = 0.65;
   private static final double THROW_PLAYER = 0.65;
   private static final double STASH_POP = 0.18;
   private static final Set<EventHorizonField> LIVE = ConcurrentHashMap.newKeySet();
   private final ServerLevel level;
   private final EventHorizonEntity hole;
   private final Vec3 center;
   private final Vec3 normal;
   private final float crush;
   private final float bossCrush;
   private final float collapseDamage;
   private final float bossCollapse;
   private final Map<UUID, Entity> stash = new LinkedHashMap<>();
   private final Set<LivingEntity> hurt = new HashSet<>();
   private final Set<UUID> killed = new HashSet<>();
   private final Set<UUID> eraseTried = new HashSet<>();
   private int age;
   private boolean featReported;
   private boolean finished;

   EventHorizonField(ServerLevel level, EventHorizonEntity hole, Vec3 center, float crush, float bossCrush, float collapseDamage, float bossCollapse) {
      this.level = level;
      this.hole = hole;
      this.center = center;
      this.normal = hole.diskNormal();
      this.crush = crush;
      this.bossCrush = bossCrush;
      this.collapseDamage = collapseDamage;
      this.bossCollapse = bossCollapse;
      LIVE.add(this);
   }

   ServerLevel level() {
      return this.level;
   }

   boolean tick(ServerPlayer caster) {
      if (this.finished) {
         return false;
      } else if (this.hole.isRemoved()) {
         this.release();
         return false;
      } else {
         this.age++;
         EventHorizonVisuals.Phase p = this.hole.phase();
         if (p.flashing()) {
            this.collapse(caster);
            this.finish();
            return false;
         } else {
            double strength = p.holding() ? (double)p.open() : 1.6;
            boolean crushing = p.open() >= 1.0F && this.age % 10 == 0;
            this.pullBodies(caster, strength, crushing);
            this.pullLoose(strength);
            this.holdStash();
            return true;
         }
      }
   }

   private List<LivingEntity> bodiesIn(ServerPlayer caster, double radius) {
      List<LivingEntity> found = this.level
         .getEntitiesOfClass(
            LivingEntity.class,
            new AABB(this.center, this.center).inflate(radius + 4.0),
            e -> e != caster && !e.isPassengerOfSameVehicle(caster) && e.isAlive() && eligible(e) && this.reach(e) <= radius
         );
      return Harm.hostilesOnly(caster, found);
   }

   private static boolean eligible(LivingEntity e) {
      if (e instanceof ArmorStand) {
         return false;
      } else {
         return !(e instanceof Player p) ? true : !p.isSpectator() && !p.isCreative();
      }
   }

   private double reach(Entity e) {
      return AimUtil.reachToBox(e, this.center);
   }

   private static boolean erasable(LivingEntity e) {
      return !(e instanceof Player) && !Bosses.is(e) && !e.isInvulnerable() && e.getMaxHealth() <= 30.0F;
   }

   private void pullBodies(ServerPlayer caster, double strength, boolean crushing) {
      List<LivingEntity> crushedNow = new ArrayList<>();

      for (LivingEntity e : this.bodiesIn(caster, 16.0)) {
         if (!TickFreeze.isFrozen(e)) {
            Vec3 mid = e.getBoundingBox().getCenter();
            double closeness = 1.0 - Mth.clamp(this.reach(e) / 16.0, 0.0, 1.0);
            if (e instanceof Player) {
               Player player = (Player)e;
               if (this.age % 3 == 0) {
                  this.tugPlayer(player, mid, closeness, strength);
               }
            } else {
               this.steer(e, mid, closeness, strength);
            }

            e.resetFallDistance();
            if (erasable(e) && mid.distanceTo(this.center) <= 1.6 && this.eraseTried.add(e.getUUID())) {
               this.erase(caster, e);
            } else if (crushing && this.reach(e) <= 4.0 && this.crush(caster, e)) {
               crushedNow.add(e);
            }
         }
      }

      if (!crushedNow.isEmpty()) {
         MasterySystem.award(caster, MasteryReason.HIT, crushedNow);
      }
   }

   private Vec3 spiral(Vec3 mid, double closeness, double inwardRim, double inwardCore, double swirlScale) {
      Vec3 r = mid.subtract(this.center);
      double h = r.dot(this.normal);
      Vec3 inPlane = r.subtract(this.normal.scale(h));
      double rho = inPlane.length();
      double ease = closeness * closeness;
      double inward = inwardRim + (inwardCore - inwardRim) * ease;
      double swirl = (0.2 + 0.3 * closeness) * swirlScale;
      Vec3 v = this.normal.scale(Mth.clamp(-h * 0.14, -0.38, 0.38));
      if (rho > 0.001) {
         Vec3 radial = inPlane.scale(1.0 / rho);
         double fade = Mth.clamp((rho - 0.8) / 3.2, 0.0, 1.0);
         v = v.add(radial.scale(-inward)).add(this.normal.cross(radial).scale(swirl * fade));
      }

      return v;
   }

   private void steer(LivingEntity e, Vec3 mid, double closeness, double strength) {
      Vec3 want = this.spiral(mid, closeness, 0.1, 0.42, 1.0).scale(strength);
      Vec3 v = e.getDeltaMovement().scale(0.35).add(want.scale(0.65)).add(0.0, e.getGravity(), 0.0);
      e.setDeltaMovement(v);
      e.hurtMarked = true;
      if (e instanceof Mob mob && mob.isNoAi()) {
         mob.move(MoverType.SELF, v);
      }
   }

   private void tugPlayer(Player player, Vec3 mid, double closeness, double strength) {
      Vec3 want = this.spiral(mid, closeness, 0.1, 0.42, 1.0).scale(strength * 0.55);
      player.setDeltaMovement(want.x, 0.02, want.z);
      player.hurtMarked = true;
   }

   private boolean crush(ServerPlayer caster, LivingEntity e) {
      boolean boss = Bosses.is(e);
      DamageSource src = Dmg.by(caster, boss ? DamageTypes.MAGIC : DamageTypes.PLAYER_ATTACK);
      return this.strike(caster, e, src, boss ? this.bossCrush : this.crush);
   }

   private void erase(ServerPlayer caster, LivingEntity e) {
      float lethal = e.getHealth() + e.getAbsorptionAmount() + 100.0F;
      if (this.strike(caster, e, Dmg.by(caster, DamageTypes.GENERIC_KILL), lethal) && e.isDeadOrDying()) {
         e.deathTime = Math.max(e.deathTime, 19);
         e.setDeltaMovement(Vec3.ZERO);
         e.hurtMarked = true;
      }
   }

   private boolean strike(ServerPlayer caster, LivingEntity e, DamageSource src, float amount) {
      boolean landed = Dmg.hurtThrough(e, this.level, src, amount);
      if (landed) {
         this.hurt.add(e);
         if (Foes.defeatedBy(e, caster)) {
            this.killed.add(e.getUUID());
            if (!this.featReported && this.killed.size() >= 3) {
               this.featReported = true;
               QuestSystem.feat(caster, "graviton_collapse");
            }
         }
      }

      return landed;
   }

   private void pullLoose(double strength) {
      for (Entity e : this.level
         .getEntitiesOfClass(
            Entity.class,
            new AABB(this.center, this.center).inflate(16.0),
            ex -> (ex instanceof ItemEntity || ex instanceof ExperienceOrb)
                  && ex.isAlive()
                  && !this.stash.containsKey(ex.getUUID())
                  && ex.distanceToSqr(this.center) <= 256.0
         )) {
         Vec3 to = this.center.subtract(e.position());
         double d = to.length();
         if (d <= 1.5) {
            this.stash.put(e.getUUID(), e);
         } else {
            double closeness = 1.0 - Mth.clamp(d / 16.0, 0.0, 1.0);
            double speed = (0.25 + 0.35 * closeness) * strength;
            Vec3 v = this.spiral(e.position(), closeness, 0.0, 0.0, 0.6).scale(strength).add(to.scale(Math.min(speed, d) / d));
            e.setDeltaMovement(e.getDeltaMovement().scale(0.2).add(v).add(0.0, e.getGravity(), 0.0));
            e.hurtMarked = true;
         }
      }
   }

   private void holdStash() {
      Iterator<Entity> it = this.stash.values().iterator();

      while (it.hasNext()) {
         Entity e = it.next();
         if (e.isAlive() && e.level() == this.level) {
            e.setPos(this.center.x, this.center.y - (double)e.getBbHeight() * 0.5, this.center.z);
            e.setDeltaMovement(Vec3.ZERO);
            e.hurtMarked = true;
            if (e instanceof ItemEntity item) {
               item.setPickUpDelay(10);
            }
         } else {
            it.remove();
         }
      }
   }

   private void releaseStash(boolean pop) {
      for (Entity e : this.stash.values()) {
         if (e.isAlive()) {
            if (pop) {
               double a = this.level.random.nextDouble() * Math.PI * 2.0;
               double s = 0.18 * (0.5 + this.level.random.nextDouble());
               e.setDeltaMovement(Math.cos(a) * s, 0.2, Math.sin(a) * s);
            } else {
               e.setDeltaMovement(Vec3.ZERO);
            }

            e.hurtMarked = true;
            if (e instanceof ItemEntity item) {
               item.setPickUpDelay(10);
            }
         }
      }

      this.stash.clear();
   }

   private void collapse(ServerPlayer caster) {
      for (LivingEntity e : this.bodiesIn(caster, 16.0)) {
         if (this.reach(e) <= 8.0) {
            boolean boss = Bosses.is(e);
            DamageSource src = Dmg.by(caster, boss ? DamageTypes.MAGIC : DamageTypes.PLAYER_ATTACK);
            this.strike(caster, e, src, boss ? this.bossCollapse : this.collapseDamage);
         }

         if (e.isAlive() && !TickFreeze.isFrozen(e)) {
            this.throwOut(e);
         }
      }

      this.releaseStash(true);
      LivingEntity victim = null;
      boolean anyKill = false;

      for (LivingEntity e : this.hurt) {
         if (Foes.counts(caster, e)) {
            victim = e;
            if (this.killed.contains(e.getUUID())) {
               anyKill = true;
               break;
            }
         }
      }

      if (victim != null) {
         MasterySystem.award(caster, anyKill ? MasteryReason.KILL : MasteryReason.HIT, victim);
      }

      Vec3 floor = this.center.subtract(0.0, (double)this.hole.groundDrop(), 0.0);
      Fx.groundShockwave(this.level, floor, 1.5, 20.8, 16, true, 16747069);
      Fx.screenShake(this.level, this.center, 32.0, 14, 16747069, true, true);
   }

   private void throwOut(LivingEntity e) {
      Vec3 away = e.getBoundingBox().getCenter().subtract(this.center);
      away = new Vec3(away.x, 0.0, away.z);
      if (away.lengthSqr() < 1.0E-4) {
         double a = this.level.random.nextDouble() * Math.PI * 2.0;
         away = new Vec3(Math.cos(a), 0.0, Math.sin(a));
      }

      double scale = e instanceof Player ? 0.65 : 1.0;
      away = away.normalize().scale(1.7 * scale);
      e.setDeltaMovement(away.x, 0.65 * scale, away.z);
      e.hurtMarked = true;
      e.resetFallDistance();
   }

   private void finish() {
      this.finished = true;
      LIVE.remove(this);
   }

   void release() {
      if (!this.finished) {
         if (!this.hole.isRemoved()) {
            this.hole.collapseNow();
         }

         this.releaseStash(false);
         this.finish();
      }
   }

   static void releaseAll() {
      for (EventHorizonField field : new ArrayList<>(LIVE)) {
         field.release();
      }

      LIVE.clear();
   }
}
