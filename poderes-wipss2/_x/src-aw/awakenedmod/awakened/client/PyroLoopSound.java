package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.pyrokinetic.FlameTone;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;

public final class PyroLoopSound {
   private static final float EASE = 0.3F;
   private final IntPredicate lit;
   private final PyroLoopSound.Layer[] layers;
   private final ToDoubleFunction<LivingEntity> throttle;
   private final boolean bystanders;
   private final PyroLoopSound.Texture texture;
   private final Map<Integer, PyroLoopSound.Instance[]> playing = new HashMap<>();

   private PyroLoopSound(
      IntPredicate lit, ToDoubleFunction<LivingEntity> throttle, boolean bystanders, PyroLoopSound.Texture texture, PyroLoopSound.Layer[] layers
   ) {
      this.lit = lit;
      this.throttle = throttle;
      this.bystanders = bystanders;
      this.texture = texture;
      this.layers = layers;
   }

   public static void register(IEventBus gameBus, ResourceLocation trigger, PyroLoopSound.Layer... layers) {
      register(gameBus, trigger, null, false, null, layers);
   }

   public static void register(
      IEventBus gameBus,
      ResourceLocation trigger,
      ToDoubleFunction<LivingEntity> throttle,
      boolean bystanders,
      PyroLoopSound.Texture texture,
      PyroLoopSound.Layer... layers
   ) {
      register(gameBus, (IntPredicate)(id -> AwakenedAnimations.isPlaying(id, trigger)), throttle, bystanders, texture, layers);
   }

   public static void register(
      IEventBus gameBus,
      IntPredicate lit,
      ToDoubleFunction<LivingEntity> throttle,
      boolean bystanders,
      PyroLoopSound.Texture texture,
      PyroLoopSound.Layer... layers
   ) {
      PyroLoopSound loop = new PyroLoopSound(lit, throttle, bystanders, texture, layers);
      gameBus.addListener(event -> loop.onClientTick());
   }

   public static void oneShot(LivingEntity rider, SoundEvent sound, float volume, float pitch, FlameTone tone) {
      Vec3 at = rider.getEyePosition();
      Minecraft.getInstance()
         .getSoundManager()
         .play(new SimpleSoundInstance(sound, sourceOf(rider), volume, pitch * tone.soundPitch(), SoundInstance.createUnseededRandom(), at.x, at.y, at.z));
   }

   private void onClientTick() {
      Minecraft mc = Minecraft.getInstance();
      this.playing.values().removeIf(set -> {
         for (PyroLoopSound.Instance i : set) {
            if (!i.isStopped()) {
               return false;
            }
         }

         return true;
      });
      if (mc.level != null && mc.player != null) {
         ClipBearers.forEachCaster(mc.level, player -> this.ride(mc, player));
      }
   }

   private void ride(Minecraft mc, LivingEntity player) {
      boolean local = player == mc.player;
      if (local || this.bystanders) {
         if (this.lit.test(player.getId())) {
            PyroLoopSound.Instance[] set = this.playing.get(player.getId());
            if (set == null) {
               set = new PyroLoopSound.Instance[this.layers.length];
               this.playing.put(player.getId(), set);
            }

            for (int n = 0; n < this.layers.length; n++) {
               if (set[n] == null || set[n].isStopped()) {
                  set[n] = new PyroLoopSound.Instance(this, this.layers[n], player, local);
                  mc.getSoundManager().play(set[n]);
               }
            }

            if (this.texture != null) {
               this.texture.tick(player, this.throttleOf(player), FlameTone.forHeat(PyroClientHeat.heat(player)));
            }
         }
      }
   }

   private static SoundSource sourceOf(LivingEntity rider) {
      return rider instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
   }

   private float throttleOf(LivingEntity player) {
      return this.throttle == null ? 1.0F : (float)this.throttle.applyAsDouble(player);
   }

   private static final class Instance extends AbstractTickableSoundInstance {
      private final PyroLoopSound owner;
      private final PyroLoopSound.Layer layer;
      private final LivingEntity rider;
      private final boolean local;

      private Instance(PyroLoopSound owner, PyroLoopSound.Layer layer, LivingEntity rider, boolean local) {
         super(layer.sound().get(), PyroLoopSound.sourceOf(rider), SoundInstance.createUnseededRandom());
         this.owner = owner;
         this.layer = layer;
         this.rider = rider;
         this.local = local;
         this.looping = true;
         this.delay = 0;
         this.relative = local;
         this.follow();
         this.volume = 0.02F;
         this.pitch = layer.pitchAt(owner.throttleOf(rider)) * this.tone().soundPitch();
      }

      private void follow() {
         if (this.local) {
            this.x = 0.0;
            this.y = 0.0;
            this.z = 0.0;
         } else {
            this.x = this.rider.getX();
            this.y = this.rider.getY();
            this.z = this.rider.getZ();
         }
      }

      private FlameTone tone() {
         return FlameTone.forHeat(PyroClientHeat.heat(this.rider));
      }

      public void tick() {
         Minecraft mc = Minecraft.getInstance();
         if (!this.rider.isRemoved() && (!this.local || mc.player == this.rider) && mc.level == this.rider.level()) {
            this.follow();
            boolean on = this.owner.lit.test(this.rider.getId());
            float throttle = this.owner.throttleOf(this.rider);
            FlameTone tone = this.tone();
            float targetPitch = this.layer.pitchAt(throttle) * tone.soundPitch();
            this.pitch = this.pitch + (targetPitch - this.pitch) * 0.3F;
            if (on) {
               this.volume = this.volume + (this.layer.volumeAt(throttle, tone) - this.volume) * 0.3F;
            } else {
               this.volume = this.volume - this.layer.volume() / (float)this.layer.fadeTicks();
               if (this.volume <= 0.01F) {
                  this.stop();
               }
            }
         } else {
            this.stop();
         }
      }
   }

   public static record Layer(
      Supplier<SoundEvent> sound, float volume, float basePitch, float pitchRise, float volumeFloor, float onset, boolean blueGated, int fadeTicks
   ) {
      public static final int DEFAULT_FADE_TICKS = 6;

      public Layer(SoundEvent sound, float volume, float basePitch) {
         this((Supplier<SoundEvent>)(() -> sound), volume, basePitch);
      }

      public Layer(Supplier<SoundEvent> sound, float volume, float basePitch) {
         this(sound, volume, basePitch, 0.0F, 1.0F);
      }

      public Layer(SoundEvent sound, float volume, float basePitch, float pitchRise, float volumeFloor) {
         this((Supplier<SoundEvent>)(() -> sound), volume, basePitch, pitchRise, volumeFloor);
      }

      public Layer(Supplier<SoundEvent> sound, float volume, float basePitch, float pitchRise, float volumeFloor) {
         this(sound, volume, basePitch, pitchRise, volumeFloor, 0.0F, false, 6);
      }

      public PyroLoopSound.Layer arrivesAt(float onset) {
         return new PyroLoopSound.Layer(
            this.sound, this.volume, this.basePitch, this.pitchRise, this.volumeFloor, Mth.clamp(onset, 0.0F, 0.99F), this.blueGated, this.fadeTicks
         );
      }

      public PyroLoopSound.Layer blueOnly() {
         return new PyroLoopSound.Layer(this.sound, this.volume, this.basePitch, this.pitchRise, this.volumeFloor, this.onset, true, this.fadeTicks);
      }

      public PyroLoopSound.Layer tail(int ticks) {
         return new PyroLoopSound.Layer(
            this.sound, this.volume, this.basePitch, this.pitchRise, this.volumeFloor, this.onset, this.blueGated, Math.max(1, ticks)
         );
      }

      private float rise(float throttle) {
         return this.onset <= 0.0F ? throttle : Mth.clamp((throttle - this.onset) / (1.0F - this.onset), 0.0F, 1.0F);
      }

      float pitchAt(float throttle) {
         return this.basePitch - this.pitchRise * (1.0F - this.rise(throttle));
      }

      float volumeAt(float throttle, FlameTone tone) {
         float t = this.rise(throttle);
         float ramp = this.onset > 0.0F && t <= 0.0F ? 0.0F : this.volumeFloor + (1.0F - this.volumeFloor) * t;
         return this.volume * ramp * (this.blueGated ? tone.blueFraction() : 1.0F);
      }
   }

   @FunctionalInterface
   public interface Texture {
      void tick(LivingEntity var1, float var2, FlameTone var3);
   }
}
