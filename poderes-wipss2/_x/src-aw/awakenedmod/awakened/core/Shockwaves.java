package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.content.vfx.Vfx;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

@EventBusSubscriber(
   modid = "awakened"
)
public final class Shockwaves {
   private static final int MAX_POPS = 30;
   private static final List<Shockwaves.Wave> ACTIVE = new ArrayList<>();
   private static final int DEFAULT_RING_COLOR = 16756768;

   private Shockwaves() {
   }

   static void start(ServerLevel level, Vec3 center, double startRadius, double endRadius, int travelTicks, boolean debrisBlocks) {
      start(level, center, startRadius, endRadius, travelTicks, Shockwaves.Style.DEFAULT.withDebrisBlocks(debrisBlocks));
   }

   static void start(ServerLevel level, Vec3 center, double startRadius, double endRadius, int travelTicks, boolean debrisBlocks, int color) {
      start(level, center, startRadius, endRadius, travelTicks, Shockwaves.Style.DEFAULT.withDebrisBlocks(debrisBlocks).withColor(color));
   }

   static void start(ServerLevel level, Vec3 center, double startRadius, double endRadius, int travelTicks, Shockwaves.Style style) {
      Shockwaves.Wave wave = new Shockwaves.Wave(level, center, startRadius, endRadius, travelTicks, style);
      ACTIVE.add(wave);
      if (style.glowRing()) {
         Vfx.ring(level, center.add(0.0, 0.15, 0.0), endRadius, style.ringColor(), wave.travelTicks);
      }

      if (style.boom()) {
         Fx.sound(level, center, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8F, 0.5F);
      }
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      ACTIVE.clear();
   }

   @SubscribeEvent
   public static void onServerTick(Post event) {
      if (!ACTIVE.isEmpty()) {
         Iterator<Shockwaves.Wave> it = ACTIVE.iterator();

         while (it.hasNext()) {
            Shockwaves.Wave wave = it.next();
            if (wave.level.getServer() != event.getServer()) {
               it.remove();
            } else {
               tick(wave);
               if (++wave.age > wave.travelTicks) {
                  it.remove();
               }
            }
         }
      }
   }

   private static void tick(Shockwaves.Wave wave) {
      ServerLevel level = wave.level;
      Vec3 c = wave.center;
      double r = wave.startR + (wave.endR - wave.startR) * (double)wave.age / (double)wave.travelTicks;
      if (wave.age % 2 == 0) {
         int points = (int)Math.max(10.0, Math.min(16.0, r * 2.5));

         for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2) * (double)i / (double)points;
            double x = c.x + Math.cos(a) * r;
            double z = c.z + Math.sin(a) * r;
            Fx.Floor floor = Fx.floorState(level, x, c.y, z);
            if (floor != null) {
               level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor.state()), x, floor.topY() + 0.1, z, 3, 0.1, 0.3, 0.1, 0.18);
            }
         }
      }

      if (wave.age % 3 == 0) {
         if (wave.style.debrisBlocks() && wave.popsSpawned < 30) {
            for (int ix = 0; ix < 6 && wave.popsSpawned < 30; ix++) {
               double a = level.random.nextDouble() * Math.PI * 2.0;
               double x = c.x + Math.cos(a) * r;
               double z = c.z + Math.sin(a) * r;
               Fx.Floor floor = Fx.floorState(level, x, c.y, z);
               if (floor != null) {
                  Vfx.block(level, new Vec3(x, floor.topY() + 0.25, z), floor.state().getBlock(), 0.55, 0.55, 0.55, 12, level.random.nextFloat() * 360.0F);
                  wave.popsSpawned++;
               }
            }
         }

         if (wave.style.rumble()) {
            float vol = 0.6F + 0.8F * (float)(1.0 - r / wave.endR);
            double a = level.random.nextDouble() * Math.PI * 2.0;

            for (int ixx = 0; ixx < 2; ixx++) {
               Vec3 p = c.add(Math.cos(a + (double)ixx * Math.PI) * r, 0.0, Math.sin(a + (double)ixx * Math.PI) * r);
               Fx.sound(level, p, SoundEvents.DEEPSLATE_BREAK, SoundSource.HOSTILE, vol, 0.5F);
            }
         }

         if (wave.style.shake()) {
            Fx.shake(level, c, r + 6.0, 0.12F, 0, 8);
         }
      }
   }

   public static record Style(int ringColor, boolean glowRing, boolean debrisBlocks, boolean boom, boolean rumble, boolean shake) {
      public static final Shockwaves.Style DEFAULT = new Shockwaves.Style(16756768, true, false, true, true, true);

      public Shockwaves.Style withColor(int rgb) {
         return new Shockwaves.Style(rgb, this.glowRing, this.debrisBlocks, this.boom, this.rumble, this.shake);
      }

      public Shockwaves.Style withDebrisBlocks(boolean on) {
         return new Shockwaves.Style(this.ringColor, this.glowRing, on, this.boom, this.rumble, this.shake);
      }

      public Shockwaves.Style silent() {
         return new Shockwaves.Style(this.ringColor, this.glowRing, this.debrisBlocks, false, false, this.shake);
      }

      public Shockwaves.Style noBoom() {
         return new Shockwaves.Style(this.ringColor, this.glowRing, this.debrisBlocks, false, this.rumble, this.shake);
      }

      public Shockwaves.Style noGlowRing() {
         return new Shockwaves.Style(this.ringColor, false, this.debrisBlocks, this.boom, this.rumble, this.shake);
      }

      public Shockwaves.Style noShake() {
         return new Shockwaves.Style(this.ringColor, this.glowRing, this.debrisBlocks, this.boom, this.rumble, false);
      }
   }

   private static final class Wave {
      final ServerLevel level;
      final Vec3 center;
      final double startR;
      final double endR;
      final int travelTicks;
      final Shockwaves.Style style;
      int age;
      int popsSpawned;

      Wave(ServerLevel level, Vec3 center, double startR, double endR, int travelTicks, Shockwaves.Style style) {
         this.level = level;
         this.center = center;
         this.startR = startR;
         this.endR = endR;
         this.travelTicks = Math.max(1, travelTicks);
         this.style = style;
      }
   }
}
