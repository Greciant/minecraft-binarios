package com.awakenedmod.awakened.content.hydro;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public final class Hydro {
   public static final float WET_BONUS = 0.25F;
   public static final double NEARBY_RANGE = 12.0;
   private static final int MIN_BODY_SOURCES = 6;
   private static final int BODY_CACHE_TICKS = 10;
   private static final DustParticleOptions WATER_DUST = Fx.dust(3051007, 1.5F);
   private static final DustParticleOptions MIST_DUST = Fx.dust(12115184, 0.9F);
   private static final Map<ServerPlayer, long[]> BODY_CACHE = new WeakHashMap<>();

   private Hydro() {
   }

   public static Hydro.Source resolve(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      if (level.dimension() == Level.NETHER) {
         return Hydro.Source.NETHER;
      } else if (level.isRainingAt(player.blockPosition())) {
         return Hydro.Source.RAIN;
      } else if (player.isInWater() || player.isUnderWater()) {
         return Hydro.Source.STANDING;
      } else if (hasWaterBodyCached(player)) {
         return Hydro.Source.NEARBY;
      } else {
         return VesselItem.available(player) > 0.0F ? Hydro.Source.VESSEL : Hydro.Source.AIR;
      }
   }

   public static Hydro.Cast peek(ServerPlayer player) {
      Hydro.Source src = resolve(player);
      return new Hydro.Cast(src, originOf(player, src));
   }

   public static Hydro.Cast take(ServerPlayer player, float want) {
      player.clearFire();
      Hydro.Cast cast = peek(player);
      if (cast.source() == Hydro.Source.VESSEL) {
         VesselItem.drain(player, want);
         PowerService.sync(player);
      }

      return cast;
   }

   public static Hydro.Source takeTick(ServerPlayer player, float want) {
      Hydro.Source src = resolve(player);
      if (src == Hydro.Source.VESSEL) {
         VesselItem.drain(player, want);
      }

      return src;
   }

   public static DamageSource source(ServerPlayer player) {
      return Dmg.by(player, DamageTypes.PLAYER_ATTACK);
   }

   public static boolean hit(ServerPlayer player, Entity target, float amount) {
      target.clearFire();
      return Dmg.hurt(target, player.serverLevel(), source(player), amount);
   }

   public static float damage(ServerPlayer player, Ability ability, float base, Hydro.Cast cast) {
      return MasteryScaling.scaledDamage(base, player, ability.balanceKey()) * (cast.wet() ? 1.25F : 1.0F);
   }

   public static void show(ServerPlayer player, Hydro.Cast cast, Vec3 castPoint) {
      ServerLevel level = player.serverLevel();
      if (cast.wet()) {
         ribbon(level, cast.origin(), castPoint, cast.source());
      } else {
         condense(level, castPoint);
         if (cast.source() == Hydro.Source.NETHER) {
            evaporate(level, castPoint);
         }
      }
   }

   private static void water(ServerLevel level, Vec3 pos, int count, double spread) {
      Fx.burst(level, WATER_DUST, pos, count, spread, 0.0);
   }

   private static void ribbon(ServerLevel level, Vec3 from, Vec3 to, Hydro.Source src) {
      HydroFx.ribbon(level, from, to, true);
      if (src == Hydro.Source.RAIN) {
         water(level, to.add(0.0, 1.4, 0.0), 8, 0.7);
      }
   }

   private static void condense(ServerLevel level, Vec3 at) {
      for (int i = 0; i < 6; i++) {
         double a = (double)i * (Math.PI / 3);
         Vec3 out = new Vec3(Math.cos(a) * 1.2, i % 2 == 0 ? 0.4 : -0.3, Math.sin(a) * 1.2);
         Vec3 p = at.add(out);
         Vec3 in = out.scale(-0.12);
         level.sendParticles(MIST_DUST, p.x, p.y, p.z, 0, in.x, in.y, in.z, 1.0);
      }
   }

   public static void evaporate(ServerLevel level, Vec3 at) {
      Fx.burst(level, ParticleTypes.WHITE_SMOKE, at, 6, 0.2, 0.03);
      Fx.sound(level, at, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.3F, 1.6F);
   }

   private static Vec3 originOf(ServerPlayer player, Hydro.Source src) {
      return switch (src) {
         case RAIN -> player.getEyePosition().add(0.0, 1.5, 0.0);
         case STANDING -> player.position();
         case NEARBY -> {
            BlockPos near = nearestWater(player, 12.0);
            yield near != null ? Vec3.atCenterOf(near).add(0.0, 0.4, 0.0) : player.position();
         }
         case VESSEL, AIR, NETHER -> AimUtil.handPosition(player);
      };
   }

   private static boolean hasWaterBodyCached(ServerPlayer player) {
      long now = player.serverLevel().getGameTime();
      long[] entry = BODY_CACHE.get(player);
      if (entry == null || now - entry[0] >= 10L || now < entry[0]) {
         entry = new long[]{now, hasWaterBody(player, 12.0) ? 1L : 0L};
         BODY_CACHE.put(player, entry);
      }

      return entry[1] == 1L;
   }

   private static boolean hasWaterBody(ServerPlayer player, double range) {
      ServerLevel level = player.serverLevel();
      BlockPos center = player.blockPosition();
      int r = (int)Math.ceil(range);
      int sources = 0;
      MutableBlockPos cursor = new MutableBlockPos();

      for (int dx = -r; dx <= r; dx++) {
         for (int dz = -r; dz <= r; dz++) {
            for (int dy = -4; dy <= 2; dy++) {
               cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
               FluidState fluid = level.getFluidState(cursor);
               if (fluid.isSource() && fluid.is(FluidTags.WATER)) {
                  if (++sources >= 6) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private static BlockPos nearestWater(ServerPlayer player, double range) {
      ServerLevel level = player.serverLevel();
      BlockPos center = player.blockPosition();
      int r = (int)Math.ceil(range);
      double bestSq = range * range;
      BlockPos best = null;
      MutableBlockPos cursor = new MutableBlockPos();

      for (int dx = -r; dx <= r; dx++) {
         for (int dz = -r; dz <= r; dz++) {
            for (int dy = -4; dy <= 2; dy++) {
               cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
               if (level.getFluidState(cursor).is(FluidTags.WATER)) {
                  double sq = player.position().distanceToSqr((double)cursor.getX() + 0.5, (double)cursor.getY() + 0.5, (double)cursor.getZ() + 0.5);
                  if (sq <= bestSq) {
                     bestSq = sq;
                     best = cursor.immutable();
                  }
               }
            }
         }
      }

      return best;
   }

   public static record Cast(Hydro.Source source, Vec3 origin) {
      public boolean wet() {
         return this.source.wet();
      }
   }

   public static enum Source {
      RAIN,
      STANDING,
      NEARBY,
      VESSEL,
      AIR,
      NETHER;

      public boolean wet() {
         return this != AIR && this != NETHER;
      }

      public boolean free() {
         return this == RAIN || this == STANDING || this == NEARBY;
      }
   }
}
