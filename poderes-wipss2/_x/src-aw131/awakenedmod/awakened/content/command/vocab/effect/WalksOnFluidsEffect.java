package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.CommandSupport;
import com.awakenedmod.awakened.content.command.vocab.Clash;
import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.Posture;
import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.Trigger;
import com.awakenedmod.awakened.core.FluidFooting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class WalksOnFluidsEffect implements Effect {
   private static final double RISE_SPEED = 0.4;

   @Override
   public String id() {
      return "walks_on_fluids";
   }

   @Override
   public boolean hostile() {
      return false;
   }

   @Override
   public int unlockLevel() {
      return 3;
   }

   @Override
   public double defaultWeight() {
      return 10.0;
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      FluidFooting.holdUp(target);
      if (target.isShiftKeyDown()) {
         return false;
      } else if (FluidFooting.wading(target) && FluidFooting.stillAtFeet(target)) {
         Vec3 motion = CommandSupport.motion(target);
         if (motion.y <= 0.0) {
            target.setDeltaMovement(motion.x, 0.4, motion.z);
            target.hurtMarked = true;
         }

         return true;
      } else {
         return FluidFooting.standingOnFluid(target);
      }
   }

   @Override
   public void release(LivingEntity target) {
      FluidFooting.letGo(target);
   }

   @Override
   public Clash clash(Target target, Effect effect, Trigger trigger) {
      return target.movesWithCaster() && trigger.postures().contains(Posture.SNEAKING) ? Clash.SNEAK_SINKS : null;
   }
}
