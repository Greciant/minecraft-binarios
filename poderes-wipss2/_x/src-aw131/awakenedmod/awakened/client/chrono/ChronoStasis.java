package com.awakenedmod.awakened.client.chrono;

import com.awakenedmod.awakened.client.chrono.stasis.StasisCue;
import com.awakenedmod.awakened.client.chrono.stasis.StasisProp;
import com.awakenedmod.awakened.client.chrono.stasis.StasisStyle;
import com.awakenedmod.awakened.client.chrono.stasis.StasisStyles;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.net.ChronoFxPayload;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ChronoStasis {
   static final Lab.Value FLICKER = Lab.CLIENT.def("chrono.stasis.flicker_ticks", 5.0, 1.0, 20.0, 1.0);
   private static final Int2ObjectMap<ChronoFxClient.Effect> LATEST = new Int2ObjectOpenHashMap();

   private ChronoStasis() {
   }

   static void preloadLabKeys() {
      StasisStyles.look();
   }

   static int tailTicks() {
      return Math.max(StasisStyles.look().releaseTicks(), FLICKER.getI());
   }

   static void locked(ClientLevel level, ChronoFxClient.Effect e) {
      LATEST.put(e.entityId, e);
      ChronoBodies.negative(e.entityId, e.duration, FLICKER.getI());
      StasisStyles.look().cast(cue(level, e, false));
   }

   static void released(ClientLevel level, int entityId) {
      boolean any = false;

      for (ChronoFxClient.Effect e : ChronoFxClient.effects(ChronoFxPayload.Kind.STASIS_LOCK)) {
         if (e.entityId == entityId && e.ended() && !e.released) {
            release(level, e);
            any = true;
         }
      }

      if (!any) {
         ChronoBodies.flicker(entityId, FLICKER.getI());
      }
   }

   private static void release(ClientLevel level, ChronoFxClient.Effect e) {
      e.released = true;
      ChronoBodies.flicker(e.entityId, FLICKER.getI());
      StasisStyles.look().release(cue(level, e, false));
   }

   static void tick(ClientLevel level) {
      StasisCue.newTick();
      List<ChronoFxClient.Effect> all = ChronoFxClient.effects(ChronoFxPayload.Kind.STASIS_LOCK);
      if (!all.isEmpty()) {
         ChronoFxClient.Effect leader = null;

         for (ChronoFxClient.Effect e : all) {
            if (!e.ended() && (leader == null || left(e) < left(leader))) {
               leader = e;
            }
         }

         StasisStyle look = StasisStyles.look();

         for (ChronoFxClient.Effect ex : all) {
            if (ex.ended() && !ex.released) {
               release(level, ex);
            }

            look.tick(cue(level, ex, leader == ex));
         }
      }
   }

   private static int left(ChronoFxClient.Effect e) {
      return e.start + e.duration - ChronoFxClient.now();
   }

   private static StasisCue cue(ClientLevel level, ChronoFxClient.Effect e, boolean leads) {
      return new StasisCue(level, e, leads, level.getEntity(e.entityId));
   }

   public static StasisProp frame(Entity entity) {
      ChronoFxClient.Effect e = shown(entity);
      if (e == null) {
         return null;
      } else {
         float partialTick = ChronoFxClient.partialTick();
         float yaw = entity instanceof LivingEntity living ? Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot) : entity.getYRot();
         return new StasisProp(entity.getBbWidth(), entity.getBbHeight(), yaw, e.age(partialTick), e.duration, e.ended() ? e.sinceEnd(partialTick) : -1.0F);
      }
   }

   private static ChronoFxClient.Effect shown(Entity entity) {
      ChronoFxClient.Effect e = latest(entity.getId());
      return e != null && !ChronoDials.ownFirstPerson(Minecraft.getInstance(), entity) ? e : null;
   }

   private static ChronoFxClient.Effect latest(int entityId) {
      return LATEST.isEmpty() ? null : (ChronoFxClient.Effect)LATEST.get(entityId);
   }

   static void forget(ChronoFxClient.Effect e) {
      if (LATEST.get(e.entityId) == e) {
         ChronoFxClient.Effect older = null;

         for (ChronoFxClient.Effect o : ChronoFxClient.effects()) {
            if (o != e && o.kind == ChronoFxPayload.Kind.STASIS_LOCK && o.entityId == e.entityId) {
               older = o;
            }
         }

         if (older != null) {
            LATEST.put(e.entityId, older);
         } else {
            LATEST.remove(e.entityId);
         }
      }
   }

   static void clear() {
      LATEST.clear();
   }
}
