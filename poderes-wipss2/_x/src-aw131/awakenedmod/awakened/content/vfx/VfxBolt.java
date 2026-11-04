package com.awakenedmod.awakened.content.vfx;

import com.awakenedmod.awakened.core.Fx;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(
   modid = "awakened"
)
public final class VfxBolt {
   private static final int HEAD_TICKS = 4;
   private static final int TRAIL_TICKS = 5;
   private static final List<VfxBolt.Mover> ACTIVE = new ArrayList<>();
   private static int lastTick = -1;

   private VfxBolt() {
   }

   public static void fire(ServerLevel level, Vec3 from, Supplier<Vec3> target, int travelTicks, VfxBolt.Look look, Consumer<Vec3> onArrive) {
      ACTIVE.add(new VfxBolt.Mover(level, from, target, travelTicks, look, onArrive));
   }

   public static void wave(ServerLevel level, Vec3 origin, Supplier<Vec3> target, int travelTicks, int color, Consumer<Vec3> onArrive) {
      Vec3 at = target.get();
      if (at != null) {
         blast(level, origin, Math.max(1.0, at.distanceTo(origin)), travelTicks, color);
         delay(level, origin, target, travelTicks, onArrive);
      }
   }

   public static void blast(ServerLevel level, Vec3 origin, double radius, int ticks, int color) {
      Vfx.ring(level, origin.add(0.0, 0.15, 0.0), radius, color, ticks + 2);
      Vfx.effect(VfxEntity.Kind.DOME, origin).scale(radius * 0.95).color(color).lifetime(ticks + 4).intensity(0.7F).spawn(level);
   }

   public static void delay(ServerLevel level, Vec3 from, Supplier<Vec3> target, int travelTicks, Consumer<Vec3> onArrive) {
      ACTIVE.add(new VfxBolt.Mover(level, from, target, travelTicks, null, onArrive));
   }

   public static Supplier<Vec3> chestOf(LivingEntity target, ServerLevel level, Vec3 from, double maxRange) {
      double maxSq = maxRange * maxRange;
      return () -> target != null && target.isAlive() && !target.isRemoved() && target.level() == level && !(target.position().distanceToSqr(from) > maxSq)
            ? target.position().add(0.0, (double)target.getBbHeight() * 0.55, 0.0)
            : null;
   }

   public static void tickAll(MinecraftServer server) {
      int now = server.getTickCount();
      if (now != lastTick && !ACTIVE.isEmpty()) {
         lastTick = now;
         List<VfxBolt.Mover> arrived = new ArrayList<>();
         ACTIVE.removeIf(moverx -> step(moverx, arrived));

         for (VfxBolt.Mover mover : arrived) {
            mover.onArrive.accept(mover.front);
         }
      }
   }

   private static boolean step(VfxBolt.Mover mover, List<VfxBolt.Mover> arrived) {
      Vec3 at = mover.target.get();
      if (at == null) {
         if (mover.look != null) {
            fizzle(mover.level, mover.front, mover.look);
         }

         return true;
      } else {
         mover.tick++;
         int left = mover.travelTicks - mover.tick + 1;
         Vec3 prev = mover.front;
         mover.front = left <= 1 ? at : prev.add(at.subtract(prev).scale(1.0 / (double)left));
         if (mover.look != null) {
            draw(mover.level, prev, mover.front, mover.look);
         }

         if (mover.tick >= mover.travelTicks) {
            arrived.add(mover);
            return true;
         } else {
            return false;
         }
      }
   }

   private static void draw(ServerLevel level, Vec3 prev, Vec3 now, VfxBolt.Look look) {
      Vfx.effect(VfxEntity.Kind.SPHERE, now).scale(look.radius()).color(look.color(), look.core()).lifetime(4).intensity(look.intensity()).spawn(level);
      Vec3 run = now.subtract(prev);
      if (run.lengthSqr() > 1.0E-4) {
         Vfx.effect(VfxEntity.Kind.BEAM, prev)
            .scale(look.trailHalfWidth())
            .secondary(run)
            .color(look.color(), look.core())
            .lifetime(5)
            .intensity(look.intensity() * 0.8F)
            .spawn(level);
      }

      if (look.motes() != null) {
         Fx.burst(level, look.motes(), now, 2, look.radius() * 0.5, 0.02);
      }
   }

   private static void fizzle(ServerLevel level, Vec3 at, VfxBolt.Look look) {
      Vfx.sphere(level, at, look.radius() * 1.6, look.color(), 5, 0.8F);
      Fx.burst(level, ParticleTypes.POOF, at, 6, 0.2, 0.02);
      Fx.sound(level, at, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4F, 1.6F);
   }

   @SubscribeEvent
   public static void onServerTick(Post event) {
      tickAll(event.getServer());
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      ACTIVE.clear();
      lastTick = -1;
   }

   public static record Look(int color, int core, double radius, double trailHalfWidth, float intensity, @Nullable ParticleOptions motes) {
      public static VfxBolt.Look of(int color, double radius) {
         return new VfxBolt.Look(color, 16777215, radius, radius * 0.35, 1.3F, null);
      }

      public VfxBolt.Look core(int hotCore) {
         return new VfxBolt.Look(this.color, hotCore, this.radius, this.trailHalfWidth, this.intensity, this.motes);
      }

      public VfxBolt.Look motes(ParticleOptions particle) {
         return new VfxBolt.Look(this.color, this.core, this.radius, this.trailHalfWidth, this.intensity, particle);
      }
   }

   private static final class Mover {
      final ServerLevel level;
      final Supplier<Vec3> target;
      final int travelTicks;
      @Nullable
      final VfxBolt.Look look;
      final Consumer<Vec3> onArrive;
      Vec3 front;
      int tick;

      Mover(ServerLevel level, Vec3 from, Supplier<Vec3> target, int travelTicks, @Nullable VfxBolt.Look look, Consumer<Vec3> onArrive) {
         this.level = level;
         this.front = from;
         this.target = target;
         this.travelTicks = Math.max(1, travelTicks);
         this.look = look;
         this.onArrive = onArrive;
      }
   }
}
