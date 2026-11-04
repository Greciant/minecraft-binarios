package com.devilfruits.fruits.suke_suke.client;

import com.devilfruits.fruits.suke_suke.clear.Clear;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class ClearTransitions {
   public static final int TICKS = 24;
   private static final Int2ObjectMap<ClearTransitions.Tr> TR = new Int2ObjectOpenHashMap();
   private static final Int2ObjectMap<Visibility.Mode> LAST = new Int2ObjectOpenHashMap();

   public static void vanish(Entity e, int delay) {
      TR.put(e.getId(), new ClearTransitions.Tr(true, e.level().getGameTime() + (long)delay, null));
   }

   public static void appear(Entity e) {
      Visibility.Mode mode = (Visibility.Mode)LAST.get(e.getId());
      TR.put(e.getId(), new ClearTransitions.Tr(false, e.level().getGameTime(), mode == null ? Visibility.Mode.HIDDEN : mode));
   }

   public static float phase(Entity e, float partial) {
      ClearTransitions.Tr tr = TR.isEmpty() ? null : (ClearTransitions.Tr)TR.get(e.getId());
      if (tr != null) {
         float t = ((float)e.level().getGameTime() + partial - (float)tr.start) / 24.0F;
         if (t < 1.0F) {
            t = Mth.clamp(t, 0.0F, 1.0F);
            return tr.vanish ? t : 1.0F - t;
         }
      }

      return Clear.invisible(e) ? 1.0F : -1.0F;
   }

   public static boolean running(Entity e, float partial) {
      ClearTransitions.Tr tr = TR.isEmpty() ? null : (ClearTransitions.Tr)TR.get(e.getId());
      return tr != null && (float)e.level().getGameTime() + partial - (float)tr.start < 24.0F;
   }

   public static Visibility.Mode relation(Entity e) {
      ClearTransitions.Tr tr = TR.isEmpty() ? null : (ClearTransitions.Tr)TR.get(e.getId());
      if (tr != null && !tr.vanish && !Clear.invisible(e)) {
         return tr.mode;
      } else {
         Visibility.Mode m = Visibility.relation(e);
         if (Clear.invisible(e)) {
            LAST.put(e.getId(), m);
         }

         return m;
      }
   }

   public static boolean gone(Entity e) {
      return phase(e, 0.0F) >= 1.0F;
   }

   static void tick(ClientLevel level) {
      long now = level.getGameTime();
      TR.int2ObjectEntrySet()
         .removeIf(
            en -> now - ((ClearTransitions.Tr)en.getValue()).start > 26L
                  || level.getEntity(en.getIntKey()) == null && now - ((ClearTransitions.Tr)en.getValue()).start > 0L
         );
      if (now % 200L == 0L) {
         LAST.int2ObjectEntrySet().removeIf(en -> level.getEntity(en.getIntKey()) == null);
      }
   }

   static void clear() {
      TR.clear();
      LAST.clear();
   }

   public static float smooth(float x) {
      x = Mth.clamp(x, 0.0F, 1.0F);
      return x * x * (3.0F - 2.0F * x);
   }

   private ClearTransitions() {
   }

   private static record Tr(boolean vanish, long start, Visibility.Mode mode) {
   }
}
