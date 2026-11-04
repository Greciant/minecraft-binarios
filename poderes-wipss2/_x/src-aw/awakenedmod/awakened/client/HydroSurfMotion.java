package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.hydro.HydroVisual;
import com.awakenedmod.awakened.core.Lab;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class HydroSurfMotion {
   private static final float TURN_EASE = 0.3F;
   private static final Lab.Value BOB = Lab.CLIENT.def("hydro.surf.bob", 0.06, 0.0, 0.5, 0.01);
   private static final float BOB_RATE = 0.28F;
   private static final float FULL_CARVE = 8.0F;
   private static final Map<Integer, HydroSurfMotion.Turn> TURNS = new HashMap<>();
   public static final Lab.Value RIDER_DIRECTION = Lab.CLIENT.def("hydro.surf.carve_direction", 1.0, -1.0, 1.0, 2.0);

   private HydroSurfMotion() {
   }

   public static float turn(Entity rider, float partialTick) {
      if (!HydroVisual.of(rider).surfing()) {
         TURNS.remove(rider.getId());
         return 0.0F;
      } else {
         HydroSurfMotion.Turn t = TURNS.computeIfAbsent(rider.getId(), id -> new HydroSurfMotion.Turn());
         if (t.tick != rider.tickCount) {
            boolean fresh = t.tick == Integer.MIN_VALUE;
            t.tick = rider.tickCount;
            t.before = t.now;
            float rate = Mth.wrapDegrees(rider.getYRot() - rider.yRotO);
            t.now = fresh ? 0.0F : t.now + (rate - t.now) * 0.3F;
            if (TURNS.size() > 64) {
               TURNS.keySet().removeIf(id -> id != rider.getId());
            }
         }

         return Mth.lerp(partialTick, t.before, t.now);
      }
   }

   public static float riderTurn(Entity rider, float partialTick) {
      return (RIDER_DIRECTION.get() < 0.0 ? -1.0F : 1.0F) * turn(rider, partialTick);
   }

   public static float riderCarve(Entity rider, float partialTick) {
      return (RIDER_DIRECTION.get() < 0.0 ? -1.0F : 1.0F) * carve(rider, partialTick);
   }

   public static float carve(Entity rider, float partialTick) {
      return Mth.clamp(turn(rider, partialTick) / 8.0F, -1.0F, 1.0F);
   }

   public static float bob(Entity rider, float partialTick) {
      return BOB.getF() * Mth.sin(((float)rider.tickCount + partialTick) * 0.28F + (float)rider.getId());
   }

   private static final class Turn {
      float now;
      float before;
      int tick = Integer.MIN_VALUE;
   }
}
