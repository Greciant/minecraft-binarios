package vectorwing.blockbox.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.Tags;
import vectorwing.blockbox.common.registry.ModBlocks;

// Port Forge 1.20.1: useItemOn(...) de 1.21 (ItemInteractionResult) -> use(...) unico de 1.20.1.
public class PackedSnowBlock extends Block
{
	public PackedSnowBlock(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.is(Tags.Items.RODS_WOODEN)) {
			Direction hitDirection = hitResult.getDirection();
			Direction facingDirection = hitDirection.getAxis() == Direction.Axis.Y ? player.getDirection().getOpposite() : hitDirection;
			level.playSound(null, pos, SoundEvents.SNOW_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.setBlock(pos, ModBlocks.CARVED_SNOW.get().defaultBlockState().setValue(CarvedPumpkinBlock.FACING, facingDirection), 11);
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		return InteractionResult.PASS;
	}
}
