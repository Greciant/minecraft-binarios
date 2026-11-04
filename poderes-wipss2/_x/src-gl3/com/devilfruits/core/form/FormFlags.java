package com.devilfruits.core.form;

import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.registry.DFAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class FormFlags {
   public static final int BOOSTING = 1;
   public static final int FLYING = 2;
   public static final int DYING = 128;

   public static int fruit(int n) {
      if (n >= 0 && n <= 15) {
         return 1 << 8 + n;
      } else {
         throw new IllegalArgumentException("Fruit flag index must be 0-15: " + n);
      }
   }

   public static boolean has(Player player, int bit) {
      return player.hasData(DFAttachments.PLAYER_DATA) && DFAttachments.data(player).hasFlag(bit);
   }

   public static boolean has(Player player, ResourceLocation fruitId, int bit) {
      if (!player.hasData(DFAttachments.PLAYER_DATA)) {
         return false;
      } else {
         PlayerFruitData data = DFAttachments.data(player);
         return data.hasFlag(bit) && fruitId.equals(data.fruitId());
      }
   }

   public static void set(ServerPlayer player, int bit, boolean on) {
      PlayerFruitData data = DFAttachments.data(player);
      int flags = on ? data.flags() | bit : data.flags() & ~bit;
      if (data.setFlags(flags)) {
         ServerFx.formState(player);
      }
   }

   private FormFlags() {
   }
}
