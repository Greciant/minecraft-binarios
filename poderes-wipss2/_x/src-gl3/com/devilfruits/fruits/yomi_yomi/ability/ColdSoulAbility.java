package com.devilfruits.fruits.yomi_yomi.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.yomi_yomi.YomiFx;
import com.devilfruits.fruits.yomi_yomi.YomiSounds;
import com.devilfruits.fruits.yomi_yomi.frost.YomiFrost;
import com.devilfruits.fruits.yomi_yomi.soul.SoulProjection;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ColdSoulAbility extends SoulSkill {
   public ColdSoulAbility() {
      super(
         "cold_soul",
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(16)
            .cooldown(160)
            .mastery(1)
            .damage(2.5)
            .range(8.0)
            .knockback(0.0)
            .extra("slowTicks", 100.0, 0.0, 400.0, "Ticks of frostbite on what the cold wave reached (5 s).")
            .extra("slowLevel", 6.0, 1.0, 9.0, "Frostbite level of the wave: 10 % slower movement and attacks per level.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.NONE;
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      int[] pulses = YomiTiming.COLD_SOUL_PULSES;
      AbilityManager.setBusy(player, pulses[pulses.length - 1] + 1);
      ServerFx.send(player, YomiFx.COLD_SOUL, player.position(), Vec3.ZERO, ctx.range(), ctx.seed(), pulses[0]);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), YomiSounds.CHILL_BURST.get(), SoundSource.PLAYERS, 0.8F, 0.7F);
      ServerScheduler.schedule(level, 0, age -> {
         if (player.isAlive() && !player.isRemoved() && player.level() == level && SoulProjection.active(player)) {
            for (int i = 0; i < pulses.length; i++) {
               if (age == pulses[i]) {
                  pulse(ctx, (float)(i + 1) / (float)pulses.length);
               }
            }

            return age >= pulses[pulses.length - 1];
         } else {
            return true;
         }
      });
   }

   public static List<Entity> pulse(AbilityContext ctx, float reach) {
      ServerPlayer player = ctx.player();
      Vec3 center = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
      double radius = (double)(ctx.range() * reach);
      List<Entity> hit = Targeting.inRadius(ctx.level(), center, radius, ex -> ex != player && Targeting.canHurt(player, ex));
      int ticks = (int)ctx.extra("slowTicks");
      int lvl = (int)ctx.extra("slowLevel");

      for (Entity e : hit) {
         e.invulnerableTime = 0;
         if (ctx.hurt(e, ctx.damage()) && e instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)e;
            if (living.isAlive()) {
               YomiFrost.cold(living, player, ticks, lvl);
               YomiFrost.addChill(living, player, 8.0F, 70);
            }
         }
      }

      ServerFx.send(player, YomiFx.COLD_SOUL_PULSE, center, Vec3.ZERO, (float)radius, ctx.seed(), hit.size());
      ctx.level().playSound(null, center.x, center.y, center.z, YomiSounds.FREEZE.get(), SoundSource.PLAYERS, 0.7F + reach * 0.4F, 1.3F - reach * 0.4F);
      return hit;
   }
}
