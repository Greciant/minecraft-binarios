package com.awakenedmod.awakened.mixin;

import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({FlyingMoveControl.class})
public interface FlyingHoverAccessor {
   @Accessor("hoversInPlace")
   boolean awakened$hoversInPlace();
}
