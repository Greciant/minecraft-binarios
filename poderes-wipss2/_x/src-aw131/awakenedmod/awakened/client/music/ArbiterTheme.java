package com.awakenedmod.awakened.client.music;

import com.awakenedmod.awakened.content.entity.ArbiterEntity;
import com.awakenedmod.awakened.content.entity.ArbiterVerses;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;

public final class ArbiterTheme extends BossTrack {
   private static final double RANGE = 48.0;
   private static final float GAIN = 0.9F;
   private static final int STEPS_PER_BAR = 16;
   private static final NoteScore SCORE = NoteScore.load("arbiter_theme");
   private static final NotePattern[] PHASES = new NotePattern[]{
      SCORE.barPattern("tier1", ArbiterVerses.barTicks(1)),
      SCORE.barPattern("tier2", ArbiterVerses.barTicks(2)),
      SCORE.barPattern("tier3", ArbiterVerses.barTicks(3))
   };
   private static final NotePattern KNEEL = SCORE.pattern("kneel");
   private static final NotePattern INTRO = SCORE.pattern("intro");
   private static final NotePattern OUTRO = SCORE.pattern("outro");
   private int phase = 1;
   private NotePattern loop = PHASES[0];
   private int loopPos;
   private int loopCursor;
   private int hold;
   private int lastBarLine = -1;
   private boolean inSession;
   private int introPos = -1;
   private int introCursor;
   private boolean kneelHolding;
   private int kneelCursor;
   private int outroPos = -1;
   private int outroCursor;
   private boolean finished;

   public ArbiterTheme() {
      super(48.0, 0.9F);
   }

   @Override
   protected boolean matches(LivingEntity entity) {
      return entity instanceof ArbiterEntity;
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
      this.hold = 0;
      this.lastBarLine = -1;
      this.inSession = false;
      this.introPos = -1;
      this.introCursor = 0;
      this.kneelHolding = false;
      this.kneelCursor = 0;
      this.outroPos = -1;
      this.outroCursor = 0;
      this.finished = false;
   }

   @Override
   protected void play(@Nullable LivingEntity boss) {
      ArbiterEntity judge = boss instanceof ArbiterEntity a ? a : null;
      if (judge != null && !judge.isDeadOrDying() && judge.deathTime <= 0) {
         int escalation = Math.max(1, Math.min(3, judge.getEscalation()));
         if (escalation != this.phase) {
            this.switchPhase(escalation);
         }

         int barLine = judge.barLine();
         boolean lineStruck = this.lastBarLine >= 0 && barLine != this.lastBarLine;
         this.lastBarLine = barLine;
         if (!this.inSession) {
            if (!this.sessionOpen(judge)) {
               if (judge.inCeremony()) {
                  this.tickIntro();
               } else {
                  this.introPos = -1;
               }

               return;
            }

            this.inSession = true;
            if (this.introPos >= 0 && this.introPos < INTRO.length()) {
               this.cutAll();
            }

            this.alignDownbeat(20);
            lineStruck = false;
         }

         if (lineStruck) {
            int tier = ArbiterEntity.barLineTier(barLine);
            if (tier >= 1 && tier != this.phase) {
               this.switchPhase(tier);
            }

            this.lockToBarLine();
         }

         if (judge.isKneeling()) {
            if (!this.kneelHolding) {
               this.kneelHolding = true;
               this.cutAll();
               this.kneelCursor = KNEEL.cursorFor(this.loopPos % this.bar());
            }

            this.tickHeartbeat();
            this.advanceLoopSilently();
         } else {
            if (this.kneelHolding) {
               this.kneelHolding = false;
               int lead = judge.getPhase() == 5 ? 40 : 20;
               this.alignDownbeat(lead);
            }

            this.tickLoop();
         }
      } else {
         this.tickOutro();
      }
   }

   @Override
   protected boolean ownsSoundscape() {
      return this.inSession || this.introPos >= 0;
   }

   private boolean sessionOpen(ArbiterEntity judge) {
      return judge.isKneeling() || !judge.inRecess() && !judge.inCeremony();
   }

   private void tickLoop() {
      if (this.hold > 0) {
         this.hold--;
      } else {
         this.loopCursor = this.loop.emit(this.loopPos, this.loopCursor, 1.0F, this);
         if (++this.loopPos >= this.loop.length()) {
            this.loopPos = 0;
            this.loopCursor = 0;
         }
      }
   }

   private void advanceLoopSilently() {
      if (this.hold > 0) {
         this.hold--;
      } else {
         if (++this.loopPos >= this.loop.length()) {
            this.loopPos = 0;
         }
      }
   }

   private void jumpTo(int pos) {
      this.loopPos = Math.floorMod(pos, Math.max(1, this.loop.length()));
      this.loopCursor = this.loop.cursorFor(this.loopPos);
      this.hold = 0;
   }

   private void lockToBarLine() {
      int bar = this.bar();
      int inBar = this.loopPos % bar;
      int ahead = inBar <= bar / 2 ? inBar : inBar - bar;
      if (ahead != 0) {
         int nudge = (Math.max(1, bar / 16) + 1) / 2;
         if (ahead > 0 && ahead <= nudge) {
            this.hold = ahead;
         } else {
            this.jumpTo(this.loopPos - ahead);
         }
      }
   }

   private void tickIntro() {
      if (this.introPos < 0) {
         this.introPos = 0;
         this.introCursor = 0;
      }

      if (this.introPos < INTRO.length()) {
         this.introCursor = INTRO.emit(this.introPos, this.introCursor, 1.0F, this);
         this.introPos++;
      }
   }

   private void tickHeartbeat() {
      NotePattern beat = KNEEL;
      int pos = this.loopPos % this.bar();
      if (pos == 0) {
         this.kneelCursor = 0;
      }

      this.kneelCursor = beat.emit(pos, this.kneelCursor, 1.0F, this);
   }

   private int bar() {
      return Math.max(1, ArbiterVerses.barTicks(this.phase));
   }

   private void alignDownbeat(int leadIn) {
      int bar = this.bar();
      int lead = Math.min(leadIn, bar);
      int boundary = (this.loopPos + bar - 1) / bar * bar;
      this.jumpTo(boundary - lead);
   }

   private void tickOutro() {
      if (!this.finished) {
         if (this.outroPos < 0) {
            this.outroPos = 0;
            this.outroCursor = 0;
            this.cutAll();
         }

         this.outroCursor = OUTRO.emit(this.outroPos, this.outroCursor, 1.0F, this);
         if (++this.outroPos >= OUTRO.length()) {
            this.finished = true;
         }
      }
   }

   private void switchPhase(int escalation) {
      int oldBar = this.bar();
      this.phase = escalation;
      this.loop = PHASES[escalation - 1];
      int newBar = this.bar();
      int barIndex = this.loopPos / oldBar;
      int inBar = this.loopPos % oldBar;
      int pos = barIndex * newBar + inBar * newBar / oldBar;
      this.jumpTo(pos >= this.loop.length() ? 0 : pos);
      if (this.kneelHolding) {
         this.kneelCursor = KNEEL.cursorFor(this.loopPos % newBar);
      }
   }
}
