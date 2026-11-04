package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.TickFreeze;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class TimelessColdBodies {
   public static final int FADE_TICKS = 4;
   public static final float MOTES_PER_SECOND = 2.5F;
   private static final double MOTE_SPREAD = 0.35;
   private static final double MOTE_DRIFT = 0.012;
   private static final float MOTE_SIZE = 0.42F;
   private static final int MOTE_GREY = 10465992;
   private static final float MOTE_TINT = 0.3F;
   private static final int MOTE_COLOURED_ONE_IN = 4;
   private static final Map<Integer, TimelessColdBodies.Body> BODIES = new ConcurrentHashMap<>();
   private static final RandomSource RANDOM = RandomSource.create();
   private static int ticks;

   private TimelessColdBodies() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(TimelessColdBodies::onClientTick);
   }

   private static void onClientTick(Post event) {
      tick(Minecraft.getInstance());
   }

   static void tick(Minecraft mc) {
      if (mc.level != null && !mc.isPaused()) {
         ticks++;

         for (TimelessColdBodies.Body body : BODIES.values()) {
            body.seen = false;
         }

         float step = 0.25F;

         for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity) {
               int divisor = TickFreeze.divisorOf(entity);
               boolean cold = divisor > 1 && entity.isAlive() && !entity.isRemoved();
               TimelessColdBodies.Body body = BODIES.get(entity.getId());
               if (cold) {
                  if (body == null) {
                     body = new TimelessColdBodies.Body();
                     BODIES.put(entity.getId(), body);
                  }

                  body.seen = true;
                  body.divisor = divisor;
                  TimelessFieldClient.Field field = TimelessFieldClient.fieldOf(entity);
                  if (field != null) {
                     body.bodyColor = field.palette().body();
                  } else if (body.bodyColor == 0) {
                     body.bodyColor = VelocityPalette.DEFAULT.body();
                  }

                  body.prevIntensity = body.intensity;
                  body.intensity = Math.min(1.0F, body.intensity + step);
                  motes(mc, entity, body);
               } else if (body != null) {
                  body.seen = true;
                  body.prevIntensity = body.intensity;
                  body.intensity -= step;
                  if (body.intensity <= 0.0F) {
                     BODIES.remove(entity.getId());
                  }
               }
            }
         }

         Iterator<TimelessColdBodies.Body> it = BODIES.values().iterator();

         while (it.hasNext()) {
            if (!it.next().seen) {
               it.remove();
            }
         }
      } else {
         if (mc.level == null) {
            BODIES.clear();
         }
      }
   }

   private static void motes(Minecraft mc, Entity entity, TimelessColdBodies.Body body) {
      if (!(RANDOM.nextFloat() * 20.0F >= 2.5F * body.intensity)) {
         AABB box = entity.getBoundingBox().inflate(0.35);
         double x = Mth.lerp(RANDOM.nextDouble(), box.minX, box.maxX);
         double y = Mth.lerp(RANDOM.nextDouble(), box.minY, box.maxY);
         double z = Mth.lerp(RANDOM.nextDouble(), box.minZ, box.maxZ);
         int colour = RANDOM.nextInt(4) == 0 ? body.bodyColor : VelocityPalette.mix(10465992, body.bodyColor, 0.3F);
         mc.level
            .addParticle(Fx.dust(colour, 0.42F), x, y, z, (RANDOM.nextDouble() - 0.5) * 0.012, RANDOM.nextDouble() * 0.012, (RANDOM.nextDouble() - 0.5) * 0.012);
      }
   }

   public static TimelessColdBodies.Cold of(int entityId) {
      TimelessColdBodies.Body body = BODIES.get(entityId);
      if (body == null) {
         return null;
      } else {
         float intensity = Mth.lerp(partialTick(), body.prevIntensity, body.intensity);
         return intensity <= 0.0F ? null : new TimelessColdBodies.Cold(intensity, body.divisor, body.bodyColor, entityId);
      }
   }

   public static TimelessColdBodies.Cold of(Entity entity) {
      return of(entity.getId());
   }

   public static float clock() {
      return (float)ticks + partialTick();
   }

   private static float partialTick() {
      Minecraft mc = Minecraft.getInstance();
      return mc.getTimer().getGameTimeDeltaPartialTick(false);
   }

   private static final class Body {
      float intensity;
      float prevIntensity;
      int divisor;
      int bodyColor;
      boolean seen;
   }

   public static record Cold(float intensity, int divisor, int bodyColor, int seed) {
   }
}
