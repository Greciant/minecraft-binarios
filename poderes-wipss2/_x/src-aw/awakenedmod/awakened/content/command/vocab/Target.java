package com.awakenedmod.awakened.content.command.vocab;

import java.util.Collection;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public interface Target extends Word {
   @Override
   default String category() {
      return "target";
   }

   double defaultWeight();

   TargetKind kind();

   default Collection<LivingEntity> select(ServerPlayer caster, RuleContext ctx) {
      return List.of();
   }

   default boolean usesRadius() {
      return this.kind() == TargetKind.NEAR;
   }

   default boolean namesPlayers() {
      return true;
   }

   default boolean repeatsInstantWords() {
      return this.kind() == TargetKind.NEAR;
   }

   default boolean carriesCaster() {
      return false;
   }

   default boolean movesWithCaster() {
      return this.kind() == TargetKind.SELF || this.carriesCaster();
   }

   default boolean allowsInfinite() {
      return !this.kind().landsBySelect();
   }

   default boolean sparesOwnSide() {
      return false;
   }
}
