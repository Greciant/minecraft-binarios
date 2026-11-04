package com.awakenedmod.awakened.client.render.limitless.voidlook;

import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import com.awakenedmod.awakened.core.Lab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class VoidAura {
   private static final String K = "limitless.void.aura.";
   private static final Lab.Value ALPHA = Lab.CLIENT.def("limitless.void.aura.alpha", 0.32, 0.0, 1.0, 0.01);
   private static final Lab.Value SIZE = Lab.CLIENT.def("limitless.void.aura.size", 0.95, 0.3, 2.0, 0.05);

   private VoidAura() {
   }

   public static void draw(LimitlessFrame f, Entity body, float k) {
      if (!(k <= 0.0F) && (body != f.mc.getCameraEntity() || !f.mc.options.getCameraType().isFirstPerson())) {
         Vec3 at = body.getPosition(f.partialTick).add(0.0, (double)body.getBbHeight() * 0.55, 0.0);
         Vector3f c = f.rel(at);
         float r = body.getBbHeight() * 0.5F * SIZE.getF();
         float a = ALPHA.getF() * k;
         LimitlessMesh light = f.begin(FxLayer.LIGHT);
         light.sphere(c, r, 14, 28, (n, facing) -> {
            float e = 1.0F - Math.max(0.0F, facing);
            return LimitlessMesh.rgba(7317759, a * e * e * e);
         });
         f.end(FxLayer.LIGHT);
      }
   }
}
