package com.awakenedmod.awakened.core.npc;

public abstract class NpcMove {
   private final String id;
   private final int unlockLevel;
   private final double minRange;
   private final double maxRange;
   private final int cooldownTicks;

   protected NpcMove(String id, int unlockLevel, double minRange, double maxRange, int cooldownTicks) {
      this.id = id;
      this.unlockLevel = unlockLevel;
      this.minRange = minRange;
      this.maxRange = maxRange;
      this.cooldownTicks = cooldownTicks;
   }

   public final String id() {
      return this.id;
   }

   public final int unlockLevel() {
      return this.unlockLevel;
   }

   public final double minRange() {
      return this.minRange;
   }

   public final double maxRange() {
      return this.maxRange;
   }

   public final int cooldownTicks() {
      return this.cooldownTicks;
   }

   public abstract float staminaCost();

   public abstract String balanceKey();

   public boolean needsLineOfSight() {
      return true;
   }

   public boolean inRange(NpcContext ctx) {
      return ctx.distance() >= this.minRange && ctx.distance() <= this.maxRange;
   }

   public boolean canStart(NpcContext ctx) {
      return true;
   }

   public abstract float score(NpcContext var1);

   public boolean usable(NpcContext ctx) {
      return this.score(ctx) > 0.0F;
   }

   public abstract void start(NpcContext var1);

   public abstract boolean tick(NpcContext var1);

   public void stop(NpcContext ctx, boolean interrupted) {
      ctx.npc().clearClip();
   }

   public double footworkWhileRunning() {
      return 1.0;
   }

   public boolean allowsSidestep() {
      return this.footworkWhileRunning() > 0.0;
   }

   public boolean committed() {
      return false;
   }
}
