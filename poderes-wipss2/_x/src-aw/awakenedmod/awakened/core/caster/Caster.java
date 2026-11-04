package com.awakenedmod.awakened.core.caster;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public interface Caster {
   LivingEntity body();

   ServerLevel level();

   int masteryLevel();

   default float abilityPower() {
      return 0.0F;
   }

   default Vec3 eyePos() {
      return this.body().getEyePosition();
   }

   Vec3 aimDir();

   default float yaw() {
      return this.body().getYRot();
   }

   default float pitch() {
      return this.body().getXRot();
   }

   default Vec3 aimPoint(double range) {
      Vec3 eye = this.eyePos();
      Vec3 end = eye.add(this.aimDir().scale(range));
      BlockHitResult hit = AimUtil.clipBlocks(this.level(), this.body(), eye, end);
      return hit.getType() == Type.MISS ? end : hit.getLocation();
   }

   @Nullable
   default LivingEntity aimTarget(double range) {
      return null;
   }

   default Vec3 throwDirection(Vec3 origin, double range) {
      return AimUtil.throwDirection(this.level(), this.body(), this.eyePos(), this.aimDir(), origin, range);
   }

   default double handSign(boolean offHand) {
      return AimUtil.handSign(this.body(), offHand);
   }

   default boolean rightArm(boolean offHand) {
      return this.body().getMainArm() == HumanoidArm.RIGHT != offHand;
   }

   default Vec3 hand(boolean offHand, double armPitch, double halfWidth, double drop, double reach) {
      return AimUtil.shoulderHand(this.eyePos(), (double)this.yaw(), armPitch, this.handSign(offHand), halfWidth, drop, reach);
   }

   default DamageSource damageSource(ResourceKey<DamageType> type) {
      return Dmg.by(this.body(), type);
   }

   default float scaledDamage(float base, String abilityKey, float... extraBonusFractions) {
      return MasteryScaling.scaledDamage(base, this.masteryLevel(), this.abilityPower(), abilityKey, extraBonusFractions);
   }

   default boolean canHarm(Entity target) {
      return Harm.allowed(this.body(), target);
   }

   float var(String var1, float var2);

   void setVar(String var1, float var2);

   default SoundSource soundSource() {
      return SoundSource.PLAYERS;
   }

   default void syncOwner() {
   }

   default void onHit(LivingEntity victim) {
   }

   default void fail(String langKey, Object... args) {
   }

   @Nullable
   default ServerPlayer asPlayer() {
      return null;
   }
}
