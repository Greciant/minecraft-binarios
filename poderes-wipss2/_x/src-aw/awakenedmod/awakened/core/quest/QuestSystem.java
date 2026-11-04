package com.awakenedmod.awakened.core.quest;

import com.awakenedmod.awakened.Awakened;
import com.awakenedmod.awakened.core.CatalystConfig;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.data.QuestData;
import com.awakenedmod.awakened.core.net.SyncQuestPayload;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.skill.SkillSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.fml.event.config.ModConfigEvent.Reloading;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class QuestSystem {
   private static final double MIN_REPEAT_FACTOR = 0.25;
   private static final int ACTION_COOLDOWN_TICKS = 5;
   private static final Map<UUID, Long> LAST_ACTION = new ConcurrentHashMap<>();

   private QuestSystem() {
   }

   public static void onOutcome(ServerPlayer player, MasteryReason reason) {
      report(player, QuestTrigger.forReason(reason), 1);
   }

   public static void onBossDefeated(ServerPlayer player) {
      report(player, QuestTrigger.DEFEAT_BOSS, 1);
   }

   public static void report(ServerPlayer player, QuestTrigger trigger, int amount) {
      report(player, trigger, "", amount);
   }

   public static void report(ServerPlayer player, QuestTrigger trigger, String target, int amount) {
      if (amount > 0 && evaluate(player, trigger, target, amount)) {
         push(player);
      }
   }

   public static void feat(ServerPlayer player, String featKey) {
      report(player, QuestTrigger.POWER_FEAT, featKey, 1);
   }

   public static void onMasteryLevel(ServerPlayer player) {
      if (evaluate(player, QuestTrigger.REACH_MASTERY, "", 0)) {
         push(player);
      }
   }

   public static void onItemCrafted(ServerPlayer player, String itemId) {
      if (TrialConfig.enabled() && PowerService.data(player).hasPower()) {
         if (AwakenedAttachments.quests(player).recordCraftedItem(itemId) && evaluate(player, QuestTrigger.CRAFT_ITEMS, "", 0)) {
            push(player);
         }
      }
   }

   public static void poll(ServerPlayer player) {
      if (TrialConfig.enabled() && PowerService.data(player).hasPower()) {
         boolean changed = QuestBoard.ensure(player);
         if (QuestBoard.hasOpenThreshold(player)) {
            changed |= evaluate(player, null, "", 0);
         }

         if (changed) {
            push(player);
         }
      }
   }

   public static void handle(ServerPlayer player, QuestBoard.Action action, int slot, @Nullable String questId) {
      if (TrialConfig.enabled() && accept(player)) {
         if (QuestBoard.handle(player, action, slot, questId)) {
            sync(player);
         }
      }
   }

   private static boolean accept(ServerPlayer player) {
      long now = player.serverLevel().getGameTime();
      Long last = LAST_ACTION.get(player.getUUID());
      if (last != null && now - last < 5L && now >= last) {
         return false;
      } else {
         LAST_ACTION.put(player.getUUID(), now);
         return true;
      }
   }

   public static void onConfigReloaded(Reloading event) {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (event.getConfig().getSpec() == TrialConfig.SPEC && server != null) {
         server.execute(() -> server.getPlayerList().getPlayers().forEach(QuestSystem::sync));
      }
   }

   public static void forget(UUID playerId) {
      LAST_ACTION.remove(playerId);
   }

   private static boolean evaluate(ServerPlayer player, @Nullable QuestTrigger trigger, String target, int amount) {
      PowerData pd = PowerService.data(player);
      if (TrialConfig.enabled() && pd.hasPower()) {
         QuestData qd = AwakenedAttachments.quests(player);
         List<Quest> finished = null;
         boolean changed = false;

         for (Quest q : QuestBoard.board(player)) {
            if (q.matches(trigger, target) && active(qd, q)) {
               int was = qd.progress(q.id());
               int next = q.trigger().threshold() ? Math.min(q.required(), q.trigger().liveValue(player, q.target())) : Math.min(q.required(), was + amount);
               if (next != was || next >= q.required()) {
                  changed = true;
                  qd.setProgress(q.id(), next);
                  if (next >= q.required()) {
                     if (finished == null) {
                        finished = new ArrayList<>();
                     }

                     finished.add(q);
                  }
               }
            }
         }

         if (finished != null) {
            for (Quest qx : finished) {
               if (active(qd, qx) && qd.progress(qx.id()) >= qx.required()) {
                  complete(player, qx);
               }
            }
         }

         return changed;
      } else {
         return false;
      }
   }

   static boolean active(QuestData qd, Quest q) {
      if (qd.isComplete(q.id())) {
         return false;
      } else {
         return !q.goal() ? qd.slotOf(q.id()) >= 0 : q.prereq() == null || qd.isComplete(q.prereq());
      }
   }

   private static void complete(ServerPlayer player, Quest q) {
      QuestData qd = AwakenedAttachments.quests(player);
      if (!qd.isComplete(q.id())) {
         int repeats = qd.timesCompleted(q.id());
         qd.recordCompletion(q.id(), q.required());
         QuestBoard.onCompleted(player, q);
         int mastery = masteryPayout(q.masteryReward(), repeats);
         if (mastery > 0) {
            MasterySystem.grant(player, (double)mastery);
         }

         boolean paidSkillXp = false;
         if (q.xpReward() > 0 && !qd.hasClaimedXp(q.id())) {
            SkillSystem.award(player, (double)q.xpReward());
            qd.markXpClaimed(q.id());
            paidSkillXp = true;
         }

         grantItems(player, qd, q);
         player.giveExperiencePoints(Math.max(2, mastery / 10));
         announce(player, q, mastery, paidSkillXp ? q.xpReward() : 0);
         Awakened.LOGGER.debug("Player {} completed quest '{}' (x{})", new Object[]{player.getName().getString(), q.id(), qd.timesCompleted(q.id())});
      }
   }

   public static int masteryPayout(int base, int repeats) {
      if (base <= 0) {
         return 0;
      } else if (repeats <= 0) {
         return base;
      } else {
         double factor = Math.max(0.25, 1.0 / (double)(1 + repeats));
         return Math.max(1, (int)Math.round((double)base * factor));
      }
   }

   private static void grantItems(ServerPlayer player, QuestData qd, Quest q) {
      if (!q.rewards().isEmpty() && !qd.hasClaimedItems(q.id())) {
         boolean granted = false;

         for (QuestReward reward : q.rewards()) {
            try {
               granted |= reward.grant(player);
            } catch (Exception var7) {
               Awakened.LOGGER.error("Failed to grant quest '{}' reward to {}", new Object[]{q.id(), player.getName().getString(), var7});
            }
         }

         if (granted) {
            qd.markItemsClaimed(q.id());
         }
      }
   }

   private static void announce(ServerPlayer player, Quest q, int masteryReward, int xpReward) {
      player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 45, 15));
      player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("awakened.quest.complete_title").withStyle(ChatFormatting.GOLD)));
      player.connection.send(new ClientboundSetSubtitleTextPacket(q.title().copy().withStyle(ChatFormatting.YELLOW)));
      Fx.sound(player.serverLevel(), player.position(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.9F, 1.0F);
      player.displayClientMessage(
         Component.translatable("awakened.quest.complete_chat", new Object[]{q.title(), masteryReward, xpReward}).withStyle(ChatFormatting.GOLD), false
      );
   }

   public static void resetForPowerChange(ServerPlayer player) {
      QuestData qd = AwakenedAttachments.quests(player);
      if (!CatalystConfig.bankProgress()) {
         qd.clearMatching(id -> QuestRegistry.byId(id) != null);
         qd.clearCraftedItems();
      }

      QuestBoard.clearForPowerChange(qd);
   }

   public static void sync(ServerPlayer player) {
      if (TrialConfig.enabled()) {
         QuestBoard.ensure(player);
         evaluate(player, null, "", 0);
      }

      push(player);
   }

   public static void push(ServerPlayer player) {
      if (!TrialConfig.enabled()) {
         PacketDistributor.sendToPlayer(player, new SyncQuestPayload(List.of(), List.of(), "", 0), new CustomPacketPayload[0]);
      } else {
         QuestData qd = AwakenedAttachments.quests(player);
         List<SyncQuestPayload.Entry> entries = new ArrayList<>();

         for (Quest q : QuestBoard.board(player)) {
            entries.add(
               new SyncQuestPayload.Entry(
                  q.id(), qd.progress(q.id()), qd.isComplete(q.id()), qd.timesCompleted(q.id()), qd.hasClaimedXp(q.id()), qd.hasClaimedItems(q.id())
               )
            );
         }

         PacketDistributor.sendToPlayer(player, new SyncQuestPayload(entries, List.copyOf(qd.slate()), qd.pinned(), qd.rerolls()), new CustomPacketPayload[0]);
      }
   }
}
