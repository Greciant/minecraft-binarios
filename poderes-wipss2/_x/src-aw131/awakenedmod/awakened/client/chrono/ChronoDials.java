package com.awakenedmod.awakened.client.chrono;

import com.awakenedmod.awakened.client.render.ChronoDial;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

final class ChronoDials {
   static final float CHEST = 0.65F;
   static final Lab.Value SCALE = Lab.CLIENT.def("chrono.stasis.dial_scale", 0.55, 0.1, 2.0, 0.01);
   static final Lab.Value MIN_RADIUS = Lab.CLIENT.def("chrono.stasis.dial_min", 0.45, 0.1, 3.0, 0.05);

   static float radius(float size) {
      return Math.max(MIN_RADIUS.getF(), SCALE.getF() * size);
   }

   private ChronoDials() {
   }

   static boolean ownFirstPerson(Minecraft mc, Entity body) {
      return body != null && body == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
   }

   static void drawBehind(
      PoseStack world,
      MultiBufferSource buffers,
      Vec3 cam,
      Entity body,
      Vec3 fallbackChest,
      float height,
      ChronoDial.Face face,
      float behind,
      float partialTick
   ) {
      Vec3 feet = body != null ? body.getPosition(partialTick) : fallbackChest.subtract(0.0, (double)(height * 0.65F), 0.0);
      float bodyHeight = body != null ? body.getBbHeight() : height;
      float halfWidth = body != null ? body.getBbWidth() * 0.5F : height * 0.3F;
      float radius = Math.max(face.radius(), radius(halfWidth * 2.0F));
      if (radius != face.radius()) {
         face = new ChronoDial.Face(radius, face.litMask(), face.hands(), face.alpha(), face.spring());
      }

      double centreY = Math.max(feet.y + (double)(bodyHeight * 0.65F), feet.y + (double)radius + 0.05);
      Vec3 chest = new Vec3(feet.x, centreY, feet.z);
      Vector3f[] plane = ChronoDial.upright(chest, cam);
      Vector3f right = plane[0];
      Vector3f away = new Vector3f(plane[1]).cross(right);
      Vec3 centre = chest.add((double)(away.x * (halfWidth + behind)), 0.0, (double)(away.z * (halfWidth + behind)));
      Vector3f c = new Vector3f((float)(centre.x - cam.x), (float)(centre.y - cam.y), (float)(centre.z - cam.z));
      ChronoDial.draw(world.last(), buffers, c, right, plane[1], face);
   }
}
