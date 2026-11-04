package com.awakenedmod.awakened.content.stormcaller;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.power.Power;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class StormCallerPower extends Power {
   public StormCallerPower() {
      super("storm_caller", List.of(new LightningStrikeAbility(), new TeslaBeamAbility(), new UpdraftAbility(), new ThunderfallAbility(), new TempestAbility()));
   }

   @Override
   public int themeColor() {
      return -7359233;
   }

   @Override
   public int signatureSlot() {
      return 1;
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      StormSupport.strike(level, player, player.position(), 0.0F, 0.0);
      Fx.sound(level, player.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0F, 1.0F);
   }
}
