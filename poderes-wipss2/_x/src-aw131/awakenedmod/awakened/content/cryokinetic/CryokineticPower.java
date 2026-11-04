package com.awakenedmod.awakened.content.cryokinetic;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Power;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class CryokineticPower extends Power {
   public CryokineticPower() {
      super(
         "cryokinetic",
         List.of(new FrostBreathAbility(), new ColdSnapAbility(), new FrozenSolidAbility(), new GlacialSlideAbility(), new GlacialRampartAbility())
      );
   }

   @Override
   public int themeColor() {
      return -10034945;
   }

   @Override
   public int signatureSlot() {
      return 2;
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      MinecraftServer server = player.serverLevel().getServer();
      if (server != null) {
         Frostbite.tickAll(server);
      }
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 c = player.position().add(0.0, 1.0, 0.0);
      CryoFx.frostBurst(level, c, 3.2, player.position().add(0.0, 0.1, 0.0));
      CryoFx.crystals(level, c, 60, 1.3);
      Fx.sound(level, player.position(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2F, 0.5F);
   }

   @Override
   public List<String> extraBalanceKeys() {
      return List.of("CryokineticFrostbite");
   }
}
