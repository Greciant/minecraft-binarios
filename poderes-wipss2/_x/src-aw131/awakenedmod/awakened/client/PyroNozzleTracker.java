package com.awakenedmod.awakened.client;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import org.joml.Vector3f;

public final class PyroNozzleTracker {
   public static final int LEFT_HAND = 0;
   public static final int RIGHT_HAND = 1;
   public static final int LEFT_FOOT = 2;
   public static final int RIGHT_FOOT = 3;
   public static final int SLOTS = 4;
   private static final int FRESH_TICKS = 2;
   private static final int PRUNE_TICKS = 40;
   private static final double SANITY_RANGE = 4.5;
   private static final int ALL_WRITTEN = 15;
   private static final Int2ObjectMap<PyroNozzleTracker.Entry> ENTRIES = new Int2ObjectOpenHashMap();
   private static final Vector3f SCRATCH = new Vector3f();
   private static int now;
   private static ClientLevel lastLevel;

   private PyroNozzleTracker() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(PyroNozzleTracker::onClientTick);
   }

   private static void onClientTick(Post event) {
      now++;
      ClientLevel level = Minecraft.getInstance().level;
      if (level != lastLevel) {
         ENTRIES.clear();
         lastLevel = level;
      }

      if (!ENTRIES.isEmpty()) {
         ENTRIES.values().removeIf(e -> now - e.tick > 40);
      }
   }

   public static void sample(int entityId, int slot, Pose pose) {
      Minecraft mc = Minecraft.getInstance();
      Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
      pose.pose().transformPosition(0.0F, 0.0F, 0.0F, SCRATCH);
      PyroNozzleTracker.Entry e = (PyroNozzleTracker.Entry)ENTRIES.get(entityId);
      if (e == null) {
         e = new PyroNozzleTracker.Entry();
         ENTRIES.put(entityId, e);
      }

      int o = slot * 3;
      e.points[o] = cam.x + (double)SCRATCH.x;
      e.points[o + 1] = cam.y + (double)SCRATCH.y;
      e.points[o + 2] = cam.z + (double)SCRATCH.z;
      e.written |= 1 << slot;
      e.tick = now;
      e.partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
   }

   public static boolean fresh(LivingEntity p) {
      PyroNozzleTracker.Entry e = (PyroNozzleTracker.Entry)ENTRIES.get(p.getId());
      if (e != null && e.written == 15 && now - e.tick <= 2) {
         double range = 4.5 * (double)p.getScale();
         double r2 = range * range;

         for (int slot = 0; slot < 4; slot++) {
            int o = slot * 3;
            if (p.distanceToSqr(e.points[o], e.points[o + 1], e.points[o + 2]) > r2) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static Vec3 point(int entityId, int slot) {
      PyroNozzleTracker.Entry e = (PyroNozzleTracker.Entry)ENTRIES.get(entityId);
      if (e == null) {
         return Vec3.ZERO;
      } else {
         int o = slot * 3;
         return new Vec3(e.points[o], e.points[o + 1], e.points[o + 2]);
      }
   }

   public static float partial(int entityId) {
      PyroNozzleTracker.Entry e = (PyroNozzleTracker.Entry)ENTRIES.get(entityId);
      return e == null ? 0.0F : e.partial;
   }

   private static final class Entry {
      final double[] points = new double[12];
      float partial;
      int tick = Integer.MIN_VALUE;
      int written;
   }
}
