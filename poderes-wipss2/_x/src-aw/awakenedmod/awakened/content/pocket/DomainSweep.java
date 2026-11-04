package com.awakenedmod.awakened.content.pocket;

import net.minecraft.world.entity.Display.BlockDisplay;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class DomainSweep {
   private static final String TAG = "awakened_domain";

   private DomainSweep() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(DomainSweep::onJoin);
   }

   private static void onJoin(EntityJoinLevelEvent event) {
      if (!event.getLevel().isClientSide()
         && event.loadedFromDisk()
         && event.getEntity() instanceof BlockDisplay panel
         && panel.getTags().contains("awakened_domain")) {
         if (!DomainManager.isNearActiveDome(event.getLevel().dimension(), panel.position())) {
            panel.discard();
         }
      }
   }
}
