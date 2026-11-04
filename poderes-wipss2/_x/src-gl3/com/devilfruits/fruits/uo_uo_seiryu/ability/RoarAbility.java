package com.devilfruits.fruits.uo_uo_seiryu.ability;

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
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RoarAbility extends Ability {
   public static final int ROAR_DELAY = 6;

   public RoarAbility() {
      super(
         UoUoSeiryu.id("roar"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(30)
            .cooldown(240)
            .mastery(6)
            .damage(4.0)
            .range(16.0)
            .knockback(2.5)
            .extra("stunTicks", 50.0, 0.0, 400.0, "Slowness / weakness duration on hit targets.")
            .extra("coneOpening", 120.0, 10.0, 360.0, "Opening angle of the roar in degrees.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.DIRECTIONAL).force(1.2).blockDamage(0.0).manual().build();
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerFx.animate(player, UoUoSeiryu.anim("dragon/roar"));
      AbilityManager.setBusy(player, 20);
      ServerScheduler.later(ctx.level(), 6, () -> this.roar(ctx));
   }

   private void roar(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      if (player.isAlive() && !player.isRemoved()) {
         ServerLevel level = ctx.level();
         Vec3 mouth = SeiryuCombat.mouth(player);
         Vec3 dir = AimHelper.look(player);
         double range = (double)ctx.range();
         double opening = ctx.extra("coneOpening");
         double half = Math.toRadians(opening * 0.5);
         int stun = (int)ctx.extra("stunTicks");

         for (Entity e : Targeting.inCone(level, mouth, dir, range, half, x -> Targeting.canPush(player, x))) {
            Vec3 center = e.getBoundingBox().getCenter();
            float falloff = (float)Mth.clamp(1.0 - center.distanceTo(mouth) / (range * 1.2), 0.3, 1.0);
            Vec3 away = center.subtract(mouth);
            away = away.lengthSqr() < 1.0E-4 ? dir : dir.scale(0.6).add(away.normalize().scale(0.4)).normalize();
            if (e instanceof LivingEntity living && Targeting.canHurt(player, e)) {
               ctx.hurt(e, ctx.damage() * falloff);
               if (stun > 0) {
                  living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stun, 3), player);
                  living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stun, 1), player);
               }
            }

            Targeting.launch(e, away.scale((double)(ctx.knockback() * falloff)).add(0.0, 0.45 * (double)falloff, 0.0));
         }

         double cos = Math.cos(half);
         AABB box = new AABB(mouth, mouth).inflate(range);

         for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, box, p -> p.getOwner() != player)) {
            Vec3 to = projectile.position().subtract(mouth);
            double dist = to.length();
            if (dist <= range && dist > 1.0E-4 && to.dot(dir) / dist >= cos) {
               projectile.setDeltaMovement(to.normalize().scale(1.6));
               projectile.hurtMarked = true;
            }
         }

         BlockImpactService.push(ctx, ImpactShape.cone(mouth, dir, range, half), dir, 1.0);
         ServerFx.send(player, SeiryuFx.ROAR, mouth, dir, (float)range, ctx.seed(), (int)opening);
         ServerFx.shake(level, mouth, 0.7F, (int)(range * 2.5));
      }
   }
}
