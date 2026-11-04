package com.devilfruits.core.ability;

import com.devilfruits.core.config.AbilityConfig;
import com.devilfruits.core.config.FruitConfigs;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.physics.PhysicsProfile;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;

public abstract class Ability {
   private final ResourceLocation id;
   private final CastType castType;
   private final AbilityStats defaults;

   protected Ability(ResourceLocation id, CastType castType, AbilityStats defaults) {
      this.id = id;
      this.castType = castType;
      this.defaults = defaults;
   }

   public final ResourceLocation id() {
      return this.id;
   }

   public final String name() {
      String path = this.id.getPath();
      return path.substring(path.lastIndexOf(47) + 1);
   }

   public final CastType castType() {
      return this.castType;
   }

   public final AbilityStats defaults() {
      return this.defaults;
   }

   public final AbilityConfig config() {
      return FruitConfigs.ability(this);
   }

   public final DevilFruit fruit() {
      DevilFruit fruit = AbilityRegistry.owner(this);
      if (fruit == null) {
         throw new IllegalStateException("Ability " + this.id + " is not part of a registered fruit");
      } else {
         return fruit;
      }
   }

   public ResourceLocation iconSprite() {
      return ResourceLocation.fromNamespaceAndPath(this.id.getNamespace(), "ability/" + this.id.getPath());
   }

   public final String translationKey() {
      return "ability." + this.id.getNamespace() + "." + this.id.getPath().replace('/', '.');
   }

   public final String descriptionKey() {
      return this.translationKey() + ".desc";
   }

   public final Component displayName() {
      return Component.translatable(this.translationKey());
   }

   @Nullable
   public ResourceKey<DamageType> damageType() {
      return null;
   }

   public float energyCost(float charge) {
      return (float)this.config().energyCost();
   }

   public int cooldownTicks(float charge) {
      return this.config().cooldownTicks();
   }

   public float masteryXp(float charge) {
      return 3.0F + this.energyCost(charge) * 0.2F;
   }

   public boolean coatable() {
      return false;
   }

   public PhysicsProfile physics() {
      return PhysicsProfile.derive(this.defaults);
   }

   @Nullable
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return null;
   }

   public void activate(AbilityContext ctx) {
   }

   public int maxChargeTicks() {
      return 40;
   }

   public void onChargeStart(AbilityContext ctx) {
   }

   public void onChargeTick(AbilityContext ctx, int ticks) {
   }

   public void release(AbilityContext ctx) {
   }

   public void onChargeCancel(ServerPlayer player) {
   }

   public int maxChannelTicks() {
      return 100;
   }

   public int minChannelTicks() {
      return 0;
   }

   public void onChannelStart(AbilityContext ctx) {
   }

   public boolean channelTick(AbilityContext ctx, int ticks) {
      return true;
   }

   public void onChannelEnd(AbilityContext ctx, int ticks, boolean interrupted) {
   }

   public void onToggle(AbilityContext ctx, boolean active) {
   }

   public void toggleTick(AbilityContext ctx) {
   }

   public boolean cooldownOnToggleOff(ServerPlayer player) {
      return true;
   }

   public boolean pausesRegeneration() {
      return true;
   }

   public boolean pressWhileActive(ServerPlayer player) {
      return false;
   }

   public void onKeyRelease(ServerPlayer player) {
   }

   public int maxComboSteps() {
      return 3;
   }

   public int comboWindowTicks() {
      return 16;
   }

   public float comboStepCost(int step) {
      return this.energyCost(1.0F);
   }

   public void comboStep(AbilityContext ctx, int step) {
   }

   public void onStartTracking(ServerPlayer user, ServerPlayer tracker) {
   }

   @Override
   public String toString() {
      return this.id.toString();
   }
}
