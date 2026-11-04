package com.awakenedmod.awakened.content.chronokinetic;

import com.awakenedmod.awakened.core.AbilityGate;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.TickFreeze;
import com.awakenedmod.awakened.core.power.Power;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class ChronokineticPower extends Power {
   public ChronokineticPower() {
      super("chronokinetic", List.of(new TimeShiftAbility(), new FlickerAbility(), new StasisAbility(), new RewindAbility(), new TimeStopAbility()));
      AbilityGate.register(TickFreeze::isFrozen);
   }

   @Override
   public int themeColor() {
      return -1653942;
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 pos = player.position().add(0.0, 1.0, 0.0);
      ChronoSupport.shimmer(level, pos, 90, 1.0);
      Fx.sound(level, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.4F);
      Fx.sound(level, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.8F);
   }
}
