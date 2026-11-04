package com.awakenedmod.awakened.client;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;

public final class ExpiringEntityValues<T> {
   private final int expiryTicks;
   private final Map<Integer, ExpiringEntityValues.Entry<T>> entries = new ConcurrentHashMap<>();

   public ExpiringEntityValues(int expiryTicks) {
      this.expiryTicks = expiryTicks;
   }

   public void put(int entityId, T value) {
      Minecraft mc = Minecraft.getInstance();
      long now = mc.level != null ? mc.level.getGameTime() : 0L;
      ExpiringEntityValues.Entry<T> e = this.entries.computeIfAbsent(entityId, k -> new ExpiringEntityValues.Entry<>());
      e.value = value;
      e.expiryTick = now + (long)this.expiryTicks;
   }

   public T get(int entityId) {
      ExpiringEntityValues.Entry<T> e = this.entries.get(entityId);
      return e == null ? null : e.value;
   }

   public void clear() {
      this.entries.clear();
   }

   public void tick() {
      if (!this.entries.isEmpty()) {
         Minecraft mc = Minecraft.getInstance();
         long now = mc.level != null ? mc.level.getGameTime() : Long.MAX_VALUE;
         Iterator<ExpiringEntityValues.Entry<T>> it = this.entries.values().iterator();

         while (it.hasNext()) {
            if (now >= it.next().expiryTick) {
               it.remove();
            }
         }
      }
   }

   private static final class Entry<T> {
      T value;
      long expiryTick;
   }
}
