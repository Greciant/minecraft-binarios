package com.devilfruits.fruits.uo_uo_seiryu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.uo_uo_seiryu.DragonShells;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DragonTwisterAbility extends Ability {
   private static final double ABOVE = 6.0;
   private static final double BELOW = 28.0;

   public DragonTwisterAbility() {
      super(
         UoUoSeiryu.id("dragon_twister"),
         CastType.HELD,
         AbilityStats.builder()
            .energy(50)
            .cooldown(300)
            .mastery(7)
            .damage(5.0)
            .range(10.0)
            .knockback(0.4)
            .blockDamage(true)
            .extra("pull", 0.35, 0.0, 3.0, "Pull strength towards the vortex.")
            .extra("lift", 0.18, 0.0, 2.0, "Upward lift inside the vortex.")
            .extra("hitInterval", 10.0, 1.0, 40.0, "Ticks between damage pulses.")
            .extra("blocksPerSecond", 6.0, 0.0, 64.0, "Blocks ripped up as debris per second.")
            .extra("maxTicks", 120.0, 20.0, 600.0, "Longest spin in ticks.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.TANGENTIAL).force(0.5).blockDamage(0.1).manual().build();
   }

   @Override
   public int maxChannelTicks() {
      return (int)this.config().extra("maxTicks");
   }

   @Override
   public void onChannelStart(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      FormFlags.set(player, DragonShells.TWISTING, true);
      ServerFx.animate(player, UoUoSeiryu.anim("dragon/dragon_twister"));
      ServerFx.send(player, SeiryuFx.TWISTER, player.position(), Vec3.ZERO, ctx.range(), ctx.seed(), this.maxChannelTicks());
   }

   @Override
   public boolean channelTick(AbilityContext ctx, int ticks) {
      ServerPlayer player = ctx.player();
      if (!UoUoSeiryu.isIn(player, "dragon")) {
         return false;
      } else {
         ServerLevel level = ctx.level();
         double range = (double)ctx.range();
         double pull = ctx.extra("pull");
         double lift = ctx.extra("lift");
         boolean pulse = ticks > 0 && ticks % Math.max(1, (int)ctx.extra("hitInterval")) == 0;

         for (Entity e : inVortex(level, player, range)) {
            double rx = e.getX() - player.getX();
            double rz = e.getZ() - player.getZ();
            double h = Math.sqrt(rx * rx + rz * rz);
            double strength = 1.0 - 0.5 * h / range;
            Vec3 in = h < 0.5 ? Vec3.ZERO : new Vec3(-rx / h, 0.0, -rz / h);
            Vec3 swirl = new Vec3(-in.z, 0.0, in.x);
            double up = e.getY() < player.getY() - 2.0 ? lift * strength : -0.04;
            Vec3 v = e.getDeltaMovement().scale(0.6).add(in.scale(pull * strength)).add(swirl.scale(0.45 * strength)).add(0.0, up, 0.0);
            Targeting.launch(e, v);
            if (pulse && e instanceof LivingEntity) {
               ctx.hurt(e, ctx.damage());
            }

            e.fallDistance = Math.min(e.fallDistance, 3.0F);
         }

         if (pulse) {
            List<Entity> victims = inVortex(level, player, range);
            if (!victims.isEmpty()) {
               Entity victim = victims.get(player.getRandom().nextInt(victims.size()));
               ServerFx.send(
                  player, SeiryuFx.CHAIN_ARC, player.position().add(0.0, -2.0, 0.0), victim.getBoundingBox().getCenter(), 1.0F, ctx.seed() + (long)ticks, 0
               );
            }
         }

         ImpactShape vortex = ImpactShape.cylinder(player.position().add(0.0, -28.0, 0.0), range, 34.0);
         BlockImpactService.push(ctx, vortex, new Vec3(0.0, 1.0, 0.0), 0.05, PhysicsProfile.Impact.TANGENTIAL);
         if (ctx.blockDamageAllowed()) {
            debris(ctx, level, player, range, ticks);
            BlockImpactService.damage(ctx, vortex, new Vec3(0.0, 1.0, 0.0), 0.05);
         }

         return true;
      }
   }

   @Override
   public void onChannelEnd(AbilityContext ctx, int ticks, boolean interrupted) {
      ServerPlayer player = ctx.player();
      FormFlags.set(player, DragonShells.TWISTING, false);
      ServerFx.send(player, SeiryuFx.TWISTER, player.position(), Vec3.ZERO, ctx.range(), ctx.seed(), 0);
      if (!interrupted) {
         ServerFx.stopAnimation(player);
      }

      for (Entity e : inVortex(ctx.level(), player, (double)ctx.range())) {
         Vec3 out = e.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
         out = out.lengthSqr() < 1.0E-4 ? ctx.horizontalLook() : out.normalize();
         Targeting.launch(e, out.scale(1.2 + (double)ctx.knockback()).add(0.0, 0.6, 0.0));
      }

      BlockImpactService.push(
         ctx, ImpactShape.cylinder(player.position().add(0.0, -28.0, 0.0), (double)ctx.range(), 34.0), ctx.horizontalLook(), 0.8, PhysicsProfile.Impact.RADIAL
      );
      ServerFx.shake(ctx.level(), player.position(), 0.4F, 32);
   }

   private static List<Entity> inVortex(ServerLevel level, ServerPlayer player, double range) {
      AABB box = new AABB(player.getX() - range, player.getY() - 28.0, player.getZ() - range, player.getX() + range, player.getY() + 6.0, player.getZ() + range);
      List<Entity> out = Targeting.inBox(level, player, box, x -> Targeting.canPush(player, x));
      out.removeIf(e -> {
         double rx = e.getX() - player.getX();
         double rz = e.getZ() - player.getZ();
         return rx * rx + rz * rz > range * range;
      });
      return out;
   }

   private static void debris(AbilityContext ctx, ServerLevel level, ServerPlayer player, double range, int ticks) {
      double perTick = ctx.extra("blocksPerSecond") / 20.0;
      int count = (int)Math.floor(perTick * (double)(ticks + 1)) - (int)Math.floor(perTick * (double)ticks);
      RandomSource random = player.getRandom();

      for (int i = 0; i < count; i++) {
         double a = random.nextDouble() * (float) (Math.PI * 2);
         double r = range * 0.8 * Math.sqrt(random.nextDouble());
         int x = Mth.floor(player.getX() + Math.cos(a) * r);
         int z = Mth.floor(player.getZ() + Math.sin(a) * r);
         int top = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
         if (!((double)top < player.getY() - 28.0) && !((double)top > player.getY() + 6.0)) {
            BlockPos surface = TerrainDestruction.findSurface(level, x, top - 1, z, 1, 2);
            if (surface != null) {
               Vec3 swirl = new Vec3(-Math.sin(a), 0.0, Math.cos(a));
               TerrainDestruction.tearOut(level, player, surface, swirl.scale(0.5).add(0.0, 0.9 + random.nextDouble() * 0.4, 0.0), ctx.budget(), i * 3, true);
            }
         }
      }
   }
}
