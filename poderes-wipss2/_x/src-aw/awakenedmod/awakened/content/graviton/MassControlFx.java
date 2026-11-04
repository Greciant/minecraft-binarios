package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.BlockCracks;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Shockwaves;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

final class MassControlFx {
   private static final int BODY_PERIOD = 4;
   private static final int CRACK_REFRESH = 8;
   private static final int CRACK_LEASE = 14;
   private static final int[] STAND_CENTRE = new int[]{-1, 3, 4, 6};
   private static final int[] STAND_EDGE = new int[]{-1, 1, 2, 3};
   private static final double[] STAND_RADIUS = new double[]{0.0, 0.6, 1.0, 1.5};
   private static final int TRAIL_STAGE = 4;
   private static final int TRAIL_HOLD = 6;
   private static final int TRAIL_FADE = 24;
   private static final double LIGHT_FLOOR_REACH = 1.5;
   private static final int FLOAT_RING_PERIOD = 10;
   private static final int BEAT_TICKS = 6;
   private static final int BEAT_RING_LIFE = 4;
   private static final int BEAT_CRACK_TICKS = 8;
   private static final DustParticleOptions PALE_MOTE = Fx.dust(16769732, 0.8F);
   private static final float FLOOR_DUST_LIFT = 0.25F;
   private static final Map<UUID, MassControlFx.State> STATE = new ConcurrentHashMap<>();
   static volatile boolean showOwnBody;

   private MassControlFx() {
   }

   private static Object standOwner(ServerPlayer player) {
      return BlockCracks.owner(player, "mass_stand");
   }

   private static Object trailOwner(ServerPlayer player) {
      return BlockCracks.owner(player, "mass_trail");
   }

   private static Object beatOwner(ServerPlayer player) {
      return BlockCracks.owner(player, "mass_beat");
   }

   private static Object landOwner(ServerPlayer player) {
      return BlockCracks.owner(player, "mass_land");
   }

   static void forget(ServerPlayer player) {
      STATE.remove(player.getUUID());
      BlockCracks.clear(standOwner(player));
      BlockCracks.clear(trailOwner(player));
      BlockCracks.clear(beatOwner(player));
   }

   static void body(ServerLevel level, ServerPlayer player, int tier) {
      MassControlFx.State st = STATE.computeIfAbsent(player.getUUID(), id -> new MassControlFx.State());
      tickBeat(level, player, st);
      if (!player.isInvisible() && !player.isSpectator()) {
         if (tier > 0) {
            heavy(level, player, st, tier);
         } else {
            dropStanding(level, player, st);
            if (tier < 0) {
               light(level, player, -tier);
            }
         }
      } else {
         dropStanding(level, player, st);
      }
   }

   private static void dropStanding(ServerLevel level, ServerPlayer player, MassControlFx.State st) {
      if (st.floor != null) {
         BlockCracks.clear(standOwner(player));
         st.floor = null;
      }
   }

   private static void heavy(ServerLevel level, ServerPlayer player, MassControlFx.State st, int n) {
      double x = player.getX();
      double y = player.getY();
      double z = player.getZ();
      if (player.onGround()) {
         BlockPos floor = BlockCracks.floorTop(level, x, y - 0.5, z);
         boolean moved = !Objects.equals(floor, st.floor);
         if (moved && st.floor != null && n == 3) {
            BlockCracks.crack(level, trailOwner(player), st.floor, 4, BlockCracks.Timing.holdThenFade(6, 24));
         }

         if (floor == null) {
            if (moved) {
               BlockCracks.clear(standOwner(player));
            }
         } else if (moved || player.tickCount % 8 == 0) {
            List<BlockPos> now = BlockCracks.disc(
               level, standOwner(player), player.position(), STAND_RADIUS[n], STAND_CENTRE[n], STAND_EDGE[n], BlockCracks.Timing.lease(14)
            );
            if (moved) {
               BlockCracks.clearExcept(standOwner(player), now);
            }
         }

         st.floor = floor;
      } else {
         st.floor = null;
      }

      if (player.tickCount % 4 == 0 && player.onGround()) {
         double dx = x - player.xo;
         double dz = z - player.zo;
         if (dx * dx + dz * dz > 0.001) {
            Fx.Floor floorx = Fx.floorUnder(level, x, y - 0.2, z, 2);
            if (floorx != null) {
               level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floorx.state()), x, floorx.topY() + 0.05, z, n, 0.25, 0.02, 0.25, 0.02);
            }
         }
      }
   }

   private static void light(ServerLevel level, ServerPlayer player, int n) {
      double x = player.getX();
      double y = player.getY();
      double z = player.getZ();
      Fx.Floor floor = Fx.floorUnder(level, x, y - 0.05, z, (int)Math.ceil(1.5) + 1);
      boolean nearFloor = floor != null && y - floor.topY() <= 1.5;
      if (n == 3 && !nearFloor && player.tickCount % 10 == 0) {
         Vfx.effect(VfxEntity.Kind.RING, new Vec3(x, y + 0.02, z)).scale(0.5).color(16769732).lifetime(6).intensity(0.45F).spawn(level);
      }

      if (player.tickCount % 4 == 0) {
         if (nearFloor) {
            floorDust(level, floor, x, z, 2 * n, 0.4, 0.03);
            if (n >= 2 && player.onGround()) {
               level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor.state()), x, floor.topY() + 0.05, z, n - 1, 0.3, 0.0, 0.3, 0.0);
            }
         }

         for (int i = 0; i < n + 1; i++) {
            double a = level.random.nextDouble() * Math.PI * 2.0;
            double r = 0.6 + level.random.nextDouble() * 0.2;
            double my = y + 0.2 + level.random.nextDouble() * 0.9;
            exceptOwner(level, player, PALE_MOTE, x + Math.cos(a) * r, my, z + Math.sin(a) * r, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   private static void floorDust(ServerLevel level, Fx.Floor floor, double x, double z, int count, double spread, double rise) {
      BlockPos pos = BlockPos.containing(x, floor.topY() - 0.5, z);
      int rgb = floorColor(level, floor.state(), pos);
      DustParticleOptions dust = Fx.dust(rgb, 1.0F);

      for (int i = 0; i < count; i++) {
         double px = x + (level.random.nextDouble() * 2.0 - 1.0) * spread;
         double pz = z + (level.random.nextDouble() * 2.0 - 1.0) * spread;
         level.sendParticles(dust, px, floor.topY() + 0.06, pz, 0, 0.0, 1.0, 0.0, rise * (0.7 + level.random.nextDouble() * 0.6));
      }
   }

   private static int floorColor(ServerLevel level, BlockState state, BlockPos pos) {
      int c = state.getMapColor(level, pos).col;
      if (c == 0) {
         c = 8421504;
      }

      int r = c >> 16 & 0xFF;
      int g = c >> 8 & 0xFF;
      int b = c & 0xFF;
      r += Math.round((float)(255 - r) * 0.25F);
      g += Math.round((float)(255 - g) * 0.25F);
      b += Math.round((float)(255 - b) * 0.25F);
      return r << 16 | g << 8 | b;
   }

   static void changed(ServerLevel level, ServerPlayer player, int from, int to) {
      MassControlFx.State st = STATE.computeIfAbsent(player.getUUID(), id -> new MassControlFx.State());
      st.beatKind = to == 0 ? 0 : Integer.signum(to - from);
      st.beatAge = 0;
      Vec3 pos = player.position();
      Fx.Floor floor = Fx.floorUnder(level, pos.x, pos.y - 0.2, pos.z, 2);
      float pitch = ladderPitch(to);
      st.floor = null;
      if (st.beatKind > 0) {
         BlockPos under = BlockCracks.floorTop(level, pos.x, pos.y - 0.5, pos.z);
         if (under != null && player.onGround() && to > 0) {
            BlockCracks.crack(level, beatOwner(player), under, Math.min(9, STAND_CENTRE[to] + 3), BlockCracks.Timing.lease(8));
         }

         if (floor != null && player.onGround()) {
            BlockParticleOption chip = new BlockParticleOption(ParticleTypes.BLOCK, floor.state());

            for (int i = 0; i < 6; i++) {
               double a = (double)i * Math.PI / 3.0 + level.random.nextDouble() * 0.5;
               level.sendParticles(chip, pos.x + Math.cos(a) * 0.3, floor.topY() + 0.05, pos.z + Math.sin(a) * 0.3, 0, Math.cos(a), 0.1, Math.sin(a), 0.25);
            }

            play(level, pos, floor.state().getSoundType().getPlaceSound(), 0.5F, pitch * 0.8F);
         }

         play(level, pos, (SoundEvent)AwakenedSounds.MASS_HEAVIER.get(), 0.7F, pitch);
         if (to == 3) {
            Fx.shakeFor(player, player.getEyePosition(), 4.0, 0.03F, 2, 4);
         }
      } else if (st.beatKind < 0) {
         BlockCracks.clear(standOwner(player));
         BlockCracks.clear(beatOwner(player));
         if (floor != null && pos.y - floor.topY() <= 1.5) {
            floorDust(level, floor, pos.x, pos.z, 8, 0.45, 0.06);
         }

         play(level, pos, (SoundEvent)AwakenedSounds.MASS_LIGHTER.get(), 0.7F, pitch);
      } else {
         BlockCracks.clear(standOwner(player));
         BlockCracks.clear(beatOwner(player));
         play(level, pos, (SoundEvent)AwakenedSounds.MASS_RESET.get(), 0.7F, pitch);
      }
   }

   private static float ladderPitch(int tier) {
      return (float)Math.pow(2.0, (double)(-tier) * 3.0 / 12.0);
   }

   private static void play(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
      level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
   }

   private static void tickBeat(ServerLevel level, ServerPlayer player, MassControlFx.State st) {
      if (st.beatAge < 6) {
         int i = st.beatAge++;
         float t = (float)i / 5.0F;
         Vec3 feet = player.position();
         if (st.beatKind > 0) {
            ring(level, feet.add(0.0, 0.05, 0.0), 1.4 - 1.0 * (double)t, 16747069, 4, 1.0F);
         } else if (st.beatKind < 0) {
            ring(level, feet.add(0.0, 0.05 + 0.45 * (double)t, 0.0), 0.4 + 0.9 * (double)t, 16769732, 4, 0.9F);
         } else if (i == 0) {
            ring(level, feet.add(0.0, 0.05, 0.0), 0.8, 16769732, 8, 0.55F);
         }
      }
   }

   private static void ring(ServerLevel level, Vec3 at, double radius, int rgb, int life, float intensity) {
      Vfx.effect(VfxEntity.Kind.HALO, at).scale(radius).color(rgb).lifetime(life).intensity(intensity).spawn(level);
   }

   static void landing(ServerLevel level, ServerPlayer player, double radius) {
      Vec3 feet = player.position();
      BlockCracks.disc(level, landOwner(player), feet, 2.5, 8, 4, BlockCracks.Timing.holdThenFade(6, 36));
      Fx.groundShockwave(level, feet, 0.5, radius, 6, Shockwaves.Style.DEFAULT.silent().noGlowRing().noShake());

      for (int i = 0; i < 5; i++) {
         double a = (double)i * Math.PI * 2.0 / 5.0 + level.random.nextDouble() * 0.8;
         double r = 0.9 + level.random.nextDouble() * 0.9;
         double px = feet.x + Math.cos(a) * r;
         double pz = feet.z + Math.sin(a) * r;
         Fx.Floor f = Fx.floorUnder(level, px, feet.y - 0.2, pz, 2);
         if (f != null) {
            Vfx.blockHeave(level, new Vec3(px, f.topY(), pz), f.state(), 0.3, 0.3, 0.3, 14, level.random.nextFloat() * 360.0F, 0.3, 12.0F, i % 2);
         }
      }

      Fx.Floor floor = Fx.floorUnder(level, feet.x, feet.y - 0.2, feet.z, 2);
      Fx.sound(level, feet, (SoundEvent)AwakenedSounds.MASS_LAND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
      if (floor != null) {
         play(level, feet, floor.state().getSoundType().getBreakSound(), 0.6F, 0.55F);
      }

      Fx.shake(level, feet, 8.0, 0.08F, 3, 6);
      Fx.shakeFor(player, player.getEyePosition(), 4.0, 0.05F, 2, 6);
   }

   static void thump(ServerLevel level, ServerPlayer player) {
      Vec3 feet = player.position();
      Fx.Floor floor = Fx.floorUnder(level, feet.x, feet.y - 0.2, feet.z, 2);
      if (floor != null) {
         play(level, feet, floor.state().getSoundType().getFallSound(), 0.8F, 0.7F);
      }

      Fx.sound(level, feet, (SoundEvent)AwakenedSounds.MASS_LAND.get(), SoundSource.PLAYERS, 0.35F, 1.25F);
   }

   private static void exceptOwner(
      ServerLevel level, ServerPlayer owner, ParticleOptions p, double x, double y, double z, int count, double dx, double dy, double dz, double speed
   ) {
      for (ServerPlayer viewer : level.players()) {
         if ((viewer != owner || showOwnBody) && !(viewer.distanceToSqr(x, y, z) > 1024.0)) {
            level.sendParticles(viewer, p, false, x, y, z, count, dx, dy, dz, speed);
         }
      }
   }

   private static final class State {
      BlockPos floor;
      int beatKind;
      int beatAge = 6;
   }
}
