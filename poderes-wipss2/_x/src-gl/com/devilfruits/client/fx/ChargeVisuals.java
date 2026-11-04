package com.devilfruits.client.fx;

import com.devilfruits.client.ClientFruitState;
import com.devilfruits.core.network.ChargePayload;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public final class ChargeVisuals {
   private static final Map<ResourceLocation, ChargeVisuals.ChargeVisual> VISUALS = new HashMap<>();

   public static void register(ResourceLocation ability, ChargeVisuals.ChargeVisual visual) {
      if (VISUALS.putIfAbsent(ability, visual) != null) {
         throw new IllegalStateException("Duplicate charge visual for " + ability);
      }
   }

   public static void handle(ClientLevel level, ChargePayload payload) {
      Entity entity = level.getEntity(payload.entityId());
      if (entity != null) {
         if (ClientFxUtil.isLocal(entity)) {
            if (payload.start()) {
               ClientFruitState.startCharge(payload.ability(), level.getGameTime(), payload.duration());
            } else {
               ClientFruitState.stopCharge();
            }
         }

         ChargeVisuals.ChargeVisual visual = VISUALS.get(payload.ability());
         if (visual != null) {
            if (payload.start()) {
               visual.start(level, entity, payload);
            } else {
               visual.stop(level, entity, payload.released());
            }
         }
      }
   }

   private ChargeVisuals() {
   }

   public interface ChargeVisual {
      void start(ClientLevel var1, Entity var2, ChargePayload var3);

      void stop(ClientLevel var1, Entity var2, boolean var3);
   }
}
