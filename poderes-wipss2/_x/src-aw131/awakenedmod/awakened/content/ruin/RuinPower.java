package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Power;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

public final class RuinPower extends Power {
   public static final String ID = "ruin";

   public RuinPower() {
      super("ruin", List.of(new RuinGripAbility(), new RuinSpreadAbility(), new RuinRushAbility(), new RuinBreachAbility(), new RuinCollapseAbility()));
   }

   @Override
   public int themeColor() {
      return -6646385;
   }

   @Override
   public List<String> extraBalanceKeys() {
      return List.of("RuinDecay");
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      RuinCues.awaken(player);
   }

   @Override
   public void onSuppressed(ServerPlayer player, PowerData data) {
      RuinStacks.clearAllBy(player.getUUID());
   }

   @Override
   protected void onAbilityStateCleared(ServerPlayer player) {
      RuinArm.off(player);
   }
}
