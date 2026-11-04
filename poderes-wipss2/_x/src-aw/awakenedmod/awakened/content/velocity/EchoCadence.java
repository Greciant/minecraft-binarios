package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import com.awakenedmod.awakened.core.power.PassiveToggle;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class EchoCadence {
   static final PassiveToggle ECHOES = new PassiveToggle(
      "velocity_echoes", "awakened.screen.setting_velocity_echoes", "awakened.screen.setting_velocity_echoes_desc", true
   );
   static final int RUN_ECHO_INTERVAL_TICKS = 3;
   static final int PHASE_ECHO_INTERVAL_TICKS = 2;
   static final int RUN_ECHO_MIN_GEAR = 2;
   static final double ECHO_MIN_SPEED = 0.05;
   static final int PHASE_SPARKS_MIN = 2;
   static final int PHASE_SPARKS_MAX = 3;
   static final double PHASE_SPARK_SPREAD = 0.3;
   static final double PHASE_SPARK_LIFT = 0.15;
   static final double PHASE_SPARK_SPEED = 0.04;
   private static final float STRIDE_SPEED_PER_BLOCK = 4.0F;
   private static final float STRIDE_EASE = 0.4F;
   private static final Map<UUID, EchoCadence.Stride> STRIDES = new ConcurrentHashMap<>();

   private EchoCadence() {
   }

   static void tick(ServerPlayer player, PowerData data) {
      Vec3 move = player.getKnownMovement();
      double speed = move.horizontalDistance();
      stride(player).update((float)speed);
      VelocityVisual vis = VelocityVisualState.of(player);
      int interval = !ECHOES.enabled(data) ? 0 : (vis.phasing() ? 2 : (vis.running() && vis.gear() >= 2 ? 3 : 0));
      if (interval > 0 && speed >= 0.05 && player.tickCount % interval == 0) {
         VelocityFx.speedEcho(player);
      }

      if (vis.phasing()) {
         int count = 2 + player.getRandom().nextInt(2);
         Fx.burst(player.serverLevel(), ParticleTypes.ELECTRIC_SPARK, player.position().add(0.0, 0.15, 0.0), count, 0.3, 0.04);
      }
   }

   static float stridePhase(ServerPlayer player) {
      return stride(player).position;
   }

   static float strideAmount(ServerPlayer player) {
      return Mth.clamp(stride(player).speed, 0.0F, 1.0F);
   }

   static void forget(ServerPlayer player) {
      STRIDES.remove(player.getUUID());
   }

   private static EchoCadence.Stride stride(ServerPlayer player) {
      return STRIDES.computeIfAbsent(player.getUUID(), k -> new EchoCadence.Stride());
   }

   private static final class Stride {
      float speed;
      float position;

      void update(float movedBlocks) {
         float target = Math.min(movedBlocks * 4.0F, 1.0F);
         this.speed = this.speed + (target - this.speed) * 0.4F;
         this.position = this.position + this.speed;
      }
   }
}
