package com.awakenedmod.awakened.content.sun;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.AwakenedParticles;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.LightBlocks;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class PersonalSunAbility extends Ability {
   private static final float COST = 100.0F;
   private static final int DURATION = 600;
   private static final int SUN_HEIGHT = 13;
   private static final float R = 4.5F;
   private final Map<UUID, PersonalSunAbility.Sun> suns = this.playerState();
   private static final int[][] LIGHT_PATTERN = new int[][]{
      {0, 0, 0},
      {6, 0, 0},
      {-6, 0, 0},
      {0, 0, 6},
      {0, 0, -6},
      {4, 0, 4},
      {4, 0, -4},
      {-4, 0, 4},
      {-4, 0, -4},
      {12, 0, 0},
      {-12, 0, 0},
      {0, 0, 12},
      {0, 0, -12},
      {9, 0, 9},
      {9, 0, -9},
      {-9, 0, 9},
      {-9, 0, -9},
      {0, 5, 0},
      {0, 9, 0},
      {0, -4, 0}
   };

   public PersonalSunAbility() {
      super("personal_sun", 100.0F, 10);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      this.extinguish(player);
      PowerData data = PowerService.data(player);
      Vec3 sun = player.position().add(0.0, 13.0, 0.0);
      Solar.lightPersonalSun(player, data, 600, sun);
      ServerLevel level = player.serverLevel();
      int orbId = this.spawnOrb(level, sun);
      List<BlockPos> lights = this.placeLights(level, player.blockPosition().above(2));
      this.suns.put(player.getUUID(), new PersonalSunAbility.Sun(level, lights, orbId));
      Fx.burst(level, (ParticleOptions)AwakenedParticles.SUN_MOTE.get(), sun, 120, 4.5, 0.05);
      Fx.sound(level, player.position(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4F, 0.7F);
      Fx.sound(level, player.position(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F, 0.5F);
      player.displayClientMessage(Component.translatable("awakened.sun.psun_summon"), true);
      return true;
   }

   @Override
   public void serverTick(ServerPlayer player) {
      PersonalSunAbility.Sun lit = this.suns.get(player.getUUID());
      if (lit != null) {
         PowerData data = PowerService.data(player);
         if (Solar.personalSun(player, data)) {
            Vec3 sun = Solar.personalSunPos(data);
            ServerLevel level = lit.level;
            RandomSource rnd = level.getRandom();

            for (int i = 0; i < 7; i++) {
               Vec3 dir = randomDir(rnd);
               Vec3 p = sun.add(dir.scale(4.59));
               double sp = 0.12 + rnd.nextDouble() * 0.18;
               level.sendParticles(AwakenedParticles.SUN_MOTE.get(), p.x, p.y, p.z, 0, dir.x * sp, dir.y * sp, dir.z * sp, 1.0);
            }
         } else {
            this.extinguish(player);
         }
      }
   }

   private int spawnOrb(ServerLevel level, Vec3 sun) {
      SunOrbEntity e = new SunOrbEntity(AwakenedEntities.SUN_ORB.get(), level);
      e.setPos(sun.x, sun.y, sun.z);
      e.configure(4.5F, 605);
      level.addFreshEntity(e);
      return e.getId();
   }

   private static Vec3 randomDir(RandomSource rnd) {
      double u = rnd.nextDouble() * 2.0 - 1.0;
      double th = rnd.nextDouble() * Math.PI * 2.0;
      double r = Math.sqrt(Math.max(0.0, 1.0 - u * u));
      return new Vec3(Math.cos(th) * r, u, Math.sin(th) * r);
   }

   private List<BlockPos> placeLights(ServerLevel level, BlockPos center) {
      List<BlockPos> placed = new ArrayList<>();

      for (int[] o : LIGHT_PATTERN) {
         BlockPos p = center.offset(o[0], o[1], o[2]);
         if (LightBlocks.placeInAir(level, p)) {
            placed.add(p);
         }
      }

      return placed;
   }

   private void extinguish(ServerPlayer player) {
      PersonalSunAbility.Sun sun = this.suns.remove(player.getUUID());
      Solar.extinguishPersonalSun(PowerService.data(player));
      if (sun != null) {
         for (BlockPos p : sun.lights) {
            LightBlocks.remove(sun.level, p);
         }

         Entity e = sun.level.getEntity(sun.orbId);
         if (e != null) {
            e.discard();
         }
      }
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      this.extinguish(player);
      super.clearPlayer(player);
   }

   private static record Sun(ServerLevel level, List<BlockPos> lights, int orbId) {
   }
}
