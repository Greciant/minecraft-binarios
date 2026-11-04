package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.vocab.Clash;
import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import com.awakenedmod.awakened.content.command.vocab.Trigger;
import com.awakenedmod.awakened.content.graviton.GravitySupport;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public abstract class DragEffect implements Effect {
   private static final double STRENGTH_PER_LEVEL = 0.15;
   private static final double WORKED_SQR = 1.0E-4;
   private static final double STOP_SHORT = 1.0;
   private final String id;
   private final double sign;

   protected DragEffect(String id, double sign) {
      this.id = id;
      this.sign = sign;
   }

   @Override
   public final String id() {
      return this.id;
   }

   @Override
   public final int unlockLevel() {
      return 3;
   }

   @Override
   public final double defaultWeight() {
      return 15.0;
   }

   @Override
   public final boolean hasLevel() {
      return true;
   }

   @Override
   public final boolean supports(TargetKind kind) {
      return kind != TargetKind.SELF;
   }

   @Override
   public final boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      if (target != caster && !target.hasIndirectPassenger(caster)) {
         double strength = 0.15 * (double)Math.max(1, level) * this.sign;
         if (this.sign > 0.0) {
            double room = target.position().distanceTo(caster.position()) - 1.0;
            if (room <= 0.0) {
               target.setDeltaMovement(target.getDeltaMovement().multiply(0.2, 1.0, 0.2));
               target.hurtMarked = true;
               return false;
            }

            strength = Math.min(strength, room);
         }

         GravitySupport.pull(target, caster.position(), strength, 0.0);
         return target.getDeltaMovement().lengthSqr() > 1.0E-4;
      } else {
         return false;
      }
   }

   @Override
   public final Clash clash(Target target, Effect effect, Trigger trigger) {
      return target.carriesCaster() ? Clash.CARRIES_YOU : null;
   }
}
