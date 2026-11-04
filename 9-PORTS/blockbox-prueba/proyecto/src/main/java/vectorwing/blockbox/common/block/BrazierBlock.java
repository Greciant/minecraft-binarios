package vectorwing.blockbox.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import vectorwing.blockbox.common.tag.ModTags;

import javax.annotation.Nullable;

/**
 * Port Forge 1.20.1: el original usa ItemAbilities.FIRESTARTER_LIGHT / SHOVEL_DOUSE
 * (NeoForge), que no existen como ToolAction de Forge 1.20.1. En su lugar se revisa
 * directamente el item usado (encendedor/carga de fuego para encender, pala para apagar),
 * con use(...) en vez de getToolModifiedState(...) para esos dos casos.
 */
public class BrazierBlock extends Block implements SimpleWaterloggedBlock
{
	public static final BooleanProperty HANGING = BlockStateProperties.HANGING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private final int fireDamage;

	private static final VoxelShape SHAPE_STANDING = Shapes.or(
			Block.box(4.0, 0.0, 4.0, 12.0, 6.0, 12.0),
			Block.box(1.0, 6.0, 1.0, 15.0, 12.0, 15.0)
	);
	private static final VoxelShape SHAPE_HANGING = Shapes.or(
			Block.box(1.0, 0.0, 1.0, 15.0, 6.0, 15.0)
	);

	protected static final VoxelShape SHAPE_FLAME = Block.box(1.0, 12.0, 1.0, 15.0, 16.0, 15.0);
	protected static final VoxelShape SHAPE_FLAME_HANGING = Block.box(1.0, 6.0, 1.0, 15.0, 12.0, 15.0);

	public BrazierBlock(int fireDamage, Properties properties) {
		super(properties);
		this.fireDamage = fireDamage;
		this.registerDefaultState(this.stateDefinition.any().setValue(HANGING, false).setValue(LIT, true).setValue(WATERLOGGED, false));
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		ItemStack stack = player.getItemInHand(hand);
		if ((stack.getItem() instanceof FlintAndSteelItem || stack.is(Items.FIRE_CHARGE)) && canLight(state)) {
			if (!level.isClientSide) {
				level.setBlock(pos, state.setValue(LIT, true), 11);
				level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
				if (!player.getAbilities().instabuild) {
					if (stack.getItem() instanceof FlintAndSteelItem) {
						stack.hurtAndBreak(1, player, (e) -> e.broadcastBreakEvent(hand));
					} else {
						stack.shrink(1);
					}
				}
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		if (stack.getItem() instanceof ShovelItem && state.getValue(LIT)) {
			if (!level.isClientSide) {
				level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
				douse(player, level, pos, state);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		return InteractionResult.PASS;
	}

	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (state.getValue(LIT) && entity instanceof LivingEntity && isEntityTouchingFlame(entity, pos, state)) {
			entity.hurt(level.damageSources().inFire(), (float) this.fireDamage);
		}

		super.entityInside(state, level, pos, entity);
	}

	protected boolean isEntityTouchingFlame(Entity entity, BlockPos pos, BlockState state) {
		VoxelShape collisionShape = state.getValue(HANGING)
				? SHAPE_FLAME_HANGING.move(pos.getX(), pos.getY(), pos.getZ())
				: SHAPE_FLAME.move(pos.getX(), pos.getY(), pos.getZ());
		return Shapes.joinIsNotEmpty(collisionShape, Shapes.create(entity.getBoundingBox()), BooleanOp.AND);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
		boolean isFlooded = fluidState.getType() == Fluids.WATER;

		for (Direction direction : context.getNearestLookingDirections()) {
			if (direction.getAxis() == Direction.Axis.Y) {
				BlockState blockstate = this.defaultBlockState().setValue(HANGING, direction == Direction.UP);
				if (blockstate.canSurvive(context.getLevel(), context.getClickedPos())) {
					return blockstate.setValue(WATERLOGGED, isFlooded).setValue(LIT, !isFlooded);
				}
			}
		}
		return null;
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (state.getValue(WATERLOGGED)) {
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}

		return getConnectedDirection(state).getOpposite() == direction && !state.canSurvive(level, pos)
				? Blocks.AIR.defaultBlockState()
				: super.updateShape(state, direction, neighborState, level, pos, neighborPos);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(HANGING) ? SHAPE_HANGING : SHAPE_STANDING;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) {
			return;
		}
		if (random.nextInt(10) == 0) {
			level.playLocalSound(
					(double) pos.getX() + 0.5,
					(double) pos.getY() + 0.5,
					(double) pos.getZ() + 0.5,
					SoundEvents.CAMPFIRE_CRACKLE,
					SoundSource.BLOCKS,
					0.5F + random.nextFloat(),
					random.nextFloat() * 0.7F + 0.6F,
					false
			);
		}
		for (int i = 0; i < 3; i++) {
			double d0 = (double) pos.getX() + random.nextDouble();
			double d1 = (double) pos.getY() + random.nextDouble() * 0.5 + 0.5;
			double d2 = (double) pos.getZ() + random.nextDouble();
			level.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0, 0.0, 0.0);
		}
	}

	@Override
	public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
		if (!state.getValue(BlockStateProperties.WATERLOGGED) && fluidState.getType() == Fluids.WATER) {
			boolean flag = state.getValue(LIT);
			if (flag) {
				if (!level.isClientSide()) {
					level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0F, 1.0F);
				}

				douse(null, level, pos, state);
			}

			level.setBlock(pos, state.setValue(WATERLOGGED, true).setValue(LIT, false), 3);
			level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
			return true;
		} else {
			return false;
		}
	}

	public static void douse(@Nullable Entity entity, LevelAccessor level, BlockPos pos, BlockState state) {
		if (level.isClientSide()) {
			for (int i = 0; i < 20; i++) {
				makeParticles((Level) level, pos);
			}
		}

		BlockEntity blockentity = level.getBlockEntity(pos);
		if (blockentity instanceof CampfireBlockEntity) {
			((CampfireBlockEntity) blockentity).dowse();
		}

		level.gameEvent(entity, GameEvent.BLOCK_CHANGE, pos);
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(LIT, false), 11);
		}
	}

	public static void makeParticles(Level level, BlockPos pos) {
		RandomSource randomsource = level.getRandom();
		level.addAlwaysVisibleParticle(
				ParticleTypes.LARGE_SMOKE,
				true,
				(double) pos.getX() + 0.5 + randomsource.nextDouble() / 3.0 * (double) (randomsource.nextBoolean() ? 1 : -1),
				(double) pos.getY() + randomsource.nextDouble() + randomsource.nextDouble(),
				(double) pos.getZ() + 0.5 + randomsource.nextDouble() / 3.0 * (double) (randomsource.nextBoolean() ? 1 : -1),
				0.0,
				0.07,
				0.0
		);
		level.addParticle(
				ParticleTypes.SMOKE,
				(double) pos.getX() + 0.5 + randomsource.nextDouble() / 4.0 * (double) (randomsource.nextBoolean() ? 1 : -1),
				(double) pos.getY() + 0.4,
				(double) pos.getZ() + 0.5 + randomsource.nextDouble() / 4.0 * (double) (randomsource.nextBoolean() ? 1 : -1),
				0.0,
				0.005,
				0.0
		);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HANGING, LIT, WATERLOGGED);
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction direction = getConnectedDirection(state).getOpposite();
		return Block.canSupportCenter(level, pos.relative(direction), direction.getOpposite());
	}

	protected static Direction getConnectedDirection(BlockState state) {
		return state.getValue(HANGING) ? Direction.DOWN : Direction.UP;
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType pathComputationType) {
		return false;
	}

	public static boolean canLight(BlockState state) {
		return state.is(ModTags.BRAZIERS, stateBase -> stateBase.hasProperty(WATERLOGGED) && stateBase.hasProperty(LIT))
				&& !state.getValue(WATERLOGGED)
				&& !state.getValue(LIT);
	}
}
