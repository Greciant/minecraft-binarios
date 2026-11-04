package com.devilfruits.fruits.uo_uo_seiryu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.AimHelper;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class DraconicAscensionAbility extends Ability {
   public static final int WINDUP = 8;
   private static final int MAX_FLIGHT = 40;

   public DraconicAscensionAbility() {
      super(
         UoUoSeiryu.id("draconic_ascension"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(25)
            .cooldown(200)
            .mastery(4)
            .damage(1.0)
            .range(16.0)
            .knockback(0.8)
            .blockDamage(true)
            .extra("arcHeight", 4.0, 0.0, 16.0, "How high the arc rises over its ends (blocks).")
            .extra("flightTicks", 12.0, 4.0, 40.0, "Ticks the arc takes.")
            .extra("stunTicks", 30.0, 0.0, 200.0, "How long the slam stuns what it hits (ticks).")
            .extra("impactRadius", 3.0, 0.5, 10.0, "Radius of the slam.")
            .extra("craterBlocks", 8.0, 0.0, 64.0, "Floor blocks the slam tears out (they heal).")
            .build()
      );
   }

   @Override
   public boolean coatable() {
      return true;
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults())
         .impact(PhysicsProfile.Impact.RADIAL)
         .force(5.0)
         .reaction(PhysicsProfile.Reaction.LANDING)
         .blockDamage(0.5)
         .manual()
         .build();
   }

   @Nullable
   @Override
   public ResourceKey<DamageType> damageType() {
      return UoUoSeiryu.DRAGON_DAMAGE;
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      LivingEntity target = HybridCombat.aimTarget(player, (double)ctx.range(), 30.0);
      Vec3 fallback = landingAhead(ctx);
      int flight = (int)ctx.extra("flightTicks");
      AbilityManager.setBusy(player, 52);
      HybridCombat.root(player, 8);
      ServerFx.animate(player, UoUoSeiryu.anim("hybrid/draconic_ascension"));
      ServerFx.send(player, SeiryuFx.ASCENSION, player.position(), Vec3.ZERO, 0.0F, ctx.seed(), 8);
      Vec3[] start = new Vec3[]{Vec3.ZERO};
      ServerScheduler.schedule(
         level,
         1,
         age -> {
            if (player.isAlive() && !player.isRemoved() && player.level() == level && UoUoSeiryu.isIn(player, "hybrid")) {
               player.fallDistance = 0.0F;
               if (age < 8) {
                  Targeting.moveSelf(player, new Vec3(0.0, Math.min(0.0, player.getDeltaMovement().y), 0.0));
                  return false;
               } else {
                  Vec3 end = target != null && target.isAlive() ? target.position() : fallback;
                  if (age == 8) {
                     start[0] = player.position();
                     ServerFx.send(player, SeiryuFx.ASCENSION_LAUNCH, start[0], end, (float)ctx.extra("arcHeight"), ctx.seed(), flight);
                  }

                  int t = age - 8;
                  float s = Math.min(1.0F, (float)t / (float)flight);
                  float next = Math.min(1.0F, (float)(t + 1) / (float)flight);
                  boolean arrived = s >= 1.0F
                     || s > 0.5F && player.onGround()
                     || target != null && s > 0.4F && player.distanceTo(target) < 1.4F + target.getBbWidth();
                  if (!arrived && t < 40) {
                     Vec3 want = arc(start[0], end, next, ctx.extra("arcHeight"));
                     Targeting.moveSelf(player, want.subtract(player.position()));
                     return false;
                  } else {
                     this.impact(ctx);
                     return true;
                  }
               }
            } else {
               AbilityManager.clearBusy(player);
               return true;
            }
         }
      );
   }

   public static Vec3 arc(Vec3 a, Vec3 b, float s, double h) {
      double top = Math.max(0.0, Math.max(a.y, b.y) - Mth.lerp((double)s, a.y, b.y)) * 4.0 * (double)s * (double)(1.0F - s)
         + 4.0 * h * (double)s * (double)(1.0F - s);
      return new Vec3(Mth.lerp((double)s, a.x, b.x), Mth.lerp((double)s, a.y, b.y) + top, Mth.lerp((double)s, a.z, b.z));
   }

   private static Vec3 landingAhead(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      Vec3 eye = player.getEyePosition();
      Vec3 to = eye.add(AimHelper.look(player).scale((double)ctx.range()));
      BlockHitResult hit = player.level().clip(new ClipContext(eye, to, Block.COLLIDER, Fluid.NONE, player));
      if (hit.getType() != Type.MISS) {
         return hit.getLocation();
      } else {
         Vec3 ahead = player.position().add(ctx.horizontalLook().scale((double)ctx.range() * 0.66));
         BlockPos ground = TerrainDestruction.findSurface(ctx.level(), Mth.floor(ahead.x), Mth.floor(ahead.y), Mth.floor(ahead.z), 4, 12);
         return ground != null ? Vec3.atBottomCenterOf(ground.above()) : ahead;
      }
   }

   private void impact(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      Targeting.moveSelf(player, new Vec3(0.0, Math.min(0.0, player.getDeltaMovement().y), 0.0));
      BlockPos ground = TerrainDestruction.findSurface(level, Mth.floor(player.getX()), Mth.floor(player.getY()), Mth.floor(player.getZ()), 1, 5);
      Vec3 at = ground != null ? Vec3.atBottomCenterOf(ground.above()) : player.position();
      float radius = (float)ctx.extra("impactRadius");
      float damage = HybridCombat.hit(ctx);

      for (Entity e : Targeting.inRadius(level, at.add(0.0, 1.0, 0.0), (double)radius, x -> Targeting.canHurt(player, x))) {
         if (HybridCombat.strike(ctx, e, damage) && e instanceof LivingEntity living) {
            HybridStun.apply(living, (int)ctx.extra("stunTicks"));
            Targeting.launch(living, new Vec3(0.0, 0.35 * (double)ctx.knockback(), 0.0));
         }
      }

      BlockImpactService.slam(ctx, at, (double)radius, 0.8);
      SeiryuCombat.crater(ctx, at.add(0.0, -0.5, 0.0), (double)radius * 0.6, (int)ctx.extra("craterBlocks"), 0.6);
      ServerFx.animate(player, UoUoSeiryu.anim("hybrid/draconic_ascension_land"));
      ServerFx.send(player, SeiryuFx.ASCENSION_IMPACT, at, Vec3.ZERO, radius, ctx.seed(), 0);
      ServerFx.shake(level, at, 0.7F, 40);
      AbilityManager.setBusy(player, 6);
   }
}
