package com.awakenedmod.awakened.content.pyrokinetic.npc;

import com.awakenedmod.awakened.content.pyrokinetic.FlameBeamAbility;
import com.awakenedmod.awakened.content.pyrokinetic.FlameBeamEngine;
import com.awakenedmod.awakened.content.pyrokinetic.FlameTone;
import com.awakenedmod.awakened.content.pyrokinetic.PyroFlames;
import com.awakenedmod.awakened.core.npc.NpcCaster;
import com.awakenedmod.awakened.core.npc.NpcContext;
import com.awakenedmod.awakened.core.npc.NpcMove;
import com.awakenedmod.awakened.core.npc.PoweredNpc;
import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

final class FlameBeamMove extends NpcMove {
   private static final int MAX_TICKS = 60;
   private static final double HOLD_MIN = 4.0;
   private static final double HOLD_MAX = 20.0;
   private static final double OFF_LINE_DEG = 14.0;
   private static final int OFF_LINE_TICKS = 12;
   private static final double SLOW = 0.1;
   private static final double FAST = 0.22;
   private static final double RUSH_SPEED = 0.12;
   private static final double RUSH_WATCH = 9.0;
   private static final double RUSH_BREAK = 6.0;
   private static final float BASE_SCORE = 1.1F;
   private static final int COOLDOWN = 110;
   private static final int WINDUP_PAD = 2;
   private static final int WINDUP_MIN = 6;
   private static final double GATHER_SCALE = 0.22;
   @Nullable
   private FlameBeamEngine.Channel channel;
   private boolean dry;
   private int offLine;
   private int windup;

   FlameBeamMove() {
      super("flame_beam", 3, 6.0, 18.0, 110);
   }

   @Override
   public float staminaCost() {
      return 6.0F;
   }

   @Override
   public String balanceKey() {
      return FlameBeamEngine.BALANCE_KEY;
   }

   @Override
   public boolean canStart(NpcContext ctx) {
      LivingEntity target = ctx.target();
      Vec3 center = ctx.targetCenter();
      if (target != null && center != null) {
         NpcCaster caster = ctx.caster();
         Vec3 eye = caster.eyePos();
         if (caster.level().clip(new ClipContext(eye, center, Block.COLLIDER, Fluid.NONE, caster.body())).getType() != Type.MISS) {
            return false;
         } else {
            FlameBeamAbility.LineHit first = FlameBeamAbility.firstOnLine(caster.level(), caster.body(), eye, center, e -> true);
            return first == null || first.entity() == target || caster.canHarm(first.entity());
         }
      } else {
         return false;
      }
   }

   @Override
   public float score(NpcContext ctx) {
      if (!(ctx.strain() >= 0.8F) && !(ctx.stamina() < 0.35F)) {
         float score = 1.1F;
         double speed = ctx.targetSpeed();
         if (speed < 0.1) {
            score *= 1.4F;
         } else if (speed > 0.22) {
            score *= 0.5F;
         }

         if (ctx.fleeing()) {
            score *= 0.7F;
         }

         if (ctx.distance() < 9.0 && closing(ctx) >= 0.12) {
            score *= 0.3F;
         }

         return score;
      } else {
         return 0.0F;
      }
   }

   private static double closing(NpcContext ctx) {
      LivingEntity target = ctx.target();
      if (target != null && !(ctx.distance() < 0.001)) {
         Vec3 in = ctx.npc().position().subtract(target.position());
         return (ctx.targetVelocity().x * in.x + ctx.targetVelocity().z * in.z) / Math.max(0.001, Math.hypot(in.x, in.z));
      } else {
         return 0.0;
      }
   }

   @Override
   public void start(NpcContext ctx) {
      this.channel = null;
      this.dry = false;
      this.offLine = 0;
      this.windup = Math.max(6, ctx.skill().reactionTicks() / 2 + 2);
      track(ctx);
      NpcCaster caster = ctx.caster();
      PyroFlames.sound(caster, caster.body().position().add(0.0, 1.2, 0.0), SoundEvents.FIRECHARGE_USE, 0.7F, 0.6F, FlameTone.of(caster));
      gather(caster);
   }

   private void ignite(NpcContext ctx) {
      this.channel = new FlameBeamEngine.Channel();
      FlameBeamEngine.ignite(ctx.caster());
      ctx.npc().showClip(PyroNpcKit.FLAME_BEAM);
   }

   private static void gather(NpcCaster caster) {
      FlameTone tone = FlameTone.of(caster);

      for (boolean off : new boolean[]{false, true}) {
         PyroFlames.knot(caster.level(), caster.hand(off, 30.0, 0.34, 0.46, 0.6), 0.22, tone);
      }
   }

   @Override
   public boolean tick(NpcContext ctx) {
      PoweredNpc npc = ctx.npc();
      LivingEntity target = ctx.target();
      if (target == null) {
         return false;
      } else if (this.windup > 0) {
         track(ctx);
         if (--this.windup % 2 == 0) {
            gather(ctx.caster());
         }

         if (!ctx.lineOfSight()) {
            return false;
         } else {
            if (this.windup == 0) {
               this.ignite(ctx);
            }

            return true;
         }
      } else {
         FlameBeamEngine.Channel ch = this.channel;
         if (ch == null) {
            return false;
         } else if (!npc.stamina().drain(0.45F, this.balanceKey(), npc.level().getGameTime())) {
            this.dry = true;
            return false;
         } else {
            track(ctx);
            FlameBeamEngine.tick(ctx.caster(), ch);
            if (this.channel == null) {
               return false;
            } else {
               Vec3 center = ctx.targetCenter();
               Vec3 toTarget = center.subtract(ctx.caster().eyePos()).normalize();
               double off = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, toTarget.dot(ctx.caster().aimDir())))));
               this.offLine = !(off > 14.0) && ctx.lineOfSight() ? 0 : this.offLine + 1;
               boolean rushed = ctx.distance() < 6.0 && closing(ctx) >= 0.12;
               return ch.age() < 60 && this.offLine < 12 && ctx.distance() >= 4.0 && ctx.distance() <= 20.0 && !rushed && ctx.strain() < 1.0F;
            }
         }
      }
   }

   @Override
   public void stop(NpcContext ctx, boolean interrupted) {
      if (this.channel != null) {
         FlameBeamEngine.shutdown(ctx.caster(), this.channel, this.dry);
         this.channel = null;
      }

      super.stop(ctx, interrupted);
   }

   @Override
   public double footworkWhileRunning() {
      return 0.4;
   }

   private static void track(NpcContext ctx) {
      Vec3 center = ctx.targetCenter();
      if (center != null) {
         ctx.npc().aimAt(center, ctx.skill().turnDegPerTick());
      }
   }
}
