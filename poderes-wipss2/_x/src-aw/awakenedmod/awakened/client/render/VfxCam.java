package com.awakenedmod.awakened.client.render;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

final class VfxCam {
   private static Vec3 position = Vec3.ZERO;
   private static final Quaternionf orientation = new Quaternionf();

   private VfxCam() {
   }

   static void update(Vec3 pos, Quaternionf orient) {
      position = pos;
      orientation.set(orient);
   }

   static Vec3 position() {
      return position;
   }

   static Quaternionf orientation() {
      return new Quaternionf(orientation);
   }

   static float yawToCameraDeg(Vec3 worldPos) {
      Vec3 d = position.subtract(worldPos);
      return (float)Math.toDegrees(Math.atan2(d.x, d.z));
   }
}
