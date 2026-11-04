package com.awakenedmod.awakened.core.quest;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.data.QuestData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;

public final class QuestDraw {
   private QuestDraw() {
   }

   @Nullable
   public static Quest draw(ServerPlayer player, @Nullable QuestTier forced, Set<String> exclude) {
      PowerData pd = PowerService.data(player);
      if (!pd.hasPower()) {
         return null;
      } else {
         QuestData qd = AwakenedAttachments.quests(player);
         Set<QuestDraw.Objective> taken = QuestDraw.Objective.of(exclude);

         for (Quest goal : QuestRegistry.goalsFor(pd.powerId())) {
            if (QuestSystem.active(qd, goal)) {
               taken.add(QuestDraw.Objective.of(goal));
            }
         }

         QuestDraw.Bands distinct = new QuestDraw.Bands();
         QuestDraw.Bands anything = new QuestDraw.Bands();

         for (Quest q : QuestRegistry.trialsFor(pd.powerId())) {
            if (!exclude.contains(q.id()) && q.unlockedFor(pd.masteryLevel()) && q.allowedBySettings(player) && !satisfied(player, q)) {
               anything.add(q);
               if (!taken.contains(QuestDraw.Objective.of(q))) {
                  distinct.add(q);
               }
            }
         }

         Quest drawn = from(player, qd, pd.masteryLevel(), forced, distinct);
         return drawn != null ? drawn : from(player, qd, pd.masteryLevel(), forced, anything);
      }
   }

   @Nullable
   private static Quest from(ServerPlayer player, QuestData qd, int masteryLevel, @Nullable QuestTier forced, QuestDraw.Bands bands) {
      QuestTier tier = forced != null ? walkDown(forced, bands.available) : QuestTier.roll(player.getRandom(), masteryLevel, bands.available);
      return tier == null ? null : pick(player, qd, bands.byTier.get(tier.ordinal()));
   }

   private static boolean satisfied(ServerPlayer player, Quest q) {
      return q.trigger().threshold() && q.trigger().liveValue(player, q.target()) >= q.required();
   }

   @Nullable
   private static QuestTier walkDown(QuestTier forced, boolean[] available) {
      for (int t = forced.ordinal(); t >= 0; t--) {
         if (available[t]) {
            return QuestTier.byIndex(t);
         }
      }

      return null;
   }

   @Nullable
   private static Quest pick(ServerPlayer player, QuestData qd, List<Quest> candidates) {
      if (candidates.isEmpty()) {
         return null;
      } else {
         int fewest = Integer.MAX_VALUE;

         for (Quest q : candidates) {
            fewest = Math.min(fewest, qd.timesCompleted(q.id()));
         }

         List<Quest> freshest = new ArrayList<>();

         for (Quest q : candidates) {
            if (qd.timesCompleted(q.id()) == fewest) {
               freshest.add(q);
            }
         }

         return freshest.get(player.getRandom().nextInt(freshest.size()));
      }
   }

   private static final class Bands {
      private final List<List<Quest>> byTier = new ArrayList<>(QuestTier.values().length);
      private final boolean[] available = new boolean[QuestTier.values().length];

      private Bands() {
         for (int t = 0; t < this.available.length; t++) {
            this.byTier.add(new ArrayList<>());
         }
      }

      private void add(Quest q) {
         this.byTier.get(q.tier().ordinal()).add(q);
         this.available[q.tier().ordinal()] = true;
      }
   }

   private static record Objective(QuestTrigger trigger, String target) {
      private static QuestDraw.Objective of(Quest q) {
         return new QuestDraw.Objective(q.trigger(), q.target());
      }

      private static Set<QuestDraw.Objective> of(Set<String> ids) {
         Set<QuestDraw.Objective> out = new HashSet<>();

         for (String id : ids) {
            Quest q = QuestRegistry.byId(id);
            if (q != null) {
               out.add(of(q));
            }
         }

         return out;
      }
   }
}
