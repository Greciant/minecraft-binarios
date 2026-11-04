package com.awakenedmod.awakened.core.team;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

public final class EntityOwners {
   private static final List<EntityOwners.Resolver> RESOLVERS = new CopyOnWriteArrayList<>();
   private static final int MAX_DEPTH = 2;

   private EntityOwners() {
   }

   public static void register(String label, Function<Entity, UUID> resolver) {
      RESOLVERS.add(new EntityOwners.Resolver(label, resolver));
   }

   public static UUID ownerOf(Entity entity) {
      return resolve(entity, 0);
   }

   public static String resolverFor(Entity entity) {
      if (entity instanceof Player) {
         return "player";
      } else {
         for (EntityOwners.Resolver resolver : RESOLVERS) {
            if (entity != null && resolver.fn().apply(entity) != null) {
               return resolver.label();
            }
         }

         return null;
      }
   }

   public static List<String> labels() {
      return RESOLVERS.stream().map(EntityOwners.Resolver::label).toList();
   }

   private static UUID resolve(Entity entity, int depth) {
      if (entity != null && depth <= 2) {
         if (entity instanceof Player player) {
            return player.getUUID();
         } else {
            for (EntityOwners.Resolver resolver : RESOLVERS) {
               UUID owner = resolver.fn().apply(entity);
               if (owner != null) {
                  return owner;
               }
            }

            if (entity instanceof Projectile projectile) {
               return resolve(projectile.getOwner(), depth + 1);
            } else {
               if (entity instanceof TraceableEntity traceable && !(entity instanceof ItemEntity)) {
                  return resolve(traceable.getOwner(), depth + 1);
               }

               return null;
            }
         }
      } else {
         return null;
      }
   }

   private static record Resolver(String label, Function<Entity, UUID> fn) {
   }
}
