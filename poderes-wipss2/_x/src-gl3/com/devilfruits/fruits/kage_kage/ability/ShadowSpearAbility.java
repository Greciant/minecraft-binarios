package com.devilfruits.fruits.kage_kage.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.combat.Stun;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.fruits.kage_kage.KageAim;
import com.devilfruits.fruits.kage_kage.KageFx;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kage_kage.KageTiming;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ShadowSpearAbility extends Ability {
   public ShadowSpearAbility() {
      super(
         KageKage.id("shadow_spear"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(18)
            .cooldown(200)
            .mastery(2)
            .damage(9.0)
            .range(30.0)
            .knockback(0.0)
            .extra("stunSeconds", 2.5, 0.0, 10.0, "Seconds the impaled target is stunned and held.")
            .extra("bats", 4.0, 0.0, 12.0, "Bats that run to the target before the spear.")
            .extra("aimConeDegrees", 6.0, 0.0, 45.0, "Aim assist: half-angle of the cone round the crosshair a target may stand in.")
            .build()
      );
   }

   @Override
   public boolean coatable() {
      return true;
   }

   @Nullable
   private LivingEntity target(ServerPlayer player) {
      return KageAim.aimed(player, this.config().range(), this.config().extra("aimConeDegrees"));
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return this.target(player) == null ? Component.translatable("message.devilfruits.kage_kage.no_target") : null;
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      LivingEntity target = this.target(player);
      if (target != null) {
         int run = KageTiming.spearRun(Math.sqrt(player.distanceToSqr(target)));
         int stun = (int)Math.round(ctx.extra("stunSeconds") * 20.0);
         float damage = ctx.damage();
         ServerFx.send(player, KageFx.SPEAR, target.position(), new Vec3((double)run, (double)stun, 0.0), (float)ctx.extra("bats"), ctx.seed(), target.getId());
         ServerFx.animate(player, ctx.animation(KageKage.id("point_thrust")));
         ServerScheduler.later(ctx.level(), KageTiming.spearPierce(run), () -> pierce(ctx, target, damage, stun));
      }
   }

   private static void pierce(AbilityContext ctx, LivingEntity target, float damage, int stun) {
      ServerPlayer player = ctx.player();
      if (target.isAlive() && !target.isRemoved() && target.level() == player.level() && !player.isRemoved()) {
         target.invulnerableTime = 0;
         if (ctx.hurt(target, damage)) {
            Stun.apply(target, stun);
         }
      }
   }
}
