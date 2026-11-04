package com.devilfruits.fruits.horo_horo.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.horo_horo.HollowFlight;
import com.devilfruits.fruits.horo_horo.HoroFx;
import com.devilfruits.fruits.horo_horo.HoroHoro;
import com.devilfruits.fruits.horo_horo.HoroSounds;
import com.devilfruits.fruits.horo_horo.negative.Negative;
import com.devilfruits.fruits.kage_kage.KageAim;
import com.devilfruits.registry.DFAttachments;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class NegativeHollowAbility extends Ability {
   public static final int EMERGE_TICKS = 6;
   private static final int MAX_FLIGHT = 80;

   public NegativeHollowAbility() {
      super(
         HoroHoro.id("negative_hollow"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(18)
            .cooldown(160)
            .mastery(1)
            .damage(2.0)
            .range(24.0)
            .knockback(0.0)
            .extra("negativeSeconds", 4.0, 0.5, 30.0, "Seconds the target stays in Negative (mastery adds up to a third more).")
            .extra("speed", 1.3, 0.2, 4.0, "Blocks per tick the long ghost flies.")
            .extra("aimConeDegrees", 8.0, 0.0, 45.0, "Aim assist: half-angle of the cone round the crosshair a target may stand in.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.NONE;
   }

   @Nullable
   private LivingEntity aimed(ServerPlayer player) {
      double range = this.range(player);
      return KageAim.aimed(player, range, this.config().extra("aimConeDegrees"));
   }

   private double range(ServerPlayer player) {
      return this.config().range() * (double)Mastery.rangeMultiplier(DFAttachments.data(player).mastery());
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return this.aimed(player) == null ? Component.translatable("message.devilfruits.horo_horo.no_target") : null;
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      LivingEntity target = KageAim.aimed(player, (double)ctx.range(), ctx.extra("aimConeDegrees"));
      if (target != null) {
         ServerLevel level = ctx.level();
         Vec3 hand = hand(player);
         double speed = ctx.extra("speed");
         HollowFlight flight = new HollowFlight(hand, ctx.look(), speed, 0.22);
         float damage = ctx.damage();
         int ticks = (int)Math.round(ctx.extra("negativeSeconds") * 20.0 * (1.0 + (double)(ctx.mastery() - 1) / 27.0));
         ServerFx.animate(player, HoroHoro.id("extend_arm"));
         ServerFx.send(player, HoroFx.NEGATIVE_LAUNCH, hand, ctx.look(), (float)speed, ctx.seed(), target.getId());
         level.playSound(null, hand.x, hand.y, hand.z, HoroSounds.GHOST_EMERGE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
         ServerScheduler.schedule(level, 6, age -> {
            if (target.isAlive() && !target.isRemoved() && target.level() == level && age <= 80) {
               if (flight.step(HollowFlight.aimPoint(target)) > (double)target.getBbWidth() * 0.5 + 0.3) {
                  return false;
               } else {
                  ServerFx.send(target, HoroFx.NEGATIVE_HIT, flight.pos, flight.heading, 1.0F, 0L, 0);
                  level.playSound(null, target.getX(), target.getY() + 1.0, target.getZ(), HoroSounds.NEGATIVE_HIT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                  if (player.isAlive() && Targeting.canHurt(player, target)) {
                     if (damage > 0.0F) {
                        ctx.hurt(target, damage);
                     }

                     Negative.apply(target, ticks, player);
                  }

                  return true;
               }
            } else {
               return true;
            }
         });
      }
   }

   public static Vec3 hand(ServerPlayer player) {
      Vec3 look = player.getLookAngle();
      Vec3 right = new Vec3(-look.z, 0.0, look.x).normalize();
      return player.getEyePosition().add(look.scale(0.9)).add(right.scale(0.35)).add(0.0, -0.35, 0.0);
   }
}
