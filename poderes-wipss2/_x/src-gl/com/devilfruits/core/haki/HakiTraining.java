package com.devilfruits.core.haki;

import com.devilfruits.core.logia.LogiaManager;
import java.util.ArrayDeque;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.Tags.EntityTypes;

public final class HakiTraining {
   public static final String SPAWNER_TAG = "devilfruits_spawner";

   public static boolean bareFist(ServerPlayer player, DamageSource source) {
      return source.getDirectEntity() == player && source.is(DamageTypes.PLAYER_ATTACK) && player.getMainHandItem().isEmpty();
   }

   public static void onKill(ServerPlayer killer, LivingEntity victim, DamageSource source) {
      float xp = killXp(killer, victim, source, killer.level().getGameTime());
      if (xp > 0.0F) {
         grant(killer, xp);
      }
   }

   public static float killXp(ServerPlayer killer, LivingEntity victim, DamageSource source, long now) {
      if (HakiManager.unlocked(killer) && bareFist(killer, source) && worthy(victim)) {
         float xp = victim.getMaxHealth() * ((Double)HakiConfig.XP_PER_HEALTH.get()).floatValue();
         if (victim instanceof Player other) {
            if (LogiaManager.isLogia(other)) {
               xp *= ((Double)HakiConfig.XP_LOGIA_VICTIM.get()).floatValue();
            }

            if (HakiManager.unlocked(other)) {
               xp *= ((Double)HakiConfig.XP_HAKI_VICTIM.get()).floatValue();
            }
         }

         if (victim.getType().is(EntityTypes.BOSSES)) {
            xp *= ((Double)HakiConfig.XP_BOSS.get()).floatValue();
         }

         if (HakiManager.mask(killer) != 0) {
            xp *= ((Double)HakiConfig.XP_COATED_MULTIPLIER.get()).floatValue();
         }

         PlayerHakiData data = HakiManager.data(killer);
         ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
         ArrayDeque<Long> recent = data.recentKills.computeIfAbsent(type, k -> new ArrayDeque<>());
         long window = (long)((Integer)HakiConfig.XP_SAME_TYPE_WINDOW.get()).intValue() * 20L;

         while (!recent.isEmpty() && now - recent.peekFirst() > window) {
            recent.pollFirst();
         }

         xp *= (float)Math.pow((Double)HakiConfig.XP_SAME_TYPE_FALLOFF.get(), (double)recent.size());
         recent.addLast(now);
         if (recent.size() > 64) {
            recent.pollFirst();
         }

         return xp;
      } else {
         return 0.0F;
      }
   }

   public static void onFistHit(ServerPlayer attacker, LivingEntity victim, DamageSource source) {
      if (HakiManager.unlocked(attacker) && bareFist(attacker, source) && worthy(victim)) {
         float xp = ((Double)HakiConfig.XP_PER_FIST_HIT.get()).floatValue();
         if (HakiManager.mask(attacker) != 0) {
            xp *= ((Double)HakiConfig.XP_COATED_MULTIPLIER.get()).floatValue();
         }

         grant(attacker, xp);
      }
   }

   static boolean worthy(LivingEntity victim) {
      return (double)victim.getMaxHealth() < HakiConfig.XP_MIN_VICTIM_HEALTH.get()
         ? false
         : (Boolean)HakiConfig.XP_FROM_SPAWNERS.get() || !victim.getPersistentData().getBoolean("devilfruits_spawner");
   }

   public static void grant(ServerPlayer player, float xp) {
      PlayerHakiData data = HakiManager.data(player);
      long day = player.level().getDayTime() / 24000L;
      float today = data.xpDay() == day ? data.xpToday() : 0.0F;
      float cap = ((Double)HakiConfig.XP_DAILY_SOFT_CAP.get()).floatValue();
      float full = Math.max(0.0F, Math.min(xp, cap - today));
      float given = full + (xp - full) * ((Double)HakiConfig.XP_OVER_CAP.get()).floatValue();
      data.setXpToday(day, today + xp);
      HakiManager.addXp(player, given);
   }

   private HakiTraining() {
   }
}
