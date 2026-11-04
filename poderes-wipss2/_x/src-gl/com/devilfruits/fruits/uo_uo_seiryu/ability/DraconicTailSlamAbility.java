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
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class DraconicTailSlamAbility extends Ability {
   public static final int SWEEP_START = 3;
   public static final int SWEEP_END = 8;

   public DraconicTailSlamAbility() {
      super(
         UoUoSeiryu.id("draconic_tail_slam"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(18)
            .cooldown(120)
            .mastery(3)
            .damage(1.2)
            .range(4.5)
            .knockback(2.2)
            .extra("sweepDegrees", 180.0, 30.0, 360.0, "How far round the tail sweeps (degrees, centred on the view).")
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
         .force(4.0)
         .reaction(PhysicsProfile.Reaction.NONE)
         .blockDamage(0.0)
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
      Vec3 facing = ctx.horizontalLook();
      float radius = ctx.range() * Math.max(1.0F, player.getScale() * 0.6F);
      double half = Math.toRadians(ctx.extra("sweepDegrees") * 0.5);
      double base = Math.atan2(facing.z, facing.x);
      Set<Entity> hit = new HashSet<>();
      AbilityManager.setBusy(player, 12);
      ServerFx.animate(player, UoUoSeiryu.anim("hybrid/draconic_tail_slam"));
      ServerFx.send(
         player,
         SeiryuFx.TAIL_SLAM,
         player.position().add(0.0, (double)player.getBbHeight() * 0.3, 0.0),
         facing,
         radius,
         ctx.seed(),
         (int)ctx.extra("sweepDegrees")
      );
      ServerScheduler.schedule(level, 1, age -> {
         if (player.isAlive() && !player.isRemoved() && player.level() == level && UoUoSeiryu.isIn(player, "hybrid")) {
            if (age < 3) {
               return false;
            } else {
               float s = Mth.clamp((float)(age - 3 + 1) / 6.0F, 0.0F, 1.0F);
               double now = base - half + 2.0 * half * (double)s;
               double halfDeg = Math.toDegrees(half);
               Vec3 center = player.position();

               for (Entity e : Targeting.inRadius(level, center.add(0.0, 1.0, 0.0), (double)radius, x -> Targeting.canHurt(player, x) && !hit.contains(x))) {
                  Vec3 to = e.position().subtract(center);
                  double bearing = Math.atan2(to.z, to.x);
                  double off = Mth.wrapDegrees(Math.toDegrees(bearing - base));
                  if (Math.abs(off) <= halfDeg && off + halfDeg <= 2.0 * halfDeg * (double)s + 0.001) {
                     hit.add(e);
                     if (HybridCombat.strike(ctx, e, HybridCombat.hit(ctx)) && e instanceof LivingEntity living) {
                        Vec3 out = new Vec3(to.x, 0.0, to.z).normalize();
                        Vec3 along = new Vec3(-Math.sin(bearing), 0.0, Math.cos(bearing));
                        Targeting.launch(living, out.add(along.scale(0.7)).normalize().scale((double)ctx.knockback()).add(0.0, 0.55, 0.0));
                     }
                  }
               }

               Vec3 tip = center.add(Math.cos(now) * (double)radius * 0.8, 0.8, Math.sin(now) * (double)radius * 0.8);
               BlockImpactService.push(ctx, ImpactShape.sphere(tip, 1.5), new Vec3(-Math.sin(now), 0.0, Math.cos(now)), 0.6, PhysicsProfile.Impact.DIRECTIONAL);
               return age >= 8;
            }
         } else {
            return true;
         }
      });
   }
}
