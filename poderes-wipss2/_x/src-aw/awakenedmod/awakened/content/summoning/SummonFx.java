package com.awakenedmod.awakened.content.summoning;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class SummonFx {
   private static final int RISE_TICKS = 12;
   private static final double SINK = 1.5;
   private static final List<SummonFx.Rising> RISES = new CopyOnWriteArrayList<>();
   private static int lastTick = -1;

   private SummonFx() {
   }

   public static void gather(ServerLevel level, Vec3 spot) {
      level.sendParticles(ParticleTypes.SCULK_SOUL, spot.x, spot.y + 0.1, spot.z, 18, 0.4, 0.05, 0.4, 0.01);
      level.sendParticles(ParticleTypes.SOUL, spot.x, spot.y + 0.1, spot.z, 6, 0.3, 0.02, 0.3, 0.01);
      level.playSound(null, spot.x, spot.y, spot.z, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.6F, 0.5F);
      level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.5F, 0.7F);
   }

   public static void rise(Mob mob, Vec3 spot) {
      mob.setNoAi(true);
      mob.noPhysics = true;
      mob.setInvulnerable(true);
      mob.setPos(spot.x, spot.y - 1.5, spot.z);
      RISES.add(new SummonFx.Rising(mob, spot.y, 12, new int[]{0}));
   }

   public static boolean isRising(Mob mob) {
      for (SummonFx.Rising r : RISES) {
         if (r.mob() == mob) {
            return true;
         }
      }

      return false;
   }

   public static void tickAll(MinecraftServer server) {
      if (server.getTickCount() != lastTick) {
         lastTick = server.getTickCount();
         if (!RISES.isEmpty()) {
            for (SummonFx.Rising r : RISES) {
               Mob mob = r.mob();
               if (mob.isAlive() && mob.level() instanceof ServerLevel level) {
                  r.elapsed()[0]++;
                  int var13 = r.elapsed()[0];
                  double frac = Math.min(1.0, (double)var13 / (double)r.total());
                  double eased = 1.0 - (1.0 - frac) * (1.0 - frac);
                  double y = r.groundY() - 1.5 + 1.5 * eased;
                  mob.setPos(mob.getX(), y, mob.getZ());
                  BlockParticleOption dirt = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
                  level.sendParticles(dirt, mob.getX(), r.groundY() + 0.05, mob.getZ(), 6, 0.35, 0.05, 0.35, 0.05);
                  level.sendParticles(ParticleTypes.SCULK_SOUL, mob.getX(), y + (double)mob.getBbHeight() * 0.5, mob.getZ(), 2, 0.25, 0.4, 0.25, 0.0);
                  if (frac >= 1.0) {
                     settle(level, mob, r.groundY());
                     RISES.remove(r);
                  }
               } else {
                  RISES.remove(r);
               }
            }
         }
      }
   }

   private static void settle(ServerLevel level, Mob mob, double groundY) {
      mob.setPos(mob.getX(), groundY, mob.getZ());
      mob.setDeltaMovement(Vec3.ZERO);
      mob.noPhysics = false;
      if (!PossessControl.isPiloted(mob)) {
         mob.setNoAi(false);
      }

      mob.setInvulnerable(false);
      BlockParticleOption dirt = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
      level.sendParticles(dirt, mob.getX(), groundY + 0.05, mob.getZ(), 14, 0.5, 0.1, 0.5, 0.08);
      level.sendParticles(ParticleTypes.SCULK_SOUL, mob.getX(), groundY + (double)mob.getBbHeight() * 0.5, mob.getZ(), 10, 0.3, 0.4, 0.3, 0.01);
      level.playSound(null, mob.getX(), groundY, mob.getZ(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 0.7F, 0.7F);
   }

   public static void cancel(Mob mob) {
      RISES.removeIf(r -> r.mob() == mob);
   }

   public static void settleNow(ServerLevel level, Mob mob) {
      for (SummonFx.Rising r : RISES) {
         if (r.mob() == mob) {
            settle(level, mob, r.groundY());
            RISES.remove(r);
            return;
         }
      }
   }

   private static record Rising(Mob mob, double groundY, int total, int[] elapsed) {
   }
}
