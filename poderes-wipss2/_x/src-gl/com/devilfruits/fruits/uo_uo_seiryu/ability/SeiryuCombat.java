package com.devilfruits.fruits.uo_uo_seiryu.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AimHelper;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.TechniqueHeat;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.uo_uo_seiryu.DragonHead;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class SeiryuCombat {
   private static final Map<Integer, int[][]> OFFSETS = new ConcurrentHashMap<>();

   public static float clubFactor(ServerPlayer player, double bonus) {
      return player.getMainHandItem().is(UoUoSeiryu.CLUBS) ? (float)bonus : 1.0F;
   }

   public static Vec3 mouth(ServerPlayer player) {
      if (UoUoSeiryu.isIn(player, "dragon")) {
         return DragonHead.mouth(player, 1.0F);
      } else {
         double ahead = 0.45 * (double)player.getScale();
         return player.getEyePosition().add(AimHelper.look(player).scale(ahead)).add(0.0, -0.12 * (double)player.getScale(), 0.0);
      }
   }

   public static Vec3 rayEnd(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 dir, double length) {
      Vec3 to = from.add(dir.scale(length));
      BlockHitResult hit = PhysicsWorld.clip(level, new ClipContext(from, to, Block.COLLIDER, Fluid.ANY, player));
      return hit.getType() == Type.MISS ? to : hit.getLocation();
   }

   public static List<Entity> inCapsule(ServerLevel level, ServerPlayer caster, Vec3 a, Vec3 b, double radius) {
      return Targeting.inCapsule(level, caster, a, b, radius);
   }

   public static void inCapsule(ServerLevel level, ServerPlayer caster, Vec3 a, Vec3 b, double radius, List<Entity> out) {
      Targeting.capsuleHits(level.getEntities((Entity)null, new AABB(a, b).inflate(radius + 1.0)), caster, a, b, radius, out);
   }

   public static void capsuleHits(List<Entity> candidates, ServerPlayer caster, Vec3 a, Vec3 b, double radius, List<Entity> out) {
      Targeting.capsuleHits(candidates, caster, a, b, radius, out);
   }

   public static int crater(AbilityContext ctx, Vec3 center, double radius, int count, double fling) {
      if (ctx.blockDamageAllowed() && count > 0) {
         BlockImpactService.damage(ctx, ImpactShape.sphere(center, radius + 0.5), new Vec3(0.0, 1.0, 0.0), Math.min(1.0, (double)count / 4.0));
         ServerLevel level = ctx.level();
         BlockPos c = BlockPos.containing(center);
         int r = (int)Math.ceil(radius);
         List<BlockPos> candidates = new ArrayList<>();
         Set<Long> columns = new HashSet<>();

         for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
               if (!((double)(dx * dx + dz * dz) > radius * radius) && columns.add(BlockPos.asLong(dx, 0, dz))) {
                  BlockPos surface = TerrainDestruction.findSurface(level, c.getX() + dx, c.getY(), c.getZ() + dz, 2, 3);
                  if (surface != null) {
                     candidates.add(surface);
                  }
               }
            }
         }

         candidates.sort((p, q) -> Double.compare(p.distSqr(c), q.distSqr(c)));
         int removed = 0;

         for (BlockPos pos : candidates) {
            if (removed >= count) {
               break;
            }

            Vec3 out = Vec3.atCenterOf(pos).subtract(center);
            Vec3 flat = new Vec3(out.x, 0.0, out.z);
            Vec3 velocity = (flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(fling * 0.5)).add(0.0, fling, 0.0);
            if (TerrainDestruction.tearOut(level, ctx.player(), pos, velocity, ctx.budget(), removed * 3, true)) {
               removed++;
            }
         }

         return removed;
      } else {
         return 0;
      }
   }

   public static int blast(AbilityContext ctx, Vec3 center, double radius, double depth, int count, double fling, Long2IntOpenHashMap ground, int healSpread) {
      if (ctx.blockDamageAllowed() && count > 0 && !(radius <= 0.0)) {
         BlockImpactService.damage(
            ctx, ImpactShape.sphere(center, radius), new Vec3(0.0, 1.0, 0.0), Math.min(1.0, (double)count / Math.max(1.0, radius * radius * 3.0))
         );
         ServerLevel level = ctx.level();
         int cx = Mth.floor(center.x);
         int cy = Mth.floor(center.y);
         int cz = Mth.floor(center.z);
         int reach = (int)Math.ceil(radius);
         int up = (int)Math.ceil(depth) + 6;
         int down = (int)Math.ceil(depth) + 8;
         int removed = 0;

         for (int[] o : offsets(reach)) {
            double d2 = (double)(o[0] * o[0] + o[1] * o[1]);
            if (d2 > radius * radius) {
               break;
            }

            int x = cx + o[0];
            int z = cz + o[1];
            long column = BlockPos.asLong(x, 0, z);
            BlockPos surface = TerrainDestruction.findSurface(level, x, cy, z, up, down);
            if (surface != null) {
               int original = ground.computeIfAbsent(column, k -> surface.getY());
               double allowed = depth * (1.0 - d2 / (radius * radius));
               if (!((double)(original - surface.getY() + 1) > allowed + 0.5)) {
                  Vec3 out = new Vec3((double)o[0], 0.0, (double)o[1]);
                  Vec3 velocity = (out.lengthSqr() < 1.0E-4 ? Vec3.ZERO : out.normalize().scale(fling * 0.5)).add(0.0, fling, 0.0);
                  if (TerrainDestruction.tearOut(
                     level, ctx.player(), surface, velocity, ctx.budget(), level.random.nextInt(Math.max(1, healSpread)), true, false
                  )) {
                     if (++removed >= count) {
                        break;
                     }
                  }
               }
            }
         }

         return removed;
      } else {
         return 0;
      }
   }

   public static void scorch(AbilityContext ctx, Vec3 center, double radius, int tries, double chance, int healSpread) {
      TechniqueHeat.emit(ctx.level(), center, radius, ctx.player());
      if (ctx.blockDamageAllowed() && !(chance <= 0.0) && tries > 0) {
         BlockImpactService.ignite(ctx, ImpactShape.sphere(center, radius), chance * Math.min(1.0, (double)tries / Math.max(1.0, Math.PI * radius * radius)));
         ServerLevel level = ctx.level();
         RandomSource random = level.random;
         int cy = Mth.floor(center.y);

         for (int i = 0; i < tries; i++) {
            if (!(random.nextDouble() >= chance)) {
               double a = random.nextDouble() * Math.PI * 2.0;
               double r = Math.sqrt(random.nextDouble()) * radius;
               BlockPos surface = TerrainDestruction.findSurface(level, Mth.floor(center.x + Math.cos(a) * r), cy, Mth.floor(center.z + Math.sin(a) * r), 6, 10);
               if (surface != null) {
                  TerrainDestruction.ignite(level, ctx.player(), surface.above(), random.nextInt(Math.max(1, healSpread)));
               }
            }
         }
      }
   }

   private static int[][] offsets(int reach) {
      return OFFSETS.computeIfAbsent(reach, r -> {
         List<int[]> list = new ArrayList<>();

         for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
               if ((double)(dx * dx + dz * dz) <= ((double)r.intValue() + 0.5) * ((double)r.intValue() + 0.5)) {
                  list.add(new int[]{dx, dz});
               }
            }
         }

         list.sort((p, q) -> Integer.compare(p[0] * p[0] + p[1] * p[1], q[0] * q[0] + q[1] * q[1]));
         return list.toArray(new int[0][]);
      });
   }

   private SeiryuCombat() {
   }
}
