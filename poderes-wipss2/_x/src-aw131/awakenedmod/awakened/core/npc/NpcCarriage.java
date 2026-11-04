package com.awakenedmod.awakened.core.npc;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.MoveControl.Operation;

final class NpcCarriage {
   private static final float HEAD_EASE = 0.5F;
   private static final float HEAD_MIN_STEP = 1.5F;
   private static final float HEAD_REST_SPEED = 10.0F;
   private static final float BODY_MAX = 50.0F;
   private static final float BODY_EASE = 0.3F;
   private static final float BODY_CAST_EASE = 0.5F;
   private static final double WALKING_SQR = 0.0025;
   private static final float BACKWARDS = 95.0F;
   private final PoweredNpc npc;
   private int facingTick = Integer.MIN_VALUE;
   private float heading = Float.NaN;

   NpcCarriage(PoweredNpc npc) {
      this.npc = npc;
   }

   boolean facing() {
      return this.facingTick == this.npc.tickCount;
   }

   static float ease(float from, float to, float max) {
      float d = Mth.wrapDegrees(to - from);
      float step = d * 0.5F;
      if (Math.abs(step) < 1.5F) {
         step = Math.copySign(Math.min(Math.abs(d), 1.5F), d);
      }

      return from + Mth.clamp(step, -max, max);
   }

   private void square() {
      this.npc.setYRot(this.npc.getYHeadRot());
      if (!Float.isNaN(this.heading)) {
         float speed = this.npc.getSpeed();
         float r = (this.heading - this.npc.getYRot()) * (float) (Math.PI / 180.0);
         this.npc.setZza(speed * Mth.cos(r));
         this.npc.setXxa(-speed * Mth.sin(r));
      }
   }

   static final class Body extends BodyRotationControl {
      private final PoweredNpc npc;

      Body(PoweredNpc npc) {
         super(npc);
         this.npc = npc;
      }

      public void clientTick() {
         float head = this.npc.getYHeadRot();
         if (this.npc.isCasting()) {
            this.npc.yBodyRot = this.npc.yBodyRot + Mth.wrapDegrees(head - this.npc.yBodyRot) * 0.5F;
         } else {
            double dx = this.npc.getX() - this.npc.xo;
            double dz = this.npc.getZ() - this.npc.zo;
            float want = this.npc.yBodyRot;
            if (dx * dx + dz * dz > 0.0025) {
               float walk = (float)(Mth.atan2(dz, dx) * 180.0F / (float)Math.PI) - 90.0F;
               want = Math.abs(Mth.wrapDegrees(head - walk)) > 95.0F ? walk - 180.0F : walk;
            }

            this.npc.yBodyRot = this.npc.yBodyRot + Mth.wrapDegrees(want - this.npc.yBodyRot) * 0.3F;
            float off = Mth.wrapDegrees(head - this.npc.yBodyRot);
            if (Math.abs(off) > 50.0F) {
               this.npc.yBodyRot = this.npc.yBodyRot + (off - Math.copySign(50.0F, off));
            }
         }
      }
   }

   static final class Look extends LookControl {
      private final PoweredNpc npc;
      private final NpcCarriage carriage;

      Look(PoweredNpc npc, NpcCarriage carriage) {
         super(npc);
         this.npc = npc;
         this.carriage = carriage;
      }

      public void tick() {
         if (!this.npc.aimedThisTick()) {
            if (this.lookAtCooldown > 0) {
               this.lookAtCooldown--;
               this.getYRotD().ifPresent(yaw -> this.npc.yHeadRot = NpcCarriage.ease(this.npc.yHeadRot, yaw, this.yMaxRotSpeed));
               this.getXRotD().ifPresent(pitch -> this.npc.setXRot(NpcCarriage.ease(this.npc.getXRot(), pitch, this.xMaxRotAngle)));
            } else {
               this.npc.yHeadRot = NpcCarriage.ease(this.npc.yHeadRot, this.npc.yBodyRot, 10.0F);
               this.npc.setXRot(NpcCarriage.ease(this.npc.getXRot(), 0.0F, 10.0F));
            }
         }

         if (this.carriage.facing()) {
            this.carriage.square();
         } else {
            this.clampHeadRotationToBody();
         }
      }
   }

   static final class Move extends MoveControl {
      private final PoweredNpc npc;
      private final NpcCarriage carriage;

      Move(PoweredNpc npc, NpcCarriage carriage) {
         super(npc);
         this.npc = npc;
         this.carriage = carriage;
      }

      public void tick() {
         boolean facing = this.npc.getTarget() != null && (this.npc.aimedThisTick() || this.npc.getLookControl().isLookingAtTarget());
         this.carriage.facingTick = facing ? this.npc.tickCount : Integer.MIN_VALUE;
         this.npc.setXxa(0.0F);
         if (!facing) {
            this.carriage.heading = Float.NaN;
            super.tick();
         } else {
            if (this.operation == Operation.MOVE_TO) {
               double dx = this.wantedX - this.npc.getX();
               double dz = this.wantedZ - this.npc.getZ();
               this.carriage.heading = dx * dx + dz * dz > 2.5E-7 ? (float)(Mth.atan2(dz, dx) * 180.0F / (float)Math.PI) - 90.0F : Float.NaN;
            } else if (this.operation != Operation.JUMPING) {
               this.carriage.heading = Float.NaN;
            }

            float facingYaw = this.npc.getYRot();
            super.tick();
            this.npc.setYRot(facingYaw);
         }
      }
   }
}
