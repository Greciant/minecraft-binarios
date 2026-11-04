package com.devilfruits.core.logia;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public interface LogiaMovementMode {
   ResourceLocation id();

   default boolean canStart(ServerPlayer player) {
      return true;
   }

   default void start(ServerPlayer player, LogiaMovement.Session session) {
   }

   boolean tick(ServerPlayer var1, LogiaMovement.Session var2);

   default Vec3 reformTarget(ServerPlayer player, LogiaMovement.Session session) {
      return player.position();
   }

   default boolean reformInFluid() {
      return false;
   }

   default void end(ServerPlayer player, LogiaMovement.Session session, boolean interrupted) {
   }

   default float drainPerSecond() {
      return 0.0F;
   }

   default int maxTicks() {
      return 200;
   }

   default boolean forcesBody() {
      return false;
   }

   default boolean staysElemental(ServerPlayer player, LogiaMovement.Session session) {
      return false;
   }
}
