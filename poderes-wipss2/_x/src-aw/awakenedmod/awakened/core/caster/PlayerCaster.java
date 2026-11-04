package com.awakenedmod.awakened.core.caster;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.AbilityFeedback;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public record PlayerCaster(ServerPlayer player) implements Caster {
   public static PlayerCaster of(ServerPlayer player) {
      return new PlayerCaster(player);
   }

   @Override
   public LivingEntity body() {
      return this.player;
   }

   @Override
   public ServerLevel level() {
      return this.player.serverLevel();
   }

   @Override
   public int masteryLevel() {
      return PowerService.data(this.player).masteryLevel();
   }

   @Override
   public float abilityPower() {
      return MasteryScaling.abilityPower(this.player);
   }

   @Override
   public Vec3 eyePos() {
      return this.player.getEyePosition();
   }

   @Override
   public Vec3 aimDir() {
      return this.player.getViewVector(1.0F);
   }

   @Override
   public float yaw() {
      return this.player.getYRot();
   }

   @Override
   public float pitch() {
      return this.player.getXRot();
   }

   @Override
   public Vec3 aimPoint(double range) {
      return AimUtil.aimedPosition(this.player, range);
   }

   @Nullable
   @Override
   public LivingEntity aimTarget(double range) {
      return AimUtil.aimedLivingEntity(this.player, range);
   }

   @Override
   public Vec3 throwDirection(Vec3 origin, double range) {
      return AimUtil.throwDirection(this.player, origin, range);
   }

   @Override
   public DamageSource damageSource(ResourceKey<DamageType> type) {
      return Dmg.by(this.player, type);
   }

   @Override
   public float scaledDamage(float base, String abilityKey, float... extraBonusFractions) {
      return MasteryScaling.scaledDamage(base, this.player, abilityKey, extraBonusFractions);
   }

   @Override
   public boolean canHarm(Entity target) {
      return Harm.allowed(this.player, target);
   }

   @Override
   public float var(String key, float fallback) {
      return PowerService.data(this.player).var(key, fallback);
   }

   @Override
   public void setVar(String key, float value) {
      PowerService.data(this.player).setVar(key, value);
   }

   @Override
   public void syncOwner() {
      PowerService.sync(this.player);
   }

   @Override
   public void onHit(LivingEntity victim) {
      MasterySystem.award(this.player, MasteryReason.HIT, victim);
   }

   @Override
   public void fail(String langKey, Object... args) {
      AbilityFeedback.fail(this.player, langKey, args);
   }

   @Override
   public ServerPlayer asPlayer() {
      return this.player;
   }
}
