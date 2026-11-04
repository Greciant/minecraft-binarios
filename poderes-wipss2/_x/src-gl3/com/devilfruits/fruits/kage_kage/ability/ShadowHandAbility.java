package com.devilfruits.fruits.kage_kage.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.kage_kage.KageAim;
import com.devilfruits.fruits.kage_kage.KageFx;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kage_kage.KageTiming;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ShadowHandAbility extends Ability {
   public ShadowHandAbility() {
      super(
         KageKage.id("shadow_hand"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(35)
            .cooldown(400)
            .mastery(6)
            .damage(12.0)
            .range(30.0)
            .knockback(1.2)
            .extra("radius", 4.0, 1.0, 10.0, "Radius of the slap.")
            .extra("slownessSeconds", 2.0, 0.0, 20.0, "Seconds of slowness on everything slapped.")
            .extra("bats", 7.0, 0.0, 16.0, "Bats that fly to the spot before the hand.")
            .extra("handHeight", 5.5, 2.0, 12.0, "Height of the hand in blocks (the look; the radius is the slap).")
            .extra("aimConeDegrees", 6.0, 0.0, 45.0, "Aim assist: a creature this close to the crosshair is slapped where it stands.")
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
         .reaction(PhysicsProfile.Reaction.NONE)
         .blockDamage(0.0)
         .manual()
         .build();
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return KageAim.groundSpot(player, this.config().range(), this.config().extra("aimConeDegrees")) == null
         ? Component.translatable("message.devilfruits.kage_kage.no_ground")
         : null;
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      Vec3 spot = KageAim.groundSpot(player, (double)ctx.range(), ctx.extra("aimConeDegrees"));
      if (spot != null) {
         Vec3 fall = spot.subtract(player.position()).multiply(1.0, 0.0, 1.0);
         fall = fall.lengthSqr() < 1.0E-4 ? ctx.horizontalLook() : fall.normalize();
         int run = KageTiming.batRun(Math.sqrt(player.distanceToSqr(spot)));
         ServerFx.send(player, KageFx.HAND, spot, fall.scale(ctx.extra("handHeight")), (float)ctx.extra("bats"), ctx.seed(), run);
         ServerFx.animate(player, ctx.animation(KageKage.id("crouch_ground")));
         ServerScheduler.later(ctx.level(), KageTiming.handImpact(run), () -> slap(ctx, spot, fall));
      }
   }

   private static void slap(AbilityContext ctx, Vec3 spot, Vec3 fall) {
      ServerPlayer player = ctx.player();
      if (!player.isRemoved()) {
         double radius = ctx.extra("radius");
         float damage = ctx.damage();
         double kb = (double)ctx.knockback();
         int slow = (int)Math.round(ctx.extra("slownessSeconds") * 20.0);

         for (LivingEntity e : ShadowTrapAbility.inPool(player, spot, radius)) {
            e.invulnerableTime = 0;
            if (ctx.hurt(e, damage)) {
               Vec3 away = e.position().subtract(spot).multiply(1.0, 0.0, 1.0);
               away = away.lengthSqr() < 0.04 ? fall : away.normalize();
               Targeting.push(e, away.scale(kb * 0.7).add(0.0, 0.35, 0.0));
               if (slow > 0) {
                  e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slow, 1));
               }
            }
         }

         if (PhysicsWorld.active()) {
            BlockImpactService.strike(ctx, ImpactShape.sphere(spot, radius), new Vec3(0.0, -1.0, 0.0), 0.4);
         }
      }
   }
}
