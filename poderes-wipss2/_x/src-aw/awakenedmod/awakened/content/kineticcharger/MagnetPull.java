package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.content.graviton.GravitySupport;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.PassiveToggle;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class MagnetPull {
   public static final PassiveToggle TOGGLE = new PassiveToggle(
      "kinetic_magnet", "awakened.screen.setting_kinetic_magnet", "awakened.screen.setting_kinetic_magnet_desc", true
   );
   private static final double RADIUS = 5.0;
   private static final double PULL_STRENGTH = 0.35;
   private static final double GRAB_DISTANCE = 2.0;
   private static final int OWNER_GRACE_TICKS = 100;

   private MagnetPull() {
   }

   static void tick(ServerPlayer player, PowerData data) {
      if (TOGGLE.enabled(data) && !player.isSpectator()) {
         Vec3 center = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);

         for (Entity loose : GravitySupport.looseAround(player.serverLevel(), center, 5.0)) {
            if (loose instanceof ItemEntity item) {
               if (!free(player, item)) {
                  continue;
               }

               if (item.distanceToSqr(player) <= 4.0) {
                  item.setNoPickUpDelay();
               }
            } else if (!(loose instanceof ExperienceOrb)) {
               continue;
            }

            GravitySupport.pullEntity(loose, center, 0.35, 0.0);
            KineticFx.magnetTick(player.serverLevel(), loose);
         }
      }
   }

   private static boolean free(ServerPlayer player, ItemEntity item) {
      Entity owner = item.getOwner();
      return !(owner instanceof Player) || owner == player || item.getAge() >= 100;
   }
}
