package com.awakenedmod.awakened.content.spider;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.SpiderVisual;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class SpiderFreefall {
   public static final String FREEFALL_CLIP = "web_freefall";
   private static final double MIN_HEIGHT = 6.0;
   private static final double LANDING_CLEARANCE = 4.0;

   private SpiderFreefall() {
   }

   static SpiderFreefall.Start start(ServerPlayer player) {
      if (!airborneFree(player) || SpiderVisualState.get(player).diving()) {
         return SpiderFreefall.Start.REFUSED;
      } else if (groundWithin(player, 6.0)) {
         return SpiderFreefall.Start.TOO_LOW;
      } else {
         SpiderVisualState.setDiving(player, true);
         PowerService.playAnimation(player, "web_freefall");
         return SpiderFreefall.Start.STARTED;
      }
   }

   static void clear(ServerPlayer player) {
      SpiderVisualState.setDiving(player, false);
   }

   static void tick(ServerPlayer player) {
      if (SpiderVisualState.get(player).diving() && (!airborneFree(player) || groundWithin(player, 4.0))) {
         clear(player);
      }
   }

   private static boolean airborneFree(ServerPlayer player) {
      SpiderVisual v = SpiderVisualState.get(player);
      return !player.onGround()
         && !player.isInWater()
         && !player.getAbilities().flying
         && !player.isFallFlying()
         && !player.isPassenger()
         && v.tether() == 0
         && !v.gliding();
   }

   private static boolean groundWithin(ServerPlayer player, double distance) {
      Vec3 feet = player.position();
      return player.serverLevel().clip(new ClipContext(feet, feet.subtract(0.0, distance, 0.0), Block.COLLIDER, Fluid.ANY, player)).getType() == Type.BLOCK;
   }

   static enum Start {
      STARTED,
      TOO_LOW,
      REFUSED;
   }
}
