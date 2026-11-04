package com.devilfruits.core.util;

import com.devilfruits.core.body.BodyShell;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.physics.StructureForces;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.world.AbilityEntity;
import com.devilfruits.core.world.DebrisEntity;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFDamageTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

public final class Targeting {
   public static boolean canHurt(ServerPlayer caster, Entity target) {
      if (!(target instanceof LivingEntity living) || target == caster || !target.isAlive() || target.isSpectator()) {
         return false;
      }

      if (target instanceof ArmorStand stand && stand.isMarker()) {
         return false;
      }

      if (target instanceof Player other && (!(Boolean)CoreConfig.AFFECT_PLAYERS.get() || other.isCreative() || !caster.canHarmPlayer(other))) {
         return false;
      }

      if (caster.isAlliedTo(living)) {
         return false;
      } else {
         if (target instanceof OwnableEntity ownable && caster.getUUID().equals(ownable.getOwnerUUID())) {
            return false;
         }

         return true;
      }
   }

   public static boolean canPush(ServerPlayer caster, Entity target) {
      if (target == caster || target.isSpectator() || !target.isAlive()) {
         return false;
      } else if (target instanceof DebrisEntity || target instanceof AbilityEntity || target instanceof Projectile) {
         return false;
      } else if (target instanceof BodyShell || target instanceof PartEntity) {
         return false;
      } else {
         return target instanceof LivingEntity ? canHurt(caster, target) : target.isPushable() || target instanceof ItemEntity || target instanceof PrimedTnt;
      }
   }

   @Nullable
   public static Entity resolve(Entity e) {
      if (e instanceof PartEntity<?> part) {
         return part.getParent() instanceof BodyShell shell ? shell.bodyOwnerEntity() : part.getParent();
      } else {
         return e instanceof BodyShell ? null : e;
      }
   }

   public static void addResolved(List<Entity> out, Entity e, Predicate<Entity> filter) {
      Entity target = resolve(e);
      if (target != null && !out.contains(target) && filter.test(target)) {
         out.add(target);
      }
   }

   public static List<Entity> inBox(ServerLevel level, @Nullable Entity except, AABB box, Predicate<Entity> filter) {
      List<Entity> out = new ArrayList<>();

      for (Entity e : level.getEntities(except, box)) {
         addResolved(out, e, filter);
      }

      return out;
   }

   public static List<Entity> inRadius(ServerLevel level, Vec3 center, double radius, Predicate<Entity> filter) {
      double r2 = radius * radius;
      List<Entity> out = new ArrayList<>();

      for (Entity e : level.getEntities(
         (Entity)null, new AABB(center, center).inflate(radius), ex -> ex.getBoundingBox().getCenter().distanceToSqr(center) <= r2
      )) {
         addResolved(out, e, filter);
      }

      return out;
   }

   public static List<Entity> inCapsule(ServerLevel level, ServerPlayer caster, Vec3 a, Vec3 b, double radius) {
      List<Entity> out = new ArrayList<>();
      capsuleHits(level.getEntities((Entity)null, new AABB(a, b).inflate(radius + 1.0)), caster, a, b, radius, out);
      return out;
   }

   public static void capsuleHits(List<Entity> candidates, ServerPlayer caster, Vec3 a, Vec3 b, double radius, List<Entity> out) {
      Vec3 ab = b.subtract(a);
      double len2 = Math.max(1.0E-6, ab.lengthSqr());

      for (Entity e : candidates) {
         Vec3 c = e.getBoundingBox().getCenter();
         double t = Math.max(0.0, Math.min(1.0, c.subtract(a).dot(ab) / len2));
         Vec3 closest = a.add(ab.scale(t));
         double reach = radius + (double)e.getBbWidth() * 0.5;
         if (closest.distanceToSqr(c) <= reach * reach) {
            addResolved(out, e, x -> canHurt(caster, x));
         }
      }
   }

   public static List<Entity> inCone(ServerLevel level, Vec3 apex, Vec3 dir, double length, double halfAngle, Predicate<Entity> filter) {
      Vec3 d = dir.normalize();
      double cos = Math.cos(halfAngle);
      AABB box = new AABB(apex, apex.add(d.scale(length))).inflate(length * Math.tan(Math.min(halfAngle, 1.2)) + 1.0);
      List<Entity> out = new ArrayList<>();

      for (Entity e : level.getEntities((Entity)null, box, ex -> {
         Vec3 to = ex.getBoundingBox().getCenter().subtract(apex);
         double dist = to.length();
         if (dist < 1.2) {
            return true;
         } else {
            double along = to.dot(d);
            return along > 0.0 && along <= length + (double)ex.getBbWidth() && along / dist >= cos;
         }
      })) {
         addResolved(out, e, filter);
      }

      return out;
   }

   public static boolean hurt(ServerPlayer caster, Entity target, float amount, ResourceKey<DamageType> type) {
      if (canHurt(caster, target) && !(amount <= 0.0F)) {
         boolean hurt = target.hurt(DFDamageTypes.source(caster.level(), type, caster), amount);
         if (hurt) {
            PlayerFruitData data = DFAttachments.data(caster);
            data.setLastCombatTick(caster.level().getGameTime());
            Mastery.addXp(caster, data, ((Double)CoreConfig.XP_PER_HIT.get()).floatValue());
         }

         return hurt;
      } else {
         return false;
      }
   }

   public static void launch(Entity target, Vec3 velocity) {
      Vec3 v = velocity;
      if (target instanceof LivingEntity living) {
         double resist = living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
         v = velocity.scale(Math.max(0.0, 1.0 - resist));
      }

      target.setDeltaMovement(v);
      target.hasImpulse = true;
      if (target instanceof ServerPlayer player) {
         player.connection.send(new ClientboundSetEntityMotionPacket(player));
      } else {
         target.hurtMarked = true;
      }
   }

   public static void moveSelf(Entity self, Vec3 velocity) {
      StructureForces.takeoff(self, velocity);
      self.setDeltaMovement(velocity);
      self.hasImpulse = true;
      if (self instanceof ServerPlayer player) {
         player.connection.send(new ClientboundSetEntityMotionPacket(player));
      } else {
         self.hurtMarked = true;
      }
   }

   public static void push(Entity target, Vec3 impulse) {
      launch(target, target.getDeltaMovement().add(impulse));
   }

   private Targeting() {
   }
}
