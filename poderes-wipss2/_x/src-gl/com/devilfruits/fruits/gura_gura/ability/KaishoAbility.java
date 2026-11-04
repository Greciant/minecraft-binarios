package com.devilfruits.fruits.gura_gura.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.gura_gura.GuraFx;
import com.devilfruits.fruits.gura_gura.GuraGura;
import com.devilfruits.fruits.gura_gura.GuraTriggers;
import com.devilfruits.fruits.gura_gura.entity.QuakeWaveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class KaishoAbility extends Ability {
   private static final double LAND_REACH = 7.0;

   public KaishoAbility() {
      super(
         GuraGura.id("kaisho"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(45)
            .cooldown(300)
            .mastery(4)
            .damage(14.0)
            .range(22.0)
            .knockback(1.8)
            .extra("waveWidth", 10.0, 2.0, 64.0, "Width of the tsunami in blocks.")
            .extra("waveHeight", 5.0, 1.0, 32.0, "Height of the tsunami in blocks.")
            .extra("speed", 0.75, 0.1, 4.0, "Travel speed in blocks per tick.")
            .extra("waterSearch", 8.0, 2.0, 32.0, "How far ahead (blocks) water is searched for.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.DIRECTIONAL).force(1.0).blockDamage(0.3).manual().build();
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      Vec3 dir = ctx.horizontalLook();
      BlockPos waterStart = findWater(level, player, dir, (int)ctx.extra("waterSearch"));
      boolean water = waterStart != null;
      ServerFx.animate(player, ctx.animation(GuraGura.anim("kaisho")));
      ServerFx.send(player, GuraFx.KAISHO_CAST, player.position().add(0.0, 1.2, 0.0).add(dir.scale(0.8)), dir, water ? 1.0F : 0.0F, ctx.seed(), 0);
      if (!water) {
         this.quake(ctx, dir);
      } else {
         float mul = ctx.range() / (float)ctx.config().range();
         Vec3 start = new Vec3((double)waterStart.getX() + 0.5, (double)waterStart.getY() + 0.9, (double)waterStart.getZ() + 0.5);
         float width = (float)ctx.extra("waveWidth") * mul;
         float height = (float)ctx.extra("waveHeight") * mul;
         level.addFreshEntity(
            new QuakeWaveEntity(level, player, start, dir, true, width, height, (float)ctx.extra("speed"), ctx.range(), ctx.damage(), ctx.knockback())
               .withDestruction(GuraGura.destruction(ctx.mastery()))
         );
         if (level.getBiome(BlockPos.containing(start)).is(BiomeTags.IS_OCEAN)) {
            GuraTriggers.OCEAN_SEAQUAKE.get().trigger(player);
         }
      }
   }

   private void quake(AbilityContext ctx, Vec3 dir) {
      ServerPlayer player = ctx.player();
      Vec3 eye = player.getEyePosition();

      for (Entity e : Targeting.inCone(ctx.level(), eye, dir, 7.0, Math.toRadians(30.0), x -> Targeting.canHurt(player, x))) {
         if (ctx.hurt(e, ctx.damage() * 0.5F)) {
            Targeting.launch(e, dir.scale((double)ctx.knockback()).add(0.0, 0.35, 0.0));
         }
      }
   }

   private static BlockPos findWater(ServerLevel level, ServerPlayer player, Vec3 dir, int search) {
      int baseY = player.getBlockY();
      Vec3 perp = new Vec3(-dir.z, 0.0, dir.x);
      BlockPos best = null;
      double bestScore = Double.MAX_VALUE;
      int count = 0;
      MutableBlockPos pos = new MutableBlockPos();

      for (int d = 0; d <= search; d++) {
         for (int s = -2; s <= 2; s++) {
            double x = player.getX() + dir.x * (double)d + perp.x * (double)s;
            double z = player.getZ() + dir.z * (double)d + perp.z * (double)s;

            for (int dy = 1; dy >= -4; dy--) {
               pos.set(Mth.floor(x), baseY + dy, Mth.floor(z));
               if (level.getFluidState(pos).is(FluidTags.WATER) && !level.getFluidState(pos.above()).is(FluidTags.WATER)) {
                  count++;
                  double score = (double)d + (double)Math.abs(s) * 1.5;
                  if (score < bestScore) {
                     bestScore = score;
                     best = pos.immutable();
                  }
                  break;
               }
            }
         }
      }

      return count >= 6 ? best : null;
   }
}
