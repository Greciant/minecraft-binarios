package com.devilfruits.core.combat;

import com.devilfruits.core.ability.AbilityManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class Interrupt {
   public static boolean apply(LivingEntity target) {
      if (target.level().isClientSide()) {
         return false;
      } else {
         boolean var10000;
         label21: {
            if (target instanceof ServerPlayer player && AbilityManager.interrupt(player)) {
               var10000 = true;
               break label21;
            }

            var10000 = false;
         }

         boolean any = var10000;
         if (!(target instanceof Player) && target.isUsingItem()) {
            target.stopUsingItem();
            any = true;
         }

         return any;
      }
   }

   private Interrupt() {
   }
}
