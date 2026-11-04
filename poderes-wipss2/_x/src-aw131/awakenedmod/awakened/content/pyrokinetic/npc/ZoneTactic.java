package com.awakenedmod.awakened.content.pyrokinetic.npc;

import com.awakenedmod.awakened.core.npc.NpcContext;
import com.awakenedmod.awakened.core.npc.NpcFooting;
import com.awakenedmod.awakened.core.npc.NpcMove;
import com.awakenedmod.awakened.core.npc.NpcStep;
import com.awakenedmod.awakened.core.npc.NpcTactic;
import net.minecraft.world.entity.LivingEntity;

final class ZoneTactic extends NpcTactic {
   private static final float COVER = 1.7F;
   private static final float SHIELD = 1.5F;
   private static final float BUNCH = 1.4F;
   private static final float STILL = 1.2F;
   private static final float OPEN = 1.0F;
   private static final int COVER_MEMORY = 100;
   private static final int SHIELD_TICKS = 8;
   private static final int STILL_TICKS = 20;
   private static final double BUNCH_RADIUS = 4.0;
   private static final double OPEN_MIN = 10.0;
   private static final double OPEN_MAX = 24.0;
   private static final float OPEN_STAMINA = 0.5F;
   private static final int FOLLOW = 30;
   private static final double SLOW = 0.08;
   private int phase;
   private int followUntil;

   ZoneTactic() {
      super("zone", 7, 120);
   }

   @Override
   public float score(NpcContext ctx) {
      LivingEntity target = ctx.target();
      if (target == null || !ctx.usable("barrage")) {
         return 0.0F;
      } else if (!ctx.lineOfSight()) {
         return ctx.lastSeenPos() != null && ctx.unseenTicks() <= 100 ? this.because("flush-cover", 1.7F) : 0.0F;
      } else if (ctx.read().shielded() && ctx.read().shieldTicks() >= 8) {
         return this.because("shield-bypass", 1.5F);
      } else if (ctx.hostilesNear(target.position(), 4.0) >= 2) {
         return this.because("flush-bunch", 1.4F);
      } else if (ctx.read().stillTicks() >= 20) {
         return this.because("flush-still", 1.2F);
      } else {
         return ctx.distance() >= 10.0 && ctx.distance() <= 24.0 && ctx.read().closing() < 0.1 && ctx.stamina() >= 0.5F ? 1.0F : 0.0F;
      }
   }

   @Override
   protected void begin(NpcContext ctx) {
      this.phase = 0;
   }

   @Override
   public NpcStep step(NpcContext ctx) {
      switch (this.phase) {
         case 0:
            return NpcStep.cast("barrage", NpcFooting.BAND);
         case 1:
            if (this.age(ctx) > this.followUntil) {
               return NpcStep.DONE;
            } else if (ctx.lineOfSight() && ctx.targetSpeed() < 0.08 && PyroChecks.beamOk(ctx)) {
               return NpcStep.cast("flame_beam", NpcFooting.BAND);
            } else {
               return ctx.lineOfSight() && !ctx.read().shielded() ? NpcStep.cast("fireball", NpcFooting.JUKE) : NpcStep.move(NpcFooting.JUKE);
            }
         default:
            return NpcStep.DONE;
      }
   }

   @Override
   public void started(NpcContext ctx, NpcMove move) {
      if (this.phase == 0 && move.id().equals("barrage")) {
         this.phase = 1;
         this.followUntil = this.age(ctx) + 30 + 60;
      } else if (this.phase == 1) {
         this.phase = 2;
      }
   }

   @Override
   public boolean broken(NpcContext ctx) {
      return this.phase == 0 && !ctx.usable("barrage");
   }

   @Override
   public String opens() {
      return "barrage";
   }

   @Override
   public int maxTicks() {
      return 150;
   }
}
