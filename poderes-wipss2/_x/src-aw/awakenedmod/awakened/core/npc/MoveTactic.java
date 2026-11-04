package com.awakenedmod.awakened.core.npc;

public class MoveTactic extends NpcTactic {
   private final String move;
   private final float weight;
   private final String why;
   private boolean cast;

   public MoveTactic(String id, String move, int unlock, float weight, String why) {
      super(id, unlock, 0);
      this.move = move;
      this.weight = weight;
      this.why = why;
   }

   @Override
   public float score(NpcContext ctx) {
      if (!ctx.ready(this.move)) {
         return 0.0F;
      } else {
         float s = ctx.moveScore(this.move) * this.weight;
         return this.why.isEmpty() ? s : this.because(this.why, s);
      }
   }

   @Override
   protected void begin(NpcContext ctx) {
      this.cast = false;
   }

   @Override
   public NpcStep step(NpcContext ctx) {
      if (this.cast) {
         return NpcStep.DONE;
      } else {
         ctx.moveScore(this.move);
         return NpcStep.cast(this.move, NpcFooting.BAND);
      }
   }

   @Override
   public void started(NpcContext ctx, NpcMove started) {
      this.cast = this.cast | started.id().equals(this.move);
   }

   @Override
   public int maxTicks() {
      return 40;
   }
}
