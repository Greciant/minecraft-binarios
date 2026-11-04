package com.devilfruits.client.vfx;

import com.devilfruits.core.network.FxPayload;
import net.minecraft.world.phys.Vec3;

public record VfxParams(Vec3 pos, Vec3 dir, float power, long seed, int extra) {
   public static final VfxParams NONE = new VfxParams(Vec3.ZERO, Vec3.ZERO, 0.0F, 0L, 0);

   public static VfxParams of(FxPayload p) {
      return new VfxParams(p.pos(), p.dir(), p.power(), p.seed(), p.extra());
   }
}
