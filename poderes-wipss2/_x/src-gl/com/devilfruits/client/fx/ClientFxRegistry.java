package com.devilfruits.client.fx;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.network.FxPayload;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public final class ClientFxRegistry {
   private static final Map<ResourceLocation, ClientFxRegistry.FxHandler> HANDLERS = new HashMap<>();
   private static final Set<ResourceLocation> WARNED = new HashSet<>();

   public static void register(ResourceLocation fx, ClientFxRegistry.FxHandler handler) {
      if (HANDLERS.putIfAbsent(fx, handler) != null) {
         throw new IllegalStateException("Duplicate client effect handler " + fx);
      }
   }

   public static void dispatch(ClientLevel level, FxPayload payload) {
      ClientFxRegistry.FxHandler handler = HANDLERS.get(payload.fx());
      if (handler == null) {
         if (WARNED.add(payload.fx())) {
            DevilFruits.LOGGER.warn("No client handler for effect {}", payload.fx());
         }
      } else {
         Entity entity = payload.entityId() >= 0 ? level.getEntity(payload.entityId()) : null;
         handler.handle(level, entity, payload);
      }
   }

   private ClientFxRegistry() {
   }

   @FunctionalInterface
   public interface FxHandler {
      void handle(ClientLevel var1, @Nullable Entity var2, FxPayload var3);
   }
}
