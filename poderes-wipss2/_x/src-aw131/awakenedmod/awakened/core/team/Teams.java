package com.awakenedmod.awakened.core.team;

import com.awakenedmod.awakened.Config;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class Teams {
   private Teams() {
   }

   public static boolean harmForbidden(Entity attacker, Entity victim) {
      if (!(Boolean)Config.TEAMS_ENABLED.get()) {
         return false;
      } else if (attacker != null && victim != null && attacker != victim) {
         if (victim.level() instanceof ServerLevel level) {
            UUID attackerOwner = EntityOwners.ownerOf(attacker);
            if (attackerOwner == null) {
               return false;
            } else {
               UUID victimOwner = EntityOwners.ownerOf(victim);
               if (victimOwner != null && !attackerOwner.equals(victimOwner)) {
                  MinecraftServer server = level.getServer();
                  return TeamService.friendlyFireBlocked(server, attackerOwner, victimOwner);
               } else {
                  return false;
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static Teams.Explanation explain(Entity attacker, Entity victim) {
      UUID attackerOwner = EntityOwners.ownerOf(attacker);
      UUID victimOwner = EntityOwners.ownerOf(victim);
      MinecraftServer server = victim != null && victim.level() instanceof ServerLevel level ? level.getServer() : null;
      boolean same = TeamService.sameTeam(server, attackerOwner, victimOwner);
      UUID teamId = TeamService.teamOf(server, attackerOwner);
      return new Teams.Explanation(
         attackerOwner,
         EntityOwners.resolverFor(attacker),
         victimOwner,
         EntityOwners.resolverFor(victim),
         same,
         TeamService.friendlyFire(server, teamId),
         harmForbidden(attacker, victim)
      );
   }

   public static record Explanation(
      UUID attackerOwner, String attackerVia, UUID victimOwner, String victimVia, boolean sameTeam, boolean friendlyFire, boolean forbidden
   ) {
   }
}
