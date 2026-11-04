package com.devilfruits.fruits.suna_suna.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.fruits.suna_suna.Desiccation;
import com.devilfruits.fruits.suna_suna.SunaEffects;
import com.devilfruits.fruits.suna_suna.SunaFx;
import com.devilfruits.fruits.suna_suna.SunaGround;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class BarjanAbility extends SandTechnique {
   public static final double BOW = 0.45;
   public static final double DEPTH = 0.45;

   public BarjanAbility() {
      super(
         "barjan",
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(20)
            .cooldown(80)
            .mastery(3)
            .damage(9.0)
            .range(26.0)
            .knockback(1.2)
            .blockDamage(true)
            .extra("speed", 2.2, 0.3, 4.0, "Speed of the crescent, blocks per tick.")
            .extra("span", 9.0, 1.0, 16.0, "Width of the crescent from tip to tip.")
            .extra("dehydrateSeconds", 6.0, 0.0, 60.0, "Seconds of Dehydrated for whatever the crescent hits (stacks up to level III).")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.DIRECTIONAL).force(1.0).blockDamage(0.2).manual().build();
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      ServerFx.animate(player, ctx.animation(SunaSuna.id("barjan")));
      sandArm(player, 12);
      Vec3 look = ctx.look();
      double speed = ctx.extra("speed");
      double span = ctx.extra("span");
      Vec3 start = player.getEyePosition().add(0.0, -0.8, 0.0).add(look.scale(1.4));
      Vec3 velocity = look.scale(speed).add(PhysicsWorld.carrierVelocity(player));
      int lifetime = Mth.ceil((double)ctx.range() / speed);
      long seed = ctx.seed();
      ServerFx.sendNear(level, SunaFx.BARJAN, player.getId(), start, velocity, (float)span, seed, lifetime, 96.0);
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      Vec3 side = flat.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(-flat.z, 0.0, flat.x).normalize();
      Vec3[] at = new Vec3[]{start};
      IntSet struck = new IntOpenHashSet();
      Set<UUID> structures = new HashSet<>();
      int seconds = (int)ctx.extra("dehydrateSeconds");
      CastBudget drying = Desiccation.budget();
      SandFlight flight = SandFlight.track(level, player, start, span * 0.5, seed, (float)span, 2);
      ServerScheduler.schedule(level, 1, age -> {
         if (!player.isRemoved() && player.level() == level && !flight.broken()) {
            Vec3 pos = at[0];
            Vec3 next = pos.add(velocity);
            BlockHitResult block = PhysicsWorld.clip(level, new ClipContext(pos, next, Block.COLLIDER, Fluid.NONE, player));
            Vec3 end = block.getType() == Type.MISS ? next : block.getLocation();
            double half = span * 0.5;
            BlockImpactService.strike(ctx, ImpactShape.capsule(end.add(side.scale(half)), end.add(side.scale(-half)), 1.2), flat.normalize(), 1.0, structures);
            AABB box = new AABB(pos, end).inflate(half + 1.0, 1.4, half + 1.0);

            for (Entity e : Targeting.inBox(level, player, box, x -> Targeting.canHurt(player, x))) {
               if (sweeps(pos, end, side, half, half * 0.45, e) && struck.add(e.getId()) && ctx.hurt(e, ctx.damage())) {
                  Targeting.launch(e, flat.normalize().scale((double)ctx.knockback()).add(0.0, 0.3, 0.0));
                  if (seconds > 0 && e instanceof LivingEntity living) {
                     SunaEffects.dehydrate(living, seconds, 1, 3, player);
                  }
               }
            }

            double ground = SunaGround.surface(level, end.x, end.y, end.z, 0, 3);
            if (!Double.isNaN(ground)) {
               Desiccation.dry(level, player, new Vec3(end.x, ground, end.z), Math.min(2.5, half * 0.5), 0.0, Desiccation.Options.SURFACE, drying);
            }

            at[0] = end;
            flight.moveTo(end);
            if (block.getType() == Type.MISS && age < lifetime) {
               return false;
            } else {
               flight.end();
               ServerFx.sendNear(level, SunaFx.BARJAN_END, player.getId(), end, velocity, (float)span, seed, 0, 96.0);
               return true;
            }
         } else {
            flight.end();
            return true;
         }
      });
   }

   private static boolean sweeps(Vec3 from, Vec3 to, Vec3 side, double half, double bow, Entity e) {
      Vec3 c = e.getBoundingBox().getCenter();
      Vec3 path = to.subtract(from);
      double length = path.length();
      Vec3 along = length < 1.0E-6 ? Vec3.ZERO : path.scale(1.0 / length);
      Vec3 rel = c.subtract(from);
      double a = rel.dot(along);
      double lateral = Math.abs(rel.dot(side));
      double vertical = Math.abs(rel.subtract(along.scale(a)).subtract(side.scale(rel.dot(side))).length());
      double w = (double)e.getBbWidth();
      double trail = bow * Mth.square(Math.min(1.0, lateral / half));
      return a >= -trail - 0.8 - w && a <= length - trail + 0.8 + w && lateral <= half + w * 0.5 && vertical <= 0.7 + (double)e.getBbHeight() * 0.5;
   }
}
