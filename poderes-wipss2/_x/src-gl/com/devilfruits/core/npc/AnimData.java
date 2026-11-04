package com.devilfruits.core.npc;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class AnimData {
   private final float[] window = new float[3];
   private int slot;
   private float lastYaw;
   private int lastHurtTime;
   private boolean primed;
   public float speed;
   public float distance;
   public float prevDistance;
   public Locomotion.Gait gait = Locomotion.Gait.IDLE;
   public Locomotion.Gait previousGait = Locomotion.Gait.IDLE;
   public int gaitAge = -100;
   public float rate = 1.0F;
   public float forward = 1.0F;
   public float side;
   public int airTicks;
   public int landAge = -100;
   public int actionId = -1;
   public int actionAge = -100;
   private int actionSeq = Integer.MIN_VALUE;
   public boolean armed;
   public int armedAge = -100;
   public int hurtAge = -100;
   public boolean hurtFromBehind;
   public float yawRate;
   public float aim;
   public float prevAim;
   public float swing;
   public float prevSwing;
   public float roll;
   public float prevRoll;
   private float swingVel;
   private float rollVel;

   public void update(FactionFighter e, Locomotion.Speeds speeds) {
      int age = e.tickCount;
      Vec3 v = e.getGroundSpeedRelativeToSupport();
      float s = (float)v.horizontalDistance();
      this.window[this.slot] = s;
      this.slot = (this.slot + 1) % this.window.length;
      this.speed = (this.window[0] + this.window[1] + this.window[2]) / 3.0F;
      this.prevDistance = this.distance;
      this.distance += s / 20.0F;
      float yaw = e.yBodyRot * (float) (Math.PI / 180.0);
      if (s > 0.2F) {
         this.forward = (float)((v.x * (double)(-Mth.sin(yaw)) + v.z * (double)Mth.cos(yaw)) / (double)s);
         this.side = (float)((v.x * (double)Mth.cos(yaw) + v.z * (double)Mth.sin(yaw)) / (double)s);
      }

      Locomotion.State st = Locomotion.state(this.speed, this.gait, speeds);
      if (st.gait() != this.gait) {
         this.previousGait = this.gait;
         this.gait = st.gait();
         this.gaitAge = age;
      }

      this.rate = st.rate();
      if (!e.onGround() && !e.isInWater() && !e.isPassenger()) {
         this.airTicks++;
      } else {
         if (this.airTicks > 6) {
            this.landAge = age;
         }

         this.airTicks = 0;
      }

      int seq = e.actionSeq();
      if (seq != this.actionSeq) {
         if (this.primed) {
            this.actionId = e.syncedAction();
            this.actionAge = age;
         }

         this.actionSeq = seq;
      }

      boolean nowArmed = e.hasFlag(1);
      if (nowArmed != this.armed) {
         this.armed = nowArmed;
         this.armedAge = this.primed ? age : -100;
      }

      if (e.hurtTime > this.lastHurtTime) {
         this.hurtAge = age;
         this.hurtFromBehind = Mth.cos((e.getHurtDir() - 90.0F) * (float) (Math.PI / 180.0)) < 0.0F;
      }

      this.lastHurtTime = e.hurtTime;
      this.prevAim = this.aim;
      this.aim = Mth.approach(this.aim, e.hasFlag(2) ? 1.0F : 0.0F, 0.25F);
      this.yawRate = this.primed ? Mth.wrapDegrees(e.yBodyRot - this.lastYaw) : 0.0F;
      this.lastYaw = e.yBodyRot;
      float swingTarget = Math.min(35.0F, this.speed * 6.0F) + (this.airTicks > 2 ? -15.0F : 0.0F);
      float rollTarget = Mth.clamp(-this.yawRate * 1.5F, -25.0F, 25.0F);
      this.prevSwing = this.swing;
      this.prevRoll = this.roll;
      this.swingVel = (this.swingVel + (swingTarget - this.swing) * 0.25F) * 0.72F;
      this.rollVel = (this.rollVel + (rollTarget - this.roll) * 0.25F) * 0.72F;
      this.swing = this.swing + this.swingVel;
      this.roll = this.roll + this.rollVel;
      this.primed = true;
   }

   public float actionTime(int age, float partialTick) {
      return this.actionAge < 0 ? 10000.0F : (float)(age - this.actionAge) + partialTick;
   }
}
