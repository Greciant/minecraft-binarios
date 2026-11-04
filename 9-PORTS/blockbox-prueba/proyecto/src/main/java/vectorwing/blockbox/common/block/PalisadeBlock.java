package vectorwing.blockbox.common.block;

import com.google.common.collect.Maps;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.Tags;
import vectorwing.blockbox.common.block.state.PalisadeConnection;
import vectorwing.blockbox.mixin.CrossCollisionBlockAccessor;
import vectorwing.blockbox.common.registry.ModBlocks;
import vectorwing.blockbox.common.registry.ModSounds;
import vectorwing.blockbox.common.tag.ModTags;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Port Forge 1.20.1: sin sistema de codec; ItemAbilities/ItemAbility (NeoForge) ->
 * ToolAction/ToolActions (Forge); useItemOn(...) de 1.21 (devuelve ItemInteractionResult) ->
 * use(...) unico de 1.20.1 (devuelve InteractionResult). getAABBIndex usa this.stateToIndex,
 * un campo protegido de CrossCollisionBlock: se agrego un Access Transformer
 * (META-INF/accesstransformer.cfg) igual que trae el original, ya que Forge usa el mismo
 * formato de AT que NeoForge.
 */
public class PalisadeBlock extends CrossCollisionBlock implements SimpleWaterloggedBlock
{
	public static final EnumProperty<PalisadeConnection> TYPE_NORTH = EnumProperty.create("north", PalisadeConnection.class);
	public static final EnumProperty<PalisadeConnection> TYPE_EAST = EnumProperty.create("east", PalisadeConnection.class);
	public static final EnumProperty<PalisadeConnection> TYPE_SOUTH = EnumProperty.create("south", PalisadeConnection.class);
	public static final EnumProperty<PalisadeConnection> TYPE_WEST = EnumProperty.create("west", PalisadeConnection.class);

	public final Supplier<Block> strippedForm;
	public final Supplier<Block> spikedForm;

	public static final Map<Direction, EnumProperty<PalisadeConnection>> PROPERTY_BY_DIRECTION = Util.make(Maps.newHashMap(), (map) -> {
		map.put(Direction.NORTH, TYPE_NORTH);
		map.put(Direction.EAST, TYPE_EAST);
		map.put(Direction.SOUTH, TYPE_SOUTH);
		map.put(Direction.WEST, TYPE_WEST);
	});

	public PalisadeBlock(Properties properties) {
		this(null, null, 4.0F, 4.0F, 16.0F, 16.0F, 16.0F, properties);
	}

	public PalisadeBlock(@Nullable Supplier<Block> spikedForm, Properties properties) {
		this(spikedForm, null, 4.0F, 4.0F, 16.0F, 16.0F, 16.0F, properties);
	}

	public PalisadeBlock(@Nullable Supplier<Block> spikedForm, @Nullable Supplier<Block> strippedForm, Properties properties) {
		this(spikedForm, strippedForm, 4.0F, 4.0F, 16.0F, 16.0F, 16.0F, properties);
	}

	public PalisadeBlock(@Nullable Supplier<Block> spikedForm, @Nullable Supplier<Block> strippedForm, float nodeWidth, float extensionWidth, float nodeHeight, float extensionHeight, float collisionHeight, Properties properties) {
		super(nodeWidth, extensionWidth, nodeHeight, extensionHeight, collisionHeight, properties);
		this.spikedForm = spikedForm;
		this.strippedForm = strippedForm;
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(TYPE_NORTH, PalisadeConnection.NONE)
				.setValue(TYPE_EAST, PalisadeConnection.NONE)
				.setValue(TYPE_SOUTH, PalisadeConnection.NONE)
				.setValue(TYPE_WEST, PalisadeConnection.NONE)
				.setValue(WATERLOGGED, false));
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		ItemStack stack = player.getItemInHand(hand);
		if (spikedForm != null && stack.canPerformAction(ToolActions.SWORD_DIG) && level.getBlockState(pos.above()).isAir()) {
			if (!level.isClientSide) {
				level.playSound(null, pos, ModSounds.ITEM_SWORD_CARVE.get(), SoundSource.BLOCKS, 1.0F, 0.9F);
				level.addDestroyBlockEffect(pos, state);
				stack.hurtAndBreak(2, player, (e) -> e.broadcastBreakEvent(hand));
				level.setBlock(pos, spikedForm.get().defaultBlockState()
						.setValue(SpikedPalisadeBlock.NORTH, !state.getValue(TYPE_NORTH).equals(PalisadeConnection.NONE))
						.setValue(SpikedPalisadeBlock.EAST, !state.getValue(TYPE_EAST).equals(PalisadeConnection.NONE))
						.setValue(SpikedPalisadeBlock.SOUTH, !state.getValue(TYPE_SOUTH).equals(PalisadeConnection.NONE))
						.setValue(SpikedPalisadeBlock.WEST, !state.getValue(TYPE_WEST).equals(PalisadeConnection.NONE))
						.setValue(WATERLOGGED, state.getValue(WATERLOGGED)), 11);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		return InteractionResult.PASS;
	}

	@Override
	public BlockState getToolModifiedState(BlockState state, UseOnContext context, ToolAction toolAction, boolean simulate) {
		if (strippedForm == null) {
			return null;
		}

		if (toolAction == ToolActions.AXE_STRIP) {
			return strippedForm.get().defaultBlockState()
					.setValue(TYPE_NORTH, state.getValue(TYPE_NORTH))
					.setValue(TYPE_EAST, state.getValue(TYPE_EAST))
					.setValue(TYPE_SOUTH, state.getValue(TYPE_SOUTH))
					.setValue(TYPE_WEST, state.getValue(TYPE_WEST))
					.setValue(WATERLOGGED, state.getValue(WATERLOGGED));
		}
		return null;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockGetter level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
		BlockPos north = pos.north();
		BlockPos east = pos.east();
		BlockPos south = pos.south();
		BlockPos west = pos.west();
		BlockState stateNorth = level.getBlockState(north);
		BlockState stateEast = level.getBlockState(east);
		BlockState stateSouth = level.getBlockState(south);
		BlockState stateWest = level.getBlockState(west);
		return super.getStateForPlacement(context)
				.setValue(TYPE_NORTH, this.getConnectionType(stateNorth, stateNorth.isFaceSturdy(level, north, Direction.SOUTH), Direction.SOUTH))
				.setValue(TYPE_EAST, this.getConnectionType(stateEast, stateEast.isFaceSturdy(level, east, Direction.WEST), Direction.WEST))
				.setValue(TYPE_SOUTH, this.getConnectionType(stateSouth, stateSouth.isFaceSturdy(level, south, Direction.NORTH), Direction.NORTH))
				.setValue(TYPE_WEST, this.getConnectionType(stateWest, stateWest.isFaceSturdy(level, west, Direction.EAST), Direction.EAST))
				.setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
		if (state.getValue(WATERLOGGED)) {
			level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}

		return facing.getAxis().getPlane() == Direction.Plane.HORIZONTAL
				? state.setValue(PROPERTY_BY_DIRECTION.get(facing), this.getConnectionType(facingState, facingState.isFaceSturdy(level, facingPos, facing.getOpposite()), facing.getOpposite()))
				: super.updateShape(state, facing, facingState, level, currentPos, facingPos);
	}

	protected static int indexFor(Direction facing) {
		return 1 << facing.get2DDataValue();
	}

	@Override
	protected int getAABBIndex(BlockState state) {
		return ((CrossCollisionBlockAccessor) this).blockbox$getStateToIndex().computeIntIfAbsent(state, (currentState) -> {
			int i = 0;
			if (!currentState.getValue(TYPE_NORTH).equals(PalisadeConnection.NONE)) {
				i |= indexFor(Direction.NORTH);
			}

			if (!currentState.getValue(TYPE_EAST).equals(PalisadeConnection.NONE)) {
				i |= indexFor(Direction.EAST);
			}

			if (!currentState.getValue(TYPE_SOUTH).equals(PalisadeConnection.NONE)) {
				i |= indexFor(Direction.SOUTH);
			}

			if (!currentState.getValue(TYPE_WEST).equals(PalisadeConnection.NONE)) {
				i |= indexFor(Direction.WEST);
			}

			return i;
		});
	}

	public PalisadeConnection getConnectionType(BlockState state, boolean isSideSolid, Direction direction) {
		PalisadeConnection type = PalisadeConnection.NONE;
		if (state.is(ModTags.SPIKED_PALISADES) || state.getBlock() instanceof SpikedPalisadeBlock) {
			return PalisadeConnection.SPIKED;
		}
		boolean isFenceGateAligned = state.getBlock() instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(state, direction);
		if (state.is(ModTags.PALISADES) || state.is(BlockTags.WALLS) || state.getBlock() instanceof IronBarsBlock || (!isExceptionForConnection(state) && isSideSolid) || isFenceGateAligned) {
			return PalisadeConnection.FULL;
		}
		return type;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TYPE_NORTH, TYPE_EAST, TYPE_WEST, TYPE_SOUTH, WATERLOGGED);
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		switch (rot) {
			case CLOCKWISE_180:
				return state.setValue(TYPE_NORTH, state.getValue(TYPE_SOUTH)).setValue(TYPE_EAST, state.getValue(TYPE_WEST)).setValue(TYPE_SOUTH, state.getValue(TYPE_NORTH)).setValue(TYPE_WEST, state.getValue(TYPE_EAST));
			case COUNTERCLOCKWISE_90:
				return state.setValue(TYPE_NORTH, state.getValue(TYPE_EAST)).setValue(TYPE_EAST, state.getValue(TYPE_SOUTH)).setValue(TYPE_SOUTH, state.getValue(TYPE_WEST)).setValue(TYPE_WEST, state.getValue(TYPE_NORTH));
			case CLOCKWISE_90:
				return state.setValue(TYPE_NORTH, state.getValue(TYPE_WEST)).setValue(TYPE_EAST, state.getValue(TYPE_NORTH)).setValue(TYPE_SOUTH, state.getValue(TYPE_EAST)).setValue(TYPE_WEST, state.getValue(TYPE_SOUTH));
			default:
				return state;
		}
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		switch (mirror) {
			case LEFT_RIGHT:
				return state.setValue(TYPE_NORTH, state.getValue(TYPE_SOUTH)).setValue(TYPE_SOUTH, state.getValue(TYPE_NORTH));
			case FRONT_BACK:
				return state.setValue(TYPE_EAST, state.getValue(TYPE_WEST)).setValue(TYPE_WEST, state.getValue(TYPE_EAST));
			default:
				return super.mirror(state, mirror);
		}
	}
}
