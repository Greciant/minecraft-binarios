package com.awakenedmod.awakened.content.pyrokinetic.npc;

import com.awakenedmod.awakened.core.npc.NpcContext;
import com.awakenedmod.awakened.core.npc.NpcFooting;
import com.awakenedmod.awakened.core.npc.NpcMove;
import com.awakenedmod.awakened.core.npc.NpcNav;
import com.awakenedmod.awakened.core.npc.NpcStep;
import com.awakenedmod.awakened.core.npc.NpcTactic;
import com.awakenedmod.awakened.core.npc.NpcTravel;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

final class FlankTactic extends NpcTactic {
   private static final float SHIELD = 1.6F;
   private static final float COVER = 1.3F;
   private static final float ANGLE = 0.85F;
   private static final int SHIELD_TICKS = 6;
   private static final int COVER_MEMORY = 60;
   private static final double COVER_NEAR = 18.0;
   private static final double STRAFER_SPEED = 0.15;
   private static final double[] TURNS = new double[]{70.0, 50.0, 95.0};
   private static final double RING_MIN = 8.0;
   private static final double RING_MAX = 13.0;
   private static final int HOP_WAIT = 40;
   @Nullable
   private Vec3 dest;
   private int phase;
   private boolean fired;

   FlankTactic() {
      super("flank", 5, 120);
   }

   @Override
   public float score(NpcContext ctx) {
      if (ctx.target() != null && PyroChecks.jetsOk(ctx)) {
         if (ctx.lineOfSight() && ctx.read().shielded() && ctx.read().shieldTicks() >= 6) {
            return this.because("shield-bypass", 1.6F);
         } else if (!ctx.lineOfSight() && ctx.unseenTicks() <= 60 && ctx.lastSeenPos() != null && ctx.npc().position().distanceTo(ctx.lastSeenPos()) <= 18.0) {
            return this.because("flank-cover", 1.3F);
         } else {
            return ctx.lineOfSight() && ctx.npc().mastery() >= 7 && ctx.targetSpeed() > 0.15 ? 0.85F : 0.0F;
         }
      } else {
         return 0.0F;
      }
   }

   @Override
   protected void begin(NpcContext ctx) {
      this.phase = 0;
      this.fired = false;
      this.dest = null;
      LivingEntity target = ctx.target();
      if (target != null) {
         boolean seen = ctx.lineOfSight() || ctx.lastSeenPos() == null;
         Vec3 centre = seen ? target.position() : ctx.lastSeenPos();
         Vec3 eye = seen ? target.getEyePosition() : centre.add(0.0, (double)target.getEyeHeight(), 0.0);
         double ring = Math.max(8.0, Math.min(13.0, ctx.npc().position().distanceTo(centre)));
         boolean right = ctx.npc().getRandom().nextBoolean();

         for (double turn : TURNS) {
            for (int side = 0; side < 2 && this.dest == null; side++) {
               this.dest = NpcNav.around(ctx.npc(), centre, eye, right == (side == 0) ? turn : -turn, ring);
            }

            if (this.dest != null) {
               break;
            }
         }
      }
   }

   @Override
   public NpcStep step(NpcContext ctx) {
      if (this.phase == 0) {
         if (this.dest == null) {
            return NpcStep.DONE;
         }

         if (this.age(ctx) < 40 && ctx.npc().position().distanceTo(this.dest) > 2.0) {
            return NpcStep.travel(this.dest, NpcTravel.Purpose.REPOSITION);
         }

         this.phase = 1;
      }

      if (!this.fired && ctx.lineOfSight()) {
         return PyroChecks.beamOk(ctx) ? NpcStep.cast("flame_beam", NpcFooting.BAND) : NpcStep.cast("fireball", NpcFooting.JUKE);
      } else {
         return this.fired ? NpcStep.DONE : NpcStep.move(NpcFooting.JUKE);
      }
   }

   @Override
   public void started(NpcContext ctx, NpcMove move) {
      if (this.phase == 0 && move.id().equals("fire_jets")) {
         this.phase = 1;
      } else if (this.phase == 1) {
         this.fired = true;
      }
   }

   @Override
   public String opens() {
      return "fire_jets";
   }

   @Override
   public int maxTicks() {
      return 90;
   }
}
