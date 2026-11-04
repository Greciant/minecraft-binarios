package com.devilfruits.fruits.kilo_kilo.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.fruits.kilo_kilo.KiloAbilities;
import com.devilfruits.fruits.kilo_kilo.KiloKilo;
import com.devilfruits.fruits.kilo_kilo.weight.BodyWeight;
import com.devilfruits.fruits.kilo_kilo.weight.HeavyBody;
import com.devilfruits.fruits.kilo_kilo.weight.WeightCurves;
import com.devilfruits.fruits.kilo_kilo.weight.WeightManager;
import net.minecraft.server.level.ServerPlayer;

public class KiloPressAbility extends Ability {
   public KiloPressAbility() {
      super(
         KiloKilo.id("ten_thousand_kilo_press"),
         CastType.TOGGLE,
         AbilityStats.builder()
            .energy(6)
            .cooldown(10)
            .mastery(1)
            .damage(0.0)
            .range(1.0)
            .knockback(0.0)
            .blockDamage(true)
            .extra("pressMinHeight", 6.0, 1.0, 64.0, "Height above the ground (blocks) from which switching on in the air starts the plunge.")
            .extra("pressSwitchSeconds", 0.35, 0.05, 5.0, "Seconds the weight takes to 10,000 kg in the plunge.")
            .extra("pressEnergy", 12.0, 0.0, 200.0, "Extra energy of the plunge.")
            .extra("pressCooldown", 60.0, 0.0, 1200.0, "Ticks before the next plunge.")
            .extra("maxBlocksPerTick", 24.0, 0.0, 512.0, "Blocks a body smashing through breaks per tick at most.")
            .extra("maxBlocksPerCast", 160.0, 0.0, 4096.0, "Blocks one fall or run of the heavy body breaks at most.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults())
         .impact(PhysicsProfile.Impact.RADIAL)
         .reaction(PhysicsProfile.Reaction.LANDING)
         .blockDamage(1.0)
         .manual()
         .build();
   }

   @Override
   public void onToggle(AbilityContext ctx, boolean active) {
      if (active) {
         ServerPlayer player = ctx.player();
         AbilityManager.stopToggle(player, KiloAbilities.ONE_KILO);
         PlayerFruitData data = ctx.data();
         long now = player.level().getGameTime();
         double minHeight = ctx.extra("pressMinHeight");
         boolean airborne = !player.onGround() && !player.isInWater() && !player.isPassenger() && !player.getAbilities().flying;
         if (airborne && !data.isOnCooldown(HeavyBody.COMBO, now) && HeavyBody.heightAboveGround(player, minHeight + 0.5) >= minHeight) {
            BodyWeight w = KiloKilo.weight(player);
            if (!AbilityManager.freeCast(player, data)) {
               data.setEnergy((float)Math.max(0.0, (double)data.energy() - ctx.extra("pressEnergy")));
            }

            data.startCooldown(HeavyBody.COMBO, now, (int)ctx.extra("pressCooldown"));
            WeightManager.aim(player, w, 2, WeightCurves.rateBetween(w.kg(), 10000.0, ctx.extra("pressSwitchSeconds")), true);
            HeavyBody.startPlunge(player, w);
         } else {
            ServerFx.animate(player, ctx.animation(KiloKilo.id("heavy_cast")));
         }
      }
   }
}
