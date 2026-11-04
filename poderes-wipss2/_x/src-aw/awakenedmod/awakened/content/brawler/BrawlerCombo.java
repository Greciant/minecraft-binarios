package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.BrawlerComboPayload;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BrawlerCombo {
   static final String K_BEST = "brawler_best";
   public static final int PAUSE_HOLD = 1;
   public static final int PAUSE_GUARD = 2;
   public static final int HITSTOP_NONE = -1;
   private static final Map<UUID, BrawlerCombo.ComboState> STATES = new ConcurrentHashMap<>();
   private static final String FEAT_UNCHAINED = "brawler_unchained";

   private BrawlerCombo() {
   }

   private static BrawlerCombo.ComboState state(ServerPlayer player) {
      return STATES.computeIfAbsent(player.getUUID(), k -> new BrawlerCombo.ComboState());
   }

   public static boolean hit(ServerPlayer player, LivingEntity target) {
      return hits(player, target, List.of(), 0, 0) > 0;
   }

   public static boolean abilityHit(ServerPlayer player, LivingEntity target) {
      return hits(player, target, List.of(), 0, 1) > 0;
   }

   public static boolean abilityHit(ServerPlayer player, LivingEntity target, int stacks) {
      return abilityHit(player, target, List.of(), stacks, 0);
   }

   public static boolean abilityHit(ServerPlayer player, LivingEntity primary, Collection<? extends LivingEntity> extras, int stacks, int heavyHitStop) {
      return hits(player, primary, extras, heavyHitStop, 1, stacks - 1) > 0;
   }

   public static int hits(ServerPlayer player, LivingEntity primary, Collection<? extends LivingEntity> extras, int heavyHitStop, int flavor) {
      return hits(player, primary, extras, heavyHitStop, flavor, 0);
   }

   public static int hits(ServerPlayer player, LivingEntity primary, Collection<? extends LivingEntity> extras, int heavyHitStop, int flavor, int bonusStacks) {
      BrawlerCombo.ComboState s = state(player);
      long now = player.serverLevel().getGameTime();
      int stacks = 0;
      boolean primaryCounted = BrawlerTargets.countsForCombo(player, primary) && (flavor != 0 || fistCadenceOpen(s, primary, now));
      if (primaryCounted) {
         stacks++;
         s.hadMob |= !(primary instanceof Player);
      }

      for (LivingEntity extra : extras) {
         if (stacks >= 4) {
            break;
         }

         if (extra != primary && BrawlerTargets.countsForCombo(player, extra)) {
            stacks++;
            s.hadMob |= !(extra instanceof Player);
         }
      }

      if (stacks == 0) {
         return 0;
      } else {
         stacks = Math.min(4, stacks + Math.max(0, bonusStacks));
         int before = s.count;
         s.flavor = flavor;
         if (flavor != 0) {
            s.lastAbilityHit = now;
         }

         addStacks(player, stacks, 1);
         if (primaryCounted && s.count >= 2 && heavyHitStop != -1) {
            int stop = heavyHitStop > 0 ? heavyHitStop : (s.tier >= 4 ? 3 : 2);
            BrawlerHitStop.apply(primary, stop);
         }

         if (primaryCounted) {
            BrawlerFx.comboHit(player.serverLevel(), player, primary, s.count, s.tier);
         }

         return s.count - before;
      }
   }

   private static boolean fistCadenceOpen(BrawlerCombo.ComboState s, LivingEntity target, long now) {
      s.fistTicks.values().removeIf(t -> now - t >= 6L);
      Long last = s.fistTicks.get(target.getId());
      if (last != null && now - last < 6L) {
         return false;
      } else {
         s.fistTicks.put(target.getId(), now);
         return true;
      }
   }

   public static void add(ServerPlayer player, int stacks) {
      state(player).flavor = 1;
      addStacks(player, stacks, 1);
   }

   private static void addStacks(ServerPlayer player, int stacks, int event) {
      if (stacks > 0) {
         BrawlerCombo.ComboState s = state(player);
         long now = player.serverLevel().getGameTime();
         int oldTier = s.tier;
         s.count += stacks;
         s.windowEnd = now + 60L;
         s.tier = BrawlerTuning.tier(s.count);
         PowerData data = PowerService.data(player);
         if (s.count > best(player, data)) {
            s.best = s.count;
            data.setVar("brawler_best", (float)s.best);
         }

         boolean tierUp = s.tier > oldTier && s.tier >= 2;
         if (tierUp) {
            float max = StaminaSystem.max(player);
            data.setStamina(Math.min(max, data.stamina() + max * 0.05F));
            PowerService.sync(player);
            BrawlerFx.tierUp(player.serverLevel(), player, s.tier);
            if (s.tier >= 5 && s.hadMob) {
               QuestSystem.feat(player, "brawler_unchained");
            }
         }

         send(player, s, tierUp ? 2 : event);
         s.flavor = 0;
      }
   }

   public static void markStrike(ServerPlayer player, LivingEntity victim) {
      BrawlerCombo.ComboState s = state(player);
      s.lastStrikeVictim = victim.getId();
      s.lastStrikeTick = player.serverLevel().getGameTime();
   }

   public static void onKill(ServerPlayer player, LivingEntity victim) {
      BrawlerCombo.ComboState s = state(player);
      if (s.count > 0 && Foes.countsAsKill(player, victim)) {
         if (s.lastStrikeVictim == victim.getId() && s.lastStrikeTick == player.serverLevel().getGameTime()) {
            addStacks(player, 1, 5);
         }
      }
   }

   public static void pause(ServerPlayer player, int reason) {
      BrawlerCombo.ComboState s = state(player);
      s.pauseMask |= reason;
      syncPause(player, s);
   }

   public static void unpause(ServerPlayer player, int reason) {
      BrawlerCombo.ComboState s = state(player);
      s.pauseMask &= ~reason;
      syncPause(player, s);
   }

   private static boolean paused(ServerPlayer player, BrawlerCombo.ComboState s) {
      return s.pauseMask != 0;
   }

   private static void syncPause(ServerPlayer player, BrawlerCombo.ComboState s) {
      boolean p = paused(player, s);
      if (p != s.pausedLast) {
         s.pausedLast = p;
         if (s.count > 0) {
            send(player, s, 0);
         }
      }
   }

   public static void tick(ServerPlayer player) {
      BrawlerCombo.ComboState s = STATES.get(player.getUUID());
      if (s != null && s.count > 0) {
         long now = player.serverLevel().getGameTime();
         if (s.tier >= 5) {
            BrawlerFx.unchainedFists(player.serverLevel(), player);
         }

         if (paused(player, s)) {
            s.windowEnd++;
            syncPause(player, s);
         } else {
            syncPause(player, s);
            if (now >= s.windowEnd) {
               drop(player, s);
            }
         }
      } else {
         Marks.clearTransitions(player);
      }
   }

   public static void reset(ServerPlayer player) {
      BrawlerCombo.ComboState s = STATES.get(player.getUUID());
      if (s != null && s.count > 0) {
         drop(player, s);
      }
   }

   private static void drop(ServerPlayer player, BrawlerCombo.ComboState s) {
      int ended = s.count;
      payComboEndMastery(player, s);
      s.count = 0;
      s.tier = 0;
      s.pauseMask = 0;
      s.pausedLast = false;
      s.paidSteps = 0;
      s.paidTransitions = 0;
      s.hadMob = false;
      s.fistTicks.clear();
      Marks.clearTransitions(player);
      PacketDistributor.sendToPlayer(
         player, new BrawlerComboPayload(ended, 0, best(player, PowerService.data(player)), 3, false, 0), new CustomPacketPayload[0]
      );
   }

   private static void payComboEndMastery(ServerPlayer player, BrawlerCombo.ComboState s) {
      if (s.hadMob) {
         int steps = Math.max(0, s.tier - 1);
         int owedSteps = steps - s.paidSteps;
         int converted = Marks.transitionsConverted(player);
         int owedTransitions = converted - s.paidTransitions;
         double points = 4.0 * (double)Math.max(0, owedSteps) + 6.0 * (double)Math.max(0, owedTransitions);
         if (points > 0.0) {
            MasterySystem.grant(player, points);
         }

         s.paidSteps = Math.max(s.paidSteps, steps);
         s.paidTransitions = Math.max(s.paidTransitions, converted);
      }
   }

   public static int count(ServerPlayer player) {
      BrawlerCombo.ComboState s = STATES.get(player.getUUID());
      return s == null ? 0 : s.count;
   }

   public static int tier(ServerPlayer player) {
      BrawlerCombo.ComboState s = STATES.get(player.getUUID());
      return s == null ? 0 : s.tier;
   }

   public static long lastAbilityHitTick(ServerPlayer player) {
      BrawlerCombo.ComboState s = STATES.get(player.getUUID());
      return s == null ? Long.MIN_VALUE : s.lastAbilityHit;
   }

   public static float bonusFraction(ServerPlayer player) {
      return BrawlerTuning.damageBonus(count(player)) + (float)Marks.transitions(player) * 0.06F;
   }

   public static int best(ServerPlayer player, PowerData data) {
      BrawlerCombo.ComboState s = state(player);
      if (s.best < 0) {
         s.best = (int)data.var("brawler_best", 0.0F);
      }

      return s.best;
   }

   private static void send(ServerPlayer player, BrawlerCombo.ComboState s, int event) {
      long now = player.serverLevel().getGameTime();
      int remaining = (int)Math.max(0L, s.windowEnd - now);
      s.pausedLast = paused(player, s);
      PacketDistributor.sendToPlayer(
         player,
         new BrawlerComboPayload(s.count, remaining, best(player, PowerService.data(player)), event, s.pausedLast, s.flavor),
         new CustomPacketPayload[0]
      );
   }

   public static void announceFree(ServerPlayer player) {
      send(player, state(player), 6);
   }

   public static void resync(ServerPlayer player) {
      send(player, state(player), 0);
   }

   public static void clearPlayer(ServerPlayer player) {
      STATES.remove(player.getUUID());
   }

   public static void clearAll() {
      STATES.clear();
   }

   static final class ComboState {
      int count;
      int tier;
      long windowEnd;
      int pauseMask;
      boolean pausedLast;
      int best = -1;
      int flavor = 0;
      long lastAbilityHit = Long.MIN_VALUE;
      int paidSteps;
      int paidTransitions;
      boolean hadMob;
      int lastStrikeVictim = -1;
      long lastStrikeTick = Long.MIN_VALUE;
      final Map<Integer, Long> fistTicks = new HashMap<>();
   }
}
