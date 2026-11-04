package com.awakenedmod.awakened.content.scaler;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class HitchControl {
   private static final int THRASH_INTERVAL = 60;
   private static final float THRASH_CHANCE = 0.35F;
   private static final float SHAKE_OFF_CHANCE = 0.4F;
   private static final double HOP_VELOCITY = 0.35;
   private static final Map<UUID, HitchControl.Latch> LATCHES = new ConcurrentHashMap<>();

   private HitchControl() {
   }

   public static boolean isLatched(ServerPlayer player) {
      return LATCHES.containsKey(player.getUUID());
   }

   public static boolean begin(ServerPlayer rider, LivingEntity mount) {
      GripAnchorEntity anchor = null;
      if (mount instanceof Player) {
         anchor = spawnRideAnchor(mount);
         boolean mounted = forceRide(rider, anchor);
         if (!mounted) {
            anchor.discard();
            return false;
         }
      } else {
         boolean mounted = forceRide(rider, mount);
         if (!mounted) {
            return false;
         }
      }

      LATCHES.put(rider.getUUID(), new HitchControl.Latch(mount, anchor, rider.level().getGameTime() + 60L));
      return true;
   }

   private static boolean forceRide(ServerPlayer rider, Entity vehicle) {
      return rider.startRiding(vehicle, true);
   }

   private static GripAnchorEntity spawnRideAnchor(LivingEntity mount) {
      ServerLevel level = (ServerLevel)mount.level();
      GripAnchorEntity anchor = new GripAnchorEntity(AwakenedEntities.GRIP_ANCHOR.get(), level);
      Vec3 p = ridePoint(mount);
      anchor.setPos(p.x, p.y, p.z);
      level.addFreshEntity(anchor);
      return anchor;
   }

   private static Vec3 ridePoint(LivingEntity mount) {
      return mount.position().add(0.0, (double)mount.getBbHeight() + 0.15, 0.0);
   }

   public static void tick(ServerPlayer rider) {
      HitchControl.Latch latch = LATCHES.get(rider.getUUID());
      if (latch != null) {
         LivingEntity mount = latch.mount;
         if (mount.isAlive() && !mount.isRemoved() && mount.level() == rider.level() && rider.isAlive() && rider.getVehicle() == latch.vehicle()) {
            if (Scaling.stage(PowerService.data(rider)).scale() > SizeStage.SMALL.scale() + 0.001F) {
               detach(rider, false);
            } else {
               rider.fallDistance = 0.0F;
               if (latch.anchor != null) {
                  Vec3 p = ridePoint(mount);
                  latch.anchor.setPos(p.x, p.y, p.z);
                  latch.anchor.setDeltaMovement(Vec3.ZERO);
               }

               long gameTime = rider.level().getGameTime();
               if (!Scaling.isSpeck(rider) && gameTime >= latch.nextThrash) {
                  latch.nextThrash = gameTime + 60L;
                  thrash(rider, mount);
               }
            }
         } else {
            detach(rider, false);
         }
      }
   }

   public static void detach(ServerPlayer rider, boolean hop) {
      HitchControl.Latch latch = LATCHES.remove(rider.getUUID());
      if (latch != null) {
         if (rider.getVehicle() == latch.vehicle()) {
            rider.stopRiding();
         }

         if (latch.anchor != null) {
            latch.anchor.discard();
         }

         rider.fallDistance = 0.0F;
         if (hop) {
            rider.setDeltaMovement(0.0, 0.35, 0.0);
            rider.hurtMarked = true;
            if (rider.level() instanceof ServerLevel level) {
               Fx.burst(level, ParticleTypes.CLOUD, rider.position(), 6, 0.2, 0.03);
               Fx.sound(level, rider.position(), SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 0.6F, 1.6F);
            }
         }
      }
   }

   private static void thrash(ServerPlayer rider, LivingEntity mount) {
      RandomSource random = mount.getRandom();
      if (!(random.nextFloat() >= 0.35F)) {
         Harm.push(rider, mount, ((double)random.nextFloat() - 0.5) * 0.5, 0.28, ((double)random.nextFloat() - 0.5) * 0.5);
         if (mount instanceof Mob mob) {
            mob.setTarget(null);
         }

         if (mount.level() instanceof ServerLevel level) {
            Vec3 top = mount.position().add(0.0, (double)mount.getBbHeight(), 0.0);
            Fx.burst(level, ParticleTypes.CLOUD, top, 8, (double)mount.getBbWidth() * 0.5, 0.06);
            Fx.sound(level, mount.position(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 0.7F, 0.7F);
         }

         if (random.nextFloat() < 0.4F) {
            detach(rider, false);
            rider.setDeltaMovement(((double)random.nextFloat() - 0.5) * 0.6, 0.35, ((double)random.nextFloat() - 0.5) * 0.6);
            rider.hurtMarked = true;
            rider.displayClientMessage(Component.translatable("awakened.scaler.shaken_off"), true);
         }
      }
   }

   private static final class Latch {
      private final LivingEntity mount;
      private final GripAnchorEntity anchor;
      private long nextThrash;

      private Latch(LivingEntity mount, GripAnchorEntity anchor, long nextThrash) {
         this.mount = mount;
         this.anchor = anchor;
         this.nextThrash = nextThrash;
      }

      private Entity vehicle() {
         return (Entity)(this.anchor != null ? this.anchor : this.mount);
      }
   }
}
