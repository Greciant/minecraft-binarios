package com.awakenedmod.awakened.core.power;

import com.awakenedmod.awakened.core.data.PowerData;
import net.minecraft.server.level.ServerPlayer;

public interface PowerResource {
   float current(ServerPlayer var1);

   float max(ServerPlayer var1);

   boolean canAfford(ServerPlayer var1, float var2);

   void spend(ServerPlayer var1, float var2);

   void sustain(ServerPlayer var1, float var2);

   void refund(ServerPlayer var1, float var2);

   void refuse(ServerPlayer var1);

   boolean showsStaminaBar();

   void regenTick(ServerPlayer var1, PowerData var2, float var3);

   boolean affordable(PowerResource.Fill var1, float var2);

   public static record Fill(float stamina, float staminaMax, float resourceFraction, boolean infinite) {
   }
}
