package com.devilfruits.core.poneglyph;

import com.mojang.serialization.MapCodec;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class PoneglyphFaceBlock extends BaseEntityBlock {
   public static final MapCodec<PoneglyphFaceBlock> CODEC = simpleCodec(PoneglyphFaceBlock::new);
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final IntegerProperty CELL = IntegerProperty.create("cell", 0, 8);
   public static final IntegerProperty CARVING = IntegerProperty.create("carving", 0, 3);

   public PoneglyphFaceBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(CELL, 4))
            .setValue(CARVING, 0)
      );
   }

   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, CELL, CARVING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   protected BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   protected BlockState mirror(BlockState state, Mirror mirror) {
      return state.rotate(mirror.getRotation((Direction)state.getValue(FACING)));
   }

   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new PoneglyphFaceBlockEntity(pos, state);
   }

   @Nullable
   public static ResourceLocation poneglyphAt(BlockGetter level, BlockPos pos, BlockState state) {
      if (level.getBlockEntity(pos) instanceof PoneglyphFaceBlockEntity be && be.poneglyph() != null && PoneglyphData.poneglyph(be.poneglyph()) != null) {
         return be.poneglyph();
      }

      int carving = (Integer)state.getValue(CARVING);

      for (Entry<ResourceLocation, PoneglyphData.Poneglyph> e : PoneglyphData.poneglyphs().entrySet()) {
         if (e.getValue().carvingVariant() == carving) {
            return e.getKey();
         }
      }

      return null;
   }
}
