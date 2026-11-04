package com.awakenedmod.awakened.content.pocket;

import com.awakenedmod.awakened.core.Harm;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class DomainManager {
   private static final Map<UUID, DomainManager.Dome> DOMES = new ConcurrentHashMap<>();
   private static final Map<UUID, Set<UUID>> REVEALED = new ConcurrentHashMap<>();

   private DomainManager() {
   }

   public static void set(UUID caster, DomainManager.Dome dome) {
      DOMES.put(caster, dome);
   }

   public static void clear(UUID caster) {
      DOMES.remove(caster);
      REVEALED.remove(caster);
   }

   public static void reveal(UUID caster, UUID mob) {
      if (DOMES.containsKey(caster)) {
         REVEALED.computeIfAbsent(caster, k -> ConcurrentHashMap.newKeySet()).add(mob);
      }
   }

   public static boolean revealed(UUID caster, UUID mob) {
      Set<UUID> set = REVEALED.get(caster);
      return set != null && set.contains(mob);
   }

   public static boolean hasDome(ServerPlayer caster) {
      DomainManager.Dome dome = DOMES.get(caster.getUUID());
      return dome != null && dome.dim().equals(caster.level().dimension());
   }

   public static boolean isSealedByForeignDome(LivingEntity inside) {
      if (DOMES.isEmpty()) {
         return false;
      } else {
         UUID id = inside.getUUID();
         ResourceKey<Level> dim = inside.level().dimension();
         Vec3 pos = inside.position();

         for (DomainManager.Dome dome : DOMES.values()) {
            if (!dome.caster().equals(id)
               && dim.equals(dome.dim())
               && pos.distanceToSqr(dome.center()) <= dome.radius() * dome.radius()
               && Harm.allowed(casterOf(inside, dome), inside)) {
               return true;
            }
         }

         return false;
      }
   }

   private static ServerPlayer casterOf(LivingEntity inside, DomainManager.Dome dome) {
      MinecraftServer server = inside.level().getServer();
      return server == null ? null : server.getPlayerList().getPlayer(dome.caster());
   }

   public static boolean crossesDomeShell(ServerPlayer player, Vec3 from, Vec3 to) {
      if (DOMES.isEmpty()) {
         return false;
      } else {
         ResourceKey<Level> dim = player.serverLevel().dimension();

         for (DomainManager.Dome dome : DOMES.values()) {
            if (dim.equals(dome.dim())) {
               double rSq = dome.radius() * dome.radius();
               boolean fromInside = from.distanceToSqr(dome.center()) <= rSq;
               boolean toInside = to.distanceToSqr(dome.center()) <= rSq;
               if (fromInside != toInside) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public static boolean isNearActiveDome(ResourceKey<Level> dim, Vec3 pos) {
      for (DomainManager.Dome dome : DOMES.values()) {
         if (dim.equals(dome.dim())) {
            double reach = dome.radius() + 2.0;
            if (pos.distanceToSqr(dome.center()) <= reach * reach) {
               return true;
            }
         }
      }

      return false;
   }

   public static record Dome(UUID caster, ResourceKey<Level> dim, Vec3 center, double radius) {
   }
}
