package com.awakenedmod.awakened.content.gamma;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ThunderclapAbility extends Ability {
   private static final float COST = 15.0F;
   private static final double BASE_RADIUS = 10.0;
   private static final float BASE_DAMAGE = 6.0F;
   private static final double VFX_REACH = 0.75;
   private static final double FRONT_DOT = 0.12;
   private static final double KNOCKBACK = 0.9;
   private static final double KNOCKUP = 0.4;
   private static final int CLAP_BEAT = 8;

   public ThunderclapAbility() {
      super("gamma_thunderclap", 15.0F, 2);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(6.0).boostsRange().build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      float rage = Rage.rage(PowerService.data(player));
      float factor = 0.6F + rage;
      double reach = 10.0 * (double)factor;
      double visualReach = reach * 0.75;
      ServerLevel level = player.serverLevel();
      GammaBeats.schedule(player, 8, () -> {
         GammaFx.thunderclap(level, player, visualReach, rage);
         this.clap(player, reach, factor, rage);
      });
      return true;
   }

   private void clap(ServerPlayer player, double reach, float factor, float rage) {
      if (player.isAlive()) {
         ServerLevel level = player.serverLevel();
         Vec3 origin = player.position();
         Vec3 flatLook = flatLook(player);
         float damage = MasteryScaling.scaledDamage(6.0F, player, this.balanceKey(), factor - 1.0F);
         int hits = 0;
         LivingEntity threat = null;

         for (LivingEntity victim : Harm.only(player, AimUtil.livingAround(player, origin.add(0.0, (double)player.getBbHeight() * 0.5, 0.0), reach))) {
            Vec3 to = victim.position().subtract(origin);
            Vec3 flatTo = new Vec3(to.x, 0.0, to.z);
            if (!(flatTo.lengthSqr() > 1.0E-4) || !(flatTo.normalize().dot(flatLook) < 0.12)) {
               Dmg.hurt(victim, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), damage);
               Vec3 push = flatTo.lengthSqr() < 1.0E-4 ? flatLook : flatTo.normalize();
               double kb = 0.9 * (double)factor;
               victim.push(push.x * kb, 0.4, push.z * kb);
               victim.hurtMarked = true;
               hits++;
               if (threat == null && Foes.counts(player, victim)) {
                  threat = victim;
               }
            }
         }

         if (hits >= 2) {
            MasterySystem.award(player, MasteryReason.HIT, threat);
         }
      }
   }

   private static Vec3 flatLook(ServerPlayer player) {
      Vec3 look = player.getLookAngle();
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      return flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
   }
}
