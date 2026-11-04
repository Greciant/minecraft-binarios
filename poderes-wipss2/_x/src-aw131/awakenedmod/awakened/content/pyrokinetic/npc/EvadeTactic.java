package com.awakenedmod.awakened.content.pyrokinetic.npc;

import com.awakenedmod.awakened.core.npc.NpcContext;
import com.awakenedmod.awakened.core.npc.NpcFooting;
import com.awakenedmod.awakened.core.npc.NpcMove;
import com.awakenedmod.awakened.core.npc.NpcNav;
import com.awakenedmod.awakened.core.npc.NpcRead;
import com.awakenedmod.awakened.core.npc.NpcStep;
import com.awakenedmod.awakened.core.npc.NpcTactic;
import com.awakenedmod.awakened.core.npc.NpcTravel;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

final class EvadeTactic extends NpcTactic {
   private static final float BOW = 1.8F;
   private static final float RUSH = 1.7F;
   private static final int DRAW_MIN = 4;
   private static final float DRAW_BONUS = 0.25F;
   private static final double BOW_MIN_RANGE = 5.0;
   private static final double RUSH_RANGE = 8.0;
   private static final double HOP = 5.0;
   private static final double HOP_BACK_BOW = 0.0;
   private static final double HOP_BACK_RUSH = 0.8;
   private static final int JUKE_TICKS = 12;
   private int rolledDraw = Integer.MIN_VALUE;
   private boolean readDraw;
   private boolean bow;
   @Nullable
   private Vec3 dest;
   private int phase;
   private boolean fired;

   EvadeTactic() {
      super("evade", 0, 20);
   }

   @Override
   public float score(NpcContext ctx) {
      LivingEntity target = ctx.target();
      if (target == null) {
         return 0.0F;
      } else {
         NpcRead r = ctx.read();
         if (r.aiming() && ctx.lineOfSight() && ctx.distance() > 5.0 && r.useTicks() >= Math.max(4, ctx.skill().readDelay())) {
            int drawStart = ctx.npc().tickCount - r.useTicks();
            if (drawStart != this.rolledDraw) {
               this.rolledDraw = drawStart;
               this.readDraw = ctx.npc().getRandom().nextFloat() < ctx.skill().sidestepChance() + 0.25F;
            }

            if (this.readDraw) {
               return this.because("bow-dodge", 1.8F);
            }
         }

         return r.rushing() && ctx.distance() < 8.0 && PyroChecks.jetsOk(ctx) ? this.because("rush-kite", 1.7F) : 0.0F;
      }
   }

   @Override
   protected void begin(NpcContext ctx) {
      this.bow = ctx.read().aiming();
      this.phase = 0;
      this.fired = false;
      this.dest = null;
      LivingEntity target = ctx.target();
      if (target != null && PyroChecks.jetsOk(ctx)) {
         this.dest = NpcNav.sidestep(ctx.npc(), target, 5.0, this.bow ? 0.0 : 0.8, ctx.npc().getRandom().nextBoolean());
      }

      if (this.dest == null) {
         ctx.npc().npcBrain().footwork().flip();
      }
   }

   @Override
   public NpcStep step(NpcContext ctx) {
      if (this.phase == 0) {
         if (this.dest != null && ctx.npc().npcBrain().cooling()) {
            this.dest = null;
            ctx.npc().npcBrain().footwork().flip();
         }

         if (this.dest != null && this.age(ctx) < 12) {
            return NpcStep.travel(this.dest, NpcTravel.Purpose.DODGE);
         }

         if (this.dest == null && this.age(ctx) < 12) {
            return NpcStep.move(NpcFooting.JUKE);
         }

         this.phase = 1;
      }

      if (!this.fired && this.bow && ctx.lineOfSight() && !ctx.npc().npcBrain().cooling()) {
         if (PyroChecks.beamOk(ctx)) {
            return NpcStep.cast("flame_beam", NpcFooting.BAND);
         }

         if (PyroChecks.fireballOk(ctx)) {
            return NpcStep.cast("fireball", NpcFooting.JUKE);
         }
      }

      return NpcStep.DONE;
   }

   @Override
   public void started(NpcContext ctx, NpcMove move) {
      if (move.id().equals("fire_jets")) {
         this.phase = 1;
      } else {
         this.fired = true;
      }
   }

   @Override
   public boolean whileCooling() {
      return true;
   }

   @Override
   public int maxTicks() {
      return 40;
   }
}
