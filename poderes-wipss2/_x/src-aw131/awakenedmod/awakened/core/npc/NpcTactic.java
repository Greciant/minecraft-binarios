package com.awakenedmod.awakened.core.npc;

public abstract class NpcTactic {
   private final String id;
   private final int unlockLevel;
   private final int cooldownTicks;
   private String reason = "";
   private int beganAt;

   protected NpcTactic(String id, int unlockLevel, int cooldownTicks) {
      this.id = id;
      this.unlockLevel = unlockLevel;
      this.cooldownTicks = cooldownTicks;
   }

   public final String id() {
      return this.id;
   }

   public final int unlockLevel() {
      return this.unlockLevel;
   }

   public final int cooldownTicks() {
      return this.cooldownTicks;
   }

   public abstract float score(NpcContext var1);

   protected final float because(String why, float score) {
      this.reason = why;
      return score;
   }

   public final String reason() {
      return this.reason;
   }

   final void clearReason() {
      this.reason = "";
   }

   final void startRun(NpcContext ctx) {
      this.beganAt = ctx.npc().tickCount;
      this.begin(ctx);
   }

   protected final int age(NpcContext ctx) {
      return ctx.npc().tickCount - this.beganAt;
   }

   protected void begin(NpcContext ctx) {
   }

   public abstract NpcStep step(NpcContext var1);

   public void started(NpcContext ctx, NpcMove move) {
   }

   public boolean broken(NpcContext ctx) {
      return false;
   }

   public void end(NpcContext ctx) {
   }

   public int maxTicks() {
      return 100;
   }

   public String opens() {
      return "";
   }

   public boolean whileCooling() {
      return false;
   }
}
