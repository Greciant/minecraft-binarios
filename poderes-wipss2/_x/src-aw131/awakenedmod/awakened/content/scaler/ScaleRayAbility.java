package com.awakenedmod.awakened.content.scaler;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Bosses;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ScaleRayAbility extends Ability {
   private static final int UNLOCK = 9;
   private static final float COST = 20.0F;
   private static final double RANGE = 24.0;
   private static final int BASE_DURATION_TICKS = 300;
   private static final int BEAM_LIFETIME = 6;
   private static final double BEAM_WIDTH = 0.14;

   public ScaleRayAbility() {
      super("scale_ray", 20.0F, 9);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().durationTicks(300).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      return this.shoot(player, 1, false);
   }

   @Override
   public boolean usesSneakActivate() {
      return true;
   }

   @Override
   public void onSecondaryAction(ServerPlayer player, int action) {
      if (action == 3) {
         this.shoot(player, -1, true);
      }
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public void serverTick(ServerPlayer player) {
      RayEffects.tick(player);
   }

   private boolean shoot(ServerPlayer player, int dir, boolean selfCharge) {
      LivingEntity target = AimUtil.aimedLivingEntity(player, 24.0);
      if (target == null) {
         return this.fail(player, "awakened.scaler.no_target", new Object[0]);
      } else if (Bosses.is(target)) {
         return this.fail(player, "awakened.scaler.boss_immune", new Object[0]);
      } else if (dir < 0 && !Harm.allowed(player, target)) {
         return this.fail(player, "awakened.feedback.teammate", new Object[0]);
      } else {
         PowerData data = PowerService.data(player);
         float cost = this.billed(20.0F);
         if (selfCharge && !StaminaSystem.canAfford(data, cost)) {
            return this.fail(player, "awakened.scaler.tired", new Object[0]);
         } else {
            int duration = Math.round(MasteryScaling.scaled(300.0F, player));
            String refusal = RayEffects.apply(player, target, dir, duration);
            if (refusal != null) {
               return this.fail(player, refusal, new Object[0]);
            } else {
               if (selfCharge && !data.infiniteStamina()) {
                  data.setStamina(data.stamina() - cost);
                  data.setLastSpendTime(player.serverLevel().getGameTime());
               }

               ServerLevel level = player.serverLevel();
               Vec3 from = AimUtil.handPosition(player);
               Vec3 to = target.position().add(0.0, (double)target.getBbHeight() * 0.55, 0.0);
               Vfx.beam(level, from, to, 0.14, 6021288, 6);
               Vfx.ring(level, target.position().add(0.0, 0.1, 0.0), 0.6 + (double)target.getBbWidth() * 0.8, dir > 0 ? 6021288 : 12582880, 10);
               Fx.burst(level, ParticleTypes.CRIT, to, 10, (double)target.getBbWidth() * 0.5, 0.08);
               Fx.sound(level, target.position(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.9F, dir > 0 ? 0.6F : 1.6F);
               PowerService.playAnimation(player, "scaler_ray");
               MasterySystem.award(player, MasteryReason.HIT, target);
               return true;
            }
         }
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      RayEffects.clearCaster(player);
      super.clearPlayer(player);
   }
}
