package com.awakenedmod.awakened.core.progression;

import com.awakenedmod.awakened.Awakened;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.MasteryData;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.rules.ServerRules;
import com.awakenedmod.awakened.core.skill.SkillSystem;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public final class MasterySystem {
   private static final Map<MasteryReason, Integer> COOLDOWN_TICKS = new EnumMap<>(
      Map.of(MasteryReason.HIT, 4, MasteryReason.KILL, 0, MasteryReason.SURVIVAL, 100, MasteryReason.USE, 20)
   );
   private static final double DECAY_WINDOW_TICKS = 200.0;
   private static final Map<UUID, EnumMap<MasteryReason, Long>> LAST_AWARD = new ConcurrentHashMap<>();
   private static final Map<UUID, EnumMap<MasteryReason, Double>> RECENT = new ConcurrentHashMap<>();

   private MasterySystem() {
   }

   public static void award(ServerPlayer player, MasteryReason reason) {
      awardInternal(player, reason, true, true);
   }

   public static void award(ServerPlayer player, MasteryReason reason, @Nullable Entity victim) {
      if (Foes.counts(player, victim)) {
         award(player, reason);
      }
   }

   public static void award(ServerPlayer player, MasteryReason reason, Collection<? extends Entity> victims) {
      for (Entity victim : victims) {
         if (Foes.counts(player, victim)) {
            award(player, reason);
            return;
         }
      }
   }

   public static void awardUse(ServerPlayer player) {
      awardInternal(player, MasteryReason.USE, false, false);
   }

   private static void awardInternal(ServerPlayer player, MasteryReason reason, boolean grantCharacterXp, boolean feedQuests) {
      PowerData data = PowerService.data(player);
      if (data.hasPower()) {
         long now = player.serverLevel().getGameTime();
         UUID id = player.getUUID();
         EnumMap<MasteryReason, Long> last = LAST_AWARD.computeIfAbsent(id, k -> new EnumMap<>(MasteryReason.class));
         long lastTime = last.getOrDefault(reason, Long.valueOf(Long.MIN_VALUE));
         if (lastTime == Long.MIN_VALUE || now - lastTime >= (long)COOLDOWN_TICKS.getOrDefault(reason, 0).intValue()) {
            EnumMap<MasteryReason, Double> recentMap = RECENT.computeIfAbsent(id, k -> new EnumMap<>(MasteryReason.class));
            double recent = recentMap.getOrDefault(reason, Double.valueOf(0.0));
            if (lastTime != Long.MIN_VALUE) {
               recent = Math.max(0.0, recent - (double)(now - lastTime) / 200.0);
            }

            double factor = 1.0 / (1.0 + recent);
            last.put(reason, now);
            recentMap.put(reason, recent + 1.0);
            double points = reason.basePoints() * factor;
            applyProgress(player, data, points);
            if (grantCharacterXp) {
               SkillSystem.award(player, points * 1.0);
            }

            if (feedQuests) {
               QuestSystem.onOutcome(player, reason);
            }
         }
      }
   }

   public static void grant(ServerPlayer player, double points) {
      PowerData data = PowerService.data(player);
      if (data.hasPower() && !(points <= 0.0)) {
         applyProgress(player, data, points);
      }
   }

   private static void applyProgress(ServerPlayer player, PowerData data, double points) {
      MasteryData mastery = data.currentMastery();
      int beforeLevel = mastery.level();
      int beforePeak = mastery.peak();
      int cap = ServerRules.maxMastery();
      if (beforeLevel < cap) {
         mastery.addProgress(points * ServerRules.masteryGain());

         while (mastery.progress() >= MasteryFormula.progressForNext(mastery.level())) {
            double carry = mastery.progress() - MasteryFormula.progressForNext(mastery.level());
            mastery.levelUp(carry);
            if (mastery.level() >= cap) {
               mastery.set(mastery.level(), 0.0);
               break;
            }
         }

         if (mastery.level() > beforeLevel) {
            onLevelUp(player, data, beforeLevel, beforePeak, mastery.level());
            QuestSystem.onMasteryLevel(player);
         }

         PowerService.sync(player);
      }
   }

   private static void onLevelUp(ServerPlayer player, PowerData data, int fromLevel, int peak, int toLevel) {
      AwakenedAttachments.progress(player).recordMastery(toLevel);
      int newLevels = toLevel - Math.max(fromLevel, peak);
      if (newLevels > 0) {
         SkillSystem.award(player, 25.0 * (double)newLevels);
      }

      player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
      player.displayClientMessage(Component.translatable("awakened.mastery.levelup", new Object[]{toLevel}), true);
      Awakened.LOGGER.debug("Player {} reached mastery level {}", player.getName().getString(), toLevel);
      Power power = PowerRegistry.get(data.powerId());
      if (power != null) {
         for (Ability ability : power.abilities()) {
            if (ability.unlockLevel() > fromLevel && ability.unlockLevel() <= toLevel) {
               PowerService.announceUnlock(player, ability.displayName());
            }
         }
      }
   }

   public static void forget(UUID playerId) {
      LAST_AWARD.remove(playerId);
      RECENT.remove(playerId);
   }
}
