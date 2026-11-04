package com.awakenedmod.awakened.content.cryokinetic;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

public final class GlacialSlideAbility extends Ability {
   private static final int UNLOCK = 7;
   private static final float COST = 6.0F;
   private static final float GLIDE_COST = 0.8F;
   private static final double SPEED = 0.72;
   private static final double BASE_SINK = 0.05;
   private static final double MAX_CLIMB = 0.22;
   private final Map<UUID, Boolean> gliding = this.activeState();

   public GlacialSlideAbility() {
      super("glacial_slide", 6.0F, 7);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (Boolean.TRUE.equals(this.gliding.get(player.getUUID()))) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         this.gliding.put(player.getUUID(), Boolean.TRUE);
         ServerLevel level = player.serverLevel();
         CryoFx.crystals(level, player.position(), 24, 0.6);
         CryoFx.glints(level, player.position(), 6, 0.5);
         Fx.sound(level, player.position(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
         glide(player);
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      if (Boolean.TRUE.equals(this.gliding.get(player.getUUID()))) {
         PowerData data = PowerService.data(player);
         long gameTime = player.serverLevel().getGameTime();
         StaminaSystem.suppressRegen(data, gameTime);
         if (!data.infiniteStamina()) {
            float drain = this.billed(0.8F);
            if (data.stamina() < drain) {
               this.stop(player);
               return;
            }

            StaminaSystem.drainSustained(data, gameTime, drain);
         }

         glide(player);
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
      this.gliding.remove(player.getUUID());
      super.clearPlayer(player);
   }

   private void stop(ServerPlayer player) {
      if (this.gliding.remove(player.getUUID()) != null) {
         PowerService.playAnimation(player, "glacial_slide_end");
         Fx.sound(player.serverLevel(), player.position(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.5F, 1.0F);
         PowerService.sync(player);
      }
   }

   private static void glide(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 look = player.getLookAngle();
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = new Vec3(0.0, 0.0, 1.0);
      }

      horizontal = horizontal.normalize();
      double speed = MasteryScaling.scaled(0.72, player);
      double vy = Mth.clamp(look.y * speed, -speed, 0.22) - 0.05;
      player.setDeltaMovement(horizontal.x * speed, vy, horizontal.z * speed);
      player.hurtMarked = true;
      player.resetFallDistance();
      player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, false, false));
      CryoFx.iceTrail(level, player.position());
   }
}
