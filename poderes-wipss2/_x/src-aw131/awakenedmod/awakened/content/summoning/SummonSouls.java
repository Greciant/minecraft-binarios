package com.awakenedmod.awakened.content.summoning;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.core.RegistryNames;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.phys.Vec3;

public final class SummonSouls {
   private static final Set<EntityType<?>> TROPHY_TYPES = Set.of(EntityType.ENDER_DRAGON);
   private static final int BASE_CAP = 6;
   private static final int CAP_PER_LEVEL = 10;
   public static final int MAX_CAP = 100;
   private static final int FAST_GROWTH_LEVEL = 6;
   private static final int SUMMON_COST_MIN = 2;
   private static final int SUMMON_COST_MAX = 24;
   private static final double SUMMON_COST_PER_HEALTH = 0.06;
   public static final int QUICK_SLOTS = 5;
   public static final int QUICK_RECENT_SLOTS = 2;

   private SummonSouls() {
   }

   public static boolean isTrophy(String soulType) {
      EntityType<?> type = type(soulType);
      return type != null && TROPHY_TYPES.contains(type);
   }

   public static boolean isDormant(String soulType) {
      return type(soulType) == null;
   }

   public static int cap(ServerPlayer player) {
      return capForLevel(AwakenedAttachments.get(player).masteryLevel());
   }

   public static int maxMinions(ServerPlayer player) {
      return maxMinionsForLevel(AwakenedAttachments.get(player).masteryLevel());
   }

   public static int maxMinionsForLevel(int masteryLevel) {
      int level = Math.max(1, masteryLevel);
      return level <= 6 ? level : 6 + (level - 6) * 2;
   }

   public static int capForLevel(int masteryLevel) {
      return Math.min(100, 6 + masteryLevel * 10);
   }

   public static int summonCost(String soulType) {
      return summonCost(type(soulType));
   }

   public static int summonCost(EntityType<?> type) {
      int cost = (int)Math.round(2.0 + 0.06 * defaultMaxHealth(type));
      return Mth.clamp(cost, 2, 24);
   }

   public static String costRangeLabel() {
      String key = SummonAbility.class.getSimpleName();
      return (int)StaminaSystem.effectiveCost(2.0F, key) + "-" + (int)StaminaSystem.effectiveCost(24.0F, key);
   }

   public static boolean isApex(String soulType) {
      return summonCost(soulType) >= 24;
   }

   private static double defaultMaxHealth(EntityType<?> type) {
      if (type != null && DefaultAttributes.hasSupplier(type)) {
         AttributeSupplier attrs = DefaultAttributes.getSupplier(type);
         return attrs.hasAttribute(Attributes.MAX_HEALTH) ? attrs.getBaseValue(Attributes.MAX_HEALTH) : 0.0;
      } else {
         return 0.0;
      }
   }

   public static List<String> quickPicks(SoulData souls, int slots, int recentSlots) {
      Set<String> picked = new LinkedHashSet<>();
      recentSlots = Math.min(recentSlots, slots);

      for (String type : souls.recent()) {
         if (picked.size() >= recentSlots) {
            break;
         }

         if (raisableNow(souls, type)) {
            picked.add(type);
         }
      }

      List<String> byUse = new ArrayList<>(souls.souls().keySet());
      byUse.removeIf(typex -> picked.contains(typex) || !raisableNow(souls, typex) || souls.useCount(typex) <= 0);
      byUse.sort(Comparator.comparingInt(souls::useCount).reversed());
      fill(picked, byUse, souls, slots);
      fill(picked, souls.souls().keySet(), souls, slots);
      List<String> wheel = new ArrayList<>();

      for (String type : souls.souls().keySet()) {
         if (picked.contains(type)) {
            wheel.add(type);
         }
      }

      return wheel;
   }

   private static void fill(Set<String> picked, Collection<String> candidates, SoulData souls, int slots) {
      for (String type : candidates) {
         if (picked.size() >= slots) {
            return;
         }

         if (raisableNow(souls, type)) {
            picked.add(type);
         }
      }
   }

   private static boolean raisableNow(SoulData souls, String type) {
      return souls.count(type) > 0 && !isDormant(type);
   }

   public static String typeId(LivingEntity entity) {
      return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
   }

   public static EntityType<?> type(String soulType) {
      return RegistryNames.entityType(soulType);
   }

   public static Component displayName(String soulType) {
      return RegistryNames.entityName(soulType);
   }

   public static boolean harvestable(LivingEntity victim) {
      return victim instanceof Mob && !SummonAttachments.minion(victim).isMinion();
   }

   public static void dropSoul(ServerLevel level, LivingEntity victim, UUID owner) {
      ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(owner);
      String type = typeId(victim);
      if (ownerPlayer != null) {
         SoulData data = SummonAttachments.souls(ownerPlayer);
         if (data.bankLocked() || data.isBlacklisted(type)) {
            return;
         }
      }

      SoulOrbEntity orb = new SoulOrbEntity(AwakenedEntities.SOUL_ORB.get(), level);
      Vec3 at = victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0);
      orb.setPos(at.x, at.y, at.z);
      orb.setDeltaMovement(0.0, 0.2, 0.0);
      orb.configure(owner, type);
      level.addFreshEntity(orb);
   }
}
