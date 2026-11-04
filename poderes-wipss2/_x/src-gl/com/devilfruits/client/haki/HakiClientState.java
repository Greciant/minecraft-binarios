package com.devilfruits.client.haki;

import com.devilfruits.core.haki.HakiManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class HakiClientState {
   private static final int STEPS = 6;
   private static final float GROW = 0.14285715F;
   private static final float SHRINK = 0.2F;
   private static final Int2ObjectOpenHashMap<HakiClientState.Entry> ENTRIES = new Int2ObjectOpenHashMap();
   private static long clock;

   public static void tick(ClientLevel level) {
      clock++;

      for (AbstractClientPlayer player : level.players()) {
         int mask = HakiManager.mask(player);
         HakiClientState.Entry e = (HakiClientState.Entry)ENTRIES.get(player.getId());
         if (e == null) {
            if (mask == 0) {
               continue;
            }

            e = new HakiClientState.Entry();
            ENTRIES.put(player.getId(), e);
         }

         e.seen = clock;
         e.previous = e.progress;
         if (mask != 0) {
            e.shown = mask;
            e.progress = Math.min(1.0F, e.progress + 0.14285715F);
         } else {
            e.progress = Math.max(0.0F, e.progress - (e.flicker > 0 ? 0.1F : 0.2F));
            if (e.progress <= 0.0F && e.contactTicks <= 0) {
               e.shown = 0;
            }
         }

         if (e.flicker > 0) {
            e.flicker--;
         }

         if (e.contactTicks > 0) {
            e.contactTicks--;
         }
      }

      ObjectIterator<it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry<HakiClientState.Entry>> it = ENTRIES.int2ObjectEntrySet().iterator();

      while (it.hasNext()) {
         HakiClientState.Entry ex = (HakiClientState.Entry)((it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry)it.next()).getValue();
         if (ex.seen != clock || ex.shown == 0 && ex.progress <= 0.0F && ex.contactTicks <= 0) {
            it.remove();
         }
      }
   }

   public static void clear() {
      ENTRIES.clear();
   }

   public static int shown(Entity entity) {
      HakiClientState.Entry e = (HakiClientState.Entry)ENTRIES.get(entity.getId());
      return e != null && !(e.progress <= 0.0F) ? e.shown : 0;
   }

   public static float progress(Entity entity, float partialTick) {
      HakiClientState.Entry e = (HakiClientState.Entry)ENTRIES.get(entity.getId());
      if (e == null) {
         return 0.0F;
      } else {
         float p = Mth.lerp(partialTick, e.previous, e.progress);
         return (float)Math.round(p * 6.0F) / 6.0F;
      }
   }

   public static boolean blinkedOut(Entity entity) {
      HakiClientState.Entry e = (HakiClientState.Entry)ENTRIES.get(entity.getId());
      return e != null && e.flicker > 0 && e.flicker / 2 % 2 == 0;
   }

   public static int contact(Entity entity) {
      HakiClientState.Entry e = (HakiClientState.Entry)ENTRIES.get(entity.getId());
      return e != null && e.contactTicks > 0 ? e.contact : 0;
   }

   public static void flicker(int entityId) {
      HakiClientState.Entry e = (HakiClientState.Entry)ENTRIES.computeIfAbsent(entityId, k -> new HakiClientState.Entry());
      e.flicker = 12;
      e.seen = clock;
   }

   public static void contact(int entityId, int mask, int ticks) {
      HakiClientState.Entry e = (HakiClientState.Entry)ENTRIES.computeIfAbsent(entityId, k -> new HakiClientState.Entry());
      e.contact = mask;
      e.contactTicks = ticks;
      e.seen = clock;
   }

   private HakiClientState() {
   }

   private static final class Entry {
      int shown;
      float progress;
      float previous;
      int flicker;
      int contact;
      int contactTicks;
      long seen;
   }
}
