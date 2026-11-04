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
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class QuestBoard {
   public static final int SLOTS = 4;
   public static final int MAX_REROLLS = 3;
   public static final int SCHEMA = 1;

   private QuestBoard() {
   }

   public static boolean ensure(ServerPlayer player) {
      PowerData pd = PowerService.data(player);
      QuestData qd = AwakenedAttachments.quests(player);
      if (!pd.hasPower()) {
         boolean had = !qd.pinned().isEmpty() || !boardIds(qd).isEmpty();
         qd.clearSlate();
         qd.setPinned("");
         return had;
      } else {
         boolean changed = false;
         if (qd.schema() < 1) {
            migrate(qd);
            changed = true;
         }

         String powerId = pd.powerId();
         int level = pd.masteryLevel();

         for (int s = 0; s < 4; s++) {
            String id = qd.slot(s);
            if (!id.isEmpty()) {
               Quest q = QuestRegistry.byId(id);
               if (q == null || q.goal() || !q.availableTo(powerId) || !q.unlockedFor(level) || !q.allowedBySettings(player)) {
                  qd.setSlot(s, "");
                  changed = true;
               }
            }
         }

         Set<String> taken = boardIds(qd);

         for (int sx = 0; sx < 4; sx++) {
            if (qd.slot(sx).isEmpty()) {
               Quest drawn = QuestDraw.draw(player, null, taken);
               if (drawn != null) {
                  qd.setSlot(sx, drawn.id());
                  qd.reopen(drawn.id(), drawn.required());
                  taken.add(drawn.id());
                  changed = true;
               }
            }
         }

         String pin = qd.pinned();
         if (!pin.isEmpty() && !pinnable(qd, powerId, pin)) {
            qd.setPinned("");
            changed = true;
         }

         return changed;
      }
   }

   public static boolean hasOpenThreshold(ServerPlayer player) {
      QuestData qd = AwakenedAttachments.quests(player);

      for (Quest q : board(player)) {
         if (q.trigger().threshold() && !qd.isComplete(q.id())) {
            return true;
         }
      }

      return false;
   }

   private static void migrate(QuestData qd) {
      qd.clearMatching(id -> QuestRegistry.byId(id) == null);
      qd.clearClaimedMatching(id -> QuestRegistry.byId(id) == null);
      qd.pruneUnknownXpClaimed(id -> QuestRegistry.byId(id) == null);

      for (Quest q : QuestRegistry.all()) {
         qd.clampProgress(q.id(), q.required());
      }

      qd.clearSlate();
      qd.setPinned("");
      qd.refillRerolls();
      qd.setSchema(1);
   }

   public static List<Quest> board(ServerPlayer player) {
      PowerData pd = PowerService.data(player);
      List<Quest> out = new ArrayList<>();
      if (!pd.hasPower()) {
         return out;
      } else {
         out.addAll(QuestRegistry.goalsFor(pd.powerId()));
         QuestData qd = AwakenedAttachments.quests(player);

         for (int s = 0; s < 4; s++) {
            String id = qd.slot(s);
            if (!id.isEmpty()) {
               Quest q = QuestRegistry.byId(id);
               if (q != null) {
                  out.add(q);
               }
            }
         }

         return out;
      }
   }

   public static void onCompleted(ServerPlayer player, Quest quest) {
      QuestData qd = AwakenedAttachments.quests(player);
      qd.refillRerolls();
      boolean wasPinned = quest.id().equals(qd.pinned());
      int slot = qd.slotOf(quest.id());
      if (slot < 0) {
         if (wasPinned) {
            qd.setPinned("");
         }
      } else {
         Quest replacement = QuestDraw.draw(player, null, boardIds(qd));
         if (replacement == null) {
            qd.setSlot(slot, "");
            if (wasPinned) {
               qd.setPinned("");
            }
         } else {
            qd.setSlot(slot, replacement.id());
            qd.reopen(replacement.id(), replacement.required());
            if (wasPinned) {
               qd.setPinned(replacement.id());
            }
         }
      }
   }

   public static boolean pin(ServerPlayer player, @Nullable String questId) {
      boolean changed = ensure(player);
      PowerData pd = PowerService.data(player);
      QuestData qd = AwakenedAttachments.quests(player);
      if (!pd.hasPower()) {
         refuse(player, "awakened.quest.no_power");
         return changed;
      } else {
         String id = questId == null ? "" : questId;
         String was = qd.pinned();
         if (!id.isEmpty() && !id.equals(was)) {
            if (!pinnable(qd, pd.powerId(), id)) {
               refuse(player, "awakened.quest.pin_unavailable");
               return changed;
            } else {
               qd.setPinned(id);
               return true;
            }
         } else {
            qd.setPinned("");
            return changed || !was.isEmpty();
         }
      }
   }

   public static boolean reroll(ServerPlayer player, int slot) {
      boolean changed = ensure(player);
      PowerData pd = PowerService.data(player);
      QuestData qd = AwakenedAttachments.quests(player);
      if (!pd.hasPower()) {
         refuse(player, "awakened.quest.no_power");
         return changed;
      } else if (slot >= 0 && slot < 4) {
         String current = qd.slot(slot);
         Quest existing = current.isEmpty() ? null : QuestRegistry.byId(current);
         if (existing == null) {
            refuse(player, "awakened.quest.reroll_empty_slot");
            return changed;
         } else if (qd.rerolls() <= 0) {
            refuse(player, "awakened.quest.reroll_none_left");
            return changed;
         } else {
            Quest drawn = QuestDraw.draw(player, existing.tier(), boardIds(qd));
            if (drawn == null) {
               refuse(player, "awakened.quest.reroll_nothing");
               return changed;
            } else {
               qd.spendReroll();
               qd.setSlot(slot, drawn.id());
               qd.reopen(drawn.id(), drawn.required());
               if (current.equals(qd.pinned())) {
                  qd.setPinned(drawn.id());
               }

               return true;
            }
         }
      } else {
         refuse(player, "awakened.quest.reroll_bad_slot");
         return changed;
      }
   }

   public static boolean handle(ServerPlayer player, QuestBoard.Action action, int slot, @Nullable String questId) {
      return switch (action) {
         case PIN -> pin(player, questId);
         case REROLL -> reroll(player, slot);
      };
   }

   public static void clearForPowerChange(QuestData qd) {
      qd.clearSlate();
      qd.setPinned("");
      qd.refillRerolls();
   }

   private static Set<String> boardIds(QuestData qd) {
      Set<String> ids = new HashSet<>();

      for (int s = 0; s < 4; s++) {
         String id = qd.slot(s);
         if (!id.isEmpty()) {
            ids.add(id);
         }
      }

      if (!qd.pinned().isEmpty()) {
         ids.add(qd.pinned());
      }

      return ids;
   }

   private static boolean pinnable(QuestData qd, String powerId, String id) {
      Quest q = QuestRegistry.byId(id);
      return q != null && q.availableTo(powerId) ? q.goal() || qd.slotOf(id) >= 0 : false;
   }

   private static void refuse(ServerPlayer player, String key) {
      player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
   }

   public static enum Action {
      PIN,
      REROLL;

      public static QuestBoard.Action byIndex(int i) {
         QuestBoard.Action[] all = values();
         return all[Math.max(0, Math.min(all.length - 1, i))];
      }
   }
}
