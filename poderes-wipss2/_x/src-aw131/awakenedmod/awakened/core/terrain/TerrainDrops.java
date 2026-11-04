package com.awakenedmod.awakened.core.terrain;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public final class TerrainDrops {
   private TerrainDrops() {
   }

   public static void asMined(ServerLevel level, ServerPlayer miner, List<RemovedBlock> blocks) {
      ItemStack tool = new ItemStack(Items.IRON_PICKAXE);

      for (RemovedBlock b : blocks) {
         Block.dropResources(b.state(), level, b.pos(), null, miner, tool);
      }
   }
}
