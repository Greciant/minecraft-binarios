package com.awakenedmod.awakened.core.power;

import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.glow.LimbGlow;
import com.awakenedmod.awakened.core.stamina.StaminaResource;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class Power {
   private final String id;
   private final List<Ability> abilities;
   public static final int MAX_ABILITIES = 5;
   private static final float[] NO_TICKS = new float[0];
   public static final long HUD_BLINK_MS = 150L;

   protected Power(String id, List<Ability> abilities) {
      if (!abilities.isEmpty() && abilities.size() <= 5) {
         this.id = id;
         this.abilities = List.copyOf(abilities);
      } else {
         throw new IllegalArgumentException("Power '" + id + "' must have 1-5 abilities");
      }
   }

   public final String id() {
      return this.id;
   }

   public Component displayName() {
      return Component.translatable("power.awakened." + this.id);
   }

   public Component description() {
      return Component.translatable("power.awakened." + this.id + ".desc");
   }

   public List<Ability> abilities() {
      return this.abilities;
   }

   public int abilityCount() {
      return this.abilities.size();
   }

   public Ability ability(int slot) {
      return slot >= 0 && slot < this.abilities.size() ? this.abilities.get(slot) : null;
   }

   public int themeColor() {
      return -13056257;
   }

   public int signatureSlot() {
      return -1;
   }

   public boolean rollable() {
      return true;
   }

   public List<CraftRecipe> recipes() {
      return List.of();
   }

   public PowerProgress progress(Player player, int masteryLevel) {
      return PowerProgress.NONE;
   }

   public boolean progressOnCard() {
      return false;
   }

   public boolean hasSkins() {
      return false;
   }

   public List<PassiveToggle> passiveToggles() {
      return List.of();
   }

   public List<String> extraBalanceKeys() {
      return List.of();
   }

   public void onAwaken(ServerPlayer player) {
   }

   public final void clearAbilityState(ServerPlayer player) {
      for (Ability ability : this.abilities) {
         ability.clearPlayer(player);
      }

      this.onAbilityStateCleared(player);
      LimbGlow.clear(player);
   }

   public final void clearCooldowns(ServerPlayer player) {
      for (Ability ability : this.abilities) {
         ability.forgetCooldown(player);
      }
   }

   public final void clearAllCooldowns() {
      for (Ability ability : this.abilities) {
         ability.forgetAllCooldowns();
      }
   }

   protected void onAbilityStateCleared(ServerPlayer player) {
   }

   public void onSuppressed(ServerPlayer player, PowerData data) {
   }

   public void onUnsuppressed(ServerPlayer player, PowerData data) {
   }

   public void onReplaced(ServerPlayer player) {
   }

   public void onLogin(ServerPlayer player, PowerData data) {
   }

   public void onRespawn(ServerPlayer player, PowerData data) {
   }

   public void onLogout(ServerPlayer player) {
   }

   public final void onDimensionChange(ServerPlayer player, ServerLevel from) {
      for (Ability ability : this.abilities) {
         ability.onDimensionChange(player, from);
      }
   }

   public void passiveTick(ServerPlayer player, PowerData data) {
      for (Ability ability : this.abilities) {
         ability.serverTick(player);
      }
   }

   public boolean hasActiveAbility(ServerPlayer player) {
      for (Ability ability : this.abilities) {
         if (ability.isActive(player)) {
            return true;
         }
      }

      return false;
   }

   public boolean occupiesHands(ServerPlayer player) {
      for (Ability ability : this.abilities) {
         if (ability.occupiesHands(player)) {
            return true;
         }
      }

      return false;
   }

   public void onDealDamage(ServerPlayer attacker, LivingEntity victim) {
   }

   public void onDealDamage(ServerPlayer attacker, LivingEntity victim, DamageSource source) {
      this.onDealDamage(attacker, victim);
   }

   public MeleeProfile meleeProfile(Player player) {
      return null;
   }

   public boolean usesRadialActivate() {
      return false;
   }

   public boolean handlesSecondaryKey() {
      return false;
   }

   public void onSecondaryKey(ServerPlayer player, boolean sneaking, boolean held) {
   }

   public List<PassiveToggle.Wedge> secondaryKeyWheel() {
      return List.of();
   }

   public void onKill(ServerPlayer killer, LivingEntity victim) {
   }

   public void onDeath(ServerPlayer victim, DamageSource source) {
   }

   public float modifyBreakSpeed(Player player, float speed) {
      return speed;
   }

   public float onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
      float current = amount;

      for (Ability ability : this.abilities) {
         current = ability.onIncomingDamage(player, source, current);
      }

      return current;
   }

   public void onDamageTaken(ServerPlayer player, DamageSource source, float amount) {
   }

   public boolean hasResourceBar() {
      return false;
   }

   public int resourceBarColor() {
      return 0;
   }

   public int resourceBarColor(Player viewer) {
      return this.resourceBarColor();
   }

   public int resourceBarColor(@Nullable Player viewer, float fraction) {
      return viewer == null ? this.resourceBarColor() : this.resourceBarColor(viewer);
   }

   public Component resourceBarLabel() {
      return Component.empty();
   }

   public float resourceBarFraction(ServerPlayer player, PowerData data) {
      return 0.0F;
   }

   public int resourceBarTier(ServerPlayer player, PowerData data) {
      return 0;
   }

   public float resourceBarMark(ServerPlayer player, PowerData data) {
      return 0.0F;
   }

   public boolean resourceBarMarkTrails() {
      return false;
   }

   public String resourceBarTierLabel(int tier) {
      return tier > 0 ? "T" + tier : null;
   }

   public String resourceBarTierRoom() {
      return "T0";
   }

   public float resourceBarTierScale() {
      return 1.0F;
   }

   public float resourceBarCeiling(@Nullable Player viewer, int masteryLevel) {
      return 1.0F;
   }

   public int resourceBarTierColor(Player player) {
      return this.resourceBarColor(player);
   }

   public int resourceBarTierColor(Player player, int tier) {
      return this.resourceBarTierColor(player);
   }

   public float[] resourceBarTicks() {
      return NO_TICKS;
   }

   public float resourceBarLockedFrom(Player viewer) {
      return 1.0F;
   }

   public float resourceBarLockedFrom(@Nullable Player viewer, int masteryLevel) {
      return viewer == null ? 1.0F : this.resourceBarLockedFrom(viewer);
   }

   public float resourceBarLockedBelow(@Nullable Player viewer, int masteryLevel) {
      return 0.0F;
   }

   public float resourceBarOrigin() {
      return 0.0F;
   }

   public boolean resourceBarHoldResets() {
      return false;
   }

   public boolean resourceBarFlashing(Player player) {
      return false;
   }

   public String lockedStaminaLabel(Player viewer) {
      return "";
   }

   public List<String> lockedStaminaReturnLabels(Player viewer) {
      return List.of();
   }

   public boolean lockedStaminaLive(Player viewer) {
      return false;
   }

   public boolean abilityLitOnClient(Player viewer, int slot) {
      return false;
   }

   public List<Component> staminaBarLines(Player viewer) {
      return List.of();
   }

   public int staminaBarLineRoom(int mastery) {
      return 0;
   }

   @Nullable
   public String staminaDrainLabel(Player viewer) {
      return null;
   }

   public boolean staminaDrainArmed(Player viewer) {
      return true;
   }

   public int staminaDrainColor(Player viewer) {
      return this.themeColor();
   }

   public static boolean hudBlinkOn() {
      return System.currentTimeMillis() / 150L % 2L == 0L;
   }

   public int resourceBarOverColor(@Nullable Player viewer, float fraction, float ceiling) {
      return 0;
   }

   public PowerResource resource() {
      return StaminaResource.INSTANCE;
   }

   public float bonusMaxStamina(Player player) {
      return 0.0F;
   }

   public float bonusStaminaRegen(Player player) {
      return 0.0F;
   }

   public float lockedStamina(ServerPlayer player, PowerData data) {
      return 0.0F;
   }

   public int resourceBarCooldownSeconds(ServerPlayer player, PowerData data) {
      return 0;
   }

   public boolean resourceBarVertical() {
      return false;
   }

   public Component resourceBarBadge(Player player, float fraction) {
      return null;
   }

   public ItemStack resourceBarBadgeIcon(Player player) {
      return ItemStack.EMPTY;
   }
}
