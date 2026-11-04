package com.awakenedmod.awakened.content.command.vocab;

import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public interface Trigger extends Word {
   @Override
   default String category() {
      return "trigger";
   }

   boolean live(ServerPlayer var1);

   default Set<Posture> postures() {
      return Set.of();
   }

   default void observe(ServerPlayer caster) {
   }

   default void onCasterHit(ServerPlayer caster, LivingEntity victim) {
   }

   default void onCasterKill(ServerPlayer caster, LivingEntity victim) {
   }

   default void forget(UUID casterId) {
   }

   default void forgetAll() {
   }

   default boolean liveOnClient(Player caster) {
      return true;
   }
}
