package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.core.Terrain;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;

public final class WaterCubeSpray {
   private static final int MAX_CATCH_UP = 4;
   private static final int ONCE_MEMORY = 200;
   private static final int LOOK_DOWN = 16;
   private static final Map<Object, WaterCubeSpray.Entry> POOLS = new HashMap<>();
   private static final Map<Object, Long> ONCE = new HashMap<>();
   private static boolean installed;

   private WaterCubeSpray() {
   }

   public static void install(IEventBus gameBus) {
      if (!installed) {
         installed = true;
         WaterFramePass.register(gameBus, (level, player, pt, ps, buffers, cam) -> {
            if (player == Minecraft.getInstance().player) {
               long now = level.getGameTime();
               if (!ONCE.isEmpty()) {
                  ONCE.values().removeIf(t -> now - t > 200L || now < t);
               }

               Iterator<WaterCubeSpray.Entry> it = POOLS.values().iterator();

               while (it.hasNext()) {
                  WaterCubeSpray.Entry e = it.next();
                  if (e.level != level) {
                     it.remove();
                  } else {
                     int steps = (int)Mth.clamp(now - e.tick, 0L, 4L);
                     e.tick = now;

                     for (int i = 0; i < steps; i++) {
                        e.pool.tick();
                     }

                     if (e.pool.isEmpty()) {
                        it.remove();
                     } else {
                        WaterMesh.Paint water = WaterMesh.Paint.of(e.look.rgb(), e.look.alpha(), 15728880);
                        e.pool.draw(ps, buffers, cam, pt, water, water.foam(e.look.foamAlpha()));
                     }
                  }
               }
            }
         });
      }
   }

   public static WaterCubePool pool(ClientLevel level, Object owner, WaterCubeSpray.Look look) {
      WaterCubeSpray.Entry e = POOLS.get(owner);
      if (e == null || e.level != level) {
         e = new WaterCubeSpray.Entry();
         e.level = level;
         e.tick = level.getGameTime();
         POOLS.put(owner, e);
      }

      e.look = look;
      return e.pool;
   }

   public static void burst(ClientLevel level, Object owner, Vec3 at, Vec3 drift, WaterCubeSpray.Burst b, WaterCubeSpray.Look look) {
      WaterCubePool pool = pool(level, owner, look);
      RandomSource rnd = level.getRandom();
      double ground = Terrain.floorNear(level, at.x, at.z, at.y, 1, 16, true);

      for (int i = 0; i < b.count(); i++) {
         float a = rnd.nextFloat() * (float) (Math.PI * 2);
         float out = 0.4F + 0.6F * rnd.nextFloat();
         float s = b.scatter() * rnd.nextFloat();
         Vec3 pos = at.add((double)(Mth.cos(a) * s), 0.0, (double)(Mth.sin(a) * s));
         Vec3 vel = drift.add(
            (double)(Mth.cos(a) * b.speed() * out), (double)(b.lift() * (0.5F + 0.5F * rnd.nextFloat())), (double)(Mth.sin(a) * b.speed() * out)
         );
         float size = b.size() * (1.0F + b.sizeJitter() * (2.0F * rnd.nextFloat() - 1.0F));
         float yaw = rnd.nextFloat() * 360.0F;
         float spin = b.spin() * (2.0F * rnd.nextFloat() - 1.0F);
         int life = Math.max(4, Math.round((float)b.life() * (0.75F + 0.5F * rnd.nextFloat())));
         pool.add(level, pos, vel, yaw, 0.0F, spin, size, life, rnd.nextFloat() < b.foamShare(), ground);
      }
   }

   public static boolean burstOnce(ClientLevel level, Object key, Vec3 at, Vec3 drift, WaterCubeSpray.Burst b, WaterCubeSpray.Look look) {
      if (ONCE.putIfAbsent(key, level.getGameTime()) != null) {
         return false;
      } else {
         burst(level, key, at, drift, b, look);
         return true;
      }
   }

   public static void clear(Object owner) {
      POOLS.remove(owner);
   }

   public static record Burst(int count, float scatter, float speed, float lift, float size, float sizeJitter, int life, float spin, float foamShare) {
      public static WaterCubeSpray.Burst of(int count, float speed, float size) {
         return new WaterCubeSpray.Burst(count, 0.3F, speed, speed * 1.6F, size, 0.35F, 22, 14.0F, 0.35F);
      }

      public WaterCubeSpray.Burst scattered(float s) {
         return new WaterCubeSpray.Burst(this.count, s, this.speed, this.lift, this.size, this.sizeJitter, this.life, this.spin, this.foamShare);
      }

      public WaterCubeSpray.Burst lifted(float l) {
         return new WaterCubeSpray.Burst(this.count, this.scatter, this.speed, l, this.size, this.sizeJitter, this.life, this.spin, this.foamShare);
      }

      public WaterCubeSpray.Burst lasting(int ticks) {
         return new WaterCubeSpray.Burst(this.count, this.scatter, this.speed, this.lift, this.size, this.sizeJitter, ticks, this.spin, this.foamShare);
      }

      public WaterCubeSpray.Burst spinning(float degPerTick) {
         return new WaterCubeSpray.Burst(this.count, this.scatter, this.speed, this.lift, this.size, this.sizeJitter, this.life, degPerTick, this.foamShare);
      }

      public WaterCubeSpray.Burst foamed(float share) {
         return new WaterCubeSpray.Burst(this.count, this.scatter, this.speed, this.lift, this.size, this.sizeJitter, this.life, this.spin, share);
      }
   }

   private static final class Entry {
      final WaterCubePool pool = new WaterCubePool();
      ClientLevel level;
      WaterCubeSpray.Look look;
      long tick;
   }

   public static record Look(int rgb, float alpha, float foamAlpha) {
   }
}
