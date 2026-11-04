package com.awakenedmod.awakened.content.teleporter;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.power.Power;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class TeleporterPower extends Power {
   public TeleporterPower() {
      super("teleporter", List.of(new BlinkAbility(), new PhaseStrikeAbility(), new SmokeVeilAbility(), new VanishingDropAbility(), new PhantomFlurryAbility()));
   }

   @Override
   public int themeColor() {
      return -5211393;
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public boolean rollable() {
      return false;
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Fx.burst(level, ParticleTypes.PORTAL, player.getEyePosition(), 90, 0.9, 0.6);
      Fx.burst(level, ParticleTypes.WITCH, player.position().add(0.0, 1.0, 0.0), 25, 0.6, 0.2);
      Fx.sound(level, player.position(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.8F);
   }
}
