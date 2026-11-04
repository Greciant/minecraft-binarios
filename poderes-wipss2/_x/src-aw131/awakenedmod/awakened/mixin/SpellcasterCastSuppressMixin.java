package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.content.aero.AirRules;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   targets = {"net.minecraft.world.entity.monster.SpellcasterIllager$SpellcasterUseSpellGoal"}
)
public abstract class SpellcasterCastSuppressMixin {
   @Shadow(
      remap = false
   )
   @Final
   private SpellcasterIllager this$0;

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$stallCastWhileSuppressed(CallbackInfo ci) {
      if (AirRules.isSuppressed(this.this$0)) {
         ci.cancel();
      }
   }
}
