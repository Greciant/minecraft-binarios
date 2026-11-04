package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.TeleportGuard;
import com.awakenedmod.awakened.core.TeleportUtil;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class SwapsWithMeEffect implements Effect {
   public static final String SAY_BLOCKED = "awakened.command.swap_blocked";
   private static final double NUDGE_RADIUS = 2.0;
   private static final HoldMemory<UUID> SWAPPED = new HoldMemory<>();
   private static final Map<UUID, Integer> LAST_SWAP = new ConcurrentHashMap<>();

   @Override
   public String id() {
      return "swaps_with_me";
   }

   @Override
   public int unlockLevel() {
      return 5;
   }

   @Override
   public double defaultWeight() {
      return 20.0;
   }

   @Override
   public boolean supports(TargetKind kind) {
      return kind != TargetKind.SELF && kind != TargetKind.NEAR;
   }

   @Override
   public boolean instant() {
      return true;
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      if (age == 0 && target != caster && SWAPPED.take(target, age, caster::getUUID)) {
         if (target.level() == caster.level() && caster.level() instanceof ServerLevel world) {
            int var13 = world.getServer().getTickCount();
            Integer last = LAST_SWAP.get(caster.getUUID());
            if (last != null && last == var13) {
               return false;
            } else {
               Vec3 casterFrom = caster.position();
               Vec3 bodyFrom = target.position();
               Vec3 casterTo = TeleportUtil.nearestFittingSpot(caster, bodyFrom, 2.0);
               Vec3 bodyTo = TeleportUtil.nearestFittingSpot(target, casterFrom, 2.0);
               label33:
               if (casterTo != null && bodyTo != null && !TeleportGuard.crosses(caster, casterFrom, casterTo)) {
                  if (target instanceof ServerPlayer other && TeleportGuard.crosses(other, bodyFrom, bodyTo)) {
                     break label33;
                  }

                  LAST_SWAP.values().removeIf(swapped -> swapped != var13);
                  LAST_SWAP.put(caster.getUUID(), var13);
                  caster.stopRiding();
                  target.stopRiding();
                  TeleportUtil.moveBody(caster, casterTo);
                  TeleportUtil.moveBody(target, bodyTo);
                  puff(world, casterFrom, caster);
                  puff(world, bodyFrom, target);
                  return true;
               }

               caster.displayClientMessage(Component.translatable("awakened.command.swap_blocked"), true);
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public void release(LivingEntity target) {
      SWAPPED.release(target);
   }

   @Override
   public void forget(UUID bodyId) {
      SWAPPED.forget(bodyId);
   }

   public static void forgetAll() {
      SWAPPED.clear();
      LAST_SWAP.clear();
   }

   private static void puff(ServerLevel world, Vec3 at, Entity body) {
      Fx.burst(world, ParticleTypes.REVERSE_PORTAL, at.add(0.0, (double)body.getBbHeight() * 0.5, 0.0), 24, 0.3, 0.1);
      Fx.sound(world, at, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.3F);
   }
}
