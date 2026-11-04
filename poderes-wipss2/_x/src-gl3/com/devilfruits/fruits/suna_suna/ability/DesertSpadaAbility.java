package com.devilfruits.fruits.suna_suna.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.suna_suna.SunaFx;
import com.devilfruits.fruits.suna_suna.SunaGround;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import com.devilfruits.fruits.suna_suna.SunaTags;
import com.devilfruits.fruits.suna_suna.SunaTriggers;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayDeque;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DesertSpadaAbility extends SandTechnique {
   public static final double STEP = 0.5;
   private static final int TRAIL = 2;

   public DesertSpadaAbility() {
      super(
         "desert_spada",
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(22)
            .cooldown(80)
            .mastery(2)
            .damage(11.0)
            .range(42.0)
            .knockback(0.9)
            .blockDamage(true)
            .extra("speed", 2.2, 0.3, 6.0, "Speed of the blade along the ground, blocks per tick.")
            .extra("cutWidth", 2.0, 0.3, 6.0, "Half-width of what the blade cuts.")
            .extra("cutHeight", 3.0, 1.0, 8.0, "Height the blade cuts through above the ground, in blocks.")
            .extra("maxBlocks", 420.0, 0.0, 4096.0, "Most blocks a single blade tears out.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.DIRECTIONAL).force(0.4).blockDamage(1.0).manual().build();
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      ServerFx.animate(player, ctx.animation(SunaSuna.id("desert_spada")));
      SunaTriggers.USED_SPADA.get().trigger(player);
      sandArm(player, 14);
      Vec3 dir = ctx.horizontalLook();
      double speed = ctx.extra("speed");
      double width = ctx.extra("cutWidth");
      int height = (int)ctx.extra("cutHeight");
      boolean cuts = ctx.blockDamageAllowed();
      Vec3 ahead = player.position().add(dir.scale(1.2));
      double y = SunaGround.surface(level, ahead.x, player.getY(), ahead.z, 1, 3);
      Vec3 start = new Vec3(ahead.x, Double.isNaN(y) ? player.getY() : y, ahead.z);
      int lifetime = Mth.ceil((double)ctx.range() / speed);
      long seed = ctx.seed();
      Vec3 velocity = dir.scale(speed);
      ServerFx.sendNear(level, SunaFx.SPADA, player.getId(), start, velocity, (float)(cuts ? width : -width), seed, lifetime, 128.0);
      Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
      int steps = steps(velocity);
      Vec3 step = velocity.scale(1.0 / (double)steps);
      Vec3[] at = new Vec3[]{start};
      ArrayDeque<Vec3> trail = new ArrayDeque<>();
      IntSet struck = new IntOpenHashSet();
      LongSet cut = new LongOpenHashSet();
      CastBudget budget = new CastBudget((int)ctx.extra("maxBlocks"), (Integer)CoreConfig.MAX_DEBRIS_ENTITIES_PER_CAST.get());
      SandFlight flight = SandFlight.track(
         level, player, start.add(0.0, (double)height * 0.5, 0.0), Math.max(width, (double)height) * 0.5, seed, (float)width, 3
      );
      ServerScheduler.schedule(level, 1, age -> {
         if (!player.isRemoved() && player.level() == level && !flight.broken()) {
            Vec3 from = at[0];
            boolean stopped = false;
            if (age == 0 && cuts) {
               cut(level, player, from, dir, side, width, 0, height, cut, budget);
               trail.add(from);
               stopped = blocked(level, from, height);
            }

            Vec3 pos = from;

            for (int i = 0; i < steps && !stopped; i++) {
               Vec3 next = pos.add(step);
               double ground = next(level, pos, next, cuts);
               if (Double.isNaN(ground)) {
                  stopped = true;
                  break;
               }

               next = new Vec3(next.x, ground, next.z);
               if (cuts) {
                  cut(level, player, next, dir, side, width, 0, height, cut, budget);
                  if (blocked(level, next, height)) {
                     stopped = true;
                     break;
                  }

                  trail.add(next);
                  if (trail.size() > 2) {
                     cut(level, player, trail.poll(), dir, side, width, -1, 0, cut, budget);
                  }
               }

               pos = next;
            }

            if (cuts) {
               ImpactShape blade = ImpactShape.blade(from, pos, width, 1.0, (double)height);
               BlockImpactService.damage(ctx, blade, dir, 1.0, true, budget);
               BlockImpactService.push(ctx, blade, dir, 0.15);
            }

            for (Entity e : Targeting.inCapsule(level, player, from.add(0.0, 1.2, 0.0), pos.add(0.0, 1.2, 0.0), width)) {
               if (struck.add(e.getId()) && ctx.hurt(e, ctx.damage())) {
                  double s = e.position().subtract(pos).dot(side) >= 0.0 ? 1.0 : -1.0;
                  Targeting.launch(e, side.scale(s * (double)ctx.knockback()).add(dir.scale(0.2)).add(0.0, 0.45, 0.0));
               }
            }

            at[0] = pos;
            flight.moveTo(pos.add(0.0, (double)height * 0.5, 0.0));
            if (!stopped && age < lifetime) {
               return false;
            } else {
               flight.end();

               while (!trail.isEmpty()) {
                  cut(level, player, trail.poll(), dir, side, width, -1, 0, cut, budget);
               }

               end(level, player, pos, seed);
               return true;
            }
         } else {
            flight.end();
            return true;
         }
      });
   }

   public static double next(Level level, Vec3 pos, Vec3 next, boolean cuts) {
      double ground = SunaGround.surface(level, next.x, pos.y, next.z, 1, 3);
      if (!Double.isNaN(ground)) {
         return ground;
      } else {
         return cuts && SunaGround.inside(level, next.x, pos.y + 0.1, next.z) ? pos.y : Double.NaN;
      }
   }

   public static int steps(Vec3 velocity) {
      return Math.max(1, Mth.ceil(velocity.length() / 0.5 - 1.0E-4));
   }

   public static int base(double ground) {
      return Mth.ceil(ground - 0.001);
   }

   public static boolean resisted(Level level, double x, double ground, double z) {
      MutableBlockPos pos = new MutableBlockPos(Mth.floor(x), base(ground), Mth.floor(z));
      int dy = 0;

      while (dy < 2) {
         if (level.getBlockState(pos).is(SunaTags.SPADA_RESISTS)) {
            return true;
         }

         dy++;
         pos.move(0, 1, 0);
      }

      return false;
   }

   private static void cut(
      ServerLevel level, ServerPlayer player, Vec3 p, Vec3 dir, Vec3 side, double width, int fromDy, int toDy, LongSet cut, CastBudget budget
   ) {
      int base = base(p.y);
      int lanes = (int)Math.floor(Math.max(0.0, width - 0.75) / 0.5);
      MutableBlockPos pos = new MutableBlockPos();

      for (int k = -lanes; k <= lanes; k++) {
         double s = (double)k * 0.5;
         int bx = Mth.floor(p.x + side.x * s);
         int bz = Mth.floor(p.z + side.z * s);

         for (int dy = fromDy; dy < toDy; dy++) {
            pos.set(bx, base + dy, bz);
            if (cut.add(pos.asLong())) {
               BlockState state = level.getBlockState(pos);
               if (!state.isAir() && !state.is(SunaTags.SPADA_RESISTS) && (dy < 0 || !state.getCollisionShape(level, pos).isEmpty())) {
                  double out = (double)(s >= 0.0 ? 1 : -1) * (0.15 + 0.08 * Math.abs(s));
                  Vec3 fling = side.scale(out).add(dir.scale(0.1)).add(0.0, 0.38 + 0.04 * (double)dy, 0.0);
                  TerrainDestruction.tearOut(level, player, pos.immutable(), fling, budget, Math.abs(k) * 3 + dy + 2, true);
               }
            }
         }
      }
   }

   private static boolean blocked(Level level, Vec3 p, int height) {
      MutableBlockPos pos = new MutableBlockPos(Mth.floor(p.x), base(p.y), Mth.floor(p.z));
      int dy = 0;

      while (dy < Math.min(2, height)) {
         if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            return true;
         }

         dy++;
         pos.move(0, 1, 0);
      }

      return false;
   }

   private static void end(ServerLevel level, ServerPlayer player, Vec3 at, long seed) {
      ServerFx.sendNear(level, SunaFx.SPADA_END, player.getId(), at, Vec3.ZERO, 1.0F, seed, 0, 128.0);
   }
}
