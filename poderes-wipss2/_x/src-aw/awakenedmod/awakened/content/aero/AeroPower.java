package com.awakenedmod.awakened.content.aero;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Power;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public final class AeroPower extends Power {
   public static final String ID = "aerokinetic";

   public AeroPower() {
      super("aerokinetic", List.of(new WooshAbility(), new GaleWindsAbility(), new AirCutAbility(), new TornadoAbility(), new AirRemovalAbility()));
   }

   @Override
   public int themeColor() {
      return -4200193;
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      AirCushion.apply(player, PowerService.data(player));
      GaleControl.pushSync(player);
   }

   @Override
   public void onLogin(ServerPlayer player, PowerData data) {
      GaleControl.pushSync(player);
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      AirCushion.apply(player, data);
      GaleControl.tick(player, data);
      AeroVisualState.sync(player);
   }

   @Override
   public void onDeath(ServerPlayer victim, DamageSource source) {
      GaleControl.clear(victim);
   }

   @Override
   public void onSuppressed(ServerPlayer player, PowerData data) {
      AirCushion.clear(player);
   }

   @Override
   public void onReplaced(ServerPlayer player) {
      AirCushion.clear(player);
   }
}
