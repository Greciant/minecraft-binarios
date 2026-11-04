package com.awakenedmod.awakened.core.power;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

public abstract class Ability {
   private final String id;
   private final float staminaCost;
   private final int unlockLevel;
   private final List<Map<UUID, ?>> playerStateMaps = new CopyOnWriteArrayList<>();
   private final List<Map<UUID, ?>> activeStateMaps = new CopyOnWriteArrayList<>();
   private final Map<UUID, Ability.Cooldown> cooldowns = new ConcurrentHashMap<>();

   protected Ability(String id, float staminaCost, int unlockLevel) {
      this.id = id;
      this.staminaCost = staminaCost;
      this.unlockLevel = unlockLevel;
   }

   protected final <V> Map<UUID, V> playerState() {
      Map<UUID, V> map = new ConcurrentHashMap<>();
      this.playerStateMaps.add(map);
      return map;
   }

   protected final <V> Map<UUID, V> activeState() {
      Map<UUID, V> map = this.playerState();
      this.activeStateMaps.add(map);
      return map;
   }

   public void clearPlayer(ServerPlayer player) {
      UUID id = player.getUUID();

      for (Map<UUID, ?> map : this.playerStateMaps) {
         map.remove(id);
      }
   }

   protected boolean cancelsOnDimensionChange() {
      return false;
   }

   public void onDimensionChange(ServerPlayer player, ServerLevel from) {
      if (this.cancelsOnDimensionChange()) {
         this.clearPlayer(player);
      }
   }

   public boolean isActive(ServerPlayer player) {
      UUID id = player.getUUID();

      for (Map<UUID, ?> map : this.activeStateMaps) {
         if (map.containsKey(id)) {
            return true;
         }
      }

      return false;
   }

   public boolean occupiesHands(ServerPlayer player) {
      return this.isActive(player);
   }

   public boolean ownsBody(ServerPlayer player) {
      return this.isActive(player);
   }

   public final String id() {
      return this.id;
   }

   public final float staminaCost() {
      return this.staminaCost;
   }

   public float staminaCost(Player viewer) {
      return this.staminaCost();
   }

   public final float shownCost(Player viewer) {
      return this.billed(viewer == null ? this.staminaCost() : this.staminaCost(viewer));
   }

   public String costLabel() {
      return String.valueOf((int)this.billed(this.staminaCost));
   }

   public String costLabel(Player viewer) {
      return this.costLabel();
   }

   public Component displayName(Player viewer) {
      return this.displayName();
   }

   public String trayLabel(int resourceTier) {
      return null;
   }

   public float staminaCost(ServerPlayer player) {
      return this.staminaCost;
   }

   public final float effectiveCost(ServerPlayer player) {
      return StaminaSystem.effectiveCost(this.staminaCost(player), this.balanceKey());
   }

   protected final float billed(float rawCost) {
      return StaminaSystem.effectiveCost(rawCost, this.balanceKey());
   }

   public final String balanceKey() {
      return this.getClass().getSimpleName();
   }

   public void serverTick(ServerPlayer player) {
   }

   public void onRelease(ServerPlayer player) {
   }

   public void onSecondaryAction(ServerPlayer player, int action) {
   }

   public float onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
      return amount;
   }

   public boolean usesGripControls() {
      return false;
   }

   public boolean clientDriven() {
      return false;
   }

   public boolean usesSneakActivate() {
      return false;
   }

   public boolean requiresFootingWhenRealistic() {
      return false;
   }

   public boolean ignoresSealWhileActive() {
      return false;
   }

   public final int unlockLevel() {
      return this.unlockLevel;
   }

   public final void forgetCooldown(ServerPlayer player) {
      this.cooldowns.remove(player.getUUID());
   }

   public final void forgetAllCooldowns() {
      this.cooldowns.clear();
   }

   protected final void startCooldown(ServerPlayer player, int ticks) {
      if (ticks <= 0) {
         this.cooldowns.remove(player.getUUID());
      } else {
         this.cooldowns.put(player.getUUID(), new Ability.Cooldown(player.serverLevel().getGameTime() + (long)ticks, ticks));
      }

      PowerService.sync(player);
   }

   public int cooldownRemainingTicks(ServerPlayer player) {
      Ability.Cooldown cd = this.cooldowns.get(player.getUUID());
      if (cd == null) {
         return 0;
      } else {
         long left = cd.readyAt() - player.serverLevel().getGameTime();
         return left > 0L ? (int)Math.min(2147483647L, left) : 0;
      }
   }

   public int cooldownLengthTicks(ServerPlayer player) {
      Ability.Cooldown cd = this.cooldowns.get(player.getUUID());
      return cd != null ? cd.length() : this.cooldownRemainingTicks(player);
   }

   public boolean refusesPressWhileCooling() {
      return true;
   }

   public String activationAnimation(ServerPlayer player) {
      return this.id;
   }

   public Component displayName() {
      return Component.translatable("ability.awakened." + this.id);
   }

   public Component description() {
      return Component.translatable("ability.awakened." + this.id + ".desc");
   }

   public AbilityScaling scalingInfo() {
      return AbilityScaling.NONE;
   }

   public boolean isUnlocked(PowerData data) {
      return data.masteryLevel() >= this.unlockLevel;
   }

   public static <T extends Ability> T resolveClientFire(ServerPlayer player, int slot, Class<T> type) {
      if (player == null) {
         return null;
      } else {
         PowerData data = PowerService.data(player);
         if (!data.awakened()) {
            return null;
         } else {
            Power power = PowerRegistry.get(data.powerId());
            if (power == null) {
               return null;
            } else {
               Ability ability = power.ability(slot);
               if (!type.isInstance(ability)) {
                  return null;
               } else if (!ability.isUnlocked(data)) {
                  PowerService.feedback(player, 1);
                  return null;
               } else {
                  return PowerService.refusedBySeal(player, ability) ? null : type.cast(ability);
               }
            }
         }
      }
   }

   protected final boolean fail(ServerPlayer player, String langKey, Object... args) {
      AbilityFeedback.fail(player, langKey, args);
      return false;
   }

   protected final boolean failSilently(ServerPlayer player) {
      AbilityFeedback.spoke(player);
      return false;
   }

   protected final boolean charge(ServerPlayer player, float cost) {
      return PowerService.charge(player, this, this.billed(cost));
   }

   protected final PowerResource resource(ServerPlayer player) {
      return PowerService.resource(player);
   }

   public abstract boolean activate(ServerPlayer var1);

   private static record Cooldown(long readyAt, int length) {
   }
}
