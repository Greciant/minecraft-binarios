package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.core.Attrs;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class WeightlessField {
   public static final String DROP_FEAT = "graviton_drop";
   public static final ResourceLocation ZERO_G_ID = AwId.of("graviton_weightless");
   static final int FIELD_TICKS = 60;
   static final int TELL_TICKS = 10;
   private static final double FLOAT_AMOUNT = -1.0;
   private static final double TELL_AMOUNT = -0.999;
   public static final int NOT_FLOATING = 0;
   public static final int FLOATING_NOW = 1;
   public static final int ABOUT_TO_DROP = 2;
   private static final int LATE_CATCH_TICKS = 20;
   static final int PLAYER_TICKS = 30;
   private static final double BELOW = 2.0;
   private static final double ABOVE = 5.0;
   static final Lab.Value RISE = Lab.SERVER.def("graviton.weightless.rise", 5.0, 1.0, 12.0, 0.25);
   private static final double RISE_JITTER = 0.5;
   private static final double RISE_EASE = 0.2;
   private static final Lab.Value RISE_MAX_UP = Lab.SERVER.def("graviton.weightless.rise_speed", 0.35, 0.05, 1.0, 0.01);
   private static final double RISE_MAX_DOWN = -0.06;
   private static final double DRIFT_KEEP = 0.6;
   private static final float SPIN_DEG = 2.5F;
   private static final double PLAYER_LIFT_PER_BLOCK = 0.02 / (1.0 - Math.pow(0.98, 30.0));
   private static final double DROP_VY = -1.1;
   private static final double DROP_FALL_CAP = 12.0;
   private static final int DROP_MAX_TICKS = 60;
   private static final double PROJECTILE_DAMP = 0.85;
   private static final Set<UUID> FLOATING = ConcurrentHashMap.newKeySet();
   private static final List<WeightlessField> LIVE = new ArrayList<>();
   private final ServerLevel level;
   private final Vec3 center;
   private final double radius;
   private final double rise;
   private final float dropDamage;
   private final float dropPerBlock;
   private final String loopKey;
   private final List<WeightlessField.Body> bodies = new ArrayList<>();
   private final List<WeightlessField.Drop> drops = new ArrayList<>();
   private final Set<UUID> seen = new HashSet<>();
   private int age;

   WeightlessField(ServerLevel level, Vec3 center, double radius, double rise, float dropDamage, float dropPerBlock, int serial) {
      this.level = level;
      this.center = center;
      this.radius = radius;
      this.rise = rise;
      this.dropDamage = dropDamage;
      this.dropPerBlock = dropPerBlock;
      this.loopKey = "graviton_weightless_" + serial;
      LIVE.add(this);
   }

   static boolean crowded(ServerLevel level, Vec3 center, double radius) {
      for (WeightlessField f : LIVE) {
         if (f.level == level && f.age <= 60 && f.center.distanceToSqr(center) < 4.0 * radius * radius) {
            return true;
         }
      }

      return false;
   }

   public static int zeroG(LivingEntity e) {
      AttributeInstance gravity = e.getAttribute(Attributes.GRAVITY);
      AttributeModifier m = gravity == null ? null : gravity.getModifier(ZERO_G_ID);
      if (m == null) {
         return 0;
      } else {
         return m.amount() > -0.9999 ? 2 : 1;
      }
   }

   static boolean isFloating(Entity entity) {
      return entity != null && FLOATING.contains(entity.getUUID());
   }

   static void forgetAll() {
      FLOATING.clear();
      LIVE.clear();
   }

   ServerLevel level() {
      return this.level;
   }

   boolean tick(ServerPlayer caster) {
      this.age++;
      if (this.age <= 60) {
         if (this.age <= 40) {
            this.catchNew(caster);
         }

         this.driftProjectiles(caster);
         WeightlessFx.fieldTick(this.level, this.center, this.radius, this.age, 60, 10, this.loopKey);
         if (this.age == 60) {
            WeightlessFx.dropBeat(this.level, this.center);
         }
      }

      this.tickBodies();
      this.tickDrops(caster);
      boolean open = this.age <= 60 || !this.bodies.isEmpty() || !this.drops.isEmpty();
      if (!open) {
         LIVE.remove(this);
      }

      return open;
   }

   private AABB zone() {
      return new AABB(
         this.center.x - this.radius,
         this.center.y - 2.0,
         this.center.z - this.radius,
         this.center.x + this.radius,
         this.center.y + 5.0,
         this.center.z + this.radius
      );
   }

   private boolean inside(Entity e) {
      double dx = e.getX() - this.center.x;
      double dz = e.getZ() - this.center.z;
      return dx * dx + dz * dz <= this.radius * this.radius;
   }

   private void catchNew(ServerPlayer caster) {
      List<LivingEntity> candidates = this.level
         .getEntitiesOfClass(
            LivingEntity.class,
            this.zone(),
            ex -> ex.isAlive()
                  && ex != caster
                  && !ex.isPassengerOfSameVehicle(caster)
                  && !this.seen.contains(ex.getUUID())
                  && !FLOATING.contains(ex.getUUID())
                  && this.inside(ex)
                  && eligible(ex)
         );

      for (LivingEntity e : Harm.hostilesOnly(caster, candidates)) {
         if (Harm.controls(caster, e)) {
            this.seen.add(e.getUUID());
            int end = e instanceof Player ? Math.min(60, this.age + 30) : 60;
            double target = e.getY() + this.rise + (this.level.random.nextDouble() * 2.0 - 1.0) * 0.5;
            float spin = this.level.random.nextBoolean() ? 2.5F : -2.5F;
            this.bodies.add(new WeightlessField.Body(e, target, end, spin));
            FLOATING.add(e.getUUID());
            Attrs.set(e, Attributes.GRAVITY, ZERO_G_ID, -1.0, Operation.ADD_MULTIPLIED_TOTAL);
            e.resetFallDistance();
            if (e instanceof Player) {
               Vec3 v = e.getDeltaMovement();
               e.setDeltaMovement(v.x * 0.3, this.rise * PLAYER_LIFT_PER_BLOCK, v.z * 0.3);
               e.hurtMarked = true;
            }

            disarm(e);
            WeightlessFx.lifted(this.level, e);
         }
      }
   }

   private static boolean eligible(LivingEntity e) {
      if (e instanceof ArmorStand) {
         return false;
      } else {
         return !(e instanceof Player p) ? true : !p.isSpectator() && !p.isCreative();
      }
   }

   private void tickBodies() {
      Iterator<WeightlessField.Body> it = this.bodies.iterator();

      while (it.hasNext()) {
         WeightlessField.Body body = it.next();
         LivingEntity e = body.entity;
         if (e.isAlive() && !e.isRemoved() && e.level() == this.level) {
            e.resetFallDistance();
            if (this.age >= body.end) {
               restore(e);
               it.remove();
               this.drop(e);
            } else {
               if (this.age == body.end - 10) {
                  Attrs.set(e, Attributes.GRAVITY, ZERO_G_ID, -0.999, Operation.ADD_MULTIPLIED_TOTAL);
               }

               if (!(e instanceof Player)) {
                  this.steer(e, body);
                  disarm(e);
               }
            }
         } else {
            restore(e);
            it.remove();
         }
      }
   }

   private void steer(LivingEntity e, WeightlessField.Body body) {
      Vec3 v = e.getDeltaMovement();
      double bob = Math.sin((double)(this.age + e.getId()) * 0.18) * 0.012;
      double vy = Mth.clamp((body.targetY - e.getY()) * 0.2, -0.06, RISE_MAX_UP.get()) + bob;
      double keep = e.hurtTime > 0 ? 1.0 : 0.6;
      e.setDeltaMovement(v.x * keep, vy, v.z * keep);
      e.hurtMarked = true;
      float yaw = e.getYRot() + body.spin;
      e.setYRot(yaw);
      e.setYBodyRot(yaw);
      e.setYHeadRot(yaw);
   }

   private static void disarm(LivingEntity e) {
      if (e instanceof Mob mob) {
         mob.setTarget(null);
         mob.setAggressive(false);
         mob.getNavigation().stop();
         mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
         if (mob instanceof Creeper creeper) {
            creeper.setSwellDir(-1);
         }
      }
   }

   private void drop(LivingEntity e) {
      Vec3 v = e.getDeltaMovement();
      e.setDeltaMovement(v.x * 0.2, -1.1, v.z * 0.2);
      e.hurtMarked = true;
      e.resetFallDistance();
      this.drops.add(new WeightlessField.Drop(e, e.getY()));
      WeightlessFx.dropRush(this.level, e);
   }

   private void tickDrops(ServerPlayer caster) {
      List<LivingEntity> hurt = new ArrayList<>();
      Iterator<WeightlessField.Drop> it = this.drops.iterator();

      while (it.hasNext()) {
         WeightlessField.Drop d = it.next();
         LivingEntity e = d.entity;
         d.ticks++;
         if (e.isAlive() && !e.isRemoved() && e.level() == this.level && d.ticks <= 60) {
            e.resetFallDistance();
            if (e.isInWater() || e.isInLava()) {
               it.remove();
            } else if (d.ticks >= 2 && e.onGround()) {
               it.remove();
               double fell = Mth.clamp(d.startY - e.getY(), 0.0, 12.0);
               float amount = this.dropDamage + this.dropPerBlock * (float)fell;
               if (Dmg.hurtNoKnockback(e, this.level, Dmg.by(caster, DamageTypes.FALL), amount)) {
                  hurt.add(e);
                  if (Foes.defeatedBy(e, caster)) {
                     QuestSystem.feat(caster, "graviton_drop");
                  }
               }

               WeightlessFx.dropImpact(this.level, e, fell);
            }
         } else {
            it.remove();
         }
      }

      if (!hurt.isEmpty()) {
         MasterySystem.award(caster, MasteryReason.HIT, hurt);
      }
   }

   private void driftProjectiles(ServerPlayer caster) {
      for (Projectile p : this.level.getEntitiesOfClass(Projectile.class, this.zone(), this::inside)) {
         Entity owner = p.getOwner();
         if (owner == null || !Foes.fightsFor(caster, owner) && Harm.allowed(caster, owner)) {
            Vec3 v = p.getDeltaMovement();
            if (!(v.lengthSqr() < 1.0E-4)) {
               p.setDeltaMovement(v.scale(0.85).add(0.0, p.getGravity(), 0.0));
               p.hurtMarked = true;
               if (this.age % 3 == 0) {
                  WeightlessFx.projectileDrift(this.level, p.position());
               }
            }
         }
      }
   }

   private static void restore(LivingEntity e) {
      FLOATING.remove(e.getUUID());
      Attrs.set(e, Attributes.GRAVITY, ZERO_G_ID, 0.0, Operation.ADD_VALUE);
   }

   void release() {
      for (WeightlessField.Body body : this.bodies) {
         restore(body.entity);
         WeightlessFx.clearCracks(body.entity);
      }

      for (WeightlessField.Drop d : this.drops) {
         WeightlessFx.clearCracks(d.entity);
      }

      this.bodies.clear();
      this.drops.clear();
      LIVE.remove(this);
      WeightlessFx.stopLoop(this.level, this.loopKey);
   }

   private static final class Body {
      final LivingEntity entity;
      final double targetY;
      final int end;
      final float spin;

      Body(LivingEntity entity, double targetY, int end, float spin) {
         this.entity = entity;
         this.targetY = targetY;
         this.end = end;
         this.spin = spin;
      }
   }

   private static final class Drop {
      final LivingEntity entity;
      final double startY;
      int ticks;

      Drop(LivingEntity entity, double startY) {
         this.entity = entity;
         this.startY = startY;
      }
   }
}
