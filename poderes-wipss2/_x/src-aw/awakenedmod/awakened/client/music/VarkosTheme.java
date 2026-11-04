package com.awakenedmod.awakened.client.music;

import com.awakenedmod.awakened.content.entity.duelist.VarkosEntity;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;

public final class VarkosTheme extends BossTrack {
   private static final Holder<SoundEvent> LEAD = SoundEvents.NOTE_BLOCK_BIT;
   private static final Holder<SoundEvent> SPARK = SoundEvents.NOTE_BLOCK_PLING;
   private static final Holder<SoundEvent> BASS = SoundEvents.NOTE_BLOCK_BASS;
   private static final Holder<SoundEvent> SNARE = SoundEvents.NOTE_BLOCK_SNARE;
   private static final Holder<SoundEvent> HAT = SoundEvents.NOTE_BLOCK_HAT;
   private static final Holder<SoundEvent> BASEDRUM = SoundEvents.NOTE_BLOCK_BASEDRUM;
   private static final Holder<SoundEvent> BELL = SoundEvents.NOTE_BLOCK_BELL;
   private static final Holder<SoundEvent> CHIME = SoundEvents.NOTE_BLOCK_CHIME;
   private static final int BEAT = 8;
   private static final int BAR = 32;
   private static final int BARS = 24;
   private static final int LOOP_LEN = 768;
   private static final int B1 = 5;
   private static final int C2 = 6;
   private static final int D2 = 8;
   private static final int E2 = 10;
   private static final int A2 = 15;
   private static final int B2 = 17;
   private static final int A3 = 3;
   private static final int B3 = 5;
   private static final int D4 = 8;
   private static final int DS4 = 9;
   private static final int E4 = 10;
   private static final int FS4 = 12;
   private static final int G4 = 13;
   private static final int GS4 = 14;
   private static final int A4 = 15;
   private static final int B4 = 17;
   private static final int C5 = 18;
   private static final int D5 = 20;
   private static final int DS5 = 21;
   private static final int E5 = 22;
   private static final double RANGE = 48.0;
   private static final float GAIN = 1.25F;
   private static final int[] RIFF_TICKS = new int[]{0, 4, 6, 8, 16, 20, 24, 28};
   private static final int[] RIFF_STEPS = new int[]{0, 0, 3, 10, 8, 7, 5, 3};
   private static final float[] RIFF_VOLS = new float[]{0.75F, 0.6F, 0.6F, 0.8F, 0.7F, 0.6F, 0.65F, 0.6F};
   private static final int[] RIFF_ROOTS = new int[]{10, 10, 10, 10, 3, 3, 5, 5, 3, 3, 3, 3, 8, 8, 10, 10};
   private static final int[] BASS_ROOTS = new int[]{10, 10, 10, 10, 15, 15, 17, 17, 15, 15, 15, 15, 8, 8, 10, 10, 6, 8, 10, 10, 6, 8, 5, 5};
   private static final int[][] B_LEAD = new int[][]{
      {0, 13, 4, 17, 8, 18, 20, 20, 28, 18},
      {0, 17, 8, 15, 16, 12, 24, 15},
      {0, 22, 16, 20, 24, 17},
      {0, 18, 8, 17, 16, 15, 24, 13},
      {0, 13, 4, 17, 8, 18, 20, 20, 28, 22},
      {0, 20, 8, 15, 16, 17, 24, 18},
      {0, 17, 8, 12, 16, 9, 24, 12},
      {0, 17, 16, 10, 18, 12, 20, 13, 22, 15, 24, 17, 26, 18, 28, 20, 30, 21}
   };
   private static final int[] B_ECHO = new int[]{10, 13, 18, 15, 10, 17, 12, 12};
   private static final NotePattern[] PHASES = new NotePattern[]{buildPhase(1), buildPhase(2), buildPhase(3)};
   private static final NotePattern OUTRO = buildOutro();
   private int phase = 1;
   private NotePattern loop = PHASES[0];
   private int loopPos;
   private int loopCursor;
   private boolean iaiHolding;
   private boolean iaiStruck;
   private boolean resumeOnBeat;
   private boolean introSynced;
   private int outroPos = -1;
   private int outroCursor;
   private boolean finished;

   public VarkosTheme() {
      super(48.0, 1.25F);
   }

   @Override
   protected boolean matches(LivingEntity entity) {
      return entity instanceof VarkosEntity;
   }

   @Override
   protected boolean finished() {
      return this.finished;
   }

   @Override
   protected boolean holdsWithoutBoss() {
      return this.outroPos >= 0 && !this.finished;
   }

   @Override
   protected void resetTrack() {
      this.phase = 1;
      this.loop = PHASES[0];
      this.loopPos = 0;
      this.loopCursor = 0;
      this.iaiHolding = false;
      this.iaiStruck = false;
      this.resumeOnBeat = false;
      this.introSynced = false;
      this.outroPos = -1;
      this.outroCursor = 0;
      this.finished = false;
   }

   @Override
   protected void play(@Nullable LivingEntity boss) {
      VarkosEntity varkos = boss instanceof VarkosEntity v ? v : null;
      if (varkos != null && !varkos.isDeadOrDying() && varkos.deathTime <= 0) {
         int escalation = Math.max(1, Math.min(3, varkos.getEscalation()));
         if (escalation != this.phase) {
            this.switchPhase(escalation);
         }

         byte action = varkos.getAction();
         if (!this.introSynced) {
            if (action == 16 && varkos.getActionProgress() < 0.35F) {
               return;
            }

            this.introSynced = true;
         }

         if (action == 4) {
            if (!this.iaiHolding) {
               this.iaiHolding = true;
               this.iaiStruck = false;
               this.resumeOnBeat = true;
               this.cutAll();
            }

            if (!this.iaiStruck && varkos.getActionProgress() >= windupFraction(varkos)) {
               this.iaiStrike();
            }

            if (!this.iaiStruck) {
               this.advanceLoopSilently();
               return;
            }
         } else if (this.iaiHolding) {
            this.iaiHolding = false;
            if (!this.iaiStruck) {
               this.iaiStrike();
            }
         }

         if (this.resumeOnBeat) {
            if (this.loopPos % 8 != 0) {
               this.advanceLoopSilently();
               return;
            }

            this.resumeOnBeat = false;
            this.loopCursor = this.loop.cursorFor(this.loopPos);
         }

         this.tickLoop();
      } else {
         this.tickOutro();
      }
   }

   private void tickLoop() {
      this.loopCursor = this.loop.emit(this.loopPos, this.loopCursor, 1.0F, this);
      if (++this.loopPos >= 768) {
         this.loopPos = 0;
         this.loopCursor = 0;
      }
   }

   private void advanceLoopSilently() {
      if (++this.loopPos >= 768) {
         this.loopPos = 0;
      }
   }

   private void tickOutro() {
      if (!this.finished) {
         if (this.outroPos < 0) {
            this.outroPos = 0;
            this.outroCursor = 0;
         }

         this.outroCursor = OUTRO.emit(this.outroPos, this.outroCursor, 1.0F, this);
         if (++this.outroPos >= OUTRO.length()) {
            this.finished = true;
         }
      }
   }

   private void iaiStrike() {
      this.note(SPARK, 22, 1.0F);
      this.note(BELL, 22, 0.5F);
      this.iaiStruck = true;
   }

   private void switchPhase(int escalation) {
      this.phase = escalation;
      this.loop = PHASES[escalation - 1];
      this.loopCursor = this.loop.cursorFor(this.loopPos);
   }

   private static float windupFraction(VarkosEntity varkos) {
      int windup = varkos.getMoveWindup();
      int recover = varkos.getMoveRecover();
      return windup + recover > 0 ? (float)windup / (float)(windup + recover) : 2.0F;
   }

   private static NotePattern buildPhase(int tier) {
      int lift = tier >= 3 ? 2 : 0;
      NotePattern.Builder b = NotePattern.builder(768);

      for (int bar = 0; bar < 24; bar++) {
         int t = bar * 32;
         boolean seam = bar % 8 == 7;
         if (tier >= 3) {
            bassPumpBar(b, t, BASS_ROOTS[bar] + lift);
         } else {
            bassBar(b, t, BASS_ROOTS[bar] + lift);
         }

         kitBar(b, t, tier, seam);
         if (bar < 16) {
            riffBar(b, t, RIFF_ROOTS[bar] + lift, tier, seam);
         }
      }

      bSection(b, 512, tier, lift);
      return b.build();
   }

   private static void riffBar(NotePattern.Builder b, int t, int root, int tier, boolean seam) {
      boolean doubled = tier >= 3;

      for (int i = 0; i < RIFF_TICKS.length; i++) {
         lead(b, t + RIFF_TICKS[i], root + RIFF_STEPS[i], RIFF_VOLS[i], doubled);
      }

      if (tier >= 2) {
         b.add(t + 10, SPARK, octave(root), 0.4F);
         if (!seam) {
            b.add(t + 26, SPARK, fifth(root), 0.4F);
         }
      }
   }

   private static void bSection(NotePattern.Builder b, int start, int tier, int lift) {
      boolean doubled = tier >= 3;

      for (int i = 0; i < B_LEAD.length; i++) {
         int t = start + i * 32;
         int[] notes = B_LEAD[i];

         for (int j = 0; j < notes.length; j += 2) {
            lead(b, t + notes[j], notes[j + 1] + lift, 0.7F, doubled);
         }

         if (tier >= 2) {
            b.add(t + 2, SPARK, B_ECHO[i] + lift, 0.45F);
         }
      }
   }

   private static void lead(NotePattern.Builder b, int t, int note, float vol, boolean doubled) {
      b.add(t, LEAD, note, vol);
      if (doubled && t % 8 != 4) {
         b.add(t, LEAD, note >= 12 ? note - 12 : note + 12, 0.4F);
      }
   }

   private static void bassBar(NotePattern.Builder b, int t, int root) {
      b.add(t, BASS, root, 0.8F);
      b.add(t + 4, BASS, root, 0.55F);
      b.add(t + 8, BASS, fifth(root), 0.7F);
      b.add(t + 12, BASS, root, 0.55F);
      b.add(t + 16, BASS, root, 0.75F);
      b.add(t + 20, BASS, fifth(root), 0.55F);
      b.add(t + 24, BASS, root, 0.7F);
      b.add(t + 28, BASS, octave(root), 0.6F);
   }

   private static void bassPumpBar(NotePattern.Builder b, int t, int root) {
      b.add(t + 4, BASS, root, 0.7F);
      b.add(t + 12, BASS, fifth(root), 0.65F);
      b.add(t + 20, BASS, root, 0.7F);
      b.add(t + 28, BASS, octave(root), 0.65F);
   }

   private static void kitBar(NotePattern.Builder b, int t, int tier, boolean seam) {
      if (tier >= 3) {
         for (int beat = 0; beat < 4; beat++) {
            b.add(t + beat * 8, BASEDRUM, 6, beat == 0 ? 0.85F : 0.7F);
            b.add(t + beat * 8 + 4, SNARE, 12, 0.5F);
         }
      } else {
         b.add(t, BASEDRUM, 6, 0.85F);
         b.add(t + 16, BASEDRUM, 6, 0.75F);
         b.add(t + 8, SNARE, 12, 0.5F);
         b.add(t + 24, SNARE, 12, 0.55F);
         if (tier >= 2) {
            b.add(t + 26, BASEDRUM, 6, 0.55F);
         }
      }

      if (tier >= 2) {
         for (int s = 2; s < 32; s += 4) {
            b.add(t + s, HAT, 18, 0.2F);
         }
      } else {
         for (int s = 4; s < 32; s += 8) {
            b.add(t + s, HAT, 18, 0.18F);
         }
      }

      if (seam && tier == 2) {
         b.add(t + 28, SNARE, 12, 0.55F);
         b.add(t + 30, SNARE, 12, 0.65F);
      }
   }

   private static int fifth(int root) {
      return root + 7 <= 24 ? root + 7 : root - 5;
   }

   private static int octave(int root) {
      return root + 12 <= 24 ? root + 12 : fifth(root);
   }

   private static NotePattern buildOutro() {
      NotePattern.Builder b = NotePattern.builder(140);

      for (int i = 0; i < RIFF_TICKS.length; i++) {
         b.add(10 + RIFF_TICKS[i] * 2, LEAD, 10 + RIFF_STEPS[i], 0.7F - 0.03F * (float)i);
      }

      b.add(100, BASS, 10, 0.7F);
      b.add(100, SPARK, 14, 0.9F);
      b.add(100, CHIME, 14, 0.5F);
      return b.build();
   }
}
