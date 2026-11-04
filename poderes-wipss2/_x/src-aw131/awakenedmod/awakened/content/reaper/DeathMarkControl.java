package com.awakenedmod.awakened.content.reaper;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class DeathMarkControl {
   private static final int BRAND_INTERVAL = 4;
   private static final double SQRT_CAP = Math.sqrt(5000.0);
   private static final Map<LivingEntity, DeathMarkControl.Mark> MARKED = new ConcurrentHashMap<>();
   private static int lastTick = -1;

   private DeathMarkControl() {
   }

   public static void mark(ServerPlayer caster, LivingEntity target) {
      markFor(caster.getUUID(), target, caster.level().getGameTime(), ReaperAttachments.souls(caster).souls());
   }

   public static void markFor(UUID caster, LivingEntity target, long now, long souls) {
      if (target.isAlive() && !target.isRemoved()) {
         DeathMarkControl.Mark m = MARKED.computeIfAbsent(target, t -> new DeathMarkControl.Mark());
         m.caster = caster;
         m.expiry = now + (long)durationTicks(souls);
      }
   }

   public static int durationTicks(long souls) {
      double seconds = 12.0 + 8.0 * (Math.sqrt((double)Math.min(souls, 5000L)) / SQRT_CAP);
      return (int)Math.round(seconds * 20.0);
   }

   public static boolean isMarked(LivingEntity victim) {
      return MARKED.containsKey(victim);
   }

   public static UUID markerOf(LivingEntity victim) {
      DeathMarkControl.Mark m = MARKED.get(victim);
      return m == null ? null : m.caster;
   }

   public static void clear(LivingEntity victim) {
      MARKED.remove(victim);
   }

   public static void clearAllBy(UUID caster) {
      MARKED.values().removeIf(m -> caster.equals(m.caster));
   }

   public static void tickAll(MinecraftServer server) {
      int now = server.getTickCount();
      if (now != lastTick) {
         lastTick = now;
         Iterator<Entry<LivingEntity, DeathMarkControl.Mark>> it = MARKED.entrySet().iterator();

         while (it.hasNext()) {
            Entry<LivingEntity, DeathMarkControl.Mark> e = it.next();
            LivingEntity victim = e.getKey();
            DeathMarkControl.Mark m = e.getValue();
            if (!victim.isAlive() || victim.isRemoved() || !(victim.level() instanceof ServerLevel level) || level.getGameTime() >= m.expiry) {
               it.remove();
            } else if (now % 4 == 0) {
               Vec3 chest = victim.position().add(0.0, (double)victim.getBbHeight() * 0.6, 0.0);
               ReaperFx.deathMarkBrand(level, chest, victim.getYRot());
            }
         }
      }
   }

   private static final class Mark {
      private UUID caster;
      private long expiry;
   }
}
