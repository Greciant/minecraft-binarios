package com.awakenedmod.awakened.client.npc;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;

public interface NpcKitVisuals {
   NpcKitVisuals NONE = new NpcKitVisuals() {
   };

   default void underlay(NpcPoseFrame frame, HumanoidModel<?> model) {
   }

   default float moodTarget(LivingEntity entity) {
      return 0.0F;
   }

   default float moodEase() {
      return 1.0F;
   }

   default void addLayers(PoweredNpcRenderer renderer) {
   }
}
