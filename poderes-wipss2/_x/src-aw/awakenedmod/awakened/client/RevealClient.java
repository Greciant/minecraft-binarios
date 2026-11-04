package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.ExoVisual;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

public final class RevealClient {
   private static volatile Set<Integer> revealed = Set.of();

   private RevealClient() {
   }

   public static void refresh() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         revealed = Set.of();
      } else {
         List<Integer> ids = ((ExoVisual)player.getData(AwakenedAttachments.EXO_VISUAL)).revealedIds();
         revealed = ids.isEmpty() ? Set.of() : Set.copyOf(ids);
      }
   }

   public static boolean isRevealed(int entityId) {
      return revealed.contains(entityId);
   }

   public static boolean isRevealed(Entity entity) {
      return entity != null && revealed.contains(entity.getId());
   }
}
