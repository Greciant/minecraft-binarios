package com.awakenedmod.awakened.content.entity.boss;

import com.awakenedmod.awakened.core.Fx;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public final class BossDefenses {
   private BossDefenses() {
   }

   public static boolean isSnipe(LivingEntity boss, DamageSource source, double maxRange) {
      Entity shooter = source.getEntity();
      Vec3 pos = shooter != null ? shooter.position() : source.getSourcePosition();
      return pos == null ? false : pos.distanceToSqr(boss.position()) > maxRange * maxRange;
   }

   public static void deflectFx(ServerLevel level, LivingEntity boss, @Nullable Entity attacker) {
      Vec3 mid = boss.position().add(0.0, (double)boss.getBbHeight() * 0.6, 0.0);
      Fx.burst(level, ParticleTypes.ENCHANTED_HIT, mid, 12, 0.8, 0.05);
      Fx.burst(level, Fx.dust(16756768, 1.4F), mid, 8, 0.8, 0.02);
      Fx.sound(level, boss.position(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.2F, 1.8F);
      if (attacker instanceof ServerPlayer player) {
         player.displayClientMessage(Component.translatable("entity.awakened.boss.deflected"), true);
      }
   }

   public static float percentDamage(LivingEntity target, float scaledBase, float pctMaxHp) {
      return scaledBase + target.getMaxHealth() * pctMaxHp;
   }

   public static void disableShield(ServerLevel level, LivingEntity victim, int ticks) {
      if (victim instanceof Player player && player.isBlocking()) {
         player.getCooldowns().addCooldown(player.getUseItem().getItem(), ticks);
         player.stopUsingItem();
         level.broadcastEntityEvent(player, (byte)30);
         Fx.burst(level, ParticleTypes.CRIT, player.position().add(0.0, 1.0, 0.0), 10, 0.4, 0.1);
         Fx.sound(level, player.position(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.9F, 0.8F);
         Fx.shake(level, player.position(), 12.0, 0.15F, 0, 8);
         return;
      }
   }

   public static boolean blockBreakTick(ServerLevel level, LivingEntity boss) {
      boolean broke = false;
      AABB box = boss.getBoundingBox().inflate(0.2);

      for (BlockPos pos : BlockPos.betweenClosed(
         (int)Math.floor(box.minX),
         (int)Math.floor(box.minY),
         (int)Math.floor(box.minZ),
         (int)Math.floor(box.maxX),
         (int)Math.floor(box.maxY),
         (int)Math.floor(box.maxZ)
      )) {
         BlockState state = level.getBlockState(pos);
         if (!state.isAir() && state.canEntityDestroy(level, pos, boss) && EventHooks.onEntityDestroyBlock(boss, pos, state)) {
            broke |= level.destroyBlock(pos, true, boss);
         }
      }

      if (broke) {
         level.levelEvent(1022, boss.blockPosition(), 0);
         Fx.shake(level, boss.position(), 12.0, 0.15F, 0, 8);
      }

      return broke;
   }

   public static record Profile(double sniperRange, float perHitCap, double towerHeight, int hiddenGraceTicks) {
   }
}
