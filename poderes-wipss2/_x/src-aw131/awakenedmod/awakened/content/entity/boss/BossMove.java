package com.awakenedmod.awakened.content.entity.boss;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;

public interface BossMove<T extends AwakenedBossEntity> {
   byte action();

   int windup();

   int recover();

   double minRange();

   double maxRange();

   default int weight() {
      return 10;
   }

   default boolean requiresGround() {
      return false;
   }

   default void windupTick(T self, ServerLevel level, LivingEntity target, int tick) {
   }

   void strike(T var1, ServerLevel var2, LivingEntity var3);

   default int lookLockAt() {
      return Math.max(2, this.windup() - 6);
   }

   default int retrackAt() {
      return this.windup() + Math.min(8, this.recover() / 2);
   }

   default float lockedTurnSpeed() {
      return 0.0F;
   }

   default boolean rooted() {
      return true;
   }

   default int cooldownTicks() {
      return 0;
   }

   default boolean canStart(T self, ServerLevel level, LivingEntity target) {
      return true;
   }

   default void recoverTick(T self, ServerLevel level, @Nullable LivingEntity target, int ticksSinceStrike) {
   }

   default boolean finishedEarly(T self, int moveTick) {
      return false;
   }

   @Nullable
   default SoundEvent windupCue() {
      return null;
   }

   default float windupCuePitch() {
      return 1.0F;
   }

   default float windupCueVolume() {
      return 1.4F;
   }

   @Nullable
   default SoundEvent commitCue() {
      return null;
   }

   default float commitCuePitch() {
      return 1.0F;
   }

   default double dangerRadius() {
      return 0.0;
   }

   default boolean dangerAtTarget() {
      return false;
   }

   default boolean punishesElevation() {
      return false;
   }

   default boolean needsGroundedTarget() {
      return false;
   }

   default boolean fixedTempo() {
      return false;
   }
}
