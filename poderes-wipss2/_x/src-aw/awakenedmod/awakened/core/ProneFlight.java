package com.awakenedmod.awakened.core;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent.Size;

@EventBusSubscriber(
   modid = "awakened"
)
public final class ProneFlight {
   public static final Pose POSE = Pose.SPIN_ATTACK;
   public static final float EYE_HEIGHT = 0.55F;
   private static final Set<UUID> SERVER = ConcurrentHashMap.newKeySet();

   private ProneFlight() {
   }

   public static void set(ServerPlayer player, boolean prone) {
      if (prone) {
         SERVER.add(player.getUUID());
      } else {
         SERVER.remove(player.getUUID());
      }
   }

   public static boolean holds(Player p) {
      return !p.level().isClientSide() ? SERVER.contains(p.getUUID()) : p.getPose() == POSE && !p.isAutoSpinAttack();
   }

   @SubscribeEvent
   public static void onEntitySize(Size event) {
      if (event.getPose() == POSE && event.getEntity() instanceof Player p && holds(p)) {
         event.setNewSize(event.getNewSize().withEyeHeight(0.55F * p.getScale()));
      }
   }
}
