package com.awakenedmod.awakened.core.npc;

import net.minecraft.world.phys.Vec3;

public interface NpcTravel {
   boolean plan(NpcContext var1, Vec3 var2, NpcTravel.Purpose var3);

   public static enum Purpose {
      ENGAGE,
      FLEE,
      LEAVE_HAZARD,
      REPOSITION,
      DODGE;
   }
}
