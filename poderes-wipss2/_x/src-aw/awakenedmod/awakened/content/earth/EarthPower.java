package com.awakenedmod.awakened.content.earth;

import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Power;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

public final class EarthPower extends Power {
   public EarthPower() {
      super("geokinetic", List.of(new RockThrowAbility(), new UpheaveAbility(), new FaultLineAbility(), new GreatMaulAbility(), new StoneMechAbility()));
   }

   @Override
   public int themeColor() {
      return -7701668;
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      GeoSeismic.tick(player, data);
   }
}
