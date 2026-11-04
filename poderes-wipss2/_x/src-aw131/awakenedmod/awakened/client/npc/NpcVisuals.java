package com.awakenedmod.awakened.client.npc;

import com.awakenedmod.awakened.core.npc.NpcKit;
import com.awakenedmod.awakened.core.npc.NpcKits;
import java.util.HashMap;
import java.util.Map;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;

public final class NpcVisuals {
   private static final Map<String, NpcKitVisuals> VISUALS = new HashMap<>();

   private NpcVisuals() {
   }

   public static void register(String kitId, NpcKitVisuals visuals) {
      VISUALS.put(kitId, visuals);
   }

   public static NpcKitVisuals of(NpcKit kit) {
      return VISUALS.getOrDefault(kit.id(), NpcKitVisuals.NONE);
   }

   public static void registerRenderers(RegisterRenderers event) {
      for (NpcKit kit : NpcKits.all()) {
         event.registerEntityRenderer(kit.type().get(), ctx -> new PoweredNpcRenderer(ctx, kit, of(kit)));
      }
   }
}
