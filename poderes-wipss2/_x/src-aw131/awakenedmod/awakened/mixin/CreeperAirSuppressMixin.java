package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.content.aero.AirRules;
import com.awakenedmod.awakened.content.brawler.Marks;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Creeper.class})
public abstract class CreeperAirSuppressMixin {
   @Shadow
   private int swell;
   @Shadow
   private int oldSwell;

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void awakened$stallFuseWhileSuppressed(CallbackInfo ci) {
      Creeper self = (Creeper)this;
      if (AirRules.isSuppressed(self) || Marks.marked(self)) {
         this.swell = 0;
         this.oldSwell = 0;
         self.setSwellDir(-1);
      }
   }
}
