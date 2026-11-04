package com.awakenedmod.awakened.client.render.archevoker;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

final class LegacyEvokerAnim {
   private static final Vector3f VEC = new Vector3f();

   private LegacyEvokerAnim() {
   }

   static void reset(HierarchicalModel<?> model) {
      model.root().getAllParts().forEach(ModelPart::resetPose);
   }

   static void apply(HierarchicalModel<?> model, AnimationDefinition def, long timeMs) {
      KeyframeAnimations.animate(model, def, timeMs, 1.0F, VEC);
   }
}
