package com.awakenedmod.awakened.content.pyrokinetic.npc;

import com.awakenedmod.awakened.core.npc.NpcContext;

final class PyroChecks {
   private static final float BEAM_STRAIN = 0.75F;
   private static final float BEAM_STAMINA = 0.3F;
   private static final double BEAM_MIN = 6.0;
   private static final double BEAM_MAX = 18.0;
   private static final float SAVE = 0.45F;

   private PyroChecks() {
   }

   static boolean beamOk(NpcContext ctx) {
      return ctx.ready("flame_beam") && ctx.lineOfSight() && ctx.strain() < 0.75F && ctx.stamina() >= 0.3F && ctx.distance() >= 6.0 && ctx.distance() <= 18.0;
   }

   static boolean fireballOk(NpcContext ctx) {
      return ctx.ready("fireball") && ctx.lineOfSight() && PyroNpcKit.throwsLeft(ctx) >= 1;
   }

   static boolean saving(NpcContext ctx) {
      return ctx.stamina() < 0.45F && (offCooldown(ctx, "flame_beam") || offCooldown(ctx, "barrage"));
   }

   private static boolean offCooldown(NpcContext ctx, String move) {
      return ctx.npc().npcBrain().offCooldown(move);
   }

   static boolean jetsOk(NpcContext ctx) {
      return ctx.ready("fire_jets") && ctx.strain() < 0.95F && ctx.stamina() >= 0.2F;
   }
}
