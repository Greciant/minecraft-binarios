package com.awakenedmod.awakened.client.chrono;

import com.awakenedmod.awakened.client.chrono.ending.FilmBurnEnding;
import com.awakenedmod.awakened.client.chrono.ending.HeldImpactLook;
import com.awakenedmod.awakened.client.render.StasisPropRenderer;
import com.awakenedmod.awakened.content.chronokinetic.TimeStopAbility;
import com.awakenedmod.awakened.core.net.ChronoFxPayload;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class ChronoFxClient {
   private static final int MAX_EFFECTS = 128;
   private static final int MAX_IMPACTS = 48;
   private static final int LONE_IMPACT_TICKS = 20;
   private static final int END_GRACE = 40;
   private static final double HOST_MARGIN = 2.0;
   private static final Map<Long, ChronoFxClient.Effect> EFFECTS = new LinkedHashMap<>();
   private static ClientLevel storeLevel;
   private static int ticks;
   private static long keys;

   private ChronoFxClient() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ChronoFxClient::onClientTick);
      ChronoSilence.register(gameBus);
      FilmBurnEnding.preloadLabKeys();
      ChronoStasis.preloadLabKeys();
      StasisPropRenderer.register(gameBus);
      TimeStopAbility.watchClientFields(ChronoFxClient::fieldRunning);
   }

   public static boolean fieldRunning(int casterId) {
      for (ChronoFxClient.Effect e : EFFECTS.values()) {
         if (e.kind == ChronoFxPayload.Kind.TIME_STOP && e.entityId == casterId && !e.ended()) {
            return true;
         }
      }

      return false;
   }

   public static int now() {
      return ticks;
   }

   public static float partialTick() {
      Minecraft mc = Minecraft.getInstance();
      return mc.getTimer().getGameTimeDeltaPartialTick(false);
   }

   public static Collection<ChronoFxClient.Effect> effects() {
      return Collections.unmodifiableCollection(EFFECTS.values());
   }

   public static List<ChronoFxClient.Effect> effects(ChronoFxPayload.Kind kind) {
      List<ChronoFxClient.Effect> out = new ArrayList<>();

      for (ChronoFxClient.Effect e : EFFECTS.values()) {
         if (e.kind == kind) {
            out.add(e);
         }
      }

      return out;
   }

   public static int impactsOf(ChronoFxClient.Effect host) {
      int n = 0;

      for (ChronoFxClient.Effect e : EFFECTS.values()) {
         if (e.host == host) {
            n++;
         }
      }

      return n;
   }

   public static void accept(ChronoFxPayload p) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         sync(mc);
         switch (p.kind()) {
            case TIME_STOP:
               if (put(p, null) != null) {
                  ChronoCues.started(mc.level, p);
                  ChronoView.tick(mc);
               }
               break;
            case TIME_STOP_RESOLVE:
               resolve(mc.level, p.entityId());
               break;
            case TIME_STOP_END:
               end(ChronoFxPayload.Kind.TIME_STOP, p.entityId());
               break;
            case STASIS_LOCK:
               ChronoFxClient.Effect e = put(p, null);
               if (e != null) {
                  ChronoStasis.locked(mc.level, e);
               }
               break;
            case STASIS_RELEASE:
               end(ChronoFxPayload.Kind.STASIS_LOCK, p.entityId());
               ChronoStasis.released(mc.level, p.entityId());
               break;
            case FLICKER:
               put(p, null);
               ChronoBodies.negative(p.entityId(), p.duration());
               break;
            case REWIND:
               put(p, null);
               ChronoBodies.develop(p.entityId(), ChronoRewindLook.developTicks(), p.duration());
               if (mc.player != null && p.entityId() == mc.player.getId()) {
                  ChronoRewindScreen.rewound(p.duration());
               }
               break;
            case HELD_IMPACT:
               ChronoFxClient.Effect host = hostFor(p);
               put(host != null ? p : withDuration(p, 20), host);
            case UNKNOWN:
         }
      }
   }

   private static ChronoFxClient.Effect hostFor(ChronoFxPayload p) {
      Vec3 hit = p.pos();

      for (ChronoFxClient.Effect e : EFFECTS.values()) {
         double reach = (double)e.radius + 2.0;
         if (e.kind == ChronoFxPayload.Kind.TIME_STOP && !e.ended() && hit.distanceToSqr(e.pos) <= reach * reach) {
            return e;
         }
      }

      for (ChronoFxClient.Effect ex : EFFECTS.values()) {
         if (ex.kind == ChronoFxPayload.Kind.STASIS_LOCK && !ex.ended() && ex.entityId == p.entityId()) {
            return ex;
         }
      }

      return null;
   }

   private static ChronoFxPayload withDuration(ChronoFxPayload p, int duration) {
      return new ChronoFxPayload(p.kind(), p.entityId(), p.x(), p.y(), p.z(), p.radius(), duration, p.seed(), p.points());
   }

   private static ChronoFxClient.Effect put(ChronoFxPayload p, ChronoFxClient.Effect host) {
      boolean impact = p.kind() == ChronoFxPayload.Kind.HELD_IMPACT;
      Long oldestImpact = null;
      int same = 0;

      for (Entry<Long, ChronoFxClient.Effect> entry : EFFECTS.entrySet()) {
         if (entry.getValue().kind == ChronoFxPayload.Kind.HELD_IMPACT == impact) {
            if (impact && oldestImpact == null) {
               oldestImpact = entry.getKey();
            }

            same++;
         }
      }

      if (impact && same >= 48) {
         EFFECTS.remove(oldestImpact);
      } else if (!impact && same >= 128) {
         return null;
      }

      ChronoFxClient.Effect e = new ChronoFxClient.Effect(p, ticks, host);
      EFFECTS.put(keys++, e);
      return e;
   }

   private static void end(ChronoFxPayload.Kind kind, int entityId) {
      for (ChronoFxClient.Effect e : EFFECTS.values()) {
         if (e.kind == kind && e.entityId == entityId && e.end < 0 && !e.ended()) {
            e.end = ticks;
         }
      }
   }

   private static void resolve(ClientLevel level, int casterId) {
      for (ChronoFxClient.Effect e : EFFECTS.values()) {
         if (e.kind == ChronoFxPayload.Kind.TIME_STOP && e.entityId == casterId && e.resolve < 0 && e.end < 0 && !e.ended()) {
            e.resolve = ticks;
            ChronoCues.resolved(level, e);
         }
      }
   }

   private static boolean endsOnEvent(ChronoFxPayload.Kind kind) {
      return kind == ChronoFxPayload.Kind.TIME_STOP || kind == ChronoFxPayload.Kind.STASIS_LOCK;
   }

   private static int tail(ChronoFxClient.Effect e) {
      return switch (e.kind) {
         case TIME_STOP -> FilmBurnEnding.tailTicks();
         default -> 0;
         case STASIS_LOCK -> ChronoStasis.tailTicks();
         case FLICKER -> ChronoFlicker.tailTicks(e);
         case REWIND -> ChronoRewindLook.tailTicks();
         case HELD_IMPACT -> HeldImpactLook.tailTicks();
      };
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      sync(mc);
      if (mc.level != null && !mc.isPaused()) {
         ticks++;
         ChronoCues.tick(mc.level);
         ChronoStasis.tick(mc.level);
         Iterator<ChronoFxClient.Effect> it = EFFECTS.values().iterator();

         while (it.hasNext()) {
            ChronoFxClient.Effect e = it.next();
            if (ticks - e.endTick() > tail(e)) {
               it.remove();
               if (e.kind == ChronoFxPayload.Kind.STASIS_LOCK) {
                  ChronoStasis.forget(e);
               }
            }
         }

         ChronoBodies.tick();
         ChronoFlicker.tick(mc.level);
         ChronoTimeStop.tick(mc.level, mc.player != null ? mc.player.getEyePosition() : Vec3.ZERO);
         ChronoView.tick(mc);
         ChronoScreen.tick(mc);
      }
   }

   private static void sync(Minecraft mc) {
      if (mc.level != storeLevel) {
         storeLevel = mc.level;
         clear();
      }
   }

   public static void clear() {
      EFFECTS.clear();
      ChronoStasis.clear();
      ChronoBodies.clear();
      ChronoView.clear();
      ChronoScreen.clear();
      ChronoRewindScreen.clear();
   }

   public static final class Effect {
      public final ChronoFxPayload.Kind kind;
      public final int entityId;
      public final Vec3 pos;
      public final float radius;
      public final int duration;
      public final int seed;
      public final List<Vec3> points;
      public final int start;
      public final ChronoFxClient.Effect host;
      int end = -1;
      int resolve = -1;
      int lastSteady = -1073741824;
      boolean resumed;
      SoundInstance swell;
      Vec3 burnFrom;
      Vec3 arrival;
      boolean released;

      Effect(ChronoFxPayload p, int start, ChronoFxClient.Effect host) {
         this.kind = p.kind();
         this.entityId = p.entityId();
         this.pos = p.pos();
         this.radius = p.radius();
         this.duration = p.duration();
         this.seed = p.seed();
         this.points = p.points();
         this.start = start;
         this.host = host;
      }

      public float age(float partialTick) {
         return (float)(ChronoFxClient.now() - this.start) + partialTick;
      }

      public int endTick() {
         if (this.host != null) {
            return this.host.endTick();
         } else {
            int own = this.resolve >= 0 ? this.resolve + 15 : this.start + this.duration;
            own += ChronoFxClient.endsOnEvent(this.kind) ? 40 : 0;
            return this.end >= 0 ? Math.min(this.end, own) : own;
         }
      }

      public float resolveAge(float partialTick) {
         return this.resolve < 0 ? -1.0F : Math.min((float)ChronoFxClient.now() + partialTick, (float)this.endTick()) - (float)this.resolve;
      }

      public int endAge() {
         return this.endTick() - this.start;
      }

      public boolean ended() {
         return ChronoFxClient.now() >= this.endTick();
      }

      public float sinceEnd(float partialTick) {
         return (float)(ChronoFxClient.now() - this.endTick()) + partialTick;
      }

      public Vec3 burnFrom() {
         return this.burnFrom;
      }

      public Vec3 origin() {
         return this.points.isEmpty() ? this.pos : this.points.get(0);
      }
   }
}
