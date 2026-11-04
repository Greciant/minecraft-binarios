package com.devilfruits.fruits.uo_uo_seiryu;

import com.devilfruits.core.data.PlayerFruitData;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SeiryuRules {
   private static final double[][] NUDGES = new double[][]{
      {0.0, 0.0, 0.0},
      {0.0, 0.6, 0.0},
      {0.6, 0.0, 0.0},
      {-0.6, 0.0, 0.0},
      {0.0, 0.0, 0.6},
      {0.0, 0.0, -0.6},
      {1.0, 0.0, 0.0},
      {-1.0, 0.0, 0.0},
      {0.0, 0.0, 1.0},
      {0.0, 0.0, -1.0}
   };

   @Nullable
   public static Component dragonHasRoom(ServerPlayer player, PlayerFruitData data) {
      return findDragonRoom(player).isPresent() ? null : Component.translatable("message.devilfruits.uo_uo_seiryu.no_room");
   }

   public static Optional<Vec3> findDragonRoom(ServerPlayer player) {
      EntityDimensions dims = UoUoSeiryu.fruit().form("dragon").dimensions();
      Vec3 base = player.position();
      AABB current = player.getBoundingBox();

      for (double[] n : NUDGES) {
         Vec3 at = base.add(n[0], n[1], n[2]);
         AABB target = dims.makeBoundingBox(at);
         boolean reachable = n[1] > 0.0
            ? fits(player.level(), target.expandTowards(0.0, -n[1], 0.0))
            : fits(player.level(), current.expandTowards(n[0], 0.0, n[2])) && fits(player.level(), target);
         if (reachable) {
            return Optional.of(at);
         }
      }

      return Optional.empty();
   }

   public static boolean fits(Level level, AABB box) {
      BlockPos min = BlockPos.containing(box.minX + 1.0E-7, box.minY + 1.0E-7, box.minZ + 1.0E-7);
      BlockPos max = BlockPos.containing(box.maxX - 1.0E-7, box.maxY - 1.0E-7, box.maxZ - 1.0E-7);

      for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
         BlockState state = level.getBlockState(pos);
         if (!state.isAir() && !state.is(BlockTags.LEAVES)) {
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (!shape.isEmpty()
               && shape.move((double)pos.getX(), (double)pos.getY(), (double)pos.getZ()).toAabbs().stream().anyMatch(b -> b.intersects(box.deflate(1.0E-7)))) {
               return false;
            }
         }
      }

      return true;
   }

   private SeiryuRules() {
   }
}
