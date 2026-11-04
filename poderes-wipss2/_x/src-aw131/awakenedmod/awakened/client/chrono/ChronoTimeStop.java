package com.awakenedmod.awakened.client.chrono;

import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.net.ChronoFxPayload;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class ChronoTimeStop {
   static final Lab.Value BLINK_TICKS = Lab.CLIENT.def("chrono.stop.blink_ticks", 1.4, 0.5, 4.0, 0.1);
   static final Lab.Value SILENCE_FROM = Lab.CLIENT.def("chrono.stop.silence_from", 21.0, 0.0, 40.0, 1.0);
   static final Lab.Value CUT_AT = Lab.CLIENT.def("chrono.stop.cut_at", 21.0, 0.0, 80.0, 1.0);
   static final Lab.Value CUT_BLINK = Lab.CLIENT.def("chrono.stop.cut_blink", 0.0, 0.0, 1.0, 1.0);
   static final Lab.Value PHOTO_AT = Lab.CLIENT.def("chrono.stop.photo_at", 32.0, 0.0, 80.0, 1.0);
   static final Lab.Value CALM_FROM = Lab.CLIENT.def("chrono.stop.calm_from", 21.0, 0.0, 80.0, 1.0);
   static final Lab.Value CALM_DEVELOP = Lab.CLIENT.def("chrono.stop.calm_develop", 8.0, 1.0, 30.0, 1.0);
   static final Lab.Value DOME_FROM = Lab.CLIENT.def("chrono.stop.dome_from", 34.0, 0.0, 80.0, 1.0);
   static final Lab.Value DOME_TO = Lab.CLIENT.def("chrono.stop.dome_to", 42.0, 1.0, 80.0, 1.0);
   static final Lab.Value TICK_FROM = Lab.CLIENT.def("chrono.stop.tick_from", 32.0, 0.0, 80.0, 1.0);
   static final Lab.Value TICK_EVERY = Lab.CLIENT.def("chrono.stop.tick_every", 20.0, 4.0, 60.0, 1.0);
   static final Lab.Value IMPACT_BURST = Lab.CLIENT.def("chrono.stop.impact_burst", 10.0, 2.0, 40.0, 1.0);
   static final int[] FINALE_TICKS = new int[]{0, 3, 5, 7, 9};

   private ChronoTimeStop() {
   }

   static void preloadLabKeys() {
   }

   public static int impactTailTicks() {
      return IMPACT_BURST.getI();
   }

   static boolean flipInverted(float age) {
      float cut = CUT_AT.getF();
      return CUT_BLINK.getI() != 0 && age >= cut && age < cut + BLINK_TICKS.getF();
   }

   static boolean silent(float age) {
      return age >= SILENCE_FROM.getF();
   }

   static float photo(float age, boolean calm) {
      if (!calm) {
         return age >= PHOTO_AT.getF() ? 1.0F : 0.0F;
      } else {
         return Mth.clamp((age - CALM_FROM.getF()) / Math.max(1.0F, CALM_DEVELOP.getF()), 0.0F, 1.0F);
      }
   }

   public static float dome(float age) {
      boolean calm = ChronoView.noNegative();
      float from = calm ? 0.0F : DOME_FROM.getF();
      float to = Math.max(from + 0.5F, calm ? ChronoNegative.GROW.getF() : DOME_TO.getF());
      float t = Mth.clamp((age - from) / (to - from), 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   static boolean steadyTick(int age) {
      int from = TICK_FROM.getI();
      return age >= from && (age - from) % Math.max(1, TICK_EVERY.getI()) == 0;
   }

   static void tick(ClientLevel level, Vec3 viewer) {
      for (ChronoFxClient.Effect e : ChronoFxClient.effects(ChronoFxPayload.Kind.TIME_STOP)) {
         if (e.burnFrom == null && !e.ended() && !(e.resolveAge(0.0F) < 0.0F)) {
            Entity caster = level.getEntity(e.entityId);
            Vec3 toward = (caster != null ? caster.getEyePosition() : viewer).subtract(e.pos);
            if (toward.lengthSqr() < 1.0E-4) {
               toward = viewer.subtract(e.pos);
            }

            e.burnFrom = toward.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : toward.normalize();
         }
      }
   }
}
