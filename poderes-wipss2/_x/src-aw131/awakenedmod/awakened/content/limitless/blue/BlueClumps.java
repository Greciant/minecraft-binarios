package com.awakenedmod.awakened.content.limitless.blue;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public interface BlueClumps {
   double orbAlong(ServerPlayer var1, Vec3 var2, Vec3 var3, double var4);

   List<BluePop> popAt(ServerPlayer var1, Vec3 var2);
}
