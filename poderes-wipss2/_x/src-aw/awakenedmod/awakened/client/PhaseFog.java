package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.PhaseState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeFogColor;
import net.neoforged.neoforge.client.event.ViewportEvent.RenderFog;

public final class PhaseFog {
   private static final float VIEW_DISTANCE = 16.0F;
   public static final int DEFAULT_FOG = 327690;

   private PhaseFog() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(PhaseFog::onFog);
      gameBus.addListener(PhaseFog::onFogColor);
   }

   private static boolean buriedPhasing() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null && PhaseState.clientPhasing(mc.player.getId())) {
         BlockPos eye = BlockPos.containing(mc.player.getEyePosition());
         BlockState state = mc.level.getBlockState(eye);
         return !state.isAir() && state.canOcclude();
      } else {
         return false;
      }
   }

   private static int fogColor() {
      Minecraft mc = Minecraft.getInstance();
      PhaseState.ClientSource source = mc.player == null ? null : PhaseState.clientSource(mc.player);
      return source == null ? 327690 : source.fogColor(mc.player);
   }

   private static void onFog(RenderFog event) {
      if (buriedPhasing()) {
         event.setFarPlaneDistance(16.0F);
         event.setNearPlaneDistance(6.4F);
      }
   }

   private static void onFogColor(ComputeFogColor event) {
      if (buriedPhasing()) {
         int color = fogColor();
         event.setRed((float)(color >> 16 & 0xFF) / 255.0F);
         event.setGreen((float)(color >> 8 & 0xFF) / 255.0F);
         event.setBlue((float)(color & 0xFF) / 255.0F);
      }
   }
}
