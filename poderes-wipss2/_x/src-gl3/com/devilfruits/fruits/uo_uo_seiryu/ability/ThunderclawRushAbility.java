package com.devilfruits.fruits.uo_uo_seiryu.ability;

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
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ThunderclawRushAbility extends Ability {
   public static final int WINDUP = 3;
   public static final int MAX_DASH = 10;
   public static final int CLAW_1 = 2;
   public static final int CLAW_2 = 8;
   public static final int CLAW_3 = 14;
   public static final int FINISHER = 22;

   public ThunderclawRushAbility() {
      super(
         UoUoSeiryu.id("thunderclaw_rush"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(25)
            .cooldown(160)
            .mastery(3)
            .damage(0.7)
            .range(12.0)
            .knockback(1.6)
            .blockDamage(true)
            .extra("lungeSpeed", 2.2, 0.5, 5.0, "Lunge speed in blocks per tick.")
            .extra("homingAngle", 35.0, 0.0, 90.0, "Half angle of the cone in front in which the lunge homes onto an enemy (degrees).")
            .extra("stunTicks", 38.0, 0.0, 200.0, "How long the first claw freezes the target (ticks).")
            .extra("finisherWeight", 1.5, 0.0, 10.0, "Weight of the lightning finisher (x hybridBaseHit); only under Thunderclad Veil after all three claws.")
            .extra("finisherRadius", 3.0, 0.0, 10.0, "Radius of the lightning finisher.")
            .extra("craterBlocks", 10.0, 0.0, 64.0, "Blocks the finisher tears out of the ground.")
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
         .impact(PhysicsProfile.Impact.DIRECTIONAL)
         .force(2.0)
         .reaction(PhysicsProfile.Reaction.DASH)
         .blockDamage(0.4)
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
      ThunderclawRushAbility.Run run = new ThunderclawRushAbility.Run();
      run.veiled = HybridCombat.veiled(player);
      run.target = HybridCombat.aimTarget(player, (double)ctx.range(), ctx.extra("homingAngle"));
      run.flat = ctx.horizontalLook();
      AbilityManager.setBusy(player, 37);
      ServerFx.animate(player, UoUoSeiryu.anim("hybrid/thunderclaw_rush"));
      ServerFx.send(player, SeiryuFx.RUSH, player.position(), run.flat, run.veiled ? 1.0F : 0.0F, ctx.seed(), 3);
      ServerScheduler.schedule(level, 1, age -> this.tick(ctx, run, age));
   }

   private boolean tick(AbilityContext ctx, ThunderclawRushAbility.Run run, int age) {
      ServerPlayer player = ctx.player();
      if (player.isAlive() && !player.isRemoved() && player.level() == ctx.level() && UoUoSeiryu.isIn(player, "hybrid")) {
         player.fallDistance = 0.0F;
         if (age < 3) {
            Targeting.moveSelf(player, new Vec3(0.0, Math.min(0.0, player.getDeltaMovement().y), 0.0));
            return false;
         } else if (run.arrival < 0) {
            return this.dash(ctx, run, age);
         } else {
            int t = age - run.arrival;
            if (run.stand != null && player.position().distanceToSqr(run.stand) > 0.64) {
               player.teleportTo(ctx.level(), run.stand.x, run.stand.y, run.stand.z, player.getYRot(), player.getXRot());
            }

            if (t % 2 == 0) {
               Targeting.moveSelf(player, new Vec3(0.0, Math.min(0.0, player.getDeltaMovement().y), 0.0));
            }

            if (t == 2 || t == 8 || t == 14) {
               this.claw(ctx, run, t == 2 ? 1 : (t == 8 ? 2 : 3));
            }

            boolean finisher = run.veiled && run.landed >= 3;
            if (finisher && t == 22) {
               this.finisher(ctx, run);
            }

            if (t >= (finisher ? 24 : 18)) {
               AbilityManager.clearBusy(player);
               return true;
            } else {
               return false;
            }
         }
      } else {
         AbilityManager.clearBusy(player);
         return true;
      }
   }

   private boolean dash(AbilityContext ctx, ThunderclawRushAbility.Run run, int age) {
      ServerPlayer player = ctx.player();
      LivingEntity target = run.target != null && run.target.isAlive() ? run.target : null;
      if (age == 3) {
         run.start = player.position();
         Vec3 end = target != null ? target.position() : run.start.add(run.flat.scale((double)ctx.range() * 0.5));
         int ticks = Mth.clamp((int)Math.ceil(end.distanceTo(run.start) / Math.max(0.5, ctx.extra("lungeSpeed"))), 1, 10);
         ServerFx.send(player, SeiryuFx.RUSH_DASH, run.start, end, run.veiled ? 1.0F : 0.0F, ctx.seed(), ticks);
      }

      Vec3 goal = target != null ? target.position() : run.start.add(run.flat.scale((double)ctx.range() * 0.5));
      Vec3 to = goal.subtract(player.position());
      double dist = Math.sqrt(to.x * to.x + to.z * to.z);
      double reach = target != null ? (double)target.getBbWidth() * 0.5 + (double)player.getBbWidth() * 0.5 + 0.4 : 0.4;
      double speed = ctx.extra("lungeSpeed");
      if (!(dist <= reach + speed) && age - 3 < 10) {
         Vec3 dir = new Vec3(to.x / dist, 0.0, to.z / dist);
         double step = Math.min(speed, dist - reach);
         double vy = target != null ? Mth.clamp((target.getY() - player.getY()) * 0.3, -0.5, 0.6) : Math.min(0.0, player.getDeltaMovement().y);
         Targeting.moveSelf(player, dir.scale(step).add(0.0, vy, 0.0));
         return false;
      } else {
         run.arrival = age;
         if (dist > 0.001) {
            run.flat = new Vec3(to.x / dist, 0.0, to.z / dist);
         }

         Targeting.moveSelf(player, new Vec3(0.0, Math.min(0.0, player.getDeltaMovement().y), 0.0));
         if (target != null && dist <= reach + speed * 2.0) {
            double y = Math.abs(target.getY() - player.getY()) < 1.5 ? target.getY() : player.getY();
            run.stand = new Vec3(goal.x - run.flat.x * reach, y, goal.z - run.flat.z * reach);
            float yaw = (float)(Mth.atan2(-run.flat.x, run.flat.z) * 180.0F / (float)Math.PI);
            player.teleportTo(ctx.level(), run.stand.x, run.stand.y, run.stand.z, yaw, player.getXRot());
            player.setYBodyRot(yaw);
            player.setYHeadRot(yaw);
         }

         ServerFx.animate(player, UoUoSeiryu.anim("hybrid/thunderclaw_claws"));
         return false;
      }
   }

   private void claw(AbilityContext ctx, ThunderclawRushAbility.Run run, int n) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      List<Entity> victims = new ArrayList<>();
      LivingEntity target = run.target;
      double reach = 1.6 + (double)player.getBbWidth();
      if (target != null && target.isAlive() && (double)target.distanceTo(player) <= reach + (double)target.getBbWidth()) {
         victims.add(target);
      } else {
         Vec3 from = player.position().add(0.0, (double)player.getBbHeight() * 0.45, 0.0);
         SeiryuCombat.inCapsule(level, player, from, from.add(run.flat.scale(reach)), 1.0, victims);
      }

      Vec3 at = victims.isEmpty()
         ? player.position().add(run.flat.scale(reach)).add(0.0, (double)player.getBbHeight() * 0.45, 0.0)
         : victims.get(0).getBoundingBox().getCenter();
      float damage = HybridCombat.hit(ctx);
      boolean any = false;

      for (Entity e : victims) {
         if (HybridCombat.strike(ctx, e, damage) && e instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)e;
            any = true;
            switch (n) {
               case 1:
                  HybridStun.apply(living, (int)ctx.extra("stunTicks"));
                  break;
               case 2:
                  HybridStun.apply(living, (int)ctx.extra("stunTicks") / 2);
                  Targeting.launch(living, new Vec3(0.0, 0.42, 0.0));
                  break;
               default:
                  HybridStun.release(living);
                  Targeting.launch(living, run.flat.scale((double)ctx.knockback()).add(0.0, 0.45, 0.0));
            }
         }
      }

      if (any) {
         run.landed++;
      }

      BlockImpactService.push(
         ctx, ImpactShape.sphere(at, 1.6), n == 2 ? new Vec3(0.0, 1.0, 0.0) : run.flat, n == 3 ? 1.0 : 0.5, PhysicsProfile.Impact.DIRECTIONAL
      );
      ServerFx.send(player, SeiryuFx.RUSH_CLAW, at, run.flat, run.veiled ? 1.0F : 0.0F, ctx.seed() + (long)n, n);
   }

   private void finisher(AbilityContext ctx, ThunderclawRushAbility.Run run) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      Vec3 where = run.target != null ? run.target.position() : player.position().add(run.flat.scale(2.0));
      BlockPos ground = TerrainDestruction.findSurface(level, Mth.floor(where.x), Mth.floor(where.y), Mth.floor(where.z), 2, 6);
      Vec3 at = ground != null ? Vec3.atBottomCenterOf(ground.above()) : where;
      float radius = (float)ctx.extra("finisherRadius");
      float damage = HybridCombat.hit(ctx, ctx.extra("finisherWeight"));

      for (Entity e : Targeting.inRadius(level, at.add(0.0, 1.0, 0.0), (double)radius, x -> Targeting.canHurt(player, x))) {
         if (HybridCombat.strike(ctx, e, damage) && e instanceof LivingEntity living) {
            HybridStun.apply(living, 10);
            Targeting.launch(living, e.position().subtract(at).multiply(1.0, 0.0, 1.0).normalize().scale(0.5).add(0.0, 0.6, 0.0));
         }
      }

      BlockImpactService.push(ctx, ImpactShape.sphere(at, (double)radius), new Vec3(0.0, -1.0, 0.0), 1.0, PhysicsProfile.Impact.RADIAL);
      SeiryuCombat.crater(ctx, at.add(0.0, -0.5, 0.0), (double)radius * 0.5, (int)ctx.extra("craterBlocks"), 0.6);
      ServerFx.send(player, SeiryuFx.RUSH_FINISHER, at, Vec3.ZERO, radius, ctx.seed(), 0);
      ServerFx.shake(level, at, 0.8F, 40);
   }

   private static final class Run {
      @Nullable
      LivingEntity target;
      Vec3 start = Vec3.ZERO;
      Vec3 flat = new Vec3(0.0, 0.0, 1.0);
      @Nullable
      Vec3 stand;
      int arrival = -1;
      int landed;
      boolean veiled;
   }
}
