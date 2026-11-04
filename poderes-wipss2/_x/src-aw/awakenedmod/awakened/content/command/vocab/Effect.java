package com.awakenedmod.awakened.content.command.vocab;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public interface Effect extends Word {
   @Override
   default String category() {
      return "effect";
   }

   double defaultWeight();

   boolean apply(ServerPlayer var1, LivingEntity var2, int var3, int var4);

   default void release(LivingEntity target) {
   }

   default void forget(UUID bodyId) {
   }

   default boolean supports(TargetKind kind) {
      return true;
   }

   default boolean playersOnly() {
      return false;
   }

   default boolean appliesTo(LivingEntity body) {
      return !this.playersOnly() || body instanceof Player;
   }

   default boolean actsThroughBlows() {
      return false;
   }

   default double costFactor(TargetKind kind) {
      return 1.0;
   }

   default boolean hasLevel() {
      return false;
   }

   default boolean instant() {
      return false;
   }

   default int repeatTicks() {
      return 0;
   }

   default String numberUnit() {
      return "awakened.command.unit.level";
   }

   default Component numberText(int number) {
      return Component.translatable(this.numberUnit(), new Object[]{number});
   }

   default int durationTicks(LivingEntity target, int written) {
      return written;
   }

   default boolean nullifiesDamageToCaster() {
      return false;
   }

   default boolean hostile() {
      return true;
   }

   default boolean holds() {
      return false;
   }

   default boolean sharesCasterDamage() {
      return false;
   }

   default float incomingDamageMultiplier(int level) {
      return 1.0F;
   }

   default void forgetAllBodies() {
   }
}
