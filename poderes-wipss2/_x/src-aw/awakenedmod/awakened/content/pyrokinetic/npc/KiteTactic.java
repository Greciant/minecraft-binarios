package com.awakenedmod.awakened.content.pyrokinetic.npc;

import com.awakenedmod.awakened.core.npc.NpcContext;
import com.awakenedmod.awakened.core.npc.NpcFooting;
import com.awakenedmod.awakened.core.npc.NpcRead;
import com.awakenedmod.awakened.core.npc.NpcStep;
import com.awakenedmod.awakened.core.npc.NpcTactic;

final class KiteTactic extends NpcTactic {
   private static final float SCORE = 1.5F;
   private static final double CLOSING = 0.06;
   private static final double THROW_RANGE = 7.0;
   private static final int MAX = 60;

   KiteTactic() {
      super("kite", 0, 30);
   }

   @Override
   public float score(NpcContext ctx) {
      double inner = ctx.npc().kit().preferredMin() - 1.0;
      if (ctx.target() != null && !(ctx.distance() >= inner)) {
         NpcRead r = ctx.read();
         return !r.rushing() && !(r.closing() > 0.06) && !ctx.hugging() ? 0.0F : this.because("rush-kite", 1.5F);
      } else {
         return 0.0F;
      }
   }

   @Override
   public NpcStep step(NpcContext ctx) {
      if (ctx.distance() >= ctx.npc().kit().preferredMin() + 1.0) {
         return NpcStep.DONE;
      } else {
         return ctx.distance() <= 7.0 && PyroChecks.fireballOk(ctx) && !ctx.read().shielded()
            ? NpcStep.cast("fireball", NpcFooting.KITE)
            : NpcStep.move(NpcFooting.KITE);
      }
   }

   @Override
   public boolean whileCooling() {
      return true;
   }

   @Override
   public int maxTicks() {
      return 60;
   }
}
