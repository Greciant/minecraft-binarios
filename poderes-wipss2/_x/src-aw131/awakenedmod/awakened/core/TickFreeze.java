package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent.Pre;

@EventBusSubscriber(
   modid = "awakened"
)
public final class TickFreeze {
   private static final Map<Integer, Integer> HELD = new ConcurrentHashMap<>();
   private static final Map<Integer, Integer> SOFT = new ConcurrentHashMap<>();
   private static final Map<Integer, int[]> SLOWED = new ConcurrentHashMap<>();

   private TickFreeze() {
   }

   public static void freeze(Entity entity) {
      freeze(entity, false);
   }

   public static void freeze(Entity entity, boolean keepHurtCooldown) {
      (keepHurtCooldown ? SOFT : HELD).merge(entity.getId(), 1, Integer::sum);
   }

   public static void release(Entity entity) {
      release(entity, false);
   }

   public static void release(Entity entity, boolean keepHurtCooldown) {
      (keepHurtCooldown ? SOFT : HELD).computeIfPresent(entity.getId(), (id, count) -> count <= 1 ? null : count - 1);
   }

   public static boolean isFrozen(Entity entity) {
      return HELD.containsKey(entity.getId()) || SOFT.containsKey(entity.getId());
   }

   public static void slow(Entity entity, int divisor) {
      int period = period(divisor);
      if (entity != null && period >= 2) {
         SLOWED.compute(entity.getId(), (id, held) -> add(held, period));
         pushSlowState(entity);
      }
   }

   public static void unslow(Entity entity, int divisor) {
      int period = period(divisor);
      if (entity != null && period >= 2) {
         SLOWED.computeIfPresent(entity.getId(), (id, held) -> drop(held, period));
         pushSlowState(entity);
      }
   }

   public static int divisorOf(Entity entity) {
      if (entity == null) {
         return 1;
      } else {
         SlowState state = (SlowState)entity.getExistingDataOrNull(AwakenedAttachments.SLOW_STATE.get());
         return state == null ? 1 : Math.max(1, state.divisor());
      }
   }

   private static int period(int divisor) {
      return divisor < 2 ? 1 : divisor | 1;
   }

   private static void pushSlowState(Entity entity) {
      if (!entity.level().isClientSide()) {
         int divisor = strongest(SLOWED.get(entity.getId()));
         SlowState current = (SlowState)entity.getExistingDataOrNull(AwakenedAttachments.SLOW_STATE.get());
         if (divisor > 1) {
            if (current == null || current.divisor() != divisor) {
               entity.setData(AwakenedAttachments.SLOW_STATE.get(), new SlowState(divisor));
            }
         } else if (current != null) {
            entity.removeData(AwakenedAttachments.SLOW_STATE.get());
         }
      }
   }

   private static int[] add(int[] held, int divisor) {
      if (held == null) {
         return new int[]{divisor};
      } else {
         int[] next = Arrays.copyOf(held, held.length + 1);
         next[held.length] = divisor;
         return next;
      }
   }

   private static int[] drop(int[] held, int divisor) {
      int at = -1;

      for (int i = 0; i < held.length; i++) {
         if (held[i] == divisor) {
            at = i;
            break;
         }
      }

      if (at < 0) {
         return held;
      } else if (held.length == 1) {
         return null;
      } else {
         int[] next = new int[held.length - 1];
         System.arraycopy(held, 0, next, 0, at);
         System.arraycopy(held, at + 1, next, at, held.length - at - 1);
         return next;
      }
   }

   private static int strongest(int[] held) {
      if (held != null && held.length != 0) {
         int best = held[0];

         for (int divisor : held) {
            best = Math.min(best, divisor);
         }

         return best;
      } else {
         return 1;
      }
   }

   @SubscribeEvent
   public static void onEntityTick(Pre event) {
      Entity entity = event.getEntity();
      if (entity.level().isClientSide()) {
         if (SlowMotion.skipping(entity)) {
            hurtTimersOnRealTime(entity);
            event.setCanceled(true);
         }
      } else if (!(entity instanceof Player)) {
         if (HELD.containsKey(entity.getId())) {
            entity.invulnerableTime = 0;
            if (entity instanceof LivingEntity living) {
               living.hurtTime = 0;
            }

            event.setCanceled(true);
         } else if (SOFT.containsKey(entity.getId())) {
            event.setCanceled(true);
         } else {
            int divisor = strongest(SLOWED.get(entity.getId()));
            if (divisor > 1 && entity.tickCount % divisor != 0) {
               hurtTimersOnRealTime(entity);
               event.setCanceled(true);
            }
         }
      }
   }

   private static void hurtTimersOnRealTime(Entity entity) {
      if (entity.invulnerableTime > 0) {
         entity.invulnerableTime--;
      }

      if (entity instanceof LivingEntity living && living.hurtTime > 0) {
         living.hurtTime--;
      }
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      HELD.clear();
      SOFT.clear();
      SLOWED.clear();
   }
}
