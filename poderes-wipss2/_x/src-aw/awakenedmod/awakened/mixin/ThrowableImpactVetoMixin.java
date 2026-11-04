package com.awakenedmod.awakened.mixin;

import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({ThrowableProjectile.class})
public abstract class ThrowableImpactVetoMixin {
}
