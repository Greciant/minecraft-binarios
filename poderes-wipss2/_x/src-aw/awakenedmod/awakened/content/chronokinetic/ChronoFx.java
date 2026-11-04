package com.awakenedmod.awakened.content.chronokinetic;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.glow.Limb;
import com.awakenedmod.awakened.core.glow.LimbGlow;
import com.awakenedmod.awakened.core.net.ChronoFxPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ChronoFx {
   public static final double VIEW_REACH = 96.0;
   public static final int FLICKER_FRAMES = 3;
   public static final int GOLD = 15123274;
   public static final int CLENCH_TICKS = 16;
   public static final int RESUME_LEAD = 2;
   public static final int STOP_FINALE_TICKS = 15;
   public static final int STOP_CAP_TICKS = 160;
   public static final String RESUME_CLIP = "time_stop_resume";
   private static final float CAST_GLOW = 0.85F;

   private ChronoFx() {
   }

   public static void timeStop(ServerLevel level, ServerPlayer caster, Vec3 center, double radius, int cap) {
      timeStop(level, (Entity)caster, center, radius, cap);
      LimbGlow.set(caster, Limb.RIGHT_ARM, 0.85F, 15123274);
      LimbGlow.flash(caster, Limb.RIGHT_ARM);
   }

   public static void timeStop(ServerLevel level, Entity caster, Vec3 center, double radius, int cap) {
      send(
         level,
         center,
         radius,
         new ChronoFxPayload(
            ChronoFxPayload.Kind.TIME_STOP, caster.getId(), center.x, center.y, center.z, (float)radius, cap, seed(caster), List.of(chest(caster))
         )
      );
   }

   public static void timeStopTick(ServerPlayer caster, int elapsed, int sinceResolve) {
      if (elapsed == 16) {
         LimbGlow.set(caster, Limb.RIGHT_ARM, 0.0F);
      }

      if (sinceResolve == 13) {
         PowerService.playAnimation(caster, "time_stop_resume");
      }
   }

   public static void timeStopResolve(ServerLevel level, Entity caster, Vec3 center, double radius) {
      sendToFieldAndCaster(
         level,
         caster,
         center,
         radius,
         new ChronoFxPayload(ChronoFxPayload.Kind.TIME_STOP_RESOLVE, caster.getId(), center.x, center.y, center.z, (float)radius, 15, 0, List.of())
      );
   }

   public static void timeStopEnd(ServerLevel level, Entity caster, Vec3 center, double radius) {
      sendToFieldAndCaster(
         level,
         caster,
         center,
         radius,
         new ChronoFxPayload(ChronoFxPayload.Kind.TIME_STOP_END, caster.getId(), center.x, center.y, center.z, (float)radius, 0, 0, List.of())
      );
      if (caster instanceof ServerPlayer player) {
         LimbGlow.set(player, Limb.RIGHT_ARM, 0.0F);
      }
   }

   private static void sendToFieldAndCaster(ServerLevel level, Entity caster, Vec3 center, double radius, ChronoFxPayload payload) {
      if (payload.finite()) {
         ServerPlayer player = caster instanceof ServerPlayer p ? p : null;
         PacketDistributor.sendToPlayersNear(level, player, center.x, center.y, center.z, radius + 96.0, payload, new CustomPacketPayload[0]);
         if (player != null) {
            PacketDistributor.sendToPlayer(player, payload, new CustomPacketPayload[0]);
         }
      }
   }

   public static void heldImpact(ServerLevel level, Entity body, Vec3 hit, Vec3 from) {
      send(level, hit, 0.0, new ChronoFxPayload(ChronoFxPayload.Kind.HELD_IMPACT, body.getId(), hit.x, hit.y, hit.z, 0.0F, 0, seed(body), List.of(from)));
   }

   public static void stasisLock(ServerLevel level, Entity body, int duration) {
      Vec3 chest = chest(body);
      send(
         level,
         chest,
         0.0,
         new ChronoFxPayload(ChronoFxPayload.Kind.STASIS_LOCK, body.getId(), chest.x, chest.y, chest.z, body.getBbHeight(), duration, seed(body), List.of())
      );
   }

   public static void stasisRelease(ServerLevel level, Entity body) {
      Vec3 chest = chest(body);
      send(
         level,
         chest,
         0.0,
         new ChronoFxPayload(ChronoFxPayload.Kind.STASIS_RELEASE, body.getId(), chest.x, chest.y, chest.z, body.getBbHeight(), 0, 0, List.of())
      );
   }

   public static void rewind(ServerLevel level, Entity body, Vec3 destination, int glideTicks, List<Vec3> path) {
      send(
         level,
         destination,
         0.0,
         new ChronoFxPayload(
            ChronoFxPayload.Kind.REWIND, body.getId(), destination.x, destination.y, destination.z, body.getBbHeight(), glideTicks, seed(body), sample(path)
         )
      );
   }

   public static void flicker(ServerLevel level, Entity dasher, Vec3 departure, Vec3 dir) {
      flicker(level, dasher, departure, dir, 3);
   }

   public static void flicker(ServerLevel level, Entity dasher, Vec3 departure, Vec3 dir, int frames) {
      send(
         level,
         departure,
         0.0,
         new ChronoFxPayload(ChronoFxPayload.Kind.FLICKER, dasher.getId(), departure.x, departure.y, departure.z, 0.0F, frames, seed(dasher), List.of(dir))
      );
   }

   public static Vec3 chest(Entity body) {
      return chest(body, body.position());
   }

   public static Vec3 chest(Entity body, Vec3 feet) {
      return feet.add(0.0, (double)body.getBbHeight() * 0.65, 0.0);
   }

   private static void send(ServerLevel level, Vec3 at, double size, ChronoFxPayload payload) {
      if (payload.finite() && Double.isFinite(size)) {
         PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, size + 96.0, payload, new CustomPacketPayload[0]);
      }
   }

   private static int seed(Entity entity) {
      return entity.getId() * -1640531535 ^ (int)entity.level().getGameTime();
   }

   static List<Vec3> sample(List<Vec3> path) {
      int max = 20;
      if (path.size() <= max) {
         return List.copyOf(path);
      } else {
         List<Vec3> out = new ArrayList<>(max);

         for (int i = 0; i < max; i++) {
            out.add(path.get(Math.round((float)(i * (path.size() - 1)) / (float)(max - 1))));
         }

         return out;
      }
   }
}
