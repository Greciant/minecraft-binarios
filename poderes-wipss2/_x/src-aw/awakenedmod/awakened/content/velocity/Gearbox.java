package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import com.awakenedmod.awakened.core.net.VelocityScreenFxPayload;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.AbilityFeedback;
import com.awakenedmod.awakened.core.power.PassiveToggle;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public final class Gearbox {
   public static final int MAX_GEAR = 5;
   public static final float FORCE_STAMINA_PER_SECOND = 5.0F;
   public static final float[] TOP_GEAR_COST_PER_SECOND = new float[]{0.0F, 0.0F, 0.0F, 0.0F, 2.0F, 4.0F};
   public static final int FREE_GEAR_CEILING = 3;
   public static final int BOOM_COOLDOWN_TICKS = 600;
   public static final PassiveToggle AUTO = new PassiveToggle(
      "velocity_auto_gearbox", "awakened.screen.setting_velocity_auto_gearbox", "awakened.screen.setting_velocity_auto_gearbox_desc", true
   );
   private static final Map<UUID, Map<String, Integer>> DEMANDS = new ConcurrentHashMap<>();
   private static final Set<UUID> BY_HAND = ConcurrentHashMap.newKeySet();
   private static final Map<UUID, Integer> LAST_GEAR = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> LAST_BOOM = new ConcurrentHashMap<>();
   private static final Set<UUID> RAN = ConcurrentHashMap.newKeySet();
   private static final Map<UUID, Long> SPAWN_PIN = new ConcurrentHashMap<>();
   private static final int SPAWN_PIN_TICKS = 40;
   private static final int SETTLE_STALL_TICKS = 10;
   private static final float TICKS_PER_SECOND = 20.0F;
   public static final String REDLINE_FEAT = "velocity_redline_10";
   private static final int REDLINE_SECONDS = 10;
   private static final int REDLINE_TICKS = 200;
   private static final Map<UUID, Integer> REDLINE = new ConcurrentHashMap<>();

   private Gearbox() {
   }

   public static void shiftDown(ServerPlayer player) {
      shift(player, -1);
   }

   public static void shiftUp(ServerPlayer player) {
      shift(player, 1);
   }

   public static void shift(ServerPlayer player, int delta) {
      if (player != null && delta != 0 && VelocityPower.owns(player)) {
         PowerData data = PowerService.data(player);
         int chosen = Momentum.chosenGear(data);
         int cap = Momentum.unlockedGear(data);
         int next;
         if (delta > 0) {
            int earned = Momentum.earnedGear(data);
            next = Math.min(cap, chosen < earned ? Math.min(chosen + 1, earned) : chosen + 1);
         } else {
            next = Math.max(0, chosen - 1);
         }

         if (next != chosen) {
            BY_HAND.add(player.getUUID());
            set(player, next);
         } else if (delta > 0 && cap < 5) {
            AbilityFeedback.fail(player, "awakened.velocity.gear_locked", cap + 1, Momentum.unlockLevel(cap + 1));
         } else {
            AbilityFeedback.fail(player, delta > 0 ? "awakened.velocity.gear_top" : "awakened.velocity.gear_bottom");
         }
      }
   }

   public static void set(ServerPlayer player, int gear) {
      set(player, gear, true);
   }

   static void set(ServerPlayer player, int gear, boolean announce) {
      if (player != null && VelocityPower.owns(player)) {
         PowerData data = PowerService.data(player);
         int next = Math.max(0, Math.min(Momentum.unlockedGear(data), gear));
         if (next != Momentum.chosenGear(data)) {
            Momentum.setChosenGear(data, next);
            if (OverdriveAbility.isOn(data)) {
               VelocityFx.gearBurst(player.serverLevel(), player, next);
            }

            if (announce) {
               player.displayClientMessage(Component.translatable("awakened.velocity.gear", new Object[]{next}), true);
            }

            VelocityVisualState.sync(player);
            PowerService.sync(player);
         }
      }
   }

   public static void autoTick(ServerPlayer player, PowerData data, boolean gaining) {
      if (AUTO.enabled(data) && OverdriveAbility.isOn(data)) {
         UUID id = player.getUUID();
         boolean running = gaining && Momentum.stillTicks(data) < 10;
         int earned = Momentum.earnedGear(data);
         int chosen = Momentum.chosenGear(data);
         if (running) {
            RAN.add(id);
            if (earned > chosen && !pinned(player)) {
               set(player, earned, false);
            }
         } else {
            if (RAN.remove(id)) {
               BY_HAND.remove(id);
            }

            if (earned < chosen && !pinned(player)) {
               idleDown(player, data, earned);
            }
         }
      }
   }

   private static boolean pinned(ServerPlayer player) {
      UUID id = player.getUUID();
      if (BY_HAND.contains(id)) {
         return true;
      } else {
         Long until = SPAWN_PIN.get(id);
         if (until == null) {
            return false;
         } else if (player.serverLevel().getGameTime() >= until) {
            SPAWN_PIN.remove(id);
            return false;
         } else {
            return true;
         }
      }
   }

   public static void pinAfterSpawn(ServerPlayer player) {
      if (player != null) {
         SPAWN_PIN.put(player.getUUID(), player.serverLevel().getGameTime() + 40L);
      }
   }

   private static void idleDown(ServerPlayer player, PowerData data, int gear) {
      Momentum.setChosenGear(data, gear);
      VelocityVisualState.sync(player);
      PowerService.sync(player);
   }

   public static void demand(ServerPlayer player, String abilityId, int gear) {
      if (player != null && abilityId != null && VelocityPower.owns(player)) {
         int want = Math.max(0, Math.min(Momentum.unlockedGear(PowerService.data(player)), gear));
         if (want <= 0) {
            release(player, abilityId);
         } else {
            Integer before = DEMANDS.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>()).put(abilityId, want);
            if (before == null || before != want) {
               pushGear(player);
            }
         }
      }
   }

   public static void release(ServerPlayer player, String abilityId) {
      if (player != null && abilityId != null) {
         Map<String, Integer> mine = DEMANDS.get(player.getUUID());
         if (mine != null && mine.remove(abilityId) != null) {
            if (mine.isEmpty()) {
               DEMANDS.remove(player.getUUID());
            }

            pushGear(player);
         }
      }
   }

   public static void clearDemands(ServerPlayer player) {
      if (player != null) {
         DEMANDS.remove(player.getUUID());
         REDLINE.remove(player.getUUID());
         BY_HAND.remove(player.getUUID());
         RAN.remove(player.getUUID());
         SPAWN_PIN.remove(player.getUUID());
         LAST_GEAR.remove(player.getUUID());
         LAST_BOOM.remove(player.getUUID());
      }
   }

   public static int demandedGear(ServerPlayer player) {
      Map<String, Integer> mine = player == null ? null : DEMANDS.get(player.getUUID());
      if (mine == null) {
         return 0;
      } else {
         int top = 0;

         for (int gear : mine.values()) {
            top = Math.max(top, gear);
         }

         return top;
      }
   }

   public static int effectiveGear(ServerPlayer player, PowerData data) {
      return Math.min(Momentum.unlockedGear(data), Math.max(Momentum.chosenGear(data), demandedGear(player)));
   }

   private static void pushGear(ServerPlayer player) {
      VelocityVisualState.sync(player);
      PowerService.sync(player);
   }

   public static int forcedGears(ServerPlayer player, PowerData data) {
      int driven = OverdriveAbility.isOn(data) ? effectiveGear(player, data) : demandedGear(player);
      return Math.max(0, driven - Momentum.earnedGear(data));
   }

   public static boolean forcing(ServerPlayer player, PowerData data) {
      return forcedGears(player, data) > 0;
   }

   public static boolean billing(ServerPlayer player, PowerData data) {
      return (forcedGears(player, data) > 0 || topGearDrain(data) > 0.0F) && billable(player);
   }

   public static int extraForcedGears(Player viewer, int requiredGear) {
      if (viewer != null && requiredGear > 0) {
         VelocityVisual visual = VelocityVisualState.of(viewer);
         int earned = visual.earnedGear();
         int already = Math.max(0, visual.gear() - earned);
         int withIt = Math.max(0, Math.max(visual.gear(), requiredGear) - earned);
         return Math.max(0, withIt - already);
      } else {
         return 0;
      }
   }

   static void forcingTick(ServerPlayer player, PowerData data) {
      if (!data.infiniteStamina()) {
         int forced = forcedGears(player, data);
         if (forced > 0 && billable(player)) {
            float remaining = StaminaSystem.drainSustained(
               data, player.serverLevel().getGameTime(), StaminaSystem.effectiveCost((float)forced * 5.0F / 20.0F, OverdriveAbility.class.getSimpleName())
            );
            if (!(remaining > 0.0F)) {
               int earned = Momentum.earnedGear(data);
               if (Momentum.chosenGear(data) > earned) {
                  set(player, earned);
                  player.displayClientMessage(Component.translatable("awakened.velocity.forcing_out", new Object[]{earned}), true);
               } else {
                  dropDemands(player, data);
               }
            }
         }
      }
   }

   private static void dropDemands(ServerPlayer player, PowerData data) {
      Map<String, Integer> mine = DEMANDS.get(player.getUUID());
      if (mine != null && !mine.isEmpty()) {
         Power power = PowerRegistry.get("velocity");
         if (power != null) {
            boolean ended = false;

            for (String demanding : List.copyOf(mine.keySet())) {
               for (Ability ability : power.abilities()) {
                  if (ability instanceof VelocityHoldAbility) {
                     VelocityHoldAbility hold = (VelocityHoldAbility)ability;
                     if (hold.id().equals(demanding)) {
                        hold.stop(player, VelocityHoldAbility.End.RELEASED);
                        ended = true;
                     }
                  }
               }
            }

            if (ended) {
               player.displayClientMessage(Component.translatable("awakened.velocity.forcing_out", new Object[]{Momentum.chosenGear(data)}), true);
            }
         }
      }
   }

   public static float topGearCost(int gear) {
      return TOP_GEAR_COST_PER_SECOND[Math.max(0, Math.min(5, gear))];
   }

   static float topGearDrain(PowerData data) {
      return OverdriveAbility.isOn(data) ? topGearCost(Momentum.chosenGear(data)) : 0.0F;
   }

   static void topGearTick(ServerPlayer player, PowerData data) {
      float perSecond = topGearDrain(data);
      if (!(perSecond <= 0.0F) && billable(player)) {
         if (!VelocityStamina.drainPerSecond(player, perSecond, OverdriveAbility.class.getSimpleName())) {
            if (Momentum.chosenGear(data) > 3) {
               BY_HAND.add(player.getUUID());
               set(player, 3);
               player.displayClientMessage(Component.translatable("awakened.velocity.top_gear_out"), true);
            }
         }
      }
   }

   static boolean billable(ServerPlayer player) {
      return player.isSprinting() && Momentum.moving(player);
   }

   static void boomTick(ServerPlayer player, PowerData data) {
      UUID id = player.getUUID();
      int gear = OverdriveAbility.isOn(data) ? effectiveGear(player, data) : 0;
      Integer previous = LAST_GEAR.put(id, gear);
      if (previous != null && gear >= 5 && previous < 5) {
         if (forcedGears(player, data) <= 0 && billable(player)) {
            long now = player.serverLevel().getGameTime();
            Long last = LAST_BOOM.get(id);
            if (last == null || now - last >= 600L) {
               LAST_BOOM.put(id, now);
               VelocityFx.sonicBoom(player.serverLevel(), player);
               PacketDistributor.sendToPlayer(player, VelocityScreenFxPayload.boom(), new CustomPacketPayload[0]);
            }
         }
      }
   }

   static void redlineTick(ServerPlayer player, PowerData data) {
      if (OverdriveAbility.isOn(data) && player.isSprinting() && Momentum.chosenGear(data) >= 5 && forcedGears(player, data) <= 0) {
         if (REDLINE.merge(player.getUUID(), 1, Integer::sum) >= 200) {
            REDLINE.remove(player.getUUID());
            QuestSystem.feat(player, "velocity_redline_10");
         }
      } else {
         REDLINE.remove(player.getUUID());
      }
   }
}
