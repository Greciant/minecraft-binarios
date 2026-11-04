package com.awakenedmod.awakened.content.pyrokinetic.npc;

import com.awakenedmod.awakened.core.npc.NpcContext;
import com.awakenedmod.awakened.core.npc.NpcFooting;
import com.awakenedmod.awakened.core.npc.NpcMove;
import com.awakenedmod.awakened.core.npc.NpcStep;
import com.awakenedmod.awakened.core.npc.NpcTactic;

final class PressureTactic extends NpcTactic {
   private static final double MAX_RANGE = 24.0;
   private static final int SHOTS = 2;
   private static final float BASE = 1.0F;
   private static final float FINISH = 1.4F;
   private static final float FINISH_HEALTH = 0.35F;
   private static final float SHIELDED = 0.3F;
   private static final float SAVING = 0.3F;
   private static final float MIN_STAMINA = 0.2F;
   private static final float HEAT_DAMP = 0.4F;
   private static final float WARM = 0.5F;
   private static final int COOL_BEAT = 32;
   private static final int BEAT = 8;
   private static final int BEAT_SPREAD = 14;
   private int shots;
   private int thrown;
   private int beatEnd;
   private boolean finish;

   PressureTactic() {
      super("pressure", 0, 0);
   }

   @Override
   public float score(NpcContext ctx) {
      boolean low = ctx.read().healthFraction() < 0.35F;
      if (!ctx.lineOfSight() || ctx.distance() > 24.0 || PyroNpcKit.throwsLeft(ctx) < (low ? 1 : 2) || ctx.stamina() < 0.2F) {
         return 0.0F;
      } else if (ctx.read().shielded()) {
         return 0.3F;
      } else {
         return low ? this.because("press-low", 1.4F) : 1.0F * (1.0F - 0.4F * ctx.strain()) * (PyroChecks.saving(ctx) ? 0.3F : 1.0F);
      }
   }

   @Override
   protected void begin(NpcContext ctx) {
      this.shots = Math.max(1, Math.min(2, PyroNpcKit.throwsLeft(ctx)));
      this.thrown = 0;
      this.beatEnd = -1;
      this.finish = ctx.read().healthFraction() < 0.35F;
   }

   @Override
   public NpcStep step(NpcContext ctx) {
      NpcFooting feet = this.finish ? NpcFooting.PRESS : NpcFooting.JUKE;
      if (this.thrown >= this.shots || this.thrown > 0 && ctx.read().shielded()) {
         if (this.beatEnd < 0) {
            int beat = 8 + ctx.npc().getRandom().nextInt(14) + ctx.skill().readDelay();
            this.beatEnd = this.age(ctx) + (ctx.strain() > 0.5F ? Math.max(beat, 32) : beat);
         }

         return this.age(ctx) < this.beatEnd ? NpcStep.move(NpcFooting.JUKE) : NpcStep.DONE;
      } else {
         return NpcStep.cast("fireball", feet);
      }
   }

   @Override
   public void started(NpcContext ctx, NpcMove move) {
      if (move.id().equals("fireball")) {
         this.thrown++;
      }
   }

   @Override
   public boolean broken(NpcContext ctx) {
      return this.thrown < this.shots && !ctx.lineOfSight() && ctx.unseenTicks() > 10;
   }

   @Override
   public String opens() {
      return "fireball";
   }

   @Override
   public int maxTicks() {
      return 110;
   }
}
