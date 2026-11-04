package com.awakenedmod.awakened.mixin;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Creeper.class})
public interface CreeperFuseAccessor {
   @Accessor("swell")
   void awakened$setSwell(int var1);

   @Accessor("oldSwell")
   void awakened$setOldSwell(int var1);
}
