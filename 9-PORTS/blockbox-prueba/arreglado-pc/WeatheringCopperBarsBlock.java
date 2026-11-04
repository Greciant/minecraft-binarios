package vectorwing.blockbox.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

// Port Forge 1.20.1: sin sistema de codec (no existe en 1.20.1). isRandomlyTicking hay que
// sobreescribirlo (en 1.20.1 no se detecta solo por tener randomTick).
public class WeatheringCopperBarsBlock extends IronBarsBlock implements WeatheringCopper
{
	private final WeatheringCopper.WeatherState weatherState;

	public WeatheringCopperBarsBlock(WeatheringCopper.WeatherState weatherState, Properties properties) {
		super(properties);
		this.weatherState = weatherState;
	}

	@Override
	public boolean isRandomlyTicking(BlockState state) {
		return WeatheringCopper.getNext(state.getBlock()).isPresent();
	}

	@Override
	public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		this.applyChangeOverTime(state, level, pos, random);
	}

	public WeatheringCopper.WeatherState getAge() {
		return this.weatherState;
	}
}
