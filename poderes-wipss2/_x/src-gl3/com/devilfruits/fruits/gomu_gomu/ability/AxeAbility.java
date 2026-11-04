package com.devilfruits.fruits.gomu_gomu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.gomu_gomu.GomuFx;
import com.devilfruits.fruits.gomu_gomu.GomuGomu;
import com.devilfruits.fruits.gomu_gomu.GomuSounds;
import com.devilfruits.fruits.gomu_gomu.GomuTriggers;
import com.devilfruits.registry.DFTags;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AxeAbility extends Ability {
   public static final int RAISE = 7;
   public static final int PAUSE = 4;
   public static final int SLAM = 3;
   public static final int HOLD = 3;
   public static final int RETRACT = 7;
   public static final int IMPACT = 14;
   public static final int DONE = 24;
   public static final double APEX = 5.0;

   public AxeAbility() {
      super(
         GomuGomu.id("axe"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(28)
            .cooldown(300)
            .mastery(6)
            .damage(16.0)
            .range(7.0)
            .knockback(1.6)
            .blockDamage(true)
            .extra("axeRadius", 7.0, 1.0, 16.0, "Reach of the stomp's shockwave in blocks.")
            .extra("axeEdgeFactor", 0.3, 0.0, 1.0, "Share of the damage left at the edge of the shockwave.")
            .extra("axeDistance", 2.5, 1.5, 5.0, "How far straight in front of the user the foot comes down.")
            .extra("axeCraterRadius", 2.0, 0.0, 6.0, "Radius of the crater the stomp tears.")
            .extra("axeMaxHardness", 5.0, 0.0, 50.0, "The hardest block the crater breaks (5: stone, ores and the like; not obsidian).")
            .extra("axeMaxBlocks", 48.0, 0.0, 1000.0, "Blocks one stomp breaks at most.")
            .build()
      );
   }

   @Override
   public boolean coatable() {
      return true;
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.RADIAL).force(1.6).manual().build();
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      Vec3 target = target(ctx);
      Vec3 flat = ctx.horizontalLook();
      AbilityManager.setBusy(player, 24);
      ServerFx.animate(player, ctx.animation(GomuGomu.id("axe")));
      ServerFx.send(player, GomuFx.AXE, target, flat, 5.0F, level.getGameTime(), 0);
      level.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), GomuSounds.STRETCH.get(), SoundSource.PLAYERS, 1.0F, 0.7F);
      ServerScheduler.later(level, 14, () -> {
         if (!player.isRemoved() && player.isAlive() && player.level() == level) {
            impact(ctx, target);
         }
      });
   }

   public static Vec3 target(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      Vec3 at = player.position().add(ctx.horizontalLook().scale(ctx.extra("axeDistance")));
      BlockPos ground = TerrainDestruction.findSurface(level, Mth.floor(at.x), Mth.floor(player.getY()) + 1, Mth.floor(at.z), 3, 8);
      double y = ground != null ? (double)(ground.getY() + 1) : player.getY();
      return new Vec3(at.x, y, at.z);
   }

   public static void impact(AbilityContext ctx, Vec3 at) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      double radius = ctx.extra("axeRadius");
      float damage = ctx.damage();
      double edge = ctx.extra("axeEdgeFactor");

      for (Entity e : Targeting.inRadius(level, at, radius, x -> x != player && Targeting.canHurt(player, x))) {
         double d = Math.sqrt(e.distanceToSqr(at));
         if (!(d > radius)) {
            float falloff = (float)Mth.lerp(d / radius, 1.0, edge);
            if (ctx.hurt(e, damage * falloff)) {
               Vec3 away = new Vec3(e.getX() - at.x, 0.0, e.getZ() - at.z);
               away = away.lengthSqr() < 1.0E-4 ? ctx.horizontalLook() : away.normalize();
               Targeting.launch(e, away.scale((double)(ctx.knockback() * falloff)).add(0.0, 0.55 * (double)falloff + 0.15, 0.0));
            }
         }
      }

      int broken = crater(ctx, at);
      if (broken >= 8) {
         GomuTriggers.AXE_CRATER.get().trigger(player);
      }

      BlockImpactService.slam(ctx, at, radius * 0.6, 1.0);
      BlockImpactService.damage(ctx, ImpactShape.sphere(at, ctx.extra("axeCraterRadius")), new Vec3(0.0, -1.0, 0.0), 1.0);
      ServerFx.send(player, GomuFx.AXE_IMPACT, at, ctx.horizontalLook(), (float)radius, ctx.seed(), broken);
      ServerFx.shake(level, at, 1.2F, (int)(radius * 3.0));
      level.playSound(null, at.x, at.y, at.z, GomuSounds.AXE_IMPACT.get(), SoundSource.PLAYERS, 1.6F, 0.85F);
   }

   public static int crater(AbilityContext ctx, Vec3 at) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      double r = ctx.extra("axeCraterRadius");
      int cap = (int)ctx.extra("axeMaxBlocks");
      if (!(r <= 0.0) && cap > 0 && ctx.blockDamageAllowed()) {
         double depth = Math.max(1.0, r * 0.6);
         double maxHardness = ctx.extra("axeMaxHardness");
         BlockPos c = BlockPos.containing(at.x, at.y - 0.5, at.z);
         List<BlockPos> bowl = new ArrayList<>();
         int ir = Mth.ceil(r);

         for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
               for (int dy = -Mth.ceil(depth); dy <= 0; dy++) {
                  double q = (double)(dx * dx + dz * dz) / (r * r) + (double)(dy * dy) / (depth * depth);
                  if (q <= 1.0) {
                     bowl.add(c.offset(dx, dy, dz));
                  }
               }
            }
         }

         bowl.sort(Comparator.comparingDouble(p -> p.distToCenterSqr(at)));
         AABB feet = player.getBoundingBox().inflate(0.6, 0.0, 0.6).expandTowards(0.0, -depth - 1.0, 0.0);
         int broken = 0;

         for (BlockPos pos : bowl) {
            if (broken >= cap) {
               break;
            }

            if (!feet.intersects(new AABB(pos))) {
               BlockState state = level.getBlockState(pos);
               float hardness = state.getDestroySpeed(level, pos);
               if (!state.isAir() && !(hardness < 0.0F) && !((double)hardness > maxHardness) && !state.is(DFTags.RESISTS_TECHNIQUES)) {
                  Vec3 out = Vec3.atCenterOf(pos).subtract(at).multiply(1.0, 0.0, 1.0);
                  Vec3 velocity = (out.lengthSqr() < 1.0E-4 ? Vec3.ZERO : out.normalize().scale(0.25)).add(0.0, 0.45 + level.random.nextDouble() * 0.3, 0.0);
                  if (TerrainDestruction.tearOut(level, player, pos, velocity, ctx.budget(), broken * 2, true, false)) {
                     broken++;
                  }
               }
            }
         }

         return broken;
      } else {
         return 0;
      }
   }
}
