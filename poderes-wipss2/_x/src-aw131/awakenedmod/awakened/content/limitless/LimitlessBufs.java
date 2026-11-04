package com.awakenedmod.awakened.content.limitless;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.phys.Vec3;

public final class LimitlessBufs {
   private LimitlessBufs() {
   }

   public static void writeVec(ByteBuf buf, Vec3 v) {
      buf.writeDouble(v.x);
      buf.writeDouble(v.y);
      buf.writeDouble(v.z);
   }

   public static Vec3 readVec(ByteBuf buf) {
      return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
   }
}
