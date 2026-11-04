package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.earth.StoneMechEntity;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre;

public final class MeldHidden {
   private static final Set<Integer> HIDDEN = ConcurrentHashMap.newKeySet();

   private MeldHidden() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(MeldHidden::onRenderPlayer);
   }

   public static void set(int entityId, boolean hidden) {
      if (hidden) {
         HIDDEN.add(entityId);
      } else {
         HIDDEN.remove(entityId);
      }
   }

   public static boolean isHidden(int entityId) {
      return HIDDEN.contains(entityId);
   }

   public static boolean shouldHide(Entity entity) {
      return isHidden(entity.getId()) || entity.getVehicle() instanceof StoneMechEntity;
   }

   private static void onRenderPlayer(Pre event) {
      if (shouldHide(event.getEntity())) {
         event.setCanceled(true);
      }
   }
}
