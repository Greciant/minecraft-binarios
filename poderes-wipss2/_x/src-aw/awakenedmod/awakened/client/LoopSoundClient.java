package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.net.LoopSoundPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.bus.api.IEventBus;

public final class LoopSoundClient {
   static final int LEASE_TICKS = 15;
   static final int FADE_TICKS = 6;
   static final float EASE = 0.3F;
   static final double BEAM_MIN_AHEAD = 4.0;
   static final float RETRIGGER_AT = 0.7F;
   private static final Map<String, LoopSoundClient.Bed> PLAYING = new HashMap<>();
   private static final List<LoopSoundClient.Bed> FADING = new ArrayList<>();

   private LoopSoundClient() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(e -> {
         if (!PLAYING.isEmpty()) {
            PLAYING.values().removeIf(LoopSoundClient.Bed::tick);
         }

         if (!FADING.isEmpty()) {
            FADING.removeIf(LoopSoundClient.Bed::tick);
         }
      });
      gameBus.addListener(e -> {
         PLAYING.clear();
         FADING.clear();
      });
   }

   public static void accept(LoopSoundPayload payload) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         String id = payload.entityId() + "|" + payload.key();
         LoopSoundClient.Bed bed = PLAYING.get(id);
         if (bed != null && bed.event.equals(payload.sound().value())) {
            bed.renew(payload);
         } else {
            if (bed != null) {
               bed.lease = 0;
               FADING.add(PLAYING.remove(id));
            }

            if (!payload.stop()) {
               PLAYING.put(id, new LoopSoundClient.Bed(mc.level, payload));
            }
         }
      }
   }

   private static final class Bed {
      private final ClientLevel level;
      private final SoundEvent event;
      private final SoundSource source;
      private final int entityId;
      private final int lengthTicks;
      private final float beamRange;
      private final List<LoopSoundClient.Voice> voices = new ArrayList<>(2);
      private double x;
      private double y;
      private double z;
      private float volume;
      private float pitch;
      private float targetVolume;
      private float targetPitch;
      private int lease;
      private float fadeStep;
      private float phase;

      Bed(ClientLevel level, LoopSoundPayload payload) {
         this.level = level;
         this.event = (SoundEvent)payload.sound().value();
         this.source = payload.source();
         this.entityId = payload.entityId();
         this.lengthTicks = payload.lengthTicks() <= 0 ? 0 : Math.max(10, payload.lengthTicks());
         this.beamRange = payload.beamRange();
         this.volume = payload.volume() > 1.0F ? payload.volume() : 0.02F;
         this.pitch = payload.pitch();
         this.renew(payload);
         this.follow();
      }

      void renew(LoopSoundPayload payload) {
         this.targetVolume = payload.volume();
         this.targetPitch = payload.pitch();
         this.lease = payload.stop() ? 0 : 15;
         this.fadeStep = 0.0F;
         if (this.entityId < 0) {
            this.x = payload.x();
            this.y = payload.y();
            this.z = payload.z();
         }
      }

      private boolean follow() {
         if (this.entityId < 0) {
            return true;
         } else {
            Entity entity = this.level.getEntity(this.entityId);
            if (entity == null || entity.isRemoved()) {
               return false;
            } else if (this.beamRange > 0.0F) {
               this.onBeam(entity);
               return true;
            } else {
               this.x = entity.getX();
               this.y = entity.getY(0.5);
               this.z = entity.getZ();
               return true;
            }
         }
      }

      private void onBeam(Entity entity) {
         Vec3 start = entity.getEyePosition();
         Vec3 dir = entity.getViewVector(1.0F).normalize();
         Vec3 far = start.add(dir.scale((double)this.beamRange));
         HitResult hit = this.level.clip(new ClipContext(start, far, Block.COLLIDER, Fluid.NONE, entity));
         double length = hit.getType() == Type.MISS ? (double)this.beamRange : hit.getLocation().distanceTo(start);
         Vec3 listener = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
         double along = Math.min(length, Math.max(Math.min(4.0, length), listener.subtract(start).dot(dir)));
         Vec3 at = start.add(dir.scale(along));
         this.x = at.x;
         this.y = at.y;
         this.z = at.z;
      }

      boolean tick() {
         Minecraft mc = Minecraft.getInstance();
         this.voices.removeIf(v -> v.isStopped() || !mc.getSoundManager().isActive(v));
         if (mc.level != this.level) {
            this.end();
            return true;
         } else {
            if (!this.follow()) {
               this.lease = 0;
            }

            if (this.lease > 0) {
               this.lease--;
               this.volume = this.volume + (this.targetVolume - this.volume) * 0.3F;
               this.pitch = this.pitch + (this.targetPitch - this.pitch) * 0.3F;
            } else {
               if (this.fadeStep == 0.0F) {
                  this.fadeStep = Math.max(this.volume / 6.0F, 0.01F);
               }

               this.volume = this.volume - this.fadeStep;
               if (this.volume <= 0.01F) {
                  this.end();
                  return true;
               }
            }

            if (this.lengthTicks > 0) {
               this.phase = this.phase + Math.max(0.5F, Math.min(2.0F, this.pitch)) / (float)this.lengthTicks;
            }

            if (this.lease > 0 && (this.voices.isEmpty() || this.lengthTicks > 0 && this.phase >= 0.7F)) {
               LoopSoundClient.Voice voice = new LoopSoundClient.Voice(this);
               mc.getSoundManager().play(voice);
               this.voices.add(voice);
               this.phase = 0.0F;
            }

            return false;
         }
      }

      private void end() {
         this.voices.forEach(LoopSoundClient.Voice::end);
         this.voices.clear();
      }
   }

   private static final class Voice extends AbstractTickableSoundInstance {
      private final LoopSoundClient.Bed bed;

      Voice(LoopSoundClient.Bed bed) {
         super(bed.event, bed.source, SoundInstance.createUnseededRandom());
         this.bed = bed;
         this.looping = bed.lengthTicks == 0;
         this.delay = 0;
         this.follow();
      }

      private void follow() {
         this.volume = this.bed.volume;
         this.pitch = this.bed.pitch;
         this.x = this.bed.x;
         this.y = this.bed.y;
         this.z = this.bed.z;
      }

      void end() {
         this.stop();
      }

      public void tick() {
         this.follow();
      }
   }
}
