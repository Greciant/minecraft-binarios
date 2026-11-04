package com.devilfruits.fruits.bomu_bomu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ExplosionProfile;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.fruits.bomu_bomu.BomuAbilities;
import com.devilfruits.fruits.bomu_bomu.BomuBomu;
import com.devilfruits.fruits.bomu_bomu.BomuFx;
import com.devilfruits.fruits.bomu_bomu.entity.BoogerBomb;
import com.devilfruits.fruits.bomu_bomu.explosion.ExplosionSpec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class NoseFancyCannonAbility extends Ability {
   public NoseFancyCannonAbility() {
      super(
         BomuBomu.id("nose_fancy_cannon"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(5)
            .cooldown(300)
            .mastery(2)
            .damage(5.0)
            .range(1.8)
            .knockback(0.5)
            .blockDamage(true)
            .extra("shotSpeed", 1.5, 0.2, 5.0, "Launch speed (blocks per tick).")
            .extra("fuseTicks", 3.0, 0.0, 5.0, "Ticks a booger clings before it goes off (0 = on contact).")
            .extra("maxPlayerDamage", 6.0, 0.0, 200.0, "Most damage one blast deals to a player (PvP cap).")
            .build()
      );
   }

   @Override
   public boolean coatable() {
      return true;
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).impact(PhysicsProfile.Impact.RADIAL).force(0.5).blockDamage(0.35).manual().build();
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      Vec3 nose = player.getEyePosition().add(0.0, -0.12, 0.0).add(ctx.horizontalLook().scale(0.25));
      Vec3 velocity = ctx.aimFrom(nose).scale(ctx.extra("shotSpeed")).add(player.getDeltaMovement().multiply(1.0, 0.0, 1.0).scale(0.5));
      BoogerBomb.shoot(player, nose, velocity, spec(ctx), Mth.clamp((int)ctx.extra("fuseTicks"), 0, 5));
      ServerFx.send(player, BomuFx.CANNON_FIRE, nose, velocity, 1.0F, player.getRandom().nextLong(), 0);
      ServerFx.animate(player, ctx.animation(BomuBomu.id("nose_flick")));
   }

   public static ExplosionSpec spec(AbilityContext ctx) {
      float radius = ctx.range();
      return new ExplosionSpec(radius, radius, 0.6F, ctx.damage(), ctx.knockback(), 0.0F, ExplosionProfile.SMALL, BomuAbilities.CANNON.id());
   }
}
