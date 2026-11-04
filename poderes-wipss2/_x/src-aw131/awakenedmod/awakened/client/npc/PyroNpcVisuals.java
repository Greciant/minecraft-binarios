package com.awakenedmod.awakened.client.npc;

import com.awakenedmod.awakened.client.PyroClientHeat;
import com.awakenedmod.awakened.client.PyroHeatPose;
import com.awakenedmod.awakened.client.render.PyroFlameLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class PyroNpcVisuals implements NpcKitVisuals {
   @Override
   public void underlay(NpcPoseFrame frame, HumanoidModel<?> model) {
      float w = frame.mood();
      if (!(w <= 0.0F)) {
         float phase = (float)(frame.entityId() % 16) * 0.4F;
         float shake = 1.2F * Mth.sin(frame.ageInTicks() * 1.9F + phase) * w;
         NpcPose.add(model.rightArm, -12.0F * w, 0.0F, 8.0F * w + shake);
         NpcPose.add(model.leftArm, -12.0F * w, 0.0F, -8.0F * w - shake);
         NpcPose.add(model.body, 3.0F * w, 0.0F, 0.0F);
      }
   }

   @Override
   public float moodTarget(LivingEntity entity) {
      return !entity.isUsingItem() && !entity.isInvisible() ? PyroHeatPose.postureWeight(PyroClientHeat.heat(entity)) : 0.0F;
   }

   @Override
   public float moodEase() {
      return 0.1F;
   }

   @Override
   public void addLayers(PoweredNpcRenderer renderer) {
      renderer.addLayer(new PyroFlameLayer(renderer));
   }
}
