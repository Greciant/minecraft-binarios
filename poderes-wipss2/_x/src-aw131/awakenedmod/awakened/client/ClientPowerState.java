package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.Config;
import com.awakenedmod.awakened.core.PowerSuppression;
import com.awakenedmod.awakened.core.net.AbilityFeedbackPayload;
import com.awakenedmod.awakened.core.net.SyncPowerDataPayload;
import com.awakenedmod.awakened.core.net.SyncProgressPayload;
import com.awakenedmod.awakened.core.net.SyncQuestPayload;
import com.awakenedmod.awakened.core.net.SyncRelicPayload;
import com.awakenedmod.awakened.core.net.SyncSkillPayload;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.awakenedmod.awakened.core.skill.Stat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public final class ClientPowerState {
   private static final ClientPowerState INSTANCE = new ClientPowerState();
   private String powerId = "";
   private boolean awakened;
   private float stamina;
   private float staminaMax = 100.0F;
   private float staminaBonus;
   private float staminaLocked;
   private int activeSlot;
   private int masteryLevel;
   private float masteryProgress;
   private boolean infiniteStamina;
   private float abilityPower;
   private float resourceFraction;
   private int resourceTier;
   private float resourceMark;
   private int resourceCooldownSeconds;
   private int[] cooldownLeft = new int[0];
   private int[] cooldownLength = new int[0];
   private long cooldownStamp = -1L;
   private int flashTicks;
   private int failKind = -1;
   private Component failReason;
   private static final int REASON_FLASH_TICKS = 40;
   private static final int GENERIC_FLASH_TICKS = 15;
   private static boolean hintShown;
   private static boolean summoningHintShown;
   private static final int ONBOARDING_DELAY_TICKS = 182;
   private static final int ONBOARDING_MIN_TICKS = 60;
   private static int onboardingDelayTicks = -1;
   private static boolean thunderfallAiming;
   private int timesAwakened;
   private int highestMastery;
   private int enemiesDefeated;
   private int bossesDefeated;
   private int skillLevel = 1;
   private float skillXp;
   private float skillXpForNext = 100.0F;
   private int skillPointsAvailable;
   private final int[] statPoints = new int[Stat.count()];
   private Map<String, ClientPowerState.QuestView> quests = Map.of();
   private List<String> slate = List.of();
   private String pinnedQuest = "";
   private int questRerolls;
   private boolean relicBelt;
   private List<String> attunedRelics = List.of();
   private Map<String, Integer> relicLevels = Map.of();
   private static BooleanSupplier failReasonShown = () -> false;
   private static final ClientPowerState.QuestView EMPTY_QUEST = new ClientPowerState.QuestView(0, false, 0, false, false);

   public static void setThunderfallAiming(boolean aiming) {
      thunderfallAiming = aiming;
   }

   public boolean thunderfallAiming() {
      return thunderfallAiming;
   }

   private ClientPowerState() {
   }

   public static ClientPowerState get() {
      return INSTANCE;
   }

   public static void apply(SyncPowerDataPayload payload) {
      boolean wasAwakened = INSTANCE.awakened;
      String prevPowerId = INSTANCE.powerId;
      INSTANCE.powerId = payload.powerId();
      INSTANCE.awakened = payload.awakened();
      INSTANCE.stamina = payload.stamina();
      INSTANCE.staminaMax = payload.staminaMax();
      INSTANCE.staminaBonus = payload.staminaBonus();
      INSTANCE.staminaLocked = payload.staminaLocked();
      INSTANCE.activeSlot = payload.activeSlot();
      INSTANCE.masteryLevel = payload.masteryLevel();
      INSTANCE.masteryProgress = payload.masteryProgress();
      INSTANCE.infiniteStamina = payload.infiniteStamina();
      INSTANCE.abilityPower = payload.abilityPower();
      INSTANCE.resourceFraction = payload.resourceFraction();
      INSTANCE.resourceTier = payload.resourceTier();
      INSTANCE.resourceMark = payload.resourceMark();
      INSTANCE.resourceCooldownSeconds = payload.resourceCooldownSeconds();
      INSTANCE.cooldownLeft = payload.cooldownLeft();
      INSTANCE.cooldownLength = payload.cooldownLength();
      INSTANCE.cooldownStamp = gameTime();
      if (!wasAwakened && INSTANCE.awakened && !hintShown) {
         hintShown = true;
         onboardingDelayTicks = 182;
      }

      if (!"reaper".equals(INSTANCE.powerId) && ("reaper".equals(prevPowerId) || !INSTANCE.awakened)) {
         ReaperAuraState.clear();
      }

      if (wasAwakened && !INSTANCE.awakened) {
         ReaperHauntFx.clear();
         hintShown = false;
         summoningHintShown = false;
         onboardingDelayTicks = -1;
      }

      if (INSTANCE.awakened && "summoning".equals(INSTANCE.powerId) && !summoningHintShown) {
         summoningHintShown = true;
         showSoulMenuHint();
      }
   }

   private static void showSoulMenuHint() {
      Minecraft mc = Minecraft.getInstance();
      mc.execute(
         () -> {
            if (mc.player != null) {
               mc.player
                  .displayClientMessage(
                     Component.translatable("awakened.onboarding.souls", new Object[]{AwakenedKeybinds.SECONDARY_ABILITY.getTranslatedKeyMessage()})
                        .withStyle(ChatFormatting.AQUA),
                     false
                  );
            }
         }
      );
   }

   public static void applyProgress(SyncProgressPayload payload) {
      INSTANCE.timesAwakened = payload.timesAwakened();
      INSTANCE.highestMastery = payload.highestMastery();
      INSTANCE.enemiesDefeated = payload.enemiesDefeated();
      INSTANCE.bossesDefeated = payload.bossesDefeated();
   }

   public static void applySkill(SyncSkillPayload payload) {
      INSTANCE.skillLevel = payload.level();
      INSTANCE.skillXp = payload.xp();
      INSTANCE.skillXpForNext = payload.xpForNext();
      INSTANCE.skillPointsAvailable = payload.availablePoints();
      List<Integer> points = payload.points();

      for (int i = 0; i < INSTANCE.statPoints.length; i++) {
         INSTANCE.statPoints[i] = i < points.size() ? points.get(i) : 0;
      }
   }

   public static void applyQuests(SyncQuestPayload payload) {
      Map<String, ClientPowerState.QuestView> map = new HashMap<>();

      for (SyncQuestPayload.Entry e : payload.entries()) {
         map.put(e.id(), new ClientPowerState.QuestView(e.progress(), e.complete(), e.timesCompleted(), e.xpClaimed(), e.itemsClaimed()));
      }

      INSTANCE.quests = map;
      INSTANCE.slate = List.copyOf(payload.slate());
      INSTANCE.pinnedQuest = payload.pinned();
      INSTANCE.questRerolls = payload.rerolls();
   }

   public static void applyRelics(SyncRelicPayload payload) {
      INSTANCE.relicBelt = payload.belt();
      INSTANCE.attunedRelics = List.copyOf(payload.attuned());
      INSTANCE.relicLevels = Map.copyOf(payload.levels());
   }

   public static void onFeedback(AbilityFeedbackPayload payload) {
      INSTANCE.failKind = payload.kind();
      INSTANCE.failReason = payload.reasonComponent();
      INSTANCE.flashTicks = INSTANCE.failReason != null ? 40 : 15;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F, 0.6F));
         if (!failReasonShown.getAsBoolean()) {
            mc.player.displayClientMessage(INSTANCE.failMessage(), true);
         }
      }
   }

   private static void showOnboardingHint() {
      Minecraft mc = Minecraft.getInstance();
      mc.execute(
         () -> {
            if (mc.player != null) {
               mc.player
                  .displayClientMessage(
                     Component.translatable("awakened.onboarding.header").withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}), false
                  );
               mc.player
                  .displayClientMessage(
                     Component.translatable("awakened.onboarding.use", new Object[]{AwakenedKeybinds.ACTIVATE.getTranslatedKeyMessage()})
                        .withStyle(ChatFormatting.YELLOW),
                     false
                  );
               mc.player
                  .displayClientMessage(
                     Component.translatable("awakened.onboarding.cycle", new Object[]{AwakenedKeybinds.CYCLE.getTranslatedKeyMessage()})
                        .withStyle(ChatFormatting.YELLOW),
                     false
                  );
               mc.player
                  .displayClientMessage(
                     Component.translatable("awakened.onboarding.screen", new Object[]{AwakenedKeybinds.OPEN_SCREEN.getTranslatedKeyMessage()})
                        .withStyle(ChatFormatting.YELLOW),
                     false
                  );
            }
         }
      );
   }

   public void clientTick() {
      if (this.flashTicks > 0) {
         this.flashTicks--;
      }

      tickOnboardingDelay();
   }

   private static void tickOnboardingDelay() {
      if (onboardingDelayTicks > 0) {
         if (!(Boolean)ClientConfig.AWAKEN_CONTROLS_CHAT.get()) {
            onboardingDelayTicks = -1;
         } else {
            onboardingDelayTicks--;
            int elapsed = 182 - onboardingDelayTicks;
            Minecraft mc = Minecraft.getInstance();
            boolean recovered = mc.player != null && (double)mc.player.getHealth() > (Double)Config.AWAKEN_HEALTH_THRESHOLD.get();
            if (onboardingDelayTicks == 0 || elapsed >= 60 && recovered) {
               onboardingDelayTicks = -1;
               showOnboardingHint();
            }
         }
      }
   }

   public boolean awakened() {
      return this.awakened;
   }

   public String powerId() {
      return this.powerId;
   }

   public boolean suppressed() {
      Minecraft mc = Minecraft.getInstance();
      return mc.player != null && PowerSuppression.isSuppressed(mc.player);
   }

   public String livePowerId() {
      return this.suppressed() ? "" : this.powerId;
   }

   public float stamina() {
      return this.stamina;
   }

   public float staminaMax() {
      return this.staminaMax;
   }

   public float staminaBonus() {
      return this.staminaBonus;
   }

   public float staminaLocked() {
      return this.staminaLocked;
   }

   public int activeSlot() {
      return this.activeSlot;
   }

   public int masteryLevel() {
      return this.masteryLevel;
   }

   public float masteryProgress() {
      return this.masteryProgress;
   }

   public float abilityPower() {
      return this.abilityPower;
   }

   public int flashTicks() {
      return this.flashTicks;
   }

   public static void failReasonShownBy(BooleanSupplier surface) {
      failReasonShown = surface;
   }

   public Component failMessage() {
      return (Component)(switch (this.failKind) {
         case 0 -> Component.translatable("awakened.feedback.no_stamina");
         case 1 -> {
            Power power = PowerRegistry.get(this.powerId);
            Ability ability = power == null ? null : power.ability(this.activeSlot);
            yield Component.translatable("awakened.feedback.locked", new Object[]{ability != null ? ability.unlockLevel() : 0});
         }
         case 2 -> Component.translatable("awakened.feedback.no_effect");
         case 3 -> Component.translatable("awakened.feedback.sealed");
         case 4 -> {
            Component reason = this.failReason;
            yield reason != null ? reason : Component.translatable("awakened.feedback.generic");
         }
         default -> Component.translatable("awakened.feedback.generic");
      });
   }

   public boolean infiniteStamina() {
      return this.infiniteStamina;
   }

   public float resourceFraction() {
      return this.resourceFraction;
   }

   public int resourceTier() {
      return this.resourceTier;
   }

   public float resourceMark() {
      return this.resourceMark;
   }

   public int resourceCooldownSeconds() {
      return this.resourceCooldownSeconds;
   }

   public int cooldownLeft(int slot) {
      if (slot >= 0 && slot < this.cooldownLeft.length && this.cooldownLeft[slot] > 0) {
         long now = gameTime();
         if (this.cooldownStamp < 0L) {
            this.cooldownStamp = now;
         }

         long left = (long)this.cooldownLeft[slot] - Math.max(0L, now - this.cooldownStamp);
         return left > 0L ? (int)left : 0;
      } else {
         return 0;
      }
   }

   public float cooldownFraction(int slot) {
      int left = this.cooldownLeft(slot);
      if (left <= 0) {
         return 0.0F;
      } else {
         int length = slot < this.cooldownLength.length ? this.cooldownLength[slot] : left;
         return Math.min(1.0F, (float)left / (float)Math.max(1, length));
      }
   }

   private static long gameTime() {
      ClientLevel level = Minecraft.getInstance().level;
      return level != null ? level.getGameTime() : -1L;
   }

   public int timesAwakened() {
      return this.timesAwakened;
   }

   public int highestMastery() {
      return this.highestMastery;
   }

   public int enemiesDefeated() {
      return this.enemiesDefeated;
   }

   public int bossesDefeated() {
      return this.bossesDefeated;
   }

   public int skillLevel() {
      return this.skillLevel;
   }

   public float skillXp() {
      return this.skillXp;
   }

   public float skillXpForNext() {
      return this.skillXpForNext;
   }

   public int skillPointsAvailable() {
      return this.skillPointsAvailable;
   }

   public int statPoints(Stat stat) {
      return this.statPoints[stat.ordinal()];
   }

   public ClientPowerState.QuestView quest(String questId) {
      return this.quests.getOrDefault(questId, EMPTY_QUEST);
   }

   public List<String> questSlate() {
      return this.slate;
   }

   public String pinnedQuest() {
      return this.pinnedQuest;
   }

   public int questRerolls() {
      return this.questRerolls;
   }

   public boolean hasRelicBelt() {
      return this.relicBelt;
   }

   public List<String> attunedRelics() {
      return this.attunedRelics;
   }

   public boolean isRelicAttuned(String id) {
      return this.attunedRelics.contains(id);
   }

   public int relicLevel(String id) {
      return Math.max(1, this.relicLevels.getOrDefault(id, 1));
   }

   public static record QuestView(int progress, boolean complete, int timesCompleted, boolean xpClaimed, boolean itemsClaimed) {
   }
}
