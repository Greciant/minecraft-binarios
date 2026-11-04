package com.awakenedmod.awakened.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public final class ExoSuitAssembly {
   private static final double ASSEMBLY_SECONDS = 1.25;
   private static final double RECALL_SECONDS = 0.8;
   private static final double STALE_SECONDS = 30.0;
   private static final Map<Integer, ExoSuitAssembly.Anim> ANIMS = new HashMap<>();
   private static double lastSweep;

   private ExoSuitAssembly() {
   }

   public static float progress(int entityId, boolean suited) {
      double now = (double)System.nanoTime() / 1.0E9;
      float target = suited ? 1.0F : 0.0F;
      ExoSuitAssembly.Anim a = ANIMS.get(entityId);
      if (a == null) {
         ANIMS.put(entityId, new ExoSuitAssembly.Anim(target, now));
         sweep(now);
         return target;
      } else {
         double dt = now - a.lastClock;
         a.lastClock = now;
         if (dt > 0.0) {
            if (a.progress < target) {
               a.progress = Math.min(target, a.progress + (float)(dt / 1.25));
            } else if (a.progress > target) {
               a.progress = Math.max(target, a.progress - (float)(dt / 0.8));
            }
         }

         sweep(now);
         return a.progress;
      }
   }

   public static int mask(int entityId, boolean suited, int visibleMask) {
      ExoSuitAssembly.Anim a = ANIMS.get(entityId);
      if (a == null) {
         a = new ExoSuitAssembly.Anim(suited ? 1.0F : 0.0F, (double)System.nanoTime() / 1.0E9);
         ANIMS.put(entityId, a);
      }

      if (suited) {
         a.mask = visibleMask;
      }

      return a.mask;
   }

   private static void sweep(double now) {
      if (!(now - lastSweep < 5.0)) {
         lastSweep = now;
         Iterator<Entry<Integer, ExoSuitAssembly.Anim>> it = ANIMS.entrySet().iterator();

         while (it.hasNext()) {
            if (now - it.next().getValue().lastClock > 30.0) {
               it.remove();
            }
         }
      }
   }

   private static final class Anim {
      float progress;
      double lastClock;
      int mask;

      Anim(float progress, double lastClock) {
         this.progress = progress;
         this.lastClock = lastClock;
      }
   }
}
