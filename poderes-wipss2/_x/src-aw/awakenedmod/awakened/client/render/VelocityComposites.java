package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.vfx.VfxEntity;

public final class VelocityComposites {
   private VelocityComposites() {
   }

   public static void register() {
      VfxComposites.register(VfxEntity.Kind.VELOCITY_SONIC_CRACK, VelocitySonicCrackDrawer::draw);
      VfxComposites.register(VfxEntity.Kind.VELOCITY_GEAR_BURST, VelocityGearBurstDrawer::draw);
      VfxComposites.register(VfxEntity.Kind.VELOCITY_BOLT, VelocityBoltDrawer::draw);
      VfxComposites.register(VfxEntity.Kind.VELOCITY_TIME_FIELD, VelocityTimeFieldDrawer::draw);
      VfxComposites.register(VfxEntity.Kind.VELOCITY_BOLT_IMPACT, VelocityBoltImpactDrawer::draw);
      VfxComposites.register(VfxEntity.Kind.VELOCITY_SONIC_BOOM, VelocitySonicBoomDrawer::draw);
      VfxComposites.register(VfxEntity.Kind.VELOCITY_TIME_RING, VelocityTimeRingDrawer::draw);
      VfxComposites.register(VfxEntity.Kind.VELOCITY_WORMHOLE, VelocityWormholeDrawer::draw);
   }
}
