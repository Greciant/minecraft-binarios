package com.awakenedmod.awakened.client.music;

import com.awakenedmod.awakened.client.ClientConfig;
import java.util.ArrayDeque;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public abstract class BossTrack implements NotePattern.NoteSink {
   private static final double FULL_RANGE = 16.0;
   private static final float MIN_DUCK = 0.4F;
   private static final int RESCAN_TICKS = 10;
   private static final int MAX_RINGING = 24;
   private final double rangeSq;
   private final float gain;
   @Nullable
   private LivingEntity boss;
   private boolean active;
   private int rescanIn;
   private float volumeScale = 1.0F;
   private final ArrayDeque<SoundInstance> ringing = new ArrayDeque<>();

   protected BossTrack(double range) {
      this(range, 1.0F);
   }

   protected BossTrack(double range, float gain) {
      this.rangeSq = range * range;
      this.gain = gain;
   }

   protected abstract boolean matches(LivingEntity var1);

   protected abstract void play(@Nullable LivingEntity var1);

   protected abstract boolean finished();

   protected abstract void resetTrack();

   protected boolean holdsWithoutBoss() {
      return false;
   }

   final boolean playing() {
      return this.active && this.ownsSoundscape() && !this.finished();
   }

   protected boolean ownsSoundscape() {
      return true;
   }

   final void clientTick(Minecraft mc) {
      if (mc.level != null && mc.player != null && (Boolean)ClientConfig.BOSS_NOTE_MUSIC.get() && !mc.player.isDeadOrDying()) {
         if (this.boss != null && (this.boss.isRemoved() || this.boss.level() != mc.level || this.boss.distanceToSqr(mc.player) > this.rangeSq)) {
            this.boss = null;
         }

         if (this.boss == null && --this.rescanIn <= 0) {
            this.rescanIn = 10;
            this.boss = this.scan(mc);
         }

         if (this.boss != null) {
            this.active = true;
            this.volumeScale = this.duck(Math.sqrt(this.boss.distanceToSqr(mc.player)));
            if (!this.finished()) {
               this.play(this.boss);
            }
         } else {
            if (this.active && this.holdsWithoutBoss()) {
               this.play(null);
            } else {
               this.deactivate();
            }
         }
      } else {
         this.deactivate();
      }
   }

   private void deactivate() {
      if (this.active) {
         this.active = false;
         this.resetTrack();
      }

      this.boss = null;
      this.ringing.clear();
   }

   @Nullable
   private LivingEntity scan(Minecraft mc) {
      LivingEntity best = null;
      double bestSq = this.rangeSq;

      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)entity;
            if (!living.isRemoved() && this.matches(living)) {
               double sq = living.distanceToSqr(mc.player);
               if (sq <= bestSq) {
                  best = living;
                  bestSq = sq;
               }
            }
         }
      }

      return best;
   }

   private float duck(double distance) {
      double range = Math.sqrt(this.rangeSq);
      if (distance <= 16.0) {
         return 1.0F;
      } else {
         float t = (float)((distance - 16.0) / Math.max(1.0, range - 16.0));
         return 1.0F - 0.6F * Math.min(1.0F, t);
      }
   }

   @Override
   public final void note(Holder<SoundEvent> instrument, int semitone, float volume) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         SimpleSoundInstance sound = new SimpleSoundInstance(
            (SoundEvent)instrument.value(),
            SoundSource.RECORDS,
            volume * this.gain * this.volumeScale,
            Notes.pitch(semitone),
            SoundInstance.createUnseededRandom(),
            mc.player.getX(),
            mc.player.getY(),
            mc.player.getZ()
         );
         mc.getSoundManager().play(sound);
         this.ringing.addLast(sound);

         while (this.ringing.size() > 24) {
            this.ringing.removeFirst();
         }
      }
   }

   protected final void cutAll() {
      SoundManager sounds = Minecraft.getInstance().getSoundManager();

      for (SoundInstance sound : this.ringing) {
         sounds.stop(sound);
      }

      this.ringing.clear();
   }
}
