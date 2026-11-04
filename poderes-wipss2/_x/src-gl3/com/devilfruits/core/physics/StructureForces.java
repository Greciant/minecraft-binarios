package com.devilfruits.core.physics;

import com.devilfruits.core.fx.CoreFx;
import com.devilfruits.core.fx.ServerFx;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class StructureForces {
   private static final Map<UUID, StructureForces.Budget> BUDGETS = new HashMap<>();
   private static final Map<UUID, long[]> SELF_HITS = new HashMap<>();
   private static final int SAMPLES = 8;
   public static final double STRUCTURE_GRAVITY = 11.0;

   public static double strike(
      Entity caster, PhysicsStructure s, List<StructureBlock> touched, ImpactShape shape, Vec3 dir, PhysicsProfile.Impact impact, double strength
   ) {
      if ((Boolean)PhysicsConfig.IMPULSES.get() && impact != PhysicsProfile.Impact.NONE && !touched.isEmpty() && !(strength <= 0.0)) {
         double total = strength * (Double)PhysicsConfig.IMPULSE_MULTIPLIER.get();
         PhysicsStructure under = PhysicsWorld.structureUnder(caster);
         boolean self = under != null && under.id().equals(s.id());
         if (self) {
            if (!selfHitAllowed(caster)) {
               return 0.0;
            }

            total *= PhysicsConfig.SELF_HIT_FACTOR.get();
         }

         total = Math.min(total, (Double)PhysicsConfig.MAX_IMPULSE_PER_HIT.get());
         int n = Math.min(8, touched.size());
         int stride = Math.max(1, touched.size() / n);
         double weightSum = 0.0;

         for (int i = 0; i < n; i++) {
            weightSum += touched.get(Math.min(touched.size() - 1, i * stride)).power();
         }

         if (weightSum <= 0.0) {
            return 0.0;
         } else {
            Vec3 d = dir.lengthSqr() < 1.0E-8 ? new Vec3(0.0, -1.0, 0.0) : dir.normalize();
            Vec3 sum = Vec3.ZERO;
            double applied = 0.0;
            Vec3 firstPoint = null;

            for (int i = 0; i < n; i++) {
               StructureBlock b = touched.get(Math.min(touched.size() - 1, i * stride));
               Vec3 point = b.worldCenter();
               Vec3 along = direction(impact, d, point, shape.center());
               Vec3 j = along.scale(total * b.power() / weightSum);
               double got = apply(s, point, j);
               if (got > 0.0) {
                  sum = sum.add(j.normalize().scale(got));
                  applied += got;
                  if (firstPoint == null) {
                     firstPoint = point;
                  }
               }
            }

            if (self && applied > 0.0 && (Boolean)PhysicsConfig.SELF_HIT_CONSERVES_MOMENTUM.get()) {
               s.applyImpulse(caster.position(), sum.scale(-1.0));
            }

            if (applied > 0.0 && firstPoint != null) {
               impactFx((ServerLevel)caster.level(), s, touched.get(0).localPos(), firstPoint, sum, applied);
            }

            return applied;
         }
      } else {
         return 0.0;
      }
   }

   private static Vec3 direction(PhysicsProfile.Impact impact, Vec3 dir, Vec3 point, Vec3 center) {
      Vec3 radial = point.subtract(center);
      Vec3 flat = new Vec3(radial.x, 0.0, radial.z);

      return switch (impact) {
         case RADIAL -> radial.lengthSqr() < 0.04 ? dir : radial.normalize().add(dir.scale(0.25)).normalize();
         case TANGENTIAL -> {
            if (flat.lengthSqr() < 0.04) {
               yield new Vec3(0.0, 1.0, 0.0);
            } else {
               Vec3 out = flat.normalize();
               Vec3 tangent = new Vec3(-out.z, 0.0, out.x);
               yield tangent.scale(0.85).add(out.scale(-0.35)).add(0.0, 0.25, 0.0).normalize();
            }
         }
         default -> dir;
      };
   }

   public static void reaction(Entity entity, Vec3 dvPerTick, double scale) {
      if (PhysicsWorld.active() && (Boolean)PhysicsConfig.REACTIONS.get() && !entity.level().isClientSide()) {
         PhysicsStructure s = PhysicsWorld.structureUnder(entity);
         if (s != null && !(s.mass() < (Double)PhysicsConfig.MIN_MASS_FOR_REACTION.get())) {
            Vec3 j = dvPerTick.scale(-20.0 * bodyMass(entity) * (Double)PhysicsConfig.REACTION_MULTIPLIER.get() * scale);
            double got = apply(s, entity.position(), j);
            if (got > 0.0 && entity.level() instanceof ServerLevel level) {
               BlockPos local = BlockPos.containing(s.toLocal(entity.position().add(0.0, -0.3, 0.0)));
               impactFx(level, s, local, entity.position(), j, got);
            }
         }
      }
   }

   public static void takeoff(Entity entity, Vec3 newVelocity) {
      if (entity.onGround() && !entity.level().isClientSide()) {
         Vec3 dv = newVelocity.subtract(entity.getDeltaMovement());
         if (dv.lengthSqr() >= 0.0225) {
            reaction(entity, dv, 1.0);
         }
      }
   }

   public static void shake(ServerPlayer caster, PhysicsStructure s, Vec3 sway, double strength) {
      if ((Boolean)PhysicsConfig.IMPULSES.get() && !(strength <= 0.0) && !(sway.lengthSqr() < 1.0E-8)) {
         AABB box = s.worldBounds();
         Vec3 c = s.centerOfMassWorld() != null ? s.centerOfMassWorld() : box.getCenter();
         Vec3 swing = sway.normalize();
         Vec3 across = new Vec3(-swing.z, 0.0, swing.x);
         double half = Math.max(1.0, Math.max(box.getXsize(), box.getZsize()) * 0.5);
         double j = Math.min(strength * (Double)PhysicsConfig.IMPULSE_MULTIPLIER.get(), (Double)PhysicsConfig.MAX_IMPULSE_PER_HIT.get()) * 0.5;
         Vec3 a = c.add(across.scale(half));
         Vec3 b = c.subtract(across.scale(half));
         double got = apply(s, a, new Vec3(0.0, j, 0.0).add(swing.scale(j * 0.5)));
         got += apply(s, b, new Vec3(0.0, -j, 0.0).subtract(swing.scale(j * 0.3)));
         got += apply(s, c, swing.scale(j * 0.4));
         if (got > 0.0) {
            impactFx(caster.serverLevel(), s, BlockPos.containing(s.toLocal(c)), c, swing, got * 2.0);
         }
      }
   }

   public static void landing(Entity entity, double fallDistance) {
      if (PhysicsWorld.active() && (Boolean)PhysicsConfig.LANDINGS.get() && !(fallDistance < (Double)PhysicsConfig.LANDING_MIN_FALL.get())) {
         PhysicsStructure s = PhysicsWorld.structureUnder(entity);
         if (s != null && !(s.mass() < (Double)PhysicsConfig.MIN_MASS_FOR_REACTION.get())) {
            double speed = Math.min(Math.sqrt(64.0 * fallDistance), 78.0);
            Vec3 slide = entity.getDeltaMovement().multiply(20.0, 0.0, 20.0);
            Vec3 j = new Vec3(slide.x, -speed, slide.z)
               .scale(bodyMass(entity) * (Double)PhysicsConfig.LANDING_FACTOR.get() * (Double)PhysicsConfig.REACTION_MULTIPLIER.get());
            double got = apply(s, entity.position(), j);
            if (got > 0.0 && entity.level() instanceof ServerLevel level) {
               BlockPos local = BlockPos.containing(s.toLocal(entity.position().add(0.0, -0.3, 0.0)));
               impactFx(level, s, local, entity.position(), j, got);
            }
         }
      }
   }

   public static void weight(PhysicsStructure s, Vec3 point, double mass) {
      if (PhysicsWorld.active() && (Boolean)PhysicsConfig.REACTIONS.get() && !(mass <= 0.0) && !(s.mass() < (Double)PhysicsConfig.MIN_MASS_FOR_REACTION.get())) {
         apply(s, point, new Vec3(0.0, -1.6 * mass * (Double)PhysicsConfig.REACTION_MULTIPLIER.get(), 0.0));
      }
   }

   public static double bodyMass(Entity entity) {
      double kg = EntityMass.provided(entity);
      return !Double.isNaN(kg)
         ? massUnits(kg) * (Double)PhysicsConfig.MASS_SCALE.get()
         : (Double)PhysicsConfig.PLAYER_MASS.get() * EntityMass.hitboxRatio(entity);
   }

   public static double massUnits(double kg) {
      return (Double)PhysicsConfig.PLAYER_MASS.get() * kg / 70.0;
   }

   public static double load(PhysicsStructure s, Vec3 contact, double massUnits) {
      if (PhysicsWorld.active()
         && (Boolean)PhysicsConfig.REACTIONS.get()
         && !(massUnits <= 0.0)
         && !(s.mass() < (Double)PhysicsConfig.MIN_MASS_FOR_REACTION.get())) {
         double j = massUnits * 11.0 / 20.0 * (Double)PhysicsConfig.REACTION_MULTIPLIER.get();
         j = Math.min(j, (Double)PhysicsConfig.MAX_LOAD_ACCELERATION.get() / 20.0 * s.mass());
         return apply(s, contact, new Vec3(0.0, -j, 0.0));
      } else {
         return 0.0;
      }
   }

   public static double ram(Entity body, PhysicsStructure s, Vec3 contact, Vec3 impulse) {
      if (PhysicsWorld.active() && (Boolean)PhysicsConfig.IMPULSES.get() && !body.level().isClientSide()) {
         PhysicsStructure under = PhysicsWorld.structureUnder(body);
         if (under != null && under.id().equals(s.id())) {
            return 0.0;
         } else {
            double got = apply(s, contact, impulse.scale((Double)PhysicsConfig.REACTION_MULTIPLIER.get()));
            if (got > 0.0 && body.level() instanceof ServerLevel level) {
               impactFx(level, s, BlockPos.containing(s.toLocal(contact)), contact, impulse, got);
            }

            return got;
         }
      } else {
         return 0.0;
      }
   }

   public static double apply(PhysicsStructure s, Vec3 worldPoint, Vec3 impulse) {
      double mag = impulse.length();
      double mass = s.mass();
      if (!(mag < 1.0E-6) && !(mass <= 0.0) && s.isValid()) {
         mag = Math.min(mag, (Double)PhysicsConfig.MAX_IMPULSE_PER_HIT.get());
         StructureForces.Budget b = budget(s);
         double allowed = Math.min(
            (Double)PhysicsConfig.MAX_VELOCITY_CHANGE_PER_TICK.get() - b.usedTick, (Double)PhysicsConfig.IMPULSE_BUDGET_PER_SECOND.get() - b.usedSecond
         );
         if (allowed <= 0.0) {
            return 0.0;
         } else {
            double dv = mag / mass;
            if (dv > allowed) {
               mag = allowed * mass;
               dv = allowed;
            }

            b.usedTick += dv;
            b.usedSecond += dv;
            s.applyImpulse(worldPoint, impulse.normalize().scale(mag));
            return mag;
         }
      } else {
         return 0.0;
      }
   }

   private static StructureForces.Budget budget(PhysicsStructure s) {
      long now = s.level().getGameTime();
      StructureForces.Budget b = BUDGETS.computeIfAbsent(s.id(), k -> new StructureForces.Budget());
      if (b.tick != now) {
         b.tick = now;
         b.usedTick = 0.0;
      }

      if (b.second != now / 20L) {
         b.second = now / 20L;
         b.usedSecond = 0.0;
      }

      return b;
   }

   public static double allowance(PhysicsStructure s, double dv) {
      if ((Boolean)PhysicsConfig.IMPULSES.get() && !(dv < 1.0E-6) && s.isValid()) {
         StructureForces.Budget b = budget(s);
         double left = Math.min(
            (Double)PhysicsConfig.MAX_VELOCITY_CHANGE_PER_TICK.get() - b.usedTick, (Double)PhysicsConfig.IMPULSE_BUDGET_PER_SECOND.get() - b.usedSecond
         );
         return Math.max(0.0, Math.min(1.0, left / dv));
      } else {
         return 0.0;
      }
   }

   public static void kick(PhysicsStructure s, Vec3 dv, Vec3 spin) {
      StructureForces.Budget b = budget(s);
      b.usedTick = b.usedTick + dv.length();
      b.usedSecond = b.usedSecond + dv.length();
      s.addVelocity(dv, spin);
   }

   private static boolean selfHitAllowed(Entity caster) {
      long second = caster.level().getGameTime() / 20L;
      long[] entry = SELF_HITS.computeIfAbsent(caster.getUUID(), k -> new long[]{second, 0L});
      if (entry[0] != second) {
         entry[0] = second;
         entry[1] = 0L;
      }

      return ++entry[1] <= (long)((Integer)PhysicsConfig.MAX_SELF_HITS_PER_SECOND.get()).intValue();
   }

   static void impactFx(ServerLevel level, PhysicsStructure s, BlockPos anchor, Vec3 point, Vec3 impulse, double magnitude) {
      double mass = Math.max(1.0, s.mass());
      float intensity = (float)Math.min(1.0, magnitude / mass / 6.0 + magnitude / 4000.0);
      if (!(intensity < 0.04F)) {
         Vec3 dir = impulse.lengthSqr() < 1.0E-8 ? Vec3.ZERO : impulse.normalize();
         ServerFx.sendNear(level, CoreFx.STRUCTURE_IMPACT, -1, point, dir, intensity, anchor.asLong(), (int)Math.min(2.147483647E9, mass), 96.0);
      }
   }

   public static void cleanup(long gameTime) {
      Iterator<Entry<UUID, StructureForces.Budget>> it = BUDGETS.entrySet().iterator();

      while (it.hasNext()) {
         if (gameTime - it.next().getValue().tick > 200L) {
            it.remove();
         }
      }

      SELF_HITS.values().removeIf(e -> gameTime / 20L - e[0] > 10L);
   }

   public static void clear() {
      BUDGETS.clear();
      SELF_HITS.clear();
   }

   private StructureForces() {
   }

   private static final class Budget {
      long tick = -1L;
      double usedTick;
      long second = -1L;
      double usedSecond;
   }
}
