package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.content.summoning.SummonAttachments;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WitherBoss.class})
public abstract class WitherIdleFireMixin {
   @Inject(
      method = {"performRangedAttack(IDDDZ)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$minionHoldsIdleSpray(int head, double x, double y, double z, boolean dangerous, CallbackInfo ci) {
      WitherBoss self = (WitherBoss)this;
      if (head != 0 && dangerous && SummonAttachments.minion(self).isMinion()) {
         ci.cancel();
      }
   }
}
