package com.awakenedmod.awakened.core;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class TeamTimeLock {
   private TeamTimeLock() {
   }

   public static boolean engaged(ServerPlayer caster, Iterable<? extends Entity> caught) {
      for (Entity entity : caught) {
         if (entity instanceof Player target && target != caster && target.isAlive() && inFight(target) && !sameSide(caster, target)) {
            return true;
         }
      }

      return false;
   }

   public static boolean allied(ServerPlayer caster, Entity entity) {
      if (entity != caster && entity instanceof LivingEntity && entity.isAlive() && !entity.hasIndirectPassenger(caster)) {
         if (entity instanceof Player target && !inFight(target)) {
            return false;
         }

         return sameSide(caster, entity);
      } else {
         return false;
      }
   }

   public static boolean sameSide(ServerPlayer caster, Entity entity) {
      return !Harm.allowed(caster, entity) || Foes.fightsFor(caster, entity);
   }

   public static void tellHeld(ServerPlayer caster, Entity ally, String key) {
      if (ally instanceof ServerPlayer teammate) {
         teammate.displayClientMessage(Component.translatable(key, new Object[]{caster.getDisplayName()}), true);
      }
   }

   private static boolean inFight(Player player) {
      return !player.isSpectator() && !player.isCreative();
   }
}
