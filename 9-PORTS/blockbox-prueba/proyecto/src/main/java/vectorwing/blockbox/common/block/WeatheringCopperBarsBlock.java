package vectorwing.blockbox.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

// Port Forge 1.20.1: sin sistema de codec (no existe en 1.20.1). isRandomlyTicking se detecta
// solo al sobreescribir randomTick, asi que no hace falta la sobrecarga aparte.
public class WeatheringCopperBarsBlock extends IronBarsBlock implements WeatheringCopper
{
	private final WeatheringCopper.WeatherState weatherState;

	public WeatheringCopperBarsBlock(WeatheringCopper.WeatherState weatherState, Properties properties) {
		super(properties);
		this.weatherState = weatherState;
	}

	@Override
	public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		this.applyChangeOverTime(state, level, pos, random);
	}

	public WeatheringCopper.WeatherState getAge() {
		return this.weatherState;
	}
}
