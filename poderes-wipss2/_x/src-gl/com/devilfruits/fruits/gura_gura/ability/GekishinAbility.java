package com.devilfruits.fruits.gura_gura.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.AimHelper;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.StructureForces;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.gura_gura.GuraFx;
import com.devilfruits.fruits.gura_gura.GuraGura;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class GekishinAbility extends Ability {
   @Override
   public boolean coatable() {
      return true;
   }

   public GekishinAbility() {
      super(
         GuraGura.id("gekishin"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(22)
            .cooldown(90)
            .mastery(1)
            .damage(24.0)
            .range(4.5)
            .knockback(3.0)
            .extra("windupTicks", 5.0, 0.0, 40.0, "Wind-up before the punch lands.")
            .extra("launchUp", 0.55, 0.0, 3.0, "Upward component of the launch.")
            .extra("impactDamage", 9.0, 0.0, 200.0, "Damage of the impact ring where the target lands.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults())
         .impact(PhysicsProfile.Impact.DIRECTIONAL)
         .force(1.0)
         .reaction(PhysicsProfile.Reaction.DASH)
         .blockDamage(0.3)
         .manual()
         .build();
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      int windup = (int)ctx.extra("windupTicks");
      AbilityManager.setBusy(player, windup + 4);
      ServerFx.animate(player, ctx.animation(GuraGura.anim("gekishin")));
      ServerFx.chargeStart(player, this, (float)Math.max(1, windup), ctx.seed());
      ServerScheduler.later(ctx.level(), windup, () -> this.strike(ctx));
   }

   private void strike(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      if (player.isAlive() && !player.isRemoved()) {
         ServerLevel level = ctx.level();
         Vec3 eye = player.getEyePosition();
         Vec3 look = AimHelper.look(player);
         Vec3 flat = ctx.horizontalLook();
         Vec3 lunge = flat.scale(0.75).add(0.0, Math.max(player.getDeltaMovement().y, 0.05), 0.0);
         StructureForces.takeoff(player, lunge);
         Targeting.launch(player, lunge);
         List<Entity> candidates = Targeting.inCone(level, eye, look, (double)ctx.range() + 1.0, Math.toRadians(28.0), e -> Targeting.canHurt(player, e));
         Entity target = candidates.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(eye))).orElse(null);
         BlockImpactService.strike(ctx, ImpactShape.cone(eye, look, (double)ctx.range() + 1.0, Math.toRadians(28.0)), look, target == null ? 0.6 : 1.0);
         QuakeDeflect.sweep(player, ImpactShape.cone(eye, look, (double)ctx.range() + 1.0, Math.toRadians(35.0)), look);
         if (target != null) {
            Vec3 hitPos = target.getBoundingBox().getCenter();
            ctx.hurt(target, ctx.damage());
            Vec3 launch = flat.scale((double)ctx.knockback()).add(0.0, ctx.extra("launchUp") + Math.max(0.0, look.y) * 1.2, 0.0);
            Targeting.launch(target, launch);
            ServerFx.send(player, GuraFx.GEKISHIN_HIT, hitPos, look, 1.0F, ctx.seed(), target.getId());
            ServerFx.send(target, GuraFx.LAUNCH_TRAIL, hitPos, launch.normalize(), 1.0F, ctx.seed(), 40);
            float impactDamage = (float)ctx.extra("impactDamage") * ctx.damage() / (float)ctx.config().damage();
            ServerScheduler.schedule(level, 2, age -> {
               if (target.isAlive() && target.level() == level) {
                  boolean landed = age > 2 && (target.onGround() || target.horizontalCollision || target.isInWater());
                  if (!landed && age <= 40) {
                     return false;
                  } else {
                     Vec3 at = target.position();
                     ServerFx.sendNear(level, GuraFx.IMPACT_RING, target.getId(), at, Vec3.ZERO, 2.5F, ctx.seed(), 0, 64.0);

                     for (Entity e : Targeting.inRadius(level, at, 2.5, x -> Targeting.canHurt(player, x))) {
                        ctx.hurt(e, impactDamage);
                     }

                     return true;
                  }
               } else {
                  return true;
               }
            });
         } else {
            Vec3 hitPos = eye.add(look.scale(2.0));
            ServerFx.send(player, GuraFx.GEKISHIN_HIT, hitPos, look, 0.5F, ctx.seed(), -1);

            for (Entity e : Targeting.inCone(level, eye, look, 4.0, Math.toRadians(35.0), x -> Targeting.canPush(player, x))) {
               ctx.hurt(e, ctx.damage() * 0.35F);
               Targeting.launch(e, look.scale((double)ctx.knockback() * 0.5).add(0.0, 0.3, 0.0));
            }
         }
      }
   }
}
