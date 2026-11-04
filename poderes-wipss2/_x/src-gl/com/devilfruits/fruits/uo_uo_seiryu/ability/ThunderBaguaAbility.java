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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class ThunderBaguaAbility extends Ability {
   @Override
   public boolean coatable() {
      return true;
   }

   public ThunderBaguaAbility() {
      super(
         UoUoSeiryu.id("thunder_bagua"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(25)
            .cooldown(120)
            .mastery(1)
            .damage(10.0)
            .range(4.5)
            .knockback(2.4)
            .blockDamage(true)
            .extra("windupTicks", 8.0, 0.0, 40.0, "Wind-up before the club lands.")
            .extra("coneOpening", 70.0, 10.0, 180.0, "Opening angle of the lightning cone in degrees.")
            .extra("clubBonus", 1.25, 1.0, 5.0, "Damage multiplier while holding a club (Hassaikai).")
            .extra("blocks", 4.0, 0.0, 64.0, "Blocks torn out at the impact point (Hybrid: x4, as cracks).")
            .extra("chainTargets", 4.0, 0.0, 16.0, "Hybrid: how many extra mobs the thunder arcs jump to from each target.")
            .extra("chainRange", 7.0, 1.0, 32.0, "Hybrid: maximum jump distance of a thunder arc.")
            .extra("chainDamage", 0.5, 0.0, 2.0, "Hybrid: damage of an arc relative to the blow.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.DIRECTIONAL).force(1.0).blockDamage(0.5).manual().build();
   }

   @Nullable
   @Override
   public ResourceKey<DamageType> damageType() {
      return UoUoSeiryu.THUNDER_DAMAGE;
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      int windup = (int)ctx.extra("windupTicks");
      AbilityManager.setBusy(player, windup + 6);
      ServerFx.animate(player, ctx.animation(UoUoSeiryu.anim("thunder_bagua")));
      ServerFx.chargeStart(player, this, (float)Math.max(1, windup), ctx.seed());
      ServerScheduler.later(ctx.level(), windup, () -> this.strike(ctx));
   }

   private void strike(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      if (player.isAlive() && !player.isRemoved()) {
         ServerFx.chargeStop(player, this, true);
         ServerLevel level = ctx.level();
         boolean hybrid = "hybrid".equals(ctx.variant());
         float scale = player.getScale();
         Vec3 flat = ctx.horizontalLook();
         Vec3 eye = player.getEyePosition();
         float range = ctx.range();
         float damage = ctx.damage() * SeiryuCombat.clubFactor(player, ctx.extra("clubBonus"));
         Vec3 ahead = player.position().add(flat.scale(2.4 * (double)scale));
         BlockPos ground = TerrainDestruction.findSurface(level, (int)Math.floor(ahead.x), (int)Math.floor(ahead.y), (int)Math.floor(ahead.z), 1, 3);
         Vec3 impact = ground != null ? Vec3.atBottomCenterOf(ground.above()) : ahead;
         Set<Entity> hit = new HashSet<>();
         double cone = Math.toRadians(ctx.extra("coneOpening") * 0.5);
         hit.addAll(Targeting.inCone(level, eye, flat, (double)range * 1.4, cone, e -> Targeting.canHurt(player, e)));
         hit.addAll(Targeting.inRadius(level, impact, (double)range * (hybrid ? 0.9 : 0.6), e -> Targeting.canHurt(player, e)));

         for (Entity e : hit) {
            ctx.hurt(e, damage);
            Vec3 away = e.position().subtract(player.position());
            Vec3 dir = new Vec3(away.x, 0.0, away.z).lengthSqr() < 1.0E-4 ? flat : new Vec3(away.x, 0.0, away.z).normalize();
            Targeting.launch(e, dir.scale((double)ctx.knockback()).add(0.0, 0.45 + (double)ctx.knockback() * 0.12, 0.0));
         }

         for (Entity e : Targeting.inRadius(level, impact, (double)range, ex -> Targeting.canPush(player, ex) && !hit.contains(ex))) {
            Targeting.push(e, e.position().subtract(impact).normalize().scale(0.6).add(0.0, 0.25, 0.0));
         }

         int blocks = (int)ctx.extra("blocks") * (hybrid ? 4 : 1);
         SeiryuCombat.crater(ctx, impact.add(0.0, -0.5, 0.0), hybrid ? 3.5 : 1.6, blocks, hybrid ? 0.55 : 0.35);
         ImpactShape struck = ImpactShape.union(
            List.of(ImpactShape.cone(eye, flat, (double)range * 1.4, cone), ImpactShape.sphere(impact, (double)range * (hybrid ? 0.9 : 0.6)))
         );
         BlockImpactService.push(ctx, struck, flat, hybrid ? 1.3 : 1.0);
         BlockImpactService.mark(player, ImpactShape.sphere(impact, 2.5), flat.scale(-1.0), BlockImpactService.Decal.LIGHTNING, hybrid ? 3.0F : 2.0F);
         BlockImpactService.ignite(ctx, ImpactShape.sphere(impact, 2.0), 0.15);
         ServerFx.send(player, SeiryuFx.THUNDER_BAGUA, impact, flat, range, ctx.seed(), hybrid ? 1 : 0);
         ServerFx.shake(level, impact, hybrid ? 0.9F : 0.45F, hybrid ? 32 : 20);
         if (hybrid) {
            this.chain(ctx, new ArrayList<>(hit), damage * (float)ctx.extra("chainDamage"));
         }
      }
   }

   private void chain(AbilityContext ctx, List<Entity> sources, float damage) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      int jumps = (int)ctx.extra("chainTargets");
      double reach = ctx.extra("chainRange");
      Set<Entity> struck = new HashSet<>(sources);
      int delay = 2;

      for (Entity source : sources) {
         Entity from = source;

         for (int i = 0; i < jumps; i++) {
            Vec3 at = from.getBoundingBox().getCenter();
            Entity next = Targeting.inRadius(level, at, reach, e -> Targeting.canHurt(player, e) && !struck.contains(e))
               .stream()
               .min((ax, b) -> Double.compare(ax.distanceToSqr(at), b.distanceToSqr(at)))
               .orElse(null);
            if (next == null) {
               break;
            }

            struck.add(next);
            Entity a = from;
            ServerScheduler.later(
               level,
               delay,
               () -> {
                  if (next.isAlive()) {
                     ServerFx.sendNear(
                        level,
                        SeiryuFx.CHAIN_ARC,
                        a.getId(),
                        a.getBoundingBox().getCenter(),
                        next.getBoundingBox().getCenter(),
                        1.0F,
                        ctx.seed() + (long)next.getId(),
                        0,
                        64.0
                     );
                     ctx.hurt(next, damage);
                  }
               }
            );
            delay += 2;
            from = next;
         }
      }
   }
}
