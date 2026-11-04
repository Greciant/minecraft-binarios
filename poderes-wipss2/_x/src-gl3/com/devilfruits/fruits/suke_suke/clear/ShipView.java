package com.devilfruits.fruits.suke_suke.clear;

import com.devilfruits.fruits.suke_suke.SukeNetwork;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

public final class ShipView {
   private static final Map<UUID, ShipView.Entry> SHIPS = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> RETURNING = new ConcurrentHashMap<>();

   public static void accept(SukeNetwork.ShipPayload p) {
      if (p.end() >= 0L) {
         SHIPS.put(p.ship(), new ShipView.Entry(p.group(), p.start(), p.end()));
         RETURNING.remove(p.ship());
      } else if (SHIPS.remove(p.ship()) != null) {
         RETURNING.put(p.ship(), p.start());
      }
   }

   @Nullable
   public static ShipView.Entry get(UUID ship) {
      return SHIPS.isEmpty() ? null : SHIPS.get(ship);
   }

   public static long returned(UUID ship) {
      Long at = RETURNING.isEmpty() ? null : RETURNING.get(ship);
      return at == null ? -1L : at;
   }

   public static boolean any() {
      return !SHIPS.isEmpty() || !RETURNING.isEmpty();
   }

   public static Map<UUID, ShipView.Entry> all() {
      return SHIPS;
   }

   public static void forgetReturns(long now, int ticks) {
      RETURNING.values().removeIf(at -> now - at > (long)ticks);
   }

   public static void clear() {
      SHIPS.clear();
      RETURNING.clear();
   }

   private ShipView() {
   }

   public static record Entry(UUID group, long start, long end) {
   }
}
