package com.awakenedmod.awakened.content.chronokinetic;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.TickFreeze;
import com.awakenedmod.awakened.core.net.TimeFrozenPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ChronoSupport {
   private ChronoSupport() {
   }

   public static void freeze(LivingEntity target) {
      TickFreeze.freeze(target);
      ChronoHeld.hold(target);
      if (target instanceof Player) {
         holdPlayer(target);
         if (target instanceof ServerPlayer caught) {
            PacketDistributor.sendToPlayer(caught, new TimeFrozenPayload(true), new CustomPacketPayload[0]);
         }
      }
   }

   public static void hold(LivingEntity target, Vec3 anchor) {
      if (target instanceof Player) {
         target.setDeltaMovement(Vec3.ZERO);
         target.setPos(anchor.x, anchor.y, anchor.z);
         target.hurtMarked = true;
         target.resetFallDistance();
         holdPlayer(target);
      }
   }

   private static void holdPlayer(LivingEntity target) {
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 250, false, false));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 6, 250, false, false));
   }

   public static void release(LivingEntity target) {
      TickFreeze.release(target);
      ChronoHeld.let(target);
      target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
      target.removeEffect(MobEffects.WEAKNESS);
      if (target instanceof ServerPlayer freed) {
         PacketDistributor.sendToPlayer(freed, new TimeFrozenPayload(false), new CustomPacketPayload[0]);
      }
   }

   public static boolean strike(ServerLevel level, ServerPlayer player, LivingEntity target, float damage) {
      boolean hit = Dmg.hurt(target, level, player.damageSources().playerAttack(player), damage);
      if (hit) {
         target.hurtMarked = true;
      }

      return hit;
   }

   public static void freezeProjectile(Projectile p) {
      TickFreeze.freeze(p);
   }

   public static void releaseProjectile(Projectile p) {
      TickFreeze.release(p);
   }

   public static void shimmer(ServerLevel level, Vec3 pos, int count, double spread) {
      Fx.burst(level, ParticleTypes.END_ROD, pos, count, spread, 0.0);
      Fx.burst(level, ParticleTypes.ENCHANT, pos, count, spread, 0.02);
   }
}
