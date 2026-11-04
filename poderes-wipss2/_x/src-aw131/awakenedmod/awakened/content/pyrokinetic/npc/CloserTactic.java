package com.awakenedmod.awakened.content.pyrokinetic.npc;

import com.awakenedmod.awakened.core.npc.NpcContext;
import com.awakenedmod.awakened.core.npc.NpcFooting;
import com.awakenedmod.awakened.core.npc.NpcMove;
import com.awakenedmod.awakened.core.npc.NpcRead;
import com.awakenedmod.awakened.core.npc.NpcStep;
import com.awakenedmod.awakened.core.npc.NpcTactic;
import net.minecraft.world.entity.LivingEntity;

final class CloserTactic extends NpcTactic {
   private static final float MELEE = 2.1F;
   private static final float CROWD = 1.8F;
   private static final double KNOCKED_MIN = 3.5;
   private static final double KNOCKED_MAX = 8.0;
   private static final double CROWD_RADIUS = 8.0;
   private static final double KNOCKED_AWAY = -0.15;
   private static final double TURTLE_RANGE = 6.0;
   private static final float TURTLE = 1.75F;
   private boolean cast;

   CloserTactic() {
      super("closer", 10, 0);
   }

   @Override
   public float score(NpcContext ctx) {
      if (ctx.ready("backdraft") && ctx.target() != null) {
         NpcRead r = ctx.read();
         LivingEntity target = ctx.target();
         boolean knocked = target.hurtTime > 0
            && r.airborne()
            && r.airTicks() >= ctx.skill().readDelay()
            && r.closing() < -0.15
            && ctx.npc().npcBrain().lastMoveId().equals("fireball");
         if (ctx.distance() >= 3.5 && ctx.distance() <= 8.0 && knocked) {
            return this.because("rush-closer", 2.1F);
         } else if (ctx.distance() <= 6.0 && r.seen() && r.shielded() && r.shieldTicks() >= ctx.skill().readDelay()) {
            return this.because("shield-bypass", 1.75F);
         } else {
            return ctx.hostilesNear(ctx.npc().position(), 8.0) >= 2 ? this.because("crowd", 1.8F) : 0.0F;
         }
      } else {
         return 0.0F;
      }
   }

   @Override
   protected void begin(NpcContext ctx) {
      this.cast = false;
   }

   @Override
   public NpcStep step(NpcContext ctx) {
      return this.cast ? NpcStep.DONE : NpcStep.cast("backdraft", NpcFooting.HOLD);
   }

   @Override
   public void started(NpcContext ctx, NpcMove move) {
      this.cast = true;
   }

   @Override
   public boolean whileCooling() {
      return true;
   }

   @Override
   public int maxTicks() {
      return 12;
   }
}
