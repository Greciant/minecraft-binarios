package com.awakenedmod.awakened.content.pyrokinetic.npc;

import com.awakenedmod.awakened.core.npc.NpcContext;
import com.awakenedmod.awakened.core.npc.NpcFooting;
import com.awakenedmod.awakened.core.npc.NpcMove;
import com.awakenedmod.awakened.core.npc.NpcRead;
import com.awakenedmod.awakened.core.npc.NpcStep;
import com.awakenedmod.awakened.core.npc.NpcTactic;

final class PunishTactic extends NpcTactic {
   private static final float EAT = 1.9F;
   private static final float WHIFF = 1.6F;
   private static final float SHIELD = 1.7F;
   private static final float AIM = 1.4F;
   private static final float STILL = 1.3F;
   private static final float AIR = 1.2F;
   private static final int STILL_TICKS = 30;
   private static final int WHIFF_TICKS = 12;
   private static final int AIR_TICKS = 2;
   private static final float WARM = 0.7F;
   private static final int AIR_SNAP_GAP = 60;
   private String move = "fireball";
   private boolean cast;
   private int nextAirSnap;

   PunishTactic() {
      super("punish", 0, 0);
   }

   @Override
   public float score(NpcContext ctx) {
      if (!ctx.lineOfSight()) {
         return 0.0F;
      } else {
         NpcRead r = ctx.read();
         int delay = ctx.skill().readDelay();
         boolean beam = PyroChecks.beamOk(ctx);
         boolean ball = PyroChecks.fireballOk(ctx);
         if ((beam || ball) && (!r.shielded() || beam)) {
            if (r.consuming() && r.useTicks() >= delay) {
               return this.because("punish-eat", 1.9F);
            } else if (r.shielded() && r.shieldTicks() >= delay && beam) {
               return this.because("shield-bypass", 1.7F);
            } else if (r.whiffed(12 + delay) && r.swungAgo() >= delay) {
               return this.because("punish-whiff", 1.6F);
            } else {
               boolean spare = beam || ctx.strain() < 0.7F;
               if (r.aiming() && r.useTicks() >= delay && spare) {
                  return this.because("punish-aim", 1.4F);
               } else if (r.stillTicks() >= 30 + delay && spare) {
                  return this.because("punish-still", 1.3F);
               } else {
                  return r.airborne()
                        && r.airTicks() >= Math.max(2, delay)
                        && ball
                        && ctx.npc().tickCount >= this.nextAirSnap
                        && ctx.strain() < 0.7F
                        && !PyroChecks.saving(ctx)
                     ? this.because("punish-air", 1.2F)
                     : 0.0F;
               }
            }
         } else {
            return 0.0F;
         }
      }
   }

   @Override
   protected void begin(NpcContext ctx) {
      this.cast = false;
      boolean channelable = !ctx.read().airborne() && PyroChecks.beamOk(ctx);
      this.move = channelable ? "flame_beam" : "fireball";
   }

   @Override
   public NpcStep step(NpcContext ctx) {
      if (this.cast) {
         return NpcStep.DONE;
      } else {
         boolean shielded = ctx.read().shielded();
         if (this.move.equals("flame_beam") && !PyroChecks.beamOk(ctx)) {
            if (shielded || !PyroChecks.fireballOk(ctx)) {
               return NpcStep.DONE;
            }

            this.move = "fireball";
         }

         return this.move.equals("fireball") && shielded
            ? NpcStep.DONE
            : NpcStep.cast(this.move, this.move.equals("flame_beam") ? NpcFooting.BAND : NpcFooting.JUKE);
      }
   }

   @Override
   public void started(NpcContext ctx, NpcMove started) {
      this.cast = true;
      if (this.reason().equals("punish-air")) {
         this.nextAirSnap = ctx.npc().tickCount + 60;
      }
   }

   @Override
   public boolean broken(NpcContext ctx) {
      return !this.cast && !ctx.read().committed() && !ctx.read().shielded() && this.age(ctx) > ctx.skill().readDelay() + 6;
   }

   @Override
   public int maxTicks() {
      return 30;
   }
}
