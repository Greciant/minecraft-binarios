package com.awakenedmod.awakened.client.graviton.weight;

import com.awakenedmod.awakened.client.ClientPowerState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class HeavyWaterClient {
   private static final int FLOOR_TIER = 2;

   private HeavyWaterClient() {
   }

   @SubscribeEvent
   public static void onPlayerTick(Post event) {
      Player self = event.getEntity();
      Minecraft mc = Minecraft.getInstance();
      if (self == mc.player && self.isInWater() && self.isSprinting()) {
         ClientPowerState state = ClientPowerState.get();
         if ("graviton".equals(state.livePowerId()) && state.resourceTier() >= 2) {
            Vec3 v = self.getDeltaMovement();
            self.setDeltaMovement(v.x, v.y - self.getAttributeValue(Attributes.GRAVITY) / 16.0, v.z);
         }
      }
   }
}
