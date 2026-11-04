package com.awakenedmod.awakened.content.pyrokinetic.npc;

import com.awakenedmod.awakened.core.npc.NpcContext;
import com.awakenedmod.awakened.core.npc.NpcFooting;
import com.awakenedmod.awakened.core.npc.NpcMove;
import com.awakenedmod.awakened.core.npc.NpcStep;
import com.awakenedmod.awakened.core.npc.NpcTactic;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

final class BurnTactic extends NpcTactic {
   private static final float BASE = 1.05F;
   private static final double TRACK = 0.6;
   private static final double LOSE = 1.0;
   private static final float OUTRUN = 0.35F;
   private static final float FINISH = 1.5F;
   private static final float FINISH_HEALTH = 0.35F;
   private static final double RUSH_CLOSING = 0.12;
   private static final double RUSH_RANGE = 9.0;
   private boolean cast;

   BurnTactic() {
      super("burn", 3, 0);
   }

   @Override
   public float score(NpcContext ctx) {
      LivingEntity target = ctx.target();
      if (target != null && PyroChecks.beamOk(ctx) && (!(ctx.distance() < 9.0) || !(ctx.read().closing() >= 0.12))) {
         double sweep = sweepDegPerTick(ctx, target) / Math.max(0.1, (double)ctx.skill().turnDegPerTick());
         if (sweep >= 1.0) {
            return 0.36749998F;
         } else if (sweep <= 0.6 && ctx.read().healthFraction() < 0.35F) {
            return this.because("burn-low", 1.5F);
         } else {
            return sweep <= 0.6 ? 1.05F : 1.05F * (float)(1.0 - (sweep - 0.6) / 0.4 * 0.6500000059604645);
         }
      } else {
         return 0.0F;
      }
   }

   private static double sweepDegPerTick(NpcContext ctx, LivingEntity target) {
      Vec3 in = new Vec3(target.getX() - ctx.npc().getX(), 0.0, target.getZ() - ctx.npc().getZ());
      double len = in.length();
      if (len < 0.001) {
         return 0.0;
      } else {
         Vec3 v = ctx.targetVelocity();
         double side = Math.abs(v.x * -in.z + v.z * in.x) / len;
         return Math.toDegrees(side / len);
      }
   }

   @Override
   protected void begin(NpcContext ctx) {
      this.cast = false;
   }

   @Override
   public NpcStep step(NpcContext ctx) {
      return this.cast ? NpcStep.DONE : NpcStep.cast("flame_beam", NpcFooting.BAND);
   }

   @Override
   public void started(NpcContext ctx, NpcMove move) {
      this.cast = true;
   }

   @Override
   public String opens() {
      return "flame_beam";
   }

   @Override
   public int maxTicks() {
      return 30;
   }
}
