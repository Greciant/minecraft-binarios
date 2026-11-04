package com.awakenedmod.awakened.client.limitless;

import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.infinity.InfinityContact;
import com.awakenedmod.awakened.client.render.limitless.infinity.InfinityCue;
import com.awakenedmod.awakened.client.render.limitless.infinity.InfinityZeno;
import com.awakenedmod.awakened.content.limitless.InfinityStopPayload;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

final class InfinityStops implements InfinityCue {
   static final InfinityStops INSTANCE = new InfinityStops();
   private static final int BRAKE_LIMIT = 30;
   private static final List<InfinityStops.Stop> STOPS = new ArrayList<>();
   @Nullable
   private static ClientLevel in;
   private final List<InfinityContact> scratch = new ArrayList<>();

   private InfinityStops() {
   }

   static void accept(InfinityStopPayload payload) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         if (mc.level != in) {
            STOPS.clear();
            in = mc.level;
         }

         if (!payload.melee() && payload.projectileId() >= 0) {
            for (InfinityStops.Stop s : STOPS) {
               if (!s.p.melee() && s.p.defenderId() == payload.defenderId() && s.p.projectileId() == payload.projectileId()) {
                  s.stopped = payload.stoppedTick();
                  return;
               }
            }
         }

         STOPS.add(new InfinityStops.Stop(payload));
      }
   }

   static void clear() {
      STOPS.clear();
      in = null;
   }

   static boolean isEmpty() {
      return STOPS.isEmpty() || Minecraft.getInstance().level != in;
   }

   static void tick(ClientLevel level) {
      if (level != in) {
         STOPS.clear();
         in = level;
      } else {
         long now = level.getGameTime();
         float tail = InfinityZeno.tail();
         Iterator<InfinityStops.Stop> it = STOPS.iterator();

         while (it.hasNext()) {
            InfinityStops.Stop s = it.next();
            boolean braking = !s.p.melee() && s.stopped < 0;
            float done;
            if (braking) {
               done = now - (long)s.p.startTick() > 30L ? Float.MAX_VALUE : 0.0F;
            } else if (s.p.melee()) {
               done = (float)(now - (long)s.p.startTick()) - 3.0F - 8.0F;
            } else {
               done = (float)(now - (long)s.stopped - 10L);
            }

            if (done > tail + 1.0F) {
               it.remove();
            }
         }
      }
   }

   @Override
   public List<InfinityContact> contacts(float age, float partialTick) {
      this.scratch.clear();
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level == in) {
         float now = (float)level.getGameTime() + partialTick;

         for (InfinityStops.Stop s : STOPS) {
            if (!s.p.melee()) {
               InfinityContact c = shot(level, s, now, partialTick);
               if (c != null) {
                  this.scratch.add(c);
               }
            }
         }

         for (InfinityStops.Stop sx : STOPS) {
            if (sx.p.melee()) {
               float since = now - (float)sx.p.startTick();
               float approach = Ease.clamp01(since / 3.0F);
               this.scratch.add(new InfinityContact(sx.p.stop(), sx.p.from(), approach, since - 3.0F, 8.0F, true, null));
            }
         }

         return this.scratch;
      } else {
         return this.scratch;
      }
   }

   @Nullable
   private static InfinityContact shot(ClientLevel level, InfinityStops.Stop s, float now, float partialTick) {
      Entity shot = s.p.projectileId() >= 0 ? level.getEntity(s.p.projectileId()) : null;
      if (s.stopped >= 0) {
         return new InfinityContact(s.p.stop(), s.p.from(), 1.0F, now - (float)s.stopped, 10.0F, false, shot);
      } else if (shot == null) {
         return null;
      } else {
         double d = shot.getPosition(partialTick).distanceTo(s.p.stop());
         float approach = Ease.clamp01((float)(1.0 - d / 2.5));
         float toGo = d < 0.06 ? 0.0F : (float)(Math.log(0.06 / d) / Math.log(0.55));
         return new InfinityContact(s.p.stop(), s.p.from(), approach, -Math.max(0.0F, toGo), 10.0F, false, shot);
      }
   }

   private static final class Stop {
      final InfinityStopPayload p;
      int stopped;

      Stop(InfinityStopPayload p) {
         this.p = p;
         this.stopped = p.stoppedTick();
      }
   }
}
