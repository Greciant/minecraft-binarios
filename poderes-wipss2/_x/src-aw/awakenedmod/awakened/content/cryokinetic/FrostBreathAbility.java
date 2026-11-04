package com.awakenedmod.awakened.content.cryokinetic;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
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
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class FrostBreathAbility extends Ability {
   private static final double RANGE = 5.0;
   private static final double CONE_DOT = 0.55;
   private static final float COST = 4.0F;
   private static final float BREATH_COST = 0.6F;
   private static final int DAMAGE_INTERVAL = 8;
   private static final float DAMAGE = 2.5F;
   private final Map<UUID, Boolean> breathing = this.activeState();

   public FrostBreathAbility() {
      super("frost_nova", 4.0F, 0);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(2.5).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (Boolean.TRUE.equals(this.breathing.get(player.getUUID()))) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         this.breathing.put(player.getUUID(), Boolean.TRUE);
         this.breathe(player);
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      if (Boolean.TRUE.equals(this.breathing.get(player.getUUID()))) {
         PowerData data = PowerService.data(player);
         long gameTime = player.serverLevel().getGameTime();
         StaminaSystem.suppressRegen(data, gameTime);
         if (!data.infiniteStamina()) {
            float drain = this.billed(0.6F);
            if (data.stamina() < drain) {
               this.stop(player);
               return;
            }

            StaminaSystem.drainSustained(data, gameTime, drain);
         }

         this.breathe(player);
         if (player.tickCount % 3 == 0) {
            PowerService.sync(player);
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      this.stop(player);
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      this.breathing.remove(player.getUUID());
      super.clearPlayer(player);
   }

   private void stop(ServerPlayer player) {
      if (this.breathing.remove(player.getUUID()) != null) {
         PowerService.playAnimation(player, "frost_nova_end");
         PowerService.sync(player);
      }
   }

   private void breathe(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();
      CryoFx.frostBreath(level, eye, look, 5.0);
      if (player.tickCount % 7 == 0) {
         Fx.sound(level, player.position(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.4F, 1.4F);
      }

      boolean pulse = player.tickCount % 8 == 0;
      float damage = MasteryScaling.scaledDamage(2.5F, player, this.balanceKey());
      boolean hit = false;
      LivingEntity threat = null;

      for (LivingEntity target : level.getEntitiesOfClass(
         LivingEntity.class, player.getBoundingBox().inflate(5.0), e -> e != player && e.isAlive() && inCone(eye, look, e) && Harm.allowed(player, e)
      )) {
         if (pulse) {
            Frostbite.apply(player, target, 1);
            Dmg.hurt(target, level, Dmg.by(player, DamageTypes.FREEZE), damage);
            target.hurtMarked = true;
            hit = true;
            if (threat == null && Foes.counts(player, target)) {
               threat = target;
            }
         }
      }

      if (pulse && hit) {
         MasterySystem.award(player, MasteryReason.HIT, threat);
      }
   }

   private static boolean inCone(Vec3 eye, Vec3 look, LivingEntity target) {
      Vec3 to = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0).subtract(eye);
      return to.lengthSqr() < 1.0E-4 ? true : to.normalize().dot(look) >= 0.55;
   }
}
