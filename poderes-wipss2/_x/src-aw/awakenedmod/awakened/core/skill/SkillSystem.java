package com.awakenedmod.awakened.core.skill;

import com.awakenedmod.awakened.Awakened;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.SkillData;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public final class SkillSystem {
   public static final double XP_PER_MASTERY_POINT = 1.0;
   public static final double XP_MASTERY_LEVEL = 25.0;
   public static final double XP_AWAKEN = 50.0;
   public static final double XP_BOSS = 200.0;
   private static final ResourceLocation HEALTH_MODIFIER_ID = AwId.of("skill_max_health");

   private SkillSystem() {
   }

   public static SkillData data(ServerPlayer player) {
      return AwakenedAttachments.skill(player);
   }

   public static int availablePoints(SkillData data) {
      return Math.max(0, SkillFormula.pointsForLevel(data.level()) - data.totalSpent());
   }

   public static void award(ServerPlayer player, double amount) {
      if (!(amount <= 0.0)) {
         SkillData data = data(player);
         int before = data.level();
         data.addXp(amount);

         while (data.xp() >= SkillFormula.xpForNext(data.level())) {
            double carry = data.xp() - SkillFormula.xpForNext(data.level());
            data.levelUp(carry);
         }

         if (data.level() > before) {
            onLevelUp(player, before, data.level());
         }

         PowerService.syncSkill(player);
      }
   }

   private static void onLevelUp(ServerPlayer player, int fromLevel, int toLevel) {
      player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 0.8F);
      int gained = SkillFormula.pointsForLevel(toLevel) - SkillFormula.pointsForLevel(fromLevel);
      player.displayClientMessage(Component.translatable("awakened.skill.levelup", new Object[]{toLevel, gained}).withStyle(ChatFormatting.GREEN), false);
      Awakened.LOGGER.debug("Player {} reached character level {}", player.getName().getString(), toLevel);
   }

   public static void handleAllocate(ServerPlayer player, int statId, int delta) {
      if (player != null) {
         if (statId < 0) {
            respec(player);
         } else {
            Stat stat = Stat.byId(statId);
            if (stat != null) {
               allocate(player, stat, delta);
            }
         }
      }
   }

   public static boolean allocate(ServerPlayer player, Stat stat, int delta) {
      SkillData data = data(player);
      int next = data.points(stat) + delta;
      if (next >= 0 && next <= stat.maxPoints()) {
         if (delta > 0 && availablePoints(data) < delta) {
            return false;
         } else {
            data.setPoints(stat, next);
            onStatsChanged(player);
            return true;
         }
      } else {
         return false;
      }
   }

   public static void respec(ServerPlayer player) {
      data(player).clearAllocations();
      onStatsChanged(player);
   }

   public static void setLevel(ServerPlayer player, int level, double xp) {
      data(player).set(level, xp);
      onStatsChanged(player);
   }

   public static void resetCharacter(ServerPlayer player) {
      data(player).reset();
      onStatsChanged(player);
   }

   private static void onStatsChanged(ServerPlayer player) {
      applyAttributes(player);
      float max = StaminaSystem.max(player);
      if (PowerService.data(player).stamina() > max) {
         PowerService.data(player).setStamina(max);
      }

      PowerService.sync(player);
      PowerService.syncSkill(player);
   }

   public static void applyAttributes(ServerPlayer player) {
      AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
      if (health != null) {
         health.removeModifier(HEALTH_MODIFIER_ID);
         float bonus = data(player).bonus(Stat.MAX_HEALTH);
         if (bonus > 0.0F) {
            health.addTransientModifier(new AttributeModifier(HEALTH_MODIFIER_ID, (double)bonus, Operation.ADD_VALUE));
         }

         if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
         }
      }
   }
}
