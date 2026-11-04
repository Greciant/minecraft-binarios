package com.awakenedmod.awakened.content;

import com.awakenedmod.awakened.content.pyrokinetic.npc.PyroNpcKit;
import com.awakenedmod.awakened.core.npc.NpcKits;

public final class AwakenedNpcs {
   private AwakenedNpcs() {
   }

   public static void registerAll() {
      NpcKits.register(new PyroNpcKit());
   }
}
