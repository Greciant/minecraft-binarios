package com.awakenedmod.awakened.core.npc;

import java.util.List;
import javax.annotation.Nullable;

public interface NpcObserver {
   void think(PoweredNpc var1, NpcContext var2, NpcBrain.State var3, List<NpcObserver.Option> var4, @Nullable NpcMove var5);

   void event(PoweredNpc var1, String var2, String var3);

   void section(PoweredNpc var1, String var2, long var3);

   void tickCost(PoweredNpc var1, long var2);

   public static record Option(String move, float score, String note) {
   }
}
