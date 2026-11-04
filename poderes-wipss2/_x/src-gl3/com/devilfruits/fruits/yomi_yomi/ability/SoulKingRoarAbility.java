package com.devilfruits.fruits.yomi_yomi.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.combat.Stun;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.yomi_yomi.YomiFx;
import com.devilfruits.fruits.yomi_yomi.YomiSounds;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class SoulKingRoarAbility extends KingAbility {
   public SoulKingRoarAbility() {
      super(
         "soul_king_roar",
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(0)
            .cooldown(500)
            .mastery(9)
            .damage(16.0)
            .range(9.3)
            .knockback(0.0)
            .extra("stunTicks", 100.0, 0.0, 400.0, "Ticks the scream stuns everything it reaches (5 s).")
            .extra("pierce", 0.4, 0.0, 1.0, "Share of the scream's damage that armour does not stop.")
            .build()
      );
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      AbilityManager.setBusy(player, 39);
      ServerFx.send(player, YomiFx.KING_ROAR, player.position(), ctx.horizontalLook(), ctx.range(), ctx.seed(), 0);
      ServerScheduler.schedule(level, 0, age -> {
         if (!present(player, level)) {
            return true;
         } else {
            if (age == 25) {
               scream(ctx);
            }

            return age >= 25;
         }
      });
   }

   public static List<Entity> scream(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      Vec3 mouth = player.position().add(0.0, 3.5, 0.0);
      Vec3 center = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
      List<Entity> hit = new ArrayList<>(Targeting.inRadius(level, center, (double)ctx.range(), ex -> ex != player && Targeting.canHurt(player, ex)));
      float pure = ctx.damage() * (float)ctx.extra("pierce");
      float blunt = ctx.damage() - pure;
      int stun = (int)ctx.extra("stunTicks");

      for (Entity e : hit) {
         breakShield(e);
         e.invulnerableTime = 0;
         boolean hurt = pure > 0.0F && Targeting.hurt(player, e, pure, YomiYomi.ROAR_DAMAGE);
         e.invulnerableTime = 0;
         hurt |= blunt > 0.0F && ctx.hurt(e, blunt);
         if (hurt && e instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)e;
            if (living.isAlive() && stun > 0) {
               Stun.apply(living, stun);
               if (living instanceof ServerPlayer victim) {
                  ServerFx.send(victim, YomiFx.KING_ROAR_STUN, victim.position(), Vec3.ZERO, (float)stun, ctx.seed(), 0);
               }
            }
         }
      }

      level.playSound(null, mouth.x, mouth.y, mouth.z, YomiSounds.KING_ROAR.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
      level.playSound(null, mouth.x, mouth.y, mouth.z, YomiSounds.RATTLE.get(), SoundSource.PLAYERS, 1.2F, 0.6F);
      return hit;
   }
}
