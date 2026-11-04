package com.awakenedmod.awakened.core.quest;

import com.awakenedmod.awakened.core.RegistryNames;
import com.awakenedmod.awakened.core.power.PassiveToggleService;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public record Quest(
   String id,
   @Nullable String powerId,
   QuestTier tier,
   boolean goal,
   QuestTrigger trigger,
   String target,
   int required,
   int masteryReward,
   int xpReward,
   List<QuestReward> rewards,
   @Nullable String prereq,
   int minMastery,
   @Nullable String needsToggle
) {
   public static Quest.Builder trial(String id, QuestTier tier) {
      return new Quest.Builder(id, tier, false);
   }

   public static Quest.Builder goal(String id, QuestTier tier) {
      return new Quest.Builder(id, tier, true);
   }

   public boolean universal() {
      return this.powerId == null;
   }

   public boolean availableTo(@Nullable String pid) {
      return this.universal() || this.powerId.equals(pid);
   }

   public boolean unlockedFor(int masteryLevel) {
      return masteryLevel >= this.minMastery;
   }

   public boolean allowedBySettings(ServerPlayer player) {
      return this.needsToggle == null || PassiveToggleService.enabled(player, this.needsToggle);
   }

   public boolean matches(@Nullable QuestTrigger reported, String reportedTarget) {
      return reported == null ? this.trigger.threshold() : this.trigger == reported && (this.target.isEmpty() || this.target.equals(reportedTarget));
   }

   public Component title() {
      return Component.translatable("quest.awakened." + this.id + ".title");
   }

   public Component description() {
      return Component.translatable("quest.awakened." + this.id + ".desc", new Object[]{this.required, this.targetName()});
   }

   @Nullable
   public Component note() {
      String key = this.trigger.note();
      return key == null ? null : Component.translatable(key);
   }

   public Component targetName() {
      if (this.target.isEmpty()) {
         return Component.empty();
      } else {
         return (Component)(switch (this.trigger) {
            case DEFEAT_NAMED -> RegistryNames.entityName(this.target);
            case OBTAIN_ITEM -> RegistryNames.itemName(this.target);
            case REACH_PLACE -> Component.translatable("awakened.place." + this.target.replace('/', '.').replace(':', '.'));
            default -> Component.literal(this.target);
         });
      }
   }

   public static final class Builder {
      private final String id;
      private final QuestTier tier;
      private final boolean goal;
      private String powerId;
      private QuestTrigger trigger = QuestTrigger.DEFEAT_ENEMIES;
      private String target = "";
      private int required = 1;
      private int mastery;
      private int xp;
      private final List<QuestReward> rewards = new ArrayList<>();
      private String prereq;
      private int minMastery;
      private String needsToggle;

      private Builder(String id, QuestTier tier, boolean goal) {
         this.id = id;
         this.tier = tier;
         this.goal = goal;
      }

      public Quest.Builder power(String powerId) {
         this.powerId = powerId;
         return this;
      }

      public Quest.Builder trigger(QuestTrigger trigger, int required) {
         this.trigger = trigger;
         this.required = required;
         return this;
      }

      public Quest.Builder kill(String entityTypeId, int count) {
         this.trigger = QuestTrigger.DEFEAT_NAMED;
         this.target = entityTypeId;
         this.required = count;
         return this;
      }

      public Quest.Builder place(String target) {
         this.trigger = QuestTrigger.REACH_PLACE;
         this.target = target;
         this.required = 1;
         return this;
      }

      public Quest.Builder obtain(String itemId, int count) {
         this.trigger = QuestTrigger.OBTAIN_ITEM;
         this.target = itemId;
         this.required = count;
         return this;
      }

      public Quest.Builder feat(String featKey, int count) {
         this.trigger = QuestTrigger.POWER_FEAT;
         this.target = featKey;
         this.required = count;
         return this;
      }

      public Quest.Builder needsMastery(int level) {
         this.minMastery = level;
         return this;
      }

      public Quest.Builder needsToggle(String toggleKey) {
         this.needsToggle = toggleKey;
         return this;
      }

      public Quest.Builder mastery(int mastery) {
         this.mastery = mastery;
         return this;
      }

      public Quest.Builder xp(int xp) {
         this.xp = xp;
         return this;
      }

      public Quest.Builder reward(QuestReward... rewards) {
         this.rewards.addAll(List.of(rewards));
         return this;
      }

      public Quest.Builder after(String prereqId) {
         this.prereq = prereqId;
         return this;
      }

      public Quest register() {
         if (this.id == null || this.id.isBlank()) {
            throw new IllegalArgumentException("Quest id must not be blank");
         } else if (this.required < 1) {
            throw new IllegalArgumentException("Quest '" + this.id + "' needs a goal of at least 1");
         } else if (this.prereq != null && !this.goal) {
            throw new IllegalArgumentException("Trial quest '" + this.id + "' cannot have a prerequisite (after())");
         } else if (this.minMastery < 0) {
            throw new IllegalArgumentException("Quest '" + this.id + "' cannot need a negative mastery level");
         } else if (this.minMastery > 0 && this.goal) {
            throw new IllegalArgumentException("Goal quest '" + this.id + "' cannot use needsMastery()");
         } else if (this.needsToggle != null && this.goal) {
            throw new IllegalArgumentException("Goal quest '" + this.id + "' cannot use needsToggle()");
         } else {
            Quest quest = new Quest(
               this.id,
               this.powerId,
               this.tier,
               this.goal,
               this.trigger,
               this.target,
               this.required,
               this.mastery,
               this.xp,
               List.copyOf(this.rewards),
               this.prereq,
               this.minMastery,
               this.needsToggle
            );
            QuestRegistry.register(quest);
            return quest;
         }
      }
   }
}
