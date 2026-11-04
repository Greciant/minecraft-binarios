package com.devilfruits.registry;

import net.neoforged.bus.api.IEventBus;

public final class DFRegistries {
   public static void register(IEventBus modBus) {
      DFDataComponents.COMPONENTS.register(modBus);
      DFBlocks.BLOCKS.register(modBus);
      DFItems.ITEMS.register(modBus);
      DFSounds.SOUNDS.register(modBus);
      DFParticles.PARTICLES.register(modBus);
      DFEntities.ENTITIES.register(modBus);
      DFAttachments.ATTACHMENTS.register(modBus);
      DFMobEffects.EFFECTS.register(modBus);
      DFCreativeTabs.TABS.register(modBus);
      DFLootModifiers.SERIALIZERS.register(modBus);
      DFTriggers.TRIGGERS.register(modBus);
   }

   private DFRegistries() {
   }
}
