package com.awakenedmod.awakened.content.command;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;

public final class Landings {
   private static final Map<UUID, List<Landings.Landing>> BY_CASTER = new ConcurrentHashMap<>();
   private static final Map<UUID, Integer> BOUND_TARGETS = new ConcurrentHashMap<>();

   private Landings() {
   }

   public static List<Landings.Landing> of(UUID casterId) {
      List<Landings.Landing> list = BY_CASTER.get(casterId);
      return list == null ? Collections.emptyList() : list;
   }

   public static Landings.Landing add(UUID casterId, int slot, UUID target, Rule rule, int ticksLeft, float paid) {
      Landings.Landing landing = new Landings.Landing(slot, target, rule, ticksLeft, paid);
      BY_CASTER.computeIfAbsent(casterId, id -> Collections.synchronizedList(new ArrayList<>(4))).add(landing);
      if (landing.bound()) {
         BOUND_TARGETS.merge(target, 1, Integer::sum);
      }

      return landing;
   }

   public static boolean holds(UUID casterId, int slot, UUID target) {
      for (Landings.Landing landing : of(casterId)) {
         if (landing.slot == slot && landing.target.equals(target)) {
            return true;
         }
      }

      return false;
   }

   public static void expire(@Nullable MinecraftServer server, Landings.Landing landing) {
      Effect effect = landing.rule.effect();
      if (effect != null && CommandSupport.byUuid(server, landing.target) instanceof LivingEntity living) {
         effect.release(living);
      } else if (effect != null) {
         effect.forget(landing.target);
      }
   }

   public static void clearCaster(@Nullable MinecraftServer server, UUID casterId) {
      List<Landings.Landing> list = BY_CASTER.remove(casterId);
      if (list != null) {
         for (Landings.Landing landing : List.copyOf(list)) {
            unbind(landing);
            expire(server, landing);
         }
      }
   }

   public static boolean any() {
      for (List<Landings.Landing> list : BY_CASTER.values()) {
         if (!list.isEmpty()) {
            return true;
         }
      }

      return false;
   }

   public static boolean isBoundTarget(UUID target) {
      return BOUND_TARGETS.containsKey(target);
   }

   static void drop(UUID casterId, Landings.Landing landing) {
      unbind(landing);
      List<Landings.Landing> list = BY_CASTER.get(casterId);
      if (list != null) {
         list.remove(landing);
         if (list.isEmpty()) {
            BY_CASTER.remove(casterId);
         }
      }
   }

   private static void unbind(Landings.Landing landing) {
      if (landing.bound()) {
         BOUND_TARGETS.computeIfPresent(landing.target, (id, n) -> n > 1 ? n - 1 : null);
      }
   }

   public static void clearAll(@Nullable MinecraftServer server) {
      for (UUID casterId : List.copyOf(BY_CASTER.keySet())) {
         clearCaster(server, casterId);
      }

      BY_CASTER.clear();
      BOUND_TARGETS.clear();
   }

   public static final class Landing {
      public final int slot;
      public final UUID target;
      public final Rule rule;
      public final float paid;
      public boolean masteryPaid;
      public int ticksLeft;
      public int age;
      public int entityId = -1;
      public long lastAward = Long.MIN_VALUE;

      Landing(int slot, UUID target, Rule rule, int ticksLeft, float paid) {
         this.slot = slot;
         this.target = target;
         this.rule = rule;
         this.ticksLeft = ticksLeft;
         this.paid = paid;
      }

      public boolean bound() {
         return this.ticksLeft < 0;
      }
   }
}
