package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.hydro.HydroLance;
import com.awakenedmod.awakened.content.hydro.HydroPalette;
import com.awakenedmod.awakened.core.Lab;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

final class HydroLanceSpray {
   private static final Lab.Value SHED = Lab.CLIENT.def("hydro.lance.shed", 1.2, 0.0, 5.0, 0.1);
   private static final Lab.Value CRUMBLE = Lab.CLIENT.def("hydro.lance.crumble", 2.0, 0.0, 6.0, 0.1);
   private static final float SHED_AFTER = 2.0F;
   private static final long FORGET = 100L;
   private static final float OFF_FACE = 0.4F;
   private static final Map<Integer, Long> LAST = new HashMap<>();

   private HydroLanceSpray() {
   }

   static void shed(HydroLance lance, float partialTick, HydroLanceMesh.Dims dims) {
      if (lance.level() instanceof ClientLevel level) {
         long var17 = level.getGameTime();
         Long last = LAST.put(lance.getId(), var17);
         if (last == null || last != var17) {
            if (LAST.size() > 64) {
               LAST.values().removeIf(t -> var17 - t > 100L || t > var17);
            }

            boolean wet = (lance.style() & 16) != 0;
            WaterCubeSpray.Look look = new WaterCubeSpray.Look(HydroPalette.rgb(wet), 0.8F * HydroPalette.opacity(wet) * 0.9F, 0.3F);
            Vec3 dir = Vec3.directionFromRotation(lance.getXRot(), lance.getYRot());
            Vec3 tip = lance.getPosition(partialTick);
            float cube = dims.shaft() * 2.0F;
            Object owner = "hydro_lance" + lance.getId();
            if (lance.isStuck()) {
               float drained = lance.drained(partialTick);
               if (!(drained >= 1.0F)) {
                  Vec3 at = tip.add(dir.scale((double)(0.3F - Math.max(dims.length(), 0.4F))));
                  int n = count(level, CRUMBLE.getF());
                  if (n > 0) {
                     WaterCubeSpray.burst(
                        level,
                        owner,
                        at,
                        Vec3.ZERO,
                        WaterCubeSpray.Burst.of(n, 0.03F, cube * 1.1F).scattered(0.06F).lifted(0.03F).lasting(34).spinning(10.0F).foamed(0.2F),
                        look
                     );
                  }
               }
            } else if (!(lance.flightAge(partialTick) < 2.0F)) {
               int n = count(level, SHED.getF());
               if (n > 0) {
                  double back = (double)dims.length() * (0.5 + 0.5 * level.getRandom().nextDouble());
                  Vec3 drift = lance.getDeltaMovement().scale(0.2);
                  WaterCubeSpray.burst(
                     level,
                     owner,
                     tip.subtract(dir.scale(back)),
                     drift,
                     WaterCubeSpray.Burst.of(n, 0.035F, cube).scattered(0.05F).lifted(0.02F).lasting(26).spinning(18.0F).foamed(0.25F),
                     look
                  );
               }
            }
         }
      }
   }

   private static int count(ClientLevel level, float rate) {
      int n = (int)rate;
      return n + (level.getRandom().nextFloat() < rate - (float)n ? 1 : 0);
   }
}
