package com.awakenedmod.awakened.content.block;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

final class SummonFrame {
   private SummonFrame() {
   }

   static List<SummonFrame.Slot> slots(BlockPos pedestal, SummonFrame.Spec spec) {
      List<SummonFrame.Slot> slots = new ArrayList<>(17);
      BlockPos floorCenter = pedestal.below();

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            slots.add(new SummonFrame.Slot(floorCenter.offset(dx, 0, dz), spec.floor(), ".floor"));
         }
      }

      int[][] corners = new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

      for (int[] c : corners) {
         BlockPos cornerPos = pedestal.offset(c[0], 0, c[1]);
         slots.add(new SummonFrame.Slot(cornerPos, spec.corner(), ".corner"));
         slots.add(new SummonFrame.Slot(cornerPos.above(), spec.topper(), ".top"));
      }

      return slots;
   }

   static List<SummonFrame.Slot> missing(ServerLevel level, BlockPos pedestal, SummonFrame.Spec spec) {
      List<SummonFrame.Slot> missing = new ArrayList<>();

      for (SummonFrame.Slot slot : slots(pedestal, spec)) {
         if (!slot.satisfied(level)) {
            missing.add(slot);
         }
      }

      return missing;
   }

   @Nullable
   static Component firstFault(ServerLevel level, BlockPos pos, SummonFrame.Spec spec) {
      for (SummonFrame.Slot slot : slots(pos, spec)) {
         if (!slot.satisfied(level)) {
            return Component.translatable(spec.keyPrefix() + slot.roleSuffix(), new Object[]{slot.pos().getX(), slot.pos().getY(), slot.pos().getZ()});
         }
      }

      return null;
   }

   static record Slot(BlockPos pos, Block required, String roleSuffix) {
      boolean satisfied(ServerLevel level) {
         return level.getBlockState(this.pos).is(this.required);
      }
   }

   static record Spec(Block floor, Block corner, Block topper, String keyPrefix) {
   }
}
