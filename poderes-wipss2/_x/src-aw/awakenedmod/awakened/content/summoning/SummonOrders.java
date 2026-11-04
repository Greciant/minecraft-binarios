package com.awakenedmod.awakened.content.summoning;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class SummonOrders {
   private static final Map<UUID, SummonOrders.Order> ORDERS = new ConcurrentHashMap<>();

   private SummonOrders() {
   }

   public static void attack(ServerPlayer owner, LivingEntity foe) {
      ORDERS.put(owner.getUUID(), new SummonOrders.Order(SummonOrders.Mode.ATTACK, foe.getId(), null));
   }

   public static void hold(ServerPlayer owner, Vec3 spot) {
      ORDERS.put(owner.getUUID(), new SummonOrders.Order(SummonOrders.Mode.HOLD, -1, spot));
   }

   public static void clear(ServerPlayer owner) {
      ORDERS.remove(owner.getUUID());
   }

   public static LivingEntity attackTarget(ServerLevel level, UUID owner) {
      SummonOrders.Order order = ORDERS.get(owner);
      if (order != null && order.mode() == SummonOrders.Mode.ATTACK) {
         if (level.getEntity(order.targetId()) instanceof LivingEntity foe && foe.isAlive()) {
            return foe;
         }

         return null;
      } else {
         return null;
      }
   }

   public static void apply(ServerPlayer owner, Mob minion) {
      SummonOrders.Order order = ORDERS.get(owner.getUUID());
      if (order != null) {
         if (order.mode() == SummonOrders.Mode.HOLD && order.spot() != null) {
            minion.setTarget(null);
            MinionSupport.setHolding(minion, order.spot());
         } else if (order.mode() == SummonOrders.Mode.ATTACK && owner.level().getEntity(order.targetId()) instanceof LivingEntity foe && foe.isAlive()) {
            MinionSupport.setHolding(minion, false);
            if (MinionSupport.canPressAttack(minion)) {
               minion.setTarget(foe);
            }
         }
      }
   }

   private static enum Mode {
      ATTACK,
      HOLD;
   }

   private static record Order(SummonOrders.Mode mode, int targetId, Vec3 spot) {
   }
}
