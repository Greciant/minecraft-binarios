package com.devilfruits.core.poneglyph;

import com.mojang.serialization.MapCodec;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DecipherDeskBlock extends BaseEntityBlock {
   public static final MapCodec<DecipherDeskBlock> CODEC = simpleCodec(DecipherDeskBlock::new);
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;
   public static final BooleanProperty BOOK = BooleanProperty.create("book");
   public static final EnumProperty<DecipherDeskBlock.Sheet> SHEET = EnumProperty.create("sheet", DecipherDeskBlock.Sheet.class);
   private static final VoxelShape SHAPE = Shapes.or(
      Block.box(0.5, 0.0, 0.5, 15.5, 2.0, 15.5), new VoxelShape[]{Block.box(2.0, 2.0, 2.0, 14.0, 10.0, 14.0), Block.box(1.0, 10.0, 1.0, 15.0, 13.5, 15.5)}
   );

   public DecipherDeskBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(ACTIVE, false))
               .setValue(BOOK, false))
            .setValue(SHEET, DecipherDeskBlock.Sheet.NONE)
      );
   }

   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, ACTIVE, BOOK, SHEET});
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

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new DecipherDeskBlockEntity(pos, state);
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (level.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         if (level.getBlockEntity(pos) instanceof DecipherDeskBlockEntity desk && player instanceof ServerPlayer sp) {
            if (desk.busyFor(sp)) {
               sp.displayClientMessage(Component.translatable("poneglyph.devilfruits.message.desk_busy"), true);
               return InteractionResult.CONSUME;
            }

            sp.openMenu(
               new SimpleMenuProvider((id, inv, p) -> new DecipherDeskMenu(id, inv, desk), Component.translatable("container.devilfruits.decipher_desk")),
               buf -> buf.writeBlockPos(pos)
            );
         }

         return InteractionResult.CONSUME;
      }
   }

   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DecipherDeskBlockEntity desk) {
         Containers.dropContents(level, pos, desk.items());
      }

      super.onRemove(state, level, pos, newState, movedByPiston);
   }

   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      if ((Boolean)state.getValue(ACTIVE)) {
         Direction facing = (Direction)state.getValue(FACING);
         double[] sheet = local(facing, 10.5, 12.2, 8.0);
         double[] from = state.getValue(BOOK) ? local(facing, 3.5, 14.0, 8.5) : local(facing, 8.0, 20.0, 8.0);

         for (int i = 0; i < 3; i++) {
            level.addParticle(
               ParticleTypes.ENCHANT,
               (double)pos.getX() + sheet[0],
               (double)pos.getY() + sheet[1],
               (double)pos.getZ() + sheet[2],
               from[0] - sheet[0] + (random.nextDouble() - 0.5) * 0.3,
               from[1] - sheet[1] + random.nextDouble() * 0.3,
               from[2] - sheet[2] + (random.nextDouble() - 0.5) * 0.3
            );
         }

         if (random.nextInt(4) == 0) {
            level.addParticle(
               (ParticleOptions)Poneglyphs.GLYPH_PARTICLE.get(),
               (double)pos.getX() + sheet[0],
               (double)pos.getY() + sheet[1] + 0.1,
               (double)pos.getZ() + sheet[2],
               (random.nextDouble() - 0.5) * 0.02,
               0.03,
               (random.nextDouble() - 0.5) * 0.02
            );
         }
      }
   }

   static double[] local(Direction facing, double x, double y, double z) {
      int turns = switch (facing) {
         case EAST -> 1;
         case SOUTH -> 2;
         case WEST -> 3;
         default -> 0;
      };

      for (int i = 0; i < turns; i++) {
         double nx = 16.0 - z;
         z = x;
         x = nx;
      }

      return new double[]{x / 16.0, y / 16.0, z / 16.0};
   }

   public static enum Sheet implements StringRepresentable {
      NONE,
      ENCRYPTED,
      DECIPHERED;

      public String getSerializedName() {
         return this.name().toLowerCase(Locale.ROOT);
      }
   }
}
