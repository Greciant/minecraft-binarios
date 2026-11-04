package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.LoopSound;
import com.awakenedmod.awakened.core.TeleportGuard;
import com.awakenedmod.awakened.core.TeleportUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class TunnelLane {
   static final int LANE_TICKS = 400;
   private static final double TRIGGER_HALF_WIDTH = 0.9;
   private static final double TRIGGER_HALF_HEIGHT = 1.4;
   private static final double ARRIVAL_OFFSET = 1.5;
   private static final double ARRIVAL_SEARCH_RADIUS = 2.0;
   private static final int ARRIVAL_MAX_DROP = 3;
   private static final int HOP_COOLDOWN_TICKS = 40;
   private static final float AMBIENT_VOLUME = 0.25F;
   private static final float AMBIENT_PITCH = 1.6F;
   private static final float ARRIVAL_VOLUME = 0.5F;
   private static final float ARRIVAL_PITCH = 1.6F;
   private static final int ARRIVAL_IFRAMES = 10;
   private static final Set<TunnelLane> OPEN = ConcurrentHashMap.newKeySet();
   private final ServerLevel level;
   private final TunnelLane.Mouth entry;
   private final TunnelLane.Mouth exit;
   private final Map<UUID, Long> cooldowns = new HashMap<>();
   private final Set<UUID> riding = ConcurrentHashMap.newKeySet();
   private int ticksLeft = 400;

   private TunnelLane(ServerLevel level, TunnelLane.Mouth entry, TunnelLane.Mouth exit) {
      this.level = level;
      this.entry = entry;
      this.exit = exit;
   }

   static TunnelLane open(ServerLevel level, Vec3 entryFeet, Vec3 entryFacing, VfxEntity entryMouth, Vec3 exitFeet, Vec3 exitFacing, VfxEntity exitMouth) {
      TunnelLane.Mouth in = new TunnelLane.Mouth(level, entryFeet, entryFacing, entryMouth);
      TunnelLane.Mouth out = new TunnelLane.Mouth(level, exitFeet, exitFacing, exitMouth);
      TunnelLane lane = new TunnelLane(level, in, out);
      lane.seed(in);
      lane.seed(out);
      OPEN.add(lane);
      return lane;
   }

   boolean tick() {
      if (--this.ticksLeft <= 0) {
         return false;
      } else {
         long now = this.level.getGameTime();
         this.cooldowns.values().removeIf(lapse -> lapse <= now);
         this.ferry(this.entry, this.exit, now);
         this.ferry(this.exit, this.entry, now);
         LoopSound.keepAt(this.level, this.humKey("entry"), this.entry.centre(), SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 0.25F, 1.6F);
         LoopSound.keepAt(this.level, this.humKey("exit"), this.exit.centre(), SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 0.25F, 1.6F);
         return true;
      }
   }

   private String humKey(String end) {
      return "lane@" + Integer.toHexString(System.identityHashCode(this)) + ":" + end;
   }

   void close() {
      OPEN.remove(this);
      LoopSound.stopAt(this.level, this.humKey("entry"));
      LoopSound.stopAt(this.level, this.humKey("exit"));

      for (UUID id : this.riding) {
         ServerPlayer inside = this.level.getServer().getPlayerList().getPlayer(id);
         if (inside != null) {
            LaneTransit.deliver(inside);
         }
      }

      this.riding.clear();
      this.entry.seal();
      this.exit.seal();
      this.cooldowns.clear();
   }

   static void closeAll() {
      OPEN.clear();
   }

   private void ferry(TunnelLane.Mouth from, TunnelLane.Mouth to, long now) {
      List<LivingEntity> standing = this.level.getEntitiesOfClass(LivingEntity.class, from.volume(), LivingEntity::isAlive);
      Set<UUID> present = new HashSet<>();

      for (LivingEntity traveller : standing) {
         UUID id = traveller.getUUID();
         present.add(id);
         if (!from.occupants.contains(id) && !this.cooldowns.containsKey(id) && !LaneTransit.riding(traveller)) {
            this.take(traveller, from, to, now);
         }
      }

      from.occupants.clear();
      from.occupants.addAll(present);
   }

   private boolean take(LivingEntity traveller, TunnelLane.Mouth from, TunnelLane.Mouth to, long now) {
      Vec3 dest = to.arrival(traveller);
      if (traveller instanceof ServerPlayer player) {
         if (TeleportGuard.crosses(player, player.position(), dest)) {
            return false;
         } else if (!LaneTransit.start(player, this.level, from.feet, dest, this::arrived)) {
            return false;
         } else {
            this.riding.add(player.getUUID());
            VelocityFx.tunnelVanish(this.level, player, from.feet, from.facing);
            return true;
         }
      } else {
         traveller.teleportTo(dest.x, dest.y, dest.z);
         this.land(traveller, to, now);
         return true;
      }
   }

   private void arrived(ServerPlayer player, Vec3 at, boolean completed) {
      this.riding.remove(player.getUUID());
      if (player.isAlive()) {
         TunnelLane.Mouth door = this.exitSideOf(at);
         if (completed) {
            VelocityFx.tunnelEmerge(this.level, player, door.feet, at, door.facing);
         }

         this.land(player, door, this.level.getGameTime());
      }
   }

   private TunnelLane.Mouth exitSideOf(Vec3 at) {
      return at.distanceToSqr(this.exit.feet) <= at.distanceToSqr(this.entry.feet) ? this.exit : this.entry;
   }

   private void land(LivingEntity traveller, TunnelLane.Mouth to, long now) {
      traveller.invulnerableTime = Math.max(traveller.invulnerableTime, 10);
      traveller.setDeltaMovement(to.facing.scale(traveller.getDeltaMovement().horizontalDistance()));
      traveller.hurtMarked = true;
      traveller.resetFallDistance();
      Fx.sound(this.level, traveller.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5F, 1.6F);
      to.occupants.add(traveller.getUUID());
      this.cooldowns.put(traveller.getUUID(), now + 40L);
   }

   private void seed(TunnelLane.Mouth mouth) {
      for (LivingEntity standing : this.level.getEntitiesOfClass(LivingEntity.class, mouth.volume(), LivingEntity::isAlive)) {
         mouth.occupants.add(standing.getUUID());
      }
   }

   private static final class Mouth {
      private final ServerLevel level;
      private final Vec3 feet;
      private final Vec3 facing;
      private final VfxEntity maw;
      private final Set<UUID> occupants = new HashSet<>();

      private Mouth(ServerLevel level, Vec3 feet, Vec3 facing, VfxEntity maw) {
         this.level = level;
         this.feet = feet;
         this.facing = Vfx.safeDir(facing == null ? null : new Vec3(facing.x, 0.0, facing.z));
         this.maw = maw;
      }

      private Vec3 centre() {
         return VelocityFx.wormholeCentre(this.feet);
      }

      private AABB volume() {
         Vec3 c = this.centre();
         return new AABB(c, c).inflate(0.9, 1.4, 0.9);
      }

      private Vec3 arrival(LivingEntity traveller) {
         Vec3 raw = this.feet.add(this.facing.scale(1.5));
         TeleportUtil.streamIn(this.level, raw);
         Vec3 grounded = TeleportUtil.nearestSafeSpot(traveller, raw, 2.0, 3);
         return grounded != null ? grounded : this.feet;
      }

      private void seal() {
         VelocityFx.closeWormhole(this.level, this.maw);
         this.occupants.clear();
      }
   }
}
