package com.awakenedmod.awakened.content.teleporter;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.TeleportUtil;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class SmokeVeilAbility extends Ability {
   private static final float COST = 16.0F;
   private static final int UNLOCK = 5;
   private static final double ESCAPE_RANGE = 9.0;
   private static final int MAX_DROP = 4;
   private static final double EFFECT_RADIUS = 4.0;
   private static final int EFFECT_TICKS = 80;
   private static final int IFRAME_TICKS = 10;

   public SmokeVeilAbility() {
      super("smoke_veil", 16.0F, 5);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 origin = player.position();
      Vec3 eye = player.getEyePosition();
      Vec3 center = origin.add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
      PowerService.playAnimation(player, "smoke_veil");
      TeleFx.ghost(level, player);
      Fx.burst(level, ParticleTypes.LARGE_SMOKE, eye, 60, 0.8, 0.05);
      Fx.burst(level, ParticleTypes.SMOKE, eye, 40, 1.0, 0.02);
      TeleFx.rift(level, center);

      for (LivingEntity foe : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4.0), e -> e != player && e.isAlive())) {
         Harm.effect(player, foe, new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
         Harm.effect(player, foe, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
      }

      Vec3 lookHorizontal = TeleportSupport.horizontalDir(origin, origin.add(player.getLookAngle()));
      Vec3 away = lookHorizontal.scale(-1.0);
      Vec3 destination = TeleportUtil.findSafeLanding(player, origin, away, 9.0, 0.5, 2.0, 4);
      if (destination != null) {
         player.teleportTo(destination.x, destination.y, destination.z);
         player.resetFallDistance();
         player.invulnerableTime = Math.max(player.invulnerableTime, 10);
         TeleportSupport.puff(level, player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0));
      }

      if (player.getHealth() <= 6.0F) {
         MasterySystem.award(player, MasteryReason.SURVIVAL);
      }

      return true;
   }
}
