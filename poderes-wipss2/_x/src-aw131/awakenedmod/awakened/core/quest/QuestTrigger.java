package com.awakenedmod.awakened.core.quest;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import java.util.function.ToIntBiFunction;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;

public enum QuestTrigger {
   LAND_HITS,
   DEFEAT_ENEMIES,
   CLUTCH_SURVIVE,
   DEFEAT_BOSS,
   MINE_BLOCKS,
   CRAFT_ITEMS((player, target) -> AwakenedAttachments.quests(player).uniqueCraftCount()),
   TRAVEL_DISTANCE,
   SURVIVE_NIGHTS,
   USE_SIGNATURE,
   REACH_MASTERY((player, target) -> PowerService.data(player).masteryLevel()),
   DEFEAT_NAMED,
   POWER_FEAT,
   OBTAIN_ITEM(QuestPlaces::countItem),
   REACH_PLACE((player, target) -> QuestPlaces.atPlace(player, target) ? 1 : 0);

   @Nullable
   private final ToIntBiFunction<ServerPlayer, String> liveValue;

   private QuestTrigger() {
      this.liveValue = null;
   }

   private QuestTrigger(ToIntBiFunction<ServerPlayer, String> liveValue) {
      this.liveValue = liveValue;
   }

   public boolean threshold() {
      return this.liveValue != null;
   }

   @Nullable
   public String note() {
      return switch (this) {
         case LAND_HITS, DEFEAT_ENEMIES -> "awakened.quest.note.fights_back";
         case USE_SIGNATURE -> "awakened.quest.note.every_cast";
         case OBTAIN_ITEM -> "awakened.quest.note.carried_now";
         default -> null;
      };
   }

   public int liveValue(ServerPlayer player, String target) {
      return this.liveValue == null ? 0 : this.liveValue.applyAsInt(player, target);
   }

   public static QuestTrigger forReason(MasteryReason reason) {
      return switch (reason) {
         case HIT -> LAND_HITS;
         case KILL -> DEFEAT_ENEMIES;
         case SURVIVAL -> CLUTCH_SURVIVE;
         case USE -> throw new IllegalArgumentException("USE mastery is not a quest trigger");
      };
   }
}
