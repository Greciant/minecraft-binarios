package com.devilfruits.core.fx;

import com.devilfruits.core.network.FxPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public record BombExplosionPayload(Vec3 pos, Vec3 normal, ExplosionProfile profile, float power, long seed, int blockState) {
   public void send(ServerLevel level, int entityId) {
      ServerFx.sendNear(level, CoreFx.EXPLOSION, entityId, this.pos, this.normal, this.power, this.seed, this.extra(), this.profile.sendRadius);
   }

   public int extra() {
      return this.profile.ordinal() | Math.max(0, this.blockState) << 3;
   }

   public static BombExplosionPayload of(FxPayload p) {
      return new BombExplosionPayload(p.pos(), p.dir(), ExplosionProfile.of(p.extra() & 7), p.power(), p.seed(), p.extra() >>> 3);
   }
}
