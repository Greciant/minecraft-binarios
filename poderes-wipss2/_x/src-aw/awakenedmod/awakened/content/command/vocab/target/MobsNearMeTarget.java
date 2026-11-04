package com.awakenedmod.awakened.content.command.vocab.target;

import com.awakenedmod.awakened.content.command.vocab.RuleContext;
import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import com.awakenedmod.awakened.core.AimUtil;
import java.util.Collection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class MobsNearMeTarget implements Target {
   private static final double MIN_RADIUS = 1.0;

   @Override
   public String id() {
      return "mobs_near_me";
   }

   @Override
   public int unlockLevel() {
      return 8;
   }

   @Override
   public double defaultWeight() {
      return 1.5;
   }

   @Override
   public TargetKind kind() {
      return TargetKind.NEAR;
   }

   @Override
   public Collection<LivingEntity> select(ServerPlayer caster, RuleContext ctx) {
      double radius = Math.max(1.0, (double)ctx.radius());
      return AimUtil.livingAround(caster, caster.position(), radius);
   }

   @Override
   public boolean sparesOwnSide() {
      return true;
   }
}
