package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.content.pyrokinetic.FlameTone;
import com.awakenedmod.awakened.content.pyrokinetic.PyroBeamSounds;
import com.awakenedmod.awakened.core.AwId;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;

public final class PyroBeamSound {
   private static final int TEXTURE_WINDOW = 8;
   private static final int TEXTURE_CHANCE_PER_MILLE = 560;
   private static final int THROB_INTERVAL = 10;

   private PyroBeamSound() {
   }

   public static void register(IEventBus gameBus) {
      ResourceLocation trigger = AwId.of("flame_beam");
      PyroLoopSound.register(
         gameBus,
         trigger,
         PyroBeamClock::ramp,
         true,
         PyroBeamSound::texture,
         new PyroLoopSound.Layer(AwakenedSounds.FLAME_BEAM_LOOP, 0.36F, 1.0F, 0.12F, 0.55F).tail(10),
         new PyroLoopSound.Layer(AwakenedSounds.FLAME_BEAM_FULL, 0.28F, 0.95F, 0.0F, 0.0F).arrivesAt(0.55F).tail(14),
         new PyroLoopSound.Layer(AwakenedSounds.FLAME_BEAM_BLUE, 0.16F, 1.0F).blueOnly()
      );
   }

   private static void texture(LivingEntity rider, float ramp, FlameTone tone) {
      long age = PyroBeamClock.age(rider);
      if (age >= 0L) {
         float body = 0.75F + 0.25F * ramp;
         int h = mix((int)(age / 8L), rider.getId());
         boolean onTick = age % 8L == (long)((h & 0xFF) % 8);
         boolean speaks = (h >>> 8 & 1023) % 1000 < 560;
         if (onTick && speaks) {
            int voice = (h >>> 18) % 10;
            float jitter = (float)(h >>> 22 & 0xFF) / 255.0F;
            PyroBeamSounds.Cue cue = voice < 5 ? PyroBeamSounds.POP : (voice < 8 ? PyroBeamSounds.GOUT : PyroBeamSounds.SURGE);
            PyroLoopSound.oneShot(rider, cue.sound().get(), cue.volume() * body, cue.pitchAt(jitter), tone);
         }

         if (ramp >= 1.0F && age % 10L == 0L) {
            float jitter = (float)(mix((int)age, rider.getId()) >>> 4 & 0xFF) / 255.0F;
            PyroBeamSounds.Cue throb = PyroBeamSounds.THROB;
            PyroLoopSound.oneShot(rider, throb.sound().get(), throb.volume(), throb.pitchAt(jitter), tone);
         }
      }
   }

   private static int mix(int window, int salt) {
      int h = window * -1640531535 ^ salt * -2048144789;
      h ^= h >>> 15;
      h *= -1028477387;
      h ^= h >>> 13;
      return h & 2147483647;
   }
}
