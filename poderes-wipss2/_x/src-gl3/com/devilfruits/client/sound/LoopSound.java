package com.devilfruits.client.sound;

import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class LoopSound extends AbstractTickableSoundInstance {
   @Nullable
   private final Entity follow;
   private final BooleanSupplier alive;
   private final float targetVolume;
   private final int maxTicks;
   private int ticks;
   private float fade;

   public LoopSound(SoundEvent sound, @Nullable Entity follow, Vec3 pos, float volume, float pitch, int maxTicks, BooleanSupplier alive) {
      super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
      this.follow = follow;
      this.alive = alive;
      this.targetVolume = volume;
      this.maxTicks = maxTicks;
      this.looping = true;
      this.delay = 0;
      this.volume = 0.01F;
      this.pitch = pitch;
      this.x = pos.x;
      this.y = pos.y;
      this.z = pos.z;
   }

   public void tick() {
      this.ticks++;
      boolean on = this.alive.getAsBoolean() && (this.maxTicks <= 0 || this.ticks < this.maxTicks) && (this.follow == null || this.follow.isAlive());
      this.fade = on ? Math.min(1.0F, this.fade + 0.1F) : this.fade - 0.08F;
      if (this.fade <= 0.0F && !on) {
         this.stop();
      } else {
         this.volume = Math.max(0.01F, this.targetVolume * this.fade);
         if (this.follow != null) {
            this.x = this.follow.getX();
            this.y = this.follow.getY() + 1.0;
            this.z = this.follow.getZ();
         }
      }
   }

   public boolean canStartSilent() {
      return true;
   }
}
