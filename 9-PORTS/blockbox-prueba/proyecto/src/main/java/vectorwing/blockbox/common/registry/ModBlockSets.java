package vectorwing.blockbox.common.registry;

import com.google.common.base.Suppliers;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;

import java.util.function.Supplier;

// Port Forge 1.20.1: BlockSetType de 1.20.1 no tiene el campo PressurePlateSensitivity
// (llego en una version posterior); se usa el constructor de 11 parametros que si existe.
public class ModBlockSets
{
	public static final Supplier<BlockSetType> IRON_PLATE = Suppliers.memoize(() -> BlockSetType.register(
			new BlockSetType(
					"iron_plate",
					true,
					SoundType.NETHERITE_BLOCK,
					SoundEvents.IRON_DOOR_CLOSE,
					SoundEvents.IRON_DOOR_OPEN,
					SoundEvents.IRON_TRAPDOOR_CLOSE,
					SoundEvents.IRON_TRAPDOOR_OPEN,
					SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF,
					SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
					SoundEvents.STONE_BUTTON_CLICK_OFF,
					SoundEvents.STONE_BUTTON_CLICK_ON
			)
	));

	public static final Supplier<BlockSetType> GOLD = Suppliers.memoize(() -> BlockSetType.register(
			new BlockSetType(
					"iron_plate_gold",
					true,
					SoundType.METAL,
					SoundEvents.IRON_DOOR_CLOSE,
					SoundEvents.IRON_DOOR_OPEN,
					SoundEvents.IRON_TRAPDOOR_CLOSE,
					SoundEvents.IRON_TRAPDOOR_OPEN,
					SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF,
					SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
					SoundEvents.STONE_BUTTON_CLICK_OFF,
					SoundEvents.STONE_BUTTON_CLICK_ON
			)
	));
}
