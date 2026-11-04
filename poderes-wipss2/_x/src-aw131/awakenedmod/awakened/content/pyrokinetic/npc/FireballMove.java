package com.awakenedmod.awakened.content.pyrokinetic.npc;

import com.awakenedmod.awakened.content.pyrokinetic.FireballEngine;
import com.awakenedmod.awakened.core.npc.NpcCaster;
import com.awakenedmod.awakened.core.npc.NpcContext;
import com.awakenedmod.awakened.core.npc.NpcMove;
import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;

final class FireballMove extends NpcMove {
   private static final int CLIP_TICKS = 10;
   private static final double MIN_RANGE = 3.0;
   private static final double PANIC_RANGE = 3.0;
   private static final float PANIC_SCORE = 2.5F;
   private static final float BASE_SCORE = 1.0F;
   private static final float THROW_TURN = 40.0F;
   private static final float RELEASE_CONE = 12.0F;
   private static final float GIVE_UP_CONE = 30.0F;
   private boolean nextOffHand;
   @Nullable
   private FireballEngine.Throw pending;
   private int age;

   FireballMove() {
      super("fireball", 0, 3.0, 22.0, 12);
   }

   @Override
   public float staminaCost() {
      return 6.0F;
   }

   @Override
   public String balanceKey() {
      return FireballEngine.BALANCE_KEY;
   }

   @Override
   public boolean inRange(NpcContext ctx) {
      return super.inRange(ctx) || ctx.distance() < 3.0;
   }

   @Override
   public float score(NpcContext ctx) {
      if (!(ctx.strain() >= 1.0F)) {
         return ctx.distance() < 3.0 ? 2.5F : 1.0F;
      } else {
         return ctx.distance() < 3.0 && PyroNpcKit.throwFits(ctx.caster()) ? 2.5F : 0.0F;
      }
   }

   @Override
   public void start(NpcContext ctx) {
      NpcCaster caster = ctx.caster();
      boolean offHand = this.nextOffHand;
      this.nextOffHand = !offHand;
      aim(ctx);
      this.pending = FireballEngine.windUp(caster, offHand);
      this.age = 0;
      ctx.npc().showClip(caster.rightArm(offHand) ? PyroNpcKit.FIREBALL_R : PyroNpcKit.FIREBALL_L);
   }

   @Override
   public boolean tick(NpcContext ctx) {
      this.age++;
      if (this.pending != null) {
         float off = aim(ctx);
         NpcCaster caster = ctx.caster();
         boolean last = this.age >= 9;
         if (FireballEngine.isDue(caster, this.pending) && (off <= 12.0F || last)) {
            if (off <= 30.0F) {
               FireballEngine.release(caster, this.pending);
            }

            this.pending = null;
         }
      }

      return this.age < 10;
   }

   @Override
   public void stop(NpcContext ctx, boolean interrupted) {
      this.pending = null;
      super.stop(ctx, interrupted);
   }

   private static float aim(NpcContext ctx) {
      Vec3 eye = ctx.caster().eyePos();
      Vec3 point = ctx.leadPoint(eye, 1.6F, 0.017, 3);
      if (point == null) {
         return 0.0F;
      } else {
         ctx.npc().aimAt(point, 40.0F);
         Vec3 to = point.subtract(eye).normalize();
         return (float)Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, to.dot(ctx.caster().aimDir())))));
      }
   }
}
