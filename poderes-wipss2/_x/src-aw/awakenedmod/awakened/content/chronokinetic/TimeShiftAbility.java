package com.awakenedmod.awakened.content.chronokinetic;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class TimeShiftAbility extends Ability {
   public static final String ANIMATION = "time_shift";
   private static final float UPFRONT_COST = 2.0F;
   private static final float DRAIN_PER_TICK = 0.12F;
   private static final long START_RATE = 16L;
   private static final long ACCEL = 4L;
   private static final long MAX_RATE = 1000L;
   private static final int SYNC_INTERVAL = 5;
   private static final int REASSERT_TICKS = 16;
   private final Map<UUID, TimeShiftAbility.Shift> shifts = new ConcurrentHashMap<>();

   public TimeShiftAbility() {
      super("time_shift", 2.0F, 0);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.shifts.containsKey(player.getUUID())) {
         return true;
      } else {
         PowerData data = PowerService.data(player);
         float upfront = StaminaSystem.effectiveCost(2.0F, this.balanceKey());
         if (!data.infiniteStamina() && data.stamina() < upfront) {
            return this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
         } else {
            if (!data.infiniteStamina()) {
               data.setStamina(data.stamina() - upfront);
            }

            boolean rewind = player.isShiftKeyDown();
            this.shifts.put(player.getUUID(), new TimeShiftAbility.Shift(rewind ? -1 : 1));
            PowerService.playAnimation(player, "time_shift", true);
            player.displayClientMessage(
               Component.translatable(rewind ? "awakened.chronokinetic.time_rewind" : "awakened.chronokinetic.time_hasten").withStyle(ChatFormatting.GOLD),
               true
            );
            PowerService.sync(player);
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      TimeShiftAbility.Shift shift = this.shifts.get(player.getUUID());
      if (shift != null) {
         ServerLevel level = player.serverLevel();
         long rate = Math.min(1000L, 16L + (long)shift.heldTicks * 4L);
         rate = Math.max(1L, Math.round(MasteryScaling.scaled((double)rate, player)));
         level.setDayTime(level.getDayTime() + rate * (long)shift.dir);
         ChronoSounds.keepWatchLoop(player, rate, shift.dir < 0);
         keepPose(player, shift);
         shift.heldTicks++;
         ClientboundSetTimePacket timePacket = new ClientboundSetTimePacket(level.getGameTime(), level.getDayTime(), true);

         for (ServerPlayer viewer : level.players()) {
            viewer.connection.send(timePacket);
         }

         PowerData data = PowerService.data(player);
         if (!data.infiniteStamina()) {
            float remaining = StaminaSystem.drainSustained(data, level.getGameTime(), StaminaSystem.effectiveCost(0.12F, this.balanceKey()));
            if (remaining <= 0.0F) {
               this.end(player);
               return;
            }

            if (player.tickCount % 5 == 0) {
               PowerService.sync(player);
            }
         }
      }
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public void onRelease(ServerPlayer player) {
      this.end(player);
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.shifts.remove(player.getUUID()) != null) {
         stopLook(player);
      }

      super.clearPlayer(player);
   }

   private void end(ServerPlayer player) {
      if (this.shifts.remove(player.getUUID()) != null) {
         stopLook(player);
         PowerService.sync(player);
      }
   }

   private static void keepPose(ServerPlayer player, TimeShiftAbility.Shift shift) {
      String held = PowerService.heldAnimation(player);
      if (held != null) {
         shift.lostAt = -1;
      } else {
         if (shift.lostAt < 0) {
            shift.lostAt = shift.heldTicks;
         } else if (shift.heldTicks - shift.lostAt >= 16) {
            PowerService.playAnimation(player, "time_shift", true);
            shift.lostAt = -1;
         }
      }
   }

   private static void stopLook(ServerPlayer player) {
      if ("time_shift".equals(PowerService.heldAnimation(player))) {
         PowerService.stopAnimation(player);
      }

      ChronoSounds.stopWatchLoop(player);
   }

   private static final class Shift {
      private final int dir;
      private int heldTicks;
      private int lostAt = -1;

      private Shift(int dir) {
         this.dir = dir;
      }
   }
}
