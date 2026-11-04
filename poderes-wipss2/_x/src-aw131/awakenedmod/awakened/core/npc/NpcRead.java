package com.awakenedmod.awakened.core.npc;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class NpcRead {
   private static final double RUSH_SPEED = 0.17;
   private static final double RUSH_RANGE = 12.0;
   private static final double STILL_SPEED = 0.04;
   private int now;
   private NpcRead.Use use = NpcRead.Use.NONE;
   private int useSince;
   private boolean shieldUp;
   private int shieldSince;
   private boolean airborne;
   private int airSince;
   private int landedAt = -1073741824;
   private int swungAt = -1073741824;
   private int stillSince;
   private boolean committed;
   private int committedSince;
   private double closing;
   private double distance = Double.MAX_VALUE;
   private float health = 1.0F;
   private boolean sprinting;
   private boolean hasShield;
   private boolean seen;
   private int seenSince;

   void reset(int tick) {
      this.now = tick;
      this.use = NpcRead.Use.NONE;
      this.shieldUp = false;
      this.airborne = false;
      this.landedAt = -1073741824;
      this.swungAt = -1073741824;
      this.stillSince = tick;
      this.committed = false;
      this.closing = 0.0;
      this.distance = Double.MAX_VALUE;
      this.health = 1.0F;
      this.sprinting = false;
      this.hasShield = false;
      this.seen = false;
      this.seenSince = tick;
   }

   void update(int tick, LivingEntity self, LivingEntity target, Vec3 velocity, boolean inSight) {
      if (!inSight) {
         this.seen = false;
      } else {
         if (!this.seen) {
            this.seen = true;
            this.seenSince = tick;
         }

         this.now = tick;
         NpcRead.Use u = useOf(target);
         if (u != this.use) {
            this.use = u;
            this.useSince = tick;
         }

         boolean blocking = target.isBlocking();
         if (blocking != this.shieldUp) {
            this.shieldUp = blocking;
            this.shieldSince = tick;
         }

         this.hasShield = blocking || isShield(target.getMainHandItem().getUseAnimation().name()) || isShield(target.getOffhandItem().getUseAnimation().name());
         boolean air = !target.onGround() && !target.isInWater() && !target.onClimbable() && !target.isPassenger();
         if (air != this.airborne) {
            if (!air) {
               this.landedAt = tick;
            } else {
               this.airSince = tick;
            }

            this.airborne = air;
         }

         if (target.swinging && target.swingTime <= 1) {
            this.swungAt = tick;
         }

         double flat = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
         if (flat > 0.04 || this.airborne) {
            this.stillSince = tick;
         }

         Vec3 in = self.position().subtract(target.position());
         double len = Math.sqrt(in.x * in.x + in.z * in.z);
         this.closing = len < 0.001 ? 0.0 : (velocity.x * in.x + velocity.z * in.z) / len;
         this.distance = (double)self.distanceTo(target);
         this.health = target.getHealth() / Math.max(1.0F, target.getMaxHealth());
         this.sprinting = target.isSprinting();
         boolean c = this.consuming() || this.aiming() || this.airborne || this.stillTicks() >= 20 || this.whiffed(10);
         if (c != this.committed) {
            this.committed = c;
            this.committedSince = tick;
         }
      }
   }

   private static NpcRead.Use useOf(LivingEntity target) {
      if (!target.isUsingItem()) {
         return NpcRead.Use.NONE;
      } else {
         String anim = target.getUseItem().getUseAnimation().name();

         return switch (anim) {
            case "EAT", "DRINK" -> NpcRead.Use.CONSUME;
            case "BOW", "CROSSBOW", "SPEAR" -> NpcRead.Use.AIM;
            case "BLOCK" -> NpcRead.Use.BLOCK;
            default -> NpcRead.Use.OTHER;
         };
      }
   }

   private static boolean isShield(String anim) {
      return anim.equals("BLOCK");
   }

   public NpcRead.Use use() {
      return this.use;
   }

   public int useTicks() {
      return this.use == NpcRead.Use.NONE ? 0 : this.now - Math.max(this.useSince, this.seenSince);
   }

   public boolean aiming() {
      return this.use == NpcRead.Use.AIM;
   }

   public boolean consuming() {
      return this.use == NpcRead.Use.CONSUME;
   }

   public boolean shielded() {
      return this.shieldUp;
   }

   public int shieldTicks() {
      return this.shieldUp ? this.now - Math.max(this.shieldSince, this.seenSince) : 0;
   }

   public boolean hasShield() {
      return this.hasShield;
   }

   public boolean airborne() {
      return this.airborne;
   }

   public int airTicks() {
      return this.airborne ? this.now - Math.max(this.airSince, this.seenSince) : 0;
   }

   public boolean landedWithin(int ticks) {
      return !this.airborne && this.now - this.landedAt <= ticks;
   }

   public int swungAgo() {
      return this.now - this.swungAt;
   }

   public boolean swungWithin(int ticks) {
      return this.now - this.swungAt <= ticks;
   }

   public boolean whiffed(int ticks) {
      return this.swungWithin(ticks) && this.distance > 3.5;
   }

   public int stillTicks() {
      return this.now - Math.max(this.stillSince, this.seenSince);
   }

   public boolean seen() {
      return this.seen;
   }

   public double closing() {
      return this.seen ? this.closing : 0.0;
   }

   public boolean charging() {
      return this.use == NpcRead.Use.CHARGE;
   }

   public boolean rushing() {
      return this.seen && this.distance < 12.0 && (this.closing >= 0.17 || (this.sprinting || this.charging()) && this.closing > 0.1);
   }

   public boolean sprinting() {
      return this.sprinting;
   }

   public float healthFraction() {
      return this.health;
   }

   public boolean committed() {
      return this.committed;
   }

   public int committedTicks() {
      return this.committed ? this.now - Math.max(this.committedSince, this.seenSince) : 0;
   }

   public static enum Use {
      NONE,
      AIM,
      CHARGE,
      CONSUME,
      BLOCK,
      OTHER;
   }
}
