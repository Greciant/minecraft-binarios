package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.content.summoning.MinionSupport;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Warden.class})
public abstract class WardenMinionMixin {
   @Inject(
      method = {"canTargetEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$minionWardenCanTargetWarden(@Nullable Entity entity, CallbackInfoReturnable<Boolean> cir) {
      Warden self = (Warden)this;
      if (entity instanceof Warden target
         && (MinionSupport.owner(self) != null || MinionSupport.owner(target) != null)
         && !MinionSupport.alliedSummonForbidden(self, target)
         && self.level() == target.level()
         && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)
         && !self.isAlliedTo(target)
         && !target.isInvulnerable()
         && !target.isDeadOrDying()
         && self.level().getWorldBorder().isWithinBounds(target.getBoundingBox())) {
         cir.setReturnValue(true);
      }
   }

   @Redirect(
      method = {"customServerAiStep()V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/monster/warden/Warden;applyDarknessAround(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/Entity;I)V"
      )
   )
   private static void awakened$noDarknessFromMinionWarden(ServerLevel level, Vec3 pos, @Nullable Entity source, int radius) {
      if (!(source instanceof Warden warden) || MinionSupport.owner(warden) == null) {
         Warden.applyDarknessAround(level, pos, source, radius);
      }
   }
}
