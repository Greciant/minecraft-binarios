package com.awakenedmod.awakened.content.command.vocab.effect;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;

final class HoldMemory<T> {
   private final Map<UUID, HoldMemory.Entry<T>> byBody = new ConcurrentHashMap<>();

   boolean take(LivingEntity body, int age, Supplier<T> fresh) {
      HoldMemory.Entry<T> entry = this.byBody.get(body.getUUID());
      if (entry == null) {
         this.byBody.put(body.getUUID(), new HoldMemory.Entry<>(fresh.get(), body));
         return true;
      } else if (age == 0 && entry.instance.get() == body) {
         entry.holds++;
         return true;
      } else {
         return false;
      }
   }

   @Nullable
   T release(LivingEntity body) {
      HoldMemory.Entry<T> entry = this.byBody.get(body.getUUID());
      if (entry != null && --entry.holds <= 0) {
         this.byBody.remove(body.getUUID());
         return entry.instance.get() == body ? entry.value : null;
      } else {
         return null;
      }
   }

   @Nullable
   T get(LivingEntity body) {
      HoldMemory.Entry<T> entry = this.byBody.get(body.getUUID());
      return entry == null ? null : entry.value;
   }

   void forget(UUID bodyId) {
      this.byBody.remove(bodyId);
   }

   void forgetIf(Predicate<T> which) {
      this.byBody.values().removeIf(entry -> which.test(entry.value));
   }

   void clear() {
      this.byBody.clear();
   }

   private static final class Entry<T> {
      final T value;
      final WeakReference<LivingEntity> instance;
      int holds = 1;

      Entry(T value, LivingEntity body) {
         this.value = value;
         this.instance = new WeakReference<>(body);
      }
   }
}
