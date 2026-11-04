package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class TimelessSound {
   public static final double HEAR_RANGE = 32.0;
   public static final int PULSE_TICKS = 40;
   private static final TimelessSound.Layer[] ENTRY = new TimelessSound.Layer[]{
      new TimelessSound.Layer(SoundEvents.WARDEN_HEARTBEAT, 1.4F, 0.5F),
      new TimelessSound.Layer(SoundEvents.FIRE_EXTINGUISH, 0.6F, 1.7F),
      new TimelessSound.Layer(SoundEvents.BEACON_DEACTIVATE, 0.55F, 0.6F)
   };
   private static final TimelessSound.Layer[] EXIT = new TimelessSound.Layer[]{
      new TimelessSound.Layer(SoundEvents.BEACON_ACTIVATE, 0.55F, 0.75F),
      new TimelessSound.Layer(SoundEvents.WARDEN_HEARTBEAT, 0.9F, 0.62F),
      new TimelessSound.Layer(SoundEvents.FIRE_EXTINGUISH, 0.35F, 1.3F)
   };
   private static final TimelessSound.Layer PULSE = new TimelessSound.Layer(SoundEvents.WARDEN_HEARTBEAT, 0.45F, 0.55F);
   private static final Map<Integer, Boolean> LAST = new HashMap<>();
   private static int ownerTicks = -1;

   private TimelessSound() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(TimelessSound::onClientTick);
   }

   private static void onClientTick(Post event) {
      tick(Minecraft.getInstance());
   }

   static void tick(Minecraft mc) {
      if (mc.level == null) {
         LAST.clear();
         ownerTicks = -1;
      } else if (!mc.isPaused() && mc.player != null) {
         Vec3 ear = mc.player.getEyePosition();

         for (Player p : mc.level.players()) {
            boolean on = VelocityVisualState.of(p).timeless();
            Boolean was = LAST.put(p.getId(), on);
            if (was != null && was != on && p.position().distanceToSqr(ear) <= 1024.0) {
               play(mc, p.position(), on ? ENTRY : EXIT);
            }

            if (p == mc.player) {
               ownerPulse(mc, p, on);
            }
         }

         Iterator<Integer> it = LAST.keySet().iterator();

         while (it.hasNext()) {
            if (mc.level.getEntity(it.next()) == null) {
               it.remove();
            }
         }
      }
   }

   private static void ownerPulse(Minecraft mc, Player owner, boolean on) {
      if (!on) {
         ownerTicks = -1;
      } else {
         ownerTicks++;
         if (ownerTicks > 0 && ownerTicks % 40 == 0) {
            play(mc, owner.position(), PULSE);
         }
      }
   }

   private static void play(Minecraft mc, Vec3 at, TimelessSound.Layer... layers) {
      for (TimelessSound.Layer layer : layers) {
         mc.getSoundManager().play(new TimelessSound.Moment(layer, at, mc.level.getRandom()));
      }
   }

   private static record Layer(SoundEvent sound, float volume, float pitch) {
   }

   private static final class Moment extends SimpleSoundInstance {
      Moment(TimelessSound.Layer layer, Vec3 at, RandomSource random) {
         super(layer.sound(), SoundSource.PLAYERS, layer.volume(), layer.pitch(), random, at.x, at.y, at.z);
      }
   }
}
