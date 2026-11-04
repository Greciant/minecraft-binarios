package com.awakenedmod.awakened.content.scaler;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class ScalerFx {
   public static final int MINT = 6021288;
   public static final int PALE = 12582880;
   public static final int DEEP = 3050598;
   private static final double SHAKE_RANGE_BASE = 10.0;
   private static final double SHAKE_RANGE_PER_RADIUS = 3.0;
   private static final double SHAKE_TICKS_BASE = 4.0;
   private static final double SHAKE_TICKS_PER_SCALE = 2.5;

   private ScalerFx() {
   }

   public static void shift(ServerPlayer player, boolean growing, double silhouetteScale) {
      ServerLevel level = player.serverLevel();
      Vec3 feet = player.position();
      double radius = 0.7 + silhouetteScale * 0.45;
      int count = (int)(8.0 + silhouetteScale * 6.0);
      Vfx.ring(level, feet.add(0.0, 0.1, 0.0), radius, growing ? 6021288 : 12582880, 10);
      Fx.burst(level, ParticleTypes.CLOUD, feet.add(0.0, 0.2, 0.0), count, radius * 0.5, 0.04);
      Fx.burst(level, ParticleTypes.CRIT, feet.add(0.0, silhouetteScale * 0.5, 0.0), count / 2, radius * 0.4, 0.08);
      if (growing) {
         Fx.sound(level, feet, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.7F, 0.55F);
      } else {
         Fx.sound(level, feet, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.9F, 1.8F);
      }
   }

   public static void screenShake(ServerLevel level, Vec3 center, double range, int durationTicks) {
      Fx.screenShake(level, center, range, durationTicks, 6021288, true, true);
   }

   public static List<LivingEntity> shockwave(ServerPlayer player, Vec3 center, double radius, float damage, double knockback, double knockUp, float stageScale) {
      ServerLevel level = player.serverLevel();
      Vfx.ring(level, center.add(0.0, 0.15, 0.0), radius, 6021288, 12);
      Vfx.ring(level, center.add(0.0, 0.1, 0.0), radius * 0.55, 12582880, 9);
      Fx.burst(level, ParticleTypes.CLOUD, center.add(0.0, 0.3, 0.0), (int)(14.0 + radius * 5.0), radius * 0.45, 0.12);
      BlockPos below = BlockPos.containing(center.x, center.y - 0.4, center.z);
      BlockState ground = level.getBlockState(below);
      if (!ground.isAir()) {
         Fx.burst(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), center.add(0.0, 0.25, 0.0), (int)(10.0 + radius * 6.0), radius * 0.5, 0.18);
      }

      Fx.sound(level, center, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 0.55F);
      Fx.sound(level, center, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.0F, 0.7F);
      screenShake(level, center, 10.0 + radius * 3.0, (int)Math.round(4.0 + (double)stageScale * 2.5));
      DamageSource source = Dmg.by(player, DamageTypes.PLAYER_ATTACK);
      List<LivingEntity> struck = new ArrayList<>();

      for (LivingEntity victim : Harm.only(player, AimUtil.livingAround(player, center, radius))) {
         Dmg.hurt(victim, level, source, damage);
         Vec3 push = new Vec3(victim.getX() - center.x, 0.0, victim.getZ() - center.z);
         push = push.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : push.normalize();
         victim.push(push.x * knockback, knockUp, push.z * knockback);
         victim.hurtMarked = true;
         struck.add(victim);
      }

      return struck;
   }
}
