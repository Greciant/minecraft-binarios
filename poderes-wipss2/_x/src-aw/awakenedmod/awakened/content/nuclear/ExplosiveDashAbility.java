package com.awakenedmod.awakened.content.nuclear;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class ExplosiveDashAbility extends Ability {
   private static final int UNLOCK = 3;
   private static final float COST = 6.0F;
   private static final float REPEAT_COST = 3.5F;
   private static final int REPEAT_INTERVAL = 3;
   private static final double DASH_SPEED = 1.05;
   private static final double FLAT_LIFT = 0.22;
   private static final double LOOK_FLAT = 0.3;
   private static final double MAX_SPEED = 2.2;
   private final Map<UUID, Boolean> nextLeft = this.playerState();
   private final Map<UUID, Boolean> holding = this.activeState();

   public ExplosiveDashAbility() {
      super("explosive_dash", 6.0F, 3);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      this.holding.put(player.getUUID(), Boolean.TRUE);
      this.dash(player);
      return true;
   }

   @Override
   public void serverTick(ServerPlayer player) {
      if (Boolean.TRUE.equals(this.holding.get(player.getUUID()))) {
         if (player.tickCount % 3 == 0) {
            PowerData data = PowerService.data(player);
            if (!data.infiniteStamina()) {
               float repeat = this.billed(3.5F);
               if (data.stamina() < repeat) {
                  this.holding.remove(player.getUUID());
                  return;
               }

               StaminaSystem.drainSustained(data, player.serverLevel().getGameTime(), repeat);
               PowerService.sync(player);
            }

            this.dash(player);
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      this.holding.remove(player.getUUID());
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      this.holding.remove(player.getUUID());
      super.clearPlayer(player);
      this.nextLeft.remove(player.getUUID());
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   private void dash(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 look = player.getLookAngle();
      Vec3 vel = player.getDeltaMovement().add(look.scale(1.05));
      if (Math.abs(look.y) < 0.3) {
         vel = vel.add(0.0, 0.22, 0.0);
      }

      if (vel.length() > 2.2) {
         vel = vel.normalize().scale(2.2);
      }

      player.setDeltaMovement(vel);
      player.hurtMarked = true;
      player.resetFallDistance();
      boolean left = this.nextLeft.getOrDefault(player.getUUID(), Boolean.FALSE);
      this.nextLeft.put(player.getUUID(), !left);
      Vec3 behind = AimUtil.handPosition(player, left).subtract(look.scale(0.4));
      NuclearFx.boom(level, behind, 1.3, NuclearFx.Kind.DASH);
      PowerService.playAnimation(player, left ? "explosive_dash_left" : "explosive_dash_right");
      Fx.sound(level, player.position(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7F, 1.5F);
   }
}
