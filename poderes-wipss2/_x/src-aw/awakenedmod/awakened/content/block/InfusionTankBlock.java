package com.awakenedmod.awakened.content.block;

import com.awakenedmod.awakened.content.AwakenedBlockEntities;
import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.regenerator.AdamantiumBonding;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.InfusionPosePayload;
import com.awakenedmod.awakened.core.net.OpenInfusionScreenPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class InfusionTankBlock extends Block implements EntityBlock {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
   public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 4);
   private static final VoxelShape OUTLINE = Block.box(0.0, 0.0, 0.0, 16.0, 11.0, 16.0);
   private static final VoxelShape COLLISION = Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0);
   private static final double BEGIN_RANGE_SQR = 64.0;

   public InfusionTankBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(PART, BedPart.FOOT))
            .setValue(CHARGE, 0)
      );
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, PART, CHARGE});
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction facing = context.getHorizontalDirection();
      BlockPos head = context.getClickedPos().relative(facing);
      Level level = context.getLevel();
      return level.getBlockState(head).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(head)
         ? (BlockState)this.defaultBlockState().setValue(FACING, facing)
         : null;
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(level, pos, state, placer, stack);
      if (!level.isClientSide()) {
         BlockPos head = pos.relative((Direction)state.getValue(FACING));
         level.setBlock(head, (BlockState)state.setValue(PART, BedPart.HEAD), 3);
         level.updateNeighborsAt(pos, Blocks.AIR);
         state.updateNeighbourShapes(level, pos, 3);
      }
   }

   private static Direction neighbourDirection(BedPart part, Direction facing) {
      return part == BedPart.FOOT ? facing : facing.getOpposite();
   }

   protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      if (dir != neighbourDirection((BedPart)state.getValue(PART), (Direction)state.getValue(FACING))) {
         return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
      } else {
         return neighbor.is(this) && neighbor.getValue(PART) != state.getValue(PART) ? state : Blocks.AIR.defaultBlockState();
      }
   }

   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.isClientSide() && !player.isCreative()) {
         BedPart part = (BedPart)state.getValue(PART);
         BlockPos other = pos.relative(neighbourDirection(part, (Direction)state.getValue(FACING)));
         BlockState otherState = level.getBlockState(other);
         if (otherState.is(this) && otherState.getValue(PART) != part) {
            level.setBlock(other, Blocks.AIR.defaultBlockState(), 35);
            level.levelEvent(player, 2001, other, Block.getId(otherState));
         }
      }

      return super.playerWillDestroy(level, pos, state, player);
   }

   protected BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   protected BlockState mirror(BlockState state, Mirror mirror) {
      return state.rotate(mirror.getRotation((Direction)state.getValue(FACING)));
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return OUTLINE;
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return COLLISION;
   }

   public static BlockPos footPos(BlockState state, BlockPos pos) {
      return state.getValue(PART) == BedPart.FOOT ? pos : pos.relative(((Direction)state.getValue(FACING)).getOpposite());
   }

   public static float lyingYaw(Direction facing) {
      return switch (facing) {
         case SOUTH -> 90.0F;
         case WEST -> 0.0F;
         case NORTH -> 270.0F;
         case EAST -> 180.0F;
         default -> 0.0F;
      };
   }

   @Nullable
   private static InfusionTankBlockEntity tankAt(Level level, BlockPos pos, BlockState state) {
      return level.getBlockEntity(footPos(state, pos)) instanceof InfusionTankBlockEntity tank ? tank : null;
   }

   public static void begin(ServerPlayer player, BlockPos footPos) {
      if (player != null) {
         if (!(player.distanceToSqr((double)footPos.getX() + 0.5, (double)footPos.getY() + 0.5, (double)footPos.getZ() + 0.5) > 64.0)) {
            if (player.serverLevel().getBlockEntity(footPos) instanceof InfusionTankBlockEntity tank) {
               tank.tryBegin(player);
            }
         }
      }
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
   ) {
      return placeFuel(stack, state, level, pos, player) == InteractionResult.PASS
         ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
         : ItemInteractionResult.SUCCESS;
   }

   private static InteractionResult placeFuel(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
      if (!stack.is((Item)AwakenedItems.ADAMANTIUM_INGOT.get())) {
         return climbIn(level, pos, state, player);
      } else if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else {
         InfusionTankBlockEntity tank = tankAt(level, pos, state);
         if (tank == null) {
            return InteractionResult.PASS;
         } else {
            int accepted = tank.addCharge(stack.getCount());
            if (accepted <= 0) {
               player.displayClientMessage(Component.translatable("awakened.infusion.tank_full"), true);
               return InteractionResult.SUCCESS;
            } else {
               if (!player.getAbilities().instabuild) {
                  stack.shrink(accepted);
               }

               Vec3 c = Vec3.atCenterOf(pos).add(0.0, 0.2, 0.0);
               if (level instanceof ServerLevel server) {
                  Fx.burst(server, ParticleTypes.FALLING_LAVA, c, 8, 0.4, 0.02);
               }

               float pitch = 0.9F + (float)tank.charge() / 4.0F * 0.5F;
               Fx.sound(level, c, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.7F, pitch);
               player.displayClientMessage(Component.translatable("awakened.infusion.charged", new Object[]{tank.charge(), 4}), true);
               if (tank.isFull()) {
                  Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.3F);
               }

               return InteractionResult.SUCCESS;
            }
         }
      }
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      return climbIn(level, pos, state, player);
   }

   private static InteractionResult climbIn(Level level, BlockPos pos, BlockState state, Player player) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else {
         if (player instanceof ServerPlayer serverPlayer) {
            InfusionTankBlockEntity tank = tankAt(level, pos, state);
            if (tank == null) {
               return InteractionResult.PASS;
            }

            if (!tank.tryEnter(serverPlayer)) {
               serverPlayer.displayClientMessage(Component.translatable("awakened.infusion.occupied"), true);
               return InteractionResult.SUCCESS;
            }

            PowerData data = AwakenedAttachments.get(serverPlayer);
            boolean regen = data.awakened() && data.powerId().equals("regenerator");
            boolean maxed = AdamantiumBonding.isMaxed(data);
            boolean ready = tank.isFull() && !tank.isInfusing() && regen && !maxed;
            if (!regen) {
               serverPlayer.displayClientMessage(Component.translatable("awakened.infusion.not_regenerator"), true);
            } else if (maxed) {
               serverPlayer.displayClientMessage(Component.translatable("awakened.adamantium.maxed"), true);
            } else if (!tank.isFull()) {
               serverPlayer.displayClientMessage(Component.translatable("awakened.infusion.need_more", new Object[]{tank.charge(), 4}), true);
            } else {
               serverPlayer.displayClientMessage(Component.translatable("awakened.infusion.lie_ready"), true);
            }

            PacketDistributor.sendToPlayer(serverPlayer, new InfusionPosePayload(true, lyingYaw((Direction)state.getValue(FACING))), new CustomPacketPayload[0]);
            PacketDistributor.sendToPlayer(
               serverPlayer,
               new OpenInfusionScreenPayload(footPos(state, pos), tank.chargeFraction(), AdamantiumBonding.coating(data), ready),
               new CustomPacketPayload[0]
            );
         }

         return InteractionResult.SUCCESS;
      }
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return state.getValue(PART) == BedPart.FOOT ? new InfusionTankBlockEntity(pos, state) : null;
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      if (!level.isClientSide() && state.getValue(PART) == BedPart.FOOT) {
         return type == AwakenedBlockEntities.INFUSION_TANK.get() ? (lvl, p, st, be) -> ((InfusionTankBlockEntity)be).serverTick((ServerLevel)lvl) : null;
      } else {
         return null;
      }
   }
}
