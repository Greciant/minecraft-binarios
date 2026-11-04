package com.awakenedmod.awakened.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public final class ExoSuitAnim {
   private static final double THR_OPEN = 0.3;
   private static final double THR_CLOSE = 0.4;
   private static final double BAR_OPEN = 0.45;
   private static final double BAR_CLOSE = 0.45;
   private static final double HAND_OPEN = 0.28;
   private static final double HAND_CLOSE = 0.32;
   private static final double HELMET_OPEN = 0.3;
   private static final double HELMET_CLOSE = 0.4;
   private static final double STALE_SECONDS = 30.0;
   private static final Map<Integer, ExoSuitAnim.Anim> ANIMS = new HashMap<>();
   private static double lastSweep;

   private ExoSuitAnim() {
   }

   public static ExoSuitAnim.Phase update(int entityId, boolean thruster, boolean barrage, boolean handCannon, boolean visor) {
      double now = (double)System.nanoTime() / 1.0E9;
      ExoSuitAnim.Anim a = ANIMS.get(entityId);
      if (a == null) {
         a = new ExoSuitAnim.Anim();
         a.thruster = thruster ? 1.0F : 0.0F;
         a.barrage = barrage ? 1.0F : 0.0F;
         a.handCannon = handCannon ? 1.0F : 0.0F;
         a.visor = visor ? 1.0F : 0.0F;
         a.lastClock = now;
         ANIMS.put(entityId, a);
         sweep(now);
         return new ExoSuitAnim.Phase(a.thruster, a.barrage, a.handCannon, a.visor);
      } else {
         double dt = now - a.lastClock;
         a.lastClock = now;
         if (dt > 0.0) {
            a.thruster = advance(a.thruster, thruster, dt, 0.3, 0.4);
            a.barrage = advance(a.barrage, barrage, dt, 0.45, 0.45);
            a.handCannon = advance(a.handCannon, handCannon, dt, 0.28, 0.32);
            a.visor = advance(a.visor, visor, dt, 0.3, 0.4);
         }

         sweep(now);
         return new ExoSuitAnim.Phase(a.thruster, a.barrage, a.handCannon, a.visor);
      }
   }

   private static float advance(float cur, boolean on, double dt, double openS, double closeS) {
      if (on) {
         return cur <= 1.0F ? Math.min(1.0F, cur + (float)(dt / openS)) : Math.max(1.0F, cur - (float)(dt / openS));
      } else if (cur >= 1.0F) {
         float nt = cur + (float)(dt / closeS);
         return nt >= 2.0F ? 0.0F : nt;
      } else {
         return Math.max(0.0F, cur - (float)(dt / closeS));
      }
   }

   private static void sweep(double now) {
      if (!(now - lastSweep < 5.0)) {
         lastSweep = now;
         Iterator<Entry<Integer, ExoSuitAnim.Anim>> it = ANIMS.entrySet().iterator();

         while (it.hasNext()) {
            if (now - it.next().getValue().lastClock > 30.0) {
               it.remove();
            }
         }
      }
   }

   private static final class Anim {
      float thruster;
      float barrage;
      float handCannon;
      float visor;
      double lastClock;
   }

   public static record Phase(float thruster, float barrage, float handCannon, float visor) {
   }
}
