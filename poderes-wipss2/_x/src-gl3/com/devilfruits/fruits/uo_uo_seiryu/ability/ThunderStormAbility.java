package com.devilfruits.fruits.uo_uo_seiryu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.physics.StructureHit;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuTriggers;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ThunderStormAbility extends Ability {
   public static final double DRIFT = 0.08;
   private static final int GATHER_TICKS = 14;

   public ThunderStormAbility() {
      super(
         UoUoSeiryu.id("thunder_storm"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(80)
            .cooldown(600)
            .mastery(8)
            .damage(14.0)
            .range(18.0)
            .knockback(0.6)
            .blockDamage(true)
            .flag("changeWeather", false, "Also turn the world's weather into a thunderstorm while the storm lasts.")
            .flag("realLightning", false, "Use real lightning bolts (can set fires) instead of visual-only bolts.")
            .extra("strikes", 16.0, 1.0, 100.0, "Lightning strikes per storm.")
            .extra("durationTicks", 160.0, 20.0, 1200.0, "Duration of the storm.")
            .extra("strikeRadius", 3.0, 0.5, 12.0, "Radius hurt by one strike.")
            .extra("cloudHeight", 18.0, 6.0, 64.0, "Height of the cloud layer above the dragon.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.RADIAL).force(0.6).blockDamage(0.4).manual().build();
   }

   @Nullable
   @Override
   public ResourceKey<DamageType> damageType() {
      return UoUoSeiryu.THUNDER_DAMAGE;
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      ServerFx.animate(player, UoUoSeiryu.anim("dragon/thunder_storm"));
      SeiryuTriggers.USED_THUNDER_STORM.get().trigger(player);
      int duration = (int)ctx.extra("durationTicks");
      int strikes = Math.max(1, (int)ctx.extra("strikes"));
      int interval = Math.max(2, (duration - 14) / strikes);
      double range = (double)ctx.range();
      double up = ctx.extra("cloudHeight");
      ServerFx.sendNear(level, SeiryuFx.STORM, player.getId(), player.position().add(0.0, up, 0.0), Vec3.ZERO, (float)range, ctx.seed(), duration, 192.0);
      ServerFx.shake(level, player.position(), 0.5F, 48);
      if (ctx.flag("changeWeather")) {
         level.setWeatherParameters(0, duration + 100, true, true);
      }

      double[] center = new double[]{player.getX(), player.getZ()};
      RandomSource random = RandomSource.create(ctx.seed());
      Map<Entity, Integer> struck = new HashMap<>();
      int[] done = new int[]{0};
      ServerScheduler.schedule(
         level,
         1,
         age -> {
            boolean gone = !player.isAlive() || player.isRemoved() || player.level() != level || !UoUoSeiryu.isUser(player);
            if (!gone && age <= duration && done[0] < strikes) {
               center[0] += (player.getX() - center[0]) * 0.08;
               center[1] += (player.getZ() - center[1]) * 0.08;
               if (age >= 14 && (age - 14) % interval == 0) {
                  this.strike(ctx, level, player, center, player.getY() + up, random, struck, age);
                  done[0]++;
               }

               return false;
            } else {
               if (gone) {
                  ServerFx.sendNear(
                     level, SeiryuFx.STORM, player.getId(), new Vec3(center[0], player.getY() + up, center[1]), Vec3.ZERO, (float)range, ctx.seed(), 0, 192.0
                  );
               }

               return true;
            }
         }
      );
   }

   private void strike(
      AbilityContext ctx, ServerLevel level, ServerPlayer player, double[] center, double cloudY, RandomSource random, Map<Entity, Integer> struck, int age
   ) {
      double range = (double)ctx.range();
      double cy = Math.min(cloudY, (double)(level.getMaxBuildHeight() + 48));
      AABB column = new AABB(center[0] - range, cy - 64.0, center[1] - range, center[0] + range, cy, center[1] + range);
      List<Entity> candidates = new ArrayList<>();

      for (Entity e : Targeting.inBox(level, null, column, x -> Targeting.canHurt(player, x))) {
         double dx = e.getX() - center[0];
         double dz = e.getZ() - center[1];
         Integer last = struck.get(e);
         if (dx * dx + dz * dz <= range * range && (last == null || age - last > 25)) {
            candidates.add(e);
         }
      }

      Vec3 ground;
      if (!candidates.isEmpty()) {
         Entity target = candidates.get(random.nextInt(candidates.size()));
         struck.put(target, age);
         ground = target.position();
      } else {
         double a = random.nextDouble() * (float) (Math.PI * 2);
         double r = range * Math.sqrt(random.nextDouble());
         int x = Mth.floor(center[0] + Math.cos(a) * r);
         int z = Mth.floor(center[1] + Math.sin(a) * r);
         int y = level.getHeight(Types.MOTION_BLOCKING, x, z);
         if ((double)y >= cy - 1.0) {
            return;
         }

         ground = new Vec3((double)x + 0.5, (double)y, (double)z + 0.5);
         Optional<StructureHit> deck = PhysicsWorld.bridge().raycast(level, new Vec3(ground.x, cy - 1.0, ground.z), ground, null);
         if (deck.isPresent()) {
            ground = deck.get().worldPoint();
         }
      }

      Vec3 top = new Vec3(ground.x + (random.nextDouble() - 0.5) * 6.0, cy, ground.z + (random.nextDouble() - 0.5) * 6.0);
      double radius = ctx.extra("strikeRadius");

      for (Entity ex : Targeting.inRadius(level, ground.add(0.0, 1.0, 0.0), radius, xx -> Targeting.canHurt(player, xx))) {
         float falloff = (float)Mth.clamp(1.0 - ex.position().distanceTo(ground) / (radius * 1.5), 0.5, 1.0);
         if (ctx.hurt(ex, ctx.damage() * falloff)) {
            Vec3 out = ex.position().subtract(ground).multiply(1.0, 0.0, 1.0);
            Vec3 push = out.lengthSqr() < 1.0E-4 ? Vec3.ZERO : out.normalize().scale((double)ctx.knockback());
            Targeting.launch(ex, push.add(0.0, 0.35, 0.0));
         }
      }

      if (ctx.flag("realLightning")) {
         LightningBolt bolt = (LightningBolt)EntityType.LIGHTNING_BOLT.create(level);
         if (bolt != null) {
            bolt.moveTo(ground);
            bolt.setCause(player);
            level.addFreshEntity(bolt);
         }
      }

      SeiryuCombat.crater(ctx, ground, 1.3, 2, 0.4);
      ImpactShape bolt = ImpactShape.sphere(ground, radius);
      BlockImpactService.push(ctx, bolt, new Vec3(0.0, -1.0, 0.0), 1.0);
      BlockImpactService.mark(player, bolt, new Vec3(0.0, 1.0, 0.0), BlockImpactService.Decal.LIGHTNING, 2.2F);
      BlockImpactService.ignite(ctx, bolt, 0.3);
      long seed = ctx.seed() * 131L + (long)age;
      ServerFx.sendNear(level, SeiryuFx.STRIKE, player.getId(), ground, top, 1.0F, seed, 0, 192.0);
      ServerFx.shake(level, ground, 0.3F, 24);
   }
}
