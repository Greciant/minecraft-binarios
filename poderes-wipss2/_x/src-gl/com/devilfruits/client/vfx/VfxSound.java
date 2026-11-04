package com.devilfruits.client.vfx;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class VfxSound extends AbstractTickableSoundInstance {
   @Nullable
   private final Entity follow;
   @Nullable
   private Supplier<Vec3> track;
   private final VfxSound.Curve volumeCurve;
   private final VfxSound.Curve pitchCurve;
   private final int maxAge;
   private final BooleanSupplier alive;
   private int age;

   private VfxSound(
      SoundEvent event, @Nullable Entity follow, Vec3 pos, boolean loop, VfxSound.Curve volume, VfxSound.Curve pitch, int maxAge, BooleanSupplier alive
   ) {
      super(event, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
      this.follow = follow;
      this.volumeCurve = volume;
      this.pitchCurve = pitch;
      this.maxAge = maxAge;
      this.alive = alive;
      this.looping = loop;
      this.delay = 0;
      this.x = pos.x;
      this.y = pos.y;
      this.z = pos.z;
      this.volume = Math.max(0.001F, volume.at(0));
      this.pitch = pitch.at(0);
   }

   public static VfxSound tracking(
      SoundEvent event, Supplier<Vec3> track, boolean loop, VfxSound.Curve volume, VfxSound.Curve pitch, int maxAge, BooleanSupplier alive
   ) {
      VfxSound s = new VfxSound(event, null, track.get(), loop, volume, pitch, maxAge, alive);
      s.track = track;
      Minecraft.getInstance().getSoundManager().play(s);
      return s;
   }

   public static VfxSound play(
      SoundEvent event, @Nullable Entity follow, Vec3 pos, boolean loop, VfxSound.Curve volume, VfxSound.Curve pitch, int maxAge, BooleanSupplier alive
   ) {
      VfxSound s = new VfxSound(event, follow, pos, loop, volume, pitch, maxAge, alive);
      Minecraft.getInstance().getSoundManager().play(s);
      return s;
   }

   public void tick() {
      this.age++;
      if (this.age <= this.maxAge && this.alive.getAsBoolean()) {
         if (this.follow != null && this.follow.isAlive()) {
            this.x = this.follow.getX();
            this.y = this.follow.getY() + (double)this.follow.getBbHeight() * 0.5;
            this.z = this.follow.getZ();
         } else if (this.track != null) {
            Vec3 p = this.track.get();
            this.x = p.x;
            this.y = p.y;
            this.z = p.z;
         }

         this.volume = Math.max(0.001F, this.volumeCurve.at(this.age));
         this.pitch = Math.max(0.5F, Math.min(2.0F, this.pitchCurve.at(this.age)));
      } else {
         this.stop();
      }
   }

   public void end() {
      this.stop();
   }

   @FunctionalInterface
   public interface Curve {
      float at(int var1);
   }
}
