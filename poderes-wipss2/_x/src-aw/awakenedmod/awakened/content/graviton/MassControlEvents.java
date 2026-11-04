package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

@EventBusSubscriber(
   modid = "awakened"
)
public final class MassControlEvents {
   static final float LIGHT3_KNOCKBACK = 2.0F;
   static final double SHOCK_MIN_FALL = 5.0;
   static final double SHOCK_RADIUS = 3.5;
   private static final float SHOCK_DAMAGE = 2.0F;
   private static final float SHOCK_DAMAGE_PER_BLOCK = 0.25F;
   static final String SHOCK_BALANCE_KEY = "MassControlShockwave";
   private static final float SHOCK_DAMAGE_MAX = 6.0F;
   private static final double SHOCK_PUSH = 0.7;
   private static final double SHOCK_LIFT = 0.3;
   private static final int THUMP_MIN_TIER = 2;
   private static final double THUMP_MIN_FALL = 2.0;

   private MassControlEvents() {
   }

   @SubscribeEvent
   public static void onKnockBack(LivingKnockBackEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && MassControl.activeTier(player) == -3) {
         event.setStrength(event.getStrength() * 2.0F);
      }
   }

   @SubscribeEvent
   public static void onFall(LivingFallEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         int tier = MassControl.activeTier(player);
         if (tier >= 2 && !GravityWellAbility.shapesFall(player)) {
            double distance = (double)event.getDistance();
            if (tier == 3 && distance >= 5.0) {
               shockwave(player, distance);
            } else if (distance >= 2.0) {
               MassControlFx.thump(player.serverLevel(), player);
            }
         }
      }
   }

   private static void shockwave(ServerPlayer player, double distance) {
      ServerLevel level = player.serverLevel();
      Vec3 feet = player.position();
      float damage = MasteryScaling.configScaledDamage((float)Math.min(6.0, 2.0 + (distance - 5.0) * 0.25), "MassControlShockwave");
      List<LivingEntity> foes = level.getEntitiesOfClass(
         LivingEntity.class,
         player.getBoundingBox().inflate(3.5, 1.5, 3.5),
         e -> e != player && e.isAlive() && !e.isSpectator() && e.distanceToSqr(feet) <= 12.25 && Harm.allowed(player, e) && !Foes.fightsFor(player, e)
      );

      for (LivingEntity foe : foes) {
         Dmg.hurtNoKnockback(foe, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), damage);
         Vec3 away = new Vec3(foe.getX() - feet.x, 0.0, foe.getZ() - feet.z);
         away = away.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : away.normalize();
         Harm.push(player, foe, away.x * 0.7, 0.3, away.z * 0.7);
      }

      if (!foes.isEmpty()) {
         MasterySystem.award(player, MasteryReason.HIT, foes);
      }

      MassControlFx.landing(level, player, 3.5);
   }
}
